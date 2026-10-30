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
import com.m57.hermescontrol.ui.chat.ChatUiState
import com.m57.hermescontrol.ui.chat.ChatViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
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
}
