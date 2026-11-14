package com.sherifradwan.ancnative.ui.patienthub.components

import com.sherifradwan.ancnative.model.SevereBpAttention
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalAttentionEntriesTest {

    @Test
    fun parseOldDialogAlertPrefixWithBullets() {
        val entries = parseAttentionEntries("[ALERT] Mild anemia\n• Calcium missed")
        assertEquals(2, entries.size)
        assertEquals(ClinicalAttentionLevel.ALERT, entries[0].level)
        assertEquals("Mild anemia", entries[0].text)
        assertEquals(ClinicalAttentionLevel.ALERT, entries[1].level)
        assertEquals("Calcium missed", entries[1].text)
    }

    @Test
    fun parseLineWithoutPrefixAfterNoteInheritsNote() {
        val entries = parseAttentionEntries("NOTE: Prefers female doctor\nFollow up next week")
        assertEquals(2, entries.size)
        assertEquals(ClinicalAttentionLevel.NOTE, entries[0].level)
        assertEquals("Prefers female doctor", entries[0].text)
        assertEquals(ClinicalAttentionLevel.NOTE, entries[1].level)
        assertEquals("Follow up next week", entries[1].text)
    }

    @Test
    fun parseFirstLineWithoutPrefixIsAlert() {
        val entries = parseAttentionEntries("Free text attention")
        assertEquals(1, entries.size)
        assertEquals(ClinicalAttentionLevel.ALERT, entries[0].level)
        assertEquals("Free text attention", entries[0].text)
    }

    @Test
    fun parsePrefixesInAnyCaseAndWarningAlias() {
        val raw = "critical: severe\n[alert] watch\nwarning: caution\n[note] info\nCrItIcAl: mixed\nWaRnInG: mixed warning"
        val entries = parseAttentionEntries(raw)
        assertEquals(6, entries.size)
        assertEquals(ClinicalAttentionLevel.CRITICAL, entries[0].level)
        assertEquals("severe", entries[0].text)
        assertEquals(ClinicalAttentionLevel.ALERT, entries[1].level)
        assertEquals("watch", entries[1].text)
        assertEquals(ClinicalAttentionLevel.ALERT, entries[2].level)
        assertEquals("caution", entries[2].text)
        assertEquals(ClinicalAttentionLevel.NOTE, entries[3].level)
        assertEquals("info", entries[3].text)
        assertEquals(ClinicalAttentionLevel.CRITICAL, entries[4].level)
        assertEquals("mixed", entries[4].text)
        assertEquals(ClinicalAttentionLevel.ALERT, entries[5].level)
        assertEquals("mixed warning", entries[5].text)
    }

    @Test
    fun parseSkipsBlankAndWhitespaceOnlyLines() {
        val entries = parseAttentionEntries("ALERT: first\n\n   \n\t\nNOTE: second")
        assertEquals(2, entries.size)
        assertEquals(ClinicalAttentionLevel.ALERT, entries[0].level)
        assertEquals("first", entries[0].text)
        assertEquals(ClinicalAttentionLevel.NOTE, entries[1].level)
        assertEquals("second", entries[1].text)
    }

    @Test
    fun parseNullAndEmptyInputGiveEmptyList() {
        assertTrue(parseAttentionEntries(null).isEmpty())
        assertTrue(parseAttentionEntries("").isEmpty())
    }

    @Test
    fun composeOutputFormat() {
        val result = composeAttentionEntries(
            listOf(
                AttentionEntry(ClinicalAttentionLevel.CRITICAL, "a"),
                AttentionEntry(ClinicalAttentionLevel.NOTE, "b"),
            )
        )
        assertEquals("CRITICAL: a\nNOTE: b", result)
    }

    @Test
    fun composeDropsBlankTextEntries() {
        val result = composeAttentionEntries(
            listOf(
                AttentionEntry(ClinicalAttentionLevel.ALERT, "keep"),
                AttentionEntry(ClinicalAttentionLevel.NOTE, ""),
                AttentionEntry(ClinicalAttentionLevel.CRITICAL, "   "),
                AttentionEntry(ClinicalAttentionLevel.NOTE, "also keep"),
            )
        )
        assertEquals("ALERT: keep\nNOTE: also keep", result)
    }

    @Test
    fun composeCutsTextToMaxLength() {
        val longText = "x".repeat(ATTENTION_TEXT_MAX + 20)
        val result = composeAttentionEntries(
            listOf(AttentionEntry(ClinicalAttentionLevel.ALERT, longText))
        )
        assertEquals("ALERT: " + "x".repeat(ATTENTION_TEXT_MAX), result)
        val parsed = parseAttentionEntries(result)
        assertEquals(1, parsed.size)
        assertEquals(ATTENTION_TEXT_MAX, parsed[0].text.length)
    }

    @Test
    fun composeKeepsAtMostMaxEntries() {
        val entries = (1..ATTENTION_ENTRIES_MAX + 3).map { i ->
            AttentionEntry(ClinicalAttentionLevel.ALERT, "entry $i")
        }
        val result = composeAttentionEntries(entries)
        val parsed = parseAttentionEntries(result)
        assertEquals(ATTENTION_ENTRIES_MAX, parsed.size)
        assertEquals("entry 1", parsed[0].text)
        assertEquals("entry ${ATTENTION_ENTRIES_MAX}", parsed[ATTENTION_ENTRIES_MAX - 1].text)
    }

    @Test
    fun composeReturnsEmptyForNothing() {
        assertEquals("", composeAttentionEntries(emptyList()))
        assertEquals(
            "",
            composeAttentionEntries(
                listOf(
                    AttentionEntry(ClinicalAttentionLevel.ALERT, ""),
                    AttentionEntry(ClinicalAttentionLevel.NOTE, "   "),
                )
            )
        )
    }

    @Test
    fun roundTripStabilityForSeveralInputs() {
        val inputs = listOf(
            "[ALERT] Mild anemia\n• Calcium missed",
            "NOTE: Prefers female doctor\nCRITICAL: Severe PIH",
            "critical: severe\n[alert] watch\nwarning: caution\n[note] info",
            "Free text attention\nSecond line",
            "ALERT: one\n\nNOTE: two\n   \nCRITICAL: three",
            "   leading spaces trimmed   ",
        )
        for (input in inputs) {
            val once = composeAttentionEntries(parseAttentionEntries(input))
            val twice = composeAttentionEntries(parseAttentionEntries(once))
            assertEquals("Round trip not stable for: $input", once, twice)
        }
    }

    @Test
    fun sortedForDisplayOrderAndStability() {
        val entries = listOf(
            AttentionEntry(ClinicalAttentionLevel.NOTE, "note1"),
            AttentionEntry(ClinicalAttentionLevel.ALERT, "alert1"),
            AttentionEntry(ClinicalAttentionLevel.CRITICAL, "critical1"),
            AttentionEntry(ClinicalAttentionLevel.NOTE, "note2"),
            AttentionEntry(ClinicalAttentionLevel.ALERT, "alert2"),
            AttentionEntry(ClinicalAttentionLevel.CRITICAL, "critical2"),
        )
        val sorted = sortedForDisplay(entries)
        assertEquals(
            listOf(
                AttentionEntry(ClinicalAttentionLevel.CRITICAL, "critical1"),
                AttentionEntry(ClinicalAttentionLevel.CRITICAL, "critical2"),
                AttentionEntry(ClinicalAttentionLevel.ALERT, "alert1"),
                AttentionEntry(ClinicalAttentionLevel.ALERT, "alert2"),
                AttentionEntry(ClinicalAttentionLevel.NOTE, "note1"),
                AttentionEntry(ClinicalAttentionLevel.NOTE, "note2"),
            ),
            sorted
        )
    }

    @Test
    fun sortedForDisplayExampleFromSpec() {
        val raw = "NOTE: Prefers female doctor\nCRITICAL: Severe PIH"
        val parsed = parseAttentionEntries(raw)
        assertEquals(
            listOf(
                AttentionEntry(ClinicalAttentionLevel.NOTE, "Prefers female doctor"),
                AttentionEntry(ClinicalAttentionLevel.CRITICAL, "Severe PIH"),
            ),
            parsed
        )
        val sorted = sortedForDisplay(parsed)
        assertEquals(
            listOf(
                AttentionEntry(ClinicalAttentionLevel.CRITICAL, "Severe PIH"),
                AttentionEntry(ClinicalAttentionLevel.NOTE, "Prefers female doctor"),
            ),
            sorted
        )
    }

    @Test
    fun clinicalAttentionLevelWithSevereBpIsCriticalEvenWhenTextIsNote() {
        val severeBp = SevereBpAttention(bloodPressure = "165/105", date = "2026-11-10")
        assertEquals(
            ClinicalAttentionLevel.CRITICAL,
            clinicalAttentionLevel("NOTE: x", severeBp)
        )
    }

    @Test
    fun clinicalAttentionLevelWithOnlyNotesIsNote() {
        assertEquals(
            ClinicalAttentionLevel.NOTE,
            clinicalAttentionLevel("NOTE: Prefers female doctor\nNOTE: Another note", null)
        )
    }

    @Test
    fun clinicalAttentionLevelMixedHighestWins() {
        assertEquals(
            ClinicalAttentionLevel.CRITICAL,
            clinicalAttentionLevel("NOTE: Prefers female doctor\nCRITICAL: Severe PIH", null)
        )
        assertEquals(
            ClinicalAttentionLevel.ALERT,
            clinicalAttentionLevel("NOTE: info\nALERT: watch", null)
        )
    }

    @Test
    fun clinicalAttentionLevelBlankIsNote() {
        assertEquals(ClinicalAttentionLevel.NOTE, clinicalAttentionLevel("", null))
        assertEquals(ClinicalAttentionLevel.NOTE, clinicalAttentionLevel("   \n  ", null))
        assertEquals(ClinicalAttentionLevel.NOTE, clinicalAttentionLevel(null, null))
    }

    @Test
    fun clinicalAttentionLevelNoPrefixIsAlert() {
        assertEquals(
            ClinicalAttentionLevel.ALERT,
            clinicalAttentionLevel("Free text attention", null)
        )
    }
}
