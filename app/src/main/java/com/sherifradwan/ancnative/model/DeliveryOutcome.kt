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
        date = date?.trim()?.takeIf { it.isNotBlank() },
        mode = mode?.trim()?.takeIf { it.isNotBlank() },
        outcome = outcome?.trim()?.takeIf { it.isNotBlank() }
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
