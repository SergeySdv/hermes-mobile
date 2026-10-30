package com.m57.hermescontrol.extension

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m57.hermes.extension.IExtensionCallback
import com.m57.hermescontrol.BuildConfig
import com.m57.hermescontrol.ui.chat.ChatAttachmentTarget
import com.m57.hermescontrol.ui.chat.ChatMessage
import com.m57.hermescontrol.ui.chat.ChatUiState
import com.m57.hermescontrol.ui.chat.ChatViewModel
import com.m57.hermescontrol.ui.chat.MessageRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Broker state tests; kernel identity is tested separately by the installed add-on. */
@RunWith(AndroidJUnit4::class)
class ExtensionBrokerTest {
    private fun main(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    @Test fun deduplicationExpiryAndConversationChangeFailClosed() {
        assumeTrue("Requires the opt-in signed S1 harness", BuildConfig.EXTENSION_TEST_SIGNER.isNotEmpty())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uid = context.packageManager.getApplicationInfo(ExtensionBroker.TEST_PACKAGE, 0).uid
        val vm = mockk<ChatViewModel>(relaxed = true)
        var target = ChatAttachmentTarget("synthetic", 1, "http://127.0.0.1:19119", null, null)
        every { vm.captureAttachmentTarget() } answers { target }
        every { vm.uiState } returns MutableStateFlow(ChatUiState())
        every { vm.stageExtensionDraft(any(), any(), any()) } returns true
        val events = LinkedBlockingQueue<Bundle>()
        val callback =
            object : IExtensionCallback.Stub() {
                override fun onEvent(event: Bundle) {
                    events.offer(event)
                }
            }

        fun event(status: String): Bundle {
            val result = events.poll(5, TimeUnit.SECONDS) ?: error("Missing $status")
            assertEquals(status, result.getString("status"))
            return result
        }
        try {
            main {
                ExtensionBroker.revoke()
                ExtensionBroker.attach(vm)
                assertEquals("AWAITING_USER", ExtensionBroker.open(context, uid, 1, 3, callback).getString("status"))
                ExtensionBroker.approve(true, true)
            }
            val grant = event("GRANTED")
            val token = requireNotNull(grant.getString("grant"))
            val epoch = requireNotNull(grant.getString("epoch"))
            main {
                assertEquals("REVOKED", ExtensionBroker.observe(uid + 1, token, epoch).getString("status"))
                assertEquals("REVOKED", ExtensionBroker.observe(uid, token, "wrong").getString("status"))
                assertEquals(
                    "UNKNOWN",
                    ExtensionBroker.stage(context, uid, token, epoch, "op", "hello", null).getString("status"),
                )
            }
            event("AWAITING_USER")
            // The receipt is emitted before final cleanup; drain the main queue before retrying.
            Thread.sleep(100)
            main { ExtensionBroker.stage(context, uid, token, epoch, "op", "hello", null) }
            event("AWAITING_USER")
            Thread.sleep(100)
            main { ExtensionBroker.stage(context, uid, token, epoch, "op", "changed", null) }
            event("PAYLOAD_CONFLICT")
            verify(exactly = 1) { vm.stageExtensionDraft(any(), "hello", null) }
            main {
                assertEquals(
                    "LIMIT_EXCEEDED",
                    ExtensionBroker
                        .stage(
                            context,
                            uid,
                            token,
                            epoch,
                            "large",
                            "x".repeat(16385),
                            null,
                        ).getString("status"),
                )
                // Expire the existing grant without adding a production clock/renewal API.
                val field = ExtensionBroker.javaClass.getDeclaredField("client").apply { isAccessible = true }
                val client = requireNotNull(field.get(ExtensionBroker))
                client.javaClass
                    .getDeclaredField("expiry")
                    .apply { isAccessible = true }
                    .setLong(client, SystemClock.elapsedRealtime() - 1)
                assertEquals("REVOKED", ExtensionBroker.observe(uid, token, epoch).getString("status"))
                assertFalse(ExtensionBroker.enabled.value)
            }
            event("REVOKED")
            main {
                assertEquals("AWAITING_USER", ExtensionBroker.open(context, uid, 1, 3, callback).getString("status"))
                ExtensionBroker.approve(true, true)
            }
            val next = event("GRANTED")
            main {
                target = target.copy(generation = 2)
                assertEquals(
                    "REVOKED",
                    ExtensionBroker
                        .stage(
                            context,
                            uid,
                            next.getString("grant")!!,
                            next.getString("epoch")!!,
                            "late",
                            "must not move",
                            null,
                        ).getString("status"),
                )
                assertFalse(ExtensionBroker.enabled.value)
            }
            verify(exactly = 1) { vm.stageExtensionDraft(any(), any(), any()) }
        } finally {
            main { ExtensionBroker.detach(vm) }
        }
    }

    @Test fun profileAndConversationSwitchDiscardInFlightDraftAndQueuedDisplay() {
        assumeTrue("Requires the opt-in signed S1 harness", BuildConfig.EXTENSION_TEST_SIGNER.isNotEmpty())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uid = context.packageManager.getApplicationInfo(ExtensionBroker.TEST_PACKAGE, 0).uid
        val initial = ChatAttachmentTarget("conversation-a", 1, "http://127.0.0.1:19119", "connection-a", "agent-a")
        val changedTargets =
            listOf(
                initial.copy(sessionId = "conversation-b"),
                initial.copy(connectionProfileId = "connection-b"),
                initial.copy(agentProfileId = "agent-b"),
                initial.copy(baseUrl = "http://127.0.0.1:19120"),
            )
        for (changed in changedTargets) {
            val hashing = CountDownLatch(1)
            val releaseHash = CountDownLatch(1)
            mockkStatic(::extensionPayloadHash)
            every { extensionPayloadHash(any(), any()) } answers {
                hashing.countDown()
                check(releaseHash.await(5, TimeUnit.SECONDS))
                callOriginal()
            }
            val vm = mockk<ChatViewModel>(relaxed = true)
            var target = initial
            val state = MutableStateFlow(ChatUiState())
            every { vm.captureAttachmentTarget() } answers { target }
            every { vm.uiState } returns state
            every { vm.stageExtensionDraft(any(), any(), any()) } returns true
            val events = LinkedBlockingQueue<Bundle>()
            val callback =
                object : IExtensionCallback.Stub() {
                    override fun onEvent(event: Bundle) {
                        events.offer(event)
                    }
                }
            try {
                main {
                    ExtensionBroker.revoke()
                    ExtensionBroker.attach(vm)
                    assertEquals(
                        "AWAITING_USER",
                        ExtensionBroker.open(context, uid, 1, 3, callback).getString("status"),
                    )
                    ExtensionBroker.approve(true, true)
                }
                val granted = requireNotNull(events.poll(5, TimeUnit.SECONDS))
                assertEquals("GRANTED", granted.getString("status"))
                val token = requireNotNull(granted.getString("grant"))
                val epoch = requireNotNull(granted.getString("epoch"))
                var pendingClient: Any? = null
                main {
                    assertEquals("OK", ExtensionBroker.observe(uid, token, epoch).getString("status"))
                    ExtensionBroker.userConfirmed()
                    state.value =
                        ChatUiState(messages = listOf(ChatMessage(role = MessageRole.ASSISTANT, content = "old scope")))
                    val field = ExtensionBroker.javaClass.getDeclaredField("client").apply { isAccessible = true }
                    pendingClient = requireNotNull(field.get(ExtensionBroker))
                    // Queue output while still authorized; IO delivery must recheck the scope after this main turn.
                    ExtensionBroker.javaClass
                        .getDeclaredMethod("publish", pendingClient!!.javaClass)
                        .apply { isAccessible = true }
                        .invoke(ExtensionBroker, pendingClient)
                    assertEquals(
                        "UNKNOWN",
                        ExtensionBroker
                            .stage(
                                context,
                                uid,
                                token,
                                epoch,
                                "in-flight",
                                "old draft",
                                null,
                            ).getString("status"),
                    )
                    assertTrue("Import reached IO boundary", hashing.await(5, TimeUnit.SECONDS))
                    target = changed
                    releaseHash.countDown()
                    assertEquals("REVOKED", ExtensionBroker.observe(uid, token, epoch).getString("status"))
                    assertEquals(
                        "REVOKED",
                        ExtensionBroker
                            .stage(
                                context,
                                uid,
                                token,
                                epoch,
                                "late",
                                "late draft",
                                null,
                            ).getString("status"),
                    )
                }
                val busy = pendingClient!!.javaClass.getDeclaredField("busy").apply { isAccessible = true }
                val deadline = SystemClock.elapsedRealtime() + 5000
                var importing = true
                while (importing && SystemClock.elapsedRealtime() < deadline) {
                    main { importing = busy.getBoolean(pendingClient) }
                    if (importing) Thread.sleep(10)
                }
                assertFalse("In-flight import must finish", importing)
                val revoked = requireNotNull(events.poll(5, TimeUnit.SECONDS))
                assertEquals("REVOKED", revoked.getString("status"))
                assertTrue("No stale DISPLAY or staging ACK", events.isEmpty())
                verify(exactly = 0) { vm.stageExtensionDraft(any(), any(), any()) }
            } finally {
                releaseHash.countDown()
                main { ExtensionBroker.detach(vm) }
                unmockkStatic(::extensionPayloadHash)
            }
        }
    }
}
