package com.m57.hermescontrol.extension

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import com.m57.hermes.extension.IExtensionBroker
import com.m57.hermes.extension.IExtensionCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Capture/check the kernel caller before hopping to the main-thread broker. No I/O in transactions. */
class ExtensionService : Service() {
    private fun <T> checked(block: (Int) -> T): T {
        val uid = Binder.getCallingUid()
        if (!ExtensionBroker.trusted(this, uid)) throw SecurityException("Untrusted extension")
        return runBlocking(Dispatchers.Main) { block(uid) }
    }

    private val binder =
        object : IExtensionBroker.Stub() {
            override fun onTransact(
                code: Int,
                data: Parcel,
                reply: Parcel?,
                flags: Int,
            ): Boolean {
                if (code in IBinder.FIRST_CALL_TRANSACTION..IBinder.LAST_CALL_TRANSACTION &&
                    data.dataSize() > 64 * 1024
                ) {
                    throw SecurityException("Extension metadata exceeds 64 KiB")
                }
                return super.onTransact(code, data, reply, flags)
            }

            override fun negotiate(
                major: Int,
                minor: Int,
                capabilities: Int,
            ): Bundle = checked { ExtensionBroker.negotiate(major, minor, capabilities) }

            override fun openSession(
                major: Int,
                capabilities: Int,
                callback: IExtensionCallback,
            ): Bundle = checked { ExtensionBroker.open(this@ExtensionService, it, major, capabilities, callback) }

            override fun stageInput(
                grant: String,
                epoch: String,
                operationId: String,
                text: String,
                jpeg: ParcelFileDescriptor?,
            ): Bundle =
                try {
                    checked { ExtensionBroker.stage(this@ExtensionService, it, grant, epoch, operationId, text, jpeg) }
                } finally {
                    jpeg?.close()
                }

            override fun observe(
                grant: String,
                epoch: String,
            ): Bundle = checked { ExtensionBroker.observe(it, grant, epoch) }

            override fun getOperationStatus(
                grant: String,
                epoch: String,
                operationId: String,
            ): Bundle = checked { ExtensionBroker.operation(it, grant, epoch, operationId) }

            override fun closeSession(
                grant: String,
                epoch: String,
            ) {
                checked { ExtensionBroker.close(it, grant, epoch) }
            }
        }

    override fun onBind(intent: Intent): IBinder = binder
}
