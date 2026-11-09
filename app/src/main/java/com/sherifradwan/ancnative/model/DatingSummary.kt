package com.sherifradwan.ancnative.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class EddLabel { PLAIN, CONFIRMED }

sealed interface DatingOrigin {
    data class Lmp(val date: String) : DatingOrigin
    data class Transfer(val date: String, val embryoAgeDays: Int?) : DatingOrigin
    data object None : DatingOrigin
}

data class DatingSummary(
    val origin: DatingOrigin,
    val edd: LocalDate?,
    val eddLabel: EddLabel,
    val methodCode: String?,
    val discordance: RedatingAdvice?,
)

fun datingSummary(dating: DemoPatientDating?, earliestScanDate: LocalDate?, today: LocalDate): DatingSummary {
    if (dating == null) return DatingSummary(DatingOrigin.None, null, EddLabel.PLAIN, null, null)
    val method = canonicalDatingMethod(dating.datingMethod)
    return DatingSummary(
        origin = originOf(dating, method),
        edd = GestationalAgeEngine.calculateEstimatedDueDate(dating),
        eddLabel = eddLabelOf(dating),
        methodCode = method,
        discordance = discordanceOf(dating, method, earliestScanDate ?: today),
    )
}

private fun originOf(dating: DemoPatientDating, method: String?): DatingOrigin = when {
    method == IVF_TRANSFER_CODE ->
        dating.transferDate?.takeIf { it.isNotBlank() }
            ?.let { DatingOrigin.Transfer(it, dating.embryoAgeDays) }
            ?: DatingOrigin.None
    else -> dating.lmpDate?.takeIf { it.isNotBlank() }?.let { DatingOrigin.Lmp(it) } ?: DatingOrigin.None
}

private fun eddLabelOf(dating: DemoPatientDating): EddLabel {
    val onlyAgreed = dating.lmpDate.isNullOrBlank() &&
        dating.transferDate.isNullOrBlank() &&
        dating.ultrasoundEdd.isNullOrBlank() &&
        !dating.agreedEdd.isNullOrBlank()
    return if (onlyAgreed) EddLabel.CONFIRMED else EddLabel.PLAIN
}

private fun discordanceOf(dating: DemoPatientDating, method: String?, anchor: LocalDate): RedatingAdvice? {
    if (method == IVF_TRANSFER_CODE || method == ULTRASOUND_CODE) return null
    val scanEdd = parseIsoDateOrNull(dating.ultrasoundEdd) ?: return null
    if (scanEdd == parseIsoDateOrNull(dating.agreedEdd)) return null
    val lmp = parseIsoDateOrNull(dating.lmpDate) ?: return null
    val datingGaDays = ChronoUnit.DAYS.between(lmp, anchor)
    val scanGaDays = FULL_TERM_DAYS - ChronoUnit.DAYS.between(anchor, scanEdd)
    return redatingAdvice(datingGaDays, scanGaDays)
}

private const val IVF_TRANSFER_CODE = "IVF_TRANSFER"
private const val ULTRASOUND_CODE = "Ultrasound"
