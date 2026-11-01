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
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_NON_COMPLIANT
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.MedicationDraftField
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.MedicationEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Oracles T400-T405 of SPEC_DESIGN_R5.md (Round 5c), written from the spec only. */
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
    fun t400_newMedicationOpensDoseRowWithRouteOralAndOtherRowsClosed() {
        host()
        exists("medOpen-dose")
        compose.onNodeWithTag("medDosage", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        exists("medRow-route")
        compose.onNode(
            hasText("Oral") and (hasTestTag("medRow-route") or hasAnyAncestor(hasTestTag("medRow-route"))),
            useUnmergedTree = true,
        ).assertExists()
        exists("medRow-frequency")
        exists("medRow-start")
        compose.onNode(
            hasTestTag("medName") and hasAnyAncestor(hasTestTag("medPickCard")),
            useUnmergedTree = true,
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun t401_presetFillsNameAndDoseAndOpensFirstBlankRowNotDose() {
        host()
        compose.onNodeWithTag("medPreset-0", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        val v = state.medicationDraft!!.values
        assertTrue("preset should fill name", v.name.isNotBlank())
        assertTrue("preset should fill dose", v.dosage.isNotBlank())
        missing("medOpen-dose")
        // First blank row in fixed order dose, route, frequency, start (all four after a preset normally filled).
        val expected = listOf(
            "dose" to v.dosage,
            "route" to v.route,
            "frequency" to v.frequency,
            "start" to v.startDate,
        ).firstOrNull { it.second.isBlank() }?.first
        if (expected != null) {
            exists("medOpen-$expected")
        } else {
            listOf("dose", "route", "frequency", "start").forEach { missing("medOpen-$it") }
        }
    }

    @Test
    fun t402_tappingFrequencyRowOpensItAndClosesDose() {
        host()
        exists("medOpen-dose")
        compose.onNodeWithTag("medRow-frequency", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        exists("medOpen-frequency")
        missing("medOpen-dose")
    }

    @Test
    fun t403_fourStatusButtonsNoProphylaxisFourthSelectsNonCompliant() {
        host()
        compose.onNodeWithTag("medStatusChoice-0", useUnmergedTree = true).performScrollTo()
        assertEquals(
            4,
            compose.onAllNodesWithTag("medStatusChoice-0", useUnmergedTree = true).fetchSemanticsNodes().size +
                compose.onAllNodesWithTag("medStatusChoice-1", useUnmergedTree = true).fetchSemanticsNodes().size +
                compose.onAllNodesWithTag("medStatusChoice-2", useUnmergedTree = true).fetchSemanticsNodes().size +
                compose.onAllNodesWithTag("medStatusChoice-3", useUnmergedTree = true).fetchSemanticsNodes().size,
        )
        missing("medStatusChoice-4")
        compose.onNode(hasText("Prophylaxis", substring = true, ignoreCase = true), useUnmergedTree = true)
            .assertDoesNotExist()
        compose.onNodeWithTag("medStatusChoice-3", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        val v = state.medicationDraft!!.values
        assertEquals(MEDICATION_STATUS_NON_COMPLIANT, v.status)
        assertTrue(v.isNonCompliant)
    }

    @Test
    fun t404_moreSectionOpenOnEntryOnlyWhenEndDateOrAdverseNotesPresent() {
        host { change(it, MedicationDraftField.END_DATE, "2026-12-01") }
        exists("medMoreSection")

        host { change(it, MedicationDraftField.ADVERSE_NOTES, "Nausea") }
        exists("medMoreSection")
    }

    @Test
    fun t404b_moreSectionClosedByDefaultAndToggleShowsIt() {
        host()
        missing("medMoreSection")
        compose.onNodeWithTag("medMoreToggle", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        exists("medMoreSection")
    }

    @Test
    fun t405_actionRowButtonsAreDisplayed() {
        host()
        listOf("saveMedication", "cancelMedication", "addAnotherMedication").forEach {
            compose.onNodeWithTag(it, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        }
    }
}
