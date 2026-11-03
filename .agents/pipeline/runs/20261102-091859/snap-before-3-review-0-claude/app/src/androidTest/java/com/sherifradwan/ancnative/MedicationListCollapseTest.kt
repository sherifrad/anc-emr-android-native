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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.WorkspaceScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Oracles T500-T503 of SPEC_DESIGN_R5.md (Round 5d), written from the spec only. */
@RunWith(AndroidJUnit4::class)
class MedicationListCollapseTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private var state by mutableStateOf(AncState(patients = emptyList()))

    private fun meds(count: Int, patientId: String) = (1..count).map { i ->
        DemoMedicationRecord(
            id = "med-collapse-$i",
            patientId = patientId,
            name = "Collapse Drug $i",
            dosage = "${i}00 mg",
            route = "Oral",
            frequency = "Twice daily",
            startDate = "2026-03-0$i",
            endDate = null,
            status = MEDICATION_STATUS_ACTIVE,
            notes = null,
            isProphylaxis = false,
        )
    }

    private fun host(count: Int) {
        val base = DemoFixtures.patients.first()
        val patient = base.copy(medications = meds(count, base.id))
        state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 6500L),
            route = AncRoute.Workspace(patient.id, WorkspaceSection.MEDICATIONS),
        )
        val route = state.route as AncRoute.Workspace
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    WorkspaceScreen(
                        state = state,
                        route = route,
                        onEvent = { state = AncReducer.reduce(state, it) },
                        onStartVisit = { _, _ -> },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun scrollTo(tag: String) {
        compose.onNodeWithTag("workspaceList", useUnmergedTree = true)
            .performScrollToNode(hasTestTag(tag))
    }

    private fun dosageUnder(id: String): Int =
        compose.onAllNodes(
            hasText("Dosage") and hasAnyAncestor(hasTestTag("medication-$id")),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().size

    @Test
    fun t500_twoMedicationsAreBothExpandedWithFullDetails() {
        host(2)
        scrollTo("medication-med-collapse-1")
        scrollTo("medication-med-collapse-2")
        assertEquals(1, dosageUnder("med-collapse-1"))
        assertEquals(1, dosageUnder("med-collapse-2"))
        compose.onNodeWithTag("medicationsToggleAll", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun t501_fourMedicationsStartCollapsedAndToggleExpandsOnlyThatCard() {
        host(4)
        scrollTo("medicationsToggleAll")
        compose.onNodeWithTag("medicationsToggleAll", useUnmergedTree = true).assertExists()
        for (i in 1..4) {
            scrollTo("medication-med-collapse-$i")
            compose.onNodeWithTag("medication-med-collapse-$i", useUnmergedTree = true).assertExists()
            assertEquals(0, dosageUnder("med-collapse-$i"))
        }
        scrollTo("medicationToggle-med-collapse-2")
        compose.onNodeWithTag("medicationToggle-med-collapse-2", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        scrollTo("medication-med-collapse-2")
        assertEquals(1, dosageUnder("med-collapse-2"))
        for (i in listOf(1, 3, 4)) {
            scrollTo("medication-med-collapse-$i")
            assertEquals(0, dosageUnder("med-collapse-$i"))
        }
    }

    @Test
    fun t502_toggleAllExpandsEveryCardThenCollapsesEveryCard() {
        host(4)
        scrollTo("medicationsToggleAll")
        compose.onNodeWithTag("medicationsToggleAll", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        for (i in 1..4) {
            scrollTo("medication-med-collapse-$i")
            assertEquals(1, dosageUnder("med-collapse-$i"))
        }
        scrollTo("medicationsToggleAll")
        compose.onNodeWithTag("medicationsToggleAll", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        for (i in 1..4) {
            scrollTo("medication-med-collapse-$i")
            assertEquals(0, dosageUnder("med-collapse-$i"))
        }
    }

    @Test
    fun t503_editPencilIsDisplayedWhileCollapsed() {
        host(4)
        for (i in 1..4) {
            scrollTo("medication-med-collapse-$i")
            assertEquals(0, dosageUnder("med-collapse-$i"))
            scrollTo("editMedication-med-collapse-$i")
            compose.onNodeWithTag("editMedication-med-collapse-$i", useUnmergedTree = true).assertIsDisplayed()
        }
    }
}
