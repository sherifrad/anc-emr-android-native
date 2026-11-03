package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.LabDraftField
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.LabEditor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Oracles T300-T306 of SPEC_DESIGN_R5.md (Round 5b), written from the spec only. */
@RunWith(AndroidJUnit4::class)
class LabEntryDesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patient = DemoFixtures.patients.first()
    private var state by mutableStateOf(AncState(patients = emptyList()))

    /** Opens a new lab with the real reducer; `setup` may pre-fill the draft before the screen mounts. */
    private fun host(setup: (AncState) -> AncState = { it }) {
        var s = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 6001L),
        )
        s = AncReducer.reduce(
            s,
            AncEvent.StartLabEditor(
                patientId = patient.id,
                labId = null,
                newLabId = "lab-design-1",
                editorSessionId = "lab-design-session-1",
            ),
        )
        state = setup(s)
        val route = state.route as AncRoute.LabEditor
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    LabEditor(
                        state = state,
                        route = route,
                        onEvent = { state = AncReducer.reduce(state, it) },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun change(s: AncState, field: LabDraftField, value: String): AncState =
        AncReducer.reduce(s, AncEvent.LabDraftChanged(field, value))

    private fun loadedHemoglobin(s: AncState): AncState {
        var r = change(s, LabDraftField.NAME, "Hemoglobin")
        r = change(r, LabDraftField.UNIT, "g/dL")
        return change(r, LabDraftField.REFERENCE_RANGE, "11-14")
    }

    private fun exists(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
    private fun missing(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).assertDoesNotExist()

    @Test
    fun t300_newLabShowsDetailsToggleNameResultAndAddTestsOpen() {
        host()
        exists("labEditDetailsToggle")
        compose.onNodeWithTag("labName", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("labResult", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        exists("labAddTestsToggle")
        // Blank name and no queue: add-tests content is open on entry.
        exists("allLabTestsChip")
    }

    @Test
    fun t301_loadedTestShowsTitleWithDetailsAndAddTestsClosed() {
        host { loadedHemoglobin(it) }
        compose.onNodeWithTag("labTestTitle", useUnmergedTree = true).performScrollTo()
        compose.onNode(
            hasText("Hemoglobin") and hasAnyAncestor(hasTestTag("labTestTitle")) or
                (hasTestTag("labTestTitle") and hasText("Hemoglobin")),
            useUnmergedTree = true,
        ).assertExists()
        missing("labName")
        missing("allLabTestsChip")
    }

    @Test
    fun t302_detailsToggleRevealsNameUnitAndRange() {
        host { loadedHemoglobin(it) }
        missing("labName")
        compose.onNodeWithTag("labEditDetailsToggle", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        exists("labName")
        exists("labUnit")
        exists("labRefRange")
    }

    @Test
    fun t303_positivePresetSetsResult() {
        host { loadedHemoglobin(it) }
        // Preset order: Negative(0), Non-reactive(1), Normal(2), Positive(3).
        compose.onNodeWithTag("labResultPreset-3", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("labResult", useUnmergedTree = true).performScrollTo().assertTextContains("Positive")
        assertEquals("Positive", state.labDraft?.values?.result)
    }

    @Test
    fun t304_resultBelowRangeIsFlaggedAbnormal() {
        host { loadedHemoglobin(change(it, LabDraftField.RESULT, "9")) }
        compose.waitForIdle()
        if (compose.onAllNodesWithTag("labAbnormal", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithTag("labFlagToggle", useUnmergedTree = true).performScrollTo().performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithTag("labAbnormal", useUnmergedTree = true).performScrollTo().assertIsOn()
    }

    @Test
    fun t305_noteToggleOpensNotesAndTypedTextReachesDraft() {
        host { loadedHemoglobin(it) }
        missing("labNotes")
        compose.onNodeWithTag("labNoteToggle", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("labNotes", useUnmergedTree = true).performScrollTo().performTextInput("repeat in 4 weeks")
        compose.waitForIdle()
        assertEquals("repeat in 4 weeks", state.labDraft?.values?.notes)
    }

    @Test
    fun t306_actionBarStillPresent() {
        host()
        exists("saveLab")
        exists("cancelLab")
        compose.onNodeWithTag("saveLab", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("cancelLab", useUnmergedTree = true).assertIsDisplayed()
    }
}
