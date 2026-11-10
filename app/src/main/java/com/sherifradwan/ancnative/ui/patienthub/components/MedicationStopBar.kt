package com.sherifradwan.ancnative.ui.patienthub.components

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.MEDICATION_STOP_UNDO_MILLIS
import com.sherifradwan.ancnative.state.MedicationQuickSave
import com.sherifradwan.ancnative.state.MedicationStopUndo
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.SaveUiError
import com.sherifradwan.ancnative.state.remainingMillis
import com.sherifradwan.ancnative.state.stringRes
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Bottom of the Meds tab for a one-tap stop and its Undo: a progress line while the save runs, the existing save-error
 * message when it fails, and after a stop commits the Undo bar until [undoWindowMillis] after the commit.
 *
 * The bar counts from [MedicationStopUndo.committedAtMillis] on [clock] (the same monotonic clock the view model
 * stamps the commit with), so recomposition, rotation or leaving the tab never gives it a fresh window.
 */
@Composable
fun MedicationStopBar(
    quickSave: MedicationQuickSave?,
    stopUndo: MedicationStopUndo?,
    savePhase: SavePhase,
    saveError: SaveUiError?,
    undoEnabled: Boolean,
    onEvent: (AncEvent) -> Unit,
    undoWindowMillis: Long = MEDICATION_STOP_UNDO_MILLIS,
    clock: () -> Long = SystemClock::elapsedRealtime,
) {
    when {
        quickSave != null && (savePhase == SavePhase.PENDING || savePhase == SavePhase.RECONCILING) -> {
            val saving = stringResource(R.string.medication_saving)
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medStopProgress")
                    .semantics {
                        contentDescription = saving
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        }
        quickSave != null && savePhase == SavePhase.UNRESOLVED -> StopFailure(SaveUiError.OUTCOME_UNRESOLVED, onDismiss = null)
        quickSave != null && savePhase == SavePhase.ERROR ->
            StopFailure(saveError, onDismiss = { onEvent(AncEvent.DismissMedicationStopFailure) })
        stopUndo != null -> StopUndo(stopUndo, undoEnabled, onEvent, undoWindowMillis, clock)
    }
}

/** [onDismiss] is null for an unresolved outcome: like every other editor, nothing may move on until it is resolved. */
@Composable
private fun StopFailure(error: SaveUiError?, onDismiss: (() -> Unit)?) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("medStopError")
            .semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.medication_save_failed_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(error.stringRes()), style = MaterialTheme.typography.bodyMedium)
            }
            if (onDismiss != null) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumTouchTarget().testTag("medStopErrorDismiss"),
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun StopUndo(
    undo: MedicationStopUndo,
    enabled: Boolean,
    onEvent: (AncEvent) -> Unit,
    windowMillis: Long,
    clock: () -> Long,
) {
    val send by rememberUpdatedState(onEvent)
    LaunchedEffect(undo.token, undo.committedAtMillis, windowMillis) {
        // Only what is left of the window; a bar shown again after its window ended goes at once.
        delay(undo.remainingMillis(clock(), windowMillis))
        send(AncEvent.MedicationStopUndoExpired(undo.token))
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("medStopUndoBar")
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.medication_stopped_undo_message, undo.medicationName),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { onEvent(AncEvent.UndoMedicationStop(undo.token, UUID.randomUUID().toString())) },
                enabled = enabled,
                modifier = Modifier.minimumTouchTarget().testTag("medStopUndo"),
            ) {
                Text(stringResource(R.string.undo))
            }
        }
    }
}
