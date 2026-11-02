package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.MedicationDraftField
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.MedicationEditor
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Medication editor oracles T400-T405, updated 2026-11-02 for the reduced editor (name, dose, route, On/Stopped, one Note). */
@RunWith(AndroidJUnit4::class)
class MedicationEntryDesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patient = DemoFixtures.patients.first()
    private var state by mutableStateOf(AncState(patients = emptyList()))

    /** Opens a new medication with the real reducer; `setup` may pre-fill the draft before the screen mounts. */
    private fun host(setup: (AncState) -> AncState = { it }) {
        var s = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 6002L),
        )
        s = AncReducer.reduce(
            s,
            AncEvent.StartMedicationEditor(
                patientId = patient.id,
                medicationId = null,
                newMedicationId = "med-design-1",
                editorSessionId = "med-design-session-1",
            ),
        )
        state = setup(s)
        val route = state.route as AncRoute.MedicationEditor
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    MedicationEditor(
                        state = state,
                        route = route,
                        onEvent = { state = AncReducer.reduce(state, it) },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun change(s: AncState, field: MedicationDraftField, value: String): AncState =
        AncReducer.reduce(s, AncEvent.MedicationDraftChanged(field, value))

    private fun exists(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
    private fun missing(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).assertDoesNotExist()

    @Test
    fun t400_newMedicationOpensDoseRowWithRouteOralAndNoFrequencyOrStartRow() {
        host()
        exists("medOpen-dose")
        compose.onNodeWithTag("medDosage", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        exists("medRow-route")
        compose.onNode(
            hasText("Oral") and (hasTestTag("medRow-route") or hasAnyAncestor(hasTestTag("medRow-route"))),
            useUnmergedTree = true,
        ).assertExists()
        missing("medRow-frequency")
        missing("medRow-start")
        compose.onNode(
            hasTestTag("medName") and hasAnyAncestor(hasTestTag("medPickCard")),
            useUnmergedTree = true,
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t401_presetFillsNameAndDoseAndClosesTheDoseRow() {
        host()
        compose.onNodeWithTag("medPreset-0", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        val v = state.medicationDraft!!.values
        assertTrue("preset should fill name", v.name.isNotBlank())
        assertTrue("preset should fill dose", v.dosage.isNotBlank())
        missing("medOpen-dose")
    }

    @Test
    fun t402_tappingRouteRowOpensItAndClosesDose() {
        host()
        exists("medOpen-dose")
        compose.onNodeWithTag("medRow-route", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        exists("medOpen-route")
        missing("medOpen-dose")
    }

    @Test
    fun t403_twoStatusButtonsOnAndStoppedNoProphylaxis() {
        host()
        compose.onNodeWithTag("medStatusChoice-0", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        exists("medStatusChoice-1")
        missing("medStatusChoice-2")
        missing("medStatusChoice-3")
        compose.onNode(hasText("Prophylaxis", substring = true, ignoreCase = true), useUnmergedTree = true)
            .assertDoesNotExist()
        compose.onNodeWithTag("medStatusChoice-1", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("Discontinued", state.medicationDraft!!.values.status)
        assertFalse(state.medicationDraft!!.values.isNonCompliant)
        compose.onNodeWithTag("medStatusChoice-0", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("Active", state.medicationDraft!!.values.status)
    }

    @Test
    fun t404_moreSectionFrequencyEndDateAdverseNotesAreGoneAndOneNoteFieldRemains() {
        host { change(change(it, MedicationDraftField.END_DATE, "2026-12-01"), MedicationDraftField.ADVERSE_NOTES, "Nausea") }
        listOf(
            "medMoreToggle", "medMoreSection", "medFrequency", "medRow-frequency", "medOpen-frequency",
            "freqPreset-0", "medEndDate", "medAdverseNotes", "medStartDate", "medRow-start",
        ).forEach { missing(it) }
        assertEquals(1, compose.onAllNodesWithTag("medNotes", useUnmergedTree = true).fetchSemanticsNodes().size)
        // The hidden values stay in the draft untouched.
        val v = state.medicationDraft!!.values
        assertEquals("2026-12-01", v.endDate)
        assertEquals("Nausea", v.adverseNotes)
    }

    @Test
    fun t404b_statusButtonsAreLabelledActiveAndStopped() {
        host()
        compose.onNode(hasText("Active") and hasAnyAncestor(hasTestTag("medStatusChoice-0")), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Stopped") and hasAnyAncestor(hasTestTag("medStatusChoice-1")), useUnmergedTree = true).assertExists()
    }

    @Test
    fun t405_actionBarIsPinnedWithCancelSaveAndSaveAndAddAnotherAtLeast48dp() {
        host()
        // No scrolling: the bar is visible at once and stays so with the form scrolled to the bottom.
        listOf("cancelMedication", "saveMedication", "addAnotherMedication").forEach {
            compose.onNodeWithTag(it, useUnmergedTree = true).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
        compose.onNodeWithTag("medNotes", useUnmergedTree = true).performScrollTo()
        listOf("cancelMedication", "saveMedication", "addAnotherMedication").forEach {
            compose.onNodeWithTag(it, useUnmergedTree = true).assertIsDisplayed()
        }
        val cancel = compose.onNodeWithTag("cancelMedication", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val save = compose.onNodeWithTag("saveMedication", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val another = compose.onNodeWithTag("addAnotherMedication", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("order Cancel, Save, Save and add another", cancel.right <= save.left + 1f && save.right <= another.left + 1f)
    }
}
