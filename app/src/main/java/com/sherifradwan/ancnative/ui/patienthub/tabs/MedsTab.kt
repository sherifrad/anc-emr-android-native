package com.sherifradwan.ancnative.ui.patienthub.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.isOngoingMedication
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.MedicationDraft
import com.sherifradwan.ancnative.state.MedicationQuickSave
import com.sherifradwan.ancnative.state.MedicationStopUndo
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.SaveUiError
import com.sherifradwan.ancnative.state.blocksEditing
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.ui.patienthub.components.MedicationCard
import com.sherifradwan.ancnative.ui.patienthub.components.MedicationTapToAddSection
import com.sherifradwan.ancnative.ui.patienthub.components.MedicationStopBar
import java.util.UUID

/**
 * The Meds tab. Adding, editing and stopping never leave it: tiles, edit and "Prescribe" open the medication sheet
 * (hosted by the hub) through one [AncEvent.StartMedicationEditor], and the stop symbol saves at once through
 * [AncEvent.StopMedication]. [canEdit] shows the controls (patient not archived); they are visibly disabled while
 * [actionsAllowed] is false, a save runs or [medicationDraft] is open.
 */
@Composable
fun MedsTab(
    patient: DemoPatient,
    canEdit: Boolean,
    actionsAllowed: Boolean,
    medicationDraft: MedicationDraft?,
    savePhase: SavePhase,
    saveError: SaveUiError?,
    quickSave: MedicationQuickSave?,
    stopUndo: MedicationStopUndo?,
    onEvent: (AncEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entryEnabled = canEdit && actionsAllowed && medicationDraft == null && !savePhase.blocksEditing()
    Column(modifier = modifier.fillMaxSize()) {
        MedsList(
            patient = patient,
            canEdit = canEdit,
            entryEnabled = entryEnabled,
            onEvent = onEvent,
            modifier = Modifier.weight(1f),
        )
        MedicationStopBar(
            quickSave = quickSave?.takeIf { it.draft.patientId == patient.id },
            stopUndo = stopUndo?.takeIf { it.patientId == patient.id },
            savePhase = savePhase,
            saveError = saveError,
            undoEnabled = entryEnabled,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun MedsList(
    patient: DemoPatient,
    canEdit: Boolean,
    entryEnabled: Boolean,
    onEvent: (AncEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeMeds = remember(patient.medications) {
        patient.medications.filter { isOngoingMedication(it.status, it.isNonCompliant) }
    }
    val pastMeds = remember(patient.medications) {
        patient.medications.filter { !isOngoingMedication(it.status, it.isNonCompliant) }
    }

    var expandedMedId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("medsTabContent"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Fast Formulary Tap-to-Add Section
        if (canEdit) {
            item {
                MedicationTapToAddSection(
                    patient = patient,
                    enabled = entryEnabled,
                    onEvent = onEvent,
                )
            }
        }

        // Active Prescriptions Header & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Active Prescriptions (${activeMeds.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (canEdit) {
                    Button(
                        enabled = entryEnabled,
                        onClick = {
                            onEvent(
                                AncEvent.StartMedicationEditor(
                                    patientId = patient.id,
                                    medicationId = null,
                                    newMedicationId = UUID.randomUUID().toString(),
                                    editorSessionId = UUID.randomUUID().toString(),
                                ),
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.minimumTouchTarget().testTag("addCustomMedicationBtn"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Prescribe", fontWeight = FontWeight.Bold, softWrap = false)
                    }
                }
            }
        }

        if (activeMeds.isEmpty()) {
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
                items = activeMeds,
                key = { it.id },
            ) { med ->
                MedicationCard(
                    med = med,
                    isExpanded = expandedMedId == med.id,
                    canEdit = canEdit,
                    actionsEnabled = entryEnabled,
                    onToggle = {
                        expandedMedId = if (expandedMedId == med.id) null else med.id
                    },
                    onStop = {
                        onEvent(AncEvent.StopMedication(patient.id, med.id, UUID.randomUUID().toString()))
                    },
                    onEdit = {
                        onEvent(
                            AncEvent.StartMedicationEditor(
                                patientId = patient.id,
                                medicationId = med.id,
                                newMedicationId = med.id,
                                editorSessionId = UUID.randomUUID().toString(),
                            ),
                        )
                    },
                    onDelete = {
                        onEvent(AncEvent.RequestDelete(patient.id, DeletableType.MEDICATION, med.id))
                    },
                )
            }
        }

        // Past / Discontinued Prescriptions
        if (pastMeds.isNotEmpty()) {
            item {
                Text(
                    text = "Discontinued / Past Prescriptions (${pastMeds.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            items(
                items = pastMeds,
                key = { it.id },
            ) { med ->
                MedicationCard(
                    med = med,
                    isExpanded = expandedMedId == med.id,
                    canEdit = canEdit,
                    actionsEnabled = entryEnabled,
                    onToggle = {
                        expandedMedId = if (expandedMedId == med.id) null else med.id
                    },
                    onEdit = {
                        onEvent(
                            AncEvent.StartMedicationEditor(
                                patientId = patient.id,
                                medicationId = med.id,
                                newMedicationId = med.id,
                                editorSessionId = UUID.randomUUID().toString(),
                            ),
                        )
                    },
                    onDelete = {
                        onEvent(AncEvent.RequestDelete(patient.id, DeletableType.MEDICATION, med.id))
                    },
                )
            }
        }
    }
}
