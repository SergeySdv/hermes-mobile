package com.m57.hermes.rokid.addon

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.rokid.sprite.aiapp.externalapp.auth.AuthResult
import com.rokid.sprite.aiapp.externalapp.auth.AuthorizationHelper
import com.rokid.sprite.aiapp.externalapp.auth.GlassPermission

class MainActivity : Activity() {
    private val probe get() = application as ProbeApplication
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 64, 24, 32)
            }
        status = TextView(this).apply { textSize = 18f }
        column.addView(status)

        fun button(
            label: String,
            action: () -> Unit,
        ) {
            column.addView(
                Button(this).apply {
                    text = label
                    setOnClickListener {
                        try {
                            action()
                        } catch (_: Exception) {
                            status.text = "Operation failed; no credentials logged"
                        }
                    }
                },
            )
        }
        button("1. Authorize with Hi Rokid Global") {
            if (AuthorizationHelper.isRequiredHiRokidInstalled(this) && AuthorizationHelper.isConnectHiRokid()) {
                AuthorizationHelper.requestAuthorization(this, arrayOf(GlassPermission.CAMERA), 1001)?.let {
                    accept(it.first, it.second)
                }
            } else {
                status.text = "Compatible Hi Rokid Global not found"
            }
        }
        button("Disconnect / reset authorization") { probe.reset() }
        button("2. Connect") { probe.connect() }
        button("3. Query companion") { probe.query() }
        button("4. Install bundled companion") {
            AlertDialog
                .Builder(
                    this,
                ).setMessage(
                    "Install new S3 companion on paired glasses? Requires owner approval. Existing package will not be overwritten.",
                ).setNegativeButton("Cancel", null)
                .setPositiveButton("Install") { _, _ -> probe.install() }
                .show()
        }
        button("5. Start companion") { probe.start() }
        button("6. Send test string") { probe.send() }
        button("I can see the test string on glasses") { probe.confirmVisible() }
        button("Uninstall S3 companion only") {
            AlertDialog
                .Builder(this)
                .setMessage("Remove only the S3 companion from glasses?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Uninstall") { _, _ -> probe.uninstall() }
                .show()
        }
        setContentView(ScrollView(this).apply { addView(column) })
    }

    override fun onStart() {
        super.onStart()
        probe.observer = { status.text = probe.status }
        status.text =
            probe.status
    }

    override fun onStop() {
        probe.observer = null
        super.onStop()
    }

    @Deprecated("SDK authorization callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) accept(resultCode, data)
    }

    private fun accept(
        code: Int,
        data: Intent?,
    ) {
        val result = AuthorizationHelper.parseAuthorizationResult(code, data)
        probe.authorized((result as? AuthResult.AuthSuccess)?.token)
    }
}
