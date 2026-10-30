package com.m57.hermescontrol.extension

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m57.hermes.extension.ExtensionProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExtensionPhotoTest {
    private val directory =
        File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "s1-tests")
            .apply { mkdirs() }

    private fun jpeg(
        width: Int = 32,
        height: Int = 24,
    ): File =
        File.createTempFile("source", ".jpg", directory).also {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            it.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            bitmap.recycle()
        }

    @Test fun copiesOriginalBytesAndHashesTextAndMedia() {
        val source = jpeg()
        val output =
            copyExtensionPhoto(ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY), directory)
        assertEquals(extensionPayloadHash("hello", source), extensionPayloadHash("hello", output))
        assertFalse(extensionPayloadHash("hello", source) == extensionPayloadHash("other", output))
        source.delete()
        output.delete()
    }

    @Test fun rejectsWritableDescriptorAndPipeWithoutWaiting() {
        val source = jpeg()
        ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_WRITE).use {
            assertEquals(
                "INVALID_MEDIA",
                assertThrows(ExtensionInputException::class.java) {
                    copyExtensionPhoto(it, directory)
                }.status,
            )
        }
        val pipe = ParcelFileDescriptor.createPipe()
        try {
            assertThrows(ExtensionInputException::class.java) { copyExtensionPhoto(pipe[0], directory) }
        } finally {
            pipe.forEach { it.close() }
            source.delete()
        }
    }

    @Test fun rejectsTransportSizedPhotoOverIpcLimitAndMalformedJpeg() {
        val source = File.createTempFile("oversize", ".jpg", directory)
        java.io.RandomAccessFile(source, "rw").use { it.setLength(ExtensionProtocol.MAX_JPEG_BYTES + 1L) }
        ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY).use {
            assertEquals(
                "LIMIT_EXCEEDED",
                assertThrows(ExtensionInputException::class.java) {
                    copyExtensionPhoto(it, directory)
                }.status,
            )
        }
        source.writeText("not a JPEG")
        ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY).use {
            assertThrows(ExtensionInputException::class.java) { copyExtensionPhoto(it, directory) }
        }
        source.delete()
    }

    @Test fun acceptsPixelBoundaryAndRejectsOverLimit() {
        for ((width, height, expected) in listOf(Triple(5000, 4000, true), Triple(5001, 4000, false))) {
            val source = jpeg(width, height)
            ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY).use {
                if (expected) {
                    copyExtensionPhoto(it, directory).delete()
                } else {
                    assertEquals(
                        "LIMIT_EXCEEDED",
                        assertThrows(ExtensionInputException::class.java) {
                            copyExtensionPhoto(it, directory)
                        }.status,
                    )
                }
            }
            source.delete()
        }
    }
}
