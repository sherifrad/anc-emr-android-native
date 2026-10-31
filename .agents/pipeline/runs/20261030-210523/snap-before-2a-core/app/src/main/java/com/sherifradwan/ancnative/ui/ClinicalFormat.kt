package com.sherifradwan.ancnative.ui

/** True when a recorded blood group is Rh negative ("A-", "O neg", "B Rh -ve", "O minus", ...). */
internal fun isRhNegative(bloodGroup: String?): Boolean {
    val raw = bloodGroup?.trim()?.replace('\u2212', '-')?.lowercase() ?: return false
    if (raw.isEmpty()) return false
    if (raw.contains('+') || Regex("\\b(pos|positive)\\b").containsMatchIn(raw)) return false
    return raw.contains('-') ||
        Regex("\\b(neg|negative|minus)\\b").containsMatchIn(raw)
}

/** Mirror of [isRhNegative]: true only for an explicit Rh positive group; blank/null/"pending" are not. */
internal fun isRhPositive(bloodGroup: String?): Boolean {
    val raw = bloodGroup?.trim()?.replace('\u2212', '-')?.lowercase() ?: return false
    if (raw.isEmpty() || isRhNegative(bloodGroup)) return false
    return raw.contains('+') || Regex("\\b(pos|positive)\\b").containsMatchIn(raw)
}

/**
 * Blood group as displayed: the trimmed value as typed, with a plain-text "(Rh negative)" marker
 * (D1) when the group is Rh negative. Never adds emoji or glyphs (D3).
 */
internal fun formatBloodGroup(bloodGroup: String?): String? {
    val value = bloodGroup?.trim().orEmpty()
    if (value.isEmpty()) return null
    return if (isRhNegative(value)) "$value (Rh negative)" else value
}

/** True only for an explicit high-risk record; "Not high risk" / "Low" / "Moderate" are not high. */
internal fun isHighRisk(risk: String?): Boolean {
    val raw = risk?.trim()?.lowercase() ?: return false
    if (raw.isEmpty()) return false
    if (Regex("\\bnot\\b").containsMatchIn(raw)) return false
    return Regex("\\bhigh\\b").containsMatchIn(raw)
}

/** A whole-text age of 10..70 years, e.g. "29 years" -> "29"; any other text carries no valid age. */
internal fun formatAgeYears(ageText: String?): String? {
    val text = ageText?.trim().orEmpty()
    val years = AGE_TEXT.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: return null
    return if (years in MIN_AGE_YEARS..MAX_AGE_YEARS) years.toString() else null
}

/** Optional "Age" + optional ':', the number, optional unit y/yr/yrs/year/years, optional '.'. */
private val AGE_TEXT = Regex(
    "^(?:age\\s*:?\\s*)?(\\d+)\\s*(?:years|year|yrs|yr|y)?\\.?$",
    RegexOption.IGNORE_CASE,
)

private const val MIN_AGE_YEARS = 10
private const val MAX_AGE_YEARS = 70
