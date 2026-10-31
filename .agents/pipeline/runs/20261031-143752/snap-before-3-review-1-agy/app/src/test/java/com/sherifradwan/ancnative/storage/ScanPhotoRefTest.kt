package com.sherifradwan.ancnative.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanPhotoRefTest {
    @Test fun prefixIsVault() {
        assertEquals("vault:", SCAN_PHOTO_REF_PREFIX)
    }

    @Test fun roundTrip() {
        val ref = scanPhotoRef("abc_123-X.webp")
        assertEquals("vault:abc_123-X.webp", ref)
        assertTrue(isScanPhotoRef(ref))
        assertEquals("abc_123-X.webp", scanPhotoFileName(ref))
    }

    @Test fun rejectsAnythingThatIsNotAPlainVaultName() {
        listOf(
            "../x.webp", "vault:../x.webp", "a/b.webp", "vault:a/b.webp", "vault:a\\b.webp",
            "x.jpg", "vault:x.jpg", "", "content://media/1", "vault:", "vault:a.b.webp", "vault:.webp",
        ).forEach { assertNull("should reject '$it'", scanPhotoFileName(it)) }
    }

    @Test fun isScanPhotoRefOnlyLooksAtPrefix() {
        assertFalse(isScanPhotoRef("content://media/1"))
        assertFalse(isScanPhotoRef(""))
        assertTrue(isScanPhotoRef("vault:anything"))
    }
}
