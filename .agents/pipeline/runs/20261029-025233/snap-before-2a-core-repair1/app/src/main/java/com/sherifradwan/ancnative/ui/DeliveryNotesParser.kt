package com.sherifradwan.ancnative.ui

internal data class DeliveryNoteSections(
    val hospital: String?,
    val fees: String?,
    val plan: String?,
    val postpartum: String?,
)

/**
 * Splits free-text obstetric notes into the four delivery subsections.
 *
 * Pure function: no I/O, no state. A non-empty input line is never dropped: it always lands in
 * exactly one home section (plan is the fallback), and only a line carrying a "<Name> Hospital"
 * phrase is additionally listed in [DeliveryNoteSections.hospital]. Only a known label prefix is
 * stripped before ':' and only for the fees / postpartum boxes; every other line is kept verbatim.
 */
internal fun parseDeliveryNotes(notes: String?): DeliveryNoteSections {
    val lines = notes?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    if (lines.isEmpty()) return DeliveryNoteSections(null, null, null, null)

    val hospitals = LinkedHashSet<String>()
    val fees = ArrayList<String>()
    val plan = ArrayList<String>()
    val postpartum = ArrayList<String>()

    for (line in lines) {
        val label = knownLabel(line)
        val unconfirmedFee = UNCONFIRMED_FEE.containsMatchIn(line)
        when {
            label == Label.POSTPARTUM -> postpartum += stripLabel(line)
            label == Label.FEE -> if (unconfirmedFee) plan += line else fees += stripLabel(line)
            label != null -> plan += line
            HISTORY_MARKER.containsMatchIn(line) -> plan += line
            CURRENT_DELIVERY.containsMatchIn(line) -> postpartum += line
            FEE_KEYWORD.containsMatchIn(line) && !unconfirmedFee -> fees += line
            else -> plan += line
        }
        HOSPITAL_PHRASE.find(line)?.groupValues?.get(1)?.let { hospitals += it.trim() }
    }

    return DeliveryNoteSections(
        hospital = hospitals.joinToString("\n").ifBlank { null },
        fees = fees.joinToString("\n").ifBlank { null },
        plan = plan.joinToString("\n").ifBlank { null },
        postpartum = postpartum.joinToString("\n").ifBlank { null },
    )
}

private enum class Label { HOSPITAL, PLAN, POSTPARTUM, FEE }

/** Only these label shapes may be stripped before ':'. Anything else (e.g. "10:30") is data. */
private val LABEL_PATTERNS: List<Pair<Label, Regex>> = listOf(
    Label.HOSPITAL to Regex("^hospital$", RegexOption.IGNORE_CASE),
    Label.PLAN to Regex("^plan$", RegexOption.IGNORE_CASE),
    Label.POSTPARTUM to Regex("^postpartum(?:\\s+note)?$", RegexOption.IGNORE_CASE),
    Label.FEE to Regex(
        "^(?:delivery\\s+)?fees?(?:\\s+(?:package|agreed|quoted|discussed|proposed|estimate))*$",
        RegexOption.IGNORE_CASE,
    ),
)

private fun knownLabel(line: String): Label? {
    val colon = line.indexOf(':')
    if (colon <= 0) return null
    val prefix = line.substring(0, colon).trim()
    return LABEL_PATTERNS.firstOrNull { it.second.matches(prefix) }?.first
}

private fun stripLabel(line: String): String {
    val colon = line.indexOf(':')
    if (colon < 0) return line
    return line.substring(colon + 1).trim().ifBlank { line }
}

/** Whole-word fee vocabulary only, so "feels"/"breastfeed" are never treated as fees. */
private val FEE_KEYWORD = Regex("\\b(fees?|egp|cost|costs|package|packages)\\b", RegexOption.IGNORE_CASE)

private val UNCONFIRMED_FEE =
    Regex("\\b(quoted|quote|estimate|estimated|proposed|proposal|discussed|undecided)\\b|\\bnot\\s+agreed\\b", RegexOption.IGNORE_CASE)

/** Markers that make "delivered"/"discharged" describe an earlier pregnancy, not this one. */
private val HISTORY_MARKER =
    Regex("\\b(previous|prior|history|earlier|pregnancy|G\\d+P\\d+)\\b", RegexOption.IGNORE_CASE)

private val CURRENT_DELIVERY = Regex("\\b(delivered|discharged)\\b", RegexOption.IGNORE_CASE)

/** A capitalised "<Name> [Maternity|Medical] Hospital" phrase, never a sentence, time or week count. */
private val HOSPITAL_PHRASE =
    Regex("\\b((?:[A-Z][\\p{L}'&-]*\\s+)+(?:Maternity\\s+|Medical\\s+)?Hospital)\\b")
