package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.ClinicalPatientCard
import com.sherifradwan.ancnative.ui.PatientCardVariant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ClinicalPatientCardTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patient = DemoFixtures.patients.first()

    @Test
    fun bedsideTopBar_displaysFullNameAndExcludesCaseId() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    ClinicalPatientCard(
                        patient = patient,
                        variant = PatientCardVariant.BEDSIDE_TOP_BAR,
                        asOfDate = LocalDate.parse("2026-11-08"),
                        onBack = { },
                        onOpenProfile = { },
                        onOpenReport = { },
                        onOpenAttentionDialog = { },
                    )
                }
            }
        }
        compose.waitForIdle()

        // Name is fully visible
        compose.onNodeWithText(patient.displayName).assertIsDisplayed()

        // Case ID / demo ID is NOT present
        compose.onAllNodesWithText("DEMO-ANC-001").assertCountEquals(0)

        // Action buttons exist
        compose.onNodeWithTag("workspaceReportButton").assertIsDisplayed()
        compose.onNodeWithTag("workspaceProfile").assertIsDisplayed()
        compose.onNodeWithTag("workspaceTopBar").assertIsDisplayed()
    }

    @Test
    fun stickyCompactBar_displaysFullNameAndActions() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    ClinicalPatientCard(
                        patient = patient,
                        variant = PatientCardVariant.STICKY_COMPACT,
                        asOfDate = LocalDate.parse("2026-11-08"),
                        onBack = { },
                        onOpenProfile = { },
                        onOpenReport = { },
                    )
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(patient.displayName).assertIsDisplayed()
        compose.onNodeWithTag("workspaceStickyCompactBar").assertIsDisplayed()
        compose.onAllNodesWithText("DEMO-ANC-001").assertCountEquals(0)
    }

    @Test
    fun editorSummary_displaysFullNameAndMetadata() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    ClinicalPatientCard(
                        patient = patient,
                        variant = PatientCardVariant.EDITOR_SUMMARY,
                        asOfDate = LocalDate.parse("2026-11-08"),
                    )
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(patient.displayName).assertIsDisplayed()
        compose.onAllNodesWithText("DEMO-ANC-001").assertCountEquals(0)
    }
}
