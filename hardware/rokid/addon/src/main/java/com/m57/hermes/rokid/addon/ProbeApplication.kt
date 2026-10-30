package com.m57.hermes.rokid.addon

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.m57.hermes.rokid.protocol.DisplayReceipt
import com.rokid.cxr.Caps
import com.rokid.cxr.link.CXRLink
import com.rokid.cxr.link.callbacks.ICXRLinkCbk
import com.rokid.cxr.link.callbacks.ICustomCmdCbk
import com.rokid.cxr.link.callbacks.IGlassAppCbk
import com.rokid.cxr.link.utils.CxrDefs
import com.rokid.cxr.link.utils.GlassInfo
import java.util.UUID

class ProbeApplication : Application() {
    companion object {
        const val TARGET = "com.m57.hermes.rokid.glasses.debug"
        const val CHANNEL = "hermes_s3_text"
        const val ACK = "hermes_s3_ack"
    }

    private val main = Handler(Looper.getMainLooper())
    private var link: CXRLink? = null
    private var token: String? = null
    private var generation = 0
    private var cxr = false
    private var bt = false
    private var opened = false
    private var installed: Boolean? = null
    private val receipt = DisplayReceipt()
    var observer: (() -> Unit)? = null
    var auth = "NOT_RUN"
        private set
    var installation = "UNKNOWN"
        private set
    var launch = "NOT_RUN"
        private set
    val display get() = receipt.status
    var error = ""
        private set
    val ready get() = cxr && bt
    val status get() =
        listOf(
            "Authorization: $auth",
            "CXR: $cxr; Bluetooth: $bt",
            "Installation: $installation",
            "Launch: $launch",
            "Display: $display",
            error,
        ).joinToString("\n")

    private fun update(action: () -> Unit) {
        main.post {
            action()
            observer?.invoke()
        }
    }

    fun reset() {
        generation++
        link?.disconnect()
        link = null
        token = null
        cxr = false
        bt = false
        opened = false
        installed = null
        receipt.invalidate()
        auth = "NOT_RUN"
        installation = "UNKNOWN"
        launch = "NOT_RUN"
        observer?.invoke()
    }

    fun authorized(value: String?) {
        reset()
        token = value?.takeIf { it.isNotBlank() }
        auth = if (token == null) "DENIED_OR_CANCELLED" else "AUTHORIZED"
        observer?.invoke()
    }

    fun connect() {
        if (link != null) return
        val credential = token ?: return
        val epoch = ++generation

        fun sessionUpdate(action: () -> Unit) {
            update { if (epoch == generation) action() }
        }
        val current = CXRLink(this)
        link = current
        check(current.configCXRSession(CxrDefs.CXRSession(CxrDefs.CXRSessionType.CUSTOMAPP, TARGET)))
        current.setCXRLinkCbk(
            object : ICXRLinkCbk {
                override fun onCXRLConnected(connected: Boolean) =
                    sessionUpdate {
                        cxr = connected
                        if (!connected) {
                            opened = false
                            receipt.invalidate()
                            launch = "CONNECTION_LOST"
                        }
                    }

                override fun onGlassBtConnected(connected: Boolean) =
                    sessionUpdate {
                        bt = connected
                        if (!connected) {
                            opened = false
                            receipt.invalidate()
                            launch = "CONNECTION_LOST"
                        }
                    }

                override fun onGlassAiAssistStart() {}

                override fun onGlassAiAssistStop() {}

                override fun onGlassAiInterrupt(interruptWake: Boolean) =
                    sessionUpdate {
                        opened = false
                        receipt.invalidate()
                        launch =
                            "INTERRUPTED"
                    }

                override fun onGlassDeviceInfo(deviceInfo: GlassInfo) {}

                override fun onGlassWearingStatus(wearing: Boolean) {}
            },
        )
        current.setCXRCustomCmdCbk(
            object : ICustomCmdCbk {
                override fun onCustomCmdResult(
                    key: String?,
                    payload: ByteArray?,
                ) {
                    if (key != ACK || payload == null || payload.size > 4096) return
                    val id =
                        runCatching {
                            val caps = Caps.fromBytes(payload)
                            require(caps.size() == 2 && (0..1).all { caps.at(it).type() == Caps.Value.TYPE_STRING })
                            if (caps.size() == 2 && caps.at(0).string == "1") caps.at(1).string else null
                        }.getOrNull() ?: return
                    sessionUpdate { if (ready && opened) receipt.acknowledge(id) }
                }
            },
        )
        if (!current.connect(credential)) {
            error = "CONNECT_REJECTED"
            link = null
            observer?.invoke()
        }
        token = null
    }

    private fun appCallback(): IGlassAppCbk {
        val epoch = generation

        fun sessionUpdate(action: () -> Unit) {
            update { if (epoch == generation) action() }
        }
        return object : IGlassAppCbk {
            override fun onInstallAppResult(success: Boolean) =
                sessionUpdate {
                    installed = success
                    installation = if (success) "INSTALLED" else "INSTALL_FAILED"
                }

            override fun onUnInstallAppResult(success: Boolean) =
                sessionUpdate {
                    if (success) {
                        installed = false
                        opened = false
                    }
                    installation = "UNINSTALL_RESULT=$success"
                }

            override fun onOpenAppResult(success: Boolean) =
                sessionUpdate {
                    opened = success && ready
                    launch =
                        "START_RESULT=$success"
                }

            override fun onStopAppResult(success: Boolean) =
                sessionUpdate {
                    if (success) opened = false
                    launch =
                        "STOP_RESULT=$success"
                }

            override fun onGlassAppResume(resumed: Boolean) =
                sessionUpdate {
                    opened = resumed && ready
                    if (!resumed) receipt.invalidate()
                    launch =
                        "RESUMED=$resumed"
                }

            override fun onQueryAppResult(found: Boolean) =
                sessionUpdate {
                    installed = found
                    installation =
                        if (found) "PRESENT (no overwrite)" else "ABSENT"
                }
        }
    }

    fun query() {
        if (ready) link?.appIsInstalled(appCallback())
    }

    fun install() {
        if (!ready ||
            installed != false
        ) {
            error = "Query first; installation only when absent"
            observer?.invoke()
            return
        }
        val apk = java.io.File(filesDir, "companion.apk")
        assets.open("companion.apk").use { input -> apk.outputStream().use { input.copyTo(it) } }
        installed = null
        installation = "INSTALL_REQUESTED"
        observer?.invoke()
        link?.appUploadAndInstall(apk.absolutePath, appCallback())
    }

    fun uninstall() {
        if (ready) link?.appUninstall(appCallback())
    }

    fun start() {
        if (ready &&
            installed == true
        ) {
            launch = "START_REQUESTED"
            observer?.invoke()
            link?.appStart("com.m57.hermes.rokid.glasses.MainActivity", appCallback())
        }
    }

    fun send() {
        if (!ready || !opened) return
        val id = UUID.randomUUID().toString()
        receipt.sent(id, "Hermes S3 ${id.take(8)}")
        observer?.invoke()
        link?.sendCustomCmd(
            CHANNEL,
            Caps().apply {
                write("1")
                write(id)
                write("Hermes S3 ${id.take(8)}")
            },
        )
        main.postDelayed({
            receipt.timeout(id)
            observer?.invoke()
        }, 10000)
    }

    fun confirmVisible() {
        receipt.confirmVisible()
        observer?.invoke()
    }
}
