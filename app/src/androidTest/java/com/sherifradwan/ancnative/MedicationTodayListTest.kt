package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_DISCONTINUED
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.MedicationDraftField
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.WorkspaceScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Independent UI test for Medication "Today's list" (mock d1 §3 & REDESIGN_SPECS §4.3).
 * Verifies Outcome Oracles O1-O6 from SPEC_DESIGN_TODAYS_LIST.md.
 */
@RunWith(AndroidJUnit4::class)
class MedicationTodayListTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private var state by mutableStateOf(AncState(patients = emptyList()))
    private val recordedEvents = mutableListOf<AncEvent>()

    private fun testMed(id: String, patientId: String, name: String, status: String) =
        DemoMedicationRecord(
            id = id,
            patientId = patientId,
            name = name,
            dosage = "500 mg",
            route = "Oral",
            frequency = "Once daily",
            startDate = "2026-05-10",
            endDate = if (status == MEDICATION_STATUS_DISCONTINUED) "2026-05-12" else null,
            status = status,
            notes = "Test instructions",
            isProphylaxis = false,
        )

    private fun host(medications: List<DemoMedicationRecord>) {
        recordedEvents.clear()
        val base = DemoFixtures.patients.first()
        val patient = base.copy(medications = medications)
        state = AncState(
            patients = listOf(patient),
            vaultState = VaultState.Unlocked(sessionEpoch = 7000L),
            route = AncRoute.Workspace(patient.id, WorkspaceSection.MEDICATIONS),
        )
        val route = state.route as AncRoute.Workspace
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    WorkspaceScreen(
                        state = state,
                        route = route,
                        onEvent = { event ->
                            recordedEvents.add(event)
                            state = AncReducer.reduce(state, event)
                        },
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

    @Test
    fun tapToAdd_and_todaysList_sectionsRender() {
        val base = DemoFixtures.patients.first()
        host(listOf(testMed("med-1", base.id, "Ferrous Sulfate", MEDICATION_STATUS_ACTIVE)))

        scrollTo("medTapToAddCard")
        compose.onNodeWithTag("medTapToAddCard", useUnmergedTree = true).assertExists().assertIsDisplayed()
        compose.onNodeWithTag("medSearchField", useUnmergedTree = true).assertExists().assertIsDisplayed()
        compose.onNodeWithTag("medTab-common", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("medTab-recent", useUnmergedTree = true).assertExists()

        scrollTo("medTodayList")
        compose.onNodeWithTag("medTodayList", useUnmergedTree = true).assertExists().assertIsDisplayed()
        compose.onNodeWithTag("medication-med-1", useUnmergedTree = true).assertExists()
    }

    @Test
    fun tapToAdd_tileTapDispatchesAddEvents() {
        val base = DemoFixtures.patients.first()
        host(emptyList())

        scrollTo("medTile-med_folic")
        compose.onNodeWithTag("medTile-med_folic", useUnmergedTree = true).performClick()
        compose.waitForIdle()

        // Verifies Oracle O1: tapping tile dispatches StartMedicationEditor + field updates + SubmitMedication
        val startEvent = recordedEvents.filterIsInstance<AncEvent.StartMedicationEditor>().firstOrNull()
        assertTrue("StartMedicationEditor dispatched", startEvent != null)
        val nameEvent = recordedEvents.filterIsInstance<AncEvent.MedicationDraftChanged>()
            .firstOrNull { it.field == MedicationDraftField.NAME }
        assertEquals("Folic Acid", nameEvent?.value)
        val submitEvent = recordedEvents.filterIsInstance<AncEvent.SubmitMedication>().firstOrNull()
        assertTrue("SubmitMedication dispatched", submitEvent != null)
    }

    @Test
    fun activeCard_stopButtonDispatchesDiscontinueEvents() {
        val base = DemoFixtures.patients.first()
        host(listOf(testMed("med-stop-test", base.id, "Paracetamol", MEDICATION_STATUS_ACTIVE)))

        scrollTo("stopMedication-med-stop-test")
        compose.onNodeWithTag("stopMedication-med-stop-test", useUnmergedTree = true).performClick()
        compose.waitForIdle()

        // Verifies Oracle O2: tapping Stop button dispatches StartMedicationEditor + STATUS=Discontinued + SubmitMedication
        val startEvent = recordedEvents.filterIsInstance<AncEvent.StartMedicationEditor>().firstOrNull()
        assertEquals("med-stop-test", startEvent?.medicationId)
        val statusEvent = recordedEvents.filterIsInstance<AncEvent.MedicationDraftChanged>()
            .firstOrNull { it.field == MedicationDraftField.STATUS }
        assertEquals(MEDICATION_STATUS_DISCONTINUED, statusEvent?.value)
        val submitEvent = recordedEvents.filterIsInstance<AncEvent.SubmitMedication>().firstOrNull()
        assertTrue("SubmitMedication dispatched on stop", submitEvent != null)
        assertTrue("StartMedicationEditor comes before draft change", recordedEvents.indexOf(startEvent as AncEvent) < recordedEvents.indexOf(statusEvent as AncEvent))
    }

    @Test
    fun stoppedMedication_rendersInStoppedSection_andRestartRestoresActive() {
        val base = DemoFixtures.patients.first()
        val todayStr = java.time.LocalDate.now().toString()
        host(listOf(testMed("med-stopped-1", base.id, "Methyldopa", MEDICATION_STATUS_DISCONTINUED).copy(endDate = todayStr)))

        scrollTo("medStoppedSection")
        compose.onNodeWithTag("medStoppedSection", useUnmergedTree = true).assertExists().assertIsDisplayed()
        compose.onNodeWithTag("stoppedMedication-med-stopped-1", useUnmergedTree = true).assertExists()

        // Verifies Oracle O3: tapping Restart dispatches STATUS=Active + SubmitMedication
        compose.onNodeWithTag("restartMedication-med-stopped-1", useUnmergedTree = true).performClick()
        compose.waitForIdle()

        val statusEvent = recordedEvents.filterIsInstance<AncEvent.MedicationDraftChanged>()
            .firstOrNull { it.field == MedicationDraftField.STATUS }
        assertEquals(MEDICATION_STATUS_ACTIVE, statusEvent?.value)
        val startDateEvent = recordedEvents.filterIsInstance<AncEvent.MedicationDraftChanged>()
            .firstOrNull { it.field == MedicationDraftField.START_DATE }
        assertTrue("START_DATE is preserved on restart and not overwritten with today", startDateEvent == null)
        val endDateEvent = recordedEvents.filterIsInstance<AncEvent.MedicationDraftChanged>()
            .firstOrNull { it.field == MedicationDraftField.END_DATE }
        assertEquals("", endDateEvent?.value)
        val submitEvent = recordedEvents.filterIsInstance<AncEvent.SubmitMedication>().firstOrNull()
        assertTrue("SubmitMedication dispatched on restart", submitEvent != null)
    }

    @Test
    fun stoppedMedication_fromPastSession_doesNotRenderInSessionStoppedSection() {
        val base = DemoFixtures.patients.first()
        // Medication stopped months ago in a past visit
        host(listOf(testMed("med-past-stop", base.id, "Methyldopa", MEDICATION_STATUS_DISCONTINUED).copy(endDate = "2026-01-01")))

        // Verifies H1: Past stopped courses do NOT show in "Stopped in this session" to prevent historical date erasing
        compose.onNodeWithTag("medStoppedSection", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun activeTileTap_doesNotDispatchSubmit() {
        val base = DemoFixtures.patients.first()
        host(listOf(testMed("med-active-folic", base.id, "Folic Acid", MEDICATION_STATUS_ACTIVE)))

        scrollTo("medTile-med_folic")
        compose.onNodeWithTag("medTile-med_folic", useUnmergedTree = true).performClick()
        compose.waitForIdle()

        val submitEvents = recordedEvents.filterIsInstance<AncEvent.SubmitMedication>()
        assertTrue("Already active tile does NOT dispatch SubmitMedication", submitEvents.isEmpty())
        assertTrue("No StartMedicationEditor dispatched for already active tile", recordedEvents.none { it is AncEvent.StartMedicationEditor })
    }

    @Test
    fun preservedControls_exist() {
        val base = DemoFixtures.patients.first()
        val meds = (1..4).map { i ->
            testMed("med-ctrl-$i", base.id, "Medication $i", MEDICATION_STATUS_ACTIVE)
        }
        host(meds)

        scrollTo("addMedication")
        compose.onNodeWithTag("addMedication", useUnmergedTree = true).assertExists().assertIsDisplayed()
        scrollTo("medicationsToggleAll")
        compose.onNodeWithTag("medicationsToggleAll", useUnmergedTree = true).assertExists().assertIsDisplayed()
        scrollTo("medicationToggle-med-ctrl-1")
        compose.onNodeWithTag("medicationToggle-med-ctrl-1", useUnmergedTree = true).assertExists().assertIsDisplayed()
        scrollTo("editMedication-med-ctrl-1")
        compose.onNodeWithTag("editMedication-med-ctrl-1", useUnmergedTree = true).assertExists().assertIsDisplayed()
        compose.onNodeWithTag("delete-medication-med-ctrl-1", useUnmergedTree = true).assertExists()
    }

    @Test
    fun addOtherChip_opensCustomChoiceDialog_andEntersCustomDrug() {
        val base = DemoFixtures.patients.first()
        host(emptyList())

        scrollTo("medAddOtherChip")
        compose.onNodeWithTag("medAddOtherChip", useUnmergedTree = true).performClick()
        compose.waitForIdle()

        val testDrugName = "Drug_${System.currentTimeMillis()}"
        // Verifies Oracle O4: custom choice dialog is displayed
        compose.onNodeWithTag("addOtherChoiceDialog", useUnmergedTree = true).assertExists().assertIsDisplayed()
        compose.onNodeWithTag("addOtherChoiceInput", useUnmergedTree = true).performTextInput(testDrugName)
        compose.onNodeWithTag("addOtherChoiceConfirm", useUnmergedTree = true).performClick()
        compose.waitForIdle()

        val nameEvent = recordedEvents.filterIsInstance<AncEvent.MedicationDraftChanged>()
            .firstOrNull { it.field == MedicationDraftField.NAME }
        assertEquals(testDrugName, nameEvent?.value)
        val submitEvents = recordedEvents.filterIsInstance<AncEvent.SubmitMedication>()
        assertTrue("Custom drug opens editor for dosage without auto-submitting", submitEvents.isEmpty())
    }

    @Test
    fun minimumTouchTargets_areAtLeast48dp() {
        val base = DemoFixtures.patients.first()
        host(listOf(testMed("med-target-1", base.id, "Iron", MEDICATION_STATUS_ACTIVE)))

        scrollTo("medTile-med_folic")
        compose.onNodeWithTag("medTile-med_folic", useUnmergedTree = true)
            .assertHeightIsAtLeast(48.dp)

        scrollTo("stopMedication-med-target-1")
        compose.onNodeWithTag("stopMedication-med-target-1", useUnmergedTree = true)
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)

        scrollTo("editMedication-med-target-1")
        compose.onNodeWithTag("editMedication-med-target-1", useUnmergedTree = true)
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }
}
