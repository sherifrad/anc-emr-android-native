package com.sherifradwan.ancnative

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_DISCONTINUED
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.PatientHubTab
import com.sherifradwan.ancnative.ui.patienthub.tabs.MedsTab
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Meds tab's own view state (picker open, search text, "Show past") is saved state: it survives recreation. */
@RunWith(AndroidJUnit4::class)
class MedsTabRestorationTest {
    @get:Rule
    val compose = createComposeRule()

    private fun row(id: String, name: String, status: String) = DemoMedicationRecord(
        id = id, patientId = DemoFixtures.patients.first().id, name = name, dosage = "100 mg", route = "Oral",
        frequency = "Once daily", startDate = "2026-03-01", endDate = null, status = status, notes = null,
        isProphylaxis = false, revision = 2,
    )

    private val patient = DemoFixtures.patients.first().copy(
        medications = listOf(row("r-m1", "UiIron", MEDICATION_STATUS_ACTIVE), row("r-m2", "UiOld", MEDICATION_STATUS_DISCONTINUED)),
        status = "active",
    )

    private fun str(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    private fun scrollTo(tag: String) = compose.scrollHubTabTo(PatientHubTab.MEDS, hasTestTag(tag))

    @Test
    fun thePickerAndShowPastSurviveRecreation() {
        val tester = StateRestorationTester(compose)
        tester.setContent {
            AncTheme {
                MedsTab(
                    patient = patient, canEdit = true, actionsAllowed = true, medicationDraft = null,
                    savePhase = SavePhase.IDLE, saveError = null, quickSave = null, stopUndo = null, onEvent = {},
                )
            }
        }
        scrollTo("medPickerToggle")
        node("medPickerToggle").performClick()
        node("medSearchField").performTextInput("Ui")
        scrollTo("medTab-recent")
        node("medTab-recent").performClick()
        scrollTo("medicationToggle-r-m1")
        node("medicationToggle-r-m1").performClick()
        scrollTo("medShowPastToggle")
        node("medShowPastToggle").performClick()
        compose.waitForIdle()
        node("medication-r-m2").assertExists()

        tester.emulateSavedInstanceStateRestore()
        compose.waitForIdle()

        scrollTo("medSearchField")
        node("medSearchField").assertTextContains("Ui")
        compose.onNodeWithTag("medTab-recent").assertIsSelected()
        compose.onNodeWithTag("medTab-common").assertIsNotSelected()
        scrollTo("medicationToggle-r-m1")
        assertEquals(str(R.string.state_expanded), compose.onNodeWithTag("medicationToggle-r-m1").fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        scrollTo("medication-r-m2")
        node("medication-r-m2").assertExists()
    }
}
