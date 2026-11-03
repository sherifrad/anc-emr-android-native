package com.sherifradwan.ancnative.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoQualityPresetTest {

    @Test
    fun presetsHaveExactValues() {
        assertEquals(1280, PhotoQualityPreset.SMALL.maxSide)
        assertEquals(70, PhotoQualityPreset.SMALL.webpQuality)
        assertEquals(1600, PhotoQualityPreset.STANDARD.maxSide)
        assertEquals(75, PhotoQualityPreset.STANDARD.webpQuality)
        assertEquals(1920, PhotoQualityPreset.DETAILED.maxSide)
        assertEquals(85, PhotoQualityPreset.DETAILED.webpQuality)
    }

    @Test
    fun exactlyThreePresets() {
        assertEquals(3, PhotoQualityPreset.values().size)
    }

    @Test
    fun defaultIsSmall() {
        assertEquals(PhotoQualityPreset.SMALL, PhotoQualityPreset.DEFAULT)
    }

    @Test
    fun ultrasoundDefaultsToDetailed() {
        assertEquals(PhotoQualityPreset.DETAILED, PhotoQualityPreset.defaultFor(AttachmentCategories.ULTRASOUND))
    }

    @Test
    fun otherCategoriesDefaultToSmall() {
        listOf(
            AttachmentCategories.LAB,
            AttachmentCategories.PRESCRIPTION,
            AttachmentCategories.CLINICAL_PHOTO,
            AttachmentCategories.OTHER,
            "SOMETHING_UNKNOWN",
            "ultrasound",
            ""
        ).forEach { category ->
            assertEquals("category '$category'", PhotoQualityPreset.SMALL, PhotoQualityPreset.defaultFor(category))
        }
    }

    @Test
    fun noPresetExceedsOldLimits() {
        PhotoQualityPreset.values().forEach {
            assertTrue(it.name, it.maxSide <= 1920)
            assertTrue(it.name, it.webpQuality <= 85)
        }
    }
}
