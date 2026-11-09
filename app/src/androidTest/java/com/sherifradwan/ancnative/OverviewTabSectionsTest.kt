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
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.tabs.OverviewTab
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
            hasText(text, substring = true) and hasAnyAncestor(hasTestTag(ancestorTag)),
            useUnmergedTree = true,
        )
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node.fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        results.forEach { layout ->
            assertFalse("'$text' overflowed", layout.hasVisualOverflow)
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
                hasText(t, substring = true) and hasAnyAncestor(hasTestTag(extrasTag)),
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
            hasText("Medical", substring = true) and hasAnyAncestor(hasTestTag("pregnancyExtras-${row.id}")),
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
}
