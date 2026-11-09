package com.sherifradwan.ancnative.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

fun daysToEdd(edd: LocalDate, asOf: LocalDate): Long = ChronoUnit.DAYS.between(asOf, edd)

data class GestationalAgeResult(
    val weeks: Int,
    val days: Int,
    val totalDays: Long,
    val formatted: String,
    val trimester: Int,
    val isPostTerm: Boolean = false,
) {
    val displayText: String get() = formatted
    val isPostConception: Boolean get() = totalDays >= 0
    val isLateTerm: Boolean get() = weeks == 41
    val progressFraction: Float get() = (totalDays / FULL_TERM_DAYS.toFloat()).coerceIn(0f, 1f)
    val shortWeeksText: String get() = "${weeks}+${days} wks"
}

data class DueDatesByMethod(
    val lmp: LocalDate?,
    val ivf: LocalDate?,
    val scan: LocalDate?,
)

/**
 * Canonical dating method code for a stored value, matched ignoring case and surrounding spaces.
 * "IVF" and "US" are aliases. Unknown values come back trimmed, blank or null gives null.
 */
fun canonicalDatingMethod(method: String?): String? {
    val trimmed = method?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return when (trimmed.uppercase(Locale.ROOT)) {
        "LMP" -> "LMP"
        "ULTRASOUND", "US" -> "Ultrasound"
        "IVF_TRANSFER", "IVF" -> "IVF_TRANSFER"
        "CLINICAL" -> "Clinical"
        else -> trimmed
    }
}

object GestationalAgeEngine {

    /** Null when the method has no usable date, including a parseable date so extreme that adding a term overflows. */
    fun dueDateForMethod(dating: DemoPatientDating, method: String?): LocalDate? =
        derivedOrNull { dueDateForMethodUnsafe(dating, method) }

    private fun dueDateForMethodUnsafe(dating: DemoPatientDating, method: String?): LocalDate? {
        return when (canonicalDatingMethod(method)) {
            "IVF_TRANSFER" -> {
                if (!dating.transferDate.isNullOrBlank()) {
                    val transfer = parseIsoDateOrNull(dating.transferDate) ?: return null
                    val embryoAge = dating.embryoAgeDays ?: 5
                    if (embryoAge !in ClinicalVocabulary.embryoAges) return null
                    ivfEddFromTransfer(transfer, embryoAge)
                } else null
            }
            "Ultrasound" -> {
                if (!dating.ultrasoundEdd.isNullOrBlank()) {
                    parseIsoDateOrNull(dating.ultrasoundEdd)
                } else null
            }
            "LMP" -> {
                if (!dating.lmpDate.isNullOrBlank()) {
                    val lmp = parseIsoDateOrNull(dating.lmpDate) ?: return null
                    eddFromLmp(lmp)
                } else null
            }
            else -> {
                val scan = if (!dating.ultrasoundEdd.isNullOrBlank()) {
                    parseIsoDateOrNull(dating.ultrasoundEdd)
                } else null
                scan ?: dueDateForMethod(dating, "LMP")
            }
        }
    }

    fun dueDatesByMethod(dating: DemoPatientDating): DueDatesByMethod {
        return DueDatesByMethod(
            lmp = dueDateForMethod(dating, "LMP"),
            ivf = dueDateForMethod(dating, "IVF_TRANSFER"),
            scan = dueDateForMethod(dating, "Ultrasound"),
        )
    }

    fun calculateGestationalAgeAt(dating: DemoPatientDating?, dateString: String?): GestationalAgeResult? {
        if (dateString.isNullOrBlank()) return null
        val date = parseIsoDateOrNull(dateString) ?: return null
        return calculate(dating, date)
    }

    fun calculate(dating: DemoPatientDating?, asOfDate: LocalDate = LocalDate.now()): GestationalAgeResult? {
        val edd = calculateEstimatedDueDate(dating) ?: return null
        return calculateForEdd(edd, asOfDate)
    }

    fun calculateForEdd(edd: LocalDate, asOfDate: LocalDate = LocalDate.now()): GestationalAgeResult? {
        val daysToEdd = daysToEdd(edd, asOfDate)
        val totalDays = FULL_TERM_DAYS - daysToEdd
        if (totalDays < 0 || totalDays > 320) return null
        return formatGestationalDays(totalDays)
    }

    fun calculateEstimatedDueDate(dating: DemoPatientDating?): LocalDate? {
        if (dating == null) return null
        if (!dating.agreedEdd.isNullOrBlank()) {
            parseIsoDateOrNull(dating.agreedEdd)?.let { return it }
        }
        return dueDateForMethod(dating, dating.datingMethod)
    }

    private fun formatGestationalDays(totalDays: Long): GestationalAgeResult {
        val weeks = (totalDays / 7).toInt()
        val days = (totalDays % 7).toInt()
        val isPostTerm = weeks >= 42
        val formatted = if (isPostTerm) {
            "${weeks}w ${days}d (Post-term)"
        } else {
            "${weeks}w ${days}d"
        }
        val trimester = when {
            weeks < 14 -> 1
            weeks < 28 -> 2
            else -> 3
        }
        return GestationalAgeResult(
            weeks = weeks,
            days = days,
            totalDays = totalDays,
            formatted = formatted,
            trimester = trimester,
            isPostTerm = isPostTerm,
        )
    }
}
