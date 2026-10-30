package com.m57.hermescontrol.extension

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import com.m57.hermes.extension.IExtensionCallback
import com.m57.hermes.extension.SignerTrust
import com.m57.hermescontrol.BuildConfig
import com.m57.hermescontrol.ui.chat.ChatAttachmentTarget
import com.m57.hermescontrol.ui.chat.ChatViewModel
import com.m57.hermescontrol.ui.chat.MessageRole
import com.m57.hermescontrol.ui.chat.canonicalRestId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import com.m57.hermes.extension.ExtensionProtocol as Wire

/** All mutable state is main-thread confined; no server IDs or auth leave this object. */
internal object ExtensionBroker {
    const val TEST_PACKAGE = "com.m57.hermes.extensiontest"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var viewModel: ChatViewModel? = null
    val consent = MutableStateFlow(false)
    val enabled = MutableStateFlow(false)
    private var client: Client? = null
    private val epoch = UUID.randomUUID().toString()

    private class Client(
        val context: Context,
        val uid: Int,
        var capabilities: Int,
        val callback: IExtensionCallback,
        val target: ChatAttachmentTarget,
        val existingMessages: Set<String>,
        val authScope: com.m57.hermescontrol.data.local.DataScope?,
    ) {
        val events = Channel<Bundle>(Channel.CONFLATED)
        val grant = UUID.randomUUID().toString()
        var expiry = 0L
        var approved = false
        var observing = false
        var busy = false
        val active =
            java.util.concurrent.atomic
                .AtomicBoolean(true)
        var sequence = 0L
        var lastOutput = ""
        var confirmedAt = Long.MAX_VALUE
        val operations = linkedMapOf<String, Pair<String, String>>()
        lateinit var death: IBinder.DeathRecipient
    }

    fun trusted(
        context: Context,
        uid: Int,
    ): Boolean =
        BuildConfig.DEBUG &&
            SignerTrust.matches(
                context.packageManager,
                uid,
                TEST_PACKAGE,
                BuildConfig.EXTENSION_TEST_SIGNER,
            )

    fun attach(vm: ChatViewModel) {
        if (viewModel !== vm) revoke()
        viewModel = vm
    }

    fun detach(vm: ChatViewModel) {
        if (viewModel === vm) {
            revoke()
            viewModel = null
        }
    }

    fun response(status: String): Bundle = Bundle().apply { putString("status", status) }

    fun negotiate(
        major: Int,
        minor: Int,
        capabilities: Int,
    ): Bundle {
        if (major != Wire.MAJOR || minor < 0) return response("UNSUPPORTED_VERSION")
        return response("OK").apply {
            putInt("major", Wire.MAJOR)
            putInt("minor", Wire.MINOR)
            putInt("capabilities", capabilities and Wire.CAPABILITIES)
            putInt("maxTextBytes", Wire.MAX_TEXT_BYTES)
            putInt("maxJpegBytes", Wire.MAX_JPEG_BYTES)
            putInt("minJpegApi", 30)
            putBoolean("jpegSupported", android.os.Build.VERSION.SDK_INT >= 30)
            putLong("maxPixels", Wire.MAX_PIXELS)
            putInt("maxMetadataBytes", 64 * 1024)
            putInt("maxOperations", 8)
            putLong("leaseMs", Wire.LEASE_MS)
        }
    }

    fun open(
        context: Context,
        uid: Int,
        major: Int,
        caps: Int,
        callback: IExtensionCallback,
    ): Bundle {
        if (major != Wire.MAJOR) return response("UNSUPPORTED_VERSION")
        if (caps == 0 || caps and Wire.CAPABILITIES != caps) return response("UNSUPPORTED_CAPABILITY")
        val vm = viewModel ?: return response("OPEN_HERMES_CHAT")
        val target = vm.captureAttachmentTarget() ?: return response("CONVERSATION_UNAVAILABLE")
        if (client != null) return response("BUSY")
        val next =
            Client(
                context.applicationContext,
                uid,
                caps,
                callback,
                target,
                vm.uiState.value.messages
                    .map {
                        it.id
                    }.toSet(),
                com.m57.hermescontrol.data.local.AuthManager
                    .currentDataScope(),
            )
        next.death = IBinder.DeathRecipient { scope.launch { if (client === next) revoke() } }
        try {
            callback.asBinder().linkToDeath(next.death, 0)
        } catch (_: android.os.RemoteException) {
            return response("DISCONNECTED")
        }
        client = next
        consent.value = true
        scope.launch(Dispatchers.IO) {
            try {
                for (event in next.events) {
                    val deliver =
                        withContext(Dispatchers.Main) {
                            event.getString("status") == "REVOKED" ||
                                (
                                    client === next && validContext(next) &&
                                        (!next.approved || SystemClock.elapsedRealtime() < next.expiry)
                                )
                        }
                    if (deliver) callback.onEvent(event)
                }
            } catch (_: android.os.RemoteException) {
                withContext(Dispatchers.Main) { if (client === next) revoke() }
            }
        }
        scope.launch {
            while (client === next) {
                if (!validContext(next) || (next.approved && SystemClock.elapsedRealtime() >= next.expiry)) {
                    revoke()
                    break
                }
                publish(next)
                delay(200)
            }
        }
        return response("AWAITING_USER")
    }

    fun approve(
        draft: Boolean,
        display: Boolean,
    ) {
        val current = client ?: return
        val selected = (if (draft) Wire.DRAFT_WRITE else 0) or (if (display) Wire.ASSISTANT_DISPLAY else 0)
        if (!validContext(current) || selected == 0) {
            revoke()
            return
        }
        current.capabilities = current.capabilities and selected
        current.approved = true
        current.expiry = SystemClock.elapsedRealtime() + Wire.LEASE_MS
        consent.value = false
        enabled.value = true
        current.events.trySend(
            response("GRANTED").apply {
                putString("grant", current.grant)
                putString("epoch", epoch)
                putLong("expiresAtElapsedMs", current.expiry)
                putInt("capabilities", current.capabilities)
            },
        )
    }

    private fun validContext(c: Client): Boolean =
        trusted(c.context, c.uid) && viewModel?.captureAttachmentTarget() == c.target &&
            com.m57.hermescontrol.data.local.AuthManager
                .currentDataScope() == c.authScope

    private fun authorized(
        uid: Int,
        grant: String,
        requestedEpoch: String,
    ): Client? {
        val c = client ?: return null
        if (!validContext(c) || (c.approved && SystemClock.elapsedRealtime() >= c.expiry)) {
            revoke()
            return null
        }
        return c.takeIf { it.uid == uid && it.approved && it.grant == grant && requestedEpoch == epoch }
    }

    fun observe(
        uid: Int,
        grant: String,
        requestedEpoch: String,
    ): Bundle {
        val c = authorized(uid, grant, requestedEpoch) ?: return response("REVOKED")
        if (c.capabilities and Wire.ASSISTANT_DISPLAY == 0) return response("DENIED")
        c.observing = true
        publish(c)
        return response("OK")
    }

    private fun publish(c: Client) {
        if (!c.approved || !c.observing || !validContext(c)) return
        val message =
            viewModel?.uiState?.value?.messages?.lastOrNull {
                it.role == MessageRole.ASSISTANT && it.id !in c.existingMessages &&
                    it.canonicalRestId == null && it.timestamp >= c.confirmedAt &&
                    it.toolName == null && it.approvalInfo == null && it.clarifyInfo == null
            } ?: return
        // Bounded UTF-8 projection; no tool/reasoning/history fields.
        val output = message.content.take(4096)
        if (output == c.lastOutput) return
        c.lastOutput = output
        c.events.trySend(
            response("DISPLAY").apply {
                putString("text", output)
                putLong("sequence", ++c.sequence)
                putString("grant", c.grant)
            },
        )
    }

    fun stage(
        context: Context,
        uid: Int,
        grant: String,
        requestedEpoch: String,
        operationId: String,
        text: String,
        jpeg: ParcelFileDescriptor?,
    ): Bundle {
        val c = authorized(uid, grant, requestedEpoch) ?: return response("REVOKED")
        if (c.capabilities and Wire.DRAFT_WRITE == 0) return response("DENIED")
        if (operationId.length !in 1..64 || text.toByteArray(Charsets.UTF_8).size > Wire.MAX_TEXT_BYTES) {
            return response("LIMIT_EXCEEDED")
        }
        if (text.isBlank() && jpeg == null) return response("EMPTY_INPUT")
        if (c.busy) return response("BUSY")
        if (operationId !in c.operations && c.operations.size >= 8) return response("LIMIT_EXCEEDED")
        val copy = jpeg?.let { ParcelFileDescriptor.dup(it.fileDescriptor) }
        c.busy = true
        scope.launch {
            var file: File? = null
            try {
                val hash =
                    withContext(Dispatchers.IO) {
                        copy?.use {
                            file =
                                copyExtensionPhoto(it, File(context.cacheDir, "extension_staging")) { c.active.get() }
                        }
                        extensionPayloadHash(text, file)
                    }
                if (client !== c || authorized(uid, grant, requestedEpoch) !== c) return@launch
                val previous = c.operations[operationId]
                val status =
                    if (previous != null) {
                        if (previous.first == hash) previous.second else "PAYLOAD_CONFLICT"
                    } else if (viewModel?.stageExtensionDraft(c.target, text, file) == true) {
                        // Ownership transfers to the existing attachment/draft pipeline.
                        file = null
                        "AWAITING_USER"
                    } else {
                        "REVOKED"
                    }
                if (previous == null) c.operations[operationId] = hash to status
                c.events.trySend(response(status).apply { putString("operationId", operationId) })
            } catch (failure: Exception) {
                if (client === c) {
                    c.events.trySend(
                        response(
                            (failure as? ExtensionInputException)?.status ?: "INVALID_MEDIA",
                        ).apply { putString("operationId", operationId) },
                    )
                }
            } finally {
                withContext(Dispatchers.IO) {
                    file?.delete()
                    copy?.close()
                }
                c.busy = false
            }
        }
        return response("UNKNOWN") // No staging ACK yet. Query this operation, never submit automatically.
    }

    fun operation(
        uid: Int,
        grant: String,
        requestedEpoch: String,
        operationId: String,
    ): Bundle {
        val c = authorized(uid, grant, requestedEpoch) ?: return response("REVOKED")
        return response(c.operations[operationId]?.second ?: "UNKNOWN")
    }

    fun close(
        uid: Int,
        grant: String,
        requestedEpoch: String,
    ) {
        if (authorized(uid, grant, requestedEpoch) != null) revoke()
    }

    /** Called only after the existing Send action accepts the user-confirmed draft. */
    fun userConfirmed() {
        val c = client ?: return
        if (!validContext(c)) {
            revoke()
            return
        }
        c.confirmedAt = System.currentTimeMillis()
        c.operations.replaceAll { _, receipt -> receipt.first to "UNKNOWN" }
    }

    fun revoke() {
        val c = client
        client = null
        c?.active?.set(false)
        consent.value = false
        enabled.value = false
        c?.events?.trySend(response("REVOKED"))
        c?.events?.close()
        if (c != null) runCatching { c.callback.asBinder().unlinkToDeath(c.death, 0) }
    }
}
