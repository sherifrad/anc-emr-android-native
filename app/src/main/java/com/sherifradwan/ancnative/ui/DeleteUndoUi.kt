package com.sherifradwan.ancnative.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.UndoAction
import com.sherifradwan.ancnative.state.UndoPhase
import com.sherifradwan.ancnative.storage.DeletableRecord
import com.sherifradwan.ancnative.storage.DeletableRegistry
import com.sherifradwan.ancnative.storage.DeletableType

/** The one place that names a deletable kind to the user; every delete/Undo string takes this as its argument. */
@StringRes
internal fun DeletableType.labelResource(): Int = when (this) {
    DeletableType.MEDICATION -> R.string.deletable_type_medication
    DeletableType.LAB -> R.string.deletable_type_lab
}

/** Global delete affordance: a trash icon with a 48dp touch target, tagged `delete-<type>-<id>`. */
@Composable
internal fun DeleteIconButton(
    type: DeletableType,
    recordId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .minimumTouchTarget()
            .testTag("delete-${type.name.lowercase()}-$recordId"),
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = stringResource(R.string.delete_record_description, stringResource(type.labelResource())),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Global confirmation, driven only by `state.deleteConfirmation`. */
@Composable
fun DeleteConfirmDialog(state: AncState, onEvent: (AncEvent) -> Unit) {
    val pending = state.deleteConfirmation ?: return
    val record = state.patient(pending.patientId)
        ?.let { DeletableRegistry.of<DeletableRecord>(pending.type).record(it, pending.recordId) }
    val label = record?.let { DeletableRegistry.of<DeletableRecord>(pending.type).label(it) }.orEmpty()
    val typeLabel = stringResource(pending.type.labelResource())
    AlertDialog(
        modifier = Modifier.testTag("deleteConfirmDialog"),
        onDismissRequest = { onEvent(AncEvent.CancelDelete) },
        title = { Text(stringResource(R.string.delete_confirm_title, typeLabel)) },
        text = { Text(stringResource(R.string.delete_confirm_body, label)) },
        confirmButton = {
            Button(
                onClick = { onEvent(AncEvent.ConfirmDelete) },
                modifier = Modifier.minimumTouchTarget().testTag("deleteConfirm"),
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(AncEvent.CancelDelete) },
                modifier = Modifier.minimumTouchTarget().testTag("deleteCancel"),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/** App-level Undo bar, placed once below the screen content so it never covers a form button. */
@Composable
fun UndoBarHost(state: AncState, onEvent: (AncEvent) -> Unit) {
    val offer = state.undoOffer ?: return
    val running = offer.phase == UndoPhase.RUNNING
    val typeLabel = stringResource(offer.type.labelResource())
    val message = when (offer.action) {
        UndoAction.DELETE -> stringResource(R.string.undo_message_deleted, typeLabel, offer.label)
        UndoAction.SAVE -> stringResource(R.string.undo_message_saved, typeLabel, offer.label)
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
            .testTag("undoBar")
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (running) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            TextButton(
                onClick = { onEvent(AncEvent.UndoRequested(offer.token)) },
                enabled = !running,
                modifier = Modifier.minimumTouchTarget().testTag("undoButton"),
            ) {
                Text(stringResource(R.string.undo))
            }
        }
    }
}
