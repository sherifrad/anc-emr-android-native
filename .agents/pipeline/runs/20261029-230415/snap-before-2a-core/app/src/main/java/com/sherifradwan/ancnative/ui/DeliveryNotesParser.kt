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
 * Pure function: no I/O, no state. Each line is split into segments at ';' and at a sentence end
 * (". " before a capital letter, never after the abbreviations Dr/Mr/Mrs/Ms/St/No/approx/vs/e.g/i.e),
 * and every segment is classified on its own. Consecutive segments of the same line that land in
 * the same section are rejoined with their original separator, so an unsplit line reads as typed,
 * and different source lines are joined with a newline.
 *
 * A non-empty segment is never dropped: it always lands in exactly one home section (plan is the
 * fallback), and only a known label prefix is stripped before ':' and only for the fees / postpartum
 * boxes; every other segment is kept verbatim. A "<Name> Hospital" phrase or a "Hospital: <name>"
 * value is additionally listed in [DeliveryNoteSections.hospital] while its source line stays in
 * its home section.
 */
internal fun parseDeliveryNotes(notes: String?): DeliveryNoteSections {
    val lines = notes?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    if (lines.isEmpty()) return DeliveryNoteSections(null, null, null, null)

    val hospitals = LinkedHashSet<String>()
    val fees = ArrayList<String>()
    val plan = ArrayList<String>()
    val postpartum = ArrayList<String>()

    for (line in lines) {
        val segments = splitSegments(line)
        val placements = segments.map { place(it.text) }
        segments.forEach { segment -> hospitals += hospitalNames(segment.text) }

        var start = 0
        while (start < segments.size) {
            var end = start
            while (end + 1 < segments.size && placements[end + 1].home == placements[start].home) end++
            val text = buildString {
                append(placements[start].text)
                for (k in start + 1..end) {
                    append(segments[k].separatorBefore)
                    append(placements[k].text)
                }
            }
            listFor(placements[start].home, fees, plan, postpartum) += text
            start = end + 1
        }
    }

    return DeliveryNoteSections(
        hospital = hospitals.joinToString("\n").ifBlank { null },
        fees = fees.joinToString("\n").ifBlank { null },
        plan = plan.joinToString("\n").ifBlank { null },
        postpartum = postpartum.joinToString("\n").ifBlank { null },
    )
}

// ---------------------------------------------------------------- segmentation (Q1)

/** Coding unit after a source line has been split; [separatorBefore] is the raw text that preceded it. */
private data class Segment(val text: String, val separatorBefore: String)

/** A trimmed slice of the source line, kept with its bounds so separators survive verbatim. */
private data class Piece(val text: String, val start: Int, val end: Int)

private fun splitSegments(line: String): List<Segment> {
    val pieces = ArrayList<Piece>()
    var start = 0
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            c == ';' -> {
                pieceOf(line, start, i)?.let { pieces += it }
                start = i + 1
            }
            c == '.' && isSentenceEnd(line, i) -> {
                // the '.' belongs to the preceding segment, so a rejoin reproduces the text exactly
                pieceOf(line, start, i + 1)?.let { pieces += it }
                start = i + 1
            }
        }
        i++
    }
    pieceOf(line, start, line.length)?.let { pieces += it }

    return pieces.mapIndexed { index, piece ->
        val separator = if (index == 0) "" else line.substring(pieces[index - 1].end, piece.start)
        Segment(piece.text, separator)
    }
}

private fun pieceOf(line: String, from: Int, to: Int): Piece? {
    var start = from
    var end = to
    while (start < end && line[start].isWhitespace()) start++
    while (end > start && line[end - 1].isWhitespace()) end--
    return if (start < end) Piece(line.substring(start, end), start, end) else null
}

/** True at a '.' that closes a sentence: it is followed by a space, a capital letter, and no abbreviation. */
private fun isSentenceEnd(line: String, dot: Int): Boolean {
    var next = dot + 1
    if (next >= line.length || line[next] != ' ') return false
    while (next < line.length && line[next] == ' ') next++
    if (next >= line.length || !line[next].isUpperCase()) return false
    return !isAbbreviation(line, dot)
}

/** "Dr." / "e.g." style abbreviations never end a sentence, so they never split a line. */
private fun isAbbreviation(line: String, dot: Int): Boolean {
    val token = Regex("[\\p{L}.]+$").find(line.substring(0, dot))?.value?.lowercase() ?: return false
    return token in ABBREVIATIONS
}

private val ABBREVIATIONS =
    setOf("dr", "mr", "mrs", "ms", "st", "no", "approx", "vs", "e.g", "i.e")

// ---------------------------------------------------------------- classification (round 1 + Q3)

private enum class Label { HOSPITAL, PLAN, POSTPARTUM, FEE }

private enum class Home { FEES, PLAN, POSTPARTUM }

private data class Placement(val home: Home, val text: String)

private fun place(text: String): Placement {
    val label = knownLabel(text)
    val unconfirmedFee = UNCONFIRMED_FEE.containsMatchIn(text)
    return when {
        label == Label.POSTPARTUM -> Placement(Home.POSTPARTUM, stripLabel(text))
        label == Label.FEE -> if (unconfirmedFee) Placement(Home.PLAN, text) else Placement(Home.FEES, stripLabel(text))
        label != null -> Placement(Home.PLAN, text)
        HISTORY_MARKER.containsMatchIn(text) -> Placement(Home.PLAN, text)
        // Q3: "delivered"/"discharged" describe this pregnancy only without a history or a plan marker
        CURRENT_DELIVERY.containsMatchIn(text) && !PLAN_MARKER.containsMatchIn(text) ->
            Placement(Home.POSTPARTUM, text)
        FEE_KEYWORD.containsMatchIn(text) && !unconfirmedFee -> Placement(Home.FEES, text)
        else -> Placement(Home.PLAN, text)
    }
}

private fun listFor(home: Home, fees: MutableList<String>, plan: MutableList<String>, postpartum: MutableList<String>) =
    when (home) {
        Home.FEES -> fees
        Home.PLAN -> plan
        Home.POSTPARTUM -> postpartum
    }

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

/** Q3: a planned/future note is never a current postpartum record, even when it says "delivered". */
private val PLAN_MARKER = Regex(
    "\\b(will|shall|plan|planned|planning|expected|expect|booked|scheduled|if|should|would|may|aim|intend|intended)\\b" +
        "|\\bto\\s+be\\b",
    RegexOption.IGNORE_CASE,
)

// ---------------------------------------------------------------- hospital box (Q4)

private fun hospitalNames(text: String): List<String> {
    val labelled = labelledHospital(text)
    val phrase = phraseHospital(text)?.let(::dropLeadingWords)
    return listOfNotNull(labelled, phrase).distinct()
}

private val HOSPITAL_LABEL = Regex("^hospital$", RegexOption.IGNORE_CASE)

/** Q4(a): "Hospital: <name>", the name cut at ';', ',', '.' or " - "; undecided values give null. */
private fun labelledHospital(text: String): String? {
    val colon = text.indexOf(':')
    if (colon <= 0) return null
    if (!HOSPITAL_LABEL.matches(text.substring(0, colon).trim())) return null
    val value = text.substring(colon + 1)
    val cut = listOf(value.indexOf(';'), value.indexOf(','), value.indexOf('.'), value.indexOf(" - "))
        .filter { it >= 0 }
        .minOrNull()
    return acceptedName(if (cut == null) value else value.substring(0, cut))
}

private fun acceptedName(raw: String): String? {
    val name = raw.trim()
    if (name.isEmpty() || name.startsWith("?")) return null
    return if (UNDECIDED_HOSPITAL.containsMatchIn(name)) null else name
}

/** A value that merely restates "not decided" must never become a hospital name. */
private val UNDECIDED_HOSPITAL = Regex(
    "^(?:tbd|to\\s+be\\s+decided|not\\s+decided|undecided|not\\s+agreed|none)\\b",
    RegexOption.IGNORE_CASE,
)

/** A capitalised, whitespace-separated word (optionally carrying an apostrophe, ampersand or dot). */
private const val CAPITALISED_WORD = "[A-Z][\\p{L}'&.\\-]*"

/**
 * Q4(b): a capitalised "<Name> [Maternity|Medical] Hospital" phrase (capital H only). Lowercase
 * connector particles may sit between the capitalised words, and lowercase words end the phrase,
 * so "Full hospital stay" / "delivery at Cleopatra hospital" never become names.
 */
private val HOSPITAL_PHRASE = Regex(
    "(?<![\\p{L}])($CAPITALISED_WORD(?:\\s+(?:$CAPITALISED_WORD|al|el|de|of|and))*\\s+Hospital)(?![\\p{L}])",
)

/** Leading verbs/prepositions that are dropped from a detected phrase ("Delivery At X Hospital"). */
private val HOSPITAL_LEADING_WORDS = setOf(
    "at", "in", "to", "for", "from", "via", "booked", "book", "booking", "planned", "plan",
    "delivery", "deliver", "admit", "admitted", "admission", "refer", "referred", "transfer",
    "transferred", "prefers", "preferred", "patient", "agreed",
)

private fun phraseHospital(text: String): String? = HOSPITAL_PHRASE.find(text)?.groupValues?.get(1)?.trim()

private fun dropLeadingWords(phrase: String): String {
    val words = phrase.split(Regex("\\s+")).toMutableList()
    while (words.size > 1 && words.first().lowercase() in HOSPITAL_LEADING_WORDS) words.removeAt(0)
    return words.joinToString(" ")
}
