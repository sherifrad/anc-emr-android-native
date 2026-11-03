package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.eddFromScan
import com.sherifradwan.ancnative.model.parseCanonicalIsoDateOrNull
import com.sherifradwan.ancnative.model.parseIsoDateOrNull
import java.time.LocalDate

internal data class IntakeDating(
    val lmpDate: String?,
    val agreedEdd: String,
    val datingMethod: String = "LMP",
    val ultrasoundEdd: String? = null,
    val transferDate: String? = null,
    val embryoAgeDays: Int? = null,
    val note: String = "",
)

internal fun intakeDatingUpdate(
    existing: DemoPatientDating?,
    values: PatientDraftValues,
): IntakeDating? {
    val method = values.datingMethod.trim()
    val isLmp = method.isEmpty() || method.equals("LMP", ignoreCase = true)

    if (existing != null) {
        if (!isLmp) return null
        val lmp = values.lmpDate.trim().takeIf(String::isNotEmpty) ?: return null
        val parsedLmp = parseCanonicalIsoDateOrNull(lmp) ?: return null
        if (existing.lmpDate?.trim() == lmp) return null
        if (!existing.ultrasoundEdd.isNullOrBlank() ||
            existing.transferDate != null ||
            existing.embryoAgeDays != null ||
            (existing.datingMethod != null && !existing.datingMethod.equals("LMP", ignoreCase = true))
        ) return null

        val dating = datingDraftToDating(
            patientId = "",
            values = DatingDraftValues(lmpDate = lmp, datingMethod = "LMP"),
        )
        val dueDate = GestationalAgeEngine.dueDateForMethod(dating, "LMP") ?: return null
        return IntakeDating(
            lmpDate = lmp,
            agreedEdd = dueDate.toString(),
            datingMethod = "LMP",
            note = "Baseline from intake LMP ($lmp)",
        )
    }

    return when {
        isLmp -> {
            val lmp = values.lmpDate.trim().takeIf(String::isNotEmpty) ?: return null
            parseCanonicalIsoDateOrNull(lmp) ?: return null
            val dating = datingDraftToDating(
                patientId = "",
                values = DatingDraftValues(lmpDate = lmp, datingMethod = "LMP"),
            )
            val dueDate = GestationalAgeEngine.dueDateForMethod(dating, "LMP") ?: return null
            IntakeDating(
                lmpDate = lmp,
                agreedEdd = dueDate.toString(),
                datingMethod = "LMP",
                note = "Baseline from intake LMP ($lmp)",
            )
        }
        method.equals("IVF_TRANSFER", ignoreCase = true) || method.equals("IVF", ignoreCase = true) -> {
            val transfer = values.transferDate.trim().takeIf(String::isNotEmpty) ?: return null
            parseCanonicalIsoDateOrNull(transfer) ?: return null
            val embryoAge = values.embryoAgeDays.trim().toIntOrNull() ?: return null
            if (embryoAge !in ClinicalVocabulary.embryoAges) return null
            val dating = datingDraftToDating(
                patientId = "",
                values = DatingDraftValues(
                    datingMethod = "IVF_TRANSFER",
                    transferDate = transfer,
                    embryoAgeDays = embryoAge.toString(),
                ),
            )
            val dueDate = GestationalAgeEngine.dueDateForMethod(dating, "IVF_TRANSFER") ?: return null
            IntakeDating(
                lmpDate = null,
                agreedEdd = dueDate.toString(),
                datingMethod = "IVF_TRANSFER",
                ultrasoundEdd = null,
                transferDate = transfer,
                embryoAgeDays = embryoAge,
                note = "Baseline from intake IVF transfer ($transfer, day $embryoAge)",
            )
        }
        method.equals("Ultrasound", ignoreCase = true) || method.equals("US", ignoreCase = true) -> {
            val scanDate = values.scanDate.trim().takeIf(String::isNotEmpty) ?: return null
            val parsedScanDate = parseCanonicalIsoDateOrNull(scanDate) ?: return null
            val weeks = values.scanGaWeeks.trim().toIntOrNull() ?: return null
            val days = values.scanGaDays.trim().toIntOrNull() ?: return null
            val scanEdd = eddFromScan(parsedScanDate, weeks, days) ?: return null
            val dating = datingDraftToDating(
                patientId = "",
                values = DatingDraftValues(
                    datingMethod = "Ultrasound",
                    ultrasoundEdd = scanEdd.toString(),
                ),
            )
            val dueDate = GestationalAgeEngine.dueDateForMethod(dating, "Ultrasound") ?: return null
            IntakeDating(
                lmpDate = null,
                agreedEdd = dueDate.toString(),
                datingMethod = "Ultrasound",
                ultrasoundEdd = dueDate.toString(),
                transferDate = null,
                embryoAgeDays = null,
                note = "Baseline from intake scan ($scanDate, ${weeks}w${days}d)",
            )
        }
        method.equals("Clinical", ignoreCase = true) -> {
            val agreed = values.agreedEdd.trim().takeIf(String::isNotEmpty) ?: return null
            val parsedAgreed = parseCanonicalIsoDateOrNull(agreed) ?: return null
            IntakeDating(
                lmpDate = null,
                agreedEdd = parsedAgreed.toString(),
                datingMethod = "Clinical",
                ultrasoundEdd = null,
                transferDate = null,
                embryoAgeDays = null,
                note = "Baseline from intake due date ($parsedAgreed)",
            )
        }
        else -> null
    }
}

internal data class IntakeDatingPreview(val dueDate: String, val gestationalAge: String?)

internal fun intakeDatingPreview(
    values: PatientDraftValues,
    asOf: LocalDate = LocalDate.now(),
): IntakeDatingPreview? {
    val update = intakeDatingUpdate(null, values) ?: return null
    val parsedDue = parseIsoDateOrNull(update.agreedEdd) ?: return null
    val ga = GestationalAgeEngine.calculateForEdd(parsedDue, asOf)?.formatted
    return IntakeDatingPreview(dueDate = update.agreedEdd, gestationalAge = ga)
}

internal fun profileSavedNotice(datingAttempted: Boolean, datingSaved: Boolean): SavedNotice =
    if (datingAttempted && !datingSaved) SavedNotice.PATIENT_DATING_NOT_UPDATED else SavedNotice.PATIENT
