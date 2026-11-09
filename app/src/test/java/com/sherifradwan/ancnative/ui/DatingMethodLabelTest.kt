package com.sherifradwan.ancnative.ui

import com.sherifradwan.ancnative.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DatingMethodLabelTest {
    @Test
    fun knownCodesMapToLabels() {
        assertEquals(R.string.dating_method_lmp, datingMethodLabelRes("LMP"))
        assertEquals(R.string.dating_method_ultrasound, datingMethodLabelRes("Ultrasound"))
        assertEquals(R.string.dating_method_ivf_transfer, datingMethodLabelRes("IVF_TRANSFER"))
        assertEquals(R.string.dating_method_clinical, datingMethodLabelRes("Clinical"))
    }

    @Test
    fun matchingIgnoresCaseSpacesAndAcceptsIvfAlias() {
        assertEquals(R.string.dating_method_ultrasound, datingMethodLabelRes(" ULTRASOUND "))
        assertEquals(R.string.dating_method_lmp, datingMethodLabelRes("lmp"))
        assertEquals(R.string.dating_method_ivf_transfer, datingMethodLabelRes("ivf"))
    }

    @Test
    fun unknownBlankOrNullFallBackToRaw() {
        assertNull(datingMethodLabelRes("AGREED_EDD"))
        assertNull(datingMethodLabelRes(""))
        assertNull(datingMethodLabelRes(null))
    }
}
