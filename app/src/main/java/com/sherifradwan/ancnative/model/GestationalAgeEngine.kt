package com.sherifradwan.ancnative.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
}

object GestationalAgeEngine {

    fun calculateGestationalAgeAt(dating: DemoPatientDating?, dateString: String?): GestationalAgeResult? {
        if (dateString.isNullOrBlank()) return null
        val date = parseIsoDateOrNull(dateString) ?: return null
        return calculate(dating, date)
    }

    fun calculate(dating: DemoPatientDating?, asOfDate: LocalDate = LocalDate.now()): GestationalAgeResult? {
        if (dating == null) return null

        // If an explicit agreed EDD is assigned by clinician, calculate from agreed EDD
        if (!dating.agreedEdd.isNullOrBlank()) {
            val edd = parseIsoDateOrNull(dating.agreedEdd)
            if (edd != null) {
                return calculateForEdd(edd, asOfDate)
            }
        }

        // Method-based calculation
        return when (dating.datingMethod?.uppercase()) {
            "IVF_TRANSFER", "IVF" -> {
                if (!dating.transferDate.isNullOrBlank()) {
                    val transfer = parseIsoDateOrNull(dating.transferDate)
                    if (transfer != null) {
                        val embryoAge = dating.embryoAgeDays ?: 5
                        calculateForIvf(transfer, embryoAge, asOfDate)
                    } else null
                } else null
            }
            "ULTRASOUND", "US" -> {
                if (!dating.ultrasoundEdd.isNullOrBlank()) {
                    val edd = parseIsoDateOrNull(dating.ultrasoundEdd)
                    if (edd != null) calculateForEdd(edd, asOfDate) else null
                } else null
            }
            "LMP" -> {
                if (!dating.lmpDate.isNullOrBlank()) {
                    val lmp = parseIsoDateOrNull(dating.lmpDate)
                    if (lmp != null) calculateForLmp(lmp, asOfDate) else null
                } else null
            }
            else -> {
                // Fallback sequence: ultrasound EDD -> LMP
                if (!dating.ultrasoundEdd.isNullOrBlank()) {
                    val edd = parseIsoDateOrNull(dating.ultrasoundEdd)
                    if (edd != null) return calculateForEdd(edd, asOfDate)
                }
                if (!dating.lmpDate.isNullOrBlank()) {
                    val lmp = parseIsoDateOrNull(dating.lmpDate)
                    if (lmp != null) return calculateForLmp(lmp, asOfDate)
                }
                null
            }
        }
    }

    fun calculateForLmp(lmpDate: LocalDate, asOfDate: LocalDate = LocalDate.now()): GestationalAgeResult? {
        val totalDays = ChronoUnit.DAYS.between(lmpDate, asOfDate)
        if (totalDays < 0 || totalDays > 320) return null
        return formatGestationalDays(totalDays)
    }

    fun calculateForIvf(transferDate: LocalDate, embryoAgeDays: Int, asOfDate: LocalDate = LocalDate.now()): GestationalAgeResult? {
        if (embryoAgeDays !in ClinicalVocabulary.embryoAges) return null
        val daysSinceTransfer = ChronoUnit.DAYS.between(transferDate, asOfDate)
        val totalDays = ivfGaDaysAtTransfer(embryoAgeDays) + daysSinceTransfer
        if (totalDays < 0 || totalDays > 320) return null
        return formatGestationalDays(totalDays)
    }

    fun calculateForEdd(edd: LocalDate, asOfDate: LocalDate = LocalDate.now()): GestationalAgeResult? {
        val daysToEdd = ChronoUnit.DAYS.between(asOfDate, edd)
        val totalDays = FULL_TERM_DAYS - daysToEdd
        if (totalDays < 0 || totalDays > 320) return null
        return formatGestationalDays(totalDays)
    }

    fun calculateEstimatedDueDate(dating: DemoPatientDating?): LocalDate? {
        if (dating == null) return null
        if (!dating.agreedEdd.isNullOrBlank()) {
            parseIsoDateOrNull(dating.agreedEdd)?.let { return it }
        }
        return when (dating.datingMethod?.uppercase()) {
            "IVF_TRANSFER", "IVF" -> {
                if (!dating.transferDate.isNullOrBlank()) {
                    parseIsoDateOrNull(dating.transferDate)?.let { transfer ->
                        val embryoAge = dating.embryoAgeDays ?: 5
                        if (embryoAge !in ClinicalVocabulary.embryoAges) return null
                        ivfEddFromTransfer(transfer, embryoAge)
                    }
                } else null
            }
            "ULTRASOUND", "US" -> {
                if (!dating.ultrasoundEdd.isNullOrBlank()) {
                    parseIsoDateOrNull(dating.ultrasoundEdd)
                } else null
            }
            "LMP" -> {
                if (!dating.lmpDate.isNullOrBlank()) {
                    parseIsoDateOrNull(dating.lmpDate)?.let(::eddFromLmp)
                } else null
            }
            else -> {
                if (!dating.ultrasoundEdd.isNullOrBlank()) {
                    parseIsoDateOrNull(dating.ultrasoundEdd)
                } else if (!dating.lmpDate.isNullOrBlank()) {
                    parseIsoDateOrNull(dating.lmpDate)?.let(::eddFromLmp)
                } else null
            }
        }
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
