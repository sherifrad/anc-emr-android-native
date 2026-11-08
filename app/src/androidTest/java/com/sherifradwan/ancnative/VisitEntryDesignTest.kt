package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.VisitEditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisitEntryDesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patientWithHistory = DemoFixtures.patients.first().copy(
        visits = listOf(
            DemoVisit(
                id = "past-v1",
                date = "2026-10-15",
                bloodPressure = "118/76",
                weight = "74.5",
                findings = "FHR 144 bpm · Oedema none · Movements good · Urine negative",
                notes = "Past note",
                revision = 1,
                isSyntheticEdit = false,
            ),
        ),
    )

    private var state by mutableStateOf(AncState(patients = listOf(patientWithHistory)))

    private fun host(setup: (AncState) -> AncState = { it }) {
        var s = AncState(
            patients = listOf(patientWithHistory),
            vaultState = VaultState.Unlocked(sessionEpoch = 7001L),
        )
        s = AncReducer.reduce(
            s,
            AncEvent.StartVisit(
                patientId = patientWithHistory.id,
                visitId = null,
                editorSessionId = "visit-session-1",
            ),
        )
        state = setup(s)
        val route = state.route as AncRoute.VisitEditor
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    VisitEditor(
                        state = state,
                        route = route,
                        onEvent = { state = AncReducer.reduce(state, it) },
                        onSave = {},
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun stepNavigationAdvancesAndJumpsViaSegments() {
        host()
        // Step 0: Date and BP
        compose.onNodeWithText("Step 1 of 4").assertIsDisplayed()
        compose.onNodeWithTag("visitDate").assertIsDisplayed()
        compose.onNodeWithTag("nextStep").performClick()
        compose.waitForIdle()

        // Step 1: Weight and FH
        compose.onNodeWithText("Step 2 of 4").assertIsDisplayed()
        compose.onNodeWithTag("visitWeight").assertIsDisplayed()
        compose.onNodeWithTag("nextStep").performClick()
        compose.waitForIdle()

        // Step 2: Findings
        compose.onNodeWithText("Step 3 of 4").assertIsDisplayed()
        compose.onNodeWithTag("fillNormalValues").assertIsDisplayed()
        compose.onNodeWithTag("nextStep").performClick()
        compose.waitForIdle()

        // Step 3: Review and Save
        compose.onNodeWithText("Step 4 of 4").assertIsDisplayed()
        compose.onNodeWithTag("saveVisit").assertIsDisplayed()

        // Jump back to step 1 via SegmentedProgressBar
        compose.onNodeWithTag("stepSegment-0").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Step 1 of 4").assertIsDisplayed()
        compose.onNodeWithTag("visitDate").assertIsDisplayed()
    }

    @Test
    fun bloodPressureHighBpShowsWarningBox() {
        host()
        // Enter 150/95
        compose.onNodeWithTag("visitSystolic").performTextReplacement("150")
        compose.onNodeWithTag("visitDiastolic").performTextReplacement("95")
        compose.waitForIdle()

        compose.onNodeWithTag("highBpWarning").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("High BP is flagged on the visit. You can still save.").performScrollTo().assertIsDisplayed()

        // Advance to Step 3 to verify high BP never blocks saving (O3)
        compose.onNodeWithTag("nextStep").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("nextStep").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("nextStep").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("saveVisit").performScrollTo().assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun sameAsLastChipsPopulateHistoricalValues() {
        host()
        // Step 0: Click "Same as last (118/76)"
        compose.onNodeWithTag("bpPreset-sameLast").performClick()
        compose.waitForIdle()
        assertEquals("118", state.draft?.values?.systolic)
        assertEquals("76", state.draft?.values?.diastolic)
        assertEquals("118/76", state.draft?.values?.bloodPressure)

        // Advance to Step 1
        compose.onNodeWithTag("nextStep").performClick()
        compose.waitForIdle()

        // Click "Same as last (74.5 kg)"
        compose.onNodeWithTag("weightPreset-sameLast").performClick()
        compose.waitForIdle()
        assertEquals("74.5", state.draft?.values?.weight)

        // Click "Same as last (144 bpm)"
        compose.onNodeWithTag("fhrPreset-sameLast").performClick()
        compose.waitForIdle()
        assertEquals("144", state.draft?.values?.fetalHeartRate)
    }

    @Test
    fun fillNormalValuesFillsOnlyBlanks() {
        // Pre-set existing non-blank oedema and vitals
        host { s ->
            s.copy(
                draft = s.draft?.copy(
                    values = s.draft!!.values.copy(
                        bloodPressure = "120/80",
                        weight = "72",
                        oedema = "Marked",
                    ),
                ),
            )
        }
        // Jump to Step 2
        compose.onNodeWithTag("stepSegment-2").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("fillNormalValues").performClick()
        compose.waitForIdle()

        // Oedema Marked was NOT overwritten; movements and urine were filled; vitals untouched
        assertEquals("Marked", state.draft?.values?.oedema)
        assertEquals("Good", state.draft?.values?.fetalMovements)
        assertEquals("Negative", state.draft?.values?.urineProtein)
        assertEquals("120/80", state.draft?.values?.bloodPressure)
        assertEquals("72", state.draft?.values?.weight)
    }

    @Test
    fun reviewStepShowsSummaryAndFlaggedPills() {
        host()
        // Step 0: Set elevated BP 145/92
        compose.onNodeWithTag("visitSystolic").performTextReplacement("145")
        compose.onNodeWithTag("visitDiastolic").performTextReplacement("92")
        compose.waitForIdle()

        // Advance to Step 3: Review and save
        compose.onNodeWithTag("nextStep").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("nextStep").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("nextStep").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("visitSummaryCard").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("High BP", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("saveVisit").performScrollTo().assertIsDisplayed()
    }
}
