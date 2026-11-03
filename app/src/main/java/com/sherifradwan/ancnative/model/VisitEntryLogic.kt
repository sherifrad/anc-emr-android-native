package com.sherifradwan.ancnative.model

import java.time.LocalDate
import java.util.Locale

// -------------------------------------------------------------
// Blood Pressure Flagging
// -------------------------------------------------------------

fun isHighBloodPressure(systolic: Int?, diastolic: Int?): Boolean {
    if (systolic == null && diastolic == null) return false
    val level = bpLevel(systolic, diastolic)
    return level == BpLevel.RAISED || level == BpLevel.SEVERE
}

fun isHighBloodPressure(raw: String?): Boolean {
    val level = bpLevel(raw)
    return level == BpLevel.RAISED || level == BpLevel.SEVERE
}

// -------------------------------------------------------------
// Structured Form Values & Normal Fill Rules
// -------------------------------------------------------------

data class VisitFormValues(
    val date: String = "",
    val systolic: String = "",
    val diastolic: String = "",
    val weight: String = "",
    val fundalHeight: String = "",
    val oedema: String = "",
    val fetalMovements: String = "",
    val urineProtein: String = "",
    val notes: String = "",
)

data class FillNormalResult(
    val filledCount: Int,
    val values: VisitFormValues,
)

/**
 * Fills ONLY blank findings:
 * - Oedema -> "None"
 * - Fetal movements -> "Good"
 * - Urine protein -> "Negative"
 *
 * CLINICAL SAFETY INVARIANT:
 * Strictly NEVER modifies blood pressure, weight, fundal height, date, or notes.
 */
fun fillNormalVisitFindings(values: VisitFormValues): FillNormalResult {
    var count = 0
    val newOedema = if (values.oedema.isBlank()) {
        count++
        "None"
    } else {
        values.oedema
    }

    val newMovements = if (values.fetalMovements.isBlank()) {
        count++
        "Good"
    } else {
        values.fetalMovements
    }

    val newUrine = if (values.urineProtein.isBlank()) {
        count++
        "Negative"
    } else {
        values.urineProtein
    }

    return FillNormalResult(
        filledCount = count,
        values = values.copy(
            oedema = newOedema,
            fetalMovements = newMovements,
            urineProtein = newUrine,
        ),
    )
}

// -------------------------------------------------------------
// "Same as Last" Historical Extraction
// -------------------------------------------------------------

data class LastVisitVitals(
    val bloodPressure: String? = null,
    val systolic: String? = null,
    val diastolic: String? = null,
    val weightKg: String? = null,
    val fundalHeightCm: String? = null,
)

fun resolveLastVisitVitals(visits: List<DemoVisit>, excludeVisitId: String? = null): LastVisitVitals {
    val prior = visits
        .filterNot { it.id == excludeVisitId }
        .sortedWith(
            compareByDescending<DemoVisit> { parseCanonicalIsoDateOrNull(it.date) ?: LocalDate.MIN }
                .thenByDescending { it.revision },
        )
        .firstOrNull() ?: return LastVisitVitals()

    val parsedBp = parseBloodPressure(prior.bloodPressure)
    val parsedWt = parseWeightKg(prior.weight)
    val parsedFindings = parseVisitFindings(prior.findings)

    return LastVisitVitals(
        bloodPressure = prior.bloodPressure?.trim()?.takeIf { it.isNotEmpty() },
        systolic = parsedBp?.systolic?.toString(),
        diastolic = parsedBp?.diastolic?.toString(),
        weightKg = parsedWt?.let { String.format(Locale.US, "%.1f", it) },
        fundalHeightCm = parsedFindings.fundalHeight,
    )
}

// -------------------------------------------------------------
// "Matches GA" Estimation for Fundal Height
// -------------------------------------------------------------

fun estimateFundalHeightForGa(gaWeeks: Int?): Int? {
    if (gaWeeks == null) return null
    return if (gaWeeks in 10..50) gaWeeks else null
}

// -------------------------------------------------------------
// Findings Serialization & Round-Trip Parsing
// -------------------------------------------------------------

data class ParsedVisitFindings(
    val fundalHeight: String? = null,
    val oedema: String? = null,
    val fetalMovements: String? = null,
    val urineProtein: String? = null,
    val additionalFindings: String? = null,
)

fun buildVisitFindings(
    fundalHeight: String?,
    oedema: String?,
    fetalMovements: String?,
    urineProtein: String?,
    additionalFindings: String? = null,
): String {
    val parts = mutableListOf<String>()

    fundalHeight?.trim()?.takeIf { it.isNotEmpty() }?.let {
        parts.add("FH $it cm")
    }
    oedema?.trim()?.takeIf { it.isNotEmpty() }?.let {
        parts.add("Oedema ${it.lowercase(Locale.ROOT)}")
    }
    fetalMovements?.trim()?.takeIf { it.isNotEmpty() }?.let {
        parts.add("Movements ${it.lowercase(Locale.ROOT)}")
    }
    urineProtein?.trim()?.takeIf { it.isNotEmpty() }?.let {
        parts.add("Urine ${it.lowercase(Locale.ROOT)}")
    }
    additionalFindings?.trim()?.takeIf { it.isNotEmpty() }?.let {
        parts.add(it)
    }

    return parts.joinToString(" · ")
}

private val structuredFhRegex = Regex("""^(?:FH|Fundal height)\s*[:=]?\s*(\d{1,3})(?:\s*cm)?$""", RegexOption.IGNORE_CASE)
private val structuredOedemaRegex = Regex("""^(?:Oedema|Edema)\s*[:=]?\s*([A-Za-z0-9+]+(?:\s+[A-Za-z0-9+]+)?)$""", RegexOption.IGNORE_CASE)
private val structuredMovementsRegex = Regex("""^(?:Movements|Fetal movement[s]?)\s*[:=]?\s*([A-Za-z]+)$""", RegexOption.IGNORE_CASE)
private val structuredUrineRegex = Regex("""^(?:Urine(?:\s*protein|\s*dipstick)?)\s*[:=]?\s*([A-Za-z0-9+]+)$""", RegexOption.IGNORE_CASE)

private val legacyNoEdemaRegex = Regex("""\bno\s+o?edema\b""", RegexOption.IGNORE_CASE)
private val legacyActiveMovementsRegex = Regex("""\bactive\s+(?:fetal\s+)?movements?\b""", RegexOption.IGNORE_CASE)
private val legacyUrineNegativeRegex = Regex("""\burine\s+(?:dipstick\s+)?negative\b""", RegexOption.IGNORE_CASE)
private val legacyFhRegex = Regex("""(?:Fundal height|FH)\s*[:=]?\s*(\b\d{1,2}\b)\s*cm""", RegexOption.IGNORE_CASE)

fun parseVisitFindings(findingsText: String?): ParsedVisitFindings {
    if (findingsText.isNullOrBlank()) return ParsedVisitFindings()

    var fh: String? = null
    var oed: String? = null
    var mov: String? = null
    var uri: String? = null

    // Split strictly by canonical joiner " · " to avoid breaking commas in free text
    val tokens = findingsText.split(" · ").map { it.trim() }.filter { it.isNotEmpty() }
    val remainingTokens = mutableListOf<String>()

    for (token in tokens) {
        val fhMatch = structuredFhRegex.matchEntire(token)
        if (fhMatch != null && fh == null) {
            val num = fhMatch.groupValues[1].toIntOrNull()
            if (num != null && num in 10..50) {
                fh = num.toString()
                continue
            }
        }

        val oedMatch = structuredOedemaRegex.matchEntire(token)
        if (oedMatch != null && oed == null) {
            oed = capitalizeValue(oedMatch.groupValues[1])
            continue
        }

        val movMatch = structuredMovementsRegex.matchEntire(token)
        if (movMatch != null && mov == null) {
            mov = capitalizeValue(movMatch.groupValues[1])
            continue
        }

        val uriMatch = structuredUrineRegex.matchEntire(token)
        if (uriMatch != null && uri == null) {
            val raw = uriMatch.groupValues[1]
            uri = if (raw.contains("+")) raw else capitalizeValue(raw)
            continue
        }

        remainingTokens.add(token)
    }

    // Only if none of the structured tokens were matched (e.g. legacy plain narrative),
    // scan for legacy narrative patterns without overwriting structured values
    val isLegacyNarrative = fh == null && oed == null && mov == null && uri == null
    if (isLegacyNarrative) {
        if (legacyNoEdemaRegex.containsMatchIn(findingsText)) {
            oed = "None"
        }
        if (legacyActiveMovementsRegex.containsMatchIn(findingsText)) {
            mov = "Good"
        }
        if (legacyUrineNegativeRegex.containsMatchIn(findingsText)) {
            uri = "Negative"
        }
        legacyFhRegex.find(findingsText)?.let {
            val num = it.groupValues[1].toIntOrNull()
            if (num != null && num in 10..50) {
                fh = num.toString()
            }
        }
    }

    val extra = if (remainingTokens.isNotEmpty()) remainingTokens.joinToString(" · ") else null

    return ParsedVisitFindings(
        fundalHeight = fh,
        oedema = oed,
        fetalMovements = mov,
        urineProtein = uri,
        additionalFindings = extra,
    )
}

private fun capitalizeValue(str: String): String {
    val trimmed = str.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed.substring(0, 1).uppercase(Locale.ROOT) + trimmed.substring(1).lowercase(Locale.ROOT)
}

// -------------------------------------------------------------
// Flagged Findings Detector
// -------------------------------------------------------------

fun detectFlaggedFindings(
    systolic: String?,
    diastolic: String?,
    oedema: String?,
    fetalMovements: String?,
    urineProtein: String?,
): List<String> {
    val flags = mutableListOf<String>()

    val sys = systolic?.toIntOrNull()
    val dia = diastolic?.toIntOrNull()
    if (isHighBloodPressure(sys, dia)) {
        flags.add("High BP")
    }

    if (oedema?.equals("Marked", ignoreCase = true) == true) {
        flags.add("Oedema marked")
    } else if (oedema?.equals("Mild", ignoreCase = true) == true) {
        flags.add("Oedema mild")
    }

    if (fetalMovements?.equals("Reduced", ignoreCase = true) == true) {
        flags.add("Movements reduced")
    } else if (fetalMovements?.equals("Absent", ignoreCase = true) == true) {
        flags.add("Movements absent")
    }

    if (urineProtein != null) {
        val trimmed = urineProtein.trim()
        if (trimmed.equals("Trace", ignoreCase = true)) {
            flags.add("Urine trace")
        } else if (trimmed.contains("+")) {
            flags.add("Urine $trimmed")
        }
    }

    return flags
}

// -------------------------------------------------------------
// Summary Formatter Matching Mock
// -------------------------------------------------------------

fun formatVisitSummary(
    dateText: String,
    gaText: String?,
    systolic: String?,
    diastolic: String?,
    weightKg: String?,
    fundalHeightCm: String?,
    oedema: String?,
    fetalMovements: String?,
    urineProtein: String?,
): String {
    val parts = mutableListOf<String>()
    val head = if (!gaText.isNullOrBlank()) "$dateText · $gaText" else dateText
    parts.add(head)

    val s = systolic?.trim()?.takeIf { it.isNotEmpty() }
    val d = diastolic?.trim()?.takeIf { it.isNotEmpty() }
    if (s != null && d != null) {
        parts.add("BP $s/$d")
    } else if (s != null) {
        parts.add("BP $s/—")
    } else if (d != null) {
        parts.add("BP —/$d")
    }

    if (!weightKg.isNullOrBlank()) {
        parts.add("Wt $weightKg kg")
    }
    if (!fundalHeightCm.isNullOrBlank()) {
        parts.add("FH $fundalHeightCm cm")
    }
    if (!oedema.isNullOrBlank()) {
        parts.add("Oedema ${oedema.lowercase(Locale.ROOT)}")
    }
    if (!fetalMovements.isNullOrBlank()) {
        parts.add("Movements ${fetalMovements.lowercase(Locale.ROOT)}")
    }
    if (!urineProtein.isNullOrBlank()) {
        parts.add("Urine ${urineProtein.lowercase(Locale.ROOT)}")
    }

    return parts.joinToString(" · ")
}
