package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.isOngoingMedication
import com.sherifradwan.ancnative.model.medicationStatusLabel
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.DeleteIconButton
import com.sherifradwan.ancnative.ui.FactRow
import com.sherifradwan.ancnative.ui.MedicationStatusBadge
import com.sherifradwan.ancnative.ui.medicationDoseRouteFrequency
import com.sherifradwan.ancnative.ui.medicationFrequencyLabel
import com.sherifradwan.ancnative.ui.minimumTouchTarget

/**
 * One medication row, the only one. Closed, it shows the name, one "dose · route · frequency" line and, when there are
 * notes, their first line; the whole note and the other facts show when the row is opened. Active rows carry the one-tap Stop control; stopped rows do not.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationCard(
    med: DemoMedicationRecord,
    isExpanded: Boolean,
    canEdit: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    /** False while a save runs or a medication draft is open: stop, edit and delete show disabled. */
    actionsEnabled: Boolean = true,
    onStop: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val status = medicationStatusLabel(med.status, med.isNonCompliant)
    val frequency = medicationFrequencyLabel(med.frequency)
    val details = medicationDoseRouteFrequency(med.dosage, med.route, frequency)
    val notes = med.notes?.trim().orEmpty()
    val toggleLabel = stringResource(if (isExpanded) R.string.hide_details else R.string.show_details)
    val toggleState = stringResource(if (isExpanded) R.string.state_expanded else R.string.state_collapsed)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("medication-${med.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(bottom = if (isExpanded) 12.dp else 0.dp)) {
            // The text takes the full card width; the status and actions share the line below it, so the buttons
            // never narrow the name or the dose, route and frequency at 360 dp.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumTouchTarget()
                    .testTag("medicationToggle-${med.id}")
                    .semantics { stateDescription = toggleState }
                    .clickable(role = Role.Button, onClickLabel = toggleLabel, onClick = onToggle)
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp).size(20.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        med.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("medicationName-${med.id}"),
                    )
                    if (details.isNotBlank()) {
                        Text(
                            text = details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("medicationInstructions-${med.id}"),
                        )
                    }
                    // One line of notes while closed (a warning such as "hold if SBP < 100" stays in the review list);
                    // the whole note shows when the row is opened.
                    if (notes.isNotEmpty() && !isExpanded) {
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("medicationNotes-${med.id}"),
                        )
                    }
                }
            }

            // Status on the start side, actions on the end side; when both do not fit (large text) the actions wrap.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 4.dp, bottom = if (canEdit) 0.dp else 8.dp)
                    .testTag("medicationActions-${med.id}"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MedicationStatusBadge(status, modifier = Modifier.padding(vertical = if (canEdit) 0.dp else 4.dp))
                if (canEdit) {
                    // The weight lets the actions take the rest of the line, or a whole line when they wrap, and sit at its end.
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isOngoingMedication(med.status, med.isNonCompliant) && onStop != null) {
                            StopControl(name = med.name, id = med.id, enabled = actionsEnabled, onStop = onStop)
                        }
                        if (onDelete != null) {
                            DeleteIconButton(DeletableType.MEDICATION, med, onDelete, enabled = actionsEnabled)
                        }
                        if (onEdit != null) {
                            IconButton(
                                onClick = onEdit,
                                enabled = actionsEnabled,
                                modifier = Modifier.minimumTouchTarget().testTag("editMedication-${med.id}"),
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit_medication_description, med.name),
                                    tint = if (actionsEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (isExpanded) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    FactRow(R.string.dosage, med.dosage)
                    FactRow(R.string.route, med.route)
                    FactRow(R.string.frequency, frequency)
                    FactRow(R.string.start_date, med.startDate)
                    if (!med.adverseNotes.isNullOrBlank()) {
                        FactRow(R.string.adverse_effects, med.adverseNotes)
                    }
                    FactRow(R.string.notes, med.notes)
                }
            }
        }
    }
}

/**
 * The one-tap stop: the stop icon over a visible "Stop" label, in the error colour (at least 4.5:1 on the card in
 * both themes), at least 48 dp. Screen readers hear "Stop <drug>".
 */
@Composable
private fun StopControl(name: String, id: String, enabled: Boolean, onStop: () -> Unit) {
    val description = stringResource(R.string.stop_medication_description, name)
    val color = if (enabled) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = MEDICATION_DISABLED_ALPHA)
    }
    Column(
        modifier = Modifier
            .minimumTouchTarget()
            .testTag("stopMedication-$id")
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, role = Role.Button, onClick = onStop)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Stop, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(
            text = stringResource(R.string.stop_medication_short),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            // Seen, not read: the control's own description already says "Stop <drug>".
            modifier = Modifier.testTag("stopMedicationLabel-$id").semantics { hideFromAccessibility() },
        )
    }
}

/** Opacity of a disabled medication control or tile, shared by the card and the picker. */
internal const val MEDICATION_DISABLED_ALPHA = 0.38f
