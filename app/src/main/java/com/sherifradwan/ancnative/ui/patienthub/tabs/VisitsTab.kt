package com.sherifradwan.ancnative.ui.patienthub.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.parseVisitFindings
import com.sherifradwan.ancnative.model.parseWeightKg
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.BpAlertLabel
import com.sherifradwan.ancnative.ui.DeleteIconButton
import com.sherifradwan.ancnative.ui.GestationalAgeBadge
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.ui.patienthub.components.BedsideVitalsGrid
import com.sherifradwan.ancnative.ui.patienthub.model.deriveVitalsSnapshot

@Composable
fun VisitsTab(
    patient: DemoPatient,
    canEdit: Boolean,
    onStartVisit: (String, String?) -> Unit,
    onEvent: (AncEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookingWeightKg = remember(patient.visits) {
        patient.visits
            .mapNotNull { parseWeightKg(it.weight) }
            .lastOrNull()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("visitsTabContent"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top Action: + Record New Antenatal Visit
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Antenatal Visits (${patient.visits.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (canEdit && patient.status != "archived") {
                    Button(
                        onClick = { onStartVisit(patient.id, null) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.minimumTouchTarget().testTag("recordNewVisitBtn"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Record Visit", fontWeight = FontWeight.Bold, softWrap = false)
                    }
                }
            }
        }

        if (patient.visits.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No records",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            items(
                items = patient.visits,
                key = { it.id },
            ) { visit ->
                val gaAtVisit = remember(patient.dating, visit.date) {
                    GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, visit.date)
                }
                val gaDisplayText = gaAtVisit?.displayText
                val gaWeeks = gaAtVisit?.weeks
                val parsedFindings = remember(visit.findings) { parseVisitFindings(visit.findings) }

                val vitals = remember(visit.bloodPressure, visit.weight, visit.findings, gaWeeks, bookingWeightKg) {
                    deriveVitalsSnapshot(
                        bloodPressure = visit.bloodPressure,
                        weight = visit.weight,
                        findings = visit.findings,
                        gaWeeks = gaWeeks,
                        bookingWeightKg = bookingWeightKg,
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("visit-${visit.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Header Row: Date + GA + Badges (FlowRow to prevent 360dp collisions)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = visit.date,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (!gaDisplayText.isNullOrBlank()) {
                                    GestationalAgeBadge(gaDisplayText)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                BpAlertLabel(visit.bloodPressure)

                                if (visit.linkedUltrasoundId != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                                    ) {
                                        Text(
                                            text = "Scan Linked",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }

                        // Pre-eclampsia Screening Alert
                        if (vitals.isPreEclampsiaWarning) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().testTag("preEclampsiaWarning-${visit.id}"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Column {
                                        Text(
                                            text = "Pre-eclampsia Risk Alert",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                        Text(
                                            text = "Elevated BP (${visit.bloodPressure}) with proteinuria (${parsedFindings.urineProtein}). Clinical evaluation required.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                }
                            }
                        }

                        // Canonical Bedside Vitals Grid
                        BedsideVitalsGrid(vitals = vitals)

                        // Additional clinical notes
                        if (!visit.notes.isNullOrBlank()) {
                            Text(
                                text = visit.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        // Actions: Edit and Delete
                        if (canEdit) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                DeleteIconButton(
                                    type = DeletableType.VISIT,
                                    record = visit,
                                    onClick = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.VISIT, visit.id)) },
                                )
                                TextButton(
                                    onClick = { onStartVisit(patient.id, visit.id) },
                                    modifier = Modifier.minimumTouchTarget().testTag("editVisit-${visit.id}"),
                                ) {
                                    Text(stringResource(R.string.edit_visit))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
