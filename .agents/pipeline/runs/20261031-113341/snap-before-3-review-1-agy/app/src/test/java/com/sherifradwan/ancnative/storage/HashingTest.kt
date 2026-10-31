package com.sherifradwan.ancnative.storage

import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HashingTest {
    private val emptyDigest = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    private val abcDigest = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    @Test fun toHexLowerPadsEveryByteToTwoDigits() {
        assertEquals("000fffa0", byteArrayOf(0x00, 0x0f, 0xff.toByte(), 0xa0.toByte()).toHexLower())
    }

    @Test fun toHexLowerOfEmptyIsEmpty() {
        assertEquals("", ByteArray(0).toHexLower())
    }

    @Test fun toHexLowerIsLowerCase() {
        assertEquals("abcdef", byteArrayOf(0xab.toByte(), 0xcd.toByte(), 0xef.toByte()).toHexLower())
    }

    @Test fun sha256OfEmptyBytesMatchesKnownVector() {
        assertEquals(emptyDigest, sha256Hex(ByteArray(0)))
    }

    @Test fun sha256OfAbcBytesMatchesKnownVector() {
        assertEquals(abcDigest, sha256Hex("abc".toByteArray(Charsets.UTF_8)))
    }

    @Test fun sha256OfTextMatchesBytesVariant() {
        assertEquals(abcDigest, sha256Hex("abc"))
        assertEquals(emptyDigest, sha256Hex(""))
    }

    @Test fun sha256OfTextIsUtf8() {
        val text = "café م"
        assertEquals(sha256Hex(text.toByteArray(Charsets.UTF_8)), sha256Hex(text))
    }

    @Test fun sha256OfStreamMatchesBytesVariantAcrossBufferSizes() {
        val data = ByteArray(200_000) { (it * 31 + 7).toByte() }
        assertEquals(sha256Hex(data), sha256Hex(ByteArrayInputStream(data)))
        assertEquals(emptyDigest, sha256Hex(ByteArrayInputStream(ByteArray(0))))
    }

    @Test fun sha256OfStreamDoesNotCloseTheStream() {
        var closed = false
        val stream = object : FilterInputStream(ByteArrayInputStream("abc".toByteArray())) {
            override fun close() {
                closed = true
                super.close()
            }
        }
        assertEquals(abcDigest, sha256Hex(stream))
        assertFalse("sha256Hex(InputStream) must leave closing to the caller", closed)
    }

    @Test fun legacyPinHashInputStaysByteIdentical() {
        val salted = "ANC_CLINIC_SALT_v1_483926_SECURE"
        val expected = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray(Charsets.UTF_8))
            .joinToString("") { b -> String.format("%02x", b) }
        assertEquals(expected, sha256Hex(salted))
    }
}
