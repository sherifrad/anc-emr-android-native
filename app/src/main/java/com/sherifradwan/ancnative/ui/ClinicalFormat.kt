package com.sherifradwan.ancnative.ui

import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.bpLevel

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

/**
 * Attachment title as displayed (Q73): a non-blank trimmed title wins; otherwise the category,
 * followed by " · " and the captured date when [capturedAt] starts with a yyyy-MM-dd date.
 * A blank category falls back to "Attachment". The file name is never used as a title.
 */
internal fun attachmentDisplayTitle(title: String?, category: String, capturedAt: String): String {
    val typed = title?.trim().orEmpty()
    if (typed.isNotEmpty()) return typed
    val label = category.trim().ifEmpty { "Attachment" }
    val date = capturedAt.take(10)
    return if (ISO_DATE.matches(date)) "$label \u00B7 $date" else label
}

/**
 * Visit weight as displayed (Q78): the trimmed text, with " kg" appended unless it already ends
 * with a kg / kilogram(s) unit. Null for null or blank text.
 */
internal fun formatWeightKg(raw: String?): String? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    return if (WEIGHT_UNIT.containsMatchIn(text)) text else "$text kg"
}

/** A trailing kg / kilogram / kilograms unit, optionally after spaces. */
private val WEIGHT_UNIT = Regex("(?:kg|kilograms?)$", RegexOption.IGNORE_CASE)

/** Optional "Age" + optional ':', the number, optional unit y/yr/yrs/year/years, optional '.'. */
private val AGE_TEXT = Regex(
    "^(?:age\\s*:?\\s*)?(\\d+)\\s*(?:years|year|yrs|yr|y)?\\.?$",
    RegexOption.IGNORE_CASE,
)

/** The first 10 characters of a capture timestamp, when they look like a yyyy-MM-dd date. */
private val ISO_DATE = Regex("\\d{4}-\\d{2}-\\d{2}")

private const val MIN_AGE_YEARS = 10
private const val MAX_AGE_YEARS = 70

/** Alert text for a blood pressure value, or null when it is normal or unreadable. */
internal fun bpAlertResource(raw: String?): Int? = when (bpLevel(raw)) {
    BpLevel.RAISED -> R.string.bp_alert_raised
    BpLevel.SEVERE -> R.string.bp_alert_severe
    BpLevel.NORMAL -> null
}
