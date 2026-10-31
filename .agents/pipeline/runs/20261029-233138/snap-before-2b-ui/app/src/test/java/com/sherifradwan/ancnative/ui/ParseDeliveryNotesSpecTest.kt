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
 * Round 2 additions: Q1 (segmentation), Q2 (lossless rejoin, supersedes P5 line-level wording),
 * Q3 (tense, refines P2), Q4 (hospital, refines P4) from .agents/pipeline/SPEC_FIX_R2.md.
 * Round 2b additions: Q8-Q12 (history hospital, generic phrases, multi-phrase collection,
 * labelled-value cut/undecided "pending", lowercase sentence split) from the R2b addendum.
 *
 * SAFE assumptions (spec ambiguity handling):
 *  - Unknown/unclassifiable free-text lines must remain reachable in some section; never dropped.
 *  - Only a known label prefix may be stripped before ':'; known labels are the delivery-section
 *    labels ("Postpartum note:", "Delivery fee:", "Fees:", "Hospital:", "Plan:", "Postpartum:").
 *    Any other ':' text (e.g. times like "10:30") is data, not a label, and must survive intact.
 *  - Q2/D2: a hospital-labelled source line stays in its home section as the FULL line (label
 *    included), exactly as typed; the extracted name additionally goes to the hospital box.
 *  - Hospital box: distinct names in source order, joined with "\n" (Q4); each segment appears
 *    in exactly one home section (D2 - no line is listed twice).
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

    // =====================================================================
    // Round 2 oracles (Q1-Q4, .agents/pipeline/SPEC_FIX_R2.md). Q2 supersedes
    // P5 "line-level wording": same-line segments in one section are rejoined
    // with their ORIGINAL separator; different source lines join with "\n".
    // Each example below uses the exact expected strings from the spec.
    // No round-1 assertion needed changing: all of them survive Q1-Q4 as is.
    // =====================================================================

    // ---- Q1/Q2: splitting and lossless rejoin (spec examples) ----
    @Test fun r2_q2_unsplitSentenceEndSameSection_rejoinsExactlyAsTyped() {
        // Split at ". " + uppercase ("39w. Avoid") but both segments stay in plan ->
        // rejoined with the original separator: an unsplit line reads exactly as typed.
        val s = parseDeliveryNotes("Elective CS at 39w. Avoid NSAIDs.")
        assertEquals("Elective CS at 39w. Avoid NSAIDs.", s.plan)
        assertNull(s.fees)
        assertNull(s.postpartum)
        assertNull(s.hospital)
    }

    @Test fun r2_q2_semicolonSplit_sendsFeeAndHistoryToTheirHomes() {
        val s = parseDeliveryNotes("Fee 15,000 EGP; penicillin allergy - avoid")
        assertEquals("Fee 15,000 EGP", s.fees)
        assertEquals("penicillin allergy - avoid", s.plan)
    }

    @Test fun r2_q1_sentenceSplits_routeEachSegment_home() {
        val s = parseDeliveryNotes(
            "Elective CS at 39w at Cleopatra Hospital. Fee 20,000 EGP agreed. Postpartum: enoxaparin 10 days.",
        )
        assertEquals("Elective CS at 39w at Cleopatra Hospital.", s.plan)
        assertEquals("Fee 20,000 EGP agreed.", s.fees)
        assertEquals("enoxaparin 10 days.", s.postpartum)
        assertEquals("Cleopatra Hospital", s.hospital)
    }

    @Test fun r2_q1_drAbbreviation_neverSplits_decimalNeverSplits() {
        val s = parseDeliveryNotes("Dr. Ahmed to attend. Fee 2.5k EGP")
        assertEquals("Dr. Ahmed to attend.", s.plan)
        assertEquals("Fee 2.5k EGP", s.fees)
    }

    // ---- Q1: own boundary cases (abbreviations case-insensitive, decimals, times) ----
    @Test fun r2_q1_abbreviationCaseInsensitive_noSplitThenSentenceSplit() {
        val s = parseDeliveryNotes("mr. Sameh will call. Planned SVD at 39w.")
        assertEquals("mr. Sameh will call. Planned SVD at 39w.", s.plan)
        assertNull(s.fees)
        assertNull(s.postpartum)
    }

    @Test fun r2_q1_decimal_doseSameSection_rejoinsExact() {
        val s = parseDeliveryNotes("Dose 2.5 mg daily. No episodes.")
        assertEquals("Dose 2.5 mg daily. No episodes.", s.plan)
    }

    @Test fun r2_q1_timeColon_neverSplit_rejoinsExact() {
        val s = parseDeliveryNotes("Arrive 10:30 on Sunday. No food after midnight.")
        assertEquals("Arrive 10:30 on Sunday. No food after midnight.", s.plan)
    }

    // ---- Q3: tense decides unlabelled segments (refines P2) ----
    @Test fun r2_q3_futureTense_neverPostpartum() {
        val s = parseDeliveryNotes("Will be delivered by elective CS at 39w")
        assertEquals("Will be delivered by elective CS at 39w", s.plan)
        assertNull(s.postpartum)
        assertNull(s.fees)
        assertNull(s.hospital)
    }

    @Test fun r2_q3_expectedTense_neverPostpartum() {
        val s = parseDeliveryNotes("Expected to be discharged day 2 after CS")
        assertEquals("Expected to be discharged day 2 after CS", s.plan)
        assertNull(s.postpartum)
    }

    @Test fun r2_q3_currentDelivered_goesToPostpartum() {
        val s = parseDeliveryNotes("Delivered 12/10 by CS, baby well")
        assertEquals("Delivered 12/10 by CS, baby well", s.postpartum)
        assertNull(s.plan)
        assertNull(s.fees)
        assertNull(s.hospital)
    }

    @Test fun r2_q3_semicolon_dischargedToPostpartum_planMarkerToPlan() {
        val s = parseDeliveryNotes("Discharged day 3; plan review at 6 weeks")
        assertEquals("Discharged day 3", s.postpartum)
        assertEquals("plan review at 6 weeks", s.plan)
    }

    @Test fun r2_q3_labelAlwaysWins_overFutureTense() {
        val s = parseDeliveryNotes("Postpartum: will review at 6 weeks")
        assertEquals("will review at 6 weeks", s.postpartum)
        assertNull(s.plan)
    }

    // ---- Q4(a): "Hospital:" label; name cut at ";", ",", "." or " - "; undecided -> null ----
    @Test fun r2_q4a_hospitalLabel_nameExtracted_fullLineStaysInPlan() {
        val s = parseDeliveryNotes("Hospital: Dar Al Fouad")
        assertEquals("Dar Al Fouad", s.hospital)
        assertEquals("Hospital: Dar Al Fouad", s.plan)
    }

    @Test fun r2_q4a_labelCaseInsensitive_valueTrimmed_labelledSegmentSplit() {
        val s = parseDeliveryNotes("hospital: Dar Al Fouad; arrive fasting")
        assertEquals("Dar Al Fouad", s.hospital)
        assertTrue("the rest of the line stays reachable", s.joined().contains("arrive fasting"))
        assertTrue(s.joined().contains("Dar Al Fouad"))
    }

    @Test fun r2_q4a_undecidedTbd_hospitalNull_lineNotLost() {
        val s = parseDeliveryNotes("Hospital: TBD")
        assertNull(s.hospital)
        assertTrue(s.joined().contains("Hospital: TBD"))
    }

    @Test fun r2_q4a_undecidedNotDecided_hospitalNull_lineNotLost() {
        val s = parseDeliveryNotes("Hospital: not decided yet")
        assertNull(s.hospital)
        assertTrue(s.joined().contains("Hospital: not decided yet"))
    }

    // Q4(a) own boundaries: the name is cut at ".", " - ", and more undecided values.
    @Test fun r2_q4a_nameCutAtSentenceEnd() {
        val s = parseDeliveryNotes("Hospital: Dar Al Fouad. Arrive fasting")
        assertEquals("Dar Al Fouad", s.hospital)
        assertTrue(s.joined().contains("Arrive fasting"))
    }

    @Test fun r2_q4a_nameCutAtSpaceDash() {
        val s = parseDeliveryNotes("Hospital: Dar Al Fouad - fasting instructions")
        assertEquals("Dar Al Fouad", s.hospital)
        assertTrue(s.joined().contains("fasting instructions"))
    }

    @Test fun r2_q4a_undecidedQuestionMarkAndNone() {
        for (value in listOf("?", "none")) {
            val s = parseDeliveryNotes("Hospital: $value")
            assertNull("undecided value '$value' must not become a hospital name", s.hospital)
            assertTrue("source line must not be lost", s.joined().contains("Hospital: $value"))
        }
    }

    // ---- Q4(b): unlabelled "<Name> Hospital" phrases ----
    @Test fun r2_q4b_bookedPhrase_extractedExactly() {
        val s = parseDeliveryNotes("Booked Cleopatra Maternity Hospital")
        assertEquals("Cleopatra Maternity Hospital", s.hospital)
        assertTrue(s.joined().contains("Booked Cleopatra Maternity Hospital"))
    }

    @Test fun r2_q4b_leadingWordsDropped_nameOnly() {
        val s = parseDeliveryNotes("Delivery At Dar Al Fouad Hospital")
        assertEquals("Dar Al Fouad Hospital", s.hospital)
    }

    @Test fun r2_q4b_lowercaseConnectorAllowed_nameKeptAsTyped() {
        val s = parseDeliveryNotes("CS at Dar al Fouad Hospital")
        assertEquals("Dar al Fouad Hospital", s.hospital)
    }

    @Test fun r2_q4b_lowercaseHospitalWord_notAccepted() {
        val s = parseDeliveryNotes("Delivery at Cleopatra hospital")
        assertNull("lowercase 'hospital' unlabelled is not a name", s.hospital)
        assertEquals("Delivery at Cleopatra hospital", s.plan)
    }

    @Test fun r2_q4b_fullHospitalStay_neverAName() {
        val s = parseDeliveryNotes("Delivery fee package agreed: 15,000 EGP (Full hospital stay + pediatrician attendance).")
        assertNull("'(Full hospital stay ...' must not become a hospital name", s.hospital)
        assertTrue("the fee text must not be lost", s.joined().contains("15,000"))
        assertTrue(s.joined().contains("Full hospital stay"))
    }

    @Test fun r2_q4_round1Negatives_stillNull() {
        val cases = listOf(
            "Declines hospital birth, wants home delivery",
            "Hospitalised at 28w for monitoring",
            "39 weeks by dates",
            "Reached clinic 10:30, no hospital agreed yet",
        )
        for (line in cases) {
            val s = parseDeliveryNotes(line)
            assertNull("round-1 negative must stay null: $line", s.hospital)
            assertTrue("text must not be lost", s.joined().contains(line))
        }
    }

    // Q4 own boundaries: distinct names joined with "\n" in source order; duplicates deduped.
    @Test fun r2_q4_twoDistinctNames_joinedNewline_sourceOrder() {
        val s = parseDeliveryNotes(
            "Hospital: Dar Al Fouad\nBooked Cleopatra Maternity Hospital",
        )
        assertEquals("Dar Al Fouad\nCleopatra Maternity Hospital", s.hospital)
    }

    @Test fun r2_q4_sameNameTwice_deduped() {
        val s = parseDeliveryNotes("Hospital: Dar Al Fouad\nHospital: Dar Al Fouad")
        assertEquals("Dar Al Fouad", s.hospital)
    }

    // =====================================================================
    // Round 2b oracles (Q8-Q12, .agents/pipeline/SPEC_FIX_R2.md addendum).
    // Q1-Q7/U1-U6 hold unchanged; only the R2b behaviours are new here.
    // =====================================================================

    // ---- Q8: a hospital phrase inside a HISTORY segment is never this delivery's hospital ----
    @Test fun r2_q8_historyHospital_notExtracted() {
        val s = parseDeliveryNotes("Delivered previous baby at Al Salam Hospital (2019)")
        assertNull("history segment's hospital must not become this delivery's hospital", s.hospital)
        assertEquals("the history segment itself stays in plan, wording intact", "Delivered previous baby at Al Salam Hospital (2019)", s.plan)
    }

    @Test fun r2_q8_historyAndCurrentSegments_onlyCurrentNameExtracted() {
        val line = "Previous CS at Al Salam Hospital; booked Cleopatra Hospital"
        val s = parseDeliveryNotes(line)
        assertEquals("Cleopatra Hospital", s.hospital)
        assertFalse(
            "Al Salam Hospital belongs to history (P2 marker 'previous') and must not be collected",
            (s.hospital ?: "").contains("Al Salam"),
        )
        assertEquals(
            "both segments land in plan (history + future marker 'booked') and rejoin exactly as typed",
            line, s.plan,
        )
    }

    // ---- Q9: generic phrases are not names (no word beyond Maternity/Medical/Hospital, or whole word To/Go/If) ----
    @Test fun r2_q9_transportPhrases_areNotNames_linesStayInPlan() {
        val cases = listOf(
            "Transfer To Hospital if PPH",
            "Go To Hospital If Bleeding",
            "Referral To Hospital Urgently",
        )
        for (line in cases) {
            val s = parseDeliveryNotes(line)
            assertNull("generic To/Go/If phrase must not become a hospital name: $line", s.hospital)
            assertEquals("rejected phrase must stay in plan: $line", line, s.plan)
        }
    }

    @Test fun r2_q9_noRealNameBeyondGenerics_isRejected() {
        // SAFE boundary: "<generic> Hospital" with no proper noun carries no destination
        // information; showing it as a selected hospital would misfile this line.
        val s = parseDeliveryNotes("Maternity Hospital")
        assertNull(s.hospital)
        assertEquals("Maternity Hospital", s.plan)
    }

    // ---- Q10: EVERY hospital phrase in a segment is collected (distinct, source order, "\n") ----
    @Test fun r2_q10_multiplePhrasesInOneSegment_allCollectedInOrder() {
        val line = "Patient prefers Andalusia Smouha Hospital or Dar Al Fouad Hospital"
        val s = parseDeliveryNotes(line)
        assertEquals("Andalusia Smouha Hospital\nDar Al Fouad Hospital", s.hospital)
        assertEquals("the unlabelled segment stays in plan, wording intact", line, s.plan)
    }

    // ---- Q11: labelled value cut at "." honours Q1 abbreviations; "pending" is undecided; dedupe keeps the longer once ----
    @Test fun r2_q11_abbreviatedDotInsideName_notCut() {
        val line = "Hospital: St. Mary's Maternity Hospital"
        val s = parseDeliveryNotes(line)
        assertEquals("the '.' after abbreviation 'St' must not end the hospital value", "St. Mary's Maternity Hospital", s.hospital)
        assertEquals("the full source line stays in plan (D2)", line, s.plan)
    }

    @Test fun r2_q11_pendingIsUndecided_hospitalNull() {
        val s = parseDeliveryNotes("Hospital: pending")
        assertNull("'pending' joins the undecided list", s.hospital)
        assertTrue("the source line must not be lost", s.joined().contains("Hospital: pending"))
    }

    @Test fun r2_q11_undecidedListIsCaseInsensitive() {
        // SAFE assumption: 'pending' inherits the whole-word, case-insensitive matching of the Q4(a) undecided list.
        val s = parseDeliveryNotes("Hospital: Pending")
        assertNull(s.hospital)
        assertTrue(s.joined().contains("Hospital: Pending"))
    }

    @Test fun r2_q11_labelledNameAndPhraseEqual_dedupedOnce() {
        // The labelled value and the unlabelled phrase from the same segment are equal here;
        // Q11 keeps only the longer (the value), once - the hospital box must not repeat it.
        val s = parseDeliveryNotes("Hospital: Al Salam Hospital")
        assertEquals("Al Salam Hospital", s.hospital)
        assertFalse("name must appear exactly once, not once per source", (s.hospital ?: "").contains("Al Salam Hospital\n"))
    }

    // ---- Q12: sentence split also before a lowercase letter (abbreviation exceptions unchanged) ----
    @Test fun r2_q12_lowercaseSplit_sendsFeeToItsHome() {
        val s = parseDeliveryNotes("Fee 15,000 EGP. if bleeding call")
        assertEquals("Fee 15,000 EGP.", s.fees)
        assertEquals("if bleeding call", s.plan)
    }

    @Test fun r2_q12_egAbbreviation_stillDoesNotSplit() {
        val s = parseDeliveryNotes("e.g. CS if breech. Otherwise SVD.")
        assertEquals("e.g. CS if breech. Otherwise SVD.", s.plan)
    }

    @Test fun r2_q12_lowercaseSplitSameSections_rejoinedExact() {
        // Split at ". i" (lowercase), both segments remain in plan -> one source line
        // re-joined with its original separator reads exactly as typed (Q2).
        val s = parseDeliveryNotes("Review Tuesday. if spotting call")
        assertEquals("Review Tuesday. if spotting call", s.plan)
        assertNull(s.fees)
    }

    @Test fun r2_q12_lowercaseSplit_rescuesDeliveredSegmentFromPlan() {
        // Without the lowercase split the whole line carries the future marker "will" and
        // misroutes a delivered note into plan; with Q12 the delivered part goes to postpartum.
        val s = parseDeliveryNotes("Delivered day 2, baby well. will review at 6 weeks")
        assertEquals("Delivered day 2, baby well.", s.postpartum)
        assertEquals("will review at 6 weeks", s.plan)
        assertNull(s.fees)
        assertNull(s.hospital)
    }
}
