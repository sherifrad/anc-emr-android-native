package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.SaveUiError
import com.sherifradwan.ancnative.state.stringRes
import com.sherifradwan.ancnative.ui.minimumTouchTarget

@Composable
internal fun SheetSaveMessage(
    savePhase: SavePhase,
    saveError: SaveUiError?,
    fieldErrorMessage: String?,
    failedTitle: String,
    tag: String,
) {
    val message = when {
        savePhase == SavePhase.RECONCILING -> stringResource(R.string.save_reconciling)
        savePhase == SavePhase.UNRESOLVED -> stringResource(R.string.save_unresolved)
        savePhase == SavePhase.ERROR -> stringResource(saveError.stringRes())
        fieldErrorMessage != null -> fieldErrorMessage
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
            .testTag(tag),
    ) {
        if (isFailure) {
            Text(
                text = failedTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isFailure || fieldErrorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Cancel and Save in one row; the caller adds any extra rows below it. Labels wrap rather than clip. */
@Composable
internal fun SheetCancelSaveRow(
    enabled: Boolean,
    saving: Boolean,
    saveEnabled: Boolean,
    savingLabel: String,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    cancelTag: String,
    saveTag: String,
    spinnerTag: String,
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
            modifier = Modifier.weight(1f).minimumTouchTarget().testTag(cancelTag),
        ) {
            Text(stringResource(R.string.cancel), maxLines = 2, textAlign = TextAlign.Center)
        }
        Button(
            onClick = onSave,
            enabled = enabled && saveEnabled,
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier.weight(1f).minimumTouchTarget().testTag(saveTag),
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp).testTag(spinnerTag),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
            }
            Text(
                savingLabel,
                maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = if (saving) 8.dp else 0.dp),
            )
        }
    }
}

/** The discard dialog of an editor bottom sheet: Discard runs [onDiscard], Keep and outside taps run [onKeep]. */
@Composable
internal fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onKeep: () -> Unit,
    dialogTag: String,
    confirmTag: String,
    keepTag: String,
) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(stringResource(R.string.discard_changes)) },
        text = { Text(stringResource(R.string.discard_body)) },
        confirmButton = {
            TextButton(
                onClick = onDiscard,
                modifier = Modifier.minimumTouchTarget().testTag(confirmTag),
            ) { Text(stringResource(R.string.discard)) }
        },
        dismissButton = {
            TextButton(
                onClick = onKeep,
                modifier = Modifier.minimumTouchTarget().testTag(keepTag),
            ) { Text(stringResource(R.string.keep_editing)) }
        },
        modifier = Modifier.testTag(dialogTag),
    )
}

/**
 * Sheet state for an editor bottom sheet: drag-down and back close at once when [dismissAllowed]
 * holds, ask first through [onNeedsConfirm] when [needsConfirm] holds, and are blocked otherwise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberGuardedSheetState(
    dismissAllowed: Boolean,
    needsConfirm: Boolean,
    onNeedsConfirm: () -> Unit,
): SheetState {
    val allowed by rememberUpdatedState(dismissAllowed)
    val confirmNeeded by rememberUpdatedState(needsConfirm)
    return rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            when {
                target != SheetValue.Hidden -> true
                !allowed -> false
                confirmNeeded -> {
                    onNeedsConfirm()
                    false
                }
                else -> true
            }
        },
    )
}
