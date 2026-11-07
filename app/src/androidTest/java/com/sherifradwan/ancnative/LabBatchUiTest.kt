package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.LabBatchTab
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.LabBatchScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LabBatchUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patient = DemoFixtures.patients.first()
    private var state by mutableStateOf(AncState(patients = emptyList()))

    private fun host(setup: (AncState) -> AncState = { it }) {
        var s = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 7001L),
        )
        s = AncReducer.reduce(
            s,
            AncEvent.StartLabBatch(
                patientId = patient.id,
                sessionId = "batch-session-1",
                basePatientRevision = 1L,
                date = "2026-11-07",
                labName = "Al Borg",
            ),
        )
        state = setup(s)
        val route = AncRoute.LabBatchEditor(patient.id)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    LabBatchScreen(
                        state = state,
                        route = route,
                        onEvent = { state = AncReducer.reduce(state, it) },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun addCbcThenEnterResultAndReviewDisplaysGroupedPanel() {
        host()
        compose.onNodeWithTag("labBatchScreen").assertIsDisplayed()

        // Verify initially Save is disabled
        compose.onNodeWithTag("labBatchSave").assertIsNotEnabled()

        // Tap Add 6 on CBC panel
        compose.onNodeWithTag("labBatchAdd-group_cbc").performClick()
        compose.waitForIdle()

        // Should automatically navigate to Enter tab with cursor at test 1 (Hb)
        assertEquals(LabBatchTab.ENTER, state.labBatchDraft?.tab)
        assertEquals(6, state.labBatchDraft?.items?.size)

        // Enter result for Hb
        compose.onNodeWithTag("labBatchResult").performTextInput("10.8")
        compose.waitForIdle()

        // Switch to Review tab
        compose.onNodeWithTag("labBatchTab-REVIEW").performClick()
        compose.waitForIdle()

        assertEquals(LabBatchTab.REVIEW, state.labBatchDraft?.tab)
        compose.onNodeWithText("CBC").assertIsDisplayed()
        compose.onNodeWithText("10.8").assertIsDisplayed()

        // Save button should now be enabled with "Save 1 result"
        compose.onNodeWithTag("labBatchSave").assertIsEnabled()
        compose.onNodeWithTag("labBatchSave").assertTextContains("Save 1 result")
    }

    @Test
    fun qualitativeChoicesRenderForDipstickUrineTests() {
        host()

        // Add Urine panel
        compose.onNodeWithTag("labBatchAdd-group_urine").performClick()
        compose.waitForIdle()

        assertEquals(LabBatchTab.ENTER, state.labBatchDraft?.tab)

        // First urine test is Urine Protein, which has qualitative choices
        compose.onNodeWithText("Normal").assertIsDisplayed()
        compose.onNodeWithText("Trace").assertIsDisplayed()
        compose.onNodeWithText("+").assertIsDisplayed()

        // Tap Trace
        compose.onNodeWithText("Trace").performClick()
        compose.waitForIdle()

        val draft = state.labBatchDraft!!
        val currentItem = draft.currentItem!!
        assertEquals("Trace", currentItem.result)
        assertTrue("Trace is abnormal", currentItem.isAbnormal)
    }

    @Test
    fun chooseSubsetDialogAddsOnlySelectedTests() {
        host()

        // Click Choose on CBC panel
        compose.onAllNodesWithText("Choose")[0].performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Choose tests").assertIsDisplayed()
        compose.onNodeWithText("CBC · 6 elements").assertIsDisplayed()

        // Deselect MCV by clicking on it
        compose.onNodeWithText("MCV").performClick()
        compose.waitForIdle()

        // Now 5 are selected
        compose.onNodeWithText("Add 5 selected").assertIsDisplayed()
        compose.onNodeWithText("Add 5 selected").performClick()
        compose.waitForIdle()

        val draft = state.labBatchDraft!!
        assertEquals(5, draft.items.size)
        assertTrue("MCV was excluded", draft.items.none { it.testName == "MCV" })
    }

    @Test
    fun markEmptyAsNormalOnlyFillsQualitativeTests() {
        host()

        // Add Urine panel (8 qualitative dipstick tests)
        compose.onNodeWithTag("labBatchAdd-group_urine").performClick()
        compose.waitForIdle()

        // Enter result for first test (Urine Protein) as Trace
        compose.onNodeWithText("Trace").performClick()
        compose.waitForIdle()

        // Switch to Review tab
        compose.onNodeWithTag("labBatchTab-REVIEW").performClick()
        compose.waitForIdle()

        // 7 empty dipstick tests remaining
        compose.onNodeWithTag("labBatchMarkNormal").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("labBatchMarkNormal").assertTextContains("Mark 7 empty as normal")

        // Click mark normal
        compose.onNodeWithTag("labBatchMarkNormal").performClick()
        compose.waitForIdle()

        val draft = state.labBatchDraft!!
        assertEquals(8, draft.filledCount)
        assertEquals("Trace", draft.item(draft.items[0].labId)?.result)
        assertEquals("Negative", draft.item(draft.items[1].labId)?.result)
        assertEquals("Negative", draft.item(draft.items[2].labId)?.result)
    }
}
