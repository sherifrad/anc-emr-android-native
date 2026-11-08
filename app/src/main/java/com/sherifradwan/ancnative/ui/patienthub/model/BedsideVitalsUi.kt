package com.sherifradwan.ancnative.ui.patienthub.model

import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.FhrLevel
import com.sherifradwan.ancnative.model.SfhConcordance
import com.sherifradwan.ancnative.model.bpLevel
import com.sherifradwan.ancnative.model.checkSfhConcordance
import com.sherifradwan.ancnative.model.fhrLevel
import com.sherifradwan.ancnative.model.isPreEclampsiaWarning
import com.sherifradwan.ancnative.model.parseBloodPressure
import com.sherifradwan.ancnative.model.parseVisitFindings
import com.sherifradwan.ancnative.model.parseWeightKg
import java.util.Locale

data class VitalsSnapshot(
    val bpText: String?,
    val bpLevel: BpLevel,
    val weightText: String?,
    val weightDeltaText: String?,
    val fhrText: String?,
    val fhrLevel: FhrLevel?,
    val urineProtein: String?,
    val sfhCm: String?,
    val sfhConcordance: SfhConcordance?,
    val isPreEclampsiaWarning: Boolean,
)

fun deriveVitalsSnapshot(
    bloodPressure: String?,
    weight: String?,
    findings: String?,
    gaWeeks: Int?,
    bookingWeightKg: Double?,
): VitalsSnapshot {
    val parsedFindings = parseVisitFindings(findings)
    val parsedBp = parseBloodPressure(bloodPressure)
    val bpLev = bpLevel(parsedBp?.systolic, parsedBp?.diastolic)
    val preEclampsia = isPreEclampsiaWarning(parsedBp?.systolic, parsedBp?.diastolic, parsedFindings.urineProtein)
    val sfh = parsedFindings.fundalHeight?.toIntOrNull()
    val sfhConc = checkSfhConcordance(sfh, gaWeeks)
    val fhrLev = fhrLevel(parsedFindings.fetalHeartRate)

    val currentWeightKg = parseWeightKg(weight)
    val weightDeltaText = if (bookingWeightKg != null && currentWeightKg != null && bookingWeightKg > 0.0 && bookingWeightKg != currentWeightKg) {
        val diff = currentWeightKg - bookingWeightKg
        val sign = if (diff >= 0) "+" else ""
        String.format(Locale.US, "Δ %s%.1f kg", sign, diff)
    } else {
        null
    }

    return VitalsSnapshot(
        bpText = bloodPressure?.trim(),
        bpLevel = bpLev,
        weightText = weight?.trim(),
        weightDeltaText = weightDeltaText,
        fhrText = parsedFindings.fetalHeartRate,
        fhrLevel = fhrLev,
        urineProtein = parsedFindings.urineProtein,
        sfhCm = parsedFindings.fundalHeight,
        sfhConcordance = sfhConc,
        isPreEclampsiaWarning = preEclampsia,
    )
}
