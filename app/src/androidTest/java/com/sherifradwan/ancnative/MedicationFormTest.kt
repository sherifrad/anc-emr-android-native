package com.sherifradwan.ancnative

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.ui.patienthub.components.MedicationSheetButtons
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.MedicationDraft
import com.sherifradwan.ancnative.state.MedicationDraftField
import com.sherifradwan.ancnative.state.MedicationDraftValues
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.SaveUiError
import com.sherifradwan.ancnative.state.stringRes
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.patienthub.components.MedicationSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MedicationFormTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<AncEvent>()
    private var phase by mutableStateOf(SavePhase.IDLE)
    private var error by mutableStateOf<SaveUiError?>(null)

    private val activeValues = MedicationDraftValues(route = "Oral", status = "Active", startDate = "2026-11-10")

    private fun str(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun draft(existing: Boolean, values: MedicationDraftValues, errors: Set<ClinicalValidationError>, session: String = "s") =
        MedicationDraft(
            editorSessionId = session,
            patientId = "p",
            medicationId = "m",
            basePatientRevision = 1L,
            baseMedicationRevision = if (existing) 3L else null,
            original = values,
            values = values,
            errors = errors,
        )

    /** Hosts the sheet and applies draft-field events locally, as the reducer would. */
    private fun mount(
        existing: Boolean,
        touched: Boolean = false,
        savePhase: SavePhase = SavePhase.IDLE,
        saveError: SaveUiError? = null,
        values: MedicationDraftValues = activeValues,
        errors: Set<ClinicalValidationError> = emptySet(),
    ) {
        events.clear()
        phase = savePhase
        error = saveError
        compose.setContent {
            var current by remember { mutableStateOf(draft(existing, values, errors)) }
            AncTheme {
                MedicationSheet(
                    draft = current,
                    savePhase = phase,
                    saveError = error,
                    touched = touched,
                    onEvent = { event ->
                        events += event
                        if (event is AncEvent.MedicationDraftChanged) {
                            val v = current.values
                            val next = when (event.field) {
                                MedicationDraftField.NAME -> v.copy(name = event.value)
                                MedicationDraftField.DOSAGE -> v.copy(dosage = event.value)
                                MedicationDraftField.ROUTE -> v.copy(route = event.value)
                                MedicationDraftField.FREQUENCY -> v.copy(frequency = event.value)
                                MedicationDraftField.START_DATE -> v.copy(startDate = event.value)
                                MedicationDraftField.END_DATE -> v.copy(endDate = event.value)
                                MedicationDraftField.STATUS -> v.copy(status = event.value)
                                MedicationDraftField.NOTES -> v.copy(notes = event.value)
                                MedicationDraftField.ADVERSE_NOTES -> v.copy(adverseNotes = event.value)
                            }
                            current = current.copy(values = next)
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun changes(field: MedicationDraftField) =
        events.filterIsInstance<AncEvent.MedicationDraftChanged>().filter { it.field == field }

    private fun assertFocused(tag: String) {
        compose.waitForIdle()
        compose.waitUntil(5_000) { runCatching { compose.onNodeWithTag(tag).assertIsFocused() }.isSuccess }
        compose.onNodeWithTag(tag).assertIsFocused()
    }

    private fun shown(tag: String) = compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()

    @Test
    fun addModeRendersEveryFieldAndAllThreeButtons() {
        mount(existing = false)
        listOf(
            "medName", "medDosage", "medRouteChoice-0", "medFreqChoice-0", "medFreqChoice-other",
            "medStatusChoice-0", "medStatusChoice-1", "medDate", "medNotes",
        ).forEach { shown(it) }
        compose.onNodeWithTag("cancelMedication").assertIsDisplayed()
        compose.onNodeWithTag("saveMedication").assertIsDisplayed()
        compose.onNodeWithTag("addAnotherMedication").assertIsDisplayed()
    }

    @Test
    fun editModeHasNoSaveAndAddAnother() {
        mount(existing = true)
        compose.onNodeWithTag("addAnotherMedication").assertDoesNotExist()
        compose.onNodeWithText(str(R.string.edit_medication)).assertIsDisplayed()
    }

    @Test
    fun saveSendsSubmitMedication() {
        mount(existing = false)
        compose.onNodeWithTag("saveMedication").performClick()
        assertEquals(listOf<AncEvent>(AncEvent.SubmitMedication), events)
    }

    @Test
    fun saveAndAddAnotherSendsItsOwnEvent() {
        mount(existing = false)
        compose.onNodeWithTag("addAnotherMedication").performClick()
        assertEquals(listOf<AncEvent>(AncEvent.SubmitMedicationAndAddAnother), events)
    }

    @Test
    fun enterMovesFocusFromNameToDoseToDateToNotes() {
        mount(existing = false)
        compose.onNodeWithTag("medName").performClick()
        compose.onNodeWithTag("medName").performImeAction()
        assertFocused("medDosage")
        compose.onNodeWithTag("medDosage").performImeAction()
        assertFocused("medDate")
        compose.onNodeWithTag("medDate").performImeAction()
        assertFocused("medNotes")
    }

    @Test
    fun enterInNotesInsertsNewLineAndKeepsFocus() {
        mount(existing = false)
        compose.onNodeWithTag("medNotes").performScrollTo().performClick()
        compose.onNodeWithTag("medNotes").performKeyInput { pressKey(Key.Enter) }
        compose.waitForIdle()
        assertFocused("medNotes")
        assertTrue(changes(MedicationDraftField.NOTES).any { it.value.contains("\n") })
    }

    @Test
    fun otherFrequencyTakesFocusAndEnterFromDoseGoesThere() {
        mount(existing = false)
        compose.onNodeWithTag("medFreqChoice-other").performScrollTo().performClick()
        shown("medFreqOther")
        assertFocused("medFreqOther")
        compose.onNodeWithTag("medDosage").performScrollTo().performClick()
        compose.onNodeWithTag("medDosage").performImeAction()
        assertFocused("medFreqOther")
        compose.onNodeWithTag("medFreqOther").performTextInput("Every 12 hours")
        assertEquals("Every 12 hours", changes(MedicationDraftField.FREQUENCY).last().value)
    }

    @Test
    fun routeIsReadOnlyOnExistingRowWithNote() {
        mount(existing = true, values = activeValues.copy(name = "X", dosage = "1", route = "IM", frequency = "Once daily"))
        shown("medRouteReadOnly")
        compose.onNodeWithTag("medRouteValue").assertTextContains("IM")
        compose.onNodeWithText(str(R.string.medication_route_locked_note)).assertIsDisplayed()
        compose.onNodeWithTag("medRouteChoice-0").assertDoesNotExist()
    }

    @Test
    fun routeChipSendsItsExactValue() {
        mount(existing = false)
        val routes = ClinicalVocabulary.medicationRoutes.toList()
        compose.onNodeWithTag("medRouteChoice-1").performScrollTo().performClick()
        assertEquals(listOf(routes[1]), changes(MedicationDraftField.ROUTE).map { it.value })
        compose.onNodeWithTag("medRouteReadOnly").assertDoesNotExist()
    }

    @Test
    fun togglingToStoppedSwapsDateLabelAndTypingWritesEndDate() {
        mount(existing = true, values = activeValues.copy(name = "X", dosage = "1", frequency = "Once daily"))
        compose.onNodeWithText(str(R.string.start_date)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("medDate").performTextInput("1")
        assertEquals(1, changes(MedicationDraftField.START_DATE).size)
        assertTrue(changes(MedicationDraftField.END_DATE).isEmpty())

        compose.onNodeWithTag("medStatusChoice-1").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("Discontinued", changes(MedicationDraftField.STATUS).single().value)
        compose.onNodeWithText(str(R.string.medication_stop_date)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("medDate").performTextInput("2")
        assertEquals(1, changes(MedicationDraftField.END_DATE).size)
        assertEquals(1, changes(MedicationDraftField.START_DATE).size)
    }

    @Test
    fun stoppedRowShowsItsStopDate() {
        mount(existing = true, values = activeValues.copy(name = "X", dosage = "1", frequency = "Once daily", status = "Discontinued", startDate = "2026-11-01", endDate = "2026-11-09"))
        compose.onNodeWithText(str(R.string.medication_stop_date)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("medDate").assertTextContains("2026-11-09")
    }

    @Test
    fun clickingTheAlreadySelectedStatusSendsNothing() {
        mount(existing = true, values = activeValues.copy(name = "X", dosage = "1", frequency = "Once daily"))
        compose.onNodeWithTag("medStatusChoice-0").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(events.isEmpty())
    }

    @Test
    fun validationErrorsShowNextToTheirFieldsAndInTheMessage() {
        mount(
            existing = false,
            values = activeValues.copy(frequency = "Custom words", startDate = ""),
            errors = setOf(
                ClinicalValidationError.REQUIRED_NAME,
                ClinicalValidationError.REQUIRED_DOSAGE,
                ClinicalValidationError.REQUIRED_FREQUENCY,
                ClinicalValidationError.REQUIRED_DATE,
            ),
        )
        compose.onNodeWithText(str(R.string.correct_highlighted_fields)).assertIsDisplayed()
        // name, dose, the Other box and the date field each carry the per-field message.
        compose.onAllNodesWithText(str(R.string.correct_highlighted_field)).assertCountEquals(4)
        shown("medFreqOther")
    }

    @Test
    fun missingFrequencyShowsItsMessageWhenNoOtherBox() {
        mount(existing = false, errors = setOf(ClinicalValidationError.REQUIRED_FREQUENCY))
        shown("medFreqError")
        compose.onNodeWithText(str(R.string.medication_frequency_required)).assertIsDisplayed()
    }

    @Test
    fun cancelIsEnabledWhenIdle() {
        mount(existing = false)
        compose.onNodeWithTag("cancelMedication").assertIsEnabled()
        compose.onNodeWithTag("saveMedication").assertIsEnabled()
        compose.onNodeWithTag("addAnotherMedication").assertIsEnabled()
        compose.onNodeWithTag("medSaveProgress").assertDoesNotExist()
    }

    @Test
    fun cancelOnUntouchedFormClosesAtOnce() {
        mount(existing = false, touched = false)
        compose.onNodeWithTag("cancelMedication").performClick()
        assertEquals(listOf<AncEvent>(AncEvent.CancelEditor), events)
        compose.onNodeWithTag("medDiscardDialog").assertDoesNotExist()
    }

    @Test
    fun cancelOnTouchedFormAsksFirstAndKeepEditingSendsNothing() {
        mount(existing = false, touched = true)
        compose.onNodeWithTag("cancelMedication").performClick()
        compose.onNodeWithTag("medDiscardDialog").assertIsDisplayed()
        assertTrue(events.isEmpty())
        compose.onNodeWithTag("medDiscardKeep").performClick()
        compose.onNodeWithTag("medDiscardDialog").assertDoesNotExist()
        assertTrue(events.isEmpty())
    }

    @Test
    fun discardingOnTouchedFormSendsCancel() {
        mount(existing = false, touched = true)
        compose.onNodeWithTag("cancelMedication").performClick()
        compose.onNodeWithTag("medDiscardConfirm").performClick()
        assertEquals(listOf<AncEvent>(AncEvent.CancelEditor), events)
    }

    @Test
    fun swipingDownOnTouchedFormAsksInsteadOfHiding() {
        mount(existing = false, touched = true)
        compose.onNodeWithTag("medicationSheet").performTouchInput { swipeDown() }
        compose.waitForIdle()
        compose.onNodeWithTag("medDiscardDialog").assertIsDisplayed()
        compose.onNodeWithTag("medicationSheet").assertIsDisplayed()
        assertTrue(events.isEmpty())
    }

    @Test
    fun buttonsAndFieldsAreDisabledWithProgressWhileSaving() {
        mount(existing = false, savePhase = SavePhase.PENDING)
        compose.onNodeWithTag("cancelMedication").assertIsNotEnabled()
        compose.onNodeWithTag("saveMedication").assertIsNotEnabled()
        compose.onNodeWithTag("addAnotherMedication").assertIsNotEnabled()
        compose.onNodeWithTag("medName").assertIsNotEnabled()
        compose.onNodeWithTag("medSaveProgress").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.medication_saving)).assertIsDisplayed()
    }

    @Test
    fun sheetCannotBeDismissedWhileSaving() {
        mount(existing = false, savePhase = SavePhase.PENDING)
        compose.onNodeWithTag("medicationSheet").performTouchInput { swipeDown() }
        compose.waitForIdle()
        compose.onNodeWithTag("medicationSheet").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("medicationSheet").assertIsDisplayed()
        assertTrue(events.isEmpty())
        compose.runOnIdle { phase = SavePhase.RECONCILING }
        compose.onNodeWithTag("medicationSheet").performTouchInput { swipeDown() }
        compose.waitForIdle()
        compose.onNodeWithTag("medicationSheet").assertIsDisplayed()
        assertTrue(events.isEmpty())
    }

    @Test
    fun reconcilingShowsItsMessageWithControlsDisabled() {
        mount(existing = false, savePhase = SavePhase.RECONCILING)
        compose.onNodeWithText(str(R.string.save_reconciling)).assertIsDisplayed()
        compose.onNodeWithTag("saveMedication").assertIsNotEnabled()
        compose.onNodeWithTag("cancelMedication").assertIsNotEnabled()
    }

    @Test
    fun unresolvedShowsItsMessageAndCurrentlyDisablesEveryControl() {
        mount(existing = false, savePhase = SavePhase.UNRESOLVED, saveError = SaveUiError.OUTCOME_UNRESOLVED)
        compose.onNodeWithText(str(R.string.save_unresolved)).assertIsDisplayed()
        compose.onNodeWithTag("cancelMedication").assertIsNotEnabled()
        compose.onNodeWithTag("saveMedication").assertIsNotEnabled()
        compose.onNodeWithTag("addAnotherMedication").assertIsNotEnabled()
        compose.onNodeWithTag("medName").assertIsNotEnabled()
    }

    @Test
    fun rejectedSaveShowsExistingMessageAndKeepsValues() {
        mount(
            existing = true,
            savePhase = SavePhase.ERROR,
            saveError = SaveUiError.STALE_OR_CONFLICT,
            values = activeValues.copy(name = "Aspirin", dosage = "75 mg", frequency = "Once daily"),
        )
        compose.onNodeWithText(str(R.string.save_stale_conflict)).assertIsDisplayed()
        compose.onNodeWithTag("medName").assertTextContains("Aspirin")
        compose.onNodeWithTag("saveMedication").assertIsEnabled()
        compose.onNodeWithTag("cancelMedication").assertIsEnabled()
    }

    @Test
    fun everySaveErrorShowsItsOwnMessage() {
        mount(existing = true, savePhase = SavePhase.ERROR, saveError = SaveUiError.entries.first())
        SaveUiError.entries.forEach { e ->
            compose.runOnIdle { error = e }
            compose.waitForIdle()
            compose.onNodeWithText(str(e.stringRes())).assertIsDisplayed()
            compose.onNodeWithTag("saveMedication").assertIsEnabled()
            compose.onNodeWithTag("cancelMedication").assertIsEnabled()
        }
    }

    @Test
    fun swipingDownOnUntouchedFormCancelsAtOnce() {
        mount(existing = false, touched = false)
        compose.onNodeWithTag("medicationSheet").performTouchInput { swipeDown() }
        compose.waitUntil(5_000) { events.contains(AncEvent.CancelEditor) }
        compose.onNodeWithTag("medDiscardDialog").assertDoesNotExist()
    }

    @Test
    fun stoppedBackToActiveWritesStartDateAgain() {
        mount(existing = true, values = activeValues.copy(name = "X", dosage = "1", frequency = "Once daily", status = "Discontinued", startDate = "2026-11-01", endDate = "2026-11-09"))
        compose.onNodeWithTag("medStatusChoice-0").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("Active", changes(MedicationDraftField.STATUS).single().value)
        compose.onNodeWithText(str(R.string.start_date)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("medDate").performTextInput("1")
        assertEquals(1, changes(MedicationDraftField.START_DATE).size)
        assertTrue(changes(MedicationDraftField.END_DATE).isEmpty())
    }

    @Test
    fun otherChosenWithEmptyTextShowsTheFrequencyError() {
        mount(existing = false, errors = setOf(ClinicalValidationError.REQUIRED_FREQUENCY))
        compose.onNodeWithTag("medFreqChoice-other").performScrollTo().performClick()
        compose.waitForIdle()
        shown("medFreqOther")
        compose.onAllNodesWithText(str(R.string.correct_highlighted_field)).assertCountEquals(1)
        compose.onNodeWithTag("medFreqError").assertDoesNotExist()
    }

    @Test
    fun nextDraftWithCatalogFrequencyDoesNotKeepOtherMode() {
        events.clear()
        var current by mutableStateOf(draft(false, activeValues.copy(frequency = "Every 36 hours"), emptySet(), "s1"))
        compose.setContent {
            AncTheme {
                MedicationSheet(draft = current, savePhase = SavePhase.IDLE, saveError = null, touched = false, onEvent = { events += it })
            }
        }
        compose.waitForIdle()
        shown("medFreqOther")
        compose.runOnIdle { current = draft(false, activeValues.copy(frequency = "Once daily"), emptySet(), "s2") }
        compose.waitForIdle()
        compose.onNodeWithTag("medFreqOther").assertDoesNotExist()
        compose.onNodeWithTag("medFreqChoice-0").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("medDosage").performScrollTo().performClick()
        compose.onNodeWithTag("medDosage").performImeAction()
        assertFocused("medDate")
    }

    @Test
    fun theChainedNameFocusHappensOnceAndNotAgainAfterRecreation() {
        val tester = StateRestorationTester(compose)
        val chained = draft(false, activeValues, emptySet()).copy(chainedAdd = true)
        tester.setContent {
            AncTheme {
                MedicationSheet(draft = chained, savePhase = SavePhase.IDLE, saveError = null, touched = false, onEvent = { events += it })
            }
        }
        assertFocused("medName")
        compose.onNodeWithTag("medNotes").performScrollTo().performClick()
        assertFocused("medNotes")

        // A rotation or theme change rebuilds the sheet: the same draft must not pull focus back to the name.
        tester.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        repeat(5) { compose.mainClock.advanceTimeByFrame() }
        compose.waitForIdle()
        compose.onNodeWithTag("medName").assertIsNotFocused()
    }

    @Test
    fun discardPromptSurvivesRecreation() {
        val tester = StateRestorationTester(compose)
        tester.setContent {
            AncTheme {
                MedicationSheet(draft = draft(false, activeValues, emptySet()), savePhase = SavePhase.IDLE, saveError = null, touched = true, onEvent = { events += it })
            }
        }
        compose.onNodeWithTag("cancelMedication").performClick()
        compose.onNodeWithTag("medDiscardDialog").assertIsDisplayed()
        tester.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.onNodeWithTag("medDiscardDialog").assertIsDisplayed()
    }

    @Test
    fun bottomButtonsAreNotClippedAt360dpAnd200PercentText() = assertButtonsUnclipped(saving = false)

    @Test
    fun savingLabelIsNotClippedAt360dpAnd200PercentText() = assertButtonsUnclipped(saving = true)

    private fun assertButtonsUnclipped(saving: Boolean) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                AncTheme {
                    Box(Modifier.width(360.dp)) {
                        MedicationSheetButtons(
                            isExistingRow = false,
                            enabled = !saving,
                            saving = saving,
                            onCancel = {},
                            onSave = {},
                            onSaveAndAddAnother = {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        val labels = mapOf(
            "cancelMedication" to str(R.string.cancel),
            "saveMedication" to str(if (saving) R.string.medication_saving else R.string.save),
            "addAnotherMedication" to str(R.string.save_and_add_another_med),
        )
        labels.forEach { (tag, label) ->
            val button = compose.onNodeWithTag(tag)
            button.assertIsDisplayed()
            assertTrue("$tag overflows 360dp", button.getUnclippedBoundsInRoot().right <= 360.dp)
            val text = compose.onNode(hasText(label) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)
            text.assertIsDisplayed()
            val results = mutableListOf<TextLayoutResult>()
            text.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            val layout = results.firstOrNull()
            assertTrue("$label has no text layout", layout != null)
            assertTrue("$label overflows its height or runs past 2 lines", !layout!!.didOverflowHeight && layout.lineCount <= 2)
            val box = button.getUnclippedBoundsInRoot()
            val inner = text.getUnclippedBoundsInRoot()
            assertTrue("$label sticks out of its button", inner.left >= box.left && inner.right <= box.right && inner.top >= box.top && inner.bottom <= box.bottom)
        }
    }
}
