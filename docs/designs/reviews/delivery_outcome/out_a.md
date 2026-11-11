FILE: app/src/main/java/com/sherifradwan/ancnative/model/DeliveryOutcome.kt
package com.sherifradwan.ancnative.model

import java.time.LocalDate
import java.time.format.DateTimeParseException

const val DELIVERY_MODE_VAGINAL = "vaginal"
const val DELIVERY_MODE_CAESAREAN = "caesarean"
const val DELIVERY_MODE_ASSISTED_VAGINAL = "assisted_vaginal"
const val DELIVERY_MODE_OTHER = "other"

const val DELIVERY_OUTCOME_LIVE_BIRTH = "live_birth"
const val DELIVERY_OUTCOME_STILLBIRTH = "stillbirth"
const val DELIVERY_OUTCOME_NEONATAL_DEATH = "neonatal_death"

val deliveryModes = listOf(
    DELIVERY_MODE_VAGINAL,
    DELIVERY_MODE_CAESAREAN,
    DELIVERY_MODE_ASSISTED_VAGINAL,
    DELIVERY_MODE_OTHER
)

val deliveryOutcomes = listOf(
    DELIVERY_OUTCOME_LIVE_BIRTH,
    DELIVERY_OUTCOME_STILLBIRTH,
    DELIVERY_OUTCOME_NEONATAL_DEATH
)

data class DeliveryOutcomeFields(
    val delivered: Boolean = false,
    val date: String? = null,
    val mode: String? = null,
    val outcome: String? = null
)

enum class DeliveryOutcomeError {
    DATE_INVALID,
    DATE_IN_FUTURE,
    DATE_BEFORE_DATING,
    MODE_INVALID,
    OUTCOME_INVALID,
    FIELDS_WITHOUT_DELIVERY
}

fun DeliveryOutcomeFields.normalized(): DeliveryOutcomeFields {
    if (!delivered) {
        return DeliveryOutcomeFields()
    }
    return DeliveryOutcomeFields(
        delivered = true,
        date = date?.trim().takeIf { it.isNotBlank() },
        mode = mode?.trim().takeIf { it.isNotBlank() },
        outcome = outcome?.trim().takeIf { it.isNotBlank() }
    )
}

fun validateDeliveryOutcome(
    fields: DeliveryOutcomeFields,
    today: LocalDate,
    datingStart: LocalDate?
): Set<DeliveryOutcomeError> {
    val errors = mutableSetOf<DeliveryOutcomeError>()
    val normalized = fields.normalized()

    val hasRawFields = fields.date?.isNotBlank() == true
        || fields.mode?.isNotBlank() == true
        || fields.outcome?.isNotBlank() == true

    if (!fields.delivered && hasRawFields) {
        errors.add(DeliveryOutcomeError.FIELDS_WITHOUT_DELIVERY)
    }

    if (!fields.delivered) {
        return errors
    }

    normalized.date?.let { dateStr ->
        val parsedDate = try {
            LocalDate.parse(dateStr)
        } catch (e: DateTimeParseException) {
            errors.add(DeliveryOutcomeError.DATE_INVALID)
            null
        }

        parsedDate?.let { parsed ->
            if (parsed > today.plusDays(1)) {
                errors.add(DeliveryOutcomeError.DATE_IN_FUTURE)
            }
            datingStart?.let { start ->
                if (parsed < start) {
                    errors.add(DeliveryOutcomeError.DATE_BEFORE_DATING)
                }
            }
        }
    }

    normalized.mode?.let { mode ->
        if (mode !in deliveryModes) {
            errors.add(DeliveryOutcomeError.MODE_INVALID)
        }
    }

    normalized.outcome?.let { outcome ->
        if (outcome !in deliveryOutcomes) {
            errors.add(DeliveryOutcomeError.OUTCOME_INVALID)
        }
    }

    return errors
}

fun daysSinceDelivery(date: String?, today: LocalDate): Long? {
    date?.trim()?.takeIf { it.isNotBlank() }?.let { dateStr ->
        try {
            val parsed = LocalDate.parse(dateStr)
            val diff = java.time.temporal.ChronoUnit.DAYS.between(parsed, today)
            return diff.coerceAtLeast(0L)
        } catch (e: DateTimeParseException) {
            return null
        }
    }
    return null
}

FILE: app/src/test/java/com/sherifradwan/ancnative/model/DeliveryOutcomeTest.kt
package com.sherifradwan.ancnative.model

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
}
[Tokens] upload: 9093 download: 2891 cache_read: 0 cache_write: 0

