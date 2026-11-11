package com.sherifradwan.ancnative.model

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryNotesComposeTest {

    @Test
    fun composeOfAllNullSectionsIsEmptyString() {
        assertEquals("", composeDeliveryNotes(DeliveryNoteSections(null, null, null, null)))
    }

    @Test
    fun composeOfASingleFieldWritesOneLabelledLineForThatBox() {
        assertEquals(
            "Agreed Delivery Hospital: Cleopatra Hospital",
            composeDeliveryNotes(DeliveryNoteSections("Cleopatra Hospital", null, null, null)),
        )
        assertEquals(
            "Agreed Delivery Fees: 20000 EGP",
            composeDeliveryNotes(DeliveryNoteSections(null, "20000 EGP", null, null)),
        )
        assertEquals(
            "Delivery Plan: SVD",
            composeDeliveryNotes(DeliveryNoteSections(null, null, "SVD", null)),
        )
        assertEquals(
            "Postpartum Note: IUD at 6 weeks",
            composeDeliveryNotes(DeliveryNoteSections(null, null, null, "IUD at 6 weeks")),
        )
    }

    @Test
    fun composeWritesOneLabelledLinePerValueLineInValueOrder() {
        val s = DeliveryNoteSections(
            hospital = "Cleopatra Hospital\nAl Salam Hospital",
            fees = "20000 EGP\n3000 EGP anesthesia",
            plan = "SVD\nReview in 2 weeks",
            postpartum = "IUD at 6 weeks\nReview at 6 weeks",
        )
        assertEquals(
            "Agreed Delivery Hospital: Cleopatra Hospital\n" +
                "Agreed Delivery Hospital: Al Salam Hospital\n" +
                "Agreed Delivery Fees: 20000 EGP\n" +
                "Agreed Delivery Fees: 3000 EGP anesthesia\n" +
                "Delivery Plan: SVD\n" +
                "Delivery Plan: Review in 2 weeks\n" +
                "Postpartum Note: IUD at 6 weeks\n" +
                "Postpartum Note: Review at 6 weeks",
            composeDeliveryNotes(s),
        )
    }

    @Test
    fun composeAlwaysWritesHospitalThenFeesThenPlanThenPostpartum() {
        val s = DeliveryNoteSections("H", "F", "P", "Q")
        assertEquals(
            listOf(
                "Agreed Delivery Hospital: H",
                "Agreed Delivery Fees: F",
                "Delivery Plan: P",
                "Postpartum Note: Q",
            ),
            composeDeliveryNotes(s).lines(),
        )
    }

    @Test
    fun composeTrimsSpacesAndTabsAtValueEdges() {
        val s = DeliveryNoteSections(null, " \t5000 EGP\t ", "\t SVD ", " IUD \t")
        assertEquals(
            "Agreed Delivery Fees: 5000 EGP\nDelivery Plan: SVD\nPostpartum Note: IUD",
            composeDeliveryNotes(s),
        )
    }

    @Test
    fun composeDropsBlankLinesAndKeepsTheOrderOfTheRemainingLines() {
        val s = DeliveryNoteSections(null, null, "SVD\n\n   \nReview in 2 weeks\n\t\n", null)
        assertEquals("Delivery Plan: SVD\nDelivery Plan: Review in 2 weeks", composeDeliveryNotes(s))
    }

    @Test
    fun composeSplitsCarriageReturnLineFeedInsideAValueIntoTwoLabelledLines() {
        assertEquals(
            "Postpartum Note: IUD at 6 weeks\nPostpartum Note: Review at 6 weeks",
            composeDeliveryNotes(DeliveryNoteSections(null, null, null, "IUD at 6 weeks\r\nReview at 6 weeks")),
        )
    }

    @Test
    fun composeCollapsesDuplicateHospitalLinesAndOnlyHospitalLines() {
        val s = DeliveryNoteSections(
            hospital = "Cleopatra Hospital\nCleopatra Hospital\nAl Salam Hospital",
            fees = "20000 EGP\n20000 EGP",
            plan = null,
            postpartum = null,
        )
        assertEquals(
            "Agreed Delivery Hospital: Cleopatra Hospital\n" +
                "Agreed Delivery Hospital: Al Salam Hospital\n" +
                "Agreed Delivery Fees: 20000 EGP\n" +
                "Agreed Delivery Fees: 20000 EGP",
            composeDeliveryNotes(s),
        )
    }

    @Test
    fun composeTrimsNonBreakingSpaceAtValueEdgesAndDropsNonBreakingSpaceOnlyLines() {
        assertEquals(
            "Agreed Delivery Fees: 5000 EGP",
            composeDeliveryNotes(DeliveryNoteSections(null, "\u00A05000 EGP\u00A0", null, null)),
        )
        assertEquals(
            "",
            composeDeliveryNotes(DeliveryNoteSections(null, null, "\u00A0\u00A0", null)),
        )
        assertEquals(
            "5000\u00A0EGP",
            parseDeliveryNotes("Agreed Delivery Fees: 5000\u00A0EGP").fees,
        )
    }

    @Test
    fun lineSeparatorInsideAValueStaysOnOneLineAndParsesBackIntoTheSameBox() {
        val plan = "SVD\u2028Review in 2 weeks"
        assertEquals(
            "Delivery Plan: SVD\u2028Review in 2 weeks",
            composeDeliveryNotes(DeliveryNoteSections(null, null, plan, null)),
        )
        val s = parseDeliveryNotes("Delivery Plan: SVD\u2028Review in 2 weeks")
        assertEquals(plan, s.plan)
        assertNull(s.postpartum)
    }

    @Test
    fun lineSeparatorAtValueEdgesIsTrimmedAway() {
        assertEquals(
            "Delivery Plan: SVD",
            composeDeliveryNotes(DeliveryNoteSections(null, null, "\u2028SVD\u2028", null)),
        )
    }

    @Test
    fun nextLineCharacterInsideAValueStaysOnOneLineAndParsesBackIntoTheSameBox() {
        val s = parseDeliveryNotes("Agreed Delivery Fees: 5000\u0085EGP")
        assertEquals("5000\u0085EGP", s.fees)
        assertNull(s.plan)
    }

    @Test
    fun nextLineCharacterAtValueEdgesIsNotTrimmedButStillRoundTrips() {
        // Kotlin trim() does not treat U+0085 as whitespace, so it stays; the round trip must hold anyway.
        val sections = DeliveryNoteSections(null, "\u0085quoted 30000\u0085", null, null)
        val composed = composeDeliveryNotes(sections)
        assertEquals("Agreed Delivery Fees: \u0085quoted 30000\u0085", composed)
        assertEquals(sections, parseDeliveryNotes(composed))
    }

    @Test
    fun loneCarriageReturnAtValueEdgesIsDroppedAndInsideAValueSplitsLines() {
        assertEquals(
            "Delivery Plan: SVD",
            composeDeliveryNotes(DeliveryNoteSections(null, null, "\rSVD\r", null)),
        )
        assertEquals(
            "Delivery Plan: SVD\nDelivery Plan: Review in 2 weeks",
            composeDeliveryNotes(DeliveryNoteSections(null, null, "SVD\rReview in 2 weeks", null)),
        )
    }

    @Test
    fun aFiveThousandCharacterValueRoundTripsWhole() {
        val plan = "x".repeat(5000)
        val notes = composeDeliveryNotes(DeliveryNoteSections(null, null, plan, null))
        assertEquals("Delivery Plan: ".length + 5000, notes.length)
        assertEquals(plan, parseDeliveryNotes(notes).plan)
    }

    @Test
    fun aFiveThousandCharacterFeesValueWithSemicolonsRoundTripsWhole() {
        val fees = "5000 EGP; ".repeat(500)
        assertEquals(5000, fees.length)
        val s = parseDeliveryNotes(composeDeliveryNotes(DeliveryNoteSections(null, fees, null, null)))
        assertEquals(fees.trim(), s.fees)
        assertNull(s.plan)
        assertNull(s.hospital)
    }

    @Test
    fun feesValueWithSemicolonAndSentenceEndStaysWholeInTheFeesBox() {
        val notes = composeDeliveryNotes(DeliveryNoteSections(null, "5000 EGP; includes 2 nights", null, null))
        assertEquals("Agreed Delivery Fees: 5000 EGP; includes 2 nights", notes)
        val s = parseDeliveryNotes(notes)
        assertEquals("5000 EGP; includes 2 nights", s.fees)
        assertNull(s.plan)
        assertNull(s.postpartum)
    }

    @Test
    fun planValueWithFeeWordsStaysWholeInThePlanBox() {
        val notes = composeDeliveryNotes(DeliveryNoteSections(null, null, "Package 20000 EGP", null))
        assertEquals("Delivery Plan: Package 20000 EGP", notes)
        val s = parseDeliveryNotes(notes)
        assertEquals("Package 20000 EGP", s.plan)
        assertNull(s.fees)
    }

    @Test
    fun quotedFeeValueStaysWholeInTheFeesBox() {
        val notes = composeDeliveryNotes(DeliveryNoteSections(null, "quoted 30000", null, null))
        assertEquals("Agreed Delivery Fees: quoted 30000", notes)
        assertEquals("quoted 30000", parseDeliveryNotes(notes).fees)
    }

    @Test
    fun clearedHospitalStaysNullWhenThePlanMentionsAHospitalName() {
        val notes = composeDeliveryNotes(DeliveryNoteSections(null, null, "Delivery at Cleopatra Hospital", null))
        assertEquals("Delivery Plan: Delivery at Cleopatra Hospital", notes)
        val s = parseDeliveryNotes(notes)
        assertNull(s.hospital)
        assertEquals("Delivery at Cleopatra Hospital", s.plan)
    }

    @Test
    fun planValueThatLooksLikeAnotherFieldsLabelStaysInThePlanBox() {
        val notes = composeDeliveryNotes(DeliveryNoteSections(null, null, "Agreed Delivery Fees: 5000", null))
        assertEquals("Delivery Plan: Agreed Delivery Fees: 5000", notes)
        val s = parseDeliveryNotes(notes)
        assertEquals("Agreed Delivery Fees: 5000", s.plan)
        assertNull(s.fees)
        assertNull(s.hospital)
    }

    @Test
    fun workedExampleParsesToFourSectionsAndComposesToFourLabelledLines() {
        val notes = "Delivery at Cleopatra Hospital\nFees: 20000 EGP\nPostpartum: IUD at 6 weeks"
        val s = parseDeliveryNotes(notes)
        assertEquals("Cleopatra Hospital", s.hospital)
        assertEquals("20000 EGP", s.fees)
        assertEquals("Delivery at Cleopatra Hospital", s.plan)
        assertEquals("IUD at 6 weeks", s.postpartum)
        assertEquals(
            "Agreed Delivery Hospital: Cleopatra Hospital\n" +
                "Agreed Delivery Fees: 20000 EGP\n" +
                "Delivery Plan: Delivery at Cleopatra Hospital\n" +
                "Postpartum Note: IUD at 6 weeks",
            composeDeliveryNotes(s),
        )
    }

    @Test
    fun handTypedExactLabelLineReadsWithoutItsLabelBeforeAndAfterTheFirstSave() {
        val handTyped = "Delivery Plan: Review in 2 weeks"
        val before = parseDeliveryNotes(handTyped)
        assertEquals("Review in 2 weeks", before.plan)
        assertNull(before.hospital)
        assertEquals("Delivery Plan: Review in 2 weeks", composeDeliveryNotes(before))
        assertEquals(before, parseDeliveryNotes(composeDeliveryNotes(before)))
    }

    @Test
    fun handTypedPostpartumNoteLineIsAlreadyComposedSoTheFirstSaveChangesNothing() {
        val handTyped = "Postpartum Note: IUD at 6 weeks"
        val before = parseDeliveryNotes(handTyped)
        assertEquals("IUD at 6 weeks", before.postpartum)
        assertNull(before.hospital)
        assertEquals(handTyped, composeDeliveryNotes(before))
        assertTrue(isComposedDeliveryNotes(handTyped))
    }

    @Test
    fun lowercaseHandTypedLabelKeepsTodaysSplittingAndIsNotAComposedText() {
        val notes = "postpartum note: a; b"
        val s = parseDeliveryNotes(notes)
        assertEquals("a", s.postpartum)
        assertEquals("b", s.plan)
        assertFalse(isComposedDeliveryNotes(notes))
    }

    @Test
    fun strictPostpartumLineKeepsSemicolonsAndSentenceEndsInsideItsValue() {
        val s = parseDeliveryNotes("Postpartum Note: a; b. Dr. X reviewed")
        assertEquals("a; b. Dr. X reviewed", s.postpartum)
        assertNull(s.plan)
        assertNull(s.hospital)
    }

    @Test
    fun strictFeesLineKeepsSemicolonsAndSentenceEndsInsideItsValue() {
        val s = parseDeliveryNotes("Agreed Delivery Fees: 5000 EGP; includes 2 nights. Dr. X")
        assertEquals("5000 EGP; includes 2 nights. Dr. X", s.fees)
        assertNull(s.plan)
        assertNull(s.postpartum)
    }

    @Test
    fun deliveryPlanLabelWithoutAValueFallsBackToTheHeuristicAndKeepsItsText() {
        val s = parseDeliveryNotes("Delivery Plan:")
        assertEquals("Delivery Plan:", s.plan)
        assertNull(s.fees)
        assertNull(s.postpartum)
        assertEquals(s, parseDeliveryNotes(composeDeliveryNotes(s)))
    }

    @Test
    fun mixedNotesApplyStrictLinesAndHeuristicLinesTogetherWithHospitalInLineOrder() {
        val notes = "Agreed Delivery Hospital: Cleopatra Hospital\n" +
            "Delivery at Al Salam Hospital\n" +
            "Fees: 20000 EGP\n" +
            "Postpartum Note: IUD at 6 weeks; review at 6 weeks"
        val s = parseDeliveryNotes(notes)
        assertEquals("Cleopatra Hospital\nAl Salam Hospital", s.hospital)
        assertEquals("20000 EGP", s.fees)
        assertEquals("Delivery at Al Salam Hospital", s.plan)
        assertEquals("IUD at 6 weeks; review at 6 weeks", s.postpartum)
    }

    @Test
    fun hospitalLinesKeepSourceOrderAcrossHeuristicAndStrictLines() {
        val notes = "Delivery at Al Salam Hospital\nAgreed Delivery Hospital: Cleopatra Hospital"
        assertEquals("Al Salam Hospital\nCleopatra Hospital", parseDeliveryNotes(notes).hospital)
    }

    @Test
    fun aValueThatStartsWithAnotherFieldsLabelStaysInItsOwnBox() {
        val notes = "Agreed Delivery Hospital: Agreed Delivery Fees: 5000\n" +
            "Agreed Delivery Fees: Delivery Plan: SVD\n" +
            "Delivery Plan: Postpartum Note: IUD at 6 weeks\n" +
            "Postpartum Note: Agreed Delivery Hospital: Cleopatra Hospital"
        val s = parseDeliveryNotes(notes)
        assertEquals("Agreed Delivery Fees: 5000", s.hospital)
        assertEquals("Delivery Plan: SVD", s.fees)
        assertEquals("Postpartum Note: IUD at 6 weeks", s.plan)
        assertEquals("Agreed Delivery Hospital: Cleopatra Hospital", s.postpartum)
    }

    @Test
    fun composedCheckIsTrueForNullBlankAndEveryComposeOutput() {
        assertTrue(isComposedDeliveryNotes(null))
        assertTrue(isComposedDeliveryNotes(""))
        assertTrue(isComposedDeliveryNotes("   \n\t "))
        assertTrue(
            isComposedDeliveryNotes(
                composeDeliveryNotes(
                    DeliveryNoteSections("Cleopatra Hospital", "20000 EGP", "SVD", "IUD at 6 weeks"),
                ),
            ),
        )
    }

    @Test
    fun composedCheckIsFalseForFreeTextAndForComposeOutputWithAnAddedTrailingNewline() {
        val workedExample = "Delivery at Cleopatra Hospital\nFees: 20000 EGP\nPostpartum: IUD at 6 weeks"
        assertFalse(isComposedDeliveryNotes(workedExample))
        val composed = composeDeliveryNotes(DeliveryNoteSections("Cleopatra Hospital", "20000 EGP", "SVD", null))
        assertTrue(isComposedDeliveryNotes(composed))
        assertFalse(isComposedDeliveryNotes(composed + "\n"))
    }

    @Test
    fun theoremAComposeThenParseEqualsTheNormalisedFieldsForRandomSections() {
        val random = Random(20261111)
        repeat(2000) { i ->
            val raw = randomSections(random)
            assertEquals(
                "iteration $i for $raw",
                normalise(raw),
                parseDeliveryNotes(composeDeliveryNotes(raw)),
            )
        }
    }

    @Test
    fun corollaryCComposeIsIdempotentThroughParseForRandomSections() {
        val random = Random(20261111)
        repeat(2000) { i ->
            val raw = randomSections(random)
            val once = composeDeliveryNotes(raw)
            assertEquals(
                "iteration $i for $raw",
                once,
                composeDeliveryNotes(parseDeliveryNotes(once)),
            )
        }
    }

    @Test
    fun theoremBParseThenComposeThenParseIsStableForRandomFreeTextNotes() {
        val random = Random(20261111)
        repeat(2000) { i ->
            val notes = randomNote(random)
            val parsed = parseDeliveryNotes(notes)
            assertEquals(
                "iteration $i for <$notes>",
                parsed,
                parseDeliveryNotes(composeDeliveryNotes(parsed)),
            )
        }
    }

    @Test
    fun theoremBHoldsForEveryFixtureNoteOnFile() {
        for (notes in fixtureNotes) {
            val parsed = parseDeliveryNotes(notes)
            assertEquals("<$notes>", parsed, parseDeliveryNotes(composeDeliveryNotes(parsed)))
        }
    }

    private val fixtureNotes = listOf(
        "Agreed delivery mode: Spontaneous Vaginal Delivery (SVD) at Cleopatra Maternity Hospital.\n" +
            "Delivery fee package agreed: 15,000 EGP (Full hospital stay + pediatrician attendance).\n" +
            "Postpartum note: Successfully delivered healthy female infant on 2026-10-12.",
        "Previous uncomplicated full-term vaginal delivery in 2022.",
        "Delivery at Cleopatra Hospital\nFees: 20000 EGP\nPostpartum: IUD at 6 weeks",
        "Booking visit. Planned LSCS at Dar Al Fouad Hospital. Fee quoted 30000 EGP. Not agreed yet.",
        "Previous LSCS Al Salam Hospital 2023.\nNow booked Cleopatra Hospital.\nDelivery Plan: SVD",
    )

    private val tokens = listOf(
        ";", ". ", ":", "Dr.", "Hospital", "X Hospital", "Al Salam Hospital",
        LABEL_HOSPITAL, LABEL_FEES, LABEL_PLAN, LABEL_POSTPARTUM,
        LABEL_HOSPITAL.lowercase(), LABEL_FEES.lowercase(), LABEL_PLAN.lowercase(), LABEL_POSTPARTUM.lowercase(),
        "quoted", "not agreed", "EGP", "delivered", "5000", "20000", "3",
        "\u00A0", "\u2028", "\u0085", "\r\n", "\n", "\n\n", " ", "  ", "\t",
        "at", "Fees", "Plan", "Postpartum", "Hospital:", "IUD at 6 weeks", "word", ".",
        "Agreed Delivery Fees: 5000",
    )

    @Test
    fun aFreeLineAddedAfterTheFirstSaveMakesTheNotesNotComposedAgain() {
        val composed = composeDeliveryNotes(DeliveryNoteSections(null, null, "SVD", "IUD at 6 weeks"))
        assertTrue(isComposedDeliveryNotes(composed))
        assertFalse(isComposedDeliveryNotes(composed + "\nCall if bleeding"))
    }

    @Test
    fun nextLineCharacterBetweenTwoLabelledLinesIsOneLineSoTheSecondLabelStaysInsideTheFirstValue() {
        // lines() does not split on U+0085, so text pasted with it as the line end reads as one line.
        val notes = "Agreed Delivery Hospital: Cleopatra\u0085Agreed Delivery Fees: 20000"
        val sections = parseDeliveryNotes(notes)
        assertEquals("Cleopatra\u0085Agreed Delivery Fees: 20000", sections.hospital)
        assertNull(sections.fees)
    }

    private fun randomSections(random: Random) = DeliveryNoteSections(
        hospital = randomField(random),
        fees = randomField(random),
        plan = randomField(random),
        postpartum = randomField(random),
    )

    private fun randomField(random: Random): String? = when (random.nextInt(4)) {
        0 -> null
        else -> randomTokens(random, random.nextInt(0, 6))
    }

    private fun randomTokens(random: Random, count: Int): String =
        buildString { repeat(count) { append(tokens[random.nextInt(tokens.size)]) } }

    private fun randomNote(random: Random): String = buildString {
        repeat(random.nextInt(1, 8)) {
            append(randomTokens(random, random.nextInt(0, 5)))
            if (random.nextBoolean()) append('\n')
        }
    }

    private fun normalise(sections: DeliveryNoteSections): DeliveryNoteSections {
        fun field(value: String?, distinct: Boolean): String? =
            value.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
                .let { if (distinct) it.distinct() else it }
                .joinToString("\n")
                .ifBlank { null }
        return DeliveryNoteSections(
            hospital = field(sections.hospital, distinct = true),
            fees = field(sections.fees, distinct = false),
            plan = field(sections.plan, distinct = false),
            postpartum = field(sections.postpartum, distinct = false),
        )
    }
}
