package com.sherifradwan.ancnative.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DatingFieldVisibilityTest {
    private fun shown(m: String?) = datingFieldVisibility(m).let { Triple(it.showLmp, it.showUltrasoundEdd, it.showIvf) }

    @Test fun lmp() = assertEquals(Triple(true, false, false), shown("LMP"))
    @Test fun ultrasound() = assertEquals(Triple(false, true, false), shown("Ultrasound"))
    @Test fun ultrasoundAlias() = assertEquals(Triple(false, true, false), shown("us"))
    @Test fun ivf() = assertEquals(Triple(false, false, true), shown("IVF_TRANSFER"))
    @Test fun clinical() = assertEquals(Triple(true, true, false), shown("Clinical"))

    @Test fun blankHasNoSelectionAndShowsLmpAndScan() {
        assertEquals(Triple(true, true, false), shown(""))
        assertEquals(Triple(true, true, false), shown(null))
        assertNull(datingFieldVisibility("").selectedMethod)
    }

    @Test fun unknownHasNoSelectionAndShowsLmpAndScan() {
        assertEquals(Triple(true, true, false), shown("Mystery"))
        assertNull(datingFieldVisibility("Mystery").selectedMethod)
    }

    @Test fun agreedEddAlwaysShown() {
        listOf("LMP", "Ultrasound", "IVF_TRANSFER", "Clinical", "", "Mystery", null)
            .forEach { assertTrue(it.toString(), datingFieldVisibility(it).showAgreedEdd) }
    }
}
