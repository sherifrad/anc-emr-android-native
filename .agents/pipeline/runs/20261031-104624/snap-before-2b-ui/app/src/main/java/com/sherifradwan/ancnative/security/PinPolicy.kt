package com.sherifradwan.ancnative.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal const val PIN_ITERATIONS = 120_000

internal fun isValidNewPin(pin: String): Boolean {
    if (pin.length != 6 || pin.any { it !in '0'..'9' }) return false
    if (pin.all { it == pin.first() }) return false
    val ascending = pin.zipWithNext().all { (a, b) -> b - a == 1 }
    val descending = pin.zipWithNext().all { (a, b) -> a - b == 1 }
    return !ascending && !descending
}

internal fun hashPin(pin: String, salt: ByteArray, iterations: Int = PIN_ITERATIONS): ByteArray {
    val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
    return try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    } finally {
        spec.clearPassword()
    }
}

internal fun newSalt(): ByteArray = ByteArray(16).also(SecureRandom()::nextBytes)

internal fun pinMatches(pin: String, salt: ByteArray, iterations: Int, expected: ByteArray): Boolean =
    MessageDigest.isEqual(hashPin(pin, salt, iterations), expected)

internal fun lockoutDelayMs(consecutiveFailures: Int): Long = when (consecutiveFailures) {
    in Int.MIN_VALUE..4 -> 0L
    5 -> 30_000L
    6 -> 60_000L
    7 -> 300_000L
    else -> 900_000L
}
