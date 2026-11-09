package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.PatientHubScreen
import com.sherifradwan.ancnative.ui.patienthub.PatientHubTab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatientHubDesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patient = DemoFixtures.patients.first()

    private fun showHub(section: WorkspaceSection) {
        val state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 7001L),
        )
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    PatientHubScreen(
                        state = state,
                        route = AncRoute.Workspace(patient.id, section),
                        onEvent = {},
                        onStartVisit = { _, _ -> },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun patientHub_rendersCanonicalHeaderAndAllSixTabs() {
        showHub(WorkspaceSection.EPISODE_OVERVIEW)

        compose.onNodeWithTag("workspaceScreen").assertIsDisplayed()
        compose.onNodeWithText(patient.displayName).assertIsDisplayed()
        compose.onNodeWithTag("workspaceReportButton").assertIsDisplayed()
        compose.onNodeWithTag("hubBackButton").assertIsDisplayed()

        PatientHubTab.entries.forEach { compose.onNodeWithTag(it.testTag).performScrollTo().assertIsDisplayed() }

        compose.onNodeWithTag(PatientHubTab.OVERVIEW.contentTag()).assertIsDisplayed()
    }

    @Test
    fun patientHub_tabNavigation_switchesTabs() {
        showHub(WorkspaceSection.EPISODE_OVERVIEW)

        listOf(PatientHubTab.VISITS, PatientHubTab.LABS, PatientHubTab.MEDS, PatientHubTab.PLAN).forEach { tab ->
            compose.openHubTab(tab)
            compose.onNodeWithTag(tab.contentTag()).assertIsDisplayed()
        }
    }
}
