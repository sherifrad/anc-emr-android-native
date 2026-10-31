package com.sherifradwan.ancnative.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Q73 (SPEC_FIX_R3.md round 3j): ONE rule for the attachment display title,
 * [attachmentDisplayTitle]. A non-blank trimmed title wins; otherwise the category plus
 * " · " and the first 10 characters of capturedAt when they look like yyyy-MM-dd, else
 * just the category; a blank category falls back to "Attachment"; a file name is never
 * returned.
 */
class AttachmentDisplayTitleTest {

    @Test
    fun titleWinsAndIsTrimmed() {
        assertEquals("Growth scan", attachmentDisplayTitle("  Growth scan  ", "Ultrasound", "2026-10-31"))
        assertEquals("Growth scan", attachmentDisplayTitle("Growth scan", "", ""))
    }

    @Test
    fun blankTitleWithDatedCaptureShowsCategoryAndDate() {
        assertEquals(
            "Ultrasound · 2026-10-31",
            attachmentDisplayTitle(null, "Ultrasound", "2026-10-31T10:11:12Z"),
        )
        // First 10 characters of an already-10-character timestamp is the date itself.
        assertEquals("Ultrasound · 2026-10-31", attachmentDisplayTitle("   ", "Ultrasound", "2026-10-31"))
    }

    @Test
    fun badCapturedAtShowsCategoryOnly() {
        assertEquals("Ultrasound", attachmentDisplayTitle(null, "Ultrasound", ""))
        assertEquals("Ultrasound", attachmentDisplayTitle(null, "Ultrasound", "x"))
    }

    @Test
    fun blankCategoryFallsBackToAttachment() {
        // Spec unit oracle: blank category -> "Attachment" (with and without date).
        // The with-date case keeps the fallback open ("..." date suffix reads both ways),
        // only asserting that the "Attachment" fallback shows and no file name appears.
        assertEquals("Attachment", attachmentDisplayTitle(null, "", ""))
        assertEquals("Attachment", attachmentDisplayTitle("  ", "   ", ""))
        val withDate = attachmentDisplayTitle(null, "", "2026-10-31")
        assertTrue(withDate.startsWith("Attachment"))
    }

    @Test
    fun neverReturnsAFileName() {
        val fallbacks = listOf(
            attachmentDisplayTitle(null, "Ultrasound", "2026-10-31T10:11:12Z"),
            attachmentDisplayTitle(null, "Ultrasound", ""),
            attachmentDisplayTitle(null, "Ultrasound", "x"),
            attachmentDisplayTitle(null, "", ""),
        )
        for (result in fallbacks) {
            assertFalse(result.contains(".webp"))
            assertFalse(result.contains(".jpg"))
            assertFalse(result.contains(".png"))
        }
    }
}
