package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.GestationalBanner
import com.sherifradwan.ancnative.ui.patienthub.tabs.OverviewTab
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverviewTabSectionsTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** Seeded multiparous patient: the first fixture with at least two previous pregnancies. */
    private val multiparous: DemoPatient =
        DemoFixtures.patients.first { it.previousPregnancies.size >= 2 }

    private fun hasTagPrefix(prefix: String) = SemanticsMatcher("testTag starts with $prefix") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    /** Matches the node carrying [tag] and any of its descendants (hasAnyAncestor alone excludes the tagged node). */
    private fun inTagged(tag: String) = hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag))

    private fun show(
        patient: DemoPatient,
        canEdit: Boolean = true,
        widthDp: Int? = null,
        fontScale: Float? = null,
    ) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    val base = LocalDensity.current
                    val density = if (fontScale != null) Density(base.density, fontScale) else base
                    CompositionLocalProvider(LocalDensity provides density) {
                        val sized = if (widthDp != null) Modifier.width(widthDp.dp) else Modifier
                        Box(sized) {
                            OverviewTab(
                                patient = patient,
                                canEdit = canEdit,
                                onEvent = {},
                                onAddAttachment = {},
                                onViewAttachment = {},
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    // ORACLE-C1
    @Test
    fun legacyTagsResolveForMultiparousPatient() {
        show(multiparous)
        val tags = listOf(
            "overviewTabContent",
            "datingCard",
            "editDating",
            "obstetricParityCard",
            "obstetricParityBadge",
            "addPregnancyButton",
            "medicalHistoriesCard",
            "editMedicalHistoryButton",
            "deliveryPlanningCard",
            "editDeliveryPlan",
            // Delivery sections always render (placeholder when empty), so they exist for any fixture.
            "deliveryHospital",
            "deliveryFees",
            "deliveryPlan",
            "deliveryPostpartum",
            "attachmentsSectionCard",
            "addAttachmentBtn",
        )
        tags.forEach { compose.onNodeWithTag(it, useUnmergedTree = true).assertExists("missing tag $it") }
        multiparous.previousPregnancies.forEach { p ->
            val row = compose.onNodeWithTag("pregnancy-${p.id}", useUnmergedTree = true)
            val edit = compose.onNodeWithTag("editPregnancy-${p.id}", useUnmergedTree = true)
            row.assertExists()
            edit.assertExists()
            compose.onNodeWithTag("delete-previous_pregnancy-${p.id}", useUnmergedTree = true).assertExists()
            val rowId = row.fetchSemanticsNode().id
            val editId = edit.fetchSemanticsNode().id
            assertTrue("pregnancy-<id> and editPregnancy-<id> must be different nodes", rowId != editId)
        }
    }

    // ORACLE-C2
    @Test
    fun readOnlyHidesEditAffordances() {
        show(multiparous, canEdit = false)
        compose.onAllNodes(hasTestTag("editDating"), useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodes(hasTestTag("addPregnancyButton"), useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodes(hasTestTag("editDeliveryPlan"), useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodes(hasTagPrefix("editPregnancy-"), useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodes(hasTagPrefix("delete-previous_pregnancy-"), useUnmergedTree = true).assertCountEquals(0)
        // Read-only content itself is still rendered.
        compose.onNodeWithTag("obstetricParityCard", useUnmergedTree = true).assertExists()
        multiparous.previousPregnancies.forEach {
            compose.onNodeWithTag("pregnancy-${it.id}", useUnmergedTree = true).assertExists()
        }
    }

    // ORACLE-C3
    @Test
    fun rowAt360dpAndFontScale13ShowsOutcomeAndModeUnclipped() {
        val outcome = "Live birth at term"
        val mode = "Cesarean Section (CS)"
        val base = multiparous.previousPregnancies.first()
        val longRow: DemoPreviousPregnancy = base.copy(
            id = "40000000-0000-4000-8000-0000000000ff",
            outcome = null,
            outcomeText = outcome,
            birthMethod = mode,
            deliveryBy = "CS",
        )
        val patient = multiparous.copy(previousPregnancies = listOf(longRow))
        show(patient, widthDp = 360, fontScale = 1.3f)

        val rowTag = "pregnancy-${longRow.id}"
        val rowBounds = compose.onNodeWithTag(rowTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val deleteBounds = compose
            .onNodeWithTag("delete-previous_pregnancy-${longRow.id}", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        listOf(outcome, mode).forEach { text ->
            val node = compose.onNode(
                hasText(text, substring = true) and hasAnyAncestor(hasTestTag(rowTag)),
                useUnmergedTree = true,
            )
            node.assertExists("text '$text' not found in row")
            node.assertIsDisplayed()
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue("'$text' starts left of row", bounds.left >= rowBounds.left)
            assertTrue("'$text' extends past row right edge", bounds.right <= rowBounds.right)
            assertTrue(
                "'$text' overlaps delete button (right=${bounds.right}, deleteLeft=${deleteBounds.left})",
                bounds.right <= deleteBounds.left,
            )
            assertNotEllipsized(text, rowTag)
        }
    }

    private fun assertNotEllipsized(text: String, ancestorTag: String) {
        val node = compose.onNode(
            hasText(text, substring = true) and inTagged(ancestorTag),
            useUnmergedTree = true,
        )
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node.fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        results.forEach { layout ->
            // hasVisualOverflow is not used: text that shrinks to its content width reports
            // size.width < laid-out paragraph width even when nothing is clipped. Check clipping directly.
            assertTrue("'$text' clipped vertically", layout.size.height >= layout.multiParagraph.height.toInt())
            for (line in 0 until layout.lineCount) {
                assertTrue("'$text' line $line clipped horizontally", layout.getLineRight(line) <= layout.size.width + 1f)
            }
            for (line in 0 until layout.lineCount) {
                assertFalse("'$text' ellipsized on line $line", layout.isLineEllipsized(line))
            }
        }
    }

    // Extras live in their own node and must stay visible with a very long placeAndDoctor.
    @Test
    fun extrasStayVisibleWithVeryLongPlaceAndDoctor() {
        val base = multiparous.previousPregnancies.first()
        val row = base.copy(
            id = "40000000-0000-4000-8000-0000000000fe",
            placeAndDoctor = "Kasr Al Ainy Educational Hospital Obstetrics Department " +
                "Dr. Somebody With A Remarkably Long Name Consultant Obstetrician ".repeat(3),
            pregnancyType = "Twin",
            lossManagement = "Expectant",
            terminationMethod = "Medical",
        )
        show(multiparous.copy(previousPregnancies = listOf(row)), widthDp = 360, fontScale = 1.3f)
        val extrasTag = "pregnancyExtras-${row.id}"
        compose.onNodeWithTag(extrasTag, useUnmergedTree = true).assertExists().assertIsDisplayed()
        listOf("Twin", "Expectant", "Medical").forEach { t ->
            compose.onNode(
                hasText(t, substring = true) and inTagged(extrasTag),
                useUnmergedTree = true,
            ).assertExists("extras text '$t' missing from $extrasTag")
            assertNotEllipsized(t, extrasTag)
        }
    }

    @Test
    fun terminationRowRendersOutcomeLabelAndMethod() {
        val base = multiparous.previousPregnancies.first()
        val row = base.copy(
            id = "40000000-0000-4000-8000-0000000000fd",
            outcome = "TerminationOfPregnancy",
            outcomeText = null,
            terminationMethod = "Medical",
            lossManagement = null,
            pregnancyType = null,
        )
        show(multiparous.copy(previousPregnancies = listOf(row)))
        val rowTag = "pregnancy-${row.id}"
        compose.onNodeWithTag(rowTag, useUnmergedTree = true).assertExists()
        compose.onNode(
            hasText("Termination of pregnancy", substring = true, ignoreCase = true) and hasAnyAncestor(hasTestTag(rowTag)),
            useUnmergedTree = true,
        ).assertExists()
        compose.onNode(
            hasText("Medical", substring = true) and inTagged("pregnancyExtras-${row.id}"),
            useUnmergedTree = true,
        ).assertExists()
    }

    // Allergy states
    private fun cardHasText(text: String) =
        compose.onAllNodes(
            hasText(text, substring = true) and hasAnyAncestor(hasTestTag("medicalHistoriesCard")),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun allergyRecordedShowsBanner() {
        show(multiparous.copy(allergyHistory = "Penicillin"))
        assertTrue(cardHasText("Allergy: Penicillin"))
        assertFalse(cardHasText("No known drug allergies"))
    }

    @Test
    fun allergyNkdaShowsCalmTextNoBanner() {
        show(multiparous.copy(allergyHistory = "NKDA"))
        assertTrue(cardHasText("No known drug allergies"))
        assertFalse(cardHasText("Allergy: "))
    }

    @Test
    fun allergyBlankShowsNotRecorded() {
        show(multiparous.copy(allergyHistory = "  "))
        assertTrue(cardHasText("Not recorded"))
        assertFalse(cardHasText("Allergy: "))
        assertFalse(cardHasText("No known drug allergies"))
    }

    // ORACLE-C4
    @Test
    fun emptyListPrimigravidaOnlyWhenGravidaAtMostOne() {
        val g1 = multiparous.copy(gravida = 1L, parity = 0L, abortion = 0L, previousPregnancies = emptyList())
        show(g1)
        compose.onAllNodes(hasText("primigravida", substring = true, ignoreCase = true), useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun emptyListDoesNotSayPrimigravidaForGravida3() {
        val g3 = multiparous.copy(gravida = 3L, parity = 1L, abortion = 1L, previousPregnancies = emptyList())
        show(g3)
        compose.onNodeWithTag("obstetricParityCard", useUnmergedTree = true).assertExists()
        compose.onAllNodes(hasText("primigravida", substring = true, ignoreCase = true), useUnmergedTree = true)
            .assertCountEquals(0)
    }

    // Dating section variants a to f and the discordance alert (overview_tab_sections.html).
    private val nbsp = "\u00A0"
    private val alertTag = "datingDiscordanceAlert"

    private fun dating(
        lmp: String? = null,
        us: String? = null,
        agreed: String? = null,
        method: String? = null,
        transfer: String? = null,
        embryoAge: Int? = null,
        notes: String? = null,
    ) = DemoPatientDating(
        patientId = multiparous.id, lmpDate = lmp, ultrasoundEdd = us, agreedEdd = agreed,
        datingMethod = method, notes = notes, transferDate = transfer, embryoAgeDays = embryoAge,
    )

    /** One scan on 2026-08-27 (80 days after the LMP) is the earliest scan, so the discordance anchor is fixed. */
    private fun withDating(d: DemoPatientDating): DemoPatient = multiparous.copy(
        dating = d,
        ultrasoundScans = listOf(
            DemoUltrasoundScan(
                id = "dating-anchor-scan", patientId = multiparous.id, date = "2026-08-27",
                gestationalAgeWeeks = null, gestationalAgeDays = null, fetalNumber = null,
                presentation = null, placentalSite = null, amnioticFluid = null,
                fetalHeartRate = null, biometryBpd = null, biometryHc = null, biometryAc = null,
                biometryFl = null, estimatedFetalWeight = null, impression = null,
            ),
        ),
    )

    private val variantA = dating(lmp = "2026-06-08", agreed = "2027-03-15", method = "LMP")
    private val variantB = dating(lmp = "2026-06-08", us = "2027-03-24", agreed = "2027-03-15", method = "LMP")
    private val variantC = dating(agreed = "2027-03-08", method = "IVF_TRANSFER", transfer = "2026-06-20", embryoAge = 5)
    private val variantD = dating(agreed = "2027-03-08", method = "IVF_TRANSFER", transfer = "2026-06-20", embryoAge = null)
    private val variantE = dating(
        lmp = "2026-06-08", us = "2027-03-24", agreed = "2027-03-24", method = "Ultrasound",
        notes = "Unsure LMP, relying on scan",
    )
    private val variantF = dating(agreed = "2027-04-02", method = "Clinical")

    /** Text nodes inside the dating card grouped into visual rows, left to right, header row dropped. */
    private fun datingRows(): List<List<String>> {
        val nodes = compose.onAllNodes(
            hasAnyAncestor(hasTestTag("datingCard")) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().sortedWith(compareBy({ it.boundsInRoot.top }, { it.boundsInRoot.left }))
        val rows = mutableListOf<MutableList<androidx.compose.ui.semantics.SemanticsNode>>()
        var rowBottom = Float.NEGATIVE_INFINITY
        nodes.forEach { n ->
            if (rows.isEmpty() || n.boundsInRoot.top >= rowBottom - 1f) {
                rows.add(mutableListOf(n))
                rowBottom = n.boundsInRoot.bottom
            } else {
                rows.last().add(n)
                rowBottom = maxOf(rowBottom, n.boundsInRoot.bottom)
            }
        }
        return rows.drop(1).map { row ->
            row.sortedBy { it.boundsInRoot.left }
                .map { it.config[SemanticsProperties.Text].joinToString("") { t -> t.text } }
        }
    }

    private fun alertCount() =
        compose.onAllNodes(hasTestTag(alertTag), useUnmergedTree = true).fetchSemanticsNodes().size

    private fun hasConfirmedEdd() = datingRows().any { it.firstOrNull() == "Confirmed EDD" }

    @Test
    fun datingVariantA_lmpPatientNoDiscordance() {
        show(withDating(variantA))
        assertEquals(
            listOf(listOf("LMP", "08/06/2026"), listOf("EDD", "15/03/2027"), listOf("Method", "LMP")),
            datingRows(),
        )
        assertEquals(0, alertCount())
        assertFalse(hasConfirmedEdd())
    }

    @Test
    fun datingVariantB_lmpPatientWithDiscordanceAlert() {
        show(withDating(variantB))
        val rows = datingRows()
        assertEquals(
            listOf(listOf("LMP", "08/06/2026"), listOf("EDD", "15/03/2027"), listOf("Method", "LMP")),
            rows.take(3),
        )
        assertEquals(4, rows.size)
        assertEquals(1, alertCount())
        val alertText = rows[3].single()
        assertEquals(
            "EDD discordance: LMP and scan differ by 9 d (limit 7 d)",
            alertText.replace(nbsp, " "),
        )
        assertTrue("number and unit must be joined by a non-breaking space", alertText.contains("9${nbsp}d"))
        assertTrue(alertText.contains("7${nbsp}d"))
        assertFalse(hasConfirmedEdd())
    }

    @Test
    fun datingVariantB_alertHasNoButton() {
        show(withDating(variantB))
        compose.onAllNodes(hasClickAction() and inTagged(alertTag), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun datingVariantC_ivfShowsEtWithEmbryoAge() {
        show(withDating(variantC))
        assertEquals(
            listOf(
                listOf("ET", "20/06/2026${nbsp}·${nbsp}D5"),
                listOf("EDD", "08/03/2027"),
                listOf("Method", "ET"),
            ),
            datingRows(),
        )
        assertEquals(0, alertCount())
        assertFalse(hasConfirmedEdd())
    }

    @Test
    fun datingVariantD_ivfWithoutEmbryoAgeHasNoD5() {
        show(withDating(variantD))
        assertEquals(
            listOf(listOf("ET", "20/06/2026"), listOf("EDD", "08/03/2027"), listOf("Method", "ET")),
            datingRows(),
        )
        compose.onAllNodes(hasText("D5", substring = true), useUnmergedTree = true).assertCountEquals(0)
        assertEquals(0, alertCount())
    }

    @Test
    fun datingVariantE_ultrasoundDatedKeepsLmpRowAndNotes() {
        show(withDating(variantE))
        assertEquals(
            listOf(
                listOf("LMP", "08/06/2026"),
                listOf("EDD", "24/03/2027"),
                listOf("Method", "by ultrasound"),
                listOf("Notes", "Unsure LMP, relying on scan"),
            ),
            datingRows(),
        )
        assertEquals(0, alertCount())
        assertFalse(hasConfirmedEdd())
    }

    @Test
    fun datingVariantF_referredShowsConfirmedEddOnlyAndNoFirstRow() {
        show(withDating(variantF))
        assertEquals(
            listOf(listOf("Confirmed EDD", "02/04/2027"), listOf("Method", "clinical")),
            datingRows(),
        )
        assertEquals(0, alertCount())
    }

    @Test
    fun datingConfirmedEddAppearsInVariantFOnly() {
        listOf(variantA, variantB, variantC, variantD, variantE).forEach { d ->
            show(withDating(d))
            assertFalse("Confirmed EDD must not show for ${d.datingMethod}", hasConfirmedEdd())
        }
        show(withDating(variantF))
        assertTrue(hasConfirmedEdd())
    }

    @Test
    fun datingAlertOnlyInVariantB() {
        listOf(variantA, variantC, variantD, variantE, variantF).forEach { d ->
            show(withDating(d))
            assertEquals("no alert expected for ${d.datingMethod}", 0, alertCount())
        }
        show(withDating(variantB))
        assertEquals(1, alertCount())
    }

    // Longest case: variant B at 360dp and font scale 1.3.
    @Test
    fun datingVariantG_alertWrapsUnclippedAt360dpFontScale13() {
        show(withDating(variantB), widthDp = 360, fontScale = 1.3f)
        compose.onNodeWithTag(alertTag, useUnmergedTree = true).assertExists().assertIsDisplayed()
        assertNotEllipsized("EDD discordance", alertTag)
        val screenWidth = 360.dp
        val card = compose.onNodeWithTag("datingCard", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val alert = compose.onNodeWithTag(alertTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("card overflows the 360dp viewport", card.right <= screenWidth)
        assertTrue("alert starts left of the card", alert.left >= card.left)
        assertTrue("alert overflows the card (right=${alert.right}, card=${card.right})", alert.right <= card.right)
        compose.onAllNodes(hasClickAction() and inTagged(alertTag), useUnmergedTree = true).assertCountEquals(0)
    }

    // GA marker: red "??" beside the current GA only when discordant (shared composable).
    private val markerTag = "gaDiscordanceMarker"

    private fun showBanner(d: DemoPatientDating) {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme { GestationalBanner(dating = d, asOfDate = LocalDate.of(2026, 10, 1)) }
            }
        }
        compose.waitForIdle()
    }

    // 25 d gap exceeds every tier limit (5 to 21 d), so the result does not depend on the anchor date.
    private val discordantLmpScan = dating(lmp = "2026-06-08", us = "2027-04-09", method = "LMP")
    private val concordantLmpScan = dating(lmp = "2026-06-08", method = "LMP")

    @Test
    fun gaMarkerShownWhenDiscordant() {
        showBanner(discordantLmpScan)
        compose.onAllNodes(hasTestTag(markerTag), useUnmergedTree = true).assertCountEquals(1)
        compose.onNode(hasText("??") and inTagged(markerTag), useUnmergedTree = true).assertExists()
        compose.onNode(
            inTagged(markerTag) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true,
        ).assertExists("marker needs a content description")
    }

    @Test
    fun gaMarkerAbsentWhenNotDiscordant() {
        showBanner(concordantLmpScan)
        compose.onAllNodes(hasTestTag(markerTag), useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodes(hasText("??", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }
}
