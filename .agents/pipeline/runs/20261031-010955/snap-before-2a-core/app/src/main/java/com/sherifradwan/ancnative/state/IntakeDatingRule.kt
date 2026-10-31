package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.eddFromLmp
import com.sherifradwan.ancnative.model.parseCanonicalIsoDateOrNull

internal data class IntakeDating(val lmpDate: String, val agreedEdd: String)

internal fun intakeDatingUpdate(existing: DemoPatientDating?, draftLmp: String?): IntakeDating? {
    val lmp = draftLmp?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val parsedLmp = parseCanonicalIsoDateOrNull(lmp) ?: return null

    if (existing != null) {
        if (existing.lmpDate?.trim() == lmp) return null
        if (!existing.ultrasoundEdd.isNullOrBlank() ||
            existing.transferDate != null ||
            existing.embryoAgeDays != null ||
            existing.datingMethod != null && !existing.datingMethod.equals("LMP", ignoreCase = true)
        ) return null
    }

    return IntakeDating(lmp, eddFromLmp(parsedLmp).toString())
}
