package com.m57.hermes.rokid.protocol

import java.util.UUID

data class TextFrame(
    val id: String,
    val text: String,
) {
    companion object {
        fun parse(fields: List<String>): TextFrame? {
            if (fields.size != 3 || fields[0] != "1") return null
            val id = fields[1]
            if (runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false).not()) return null
            if (fields[2].isBlank() || fields[2].toByteArray(Charsets.UTF_8).size > 4096) return null
            return TextFrame(id, fields[2])
        }
    }
}

class DisplayReceipt {
    var pending: String? = null
        private set
    var status = "NOT_SENT"
        private set

    fun sent(
        id: String,
        text: String,
    ) {
        pending = id
        status = "SENT: $text"
    }

    fun acknowledge(id: String) {
        if (id == pending && status.startsWith("SENT:")) status = "VIEW_UPDATED_ACK (visibility unconfirmed)"
    }

    fun timeout(id: String) {
        if (id == pending && status.startsWith("SENT:")) {
            status = "ACK_TIMEOUT"
            pending = null
        }
    }

    fun invalidate() {
        pending = null
        status = "CONNECTION_OR_SCENE_LOST"
    }

    fun confirmVisible() {
        if (status.startsWith("VIEW_UPDATED_ACK")) status = "USER_CONFIRMED_VISIBLE"
    }
}
