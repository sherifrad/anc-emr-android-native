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
        val date = parseDateOrNull(dateString) ?: return null
        return calculate(dating, date)
    }

    fun calculate(dating: DemoPatientDating?, asOfDate: LocalDate = LocalDate.now()): GestationalAgeResult? {
        if (dating == null) return null

        // If an explicit agreed EDD is assigned by clinician, calculate from agreed EDD
        if (!dating.agreedEdd.isNullOrBlank()) {
            val edd = parseDateOrNull(dating.agreedEdd)
            if (edd != null) {
                return calculateForEdd(edd, asOfDate)
            }
        }

        // Method-based calculation
        return when (dating.datingMethod?.uppercase()) {
            "IVF_TRANSFER", "IVF" -> {
                if (!dating.transferDate.isNullOrBlank()) {
                    val transfer = parseDateOrNull(dating.transferDate)
                    if (transfer != null) {
                        val embryoAge = dating.embryoAgeDays ?: 5
                        calculateForIvf(transfer, embryoAge, asOfDate)
                    } else null
                } else null
            }
            "ULTRASOUND", "US" -> {
                if (!dating.ultrasoundEdd.isNullOrBlank()) {
                    val edd = parseDateOrNull(dating.ultrasoundEdd)
                    if (edd != null) calculateForEdd(edd, asOfDate) else null
                } else null
            }
            "LMP" -> {
                if (!dating.lmpDate.isNullOrBlank()) {
                    val lmp = parseDateOrNull(dating.lmpDate)
                    if (lmp != null) calculateForLmp(lmp, asOfDate) else null
                } else null
            }
            else -> {
                // Fallback sequence: ultrasound EDD -> LMP
                if (!dating.ultrasoundEdd.isNullOrBlank()) {
                    val edd = parseDateOrNull(dating.ultrasoundEdd)
                    if (edd != null) return calculateForEdd(edd, asOfDate)
                }
                if (!dating.lmpDate.isNullOrBlank()) {
                    val lmp = parseDateOrNull(dating.lmpDate)
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
        if (embryoAgeDays != 3 && embryoAgeDays != 5) return null
        val daysSinceTransfer = ChronoUnit.DAYS.between(transferDate, asOfDate)
        // Day 3 embryo transfer = 17 days gestational age at transfer (2w 3d)
        // Day 5 blastocyst transfer = 19 days gestational age at transfer (2w 5d)
        val initialGestationalDays = if (embryoAgeDays == 3) 17L else 19L
        val totalDays = initialGestationalDays + daysSinceTransfer
        if (totalDays < 0 || totalDays > 320) return null
        return formatGestationalDays(totalDays)
    }

    fun calculateForEdd(edd: LocalDate, asOfDate: LocalDate = LocalDate.now()): GestationalAgeResult? {
        val daysToEdd = ChronoUnit.DAYS.between(asOfDate, edd)
        val totalDays = 280 - daysToEdd
        if (totalDays < 0 || totalDays > 320) return null
        return formatGestationalDays(totalDays)
    }

    fun calculateEstimatedDueDate(dating: DemoPatientDating?): LocalDate? {
        if (dating == null) return null
        if (!dating.agreedEdd.isNullOrBlank()) {
            parseDateOrNull(dating.agreedEdd)?.let { return it }
        }
        return when (dating.datingMethod?.uppercase()) {
            "IVF_TRANSFER", "IVF" -> {
                if (!dating.transferDate.isNullOrBlank()) {
                    parseDateOrNull(dating.transferDate)?.let { transfer ->
                        val embryoAge = dating.embryoAgeDays ?: 5
                        if (embryoAge != 3 && embryoAge != 5) return null
                        val daysToAdd = if (embryoAge == 3) 263L else 261L
                        transfer.plusDays(daysToAdd)
                    }
                } else null
            }
            "ULTRASOUND", "US" -> {
                if (!dating.ultrasoundEdd.isNullOrBlank()) {
                    parseDateOrNull(dating.ultrasoundEdd)
                } else null
            }
            "LMP" -> {
                if (!dating.lmpDate.isNullOrBlank()) {
                    parseDateOrNull(dating.lmpDate)?.plusDays(280)
                } else null
            }
            else -> {
                if (!dating.ultrasoundEdd.isNullOrBlank()) {
                    parseDateOrNull(dating.ultrasoundEdd)
                } else if (!dating.lmpDate.isNullOrBlank()) {
                    parseDateOrNull(dating.lmpDate)?.plusDays(280)
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

    private fun parseDateOrNull(value: String): LocalDate? = try {
        LocalDate.parse(value.trim())
    } catch (_: Exception) {
        null
    }
}
