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
import com.sherifradwan.ancnative.ui.ProfileScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenDesignTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun profileScreen_rendersHeroCardAndActions() {
        val patient = DemoFixtures.patients.first()
        val state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 7001L),
        )

        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    ProfileScreen(
                        state = state,
                        route = AncRoute.Profile(patient.id),
                        onEvent = {},
                    )
                }
            }
        }

        // Verify patient name and test tags
        compose.onNodeWithTag("profileScreen").assertIsDisplayed()
        compose.onNodeWithText(patient.displayName).assertIsDisplayed()
        compose.onNodeWithTag("workspaceButton").assertIsDisplayed()
        compose.onNodeWithTag("openReportButton").assertIsDisplayed()

        // Verify Clinical Attention Callout if patient has it
        if (!patient.clinicalAttention.isNullOrBlank()) {
            compose.onNodeWithText(patient.clinicalAttention).assertIsDisplayed()
        }

        // Verify collapsible sections exist via LazyColumn scrolling
        compose.onNodeWithTag("profileList").performScrollToNode(hasTestTag("profileVisitsSection"))
        compose.onNodeWithTag("profileVisitsSection").assertIsDisplayed()

        compose.onNodeWithTag("profileList").performScrollToNode(hasTestTag("profileLabsSection"))
        compose.onNodeWithTag("profileLabsSection").assertIsDisplayed()

        compose.onNodeWithTag("profileList").performScrollToNode(hasTestTag("addVisit"))
        compose.onNodeWithTag("addVisit").assertIsDisplayed()
    }

    @Test
    fun profileScreen_visitsSection_expandsAndCollapses() {
        val patient = DemoFixtures.patients.first()
        val state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 7001L),
        )

        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    ProfileScreen(
                        state = state,
                        route = AncRoute.Profile(patient.id),
                        onEvent = {},
                    )
                }
            }
        }

        // Scroll to VISITS header
        compose.onNodeWithTag("profileList").performScrollToNode(hasTestTag("profileVisitsSection"))
        compose.onNodeWithTag("profileVisitsSection").assertIsDisplayed()

        // Click header to collapse
        compose.onNodeWithText("VISITS").performClick()
        compose.waitForIdle()

        // Click again to expand
        compose.onNodeWithText("VISITS").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("addVisit").assertIsDisplayed()
    }
}
