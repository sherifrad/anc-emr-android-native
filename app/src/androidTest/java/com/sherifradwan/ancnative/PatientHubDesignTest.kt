package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.PatientHubScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatientHubDesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun patientHub_rendersCanonicalHeaderAndAllSixTabs() {
        val patient = DemoFixtures.patients.first()
        val state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 7001L),
        )

        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    PatientHubScreen(
                        state = state,
                        route = AncRoute.Workspace(patient.id),
                        onEvent = {},
                        onStartVisit = { _, _ -> },
                    )
                }
            }
        }
        compose.waitForIdle()

        // Verify Patient Hub orchestrator and patient name
        compose.onNodeWithTag("patientHubScreen").assertIsDisplayed()
        compose.onNodeWithText(patient.displayName).assertIsDisplayed()
        compose.onNodeWithTag("hubReportButton").assertIsDisplayed()
        compose.onNodeWithTag("hubBackButton").assertIsDisplayed()

        // Verify all 6 canonical tabs
        compose.onNodeWithTag("tab-overview").assertIsDisplayed()
        compose.onNodeWithTag("tab-visits").assertIsDisplayed()
        compose.onNodeWithTag("tab-scans").assertIsDisplayed()
        compose.onNodeWithTag("tab-labs").assertIsDisplayed()
        compose.onNodeWithTag("tab-meds").assertIsDisplayed()
        compose.onNodeWithTag("tab-plan").assertIsDisplayed()

        // Default tab is OVERVIEW
        compose.onNodeWithTag("overviewTab").assertIsDisplayed()
    }

    @Test
    fun patientHub_tabNavigation_switchesTabs() {
        val patient = DemoFixtures.patients.first()
        val state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 7001L),
        )

        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    PatientHubScreen(
                        state = state,
                        route = AncRoute.Workspace(patient.id),
                        onEvent = {},
                        onStartVisit = { _, _ -> },
                    )
                }
            }
        }
        compose.waitForIdle()

        // Click VISITS tab
        compose.onNodeWithTag("tab-visits").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("visitsTab").assertIsDisplayed()

        // Click LABS tab
        compose.onNodeWithTag("tab-labs").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("labsTab").assertIsDisplayed()

        // Click MEDS tab
        compose.onNodeWithTag("tab-meds").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("medsTab").assertIsDisplayed()

        // Click PLAN tab
        compose.onNodeWithTag("tab-plan").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("planTab").assertIsDisplayed()
    }
}
