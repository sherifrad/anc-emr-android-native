package com.sherifradwan.ancnative.ui.patienthub.components

import com.sherifradwan.ancnative.model.SevereBpAttention

data class AttentionEntry(val level: ClinicalAttentionLevel, val text: String)

const val ATTENTION_TEXT_MAX = 140
const val ATTENTION_ENTRIES_MAX = 10

fun parseAttentionEntries(raw: String?): List<AttentionEntry> {
    if (raw.isNullOrBlank()) return emptyList()
    val result = mutableListOf<AttentionEntry>()
    var currentLevel: ClinicalAttentionLevel? = null

    try {
        for (rawLine in raw.lines()) {
            val lineWithoutBullet = stripBullet(rawLine)
            if (lineWithoutBullet.isBlank()) continue

            val match = matchPrefix(lineWithoutBullet)
            val level: ClinicalAttentionLevel
            val cleanText: String

            if (match != null) {
                level = match.level
                cleanText = stripBullet(match.remainder)
            } else {
                level = currentLevel ?: ClinicalAttentionLevel.ALERT
                cleanText = lineWithoutBullet
            }

            if (cleanText.isNotBlank()) {
                currentLevel = level
                result.add(AttentionEntry(level = level, text = cleanText))
            }
        }
    } catch (_: Throwable) {
        return emptyList()
    }
    return result
}

fun composeAttentionEntries(entries: List<AttentionEntry>): String {
    val valid = entries
        .filter { it.text.isNotBlank() }
        .take(ATTENTION_ENTRIES_MAX)
    if (valid.isEmpty()) return ""
    return valid.joinToString("\n") { entry ->
        val text = entry.text.trim().take(ATTENTION_TEXT_MAX)
        "${entry.level.name}: $text"
    }
}

fun sortedForDisplay(entries: List<AttentionEntry>): List<AttentionEntry> {
    return entries.sortedWith(compareBy { entry ->
        when (entry.level) {
            ClinicalAttentionLevel.CRITICAL -> 0
            ClinicalAttentionLevel.ALERT -> 1
            ClinicalAttentionLevel.NOTE -> 2
        }
    })
}

fun clinicalAttentionLevel(
    attention: String?,
    severeBp: SevereBpAttention?,
): ClinicalAttentionLevel {
    if (severeBp != null) return ClinicalAttentionLevel.CRITICAL
    val entries = parseAttentionEntries(attention)
    if (entries.isEmpty()) return ClinicalAttentionLevel.NOTE
    return when {
        entries.any { it.level == ClinicalAttentionLevel.CRITICAL } -> ClinicalAttentionLevel.CRITICAL
        entries.any { it.level == ClinicalAttentionLevel.ALERT } -> ClinicalAttentionLevel.ALERT
        else -> ClinicalAttentionLevel.NOTE
    }
}

private data class PrefixMatch(val level: ClinicalAttentionLevel, val remainder: String)

private fun matchPrefix(line: String): PrefixMatch? {
    val candidates = listOf(
        "[CRITICAL ALERT]:" to ClinicalAttentionLevel.CRITICAL,
        "[CRITICAL ALERT]" to ClinicalAttentionLevel.CRITICAL,
        "CRITICAL ALERT:" to ClinicalAttentionLevel.CRITICAL,
        "[CRITICAL]:" to ClinicalAttentionLevel.CRITICAL,
        "[CRITICAL]" to ClinicalAttentionLevel.CRITICAL,
        "CRITICAL:" to ClinicalAttentionLevel.CRITICAL,
        "[ALERT]:" to ClinicalAttentionLevel.ALERT,
        "[ALERT]" to ClinicalAttentionLevel.ALERT,
        "ALERT:" to ClinicalAttentionLevel.ALERT,
        "[WARNING]:" to ClinicalAttentionLevel.ALERT,
        "[WARNING]" to ClinicalAttentionLevel.ALERT,
        "WARNING:" to ClinicalAttentionLevel.ALERT,
        "[NOTE]:" to ClinicalAttentionLevel.NOTE,
        "[NOTE]" to ClinicalAttentionLevel.NOTE,
        "NOTE:" to ClinicalAttentionLevel.NOTE,
    )
    for ((prefix, level) in candidates) {
        if (line.startsWith(prefix, ignoreCase = true)) {
            val rem = line.substring(prefix.length)
            return PrefixMatch(level, rem)
        }
    }
    return null
}

private fun stripBullet(text: String): String {
    var t = text.trim()
    while (t.isNotEmpty() && (t.startsWith('•') || t.startsWith('-') || t.startsWith('*'))) {
        t = t.substring(1).trim()
    }
    return t
}
