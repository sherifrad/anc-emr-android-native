package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.state.AncReducer
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
class WorkspaceScreenDesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun workspaceScreen_rendersBedsideHeaderAnd5Tabs() {
        val patient = DemoFixtures.patients.first()
        var state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 8001L),
            route = AncRoute.Workspace(patient.id, WorkspaceSection.DATING),
        )

        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    WorkspaceScreen(
                        state = state,
                        route = state.route as AncRoute.Workspace,
                        onEvent = { state = AncReducer.reduce(state, it) },
                        onStartVisit = { _, _ -> },
                    )
                }
            }
        }
        compose.waitForIdle()

        // Verify Bedside TopBar elements
        compose.onNodeWithTag("workspaceScreen").assertIsDisplayed()
        compose.onNodeWithText(patient.displayName).assertIsDisplayed()
        compose.onNodeWithTag("workspaceReportButton").assertIsDisplayed()
        compose.onNodeWithTag("workspaceProfile").assertIsDisplayed()

        // Verify 5 fixed bedside tabs
        compose.onNodeWithTag("tab-history").assertIsDisplayed()
        compose.onNodeWithTag("tab-encounters").assertIsDisplayed()
        compose.onNodeWithTag("tab-labs").assertIsDisplayed()
        compose.onNodeWithTag("tab-meds").assertIsDisplayed()
        compose.onNodeWithTag("tab-plan").assertIsDisplayed()
    }

    @Test
    fun workspaceScreen_encountersTab_switchesFiltersAndShowsActions() {
        val patient = DemoFixtures.patients.first()
        var state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 8002L),
            route = AncRoute.Workspace(patient.id, WorkspaceSection.VISITS),
        )

        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    WorkspaceScreen(
                        state = state,
                        route = state.route as AncRoute.Workspace,
                        onEvent = { state = AncReducer.reduce(state, it) },
                        onStartVisit = { _, _ -> },
                    )
                }
            }
        }
        compose.waitForIdle()

        // Verify Encounters actions and filter pills
        compose.onNodeWithTag("tab-encounters").assertIsDisplayed()
        compose.onNodeWithTag("filterEncounters-all").assertIsDisplayed()
        compose.onNodeWithTag("filterEncounters-visits").assertIsDisplayed()
        compose.onNodeWithTag("filterEncounters-scans").assertIsDisplayed()
        compose.onNodeWithTag("addVisit").assertIsDisplayed()
        compose.onNodeWithTag("addUltrasound").assertIsDisplayed()

        // Click Visits filter
        compose.onNodeWithTag("filterEncounters-visits").performClick()
        compose.waitForIdle()

        // Click Scans filter
        compose.onNodeWithTag("filterEncounters-scans").performClick()
        compose.waitForIdle()

        // Click All filter
        compose.onNodeWithTag("filterEncounters-all").performClick()
        compose.waitForIdle()
    }

    @Test
    fun workspaceScreen_tabSwitching_navigatesContent() {
        val patient = DemoFixtures.patients.first()
        var state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 8003L),
            route = AncRoute.Workspace(patient.id, WorkspaceSection.DATING),
        )

        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    WorkspaceScreen(
                        state = state,
                        route = state.route as AncRoute.Workspace,
                        onEvent = { state = AncReducer.reduce(state, it) },
                        onStartVisit = { _, _ -> },
                    )
                }
            }
        }
        compose.waitForIdle()

        // Switch to Labs tab
        compose.onNodeWithTag("tab-labs").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("addLab").assertIsDisplayed()

        // Switch to Meds tab
        compose.onNodeWithTag("tab-meds").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("medTapToAddCard").assertIsDisplayed()

        // Switch to Plan tab
        compose.onNodeWithTag("tab-plan").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("deliveryPlanningCard").assertIsDisplayed()
        compose.onNodeWithTag("workspaceList").performScrollToNode(hasTestTag("attachmentsSectionCard"))
        compose.onNodeWithTag("attachmentsSectionCard").assertIsDisplayed()

        // Switch back to History tab
        compose.onNodeWithTag("tab-history").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("editDating").assertIsDisplayed()
    }
}
