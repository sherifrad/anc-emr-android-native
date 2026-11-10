package com.sherifradwan.ancnative

import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.state.MedicationDraft
import com.sherifradwan.ancnative.state.MedicationDraftValues
import com.sherifradwan.ancnative.ui.COMMON_MEDICATION_FREQUENCIES
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_DISCONTINUED
import com.sherifradwan.ancnative.model.catalog.ClinicalCatalogRepository
import com.sherifradwan.ancnative.model.catalog.MedicationCatalogItem
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.PendingSaveToken
import com.sherifradwan.ancnative.state.SavedNotice
import com.sherifradwan.ancnative.state.SaveUiError
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.ClinicalCommandPreparation
import com.sherifradwan.ancnative.storage.ClinicalRecordKind
import com.sherifradwan.ancnative.storage.SaveMedicationCommand
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.PatientHubTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

/**
 * The Meds tab with the medication sheet, mounted through the real AncApp and reducer. Saves go through the real
 * [SaveMedicationCommand.prepare]; a small in-memory store commits them (or holds them, or rejects them) the way the
 * view model reports a store result.
 */
@RunWith(AndroidJUnit4::class)
class MedsTabSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private var state by mutableStateOf(AncState(patients = emptyList()))
    // JUnit makes a new instance for every test, so this is read once per test.
    private val today = LocalDate.now().toString()
    private val medsTab get() = AncRoute.Workspace(patient.id, WorkspaceSection.MEDICATIONS)

    /** Every command the store was given, in order. */
    private val commands = mutableListOf<SaveMedicationCommand>()
    private var holdSaves = false
    private var addAnother = false
    private var rejectWith: SaveUiError? = null
    private var held: Pair<SaveMedicationCommand, PendingSaveToken>? = null

    private fun row(id: String, name: String, status: String = MEDICATION_STATUS_ACTIVE) = DemoMedicationRecord(
        id = id, patientId = DemoFixtures.patients.first().id, name = name, dosage = "100 mg", route = "Oral",
        frequency = "Once daily", startDate = "2026-03-01", endDate = null, status = status, notes = null,
        isProphylaxis = false, revision = 2,
    )

    private val patient = DemoFixtures.patients.first().copy(
        medications = listOf(row("ui-m1", "UiIron"), row("ui-m2", "UiAspirin"), row("ui-m3", "UiOld", MEDICATION_STATUS_DISCONTINUED)),
        status = "active",
    )

    private fun catalog(): List<MedicationCatalogItem> =
        ClinicalCatalogRepository.getInstance(compose.activity).medicationCatalogItems.value

    private fun onEvent(event: AncEvent) {
        state = AncReducer.reduce(state, event)
        when (event) {
            is AncEvent.StopMedication -> save(event.editorSessionId, quick = true)
            is AncEvent.UndoMedicationStop -> save(event.editorSessionId, quick = true)
            AncEvent.SubmitMedication -> state.medicationDraft?.let { save(it.editorSessionId, quick = false) }
            AncEvent.SubmitMedicationAndAddAnother -> state.medicationDraft?.let {
                addAnother = true
                save(it.editorSessionId, quick = false)
            }
            else -> Unit
        }
    }

    private fun save(session: String, quick: Boolean) {
        val draft = if (quick) state.medicationQuickSave?.draft?.takeIf { it.editorSessionId == session } else state.medicationDraft
        draft ?: return
        val prepared = SaveMedicationCommand.prepare(state.copy(medicationDraft = draft), UUID.randomUUID().toString(), draft.medicationId)
        if (prepared is ClinicalCommandPreparation.Rejected) {
            // As the view model does: the rules' errors go back to the draft.
            state = AncReducer.reduce(state, AncEvent.ClinicalValidationFailed(ClinicalRecordKind.MEDICATION_CREATE, prepared.errors))
            return
        }
        val command = (prepared as ClinicalCommandPreparation.Ready).command as SaveMedicationCommand
        val token = PendingSaveToken(command.operationUuid, command.editorSessionId)
        state = AncReducer.reduce(state, AncEvent.SaveStarted(token))
        commands += command
        if (holdSaves) held = command to token else finish(command, token)
    }

    private fun finish(command: SaveMedicationCommand, token: PendingSaveToken) {
        val error = rejectWith
        state = if (error != null) {
            AncReducer.reduce(state, AncEvent.SaveFailed(token, error, unresolved = false))
        } else {
            AncReducer.reduce(
                state,
                AncEvent.RecordCommitted(token, committed(command), medsTab, SavedNotice.MEDICATIONS, SystemClock.elapsedRealtime()),
            )
        }
        if (error == null && addAnother) {
            // As the view model does after "Save and add another": a blank draft at once.
            state = AncReducer.reduce(
                state,
                AncEvent.StartMedicationEditor(command.patientId, null, UUID.randomUUID().toString(), UUID.randomUUID().toString(), keepSavedNotice = true),
            )
        }
        addAnother = false
    }

    private fun releaseHeldSave() {
        val (command, token) = held!!
        held = null
        compose.runOnIdle { finish(command, token) }
        compose.waitForIdle()
    }

    private fun committed(command: SaveMedicationCommand) = state.patients.map { p ->
        if (p.id != command.patientId) return@map p
        val old = p.medications.firstOrNull { it.id == command.medicationId }
        val saved = DemoMedicationRecord(
            id = command.medicationId, patientId = p.id, name = command.name, dosage = command.dosage, route = command.route,
            frequency = command.frequency, startDate = command.startDate, endDate = command.endDate, status = command.status,
            notes = command.notes, isProphylaxis = command.isProphylaxis, isNonCompliant = command.isNonCompliant,
            adverseNotes = command.adverseNotes, revision = (old?.revision ?: 0L) + 1,
        )
        p.copy(
            revision = p.revision + 1,
            medications = if (old == null) p.medications + saved else p.medications.map { if (it.id == saved.id) saved else it },
        )
    }

    private fun host(meds: List<DemoMedicationRecord> = patient.medications) {
        state = AncState(
            patients = listOf(patient.copy(medications = meds)),
            vaultState = VaultState.Unlocked(sessionEpoch = 7101L),
            route = medsTab,
        )
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
    }

    private fun node(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
    private fun scrollTo(tag: String) = compose.scrollHubTabTo(PatientHubTab.MEDS, hasTestTag(tag))
    private fun click(tag: String) {
        node(tag).performClick()
        compose.waitForIdle()
    }
    private fun med(id: String) = state.patient(patient.id)!!.medications.first { it.id == id }

    @Test
    fun tappingATileOpensTheSheetWithoutLeavingTheTabAndKeepsTheSearchText() {
        host()
        val item = catalog().first()
        node("medSearchField").performTextInput(item.name.take(3))
        compose.waitForIdle()
        scrollTo("medTile-${item.id}")
        click("medTile-${item.id}")

        node("medicationSheet").assertIsDisplayed()
        node("workspaceScreen").assertExists()
        assertEquals(medsTab, state.route)
        val values = state.medicationDraft!!.values
        assertEquals(item.name, values.name)
        assertEquals(item.dosage, values.dosage)
        assertEquals(item.frequency, values.frequency)
        assertEquals(today, values.startDate)
        // While the sheet is open the tab's entry points are visibly disabled.
        scrollTo("addCustomMedicationBtn")
        node("addCustomMedicationBtn").assertIsNotEnabled()

        click("saveMedication")
        node("medicationSheet").assertDoesNotExist()
        assertEquals(medsTab, state.route)
        assertEquals(1, commands.size)
        assertNull(commands.single().expectedMedicationRevision)
        assertNotNull(state.patient(patient.id)!!.medications.firstOrNull { it.name == item.name })
        node("medSearchField").assertTextContains(item.name.take(3))
    }

    @Test
    fun editAndPrescribeOpenTheSheetOnTheTab() {
        host()
        scrollTo("editMedication-ui-m1")
        click("editMedication-ui-m1")
        node("medicationSheet").assertIsDisplayed()
        assertEquals("ui-m1", state.medicationDraft!!.medicationId)
        click("cancelMedication")
        node("medicationSheet").assertDoesNotExist()
        assertEquals(medsTab, state.route)

        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medicationSheet").assertIsDisplayed()
        assertEquals("", state.medicationDraft!!.values.name)
        assertEquals(medsTab, state.route)
    }

    @Test
    fun stopIsOneTapWithoutASheetAndUndoPutsTheRowBack() {
        host()
        scrollTo("stopMedication-ui-m1")
        click("stopMedication-ui-m1")

        node("medicationSheet").assertDoesNotExist()
        assertNull(state.medicationDraft)
        assertEquals(medsTab, state.route)
        assertEquals(MEDICATION_STATUS_DISCONTINUED, med("ui-m1").status)
        assertEquals(today, med("ui-m1").endDate)
        node("medStopUndoBar").assertIsDisplayed()
        compose.onNodeWithText("Stopped: UiIron", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(2L, commands.single().expectedMedicationRevision)

        click("medStopUndo")
        node("medStopUndoBar").assertDoesNotExist()
        assertEquals(MEDICATION_STATUS_ACTIVE, med("ui-m1").status)
        assertNull(med("ui-m1").endDate)
        assertEquals(2, commands.size)
        assertEquals("Undo uses the revision the stop produced", 3L, commands[1].expectedMedicationRevision)
        assertEquals(medsTab, state.route)
    }

    @Test
    fun everyEntryPointIsDisabledWhileAStopSaves() {
        host()
        holdSaves = true
        scrollTo("stopMedication-ui-m1")
        click("stopMedication-ui-m1")
        node("medStopProgress").assertIsDisplayed()

        scrollTo("stopMedication-ui-m2")
        node("stopMedication-ui-m2").assertIsNotEnabled()
        node("editMedication-ui-m2").assertIsNotEnabled()
        node("delete-medication-ui-m2").assertIsNotEnabled()
        scrollTo("addCustomMedicationBtn")
        node("addCustomMedicationBtn").assertIsNotEnabled()
        val tile = catalog().first { item -> patient.medications.none { it.name.equals(item.name, ignoreCase = true) } }
        scrollTo("medTile-${tile.id}")
        node("medTile-${tile.id}").assertIsNotEnabled()

        releaseHeldSave()
        node("medStopProgress").assertDoesNotExist()
        scrollTo("stopMedication-ui-m2")
        node("stopMedication-ui-m2").assertIsEnabled()
        node("editMedication-ui-m2").assertIsEnabled()
    }

    @Test
    fun aFailedStopLeavesTheRowActiveAndShowsTheCause() {
        host()
        rejectWith = SaveUiError.STALE_OR_CONFLICT
        scrollTo("stopMedication-ui-m1")
        click("stopMedication-ui-m1")
        assertEquals(MEDICATION_STATUS_ACTIVE, med("ui-m1").status)
        node("medStopError").assertIsDisplayed()
        node("medStopUndoBar").assertDoesNotExist()
        scrollTo("stopMedication-ui-m1")
        node("stopMedication-ui-m1").assertIsEnabled()
    }

    @Test
    fun aTileForAnActiveDrugShowsItsCheckAndDoesNothing() {
        val item = catalog().first()
        host(patient.medications + row("ui-m4", item.name))
        scrollTo("medTile-${item.id}")
        node("medTile-${item.id}").assertIsNotEnabled()
        node("medTile-${item.id}").performClick()
        compose.waitForIdle()
        node("medicationSheet").assertDoesNotExist()
        assertNull(state.medicationDraft)
    }

    private fun str(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
    private fun count(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().size
    private fun top(tag: String) = node(tag).fetchSemanticsNode().boundsInRoot.top

    private fun openEditAndChangeDose(id: String = "ui-m1") {
        scrollTo("editMedication-$id")
        click("editMedication-$id")
        node("medDosage").performTextInput("5")
        compose.waitForIdle()
    }

    @Test
    fun onlyOneDiscardPromptIsEverShownWhenBackIsPressedOverTheSheetPrompt() {
        host()
        openEditAndChangeDose()
        assertEquals(0, count("medDiscardDialog") + count("discardDialog"))
        click("cancelMedication")
        assertEquals(1, count("medDiscardDialog"))
        assertEquals(0, count("discardDialog"))
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        assertTrue("never two prompts", count("medDiscardDialog") + count("discardDialog") <= 1)
        assertEquals("Back never reaches the app's own prompt", 0, count("discardDialog"))
        assertNotNull("Back over the prompt keeps the edits", state.medicationDraft)
        assertEquals(medsTab, state.route)
    }

    @Test
    fun aFailedSheetSaveKeepsTheSheetOpenWithTheValuesAndTheMessage() {
        host()
        rejectWith = SaveUiError.STALE_OR_CONFLICT
        openEditAndChangeDose()
        click("saveMedication")
        node("medicationSheet").assertIsDisplayed()
        node("medDosage").assertTextContains("5", substring = true)
        compose.onNodeWithText(str(R.string.save_stale_conflict), useUnmergedTree = true).assertIsDisplayed()
        node("saveMedication").assertIsEnabled()
        assertEquals(MEDICATION_STATUS_ACTIVE, med("ui-m1").status)
        assertEquals("100 mg", med("ui-m1").dosage)
    }

    @Test
    fun theEditSheetShowsTheRouteReadOnly() {
        host()
        scrollTo("editMedication-ui-m1")
        click("editMedication-ui-m1")
        node("medRouteReadOnly").assertExists()
        node("medRouteChoice-0").assertDoesNotExist()
    }

    @Test
    fun savingABlankSheetShowsTheErrorsNextToTheFields() {
        host()
        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        click("saveMedication")
        node("medicationSheet").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.correct_highlighted_fields), useUnmergedTree = true).assertIsDisplayed()
        // The name and the dose each carry the per-field message.
        assertEquals(2, compose.onAllNodes(hasText(str(R.string.correct_highlighted_field)), useUnmergedTree = true).fetchSemanticsNodes().size)
        assertTrue(commands.isEmpty())
    }

    @Test
    fun theSheetButtonsAreDisabledWithProgressWhileADraftSaves() {
        host()
        holdSaves = true
        openEditAndChangeDose()
        click("saveMedication")
        node("medSaveProgress").assertIsDisplayed()
        node("saveMedication").assertIsNotEnabled()
        node("cancelMedication").assertIsNotEnabled()
        node("medDosage").assertIsNotEnabled()
        releaseHeldSave()
        node("medicationSheet").assertDoesNotExist()
    }

    @Test
    fun aSecondStopTapWhileOneSavesIsIgnored() {
        host()
        holdSaves = true
        scrollTo("stopMedication-ui-m1")
        click("stopMedication-ui-m1")
        node("stopMedication-ui-m1").assertIsNotEnabled()
        node("stopMedication-ui-m1").performClick()
        compose.waitForIdle()
        assertEquals(1, commands.size)
        releaseHeldSave()
        assertEquals(1, commands.size)
        node("medStopUndoBar").assertIsDisplayed()
    }

    @Test
    fun aFailedStopMessageCanBeClosed() {
        host()
        rejectWith = SaveUiError.STALE_OR_CONFLICT
        scrollTo("stopMedication-ui-m1")
        click("stopMedication-ui-m1")
        node("medStopError").assertIsDisplayed()
        click("medStopErrorDismiss")
        node("medStopError").assertDoesNotExist()
        assertNull(state.medicationQuickSave)
    }

    private val longList = (1..12).map { row("ui-long-$it", "UiLong $it") }

    @Test
    fun theListKeepsItsScrollPositionThroughEditStopAndAdd() {
        host(longList)
        // Edit: the edited row stays where it was.
        scrollTo("medication-ui-long-10")
        val editedTop = top("medication-ui-long-10")
        click("editMedication-ui-long-10")
        node("medDosage").performTextInput("5")
        click("saveMedication")
        node("medicationSheet").assertDoesNotExist()
        assertEquals(editedTop, top("medication-ui-long-10"), 1f)

        // Stop: the row moves to the past list; the row above it does not move.
        val aboveTop = top("medication-ui-long-9")
        click("stopMedication-ui-long-10")
        node("medStopUndoBar").assertIsDisplayed()
        assertEquals(aboveTop, top("medication-ui-long-9"), 1f)

        // Add: the new row joins the end of the active list; what is on screen does not move.
        scrollTo("addCustomMedicationBtn")
        val buttonTop = top("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medName").performTextInput("UiNew")
        node("medDosage").performTextInput("1 mg")
        click("saveMedication")
        node("medicationSheet").assertDoesNotExist()
        assertNotNull(state.patient(patient.id)!!.medications.firstOrNull { it.name == "UiNew" })
        assertEquals(buttonTop, top("addCustomMedicationBtn"), 1f)
    }

    @Test
    fun anEditedRowShowsItsNewDoseAndFrequencyAfterSaving() {
        host()
        scrollTo("editMedication-ui-m1")
        click("editMedication-ui-m1")
        node("medDosage").performTextReplacement("250 mg")
        val newFrequency = COMMON_MEDICATION_FREQUENCIES.first { it != "Once daily" }
        val index = COMMON_MEDICATION_FREQUENCIES.indexOf(newFrequency)
        node("medFreqChoice-$index").performScrollTo().performClick()
        compose.waitForIdle()
        click("saveMedication")
        node("medicationSheet").assertDoesNotExist()
        scrollTo("medication-ui-m1")
        node("medicationInstructions-ui-m1").assertTextContains("250 mg", substring = true)
        node("medicationFrequency-ui-m1").assertTextEquals(newFrequency)
        assertEquals("the same row was updated in place", 1, state.patient(patient.id)!!.medications.count { it.id == "ui-m1" })
        assertEquals(3L, med("ui-m1").revision)
        compose.onAllNodes(hasText("100 mg", substring = true) and hasAnyAncestor(hasTestTag("medication-ui-m1")), useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun aFailedAddKeepsTheSheetAndTheValues() {
        host()
        rejectWith = SaveUiError.STORAGE_ERROR
        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medName").performTextInput("UiCustom")
        node("medDosage").performTextInput("2 mg")
        click("saveMedication")
        node("medicationSheet").assertIsDisplayed()
        node("medName").assertTextContains("UiCustom")
        node("medDosage").assertTextContains("2 mg")
        compose.onNodeWithText(str(R.string.save_storage_error), useUnmergedTree = true).assertIsDisplayed()
        assertNull(state.patient(patient.id)!!.medications.firstOrNull { it.name == "UiCustom" })
    }

    @Test
    fun saveAndAddAnotherReopensABlankSheetAndKeepsTheTab() {
        host(longList)
        node("medSearchField").performTextInput("Ui")
        compose.waitForIdle()
        scrollTo("addCustomMedicationBtn")
        val buttonTop = top("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medName").performTextInput("UiFirst")
        node("medDosage").performTextInput("1 mg")
        click("addAnotherMedication")
        node("medicationSheet").assertIsDisplayed()
        val next = state.medicationDraft!!
        assertEquals("", next.values.name)
        assertEquals(next.original, next.values)
        assertNotNull(state.patient(patient.id)!!.medications.firstOrNull { it.name == "UiFirst" })
        click("cancelMedication")
        node("medicationSheet").assertDoesNotExist()
        node("medTapToAddCard").assertExists()
        node("medSearchField").assertTextContains("Ui")
        assertEquals(buttonTop, top("addCustomMedicationBtn"), 1f)
        assertEquals(medsTab, state.route)
    }

    @Test
    fun everyEntryPointIsDisabledWhileAnAddSaves() {
        host()
        holdSaves = true
        val tile = catalog().first { item -> patient.medications.none { it.name.equals(item.name, ignoreCase = true) } }
        scrollTo("medTile-${tile.id}")
        click("medTile-${tile.id}")
        click("saveMedication")
        node("medSaveProgress").assertIsDisplayed()
        // The tab under the sheet: every entry point is visibly disabled.
        node("medTile-${tile.id}").assertIsNotEnabled()
        for (id in listOf("ui-m1", "ui-m2")) {
            scrollTo("stopMedication-$id")
            node("stopMedication-$id").assertIsNotEnabled()
            node("editMedication-$id").assertIsNotEnabled()
            node("delete-medication-$id").assertIsNotEnabled()
        }
        scrollTo("editMedication-ui-m3")
        node("editMedication-ui-m3").assertIsNotEnabled()
        scrollTo("addCustomMedicationBtn")
        node("addCustomMedicationBtn").assertIsNotEnabled()
        releaseHeldSave()
        node("medicationSheet").assertDoesNotExist()
        scrollTo("addCustomMedicationBtn")
        node("addCustomMedicationBtn").assertIsEnabled()
    }

    @Test
    fun anArchivedPatientShowsNoEntryPoints() {
        host()
        compose.runOnIdle {
            state = state.copy(patients = state.patients.map { it.copy(status = "archived") })
        }
        compose.waitForIdle()
        for (tag in listOf("medTapToAddCard", "addCustomMedicationBtn", "stopMedication-ui-m1", "editMedication-ui-m1", "delete-medication-ui-m1")) {
            node(tag).assertDoesNotExist()
        }
        scrollTo("medication-ui-m1")
        node("medication-ui-m1").assertExists()
    }

    @Test
    fun aDraftOrAFailedStopOfOnePatientDoesNotFollowToAnother() {
        val other = DemoFixtures.patients[1].copy(status = "active")
        host()
        compose.runOnIdle { state = state.copy(patients = state.patients + other) }
        rejectWith = SaveUiError.STALE_OR_CONFLICT
        scrollTo("stopMedication-ui-m1")
        click("stopMedication-ui-m1")
        node("medStopError").assertIsDisplayed()
        compose.runOnIdle { onEvent(AncEvent.OpenWorkspace(other.id)) }
        compose.runOnIdle { onEvent(AncEvent.SelectSection(WorkspaceSection.MEDICATIONS)) }
        compose.waitForIdle()
        node("medStopError").assertDoesNotExist()
        assertNull(state.medicationQuickSave)
        // A draft of the first patient is never shown on the second patient's screen.
        compose.runOnIdle {
            state = state.copy(
                medicationDraft = MedicationDraft("s-leak", patient.id, "ui-m1", 1L, 2L, MedicationDraftValues(), MedicationDraftValues()),
            )
        }
        compose.waitForIdle()
        node("medicationSheet").assertDoesNotExist()
    }

    @Test
    fun prescribeOpensABlankDraftAtTheDefaults() {
        host()
        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        val draft = state.medicationDraft!!
        val blank = MedicationDraftValues(route = "Oral", status = MEDICATION_STATUS_ACTIVE, startDate = today)
        assertEquals(blank, draft.original)
        assertEquals(blank, draft.values)
        assertNull(draft.baseMedicationRevision)
    }

    @Test
    fun theRouteIsChoosableWhenAdding() {
        host()
        val routes = ClinicalVocabulary.medicationRoutes.toList()
        val tile = catalog().first { item -> patient.medications.none { it.name.equals(item.name, ignoreCase = true) } }
        scrollTo("medTile-${tile.id}")
        click("medTile-${tile.id}")
        node("medRouteReadOnly").assertDoesNotExist()
        node("medRouteChoice-1").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(routes[1], state.medicationDraft!!.values.route)
        click("cancelMedication")

        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medRouteReadOnly").assertDoesNotExist()
        node("medRouteChoice-2").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(routes[2], state.medicationDraft!!.values.route)
    }
}
