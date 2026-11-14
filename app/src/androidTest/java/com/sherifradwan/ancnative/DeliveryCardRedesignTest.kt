package com.sherifradwan.ancnative

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.components.DeliveryPlanningCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeliveryCardRedesignTest {
    @get:Rule
    val compose = createComposeRule()

    private val base = DemoFixtures.patients.first()

    private fun composedNotes(
        hospital: String = "Cairo University Hospital",
        fees: String = "25000 EGP",
        plan: String = "Elective caesarean at 39 weeks",
        postpartum: String = "IUD at 6 weeks",
    ): String = buildString {
        append("Agreed Delivery Hospital: ").append(hospital).append('\n')
        append("Agreed Delivery Fees: ").append(fees).append('\n')
        append("Delivery Plan: ").append(plan).append('\n')
        append("Postpartum Note: ").append(postpartum)
    }

    private fun textOf(tag: String): String =
        compose.onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config
            .getOrElse(SemanticsProperties.Text) { emptyList() }
            .joinToString("") { it.text }

    @Test
    fun fourFieldsShowLabelsAndValuesWithMinimumHeight() {
        val patient = base.copy(
            obstetricNotes = composedNotes(),
            delivered = false,
        )
        compose.setContent {
            AncTheme {
                DeliveryPlanningCard(patient = patient, onEditDeliveryPlan = {}, canEdit = true)
            }
        }
        compose.waitForIdle()

        val fields = listOf(
            "deliveryHospital" to listOf("Agreed delivery hospital", "Cairo University Hospital"),
            "deliveryFees" to listOf("Agreed delivery fees", "25000 EGP"),
            "deliveryPlan" to listOf("Delivery plan", "Elective caesarean at 39 weeks"),
            "deliveryPostpartum" to listOf("Postpartum note", "IUD at 6 weeks"),
        )
        fields.forEach { (tag, texts) ->
            val node = compose.onNodeWithTag(tag, useUnmergedTree = true)
            node.assertIsDisplayed()
            node.assertHeightIsAtLeast(44.dp)
            texts.forEach { expected ->
                compose.onNode(
                    hasText(expected, substring = true) and hasAnyAncestor(hasTestTag(tag)),
                    useUnmergedTree = true,
                ).assertExists()
            }
        }
    }

    @Test
    fun emptyNotesShowNotRecordedFourTimesAndNeverNoRecord() {
        val patient = base.copy(obstetricNotes = "", delivered = false)
        compose.setContent {
            AncTheme {
                DeliveryPlanningCard(patient = patient, onEditDeliveryPlan = {}, canEdit = true)
            }
        }
        compose.waitForIdle()

        val notRecordedCount = compose.onAllNodes(
            hasText("Not recorded") and hasAnyAncestor(hasTestTag("deliveryPlanningCard")),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().size
        assertEquals(4, notRecordedCount)

        compose.onAllNodes(
            hasText("No record", substring = true) and hasAnyAncestor(hasTestTag("deliveryPlanningCard")),
            useUnmergedTree = true,
        ).assertCountEquals(0)
    }

    @Test
    fun canEditFalseHidesEditAction() {
        val patient = base.copy(obstetricNotes = composedNotes(), delivered = false)
        compose.setContent { AncTheme { DeliveryPlanningCard(patient = patient, onEditDeliveryPlan = {}, canEdit = false) } }
        compose.waitForIdle()
        compose.onNodeWithTag("editDeliveryPlan").assertDoesNotExist()
    }

    @Test
    fun canEditTrueGivesEditActionMinimumTouchTarget() {
        val patient = base.copy(obstetricNotes = composedNotes(), delivered = false)
        compose.setContent { AncTheme { DeliveryPlanningCard(patient = patient, onEditDeliveryPlan = {}, canEdit = true) } }
        compose.waitForIdle()
        compose.onNodeWithTag("editDeliveryPlan", useUnmergedTree = true)
            .assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun deliveredWithDateShowsTagAndFormattedDate() {
        val patient = base.copy(
            obstetricNotes = composedNotes(),
            delivered = true,
            deliveryDate = "2026-11-05",
            deliveryMode = "vaginal",
            deliveryOutcome = "live_birth",
        )
        compose.setContent {
            AncTheme {
                DeliveryPlanningCard(patient = patient, onEditDeliveryPlan = {}, canEdit = true)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("Delivered", useUnmergedTree = true).assertIsDisplayed()
        val headline = textOf("deliveredHeadline")
        assertTrue(
            "Expected 'dd MMM yyyy' in delivered headline but was: $headline",
            Regex("""\d{2} [A-Z][a-z]{2} \d{4}""").containsMatchIn(headline),
        )
    }

    @Test
    fun deliveredWithoutDateShowsQuestionMarksAndModeOnlyHidesResult() {
        val withoutDate = base.copy(
            obstetricNotes = composedNotes(),
            delivered = true,
            deliveryDate = null,
            deliveryMode = "vaginal",
            deliveryOutcome = null,
        )
        compose.setContent {
            AncTheme {
                DeliveryPlanningCard(patient = withoutDate, onEditDeliveryPlan = {}, canEdit = true)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag("deliveredHeadline", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNode(
            hasText("???", substring = true) and hasAnyAncestor(hasTestTag("deliveredHeadline")),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun deliveredModeOnlyHidesResult() {
        val modeOnly = base.copy(
            obstetricNotes = composedNotes(),
            delivered = true,
            deliveryDate = "2026-11-05",
            deliveryMode = "vaginal",
            deliveryOutcome = null,
        )
        compose.setContent {
            AncTheme {
                DeliveryPlanningCard(patient = modeOnly, onEditDeliveryPlan = {}, canEdit = true)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag("deliveredMode", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("deliveredResult").assertDoesNotExist()
    }

    @Test
    fun hospitalLabelIsSentenceCaseNotUppercase() {
        val patient = base.copy(obstetricNotes = composedNotes(), delivered = false)
        compose.setContent {
            AncTheme {
                DeliveryPlanningCard(patient = patient, onEditDeliveryPlan = {}, canEdit = true)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("Agreed delivery hospital", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("AGREED DELIVERY HOSPITAL").assertDoesNotExist()
    }

    @Test
    fun rtlPutsTitleRightOfEdit() {
        val patient = base.copy(obstetricNotes = composedNotes(), delivered = false)
        compose.setContent {
            AncTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    DeliveryPlanningCard(patient = patient, onEditDeliveryPlan = {}, canEdit = true)
                }
            }
        }
        compose.waitForIdle()

        val titleBounds = compose.onNodeWithText("Delivery planning & outcome", useUnmergedTree = true)
            .getBoundsInRoot()
        val editBounds = compose.onNodeWithTag("editDeliveryPlan", useUnmergedTree = true)
            .getBoundsInRoot()
        assertTrue(
            "Expected title to be right of edit in RTL (title.left=${titleBounds.left}, edit.right=${editBounds.right})",
            titleBounds.left >= editBounds.right,
        )
    }

    @Test
    fun darkThemeRendersAllFourFieldsDisplayed() {
        val patient = base.copy(obstetricNotes = composedNotes(), delivered = false)
        compose.setContent {
            AncTheme(darkTheme = true) {
                DeliveryPlanningCard(patient = patient, onEditDeliveryPlan = {}, canEdit = true)
            }
        }
        compose.waitForIdle()

        listOf("deliveryHospital", "deliveryFees", "deliveryPlan", "deliveryPostpartum").forEach { tag ->
            compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
        }
    }
}
