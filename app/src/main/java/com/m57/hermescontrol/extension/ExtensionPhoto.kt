package com.m57.hermescontrol.extension

import android.graphics.BitmapFactory
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import com.m57.hermes.extension.ExtensionProtocol
import java.io.File
import java.io.IOException
import java.security.MessageDigest

internal class ExtensionInputException(
    val status: String,
) : IOException(status)

/** Reject pipes/devices/writable FDs. Copy untrusted bytes off main; never resize/recompress. */
internal fun copyExtensionPhoto(
    fd: ParcelFileDescriptor,
    directory: File,
    active: () -> Boolean = { true },
): File {
    // Public read-only FD validation is available from API 30. Fail closed on older clients.
    if (Build.VERSION.SDK_INT < 30) throw ExtensionInputException("INVALID_MEDIA")
    val stat = Os.fstat(fd.fileDescriptor)
    val flags = Os.fcntlInt(fd.fileDescriptor, OsConstants.F_GETFL, 0)
    if (!OsConstants.S_ISREG(stat.st_mode) || flags and OsConstants.O_ACCMODE != OsConstants.O_RDONLY) {
        throw ExtensionInputException("INVALID_MEDIA")
    }
    if (stat.st_size > ExtensionProtocol.MAX_JPEG_BYTES) throw ExtensionInputException("LIMIT_EXCEEDED")
    directory.mkdirs()
    val file = File.createTempFile("extension-", ".jpg", directory)
    try {
        val deadline = SystemClock.elapsedRealtime() + 30_000
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    if (!active()) throw ExtensionInputException("REVOKED")
                    if (SystemClock.elapsedRealtime() > deadline) throw ExtensionInputException("TIMEOUT")
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > ExtensionProtocol.MAX_JPEG_BYTES) throw ExtensionInputException("LIMIT_EXCEEDED")
                    output.write(buffer, 0, count)
                }
            }
        }
        if (!active()) throw ExtensionInputException("REVOKED")
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        if (options.outMimeType != "image/jpeg" || options.outWidth <= 0 || options.outHeight <= 0) {
            throw ExtensionInputException("INVALID_MEDIA")
        }
        if (options.outWidth.toLong() * options.outHeight > ExtensionProtocol.MAX_PIXELS) {
            throw ExtensionInputException("LIMIT_EXCEEDED")
        }
        file.inputStream().use { input ->
            if (input.read() != 255 || input.read() != 216) throw ExtensionInputException("INVALID_MEDIA")
        }
        java.io.RandomAccessFile(file, "r").use {
            if (it.length() < 4) throw ExtensionInputException("INVALID_MEDIA")
            it.seek(it.length() - 2)
            if (it.read() != 255 || it.read() != 217) throw ExtensionInputException("INVALID_MEDIA")
        }
        val decoded = BitmapFactory.decodeFile(file.path) ?: throw ExtensionInputException("INVALID_MEDIA")
        decoded.recycle()
        return file
    } catch (failure: Throwable) {
        file.delete()
        throw failure
    }
}

internal fun extensionPayloadHash(
    text: String,
    file: File?,
): String {
    val hash = MessageDigest.getInstance("SHA-256")
    val bytes = text.toByteArray(Charsets.UTF_8)
    hash.update(
        java.nio.ByteBuffer
            .allocate(4)
            .putInt(bytes.size)
            .array(),
    )
    hash.update(bytes)
    hash.update(if (file == null) 0.toByte() else 1.toByte())
    file?.inputStream()?.use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            hash.update(buffer, 0, count)
        }
    }
    return hash.digest().joinToString("") { "%02x".format(it) }
}
