package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.MedicationDraftField
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.PatientHubScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Medication entry oracles T400-T405, rewritten 2026-11-10 for the medication sheet: a new medication opens the
 * shared form in a bottom sheet over the Meds tab, and the route never changes.
 */
@RunWith(AndroidJUnit4::class)
class MedicationEntryDesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patient = DemoFixtures.patients.first()
    private val medsTab = AncRoute.Workspace(patient.id, WorkspaceSection.MEDICATIONS)
    private var state by mutableStateOf(AncState(patients = emptyList()))

    /** Opens a new medication with the real reducer; `setup` may change the draft before the screen mounts. */
    private fun host(setup: (AncState) -> AncState = { it }) {
        var s = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 6002L),
            route = medsTab,
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
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    PatientHubScreen(
                        state = state,
                        route = state.route as AncRoute.Workspace,
                        onEvent = { state = AncReducer.reduce(state, it) },
                        onStartVisit = { _, _ -> },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun change(s: AncState, field: MedicationDraftField, value: String): AncState =
        AncReducer.reduce(s, AncEvent.MedicationDraftChanged(field, value))

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    private fun missing(tag: String) = node(tag).assertDoesNotExist()

    @Test
    fun t400_newMedicationOpensTheSheetOverTheMedsTabWithOralAndTodaysStartDate() {
        host()
        node("medicationSheet").assertIsDisplayed()
        node("workspaceScreen").assertExists()
        assertEquals(medsTab, state.route)
        node("medName").performScrollTo().assertIsDisplayed()
        node("medRouteChoice-0").performScrollTo().assertIsSelected()
        compose.onNode(hasText("Oral") and hasAnyAncestor(hasTestTag("medRouteChoice-0")), useUnmergedTree = true).assertExists()
        assertEquals(LocalDate.now().toString(), state.medicationDraft!!.values.startDate)
    }

    @Test
    fun t401_cancelOnAnUntouchedSheetClosesItAndStaysOnTheTab() {
        host()
        node("cancelMedication").performClick()
        compose.waitForIdle()
        missing("medicationSheet")
        missing("medDiscardDialog")
        assertEquals(medsTab, state.route)
        node("workspaceScreen").assertIsDisplayed()
    }

    @Test
    fun t402_cancelAfterAnEditAsksOnceAndDiscardStaysOnTheTab() {
        host { change(it, MedicationDraftField.NAME, "Iron") }
        assertTrue(state.medicationDraft!!.touched)
        node("cancelMedication").performClick()
        compose.waitForIdle()
        node("medDiscardDialog").assertIsDisplayed()
        node("medDiscardConfirm").performClick()
        compose.waitForIdle()
        missing("medicationSheet")
        assertFalse("the reducer must not ask a second time", state.showDiscardDialog)
        assertEquals(medsTab, state.route)
    }

    @Test
    fun t403_twoStatusButtonsAndStoppedStampsTodayAsTheStopDate() {
        host()
        node("medStatusChoice-0").performScrollTo().assertIsDisplayed()
        missing("medStatusChoice-2")
        compose.onNode(hasText("Prophylaxis", substring = true, ignoreCase = true), useUnmergedTree = true).assertDoesNotExist()
        node("medStatusChoice-1").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("Discontinued", state.medicationDraft!!.values.status)
        assertEquals(LocalDate.now().toString(), state.medicationDraft!!.values.endDate)
        node("medStatusChoice-0").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("Active", state.medicationDraft!!.values.status)
        assertEquals("", state.medicationDraft!!.values.endDate)
    }

    @Test
    fun t404_oneNotesFieldAndNoAdverseNotesFieldWhileHiddenValuesStay() {
        host { change(it, MedicationDraftField.ADVERSE_NOTES, "Nausea") }
        listOf("medMoreToggle", "medMoreSection", "medAdverseNotes", "medEndDate", "medStartDate").forEach { missing(it) }
        assertEquals(1, compose.onAllNodesWithTag("medNotes", useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals("Nausea", state.medicationDraft!!.values.adverseNotes)
    }

    @Test
    fun t405_cancelSaveAndSaveAndAddAnotherAreAtLeast48dpAndStayVisible() {
        host()
        listOf("cancelMedication", "saveMedication", "addAnotherMedication").forEach {
            node(it).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
        node("medNotes").performScrollTo()
        listOf("cancelMedication", "saveMedication", "addAnotherMedication").forEach { node(it).assertIsDisplayed() }
        val cancel = node("cancelMedication").fetchSemanticsNode().boundsInRoot
        val save = node("saveMedication").fetchSemanticsNode().boundsInRoot
        val another = node("addAnotherMedication").fetchSemanticsNode().boundsInRoot
        assertTrue("Cancel left of Save", cancel.right <= save.left + 1f)
        assertTrue("Save and add another under them", save.bottom <= another.top + 1f)
    }
}
