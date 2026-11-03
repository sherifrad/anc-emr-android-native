package com.sherifradwan.ancnative.ui

/** True when a recorded blood group is Rh negative ("A-", "O neg", "B Rh -ve", "O minus", ...). */
internal fun isRhNegative(bloodGroup: String?): Boolean {
    val raw = bloodGroup?.trim()?.replace('\u2212', '-')?.lowercase() ?: return false
    if (raw.isEmpty()) return false
    if (raw.contains('+') || Regex("\\b(pos|positive)\\b").containsMatchIn(raw)) return false
    return raw.contains('-') ||
        Regex("\\b(neg|negative|minus)\\b").containsMatchIn(raw)
}

/** True only for an explicit high-risk record; "Not high risk" / "Low" / "Moderate" are not high. */
internal fun isHighRisk(risk: String?): Boolean {
    val raw = risk?.trim()?.lowercase() ?: return false
    if (raw.isEmpty()) return false
    if (Regex("\\bnot\\b").containsMatchIn(raw)) return false
    return Regex("\\bhigh\\b").containsMatchIn(raw)
}

/** Digits-only age, e.g. "29 years" -> "29"; null when the text carries no valid age. */
internal fun formatAgeYears(ageText: String?): String? =
    ageText?.let { Regex("\\d+").find(it)?.value }
