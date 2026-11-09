package com.sherifradwan.ancnative.ui.patienthub.model

import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PastDeliveryRowModelTest {

    private fun pregnancy(
        dateText: String? = null,
        outcomeText: String? = null,
        outcome: String? = null,
        birthMethod: String? = null,
        deliveryBy: String? = null,
        pregnancyType: String? = null,
        lossManagement: String? = null,
        terminationMethod: String? = null,
        placeAndDoctor: String? = null,
        otherDetails: String? = null,
    ) = DemoPreviousPregnancy(
        id = "pp-1",
        patientId = "p-1",
        modelVersion = 3,
        dateText = dateText,
        outcomeText = outcomeText,
        pregnancyType = pregnancyType,
        otherDetails = otherDetails,
        placeAndDoctor = placeAndDoctor,
        deliveryBy = deliveryBy,
        outcome = outcome,
        birthMethod = birthMethod,
        lossManagement = lossManagement,
        terminationMethod = terminationMethod,
        provenanceKind = "CLINICIAN_ENTRY",
        clinicianId = "c-1",
        recordedAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
        revision = 1,
    )

    // ORACLE-B1: year extraction

    @Test
    fun year_plainYear() {
        val r = pastDeliveryRow(pregnancy(dateText = "2023"))
        assertEquals("2023", r.year)
        assertNull(r.dateTextFallback)
    }

    @Test
    fun year_isoDateTakesYear() {
        assertEquals("2023", pastDeliveryRow(pregnancy(dateText = "2023-05-20")).year)
    }

    @Test
    fun year_monthNameAndYear() {
        val r = pastDeliveryRow(pregnancy(dateText = "Mar 2019"))
        assertEquals("2019", r.year)
        assertNull(r.dateTextFallback)
    }

    @Test
    fun year_noYearUsesFallback() {
        val r = pastDeliveryRow(pregnancy(dateText = "last year"))
        assertNull(r.year)
        assertEquals("last year", r.dateTextFallback)
    }

    @Test
    fun year_nullAndBlankAndWhitespace() {
        for (t in listOf(null, "", "   ", "\t\n")) {
            val r = pastDeliveryRow(pregnancy(dateText = t))
            assertNull("year for '$t'", r.year)
            assertNull("fallback for '$t'", r.dateTextFallback)
        }
    }

    @Test
    fun year_twoYearsTakesFirst() {
        assertEquals("2019", pastDeliveryRow(pregnancy(dateText = "2019 then 2021")).year)
        assertEquals("1998", pastDeliveryRow(pregnancy(dateText = "from 1998 to 2003")).year)
    }

    @Test
    fun year_boundaryRange() {
        assertEquals("1900", pastDeliveryRow(pregnancy(dateText = "1900")).year)
        assertEquals("2099", pastDeliveryRow(pregnancy(dateText = "2099")).year)
        for (t in listOf("1899", "2100")) {
            val r = pastDeliveryRow(pregnancy(dateText = t))
            assertNull("year for $t", r.year)
            assertEquals(t, r.dateTextFallback)
        }
    }

    @Test
    fun year_twoDigitOrShortNumberIsNotAYear() {
        val r = pastDeliveryRow(pregnancy(dateText = "19"))
        assertNull(r.year)
        assertEquals("19", r.dateTextFallback)
    }

    // ORACLE-B2: tone and label

    @Test
    fun tone_stillbirthIsSevere() {
        assertEquals(OutcomeTone.Severe, pastDeliveryRow(pregnancy(outcome = "Stillbirth")).outcomeTone)
    }

    @Test
    fun tone_earlyLossIsCaution() {
        assertEquals(OutcomeTone.Caution, pastDeliveryRow(pregnancy(outcome = "EarlyPregnancyLoss")).outcomeTone)
    }

    @Test
    fun tone_otherOutcomesAreNeutral() {
        for (o in listOf("LiveBirth", "TerminationOfPregnancy")) {
            assertEquals(o, OutcomeTone.Neutral, pastDeliveryRow(pregnancy(outcome = o)).outcomeTone)
        }
    }

    @Test
    fun tone_freeTextAndMissingAreNeutral() {
        assertEquals(OutcomeTone.Neutral, pastDeliveryRow(pregnancy(outcomeText = "stillbirth at 34 weeks")).outcomeTone)
        assertEquals(OutcomeTone.Neutral, pastDeliveryRow(pregnancy()).outcomeTone)
    }

    @Test
    fun label_neverNullWhenOutcomeOrTextExists() {
        for (o in listOf("LiveBirth", "Stillbirth", "EarlyPregnancyLoss", "TerminationOfPregnancy")) {
            assertNotNull(o, pastDeliveryRow(pregnancy(outcome = o)).outcomeLabel)
        }
        assertEquals("Live female infant", pastDeliveryRow(pregnancy(outcomeText = "Live female infant")).outcomeLabel)
        assertNotNull(pastDeliveryRow(pregnancy(outcome = "Stillbirth", outcomeText = "IUFD at 34 weeks")).outcomeLabel)
    }

    @Test
    fun labelRes_mapsEachEnumValue() {
        mapOf(
            "LiveBirth" to R.string.outcome_live_birth,
            "Stillbirth" to R.string.outcome_stillbirth,
            "EarlyPregnancyLoss" to R.string.outcome_early_loss,
            "TerminationOfPregnancy" to R.string.outcome_termination,
        ).forEach { (o, res) ->
            assertEquals(o, res, pastDeliveryRow(pregnancy(outcome = o)).outcomeLabelRes)
        }
    }

    @Test
    fun labelRes_nullForFreeTextAndMissing() {
        assertNull(pastDeliveryRow(pregnancy(outcomeText = "Live female infant")).outcomeLabelRes)
        assertNull(pastDeliveryRow(pregnancy(outcome = "SomethingElse", outcomeText = "x")).outcomeLabelRes)
        assertNull(pastDeliveryRow(pregnancy()).outcomeLabelRes)
    }

    @Test
    fun label_nullWhenNothingRecordedOrBlank() {
        assertNull(pastDeliveryRow(pregnancy()).outcomeLabel)
        assertNull(pastDeliveryRow(pregnancy(outcome = "  ", outcomeText = " ")).outcomeLabel)
    }

    @Test
    fun label_freeTextOutcomeKeptVerbatim() {
        val text = "Intrauterine fetal demise at 34 weeks (IUFD Stillbirth)"
        assertEquals(text, pastDeliveryRow(pregnancy(outcomeText = text)).outcomeLabel)
    }

    // ORACLE-B3: mode and extras

    @Test
    fun mode_birthMethodWinsOverDeliveryBy() {
        assertEquals("Cesarean", pastDeliveryRow(pregnancy(birthMethod = "Cesarean", deliveryBy = "CS")).mode)
    }

    @Test
    fun mode_fallsBackToDeliveryBy() {
        assertEquals("CS", pastDeliveryRow(pregnancy(deliveryBy = "CS")).mode)
        assertEquals("Vaginal", pastDeliveryRow(pregnancy(birthMethod = "  ", deliveryBy = "Vaginal")).mode)
    }

    @Test
    fun mode_blankBecomesNull() {
        assertNull(pastDeliveryRow(pregnancy()).mode)
        assertNull(pastDeliveryRow(pregnancy(birthMethod = "", deliveryBy = "   ")).mode)
    }

    @Test
    fun extras_orderIsTypeThenLossThenTermination() {
        val r = pastDeliveryRow(
            pregnancy(pregnancyType = "Twin", lossManagement = "Expectant", terminationMethod = "Medical"),
        )
        assertEquals(
            listOf(
                RowExtraKind.PregnancyType to "Twin",
                RowExtraKind.LossManagement to "Expectant",
                RowExtraKind.TerminationMethod to "Medical",
            ),
            r.extras,
        )
    }

    @Test
    fun extras_blanksNeverAppear() {
        val r = pastDeliveryRow(pregnancy(pregnancyType = " ", lossManagement = "Surgical", terminationMethod = ""))
        assertEquals(listOf(RowExtraKind.LossManagement to "Surgical"), r.extras)
        assertTrue(pastDeliveryRow(pregnancy()).extras.isEmpty())
        r.extras.forEach { assertFalse(it.second.isBlank()) }
    }

    @Test
    fun placeAndNotes_blankBecomeNull() {
        val r = pastDeliveryRow(pregnancy(placeAndDoctor = "  ", otherDetails = ""))
        assertNull(r.placeAndDoctor)
        assertNull(r.notes)
        val s = pastDeliveryRow(pregnancy(placeAndDoctor = "Cairo Hospital", otherDetails = "Uncomplicated"))
        assertEquals("Cairo Hospital", s.placeAndDoctor)
        assertEquals("Uncomplicated", s.notes)
    }
}
