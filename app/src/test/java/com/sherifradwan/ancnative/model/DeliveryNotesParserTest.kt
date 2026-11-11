package com.sherifradwan.ancnative.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryNotesParserTest {
    @Test
    fun splitsMayaNotesIntoFourSections() {
        val notes = "Agreed delivery mode: Spontaneous Vaginal Delivery (SVD) at Cleopatra Maternity Hospital.\n" +
            "Delivery fee package agreed: 15,000 EGP (Full hospital stay + pediatrician attendance).\n" +
            "Postpartum note: Successfully delivered healthy female infant on 2026-10-12."
        val s = parseDeliveryNotes(notes)
        assertEquals("Cleopatra Maternity Hospital", s.hospital)
        assertTrue(s.fees!!.startsWith("15,000 EGP"))
        assertTrue(s.plan!!.contains("Spontaneous Vaginal Delivery"))
        assertTrue(s.postpartum!!.startsWith("Successfully delivered"))
    }

    @Test
    fun plainNotesGoToPlanOnly() {
        val s = parseDeliveryNotes("Previous uncomplicated full-term vaginal delivery in 2022.")
        assertNull(s.hospital)
        assertNull(s.fees)
        assertNull(s.postpartum)
        assertEquals("Previous uncomplicated full-term vaginal delivery in 2022.", s.plan)
    }

    @Test
    fun nullNotesYieldEmptySections() {
        val s = parseDeliveryNotes(null)
        assertNull(s.hospital); assertNull(s.fees); assertNull(s.plan); assertNull(s.postpartum)
    }
}
