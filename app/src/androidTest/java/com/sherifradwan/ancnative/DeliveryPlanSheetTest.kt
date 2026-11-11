package com.sherifradwan.ancnative

import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.LABEL_FEES
import com.sherifradwan.ancnative.model.LABEL_HOSPITAL
import com.sherifradwan.ancnative.model.LABEL_PLAN
import com.sherifradwan.ancnative.model.LABEL_POSTPARTUM
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.PendingSaveToken
import com.sherifradwan.ancnative.state.SaveUiError
import com.sherifradwan.ancnative.state.SavedNotice
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.PatientCommandPreparation
import com.sherifradwan.ancnative.storage.SavePatientProfileCommand
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.PatientHubTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * The delivery plan sheet on the Plan tab, mounted through the real AncApp and reducer. A save goes through the real
 * [SavePatientProfileCommand.prepare]; a small in-memory store commits it, holds it or rejects it the way the view model
 * reports a store result.
 */
@RunWith(AndroidJUnit4::class)
class DeliveryPlanSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private var state by mutableStateOf(AncState(patients = emptyList()))
    private var holdSave = false
    private var rejectWith: SaveUiError? = null
    private var held: Pair<SavePatientProfileCommand, PendingSaveToken>? = null
    private val commands = mutableListOf<SavePatientProfileCommand>()

    // Heuristic notes, not yet in the composed format, so the rewrite notice shows.
    private val freeNotes = "Delivery at Cleopatra Hospital\nFees: 20000 EGP\nPostpartum: IUD at 6 weeks"
    private val patient = DemoFixtures.patients.first().copy(obstetricNotes = freeNotes, status = "active")
    private val planTab = AncRoute.Workspace(patient.id, WorkspaceSection.DELIVERY)

    private fun onEvent(event: AncEvent) {
        state = AncReducer.reduce(state, event)
        if (event == AncEvent.SubmitPatientProfile) save()
    }

    private fun save() {
        val draft = state.patientDraft ?: return
        val prepared = SavePatientProfileCommand.prepare(state, UUID.randomUUID().toString(), "2026-11-11T08:00:00Z")
        val command = (prepared as PatientCommandPreparation.Ready).command as SavePatientProfileCommand
        val token = PendingSaveToken(command.operationUuid, draft.editorSessionId)
        state = AncReducer.reduce(state, AncEvent.SaveStarted(token))
        commands += command
        if (holdSave) held = command to token else finish(command, token)
    }

    private fun finish(command: SavePatientProfileCommand, token: PendingSaveToken) {
        val error = rejectWith
        state = if (error != null) {
            AncReducer.reduce(state, AncEvent.SaveFailed(token, error, unresolved = false))
        } else {
            val saved = state.patients.map {
                if (it.id == command.patientId) it.copy(obstetricNotes = command.obstetricNotes, revision = it.revision + 1) else it
            }
            AncReducer.reduce(
                state,
                // The real view model reports the profile route as the destination; the sheet must not follow it.
                AncEvent.RecordCommitted(token, saved, AncRoute.Workspace(command.patientId), SavedNotice.PATIENT, SystemClock.elapsedRealtime()),
            )
        }
    }

    private fun host() {
        state = AncState(patients = listOf(patient), vaultState = VaultState.Unlocked(sessionEpoch = 7301L), route = planTab)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    AncApp(
                        state = state,
                        onEvent = ::onEvent,
                        onStartVisit = { _, _ -> },
                        onSaveVisit = {},
                        onInitialize = {},
                        onUnlock = {},
                        onLock = {},
                        onRetryStorage = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.openHubTab(PatientHubTab.PLAN)
    }

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    /** The Cancel and Save row is pinned under the scrolling body, so there is nothing to scroll to. */
    private fun click(tag: String) {
        node(tag).performClick()
        compose.waitForIdle()
    }
    private fun exists(tag: String) = compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag), useUnmergedTree = true)
        .fetchSemanticsNodes().isNotEmpty()
    private fun openSheet() {
        node("editDeliveryPlan").performClick()
        compose.waitForIdle()
        node("deliveryPlanSheet").assertIsDisplayed()
    }
    private fun type(tag: String, text: String) {
        node(tag).performScrollTo().performTextReplacement(text)
        compose.waitForIdle()
    }

    @Test
    fun thePencilOpensTheSheetOverThePlanTabWithoutChangingTheRoute() {
        host()
        openSheet()
        assertEquals(planTab, state.route)
        assertTrue(exists("planTabContent"))
        node("deliveryPlanHospitalField").assertTextContains("Cleopatra Hospital", substring = true)
        node("deliveryPlanFeesField").assertTextContains("20000 EGP", substring = true)
        node("deliveryPlanPostpartumField").assertTextContains("IUD at 6 weeks", substring = true)
    }

    @Test
    fun theRewriteNoticeShowsForNotesThatAreNotComposedYet() {
        host()
        openSheet()
        node("deliveryPlanRewriteNotice").assertIsDisplayed()
    }

    @Test
    fun saveIsDisabledUntilAFieldChanges() {
        host()
        openSheet()
        node("saveDeliveryPlan").assertIsNotEnabled()
        type("deliveryPlanFeesField", "25000 EGP")
        node("saveDeliveryPlan").assertIsEnabled()
    }

    @Test
    fun theSaveButtonSaysSaveUntilASaveRuns() {
        holdSave = true
        host()
        openSheet()
        compose.onNodeWithTag("saveDeliveryPlan").assertTextEquals("Save")
        type("deliveryPlanFeesField", "25000 EGP")
        click("saveDeliveryPlan")
        compose.onNodeWithTag("saveDeliveryPlan").assertTextEquals("Saving…")
        val (command, token) = held!!
        held = null
        compose.runOnIdle { finish(command, token) }
    }

    @Test
    fun typingAFieldBackToItsOpenedTextLeavesSaveDisabled() {
        host()
        openSheet()
        type("deliveryPlanFeesField", "25000 EGP")
        node("saveDeliveryPlan").assertIsEnabled()
        type("deliveryPlanFeesField", "20000 EGP")
        node("saveDeliveryPlan").assertIsNotEnabled()
    }

    @Test
    fun theRewriteNoticeNamesWhatIsLost() {
        host()
        openSheet()
        node("deliveryPlanRewriteNotice").assertTextContains("dropped", substring = true)
        node("deliveryPlanRewriteNotice").assertTextContains("grouped by box", substring = true)
    }

    @Test
    fun savingEditedFeesClosesTheSheetKeepsTheTabAndTheCardShowsTheNewFees() {
        host()
        openSheet()
        type("deliveryPlanFeesField", "25000 EGP")
        click("saveDeliveryPlan")
        assertTrue(!exists("deliveryPlanSheet"))
        assertEquals(planTab, state.route)
        val command = commands.single()
        assertTrue(command.obstetricNotes!!.contains("$LABEL_FEES: 25000 EGP"))
        // Only the notes change: every other profile field is the stored patient's.
        assertEquals(patient.displayName, command.displayName)
        assertEquals(patient.caseId, command.caseId)
        assertEquals(patient.phone, command.phone)
        assertEquals(patient.riskLevel, command.riskLevel)
        // The box is a container (title and value): look for the new value inside it.
        compose.onNode(
            androidx.compose.ui.test.hasText("25000 EGP", substring = true) and
                androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.hasTestTag("deliveryFees")),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun cancelOnAnUntouchedSheetClosesAtOnce() {
        host()
        openSheet()
        click("cancelDeliveryPlan")
        assertTrue(!exists("deliveryPlanSheet"))
        assertTrue(commands.isEmpty())
    }

    @Test
    fun cancelOnATouchedSheetAsksAndDiscardLeavesTheCardUnchanged() {
        host()
        openSheet()
        type("deliveryPlanPlanField", "Elective caesarean")
        click("cancelDeliveryPlan")
        node("deliveryPlanDiscardDialog").assertIsDisplayed()
        click("deliveryPlanDiscardKeep")
        node("deliveryPlanSheet").assertIsDisplayed()
        click("cancelDeliveryPlan")
        click("deliveryPlanDiscardConfirm")
        assertTrue(!exists("deliveryPlanSheet"))
        assertEquals(freeNotes, state.patient(patient.id)!!.obstetricNotes)
    }

    @Test
    fun whileASaveRunsTheFieldsAndButtonsAreDisabled() {
        holdSave = true
        host()
        openSheet()
        type("deliveryPlanFeesField", "25000 EGP")
        click("saveDeliveryPlan")
        node("deliveryPlanSaveProgress").assertIsDisplayed()
        node("deliveryPlanFeesField").assertIsNotEnabled()
        node("cancelDeliveryPlan").assertIsNotEnabled()
        node("saveDeliveryPlan").assertIsNotEnabled()
        val (command, token) = held!!
        held = null
        compose.runOnIdle { finish(command, token) }
        compose.waitForIdle()
        assertTrue(!exists("deliveryPlanSheet"))
    }

    @Test
    fun aRejectedSaveKeepsTheSheetTheValuesAndShowsTheMessage() {
        rejectWith = SaveUiError.STALE_OR_CONFLICT
        host()
        openSheet()
        type("deliveryPlanFeesField", "25000 EGP")
        click("saveDeliveryPlan")
        node("deliveryPlanSheet").assertIsDisplayed()
        node("deliveryPlanSaveMessage").assertIsDisplayed()
        node("deliveryPlanFeesField").assertTextContains("25000 EGP", substring = true)
        assertEquals(freeNotes, state.patient(patient.id)!!.obstetricNotes)
    }

    @Test
    fun theStoredLabelsAreTheCardTitles() {
        val context = compose.activity
        assertEquals(LABEL_HOSPITAL, context.getString(R.string.delivery_hospital_title))
        assertEquals(LABEL_FEES, context.getString(R.string.delivery_fees_title))
        assertEquals(LABEL_PLAN, context.getString(R.string.delivery_plan_title))
        assertEquals(LABEL_POSTPARTUM, context.getString(R.string.delivery_postpartum_title))
    }
}
