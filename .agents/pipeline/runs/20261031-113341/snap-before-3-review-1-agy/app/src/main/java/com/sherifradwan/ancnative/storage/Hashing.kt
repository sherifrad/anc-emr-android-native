package com.sherifradwan.ancnative.storage

import java.io.InputStream
import java.security.MessageDigest

internal fun ByteArray.toHexLower(): String = joinToString("") { "%02x".format(it) }

internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).toHexLower()

internal fun sha256Hex(text: String): String = sha256Hex(text.toByteArray(Charsets.UTF_8))

/** Reads the stream to its end; the caller closes it. */
internal fun sha256Hex(input: InputStream): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(8192)
    var read: Int
    while (input.read(buffer).also { read = it } != -1) {
        digest.update(buffer, 0, read)
    }
    return digest.digest().toHexLower()
}
