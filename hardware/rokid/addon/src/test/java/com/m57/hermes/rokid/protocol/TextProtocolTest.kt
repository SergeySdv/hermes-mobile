package com.m57.hermes.rokid.protocol

import org.junit.Assert.*
import org.junit.Test

class TextProtocolTest {
    private val id = "aaaabbbb-0000-4000-8000-000000000001"

    @Test fun acceptsExactUtf8Limit() {
        assertNotNull(TextFrame.parse(listOf("1", id, "ж".repeat(2048))))
    }

    @Test fun rejectsUtf8Overflow() {
        assertNull(TextFrame.parse(listOf("1", id, "ж".repeat(2049))))
    }

    @Test fun rejectsVersionAndMalformedFrame() {
        assertNull(TextFrame.parse(listOf("2", id, "text")))
        assertNull(TextFrame.parse(listOf("1", "bad-id", "text")))
        assertNull(TextFrame.parse(listOf("1", id, "")))
        assertNull(TextFrame.parse(listOf("1", id)))
    }

    @Test fun ackIsNotPhysicalDisplayProof() {
        val receipt = DisplayReceipt()
        receipt.sent(id, "text")
        receipt.acknowledge(id)
        assertTrue(receipt.status.startsWith("VIEW_UPDATED_ACK"))
        receipt.confirmVisible()
        assertEquals("USER_CONFIRMED_VISIBLE", receipt.status)
    }

    @Test fun lateAckAfterDisconnectCannotRestoreSuccess() {
        val receipt = DisplayReceipt()
        receipt.sent(id, "text")
        receipt.invalidate()
        receipt.acknowledge(id)
        assertEquals("CONNECTION_OR_SCENE_LOST", receipt.status)
    }

    @Test fun staleAndDuplicateAcknowledgementsDoNotChangeCurrentOperation() {
        val receipt = DisplayReceipt()
        receipt.sent(id, "text")
        receipt.sent("new-id", "new")
        receipt.acknowledge(id)
        assertEquals("SENT: new", receipt.status)
        receipt.acknowledge("new-id")
        receipt.confirmVisible()
        receipt.acknowledge("new-id")
        assertEquals("USER_CONFIRMED_VISIBLE", receipt.status)
    }

    @Test fun timeoutCannotBecomeSuccessFromLateAck() {
        val receipt = DisplayReceipt()
        receipt.sent(id, "text")
        receipt.timeout(id)
        receipt.acknowledge(id)
        receipt.confirmVisible()
        assertEquals("ACK_TIMEOUT", receipt.status)
    }

    @Test fun staleTimeoutDoesNotInvalidateNewOperation() {
        val receipt = DisplayReceipt()
        receipt.sent(id, "text")
        receipt.sent("new-id", "new")
        receipt.timeout(id)
        assertEquals("SENT: new", receipt.status)
    }
}
