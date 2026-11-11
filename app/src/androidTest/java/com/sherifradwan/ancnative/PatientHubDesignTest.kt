package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.PatientHubScreen
import com.sherifradwan.ancnative.ui.patienthub.PatientHubTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun patientHub_header_largeTextAt360dpKeepsTheNameAndTheAllergyLineWhole() {
        val longName = "Amina Abdulrahman Ogunbanjo Adeyemi"
        val state = AncState(
            patients = listOf(patient.copy(displayName = longName)),
            vaultState = VaultState.Unlocked(sessionEpoch = 7001L),
        )
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    val base = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(base.density, 2f)) {
                        Box(Modifier.width(360.dp)) {
                            PatientHubScreen(
                                state = state,
                                route = AncRoute.Workspace(patient.id, WorkspaceSection.EPISODE_OVERVIEW),
                                onEvent = {},
                                onStartVisit = { _, _ -> },
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()

        val name = layout("hubPatientName")
        assertTrue("the name takes at most two lines", name.lineCount <= 2)
        assertTrue("no line of the name is cut with an ellipsis", (0 until name.lineCount).none { name.isLineEllipsized(it) })
        val allergy = layout("hubAllergyText")
        assertTrue("the allergy line is not cut", (0 until allergy.lineCount).none { allergy.isLineEllipsized(it) })
        val pillHeight = compose.onNodeWithTag("hubAllergyPill").fetchSemanticsNode().size.height
        assertTrue("the allergy pill is as tall as its text ($pillHeight px)", pillHeight >= allergy.size.height)
        compose.onNodeWithTag("hubGaBadge").assertIsDisplayed()
        compose.onNodeWithTag("hubRiskBadge").assertIsDisplayed()
    }

    private fun layout(tag: String): TextLayoutResult {
        compose.onNodeWithTag(tag).assertIsDisplayed()
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        assertEquals("one text layout for $tag", 1, results.size)
        return results.single()
    }
}
