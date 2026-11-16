package com.sherifradwan.ancnative.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DeleteMessage
import com.sherifradwan.ancnative.state.DeleteMessageKind
import com.sherifradwan.ancnative.state.stringRes
import com.sherifradwan.ancnative.state.UndoAction
import com.sherifradwan.ancnative.state.UndoOffer
import com.sherifradwan.ancnative.state.UndoPhase
import com.sherifradwan.ancnative.storage.DeletableRecord
import com.sherifradwan.ancnative.storage.DeletableRegistry
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType

/** The one place that names a deletable kind to the user; every delete/Undo string takes this as its argument. */
@StringRes
internal fun DeletableType.labelResource(): Int = when (this) {
    DeletableType.MEDICATION -> R.string.deletable_type_medication
    DeletableType.LAB -> R.string.deletable_type_lab
    DeletableType.ULTRASOUND -> R.string.deletable_type_ultrasound
    DeletableType.VISIT -> R.string.deletable_type_visit
    DeletableType.PREVIOUS_PREGNANCY -> R.string.deletable_type_previous_pregnancy
    DeletableType.ATTACHMENT -> R.string.deletable_type_attachment
}

/** " (patient Name)", or nothing when the name is blank: the UI never shows "(patient )". */
@Composable
internal fun patientClause(patientName: String): String =
    if (patientName.isBlank()) "" else " " + stringResource(R.string.patient_clause, patientName.trim())

/**
 * Global delete affordance: a trash icon with a 48dp touch target, tagged `delete-<type>-<id>`.
 * Its spoken name comes from the registry label, the same one the confirm dialog and Undo bar use.
 */
@Composable
internal fun DeleteIconButton(
    type: DeletableType,
    record: DeletableRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    enabled: Boolean = true,
    /** Dimmed look; differs from "not enabled" only while a sheet covers the screen. */
    dimmed: Boolean = !enabled,
) {
    val recordId = record.id
    // A type/record mismatch draws nothing instead of throwing while the screen is composed.
    val label = DeletableRegistry.labelOrNull(type, record) ?: return
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .minimumTouchTarget()
            .testTag(tag ?: "delete-${type.name.lowercase()}-$recordId"),
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = stringResource(R.string.delete_record_description, stringResource(type.labelResource()), label),
            tint = if (!dimmed) MaterialTheme.colorScheme.error else LocalContentColor.current,
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
    // A record that is gone cannot be confirmed: never render a dialog with an empty name.
    if (record == null) {
        LaunchedEffect(pending) { onEvent(AncEvent.CancelDelete) }
        return
    }
    val label = DeletableRegistry.of<DeletableRecord>(pending.type).label(record)
    val typeLabel = stringResource(pending.type.labelResource())
    AlertDialog(
        modifier = Modifier.testTag("deleteConfirmDialog"),
        onDismissRequest = { onEvent(AncEvent.CancelDelete) },
        shape = RecordShapes.Large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                stringResource(R.string.delete_confirm_title, typeLabel),
                style = RecordType.SectionTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                stringResource(R.string.delete_confirm_body, label),
                style = RecordType.FieldValue,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(AncEvent.ConfirmDelete) },
                shape = RecordShapes.Small,
                modifier = Modifier.minimumTouchTarget().testTag("deleteConfirm"),
            ) {
                Text(
                    stringResource(R.string.delete),
                    style = RecordType.Action,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(AncEvent.CancelDelete) },
                shape = RecordShapes.Small,
                modifier = Modifier.minimumTouchTarget().testTag("deleteCancel"),
            ) {
                Text(
                    stringResource(R.string.cancel),
                    style = RecordType.Action,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

/** App-level Undo bar and failure message, placed once below the screen content so they never cover a form button. */
@Composable
fun UndoBarHost(state: AncState, onEvent: (AncEvent) -> Unit) {
    val message = state.deleteMessage
    val offer = state.undoOffer
    if (message == null && offer == null) return
    Column {
        if (message != null) DeleteMessageBar(message, onEvent)
        if (offer != null) UndoOfferBar(offer, state.deleteRunning, onEvent)
    }
}

@Composable
private fun DeleteMessageBar(message: DeleteMessage, onEvent: (AncEvent) -> Unit) {
    val typeLabel = stringResource(message.type.labelResource())
    val reason = stringResource(message.error.stringRes())
    val patient = patientClause(message.patientName)
    val text = when (message.kind) {
        DeleteMessageKind.DELETE_FAILED -> stringResource(R.string.delete_failed_message, typeLabel, message.label, reason, patient)
        DeleteMessageKind.UNDO_FAILED -> if (message.action == UndoAction.SAVE) {
            stringResource(R.string.undo_failed_saved_message, typeLabel, message.label, reason, patient)
        } else {
            stringResource(R.string.undo_failed_deleted_message, typeLabel, message.label, reason, patient)
        }
        DeleteMessageKind.UNDO_BLOCKED -> stringResource(R.string.undo_blocked_message, typeLabel, message.label)
        DeleteMessageKind.CLEANUP_FAILED -> stringResource(R.string.cleanup_failed_message, typeLabel, message.label, patient)
    }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RecordShapes.Small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("deleteMessage")
            .semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = text,
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { onEvent(AncEvent.DismissDeleteMessage) },
                shape = RecordShapes.Small,
                modifier = Modifier.minimumTouchTarget().testTag("deleteMessageDismiss"),
            ) {
                Text(
                    stringResource(R.string.close),
                    style = RecordType.Action,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun UndoOfferBar(offer: UndoOffer, deleteRunning: Boolean, onEvent: (AncEvent) -> Unit) {
    val running = offer.phase == UndoPhase.RUNNING
    // One short line: "Deleted <record> - <patient>". The record name is cut first, so the patient stays visible.
    val patientName = offer.patientName.trim()
    val head = when (offer.action) {
        UndoAction.DELETE -> stringResource(R.string.undo_message_deleted_no_patient, offer.label)
        UndoAction.SAVE -> stringResource(R.string.undo_message_saved_no_patient, offer.label)
    }
    val spoken = when (offer.action) {
        UndoAction.DELETE -> if (patientName.isEmpty()) head else stringResource(R.string.undo_message_deleted, offer.label, patientName)
        UndoAction.SAVE -> if (patientName.isEmpty()) head else stringResource(R.string.undo_message_saved, offer.label, patientName)
    }
    val tail = if (patientName.isEmpty()) "" else stringResource(R.string.undo_message_patient_tail, patientName)
    val progressDescription = stringResource(R.string.undo_in_progress)
    val patientBg = FieldFamilies.patient().container
    val wrapLines = LocalDensity.current.fontScale > 1.3f
    Surface(
        color = patientBg,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RecordShapes.Small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("undoBar")
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (wrapLines) {
                Text(
                    text = spoken,
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    modifier = Modifier.weight(1f).semantics { contentDescription = spoken },
                )
            } else {
                Row(
                    modifier = Modifier.weight(1f).semantics { contentDescription = spoken },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = head,
                        style = RecordType.Secondary,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (tail.isNotEmpty()) {
                        Text(
                            text = tail,
                            style = RecordType.Secondary,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 130.dp),
                        )
                    }
                }
            }
            if (running) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp).semantics { contentDescription = progressDescription },
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(
                onClick = { onEvent(AncEvent.UndoRequested(offer.token)) },
                enabled = !running && !deleteRunning,
                shape = RecordShapes.Small,
                modifier = Modifier.minimumTouchTarget().testTag("undoButton"),
            ) {
                Text(stringResource(R.string.undo), style = RecordType.Action)
            }
        }
    }
}
