package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.MedicationDraft
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.SaveUiError
import com.sherifradwan.ancnative.ui.minimumTouchTarget

/**
 * Bottom sheet that hosts [MedicationForm] for one [MedicationDraft].
 *
 * Add (and custom) mode is a draft with no stored revision; it also offers "Save and add another".
 * Cancel and dismiss send [AncEvent.CancelEditor] at once when the form is untouched, and ask first
 * when [touched] is true. While a save runs the form, Cancel, Save and Save and add another are disabled; a rejected save
 * leaves the sheet open with the doctor's values and shows the [saveError] message, scrolled into view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationSheet(
    draft: MedicationDraft,
    savePhase: SavePhase,
    saveError: SaveUiError?,
    touched: Boolean,
    onEvent: (AncEvent) -> Unit,
    /** Shown under the buttons, inside the sheet: the host puts the app's Undo bar here so the sheet never hides it. */
    footer: @Composable () -> Unit = {},
) {
    val isExistingRow = draft.baseMedicationRevision != null
    val enabled = savePhase == SavePhase.IDLE || savePhase == SavePhase.ERROR
    val saving = savePhase == SavePhase.PENDING || savePhase == SavePhase.RECONCILING
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val draftKey = "${draft.editorSessionId}:${draft.medicationId}"
    // The draft whose name already took focus. Saved here, outside the sheet's own window, so a rotation or theme
    // change later in the same draft does not pull focus back to the name.
    var nameFocusedFor by rememberSaveable { mutableStateOf<String?>(null) }
    // Goes up on every Save press, so a repeated rejected save scrolls to the first error again.
    var saveAttempts by rememberSaveable { mutableStateOf(0) }

    val requestCancel = {
        if (touched) confirmDiscard = true else onEvent(AncEvent.CancelEditor)
    }
    val sheetState = rememberGuardedSheetState(
        dismissAllowed = enabled,
        needsConfirm = touched,
        onNeedsConfirm = { confirmDiscard = true },
    )

    ModalBottomSheet(
        onDismissRequest = { if (enabled) requestCancel() },
        sheetState = sheetState,
        modifier = Modifier.testTag("medicationSheet"),
    ) {
        Column(modifier = Modifier.fillMaxWidth().imePadding()) {
            if (saving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag("medSaveProgress"))
            }
            Text(
                text = stringResource(if (isExistingRow) R.string.edit_medication else R.string.add_medication),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { heading() },
            )
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    // Bottom room so the last row (route or notes) is fully visible when scrolled to the end.
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SheetSaveMessage(
                    savePhase = savePhase,
                    saveError = saveError,
                    fieldErrorMessage = if (draft.errors.isNotEmpty()) stringResource(R.string.correct_highlighted_fields) else null,
                    failedTitle = stringResource(R.string.medication_save_failed_title),
                    tag = "medSaveMessage",
                )
                MedicationForm(
                    values = draft.values,
                    errors = draft.errors,
                    isExistingRow = isExistingRow,
                    enabled = enabled,
                    draftKey = draftKey,
                    // The next drug of "Save and add another": its name takes focus, so the keyboard stays up,
                    // whether this sheet stayed on screen between the two drafts or was shown again.
                    focusNameOnStart = draft.chainedAdd && nameFocusedFor != draftKey,
                    onNameFocused = { nameFocusedFor = draftKey },
                    revealToken = saveAttempts,
                    onEvent = onEvent,
                )
            }
            MedicationSheetButtons(
                isExistingRow = isExistingRow,
                enabled = enabled,
                saving = saving,
                onCancel = requestCancel,
                onSave = {
                    saveAttempts++
                    onEvent(AncEvent.SubmitMedication)
                },
                onSaveAndAddAnother = {
                    saveAttempts++
                    onEvent(AncEvent.SubmitMedicationAndAddAnother)
                },
            )
            footer()
        }
    }

    if (confirmDiscard) {
        DiscardChangesDialog(
            onDiscard = {
                confirmDiscard = false
                onEvent(AncEvent.CancelEditor)
            },
            onKeep = { confirmDiscard = false },
            dialogTag = "medDiscardDialog",
            confirmTag = "medDiscardConfirm",
            keepTag = "medDiscardKeep",
        )
    }
}

/** Cancel and Save share one row; "Save and add another" (new rows only) gets its own full-width row. Labels wrap rather than clip. */
@Composable
internal fun MedicationSheetButtons(
    isExistingRow: Boolean,
    enabled: Boolean,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onSaveAndAddAnother: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SheetCancelSaveRow(
            enabled = enabled,
            saving = saving,
            saveEnabled = true,
            savingLabel = stringResource(if (saving) R.string.medication_saving else R.string.save),
            onCancel = onCancel,
            onSave = onSave,
            cancelTag = "cancelMedication",
            saveTag = "saveMedication",
            spinnerTag = "medSaveSpinner",
        )
        if (!isExistingRow) {
            OutlinedButton(
                onClick = onSaveAndAddAnother,
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("addAnotherMedication"),
            ) {
                Text(
                    stringResource(R.string.save_and_add_another_med),
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
