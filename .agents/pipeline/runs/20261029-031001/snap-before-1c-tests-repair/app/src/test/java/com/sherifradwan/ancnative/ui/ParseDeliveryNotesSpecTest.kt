package com.sherifradwan.ancnative.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec-derived tests (P1-P5, from .agents/pipeline/SPEC_FIX.md) for parseDeliveryNotes.
 * Written from the spec only (red phase: production code may not satisfy them yet).
 *
 * SAFE assumptions (spec ambiguity handling):
 *  - Unknown/unclassifiable free-text lines must remain reachable in some section; never dropped.
 *  - Only a known label prefix may be stripped before ':'; known labels are the delivery-section
 *    labels ("Postpartum note:", "Delivery fee:", "Fees:", "Hospital:", "Plan:", "Postpartum:").
 *    Any other ':' text (e.g. times like "10:30") is data, not a label, and must survive intact.
 *  - A non-hospital-bearing input line appears in exactly one section; a hospital-bearing line may
 *    appear in max two places (hospital box + its home section), pending owner confirmation.
 */
class ParseDeliveryNotesSpecTest {

    private fun DeliveryNoteSections.all(): List<String?> = listOf(hospital, fees, plan, postpartum)
    private fun DeliveryNoteSections.joined(): String = all().filterNotNull().joinToString("\n")
    private fun DeliveryNoteSections.countOf(line: String): Int =
        all().filterNotNull().sumOf { sec -> sec.lines().count { it.contains(line.trim()) } }

    /**
     * P3 allows stripping a known label prefix before ':'. Losslessness is therefore judged on the
     * line itself OR the line with such a prefix stripped (label names may sit in the box header).
     */
    private fun DeliveryNoteSections.countOfAllowingLabelStrip(line: String): Int {
        val t = line.trim()
        val stripped = KNOWN_LABELS.firstOrNull { t.startsWith(it, ignoreCase = true) && t.length > it.length }
            ?.let { t.substringAfter(":", t).trim() } ?: t
        val variants = listOf(t, stripped).filter { it.isNotBlank() }.distinct()
        return all().filterNotNull().sumOf { sec ->
            sec.lines().count { secLine -> variants.any { secLine.contains(it) } }
        }
    }

    private val KNOWN_LABELS = listOf(
        "postpartum note:",
        "delivery fee:",
        "fees:",
        "fee:",
        "hospital:",
        "plan:",
        "postpartum:",
    )

    private fun assertAllNull(s: DeliveryNoteSections) {
        assertNull(s.hospital); assertNull(s.fees); assertNull(s.plan); assertNull(s.postpartum)
    }

    // ---- P5: null / blank ----
    @Test fun nullNotes_allNull() = assertAllNull(parseDeliveryNotes(null))
    @Test fun emptyNotes_allNull() = assertAllNull(parseDeliveryNotes(""))
    @Test fun blankNotes_allNull() = assertAllNull(parseDeliveryNotes("   "))
    @Test fun whitespaceOnlyMultiline_allNull() = assertAllNull(parseDeliveryNotes(" \n\t\n  \r\n"))

    // ---- P5: lossless; hospital-bearing lines may appear at most twice ----
    @Test fun everyNonEmptyLine_appears_atLeastOnce_othersExactlyOnce() {
        val hospitalLine = "Plan: deliver at Cleopatra Maternity Hospital"
        val lines = listOf(
            hospitalLine,
            "Fees: 15,000 EGP agreed package",
            "Plan: elective CS booked",
            "Postpartum note: start iron, review at 6 weeks",
            "Patient prefers morning appointments",   // unclassifiable
            "Husband will accompany her",             // unclassifiable
        )
        val s = parseDeliveryNotes(lines.joinToString("\n\n  \n"))
        for (l in lines) {
            val n = s.countOfAllowingLabelStrip(l)
            if (l == hospitalLine) {
                assertTrue("hospital-bearing line must appear (may repeat max twice): $l", n in 1..2)
            } else {
                assertEquals("line must appear exactly once: $l", 1, n)
            }
        }
    }

    @Test fun hospitalAndFeeInOneLine_visible_andOnceAtMostTwice() {
        val line = "Fee: 30000 EGP agreed at Dar Al Fouad Hospital"
        val s = parseDeliveryNotes(line)
        val n = s.countOfAllowingLabelStrip(line)
        assertTrue("combined line must survive (may repeat max twice): n=$n", n in 1..2)
        assertTrue("hospital name must still be reachable", s.joined().contains("Dar Al Fouad Hospital"))
        assertTrue("fee amount must still be reachable", s.joined().contains("30000"))
    }

    @Test fun unclassifiedLine_isNotDropped() {
        val s = parseDeliveryNotes("Allergic to penicillin")
        assertTrue(s.joined().contains("Allergic to penicillin"))
    }

    @Test fun multiLine_orderPreservedInsideSection() {
        val s = parseDeliveryNotes("Plan: step one induction\nPlan: step two epidural\nPlan: step three monitoring")
        val p = s.plan ?: s.joined()
        val i1 = p.indexOf("step one"); val i2 = p.indexOf("step two"); val i3 = p.indexOf("step three")
        assertTrue(i1 >= 0 && i1 < i2 && i2 < i3)
    }

    @Test fun crlfAndBlankLines_doNotLoseOrDuplicate() {
        val s = parseDeliveryNotes("Plan: induction\r\n\r\nPostpartum note: contraception counselling\r\n")
        assertEquals(1, s.countOfAllowingLabelStrip("Plan: induction"))
        assertEquals(1, s.countOfAllowingLabelStrip("Postpartum note: contraception counselling"))
    }

    @Test fun nothingIsInvented_wordsCameFromInput() {
        val input = "Plan: elective CS at 6 weeks\nPostpartum note: iron tablets started"
        val s = parseDeliveryNotes(input)
        val inputWords = Regex("\\w+").findAll(input.lowercase()).map { it.value }.toSet()
        val outWords = Regex("\\w+").findAll(s.joined().lowercase()).map { it.value }.toSet()
        val invented = outWords - inputWords
        assertTrue("sections must not invent text; invented=$invented", invented.isEmpty())
    }

    @Test fun surroundingWhitespace_doesNotCreateEmptyStringSections() {
        val s = parseDeliveryNotes("\n\n  Plan: induction  \n\n")
        for (sec in s.all()) if (sec != null) assertTrue("no blank non-null sections", sec.isNotBlank())
    }

    // ---- P1: whole-word keyword matching only ----
    @Test fun reducedFetalMovements_neverGoesToFees() {
        val s = parseDeliveryNotes("Feels reduced fetal movements, CTG booked")
        assertNull("substring 'fee' must not misroute free text to fees", s.fees)
        assertTrue(s.joined().contains("reduced fetal movements"))
    }

    @Test fun exclusivelyBreastfeed_neverGoesToFees() {
        val s = parseDeliveryNotes("Wishes to exclusively breastfeed")
        assertNull("'breastfeed' contains 'fee' but is not the fee keyword", s.fees)
        assertTrue(s.joined().contains("exclusively breastfeed"))
    }

    @Test fun confirmedFeeSentence_goesToFees() {
        val s = parseDeliveryNotes("Delivery fee: 25000 EGP package agreed")
        assertTrue("confirmed fee must land under Agreed Delivery Fees", s.fees?.contains("25000") == true)
    }

    // ---- P3: only a known label prefix may be stripped; times untouched ----
    @Test fun deliveryFeeLabelStripped_valueKept() {
        val s = parseDeliveryNotes("Delivery fee: 25000 EGP agreed")
        assertTrue(s.fees?.contains("25000 EGP") == true)
        assertFalse((s.fees ?: "").contains("Delivery fee:"))
    }

    @Test fun postpartumNoteLabelStripped_valueKept() {
        val s = parseDeliveryNotes("Postpartum note: rest, hydration, review in 2 weeks")
        val post = (s.postpartum ?: "") + (s.plan ?: "")
        assertTrue("value must survive label stripping", post.contains("rest, hydration, review in 2 weeks"))
        assertFalse("known label must not be duplicated into the value", post.contains("Postpartum note:"))
    }

    @Test fun timeColon_lineUntouched() {
        val s = parseDeliveryNotes("Lab review booked: 10:30")
        assertTrue(s.joined().contains("Lab review booked: 10:30"))
        assertTrue(s.joined().contains("10:30"))
    }

    @Test fun colonAfterTime_isNotTreatedAsLabelBoundary() {
        val s = parseDeliveryNotes("At 10:30 the couple confirmed fee: 20000 EGP")
        val fees = (s.fees ?: "").plus(" ").plus(s.plan ?: "")
        assertTrue("10:30 is data, not part of a label; it must survive", s.joined().contains("10:30"))
        assertTrue("non-label text before ':' must not be stripped", fees.contains("confirmed"))
    }

    @Test fun unknownLabelPrefix_notStripped() {
        val line = "Timing: reaches ward 10:30"
        val s = parseDeliveryNotes(line)
        assertTrue("unknown label is not a known label; text stays intact", s.joined().contains(line))
    }

    // ---- P3: unconfirmed fee wording stays in plan, never under "Agreed Delivery Fees" ----
    @Test fun unconfirmedFeeWording_neverUnderAgreedFees() {
        val cases = listOf(
            "Fee quoted: ~30,000 EGP" to "30,000",
            "Delivery fee: 25,000 EGP (estimate)" to "25,000",
            "Proposed fee: 35,000 EGP" to "35,000",
            "Fees discussed: 45,000 EGP" to "45,000",
            "Delivery fee 22,000 EGP still undecided" to "22,000",
            "Fee \"about 18,000 EGP\" not agreed" to "18,000",
        )
        for ((line, amount) in cases) {
            val s = parseDeliveryNotes(line)
            assertNull("unconfirmed fee must not sit under 'Agreed Delivery Fees': $line", s.fees)
            assertTrue("unconfirmed fee wording must stay in plan: $line", s.plan?.contains(amount) == true)
            assertTrue("and must not be lost", s.joined().contains(amount))
        }
    }

    // ---- P2: postpartum only takes lines about THIS pregnancy ----
    @Test fun currentDeliveredLine_goesToPostpartum() {
        val s = parseDeliveryNotes("Delivered healthy girl on 2026-10-12")
        assertTrue(s.postpartum?.contains("Delivered healthy girl") == true)
    }

    @Test fun labeledPostpartumLine_goesToPostpartum() {
        val s = parseDeliveryNotes("Postpartum note: feeding well, iron started")
        assertTrue(s.postpartum?.contains("feeding well") == true)
    }

    @Test fun historyLines_goToPlan_wordingIntact() {
        val cases = listOf(
            "previous" to "Delivered by emergency CS in 2019, previous baby 4kg",
            "prior" to "Prior discharge complicated by wound infection",
            "history" to "Delivery history: one CS at 37 weeks",
            "earlier" to "Earlier pregnancy delivered a stillbirth",
            "pregnancy" to "Her last pregnancy was discharged at day 2",
            "G5P3" to "G5P3 delivered vaginally in 2021 uneventful",
        )
        for ((marker, line) in cases) {
            val s = parseDeliveryNotes(line)
            assertTrue(
                "history line (marker: $marker) must go to plan with wording intact: $line",
                s.plan?.contains(line) == true,
            )
            assertFalse(
                "history must never show as current postpartum note (marker: $marker)",
                (s.postpartum ?: "").contains(line),
            )
            assertTrue("and must not be lost", s.joined().contains(line))
        }
    }

    @Test fun previousDeliveryArabic_notUnderPostpartum() {
        val line = "ولادة سابقة قيصرية عام 2019"
        val s = parseDeliveryNotes(line)
        assertFalse("history must not be shown as current postpartum note", (s.postpartum ?: "").contains(line))
        assertTrue("must still be reachable", s.joined().contains(line))
    }

    // ---- P4: hospital is only a "<Name> Hospital" phrase, searched on ALL lines ----
    @Test fun hospitalLine_returnedAsHospital() {
        val s = parseDeliveryNotes("Hospital: Cairo Maternity Hospital")
        assertTrue((s.hospital ?: "").contains("Cairo Maternity Hospital"))
    }

    @Test fun hospitalOnFeeLine_extracted_withoutLosingFee() {
        val s = parseDeliveryNotes("Fee: 25000 EGP agreed at Dar Al Shifa Hospital")
        assertTrue("hospital must be searched on fee lines too", s.hospital?.contains("Dar Al Shifa Hospital") == true)
        assertTrue("the fee must still be under fees", s.fees?.contains("25000") == true)
    }

    @Test fun hospitalPhraseOnly_neverWholeSentence() {
        val s = parseDeliveryNotes("Plan: deliver at Nile Badrawi Hospital, follow up weekly")
        val h = s.hospital ?: ""
        assertTrue(h.contains("Nile Badrawi Hospital"))
        assertFalse("never return the whole sentence", h.contains("deliver"))
        assertFalse(h.contains("follow up"))
        assertFalse(h.contains("weekly"))
    }

    @Test fun multipleAtOccurrences_phraseExtracted_notTimesOrWeeks() {
        val s = parseDeliveryNotes("Fee: 30000 EGP agreed at 7 months, deliver at Helios Medical Hospital at 38 weeks")
        val h = s.hospital ?: ""
        assertTrue("hospital phrase must be found despite multiple ' at '", h.contains("Helios Medical Hospital"))
        assertFalse("weeks must never end up in the hospital box", h.contains("38 weeks"))
        assertFalse("fee prefix must never end up in the hospital box", h.contains("Fee"))
        assertFalse("unrelated qualifiers must never end up in the hospital box", h.contains("7 months"))
        assertTrue("the line itself is not lost", s.joined().contains("38 weeks"))
    }

    @Test fun detectedHospital_neverContainsTime() {
        val s = parseDeliveryNotes("Admitted 10:30, option is Badrawi Hospital")
        val h = s.hospital ?: ""
        assertFalse("never return a time as hospital", h.contains("10:30"))
        assertTrue("hospital phrase on a line that also has a time", h.contains("Badrawi Hospital"))
    }

    @Test fun declinesHospitalBirth_hospitalNull() {
        val s = parseDeliveryNotes("Declines hospital birth, wants home delivery")
        assertNull("'<Name> Hospital' phrase absent -> no hospital", s.hospital)
        assertTrue("text is not lost", s.joined().contains("Declines hospital birth"))
    }

    @Test fun hospitalisedAt28w_hospitalNull() {
        val s = parseDeliveryNotes("Hospitalised at 28w for monitoring")
        assertNull("'Hospitalised' is not '<Name> Hospital'", s.hospital)
        assertTrue("text is not lost", s.joined().contains("Hospitalised at 28w"))
    }

    @Test fun tenThirtyMentionWithoutHospitalPhrase_hospitalNull() {
        val s = parseDeliveryNotes("Reached clinic 10:30, no agreed hospital yet")
        assertNull(s.hospital)
        assertTrue(s.joined().contains("10:30"))
    }

    @Test fun feeOnlyLine_isNotHospital() {
        val s = parseDeliveryNotes("Fees: 25000 EGP, deposit paid")
        assertNull(s.hospital)
        assertTrue(s.joined().contains("25000"))
    }

    @Test fun clinicalSentence_isNotHospital() {
        val s = parseDeliveryNotes("Severe preeclampsia, BP 160/110, admit for steroids")
        assertNull(s.hospital)
        assertTrue(s.joined().contains("preeclampsia"))
    }

    @Test fun noHospitalMentioned_hospitalNull() {
        val s = parseDeliveryNotes("Plan: induce at 40 weeks")
        assertNull(s.hospital)
    }

    // ---- keyword robustness (labels are English; app is English-only) ----
    @Test fun mixedCaseKeywords_stillClassified() {
        val s = parseDeliveryNotes(
            "HOSPITAL: Nile Badrawi Hospital\nfEeS: 20,000 EGP agreed\nPLAN: elective CS\nPOSTPARTUM NOTE: review at 2 weeks",
        )
        assertTrue((s.hospital ?: "").contains("Nile Badrawi Hospital"))
        assertTrue((s.fees ?: "").contains("20,000"))
        assertTrue((s.plan ?: "").contains("elective CS"))
        assertTrue(((s.postpartum ?: "").plus(" ").plus(s.plan ?: "")).contains("review at 2 weeks"))
    }

    @Test fun arabicLine_notDropped() {
        // Arabic keyword support is unspecified; SAFE: never dropped, never duplicated.
        val line = "المستشفى: دار الفؤاد - الرسوم 30000 جنيه"
        val s = parseDeliveryNotes(line)
        assertTrue(s.joined().contains(line))
    }
}
