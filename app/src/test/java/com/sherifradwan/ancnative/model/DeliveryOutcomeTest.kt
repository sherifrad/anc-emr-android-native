package com.sherifradwan.ancnative.model

import com.sherifradwan.ancnative.ui.report.patientShareText
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class DeliveryOutcomeTest {

    private val today = LocalDate.of(2026, 11, 11)
    private val datingStart = LocalDate.of(2026, 3, 1)

    @Test
    fun `not delivered with date gives FIELDS_WITHOUT_DELIVERY`() {
        val fields = DeliveryOutcomeFields(delivered = false, date = "2026-10-01")
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertTrue(errors.contains(DeliveryOutcomeError.FIELDS_WITHOUT_DELIVERY))
    }

    @Test
    fun `not delivered with mode gives FIELDS_WITHOUT_DELIVERY`() {
        val fields = DeliveryOutcomeFields(delivered = false, mode = "vaginal")
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertTrue(errors.contains(DeliveryOutcomeError.FIELDS_WITHOUT_DELIVERY))
    }

    @Test
    fun `not delivered with outcome gives FIELDS_WITHOUT_DELIVERY`() {
        val fields = DeliveryOutcomeFields(delivered = false, outcome = "live_birth")
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertTrue(errors.contains(DeliveryOutcomeError.FIELDS_WITHOUT_DELIVERY))
    }

    @Test
    fun `normalized clears everything when not delivered`() {
        val fields = DeliveryOutcomeFields(
            delivered = false,
            date = "2026-10-01",
            mode = "vaginal",
            outcome = "live_birth"
        )
        val normalized = fields.normalized()
        assertFalse(normalized.delivered)
        assertNull(normalized.date)
        assertNull(normalized.mode)
        assertNull(normalized.outcome)
    }

    @Test
    fun `blank strings become null in normalized`() {
        val fields = DeliveryOutcomeFields(
            delivered = true,
            date = "  ",
            mode = "\t",
            outcome = "\n"
        )
        val normalized = fields.normalized()
        assertNull(normalized.date)
        assertNull(normalized.mode)
        assertNull(normalized.outcome)
    }

    @Test
    fun `delivered with null date mode outcome is valid`() {
        val fields = DeliveryOutcomeFields(delivered = true)
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `invalid date text 2026-13-40 gives DATE_INVALID`() {
        val fields = DeliveryOutcomeFields(delivered = true, date = "2026-13-40")
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertTrue(errors.contains(DeliveryOutcomeError.DATE_INVALID))
    }

    @Test
    fun `invalid date text yesterday gives DATE_INVALID`() {
        val fields = DeliveryOutcomeFields(delivered = true, date = "yesterday")
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertTrue(errors.contains(DeliveryOutcomeError.DATE_INVALID))
    }

    @Test
    fun `invalid date text 2026-2-3 gives DATE_INVALID`() {
        val fields = DeliveryOutcomeFields(delivered = true, date = "2026-2-3")
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertTrue(errors.contains(DeliveryOutcomeError.DATE_INVALID))
    }

    @Test
    fun `today plus 1 day is ok`() {
        val fields = DeliveryOutcomeFields(delivered = true, date = "2026-11-12")
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertFalse(errors.contains(DeliveryOutcomeError.DATE_IN_FUTURE))
    }

    @Test
    fun `today plus 2 days gives DATE_IN_FUTURE`() {
        val fields = DeliveryOutcomeFields(delivered = true, date = "2026-11-13")
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertTrue(errors.contains(DeliveryOutcomeError.DATE_IN_FUTURE))
    }

    @Test
    fun `date before dating start gives DATE_BEFORE_DATING`() {
        val fields = DeliveryOutcomeFields(delivered = true, date = "2026-02-28")
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertTrue(errors.contains(DeliveryOutcomeError.DATE_BEFORE_DATING))
    }

    @Test
    fun `date equal to dating start is ok`() {
        val fields = DeliveryOutcomeFields(delivered = true, date = "2026-03-01")
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertFalse(errors.contains(DeliveryOutcomeError.DATE_BEFORE_DATING))
    }

    @Test
    fun `datingStart null skips that rule`() {
        val fields = DeliveryOutcomeFields(delivered = true, date = "2025-01-01")
        val errors = validateDeliveryOutcome(fields, today, null)
        assertFalse(errors.contains(DeliveryOutcomeError.DATE_BEFORE_DATING))
    }

    @Test
    fun `unknown mode gives MODE_INVALID`() {
        val fields = DeliveryOutcomeFields(delivered = true, mode = "unknown")
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertTrue(errors.contains(DeliveryOutcomeError.MODE_INVALID))
    }

    @Test
    fun `unknown outcome gives OUTCOME_INVALID`() {
        val fields = DeliveryOutcomeFields(delivered = true, outcome = "unknown")
        val errors = validateDeliveryOutcome(fields, today, datingStart)
        assertTrue(errors.contains(DeliveryOutcomeError.OUTCOME_INVALID))
    }

    @Test
    fun `every allowed mode is valid`() {
        deliveryModes.forEach { mode ->
            val fields = DeliveryOutcomeFields(delivered = true, mode = mode)
            val errors = validateDeliveryOutcome(fields, today, datingStart)
            assertFalse("mode $mode should be valid", errors.contains(DeliveryOutcomeError.MODE_INVALID))
        }
    }

    @Test
    fun `every allowed outcome is valid`() {
        deliveryOutcomes.forEach { outcome ->
            val fields = DeliveryOutcomeFields(delivered = true, outcome = outcome)
            val errors = validateDeliveryOutcome(fields, today, datingStart)
            assertFalse("outcome $outcome should be valid", errors.contains(DeliveryOutcomeError.OUTCOME_INVALID))
        }
    }

    @Test
    fun `daysSinceDelivery null date returns null`() {
        assertNull(daysSinceDelivery(null, today))
    }

    @Test
    fun `daysSinceDelivery bad text returns null`() {
        assertNull(daysSinceDelivery("not a date", today))
    }

    @Test
    fun `daysSinceDelivery today returns 0`() {
        assertEquals(0L, daysSinceDelivery("2026-11-11", today))
    }

    @Test
    fun `daysSinceDelivery 6 days ago returns 6`() {
        assertEquals(6L, daysSinceDelivery("2026-11-05", today))
    }

    @Test
    fun `daysSinceDelivery future date returns 0`() {
        assertEquals(0L, daysSinceDelivery("2026-11-15", today))
    }

    private fun patient(delivered: Boolean, date: String? = null, mode: String? = null, outcome: String? = null) = DemoPatient(
        id = "p1", caseId = "P1", displayName = "Patient One", dateOfBirth = null, phone = null, visits = emptyList(),
        delivered = delivered, deliveryDate = date, deliveryMode = mode, deliveryOutcome = outcome,
    )

    @Test
    fun `outcome line is null for a patient who has not delivered`() {
        assertNull(patient(delivered = false).deliveryOutcomeLine())
    }

    @Test
    fun `outcome line joins date, mode and result`() {
        assertEquals(
            "Delivered 05 Nov 2026 · Caesarean · Live birth",
            patient(true, "2026-11-05", DELIVERY_MODE_CAESAREAN, DELIVERY_OUTCOME_LIVE_BIRTH).deliveryOutcomeLine(),
        )
    }

    @Test
    fun `outcome line shows question marks when the date is unknown`() {
        assertEquals("Delivered, date ???", patient(true).deliveryOutcomeLine())
    }

    @Test
    fun `summary text carries the delivery line`() {
        val text = patientShareText(patient(true, "2026-11-05", DELIVERY_MODE_VAGINAL, DELIVERY_OUTCOME_STILLBIRTH))
        assertTrue(text, text.contains("Delivery: Delivered 05 Nov 2026 · Vaginal · Stillbirth"))
        assertTrue(!patientShareText(patient(false)).contains("Delivery:"))
    }
}

