package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.isOngoingMedication
import com.sherifradwan.ancnative.model.medicationInstructions
import com.sherifradwan.ancnative.model.medicationStatusLabel
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.DeleteIconButton
import com.sherifradwan.ancnative.ui.FactRow
import com.sherifradwan.ancnative.ui.MedicationStatusBadge
import com.sherifradwan.ancnative.ui.minimumTouchTarget

@Composable
fun MedicationCard(
    med: DemoMedicationRecord,
    isExpanded: Boolean,
    canEdit: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onStop: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val status = medicationStatusLabel(med.status, med.isNonCompliant)
    val instructions = medicationInstructions(med.dosage, med.route, med.notes)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("medication-${med.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(bottom = if (isExpanded) 12.dp else 0.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .minimumTouchTarget()
                        .testTag("medicationToggle-${med.id}")
                        .clickable(onClick = onToggle)
                        .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(if (isExpanded) R.string.hide_details else R.string.show_details),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(med.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        MedicationStatusBadge(status)
                        if (instructions.isNotBlank()) {
                            Text(
                                text = instructions,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("medicationInstructions-${med.id}"),
                            )
                        }
                    }
                }

                if (canEdit) {
                    if (isOngoingMedication(med.status, med.isNonCompliant) && onStop != null) {
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier.minimumTouchTarget().testTag("stopMedication-${med.id}"),
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = stringResource(R.string.stop_medication),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    if (onDelete != null) {
                        DeleteIconButton(DeletableType.MEDICATION, med, onDelete)
                    }
                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.minimumTouchTarget().testTag("editMedication-${med.id}"),
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_medication),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.width(12.dp))
                }
            }

            if (isExpanded) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    FactRow(R.string.dosage, med.dosage)
                    FactRow(R.string.route, med.route)
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
