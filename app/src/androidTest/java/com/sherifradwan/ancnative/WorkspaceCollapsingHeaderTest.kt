package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.WorkspaceScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceCollapsingHeaderTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun workspaceHeader_displaysPatientNameWithoutDemoCaseId() {
        val patient = DemoFixtures.patients.first()
        val state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 9001L),
            route = AncRoute.Workspace(patient.id, WorkspaceSection.DATING),
        )

        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    WorkspaceScreen(
                        state = state,
                        route = state.route as AncRoute.Workspace,
                        onEvent = { },
                        onStartVisit = { _, _ -> },
                    )
                }
            }
        }
        compose.waitForIdle()

        // Verify patient display name is present
        compose.onNodeWithText(patient.displayName).assertIsDisplayed()

        // Verify DEMO-ANC-001 or caseId is NOT displayed in the workspace header
        compose.onAllNodesWithText("DEMO-ANC-001").assertCountEquals(0)
    }

    @Test
    fun workspaceLabsTab_displaysGroupedReportsAndCompositePanels() {
        val basePatient = DemoFixtures.patients.first()
        val cbcLabs = listOf(
            DemoLabInvestigation(
                id = "lab-cbc-1",
                patientId = basePatient.id,
                date = "2026-11-05",
                labName = "Al Borg Lab",
                testCategory = "Hematology",
                testName = "Hemoglobin (Hb)",
                result = "10.2",
                unit = "g/dL",
                referenceRange = "11.0-15.0",
                isAbnormal = true,
                notes = null,
            ),
            DemoLabInvestigation(
                id = "lab-cbc-2",
                patientId = basePatient.id,
                date = "2026-11-05",
                labName = "Al Borg Lab",
                testCategory = "Hematology",
                testName = "Platelet Count",
                result = "240",
                unit = "x10^3/uL",
                referenceRange = "150-450",
                isAbnormal = false,
                notes = null,
            ),
            DemoLabInvestigation(
                id = "lab-single-1",
                patientId = basePatient.id,
                date = "2026-11-05",
                labName = "Al Borg Lab",
                testCategory = "Chemistry",
                testName = "Serum Ferritin",
                result = "18",
                unit = "ng/mL",
                referenceRange = "15-150",
                isAbnormal = false,
                notes = null,
            ),
        )
        val patientWithLabs = basePatient.copy(labInvestigations = cbcLabs)
        val state = AncState(
            patients = listOf(patientWithLabs),
            vaultState = VaultState.Unlocked(sessionEpoch = 9002L),
            route = AncRoute.Workspace(patientWithLabs.id, WorkspaceSection.LABORATORY),
        )

        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                var currentRoute by remember { mutableStateOf(AncRoute.Workspace(patientWithLabs.id, WorkspaceSection.LABORATORY)) }
                AncTheme {
                    WorkspaceScreen(
                        state = state.copy(route = currentRoute),
                        route = currentRoute,
                        onEvent = { event ->
                            if (event is AncEvent.SelectSection) {
                                currentRoute = currentRoute.copy(section = event.section)
                            }
                        },
                        onStartVisit = { _, _ -> },
                    )
                }
            }
        }
        compose.waitForIdle()

        // Verify action buttons
        compose.onNodeWithTag("addLab").assertIsDisplayed()
        compose.onNodeWithTag("startLabBatch").assertIsDisplayed()

        // Ensure we scroll to the report card if it's below the fold on narrow screens
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasText("Al Borg Lab", substring = true))
        compose.waitForIdle()

        // Verify report card is shown with Lab name and Date
        compose.onNodeWithText("Al Borg Lab", substring = true).assertIsDisplayed()
        compose.onNodeWithText("2026-11-05", substring = true).assertIsDisplayed()

        // Verify composite panel grouping header is rendered
        compose.onNodeWithText("Complete Blood Count (CBC)").assertIsDisplayed()

        // Scroll to individual test section if needed
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasText("Serum Ferritin"))
        compose.waitForIdle()

        // Verify individual test section is rendered
        compose.onNodeWithText("INDIVIDUAL INVESTIGATIONS").assertIsDisplayed()
        compose.onNodeWithText("Serum Ferritin").assertIsDisplayed()
    }
}
