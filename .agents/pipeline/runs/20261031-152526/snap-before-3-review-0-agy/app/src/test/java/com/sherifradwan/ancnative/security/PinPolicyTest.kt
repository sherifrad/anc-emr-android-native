package com.sherifradwan.ancnative.security

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinPolicyTest {

    private val salt = ByteArray(16) { (it * 7 + 3).toByte() }

    private fun referencePbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, iterations, 256))
            .encoded

    @Test fun iterationsConstant() = assertEquals(120_000, PIN_ITERATIONS)

    @Test fun acceptsGoodPins() {
        assertTrue(isValidNewPin("482915"))
        assertTrue(isValidNewPin("135790"))
        assertTrue(isValidNewPin("210987"))
    }

    @Test fun rejectsWrongLengthOrChars() {
        assertFalse(isValidNewPin(""))
        assertFalse(isValidNewPin("12345"))
        assertFalse(isValidNewPin("1234567"))
        assertFalse(isValidNewPin("12345a"))
        assertFalse(isValidNewPin("12 456"))
        assertFalse(isValidNewPin("１２３４５６"))
        assertFalse(isValidNewPin("٤٨٢٩١٥"))
    }

    @Test fun rejectsRepeatedDigit() {
        for (d in '0'..'9') assertFalse("$d", isValidNewPin(d.toString().repeat(6)))
    }

    @Test fun rejectsAllTenStraightRuns() {
        val ascending = listOf("012345", "123456", "234567", "345678", "456789")
        val descending = listOf("987654", "876543", "765432", "654321", "543210")
        assertEquals(10, (ascending + descending).size)
        for (p in ascending + descending) assertFalse(p, isValidNewPin(p))
    }

    @Test fun hashIsDeterministic() {
        assertArrayEquals(hashPin("482915", salt, 1000), hashPin("482915", salt, 1000))
    }

    @Test fun hashIs32Bytes() {
        assertEquals(32, hashPin("482915", salt, 1000).size)
    }

    @Test fun differentSaltDifferentHash() {
        val other = salt.copyOf().also { it[0] = (it[0] + 1).toByte() }
        assertFalse(hashPin("482915", salt, 1000).contentEquals(hashPin("482915", other, 1000)))
    }

    @Test fun differentPinDifferentHash() {
        assertFalse(hashPin("482915", salt, 1000).contentEquals(hashPin("482916", salt, 1000)))
    }

    @Test fun matchesPbkdf2HmacSha256Vector() {
        assertArrayEquals(referencePbkdf2("482915", salt, 1000), hashPin("482915", salt, 1000))
    }

    @Test fun defaultIterationsUseConstant() {
        assertArrayEquals(referencePbkdf2("482915", salt, PIN_ITERATIONS), hashPin("482915", salt))
    }

    @Test fun pinMatchesTrueAndFalse() {
        val expected = hashPin("482915", salt, 1000)
        assertTrue(pinMatches("482915", salt, 1000, expected))
        assertFalse(pinMatches("482916", salt, 1000, expected))
        assertFalse(pinMatches("482915", salt, 1001, expected))
        assertFalse(pinMatches("482915", salt, 1000, expected.copyOf(16)))
    }

    @Test fun newSaltIs16BytesAndRandom() {
        val a = newSalt()
        val b = newSalt()
        assertEquals(16, a.size)
        assertEquals(16, b.size)
        assertFalse(a.contentEquals(b))
    }

    @Test fun lockoutDelayBoundaries() {
        for (n in 0..4) assertEquals("n=$n", 0L, lockoutDelayMs(n))
        assertEquals(30_000L, lockoutDelayMs(5))
        assertEquals(60_000L, lockoutDelayMs(6))
        assertEquals(300_000L, lockoutDelayMs(7))
        assertEquals(900_000L, lockoutDelayMs(8))
        assertEquals(900_000L, lockoutDelayMs(9))
        assertEquals(900_000L, lockoutDelayMs(1000))
        assertEquals(0L, lockoutDelayMs(-1))
        assertEquals(0L, lockoutDelayMs(Int.MIN_VALUE))
        assertNotEquals(0L, lockoutDelayMs(5))
    }
}
