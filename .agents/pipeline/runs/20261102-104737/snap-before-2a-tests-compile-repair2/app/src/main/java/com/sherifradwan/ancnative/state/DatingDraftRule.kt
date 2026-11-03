package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.parseIsoDateOrNull
import java.time.LocalDate

internal fun datingDraftToDating(patientId: String, values: DatingDraftValues): DemoPatientDating {
    return DemoPatientDating(
        patientId = patientId,
        lmpDate = values.lmpDate.ifBlank { null },
        ultrasoundEdd = values.ultrasoundEdd.ifBlank { null },
        agreedEdd = values.agreedEdd.ifBlank { null },
        datingMethod = values.datingMethod.ifBlank { null },
        transferDate = values.transferDate.ifBlank { null },
        embryoAgeDays = values.embryoAgeDays.toIntOrNull(),
        notes = values.notes.ifBlank { null },
    )
}

internal fun isManualAgreedEdd(values: DatingDraftValues): Boolean {
    val agreed = values.agreedEdd.trim()
    if (agreed.isEmpty()) return false
    val parsedAgreed = parseIsoDateOrNull(agreed)
    return parsedAgreed == null || parsedAgreed != methodDueDate(values)
}

private fun methodDueDate(values: DatingDraftValues): LocalDate? {
    val dating = datingDraftToDating("", values)
    return GestationalAgeEngine.dueDateForMethod(dating, dating.datingMethod)
}

internal fun applyDatingChange(
    values: DatingDraftValues,
    field: DatingDraftField,
    value: String,
): DatingDraftValues {
    val wasManual = isManualAgreedEdd(values)
    val updated = when (field) {
        DatingDraftField.LMP_DATE -> values.copy(lmpDate = value)
        DatingDraftField.ULTRASOUND_EDD -> values.copy(ultrasoundEdd = value)
        DatingDraftField.DATING_METHOD -> values.copy(datingMethod = value)
        DatingDraftField.TRANSFER_DATE -> values.copy(transferDate = value)
        DatingDraftField.EMBRYO_AGE_DAYS -> values.copy(embryoAgeDays = value)
        DatingDraftField.AGREED_EDD -> values.copy(agreedEdd = value)
        DatingDraftField.NOTES -> values.copy(notes = value)
    }
    return when (field) {
        DatingDraftField.LMP_DATE,
        DatingDraftField.ULTRASOUND_EDD,
        DatingDraftField.DATING_METHOD,
        DatingDraftField.TRANSFER_DATE,
        DatingDraftField.EMBRYO_AGE_DAYS -> {
            if (wasManual) {
                updated
            } else {
                updated.copy(agreedEdd = methodDueDate(updated)?.toString() ?: "")
            }
        }
        DatingDraftField.AGREED_EDD,
        DatingDraftField.NOTES -> updated
    }
}
