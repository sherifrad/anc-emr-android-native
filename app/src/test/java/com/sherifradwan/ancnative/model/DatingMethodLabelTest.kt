package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DatingMethodLabelTest {
    @Test
    fun knownCodesMapToLabels() {
        assertEquals("LMP", datingMethodLabel("LMP"))
        assertEquals("by ultrasound", datingMethodLabel("Ultrasound"))
        assertEquals("ET", datingMethodLabel("IVF_TRANSFER"))
        assertEquals("clinical", datingMethodLabel("Clinical"))
    }

    @Test
    fun matchingIgnoresCaseSpacesAndAcceptsIvfAlias() {
        assertEquals("by ultrasound", datingMethodLabel(" ULTRASOUND "))
        assertEquals("LMP", datingMethodLabel("lmp"))
        assertEquals("ET", datingMethodLabel("ivf"))
    }

    @Test
    fun unknownShownAsTypedAndBlankOrNullIsNull() {
        assertEquals("AGREED_EDD", datingMethodLabel("AGREED_EDD"))
        assertNull(datingMethodLabel(""))
        assertNull(datingMethodLabel(null))
    }
}
