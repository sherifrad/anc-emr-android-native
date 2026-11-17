package com.sherifradwan.ancnative.ui.patienthub.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
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
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.ui.patienthub.components.MedicationCard
import com.sherifradwan.ancnative.ui.patienthub.components.MedicationStopBar
import com.sherifradwan.ancnative.ui.patienthub.components.MedicationTapToAddSection
import com.sherifradwan.ancnative.ui.patienthub.components.rememberMedicationPickerState
import com.sherifradwan.ancnative.ui.patienthub.model.stableMedicationOrder
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
    // While the sheet covers the tab nothing looks dimmed, so the tab does not flash when the sheet opens and closes.
    val entryDimmed = !entryEnabled && medicationDraft == null
    // The keyboard (picker search) shortens the list instead of covering its end.
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        MedsList(
            patient = patient,
            canEdit = canEdit,
            entryEnabled = entryEnabled,
            entryDimmed = entryDimmed,
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
    entryDimmed: Boolean,
    onEvent: (AncEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Rows already shown keep their place; a new drug joins the end of its list instead of jumping to the top.
    var shownOrder by rememberSaveable(patient.id) { mutableStateOf(emptyList<String>()) }
    val ordered = remember(patient.medications, shownOrder) { stableMedicationOrder(shownOrder, patient.medications) }
    LaunchedEffect(ordered) { shownOrder = ordered.map { it.id } }
    val activeMeds = remember(ordered) { ordered.filter { isOngoingMedication(it.status, it.isNonCompliant) } }
    val pastMeds = remember(ordered) { ordered.filter { !isOngoingMedication(it.status, it.isNonCompliant) } }

    // Kept here, above the lazy items, in saved state per patient: adding, stopping, editing, rotation and tab swipes
    // leave the open card, the picker and the "Show past" toggle as they were.
    var expandedMedId by rememberSaveable(patient.id) { mutableStateOf<String?>(null) }
    var showPast by rememberSaveable(patient.id) { mutableStateOf(false) }
    val pickerState = rememberMedicationPickerState(patient.id)

    val listState = key(patient.id) { rememberLazyListState() }
    // A drug added from the open picker joins the end of the active list, just above the picker. The list is told,
    // in the same composition, to keep the picker where it was on screen, so the first frame that shows the new row
    // already has the picker in place (no frame with it moved). Otherwise the list keeps its usual anchor (the first
    // visible row), so a stop, or an add from "Prescribe" with the picker closed, leaves the top of the screen still.
    val activeIds = remember(activeMeds) { activeMeds.mapTo(HashSet()) { it.id } }
    val pickerAnchor = remember(listState) { PickerAnchor() }
    val before = pickerAnchor.activeIds
    if (canEdit && pickerState.expanded && before != null && !before.containsAll(activeIds)) {
        // Not observed: the scroll position must not recompose the tab. layoutInfo still holds the last layout here.
        val oldOffset = Snapshot.withoutReadObservation {
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == MEDS_PICKER_KEY }?.offset
        }
        if (oldOffset != null) {
            // Items before the picker: the header, the "no active" line (or the empty state), then the active rows.
            val placeholder = if (patient.medications.isEmpty() || activeMeds.isEmpty()) 1 else 0
            listState.requestScrollToItem(index = 1 + placeholder + activeMeds.size, scrollOffset = -oldOffset)
        }
    }
    SideEffect { pickerAnchor.activeIds = activeIds }

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
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag("medsTabContent"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Fixed keys, so the rows keep their places (and the list its scroll position) as drugs move between lists.
        item(key = "medsActiveHeader") {
            // Title and "Prescribe" share a line when they fit; with large text the button wraps under the title.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pluralStringResource(R.plurals.active_prescriptions_title, activeMeds.size, activeMeds.size),
                    style = RecordType.SectionTitle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("medsActiveTitle"),
                )
                // With no medication at all the empty state carries the one primary "Prescribe" action.
                if (canEdit && patient.medications.isNotEmpty()) {
                    // The weight gives the button the rest of the line, or a whole line when it wraps, and it sits at the end.
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        Button(
                            enabled = entryEnabled,
                            onClick = startBlank,
                            colors = prescribeButtonColors(dimmed = entryDimmed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.minimumTouchTarget().testTag("addCustomMedicationBtn"),
                        ) {
                            Text(stringResource(R.string.prescribe_medication), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (patient.medications.isEmpty()) {
            item(key = "medsEmpty") {
                MedsEmptyState(canEdit = canEdit, enabled = entryEnabled, dimmed = entryDimmed, onPrescribe = startBlank)
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
                actionsDimmed = entryDimmed,
                onToggle = { expandedMedId = if (expandedMedId == med.id) null else med.id },
                onStop = { onEvent(AncEvent.StopMedication(patient.id, med.id, UUID.randomUUID().toString())) },
                onEdit = { startEdit(med) },
                onDelete = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.MEDICATION, med.id)) },
            )
        }

        if (canEdit) {
            item(key = MEDS_PICKER_KEY) {
                MedicationTapToAddSection(
                    patient = patient,
                    enabled = entryEnabled,
                    dimmed = entryDimmed,
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
                        actionsDimmed = entryDimmed,
                        onToggle = { expandedMedId = if (expandedMedId == med.id) null else med.id },
                        onEdit = { startEdit(med) },
                        onDelete = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.MEDICATION, med.id)) },
                    )
                }
            }
        }
    }
}

/** The lazy key of the picker item, also used to find it in the list's layout. */
private const val MEDS_PICKER_KEY = "medsPicker"

/** The active rows the last composition showed; plain (not state), so updating it never recomposes. */
/** Under the sheet (not dimmed) a disabled "Prescribe" keeps its look; only a real dim (a save runs) greys it. */
@Composable
private fun prescribeButtonColors(dimmed: Boolean) = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    disabledContainerColor = if (dimmed) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primary
    },
    disabledContentColor = if (dimmed) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else {
        MaterialTheme.colorScheme.onPrimary
    },
)

private class PickerAnchor {
    var activeIds: Set<String>? = null
}

/** No medication at all: says so and offers the first prescription as the main action (none when read-only). */
@Composable
private fun MedsEmptyState(canEdit: Boolean, enabled: Boolean, dimmed: Boolean, onPrescribe: () -> Unit) {
    Surface(
        shape = RecordShapes.Large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().testTag("medsEmptyState"),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.medications_empty_title),
                style = RecordType.CardName,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.medications_empty_hint),
                style = RecordType.FieldValue,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (canEdit) {
                Button(
                    onClick = onPrescribe,
                    enabled = enabled,
                    colors = prescribeButtonColors(dimmed),
                    modifier = Modifier.minimumTouchTarget().testTag("medEmptyPrescribe"),
                ) {
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
            style = RecordType.FieldValue.copy(fontWeight = FontWeight.Bold),
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
