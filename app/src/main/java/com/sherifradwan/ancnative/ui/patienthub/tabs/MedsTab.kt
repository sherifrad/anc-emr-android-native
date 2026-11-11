package com.sherifradwan.ancnative.ui.patienthub.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoMedicationRecord
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
import com.sherifradwan.ancnative.ui.patienthub.components.MedicationStopBar
import com.sherifradwan.ancnative.ui.patienthub.components.MedicationTapToAddSection
import com.sherifradwan.ancnative.ui.patienthub.components.rememberMedicationPickerState
import java.util.UUID

/**
 * The Meds tab, review first: the active list, then the collapsed "Add medication" picker, then stopped drugs behind
 * "Show past". Adding, editing and stopping never leave it: tiles, edit and "Prescribe" open the medication sheet
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
    // The keyboard (picker search) shortens the list instead of covering its end.
    Column(modifier = modifier.fillMaxSize().imePadding()) {
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

@OptIn(ExperimentalLayoutApi::class)
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

    // Kept here, above the lazy items, in saved state per patient: adding, stopping, editing, rotation and tab swipes
    // leave the open card, the picker and the "Show past" toggle as they were.
    var expandedMedId by rememberSaveable(patient.id) { mutableStateOf<String?>(null) }
    var showPast by rememberSaveable(patient.id) { mutableStateOf(false) }
    val pickerState = rememberMedicationPickerState(patient.id)

    val startBlank = {
        onEvent(
            AncEvent.StartMedicationEditor(
                patientId = patient.id,
                medicationId = null,
                newMedicationId = UUID.randomUUID().toString(),
                editorSessionId = UUID.randomUUID().toString(),
            ),
        )
    }
    val startEdit = { med: DemoMedicationRecord ->
        onEvent(
            AncEvent.StartMedicationEditor(
                patientId = patient.id,
                medicationId = med.id,
                newMedicationId = med.id,
                editorSessionId = UUID.randomUUID().toString(),
            ),
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("medsTabContent"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Fixed keys, so the rows keep their places (and the list its scroll position) as drugs move between lists.
        item(key = "medsActiveHeader") {
            // Title and "Prescribe" share a line when they fit; with large text the button wraps under the title.
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pluralStringResource(R.plurals.active_prescriptions_title, activeMeds.size, activeMeds.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp).testTag("medsActiveTitle"),
                )
                // With no medication at all the empty state carries the one primary "Prescribe" action.
                if (canEdit && patient.medications.isNotEmpty()) {
                    Button(
                        enabled = entryEnabled,
                        onClick = startBlank,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.minimumTouchTarget().testTag("addCustomMedicationBtn"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.prescribe_medication), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (patient.medications.isEmpty()) {
            item(key = "medsEmpty") {
                MedsEmptyState(canEdit = canEdit, enabled = entryEnabled, onPrescribe = startBlank)
            }
        } else if (activeMeds.isEmpty()) {
            item(key = "medsNoActive") {
                Text(
                    text = stringResource(R.string.no_active_medications),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp).testTag("medsNoActive"),
                )
            }
        }

        items(items = activeMeds, key = { it.id }) { med ->
            MedicationCard(
                med = med,
                isExpanded = expandedMedId == med.id,
                canEdit = canEdit,
                actionsEnabled = entryEnabled,
                onToggle = { expandedMedId = if (expandedMedId == med.id) null else med.id },
                onStop = { onEvent(AncEvent.StopMedication(patient.id, med.id, UUID.randomUUID().toString())) },
                onEdit = { startEdit(med) },
                onDelete = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.MEDICATION, med.id)) },
            )
        }

        if (canEdit) {
            item(key = "medsPicker") {
                MedicationTapToAddSection(
                    patient = patient,
                    enabled = entryEnabled,
                    state = pickerState,
                    onEvent = onEvent,
                )
            }
        }

        if (pastMeds.isNotEmpty()) {
            item(key = "medsPastToggle") {
                ShowPastToggle(count = pastMeds.size, shown = showPast, onToggle = { showPast = !showPast })
            }
            if (showPast) {
                // Stopped rows get no stop callback, so they never show the stop symbol.
                items(items = pastMeds, key = { it.id }) { med ->
                    MedicationCard(
                        med = med,
                        isExpanded = expandedMedId == med.id,
                        canEdit = canEdit,
                        actionsEnabled = entryEnabled,
                        onToggle = { expandedMedId = if (expandedMedId == med.id) null else med.id },
                        onEdit = { startEdit(med) },
                        onDelete = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.MEDICATION, med.id)) },
                    )
                }
            }
        }
    }
}

/** No medication at all: says so and offers the first prescription as the main action (none when read-only). */
@Composable
private fun MedsEmptyState(canEdit: Boolean, enabled: Boolean, onPrescribe: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().testTag("medsEmptyState"),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.medications_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.medications_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (canEdit) {
                Button(
                    onClick = onPrescribe,
                    enabled = enabled,
                    modifier = Modifier.minimumTouchTarget().testTag("medEmptyPrescribe"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.medications_empty_action), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** The "Show past (N)" row. Viewing stopped drugs starts no action, so it stays usable while a save runs. */
@Composable
private fun ShowPastToggle(count: Int, shown: Boolean, onToggle: () -> Unit) {
    val stateText = stringResource(if (shown) R.string.state_expanded else R.string.state_collapsed)
    val actionText = stringResource(if (shown) R.string.action_collapse else R.string.action_expand)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumTouchTarget()
            .testTag("medShowPastToggle")
            .semantics { stateDescription = stateText }
            .clickable(role = Role.Button, onClickLabel = actionText, onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = pluralStringResource(if (shown) R.plurals.medication_hide_past else R.plurals.medication_show_past, count, count),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        // Decorative: the row's text and state say whether the list is open.
        Icon(
            imageVector = if (shown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
