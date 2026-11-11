package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.MedicationDraft
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.SaveUiError
import com.sherifradwan.ancnative.state.stringRes
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
    val dismissAllowed by rememberUpdatedState(enabled)
    val needsConfirm by rememberUpdatedState(touched)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            when {
                target != SheetValue.Hidden -> true
                !dismissAllowed -> false
                needsConfirm -> {
                    confirmDiscard = true
                    false
                }
                else -> true
            }
        },
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
                SaveMessage(savePhase = savePhase, saveError = saveError, hasFieldErrors = draft.errors.isNotEmpty())
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
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_changes)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard = false
                        onEvent(AncEvent.CancelEditor)
                    },
                    modifier = Modifier.minimumTouchTarget().testTag("medDiscardConfirm"),
                ) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDiscard = false },
                    modifier = Modifier.minimumTouchTarget().testTag("medDiscardKeep"),
                ) { Text(stringResource(R.string.keep_editing)) }
            },
            modifier = Modifier.testTag("medDiscardDialog"),
        )
    }
}

@Composable
private fun SaveMessage(savePhase: SavePhase, saveError: SaveUiError?, hasFieldErrors: Boolean) {
    val message = when {
        savePhase == SavePhase.RECONCILING -> stringResource(R.string.save_reconciling)
        savePhase == SavePhase.UNRESOLVED -> stringResource(R.string.save_unresolved)
        savePhase == SavePhase.ERROR -> stringResource(saveError.stringRes())
        hasFieldErrors -> stringResource(R.string.correct_highlighted_fields)
        else -> return
    }
    val isFailure = savePhase == SavePhase.ERROR || savePhase == SavePhase.UNRESOLVED
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            // A failed save is shown at the top of the body; bring it back if the doctor had scrolled down.
            .revealWhen(isFailure)
            .testTag("medSaveMessage"),
    ) {
        if (isFailure) {
            Text(
                text = stringResource(R.string.medication_save_failed_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isFailure || hasFieldErrors) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).minimumTouchTarget().testTag("cancelMedication"),
            ) {
                Text(stringResource(R.string.cancel), maxLines = 2, textAlign = TextAlign.Center)
            }
            Button(
                onClick = onSave,
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).minimumTouchTarget().testTag("saveMedication"),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp).testTag("medSaveSpinner"),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                }
                Text(
                    stringResource(if (saving) R.string.medication_saving else R.string.save),
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = if (saving) 8.dp else 0.dp),
                )
            }
        }
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
