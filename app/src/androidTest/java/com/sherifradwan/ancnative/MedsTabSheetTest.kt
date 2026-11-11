package com.sherifradwan.ancnative

import android.os.SystemClock
import android.view.WindowInsets
import android.view.inspector.WindowInspector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.graphics.luminance
import com.sherifradwan.ancnative.model.catalog.CustomChoiceList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.filters.SdkSuppress
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
import com.sherifradwan.ancnative.state.ClinicalValidationError
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
import org.junit.Assert.assertFalse
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
    /** When set, "Save and add another" commits and lets a frame pass (the sheet closes) before the next start. */
    private var chainAfterAFrame = false
    private var deferredChain: String? = null
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
        if (error == null && addAnother && chainAfterAFrame) {
            deferredChain = command.patientId
        } else if (error == null && addAnother) {
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

    /** Mounts the app on the Meds tab. With [fontScale] the screen is also held to at most 360 dp wide. */
    private fun host(meds: List<DemoMedicationRecord> = patient.medications, fontScale: Float? = null) {
        state = AncState(
            patients = listOf(patient.copy(medications = meds)),
            vaultState = VaultState.Unlocked(sessionEpoch = 7101L),
            route = medsTab,
        )
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val app = @Composable {
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
                if (fontScale == null) {
                    app()
                } else {
                    val base = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                        Box(Modifier.widthIn(max = 360.dp).testTag(NARROW_HOST)) { app() }
                    }
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
    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    /** Opens the collapsed "Add medication" picker (a no-op when it is already open). */
    private fun openPicker() {
        scrollTo("medPickerToggle")
        if (!exists("medSearchField")) click("medPickerToggle")
        node("medSearchField").assertExists()
    }

    /** Merged node: a clickable row's text sits on the row itself. */
    private fun merged(tag: String) = compose.onNodeWithTag(tag)

    /**
     * Brings one tile wholly into view: the picker's own list to the tile, then the tab list by the tile's unclipped
     * position (the tab's scroll-to-node helper measures the clipped bounds, which a nested list cuts short).
     */
    private fun showTile(id: String) {
        scrollTo("medTilesGrid")
        node("medTile-$id").performScrollTo()
        compose.waitForIdle()
        val list = node(PatientHubTab.MEDS.contentTag())
        val viewport = list.fetchSemanticsNode().boundsInRoot
        val tile = node("medTile-$id").fetchSemanticsNode()
        val top = tile.positionInRoot.y
        val bottom = top + tile.size.height
        val dy = when {
            bottom > viewport.bottom -> bottom - viewport.bottom
            top < viewport.top -> top - viewport.top
            else -> 0f
        }
        if (dy != 0f) list.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, dy) }
        compose.waitForIdle()
        node("medTile-$id").assertIsDisplayed()
    }

    private fun openPast() {
        scrollTo("medShowPastToggle")
        if (!exists("medication-ui-m3")) click("medShowPastToggle")
    }

    @Test
    fun tappingATileOpensTheSheetWithoutLeavingTheTabAndKeepsTheSearchText() {
        host()
        val item = catalog().first()
        openPicker()
        node("medSearchField").performTextInput(item.name.take(3))
        compose.waitForIdle()
        showTile(item.id)
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
        openPicker()
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
        showTile(tile.id)
        node("medTile-${tile.id}").assertIsNotEnabled()
        scrollTo("medPickerToggle")
        node("medPickerToggle").assertIsNotEnabled()
        node("medSearchField").assertIsNotEnabled()

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
        openPicker()
        showTile(item.id)
        node("medTileCheck-${item.id}").assertIsDisplayed()
        node("medTile-${item.id}").assertIsNotEnabled()
        node("medTile-${item.id}").performClick()
        compose.waitForIdle()
        node("medicationSheet").assertDoesNotExist()
        assertNull(state.medicationDraft)
    }

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
    fun aNewMedicationWithoutFrequencyShowsTheErrorAndKeepsTheSheetOpen() {
        host()
        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medName").performTextInput("UiNoFreq")
        node("medDosage").performTextInput("1 mg")
        click("saveMedication")
        node("medicationSheet").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.medication_frequency_required), useUnmergedTree = true).assertIsDisplayed()
        assertTrue(commands.isEmpty())
        assertNull(state.patient(patient.id)!!.medications.firstOrNull { it.name == "UiNoFreq" })
        // The doctor scrolls away and presses Save again with nothing changed: the error is brought back into view.
        node("medNotes").performScrollTo()
        click("saveMedication")
        compose.onNodeWithText(str(R.string.medication_frequency_required), useUnmergedTree = true).assertIsDisplayed()
        // Choosing a frequency clears the error; the same sheet then saves.
        node("medFreqChoice-0").performScrollTo().performClick()
        click("saveMedication")
        node("medicationSheet").assertDoesNotExist()
        assertNotNull(state.patient(patient.id)!!.medications.firstOrNull { it.name == "UiNoFreq" })
    }

    @Test
    fun otherChosenWithNoTextIsRejectedWithTheErrorOnTheOtherBox() {
        host()
        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medName").performTextInput("UiOtherBlank")
        node("medDosage").performTextInput("1 mg")
        node("medFreqChoice-0").performScrollTo().performClick()
        // Other clears the chip's value, so the box starts empty.
        node("medFreqChoice-other").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("", state.medicationDraft!!.values.frequency)
        click("saveMedication")
        node("medicationSheet").assertIsDisplayed()
        assertTrue(state.medicationDraft!!.errors.contains(ClinicalValidationError.REQUIRED_FREQUENCY))
        compose.onNodeWithText(str(R.string.medication_frequency_required), useUnmergedTree = true).assertIsDisplayed()
        node("medFreqError").assertDoesNotExist()
        assertTrue(commands.isEmpty())
        // The typed text is the frequency that is saved.
        node("medFreqOther").performTextInput("Every other day")
        click("saveMedication")
        node("medicationSheet").assertDoesNotExist()
        assertEquals("Every other day", state.patient(patient.id)!!.medications.first { it.name == "UiOtherBlank" }.frequency)
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
        node("medFreqChoice-0").performScrollTo().performClick()
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
        node("medicationInstructions-ui-m1").assertTextEquals("250 mg · Oral · $newFrequency")
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
        node("medFreqChoice-0").performScrollTo().performClick()
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
        openPicker()
        node("medSearchField").performTextInput("Ui")
        // Search closes the keyboard, so the list is back to its full height before it is scrolled.
        node("medSearchField").performImeAction()
        compose.waitForIdle()
        scrollTo("addCustomMedicationBtn")
        val buttonTop = top("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medName").performTextInput("UiFirst")
        node("medDosage").performTextInput("1 mg")
        node("medFreqChoice-0").performScrollTo().performClick()
        click("addAnotherMedication")
        node("medicationSheet").assertIsDisplayed()
        val next = state.medicationDraft!!
        assertEquals("", next.values.name)
        assertEquals(next.original, next.values)
        assertNotNull(state.patient(patient.id)!!.medications.firstOrNull { it.name == "UiFirst" })
        click("cancelMedication")
        node("medicationSheet").assertDoesNotExist()
        assertEquals(buttonTop, top("addCustomMedicationBtn"), 1f)
        scrollTo("medSearchField")
        node("medPicker").assertExists()
        node("medSearchField").assertTextContains("Ui")
        assertEquals(medsTab, state.route)
    }

    @Test
    fun everyEntryPointIsDisabledWhileAnAddSaves() {
        host()
        openPast()
        openPicker()
        holdSaves = true
        val tile = catalog().first { item -> patient.medications.none { it.name.equals(item.name, ignoreCase = true) } }
        showTile(tile.id)
        click("medTile-${tile.id}")
        click("saveMedication")
        node("medSaveProgress").assertIsDisplayed()
        // The tab under the sheet: every entry point is visibly disabled.
        node("medTile-${tile.id}").assertIsNotEnabled()
        node("medPickerToggle").assertIsNotEnabled()
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
        for (tag in listOf("medPicker", "medPickerToggle", "addCustomMedicationBtn", "stopMedication-ui-m1", "editMedication-ui-m1", "delete-medication-ui-m1")) {
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
        openPicker()
        showTile(tile.id)
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

    // ---- Collapsed picker, "Show past", strings, empty state, tap counts ----

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
    private fun plural(id: Int, count: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(id, count, count)

    /** Catalog drugs this patient does not take, with a saved dose and frequency (so Save needs no extra tap). */
    private fun readyTiles() = catalog().filter { item ->
        item.dosage.isNotBlank() && item.frequency.isNotBlank() &&
            state.patient(patient.id)!!.medications.none { it.name.equals(item.name, ignoreCase = true) }
    }

    @Test
    fun thePickerStartsAsOneRowAndOpensToSearchChipsAndAListThatScrollsInsideItself() {
        host()
        scrollTo("medPickerToggle")
        node("medPickerToggle").assertHeightIsAtLeast(48.dp).assertIsEnabled()
        merged("medPickerToggle").assertTextContains(str(R.string.add_medication))
        for (tag in listOf("medSearchField", "medTab-common", "medTab-recent", "medTilesGrid")) node(tag).assertDoesNotExist()

        click("medPickerToggle")
        for (tag in listOf("medSearchField", "medTab-common", "medTab-recent", "medTilesGrid")) node(tag).assertExists()
        val grid = node("medTilesGrid").fetchSemanticsNode()
        val density = compose.activity.resources.displayMetrics.density
        assertTrue("the list is held to about five rows", grid.boundsInRoot.height <= 304 * density + 1f)
        assertTrue("the demo list has more than five drugs", catalog().size > 5)
        val range = grid.config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue("a long list scrolls inside the picker", range.maxValue() > 0f)

        click("medPickerToggle")
        node("medSearchField").assertDoesNotExist()
        node("medTilesGrid").assertDoesNotExist()
    }

    @Test
    fun searchKeepsOnlyMatchingTilesAndSaysSoWhenNothingMatches() {
        host()
        openPicker()
        val target = catalog().first()
        val query = target.name.take(4)
        node("medSearchField").performTextInput(query)
        compose.waitForIdle()
        for (item in catalog()) {
            val shown = exists("medTile-${item.id}")
            assertEquals(item.name, item.name.contains(query, ignoreCase = true), shown)
        }
        node("medSearchField").performTextReplacement("zzqx-no-drug")
        compose.waitForIdle()
        catalog().forEach { node("medTile-${it.id}").assertDoesNotExist() }
        node("medPickerEmpty").assertTextEquals(str(R.string.medication_picker_no_match, str(R.string.prescribe_medication)))
    }

    @Test
    fun aTileShowsItsNameAndDoseRouteAndFrequency() {
        host()
        openPicker()
        val item = readyTiles().first()
        showTile(item.id)
        merged("medTile-${item.id}").assertTextContains(item.name)
        val frequency = com.sherifradwan.ancnative.ui.medicationFrequencyLabelRes(item.frequency)?.let { str(it) } ?: item.frequency
        node("medTileMeta-${item.id}").assertTextEquals("${item.dosage} · ${item.route} · $frequency")
    }

    @Test
    fun recentListsTheDrugsThisPatientHadBefore() {
        val item = catalog().first()
        // Stored with a different case: the picker still counts it as recent.
        host(patient.medications + row("ui-m5", item.name.uppercase(), MEDICATION_STATUS_DISCONTINUED))
        openPicker()
        scrollTo("medTab-recent")
        click("medTab-recent")
        showTile(item.id)
        node("medTile-${item.id}").assertIsDisplayed()
        merged("medTab-recent").assertIsSelected()
        val given = state.patient(patient.id)!!.medications.map { it.name.lowercase() }
        val wrong = catalog().filter { it.name.lowercase() !in given && exists("medTile-${it.id}") }.map { it.name }
        assertTrue("only drugs the patient had: $wrong", wrong.isEmpty())
    }

    @Test
    fun addingTakesThreeTapsWithThePickerClosedAndTwoWhileItStaysOpen() {
        host()
        val (first, second) = readyTiles().take(2)
        var taps = 0
        fun tap(tag: String) {
            taps++
            click(tag)
        }

        scrollTo("medPickerToggle")
        tap("medPickerToggle")
        showTile(first.id)
        tap("medTile-${first.id}")
        tap("saveMedication")
        assertEquals("closed picker: open it, the tile, Save", 3, taps)
        node("medicationSheet").assertDoesNotExist()
        assertNotNull(state.patient(patient.id)!!.medications.firstOrNull { it.name == first.name })
        node("medSearchField").assertExists()

        taps = 0
        showTile(second.id)
        tap("medTile-${second.id}")
        tap("saveMedication")
        assertEquals("open picker: the tile, Save", 2, taps)
        assertNotNull(state.patient(patient.id)!!.medications.firstOrNull { it.name == second.name })
        assertEquals(2, commands.size)
    }

    @Test
    fun thePickerStaysOpenWithItsSearchThroughAddStopEditAndTabSwipes() {
        host()
        openPicker()
        scrollTo("medTab-recent")
        click("medTab-recent")
        merged("medTab-recent").assertIsSelected()
        click("medTab-common")
        merged("medTab-common").assertIsSelected()
        val item = readyTiles().first()
        val query = item.name.take(3)
        node("medSearchField").performTextInput(query)
        compose.waitForIdle()

        // Add from a tile.
        showTile(item.id)
        click("medTile-${item.id}")
        click("saveMedication")
        node("medSearchField").assertTextContains(query)
        // Stop a row.
        scrollTo("stopMedication-ui-m1")
        click("stopMedication-ui-m1")
        node("medStopUndoBar").assertIsDisplayed()
        scrollTo("medSearchField")
        node("medSearchField").assertTextContains(query)
        // Edit a row.
        scrollTo("editMedication-ui-m2")
        click("editMedication-ui-m2")
        node("medDosage").performTextReplacement("80 mg")
        click("saveMedication")
        node("medicationSheet").assertDoesNotExist()
        scrollTo("medSearchField")
        node("medSearchField").assertTextContains(query)
        // Swipe away to another tab and back.
        compose.openHubTab(PatientHubTab.VISITS)
        node("medSearchField").assertDoesNotExist()
        compose.openHubTab(PatientHubTab.MEDS)
        scrollTo("medSearchField")
        node("medSearchField").assertTextContains(query)
        assertEquals(medsTab, state.route)
    }

    @Test
    fun stoppedDrugsSitBehindShowPastWithoutAStopSymbol() {
        host()
        node("medication-ui-m3").assertDoesNotExist()
        scrollTo("medShowPastToggle")
        merged("medShowPastToggle").assertTextContains(plural(R.plurals.medication_show_past, 1))
        node("medShowPastToggle").assertHeightIsAtLeast(48.dp)
        click("medShowPastToggle")
        merged("medShowPastToggle").assertTextContains(plural(R.plurals.medication_hide_past, 1))
        scrollTo("medication-ui-m3")
        node("medication-ui-m3").assertIsDisplayed()
        node("stopMedication-ui-m3").assertDoesNotExist()
        node("editMedication-ui-m3").assertExists()
        click("medShowPastToggle")
        node("medication-ui-m3").assertDoesNotExist()
    }

    @Test
    fun showPastStaysOpenThroughStopAddEditAndTabSwipes() {
        host()
        openPast()
        // Stop: the row joins the past list, which stays open, and loses its stop symbol.
        scrollTo("stopMedication-ui-m1")
        click("stopMedication-ui-m1")
        scrollTo("medShowPastToggle")
        merged("medShowPastToggle").assertTextContains(plural(R.plurals.medication_hide_past, 2))
        scrollTo("medication-ui-m1")
        node("stopMedication-ui-m1").assertDoesNotExist()
        scrollTo("medication-ui-m3")
        // Add.
        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medName").performTextInput("UiNewPast")
        node("medDosage").performTextInput("1 mg")
        node("medFreqChoice-0").performScrollTo().performClick()
        click("saveMedication")
        scrollTo("medication-ui-m3")
        node("medication-ui-m3").assertExists()
        // Edit a past row.
        scrollTo("editMedication-ui-m3")
        click("editMedication-ui-m3")
        node("medNotes").performScrollTo().performTextInput("seen")
        click("saveMedication")
        node("medicationSheet").assertDoesNotExist()
        scrollTo("medication-ui-m3")
        node("medication-ui-m3").assertExists()
        // Tab swipe.
        compose.openHubTab(PatientHubTab.LABS)
        compose.openHubTab(PatientHubTab.MEDS)
        scrollTo("medication-ui-m3")
        node("medication-ui-m3").assertExists()
    }

    @Test
    fun theScreenShowsItsTitlesLabelsAndFrequenciesFromTheStringsFile() {
        host(patient.medications + row("ui-m6", "UiPrn").copy(frequency = "As needed (PRN)"))
        node("medsActiveTitle").assertTextEquals(plural(R.plurals.active_prescriptions_title, 3))
        merged("addCustomMedicationBtn").assertTextEquals(str(R.string.prescribe_medication))
        scrollTo("medication-ui-m6")
        node("medicationInstructions-ui-m6").assertTextEquals(
            str(R.string.medication_dose_route_frequency, "100 mg", "Oral", str(R.string.medication_frequency_as_needed)),
        )
        scrollTo("stopMedicationLabel-ui-m6")
        node("stopMedicationLabel-ui-m6").assertTextEquals(str(R.string.stop_medication_short))
        scrollTo("medPickerToggle")
        merged("medPickerToggle").assertTextEquals(str(R.string.add_medication))
        scrollTo("medShowPastToggle")
        merged("medShowPastToggle").assertTextEquals(plural(R.plurals.medication_show_past, 1))
    }

    @Test
    fun theTogglesExposeTheirStateAndAClickLabel() {
        host()
        scrollTo("medPickerToggle")
        val toggle = merged("medPickerToggle").fetchSemanticsNode().config
        assertEquals(str(R.string.state_collapsed), toggle[SemanticsProperties.StateDescription])
        assertEquals(str(R.string.action_expand), toggle[SemanticsActions.OnClick].label)
        assertEquals(androidx.compose.ui.semantics.Role.Button, toggle[SemanticsProperties.Role])
        click("medPickerToggle")
        assertEquals(str(R.string.state_expanded), merged("medPickerToggle").fetchSemanticsNode().config[SemanticsProperties.StateDescription])

        scrollTo("medShowPastToggle")
        assertEquals(str(R.string.state_collapsed), merged("medShowPastToggle").fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertEquals(str(R.string.action_expand), merged("medShowPastToggle").fetchSemanticsNode().config[SemanticsActions.OnClick].label)

        scrollTo("medicationToggle-ui-m1")
        val row = merged("medicationToggle-ui-m1").fetchSemanticsNode().config
        assertEquals(str(R.string.show_details), row[SemanticsActions.OnClick].label)
        assertEquals(androidx.compose.ui.semantics.Role.Button, row[SemanticsProperties.Role])
        assertEquals(str(R.string.state_collapsed), row[SemanticsProperties.StateDescription])
        // Stop says which drug; its visible label is not read twice.
        scrollTo("stopMedication-ui-m1")
        assertEquals(listOf(str(R.string.stop_medication_description, "UiIron")),
            merged("stopMedication-ui-m1").fetchSemanticsNode().config[SemanticsProperties.ContentDescription])
    }

    @Test
    fun aClosedRowShowsOneLineOfNotesAndTheWholeNoteWhenOpened() {
        val note = "UiHold if SBP < 100; recheck the blood pressure before every dose and call the clinic if dizzy"
        host(listOf(row("ui-n1", "UiNoted").copy(notes = note), row("ui-n2", "UiPlain")))
        scrollTo("medicationNotes-ui-n1")
        node("medicationNotes-ui-n1").assertTextEquals(note).assertIsDisplayed()
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node("medicationNotes-ui-n1").fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        assertEquals("one line while closed", 1, results.single().lineCount)
        assertTrue("a long note ends in an ellipsis", results.single().isLineEllipsized(0))
        // The preview sits under the dose, route and frequency.
        assertTrue(node("medicationNotes-ui-n1").getUnclippedBoundsInRoot().top >= node("medicationInstructions-ui-n1").getUnclippedBoundsInRoot().bottom)
        node("medicationNotes-ui-n2").assertDoesNotExist()

        click("medicationToggle-ui-n1")
        node("medicationNotes-ui-n1").assertDoesNotExist()
        compose.onNode(hasText(note) and inTagged("medication-ui-n1"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun aPatientWithNoMedicationsSeesTheEmptyStateAndItsMainAction() {
        host(emptyList())
        node("medsEmptyState").assertIsDisplayed()
        compose.onNode(hasText(str(R.string.medications_empty_title)) and inTagged("medsEmptyState"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText(str(R.string.medications_empty_hint)) and inTagged("medsEmptyState"), useUnmergedTree = true).assertIsDisplayed()
        merged("medEmptyPrescribe").assertTextContains(str(R.string.medications_empty_action)).assertIsEnabled()
            .assertHeightIsAtLeast(48.dp)
        node("medsActiveTitle").assertTextEquals(plural(R.plurals.active_prescriptions_title, 0))
        node("addCustomMedicationBtn").assertDoesNotExist()
        node("medShowPastToggle").assertDoesNotExist()
        click("medEmptyPrescribe")
        node("medicationSheet").assertIsDisplayed()
        assertEquals("", state.medicationDraft!!.values.name)
        node("medEmptyPrescribe").assertIsNotEnabled()
    }

    @Test
    fun onlyStoppedDrugsShowNoActiveMedicationsInsteadOfTheEmptyState() {
        host(listOf(row("ui-m3", "UiOld", MEDICATION_STATUS_DISCONTINUED)))
        node("medsEmptyState").assertDoesNotExist()
        node("medsNoActive").assertTextEquals(str(R.string.no_active_medications))
        scrollTo("medShowPastToggle")
        merged("medShowPastToggle").assertTextContains(plural(R.plurals.medication_show_past, 1))
    }

    // ---- 360 dp at 200% text ----

    private val longNames = listOf(
        row("ui-l1", "UiAmoxicillin / Clavulanate potassium extended-release"),
        row("ui-l2", "UiMethyldopa").copy(frequency = "As needed (PRN)", dosage = "250 mg twice on bad days"),
    )

    private fun assertInside(inner: String, outer: String) {
        val i = node(inner).getUnclippedBoundsInRoot()
        val o = node(outer).getUnclippedBoundsInRoot()
        assertTrue("$inner sticks out of $outer", i.left >= o.left && i.right <= o.right)
    }

    @Test
    fun rowsKeepTheirTextWholeAndTheirButtonsAt48dpAt360dpAnd200PercentText() {
        host(longNames, fontScale = 2f)
        val width = node(NARROW_HOST).getUnclippedBoundsInRoot().width
        assertTrue("held to 360 dp", width <= 360.dp)
        for (med in longNames) {
            scrollTo("medication-${med.id}")
            compose.assertTextNotClipped("${med.id} name", hasTestTag("medicationName-${med.id}"))
            compose.assertTextNotClipped("${med.id} dose, route and frequency", hasTestTag("medicationInstructions-${med.id}"))
            for (tag in listOf("medicationName-", "medicationInstructions-")) {
                assertInside(tag + med.id, "medication-${med.id}")
            }
            // The actions sit below the text, so they never narrow it. Both are measured after the same scroll.
            for (tag in listOf("stopMedication-", "delete-medication-", "editMedication-")) {
                scrollTo(tag + med.id)
                val textBottom = node("medicationInstructions-${med.id}").getUnclippedBoundsInRoot().bottom
                node(tag + med.id).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
                assertTrue("$tag${med.id} below the text", node(tag + med.id).getUnclippedBoundsInRoot().top >= textBottom)
                assertInside(tag + med.id, "medication-${med.id}")
            }
            compose.assertTextNotClipped("${med.id} Stop label", hasTestTag("stopMedicationLabel-${med.id}"))
        }
    }

    @Test
    fun theHeaderPrescribeButtonIsNotClippedAt360dpAnd200PercentText() {
        host(longNames, fontScale = 2f)
        compose.assertTextNotClipped("Prescribe", hasText(str(R.string.prescribe_medication)) and inTagged("addCustomMedicationBtn"))
        compose.assertTextNotClipped("title", hasTestTag("medsActiveTitle"))
        assertInside("addCustomMedicationBtn", NARROW_HOST)
        node("addCustomMedicationBtn").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun thePickerKeepsItsTextWholeAt360dpAnd200PercentText() {
        host(longNames, fontScale = 2f)
        scrollTo("medPickerToggle")
        node("medPickerToggle").assertHeightIsAtLeast(48.dp)
        compose.assertTextNotClipped("picker row", hasText(str(R.string.add_medication)) and hasAnyAncestor(hasTestTag("medPickerToggle")))
        click("medPickerToggle")
        val item = readyTiles().first()
        showTile(item.id)
        node("medTile-${item.id}").assertHeightIsAtLeast(48.dp)
        compose.assertTextNotClipped("tile name", hasText(item.name) and hasAnyAncestor(hasTestTag("medTile-${item.id}")))
        compose.assertTextNotClipped("tile meta", hasTestTag("medTileMeta-${item.id}"))
        assertInside("medTile-${item.id}", NARROW_HOST)
        for (tag in listOf("medTab-common", "medTab-recent")) {
            scrollTo(tag)
            node(tag).assertHeightIsAtLeast(48.dp)
        }
    }

    // ---- The sheet with the keyboard open at 360 dp ----

    /** The window the keyboard covers: its height and the keyboard's inset, in px; null while the keyboard is hidden. */
    private fun keyboard(): Pair<Int, Int>? {
        var found: Pair<Int, Int>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for (view in WindowInspector.getGlobalWindowViews()) {
                val insets = view.rootWindowInsets ?: continue
                if (!insets.isVisible(WindowInsets.Type.ime())) continue
                val bottom = insets.getInsets(WindowInsets.Type.ime()).bottom
                if (bottom > (found?.second ?: 0)) found = view.height to bottom
            }
        }
        return found
    }

    private fun awaitKeyboard() {
        compose.waitUntil(10_000) { keyboard() != null }
    }

    /** The node is wholly above the keyboard (after any scroll that brings the focused field into view settles). */
    private fun assertAboveKeyboard(tag: String) {
        compose.waitForIdle()
        compose.waitUntil(5_000) {
            val (height, ime) = keyboard() ?: return@waitUntil false
            node(tag).fetchSemanticsNode().boundsInWindow.bottom <= height - ime + 1f
        }
        node(tag).assertIsDisplayed()
    }

    private fun assertFocused(tag: String) {
        compose.waitForIdle()
        compose.waitUntil(5_000) { runCatching { node(tag).assertIsFocused() }.isSuccess }
        node(tag).assertIsFocused()
    }

    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun withTheKeyboardOpenAt360dpTheFocusedFieldAndSaveStayVisibleAndEnterMovesOn() {
        host()
        assertTrue(
            "run .agents/scripts/set_emulator_360dp.sh first",
            compose.activity.resources.configuration.screenWidthDp <= 360,
        )
        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medName").performClick()
        awaitKeyboard()
        assertFocused("medName")
        assertAboveKeyboard("medName")
        assertAboveKeyboard("saveMedication")
        for ((from, to) in listOf("medName" to "medDosage", "medDosage" to "medDate", "medDate" to "medNotes")) {
            node(from).performImeAction()
            assertFocused(to)
            assertTrue("the keyboard stays up on $to", keyboard() != null)
            assertAboveKeyboard(to)
            assertAboveKeyboard("saveMedication")
        }
    }

    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun saveAndAddAnotherKeepsTheKeyboardAndPutsFocusOnTheNextName() {
        host()
        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medFreqChoice-0").performScrollTo().performClick()
        node("medName").performClick()
        node("medName").performTextInput("UiKeyboard")
        node("medName").performImeAction()
        assertFocused("medDosage")
        node("medDosage").performTextInput("1 mg")
        awaitKeyboard()
        val firstSession = state.medicationDraft!!.editorSessionId
        click("addAnotherMedication")
        node("medicationSheet").assertIsDisplayed()
        assertNotNull(state.patient(patient.id)!!.medications.firstOrNull { it.name == "UiKeyboard" })
        assertFalse("a new draft", state.medicationDraft!!.editorSessionId == firstSession)
        assertFocused("medName")
        assertEquals("", state.medicationDraft!!.values.name)
        assertTrue("the keyboard is still up", keyboard() != null)
        assertAboveKeyboard("medName")
        assertAboveKeyboard("saveMedication")
    }

    // ---- Fix round 1: tab sync, picker fallbacks, isolation, chained add, contrast ----

    @Test
    fun swipingAwayAndBackToTheOpeningTabReportsIt() {
        host()
        compose.onNodeWithTag(PatientHubTab.MEDS.contentTag()).performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.waitUntil(5_000) { state.route == AncRoute.Workspace(patient.id, WorkspaceSection.LABORATORY) }
        compose.onNodeWithTag(PatientHubTab.LABS.contentTag()).performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.waitUntil(5_000) { state.route == medsTab }
        assertEquals("the return to the tab the screen opened on is reported", medsTab, state.route)
        node(PatientHubTab.MEDS.contentTag()).assertIsDisplayed()
    }

    @Test
    fun recentWithNoHistorySaysSo() {
        host()
        openPicker()
        scrollTo("medTab-recent")
        click("medTab-recent")
        catalog().forEach { node("medTile-${it.id}").assertDoesNotExist() }
        node("medPickerEmpty").assertTextEquals(str(R.string.no_recent_medications, str(R.string.prescribe_medication)))
    }

    @Test
    fun tilesWithoutADoseOrFrequencySaySoAndABlankFrequencyOpensWithNoneChosen() {
        val repo = ClinicalCatalogRepository.getInstance(compose.activity)
        val noFrequency = MedicationCatalogItem(id = "ui-nofreq", name = "UiNoFrequency", dosage = "7 mg", route = "Oral", frequency = "")
        repo.addMedicationCatalogItem(noFrequency)
        repo.addCustomChoice(CustomChoiceList.MEDICATION_NAME, "UiOwnDrug", emptyList())
        val own = repo.customChoices(CustomChoiceList.MEDICATION_NAME).value.first { it.label == "UiOwnDrug" }
        try {
            host()
            openPicker()
            node("medSearchField").performTextInput("UiNo")
            compose.waitForIdle()
            showTile(noFrequency.id)
            node("medTileMeta-${noFrequency.id}").assertTextEquals(
                str(R.string.medication_dose_route_frequency, "7 mg", "Oral", str(R.string.medication_choose_frequency)),
            )
            node("medSearchField").performTextReplacement("UiOwn")
            compose.waitForIdle()
            showTile("custom_${own.id}")
            node("medTileMeta-custom_${own.id}").assertTextEquals(str(R.string.set_dose_in_editor))

            node("medSearchField").performTextReplacement("UiNo")
            compose.waitForIdle()
            showTile(noFrequency.id)
            click("medTile-${noFrequency.id}")
            // The sheet opens with no frequency chosen, so the doctor picks one. (The save rules do not require a
            // frequency today; ClinicalCommands is outside Stage 1, so no error on Save is asserted here.)
            node("medicationSheet").assertIsDisplayed()
            assertEquals("", state.medicationDraft!!.values.frequency)
            COMMON_MEDICATION_FREQUENCIES.indices.forEach { node("medFreqChoice-$it").performScrollTo().assertIsNotSelected() }
            node("medFreqChoice-other").assertIsNotSelected()
        } finally {
            repo.deleteMedicationCatalogItem(noFrequency.id)
            repo.removeCustomChoice(CustomChoiceList.MEDICATION_NAME, own.id)
        }
    }

    @Test
    fun thePickerShowPastAndOpenRowOfOnePatientDoNotFollowToAnother() {
        val otherId = DemoFixtures.patients[1].id
        val other = DemoFixtures.patients[1].copy(
            status = "active",
            medications = listOf(
                row("ui-b1", "UiOtherActive").copy(patientId = otherId),
                row("ui-b2", "UiOtherOld", MEDICATION_STATUS_DISCONTINUED).copy(patientId = otherId),
            ),
        )
        host()
        compose.runOnIdle { state = state.copy(patients = state.patients + other) }
        compose.waitForIdle()
        scrollTo("medicationToggle-ui-m1")
        click("medicationToggle-ui-m1")
        openPast()
        openPicker()
        node("medSearchField").performTextInput("Ui")
        compose.runOnIdle { onEvent(AncEvent.OpenWorkspace(other.id)) }
        compose.runOnIdle { onEvent(AncEvent.SelectSection(WorkspaceSection.MEDICATIONS)) }
        compose.waitForIdle()

        scrollTo("medPickerToggle")
        node("medSearchField").assertDoesNotExist()
        merged("medPickerToggle").assertExists()
        node("medication-ui-b2").assertDoesNotExist()
        scrollTo("medShowPastToggle")
        merged("medShowPastToggle").assertTextEquals(plural(R.plurals.medication_show_past, 1))
        scrollTo("medicationToggle-ui-b1")
        assertEquals(str(R.string.state_collapsed), merged("medicationToggle-ui-b1").fetchSemanticsNode().config[SemanticsProperties.StateDescription])
    }

    @Test
    fun showPastStaysUsableWhileAStopSaves() {
        host()
        holdSaves = true
        scrollTo("stopMedication-ui-m1")
        click("stopMedication-ui-m1")
        node("medStopProgress").assertIsDisplayed()
        scrollTo("medShowPastToggle")
        node("medShowPastToggle").assertIsEnabled()
        click("medShowPastToggle")
        scrollTo("medication-ui-m3")
        node("medication-ui-m3").assertExists()
        releaseHeldSave()
    }

    @Test
    fun aReadOnlyPatientWithNoMedicationsSeesTheEmptyStateWithoutItsAction() {
        host(emptyList())
        compose.runOnIdle { state = state.copy(patients = state.patients.map { it.copy(status = "archived") }) }
        compose.waitForIdle()
        node("medsEmptyState").assertIsDisplayed()
        compose.onNode(hasText(str(R.string.medications_empty_title)) and inTagged("medsEmptyState"), useUnmergedTree = true).assertIsDisplayed()
        node("medEmptyPrescribe").assertDoesNotExist()
        node("addCustomMedicationBtn").assertDoesNotExist()
    }

    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun theNextNameTakesFocusEvenWhenTheSheetClosesBetweenTheTwoDrafts() {
        host()
        chainAfterAFrame = true
        scrollTo("addCustomMedicationBtn")
        click("addCustomMedicationBtn")
        node("medFreqChoice-0").performScrollTo().performClick()
        node("medName").performClick()
        node("medName").performTextInput("UiGap")
        node("medDosage").performClick()
        node("medDosage").performTextInput("1 mg")
        awaitKeyboard()
        click("addAnotherMedication")
        // The commit closed the draft and a frame passed: the sheet is gone before the next start.
        node("medicationSheet").assertDoesNotExist()
        assertNotNull(state.patient(patient.id)!!.medications.firstOrNull { it.name == "UiGap" })
        val next = deferredChain!!
        compose.runOnIdle {
            onEvent(AncEvent.StartMedicationEditor(next, null, UUID.randomUUID().toString(), UUID.randomUUID().toString(), keepSavedNotice = true))
        }
        compose.waitForIdle()
        node("medicationSheet").assertIsDisplayed()
        assertTrue(state.medicationDraft!!.chainedAdd)
        assertFocused("medName")
        awaitKeyboard()
        assertAboveKeyboard("medName")
        assertAboveKeyboard("saveMedication")
    }

    /** The node is wholly visible (not clipped by its lists) and wholly above the keyboard. */
    private fun assertWhollyAboveKeyboard(tag: String) {
        compose.waitForIdle()
        compose.waitUntil(5_000) {
            val (height, ime) = keyboard() ?: return@waitUntil false
            val n = node(tag).fetchSemanticsNode()
            val shown = n.boundsInWindow
            shown.height >= n.size.height - 1f && shown.bottom <= height - ime + 1f
        }
    }

    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun searchingThePickerWithTheKeyboardOpenKeepsTheResultsAboveTheKeyboard() {
        host(longList)
        assertTrue(compose.activity.resources.configuration.screenWidthDp <= 360)
        openPicker()
        scrollTo("medSearchField")
        node("medSearchField").performClick()
        assertFocused("medSearchField")
        awaitKeyboard()
        val query = readyTiles().first().name.take(4)
        node("medSearchField").performTextInput(query)
        // The first result (the list's order) is wholly visible above the keyboard, without closing it.
        val first = catalog().first { it.name.contains(query, ignoreCase = true) }
        assertWhollyAboveKeyboard("medTile-${first.id}")
        assertTrue("the keyboard is still up", keyboard() != null)
        // The Search key closes the keyboard; the search text stays.
        node("medSearchField").performImeAction()
        compose.waitUntil(5_000) { keyboard() == null }
        node("medSearchField").assertTextContains(query)
    }

    /** WCAG contrast of two opaque colours. */
    private fun contrast(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return maxOf(la, lb) / minOf(la, lb)
    }

    @Test
    fun theStopLabelAndIconHaveEnoughContrastInBothThemes() {
        for (dark in listOf(false, true)) {
            var label = androidx.compose.ui.graphics.Color.Unspecified
            var card = androidx.compose.ui.graphics.Color.Unspecified
            compose.activityRule.scenario.onActivity { activity ->
                activity.setContent {
                    AncTheme(darkTheme = dark) {
                        label = androidx.compose.material3.MaterialTheme.colorScheme.error
                        card = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow
                    }
                }
            }
            compose.waitForIdle()
            val ratio = contrast(label, card)
            assertTrue("Stop text (dark=$dark) $ratio < 4.5", ratio >= 4.5)
        }
    }

    private companion object {
        const val NARROW_HOST = "medsNarrowHost"
    }
}
