package com.m57.hermes.rokid.glasses

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.TextView
import com.m57.hermes.rokid.protocol.TextFrame
import com.rokid.cxr.CXRServiceBridge
import com.rokid.cxr.Caps

class BridgeApplication : Application() {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var bridge: CXRServiceBridge
    var observer: (() -> Unit)? = null
    var messageText = "Hermes S3 — waiting"
        private set
    var frameId: String? = null
        private set

    private fun update(value: String) {
        main.post {
            frameId = null
            messageText = value
            observer?.invoke()
        }
    }

    fun acknowledge(id: String) {
        if (id == frameId) {
            bridge.sendMessage(
                "hermes_s3_ack",
                Caps().apply {
                    write("1")
                    write(id)
                },
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        bridge = CXRServiceBridge()
        bridge.setStatusListener(
            object : CXRServiceBridge.StatusListener {
                override fun onConnected(
                    p0: String?,
                    p1: String?,
                    p2: Int,
                ) {}

                override fun onDisconnected() {
                    update("Disconnected")
                }

                override fun onConnecting(
                    p0: String?,
                    p1: String?,
                    p2: Int,
                ) {}

                override fun onARTCStatus(
                    p0: Float,
                    p1: Boolean,
                ) {}

                override fun onRokidAccountChanged(p0: String?) {
                    update("Account changed")
                }

                override fun onAudioNoise(p0: Float) {}
            },
        )
        bridge.subscribe(
            "hermes_s3_text",
            object : CXRServiceBridge.MsgCallback {
                override fun onReceive(
                    name: String?,
                    args: Caps?,
                    bytes: ByteArray?,
                ) {
                    val message =
                        runCatching {
                            require(args != null && args.size() == 3)
                            require((0..2).all { args.at(it).type() == Caps.Value.TYPE_STRING })
                            TextFrame.parse((0..2).map { args.at(it).string })
                        }.getOrNull() ?: return
                    main.post {
                        messageText = message.text
                        frameId = message.id
                        observer?.invoke()
                    }
                }
            },
        )
    }
}

class MainActivity : Activity() {
    private val bridge get() = application as BridgeApplication
    private lateinit var label: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        label =
            TextView(this).apply {
                textSize = 28f
                setPadding(24, 24, 24, 24)
            }
        setContentView(label)
    }

    override fun onStart() {
        super.onStart()
        bridge.observer = {
            label.text = bridge.messageText
            val id = bridge.frameId
            label.post { if (!isDestroyed && !isFinishing && id != null && hasWindowFocus()) bridge.acknowledge(id) }
        }
        bridge.observer?.invoke()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) bridge.observer?.invoke()
    }

    override fun onStop() {
        bridge.observer = null
        super.onStop()
    }
}
