package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.catalog.ClinicalCatalogRepository
import com.sherifradwan.ancnative.model.catalog.CustomChoiceList
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.MedicationPrefill
import com.sherifradwan.ancnative.ui.EditorField
import com.sherifradwan.ancnative.ui.medicationDoseRouteFrequency
import com.sherifradwan.ancnative.ui.medicationFrequencyLabel
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.ui.patienthub.model.MedicationTile
import com.sherifradwan.ancnative.ui.patienthub.model.PICKER_SOURCE_COMMON
import com.sherifradwan.ancnative.ui.patienthub.model.PICKER_SOURCE_RECENT
import com.sherifradwan.ancnative.ui.patienthub.model.activeMedicationKeys
import com.sherifradwan.ancnative.ui.patienthub.model.filterMedicationTiles
import com.sherifradwan.ancnative.ui.patienthub.model.isAlreadyActive
import com.sherifradwan.ancnative.ui.patienthub.model.medicationTiles
import java.util.UUID

/** About five tiles (56 dp each with their gaps) show before the list scrolls inside itself. */
private val PICKER_LIST_MAX_HEIGHT = 304.dp

/** How much of the results is kept above the keyboard while searching: the first tile and its gap. */
private val PICKER_FIRST_ROW = 64.dp

/**
 * What the picker shows: open or closed, the search text and the source chip. The Meds tab keeps one per patient
 * in saved state, so adding, stopping, editing, rotation and tab swipes leave it as it was.
 */
@Stable
class MedicationPickerState(expanded: Boolean = false, query: String = "", source: String = PICKER_SOURCE_COMMON) {
    var expanded by mutableStateOf(expanded)
    var query by mutableStateOf(query)
    var source by mutableStateOf(source)

    companion object {
        val Saver: Saver<MedicationPickerState, Any> = listSaver(
            save = { listOf(it.expanded, it.query, it.source) },
            restore = { MedicationPickerState(it[0] as Boolean, it[1] as String, it[2] as String) },
        )
    }
}

@Composable
fun rememberMedicationPickerState(patientId: String): MedicationPickerState =
    rememberSaveable(patientId, saver = MedicationPickerState.Saver) { MedicationPickerState() }

/**
 * The add picker: one "Add medication" row that opens to a search field, the Common and Recent chips and a list
 * with a limited height that scrolls inside itself. A tile opens the medication sheet filled with its dose, route
 * and frequency through one [AncEvent.StartMedicationEditor]. A tile for a drug that is already active shows its
 * check and does nothing. While [enabled] is false (a save runs or a draft is open) every control is visibly disabled.
 */
@Composable
fun MedicationTapToAddSection(
    patient: DemoPatient,
    enabled: Boolean,
    state: MedicationPickerState,
    onEvent: (AncEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("medPicker"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PickerToggle(expanded = state.expanded, enabled = enabled, onToggle = { state.expanded = !state.expanded })
        if (state.expanded) {
            PickerPanel(patient = patient, enabled = enabled, state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun PickerToggle(expanded: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val stateText = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    val actionText = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand)
    // Dimmed as a whole (border and background too) while disabled.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else MEDICATION_DISABLED_ALPHA)
            .testTag("medPickerToggle")
            .semantics { stateDescription = stateText }
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = actionText, onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Both icons are decorative: the row's text, state and click label say it all.
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.add_medication),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PickerPanel(
    patient: DemoPatient,
    enabled: Boolean,
    state: MedicationPickerState,
    onEvent: (AncEvent) -> Unit,
) {
    val context = LocalContext.current
    val catalogRepo = remember { ClinicalCatalogRepository.getInstance(context) }
    val catalogMedItems by catalogRepo.medicationCatalogItems.collectAsState()
    val customChoices by catalogRepo.customChoices(CustomChoiceList.MEDICATION_NAME).collectAsState(initial = emptyList())

    // While the doctor types with the keyboard up, the top of the results is scrolled into view above it.
    val keyboard = LocalSoftwareKeyboardController.current
    val resultsRequester = remember { BringIntoViewRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible
    val firstRowPx = with(LocalDensity.current) { PICKER_FIRST_ROW.toPx() }
    LaunchedEffect(state.query, state.source, imeVisible, searchFocused) {
        if (searchFocused && imeVisible) {
            withFrameNanos { }
            resultsRequester.bringIntoView(Rect(0f, 0f, 1f, firstRowPx))
        }
    }

    EditorField(
        value = state.query,
        onValueChange = { state.query = it },
        label = stringResource(R.string.search_drugs_placeholder),
        tag = "medSearchField",
        modifier = Modifier.onFocusChanged { searchFocused = it.isFocused },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = state.source == PICKER_SOURCE_COMMON,
            onClick = { state.source = PICKER_SOURCE_COMMON },
            enabled = enabled,
            label = { Text(stringResource(R.string.tab_common), maxLines = 1, softWrap = false) },
            modifier = Modifier.minimumTouchTarget().testTag("medTab-common"),
        )
        FilterChip(
            selected = state.source == PICKER_SOURCE_RECENT,
            onClick = { state.source = PICKER_SOURCE_RECENT },
            enabled = enabled,
            label = { Text(stringResource(R.string.tab_recent), maxLines = 1, softWrap = false) },
            modifier = Modifier.minimumTouchTarget().testTag("medTab-recent"),
        )
    }

    val allTiles = remember(catalogMedItems, customChoices) { medicationTiles(catalogMedItems, customChoices) }
    val pastNames = remember(patient.medications) { patient.medications.mapTo(HashSet()) { it.name } }
    val activeKeys = remember(patient.medications) { activeMedicationKeys(patient.medications) }
    val filteredTiles = remember(state.query, state.source, allTiles, pastNames) {
        filterMedicationTiles(
            allTiles = allTiles,
            selectedTab = state.source,
            searchQuery = state.query,
            pastMedicationNames = pastNames,
        )
    }

    if (filteredTiles.isEmpty()) {
        Text(
            text = stringResource(
                if (state.source == PICKER_SOURCE_RECENT && state.query.isBlank()) {
                    R.string.no_recent_medications
                } else {
                    R.string.medication_picker_no_match
                },
                stringResource(R.string.prescribe_medication),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.bringIntoViewRequester(resultsRequester).padding(vertical = 8.dp).testTag("medPickerEmpty"),
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(resultsRequester)
                .heightIn(max = PICKER_LIST_MAX_HEIGHT)
                .verticalScroll(rememberScrollState())
                .testTag("medTilesGrid"),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            filteredTiles.forEach { tile ->
                MedicationPickerTile(
                    tile = tile,
                    isActive = tile.isAlreadyActive(activeKeys),
                    enabled = enabled,
                    onClick = {
                        onEvent(
                            AncEvent.StartMedicationEditor(
                                patientId = patient.id,
                                medicationId = null,
                                newMedicationId = UUID.randomUUID().toString(),
                                editorSessionId = UUID.randomUUID().toString(),
                                prefill = MedicationPrefill(
                                    name = tile.name,
                                    dosage = tile.dosage,
                                    route = tile.route,
                                    frequency = tile.frequency,
                                ),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MedicationPickerTile(tile: MedicationTile, isActive: Boolean, enabled: Boolean, onClick: () -> Unit) {
    // "dose · route · frequency", as on the rows. A tile with no saved dose asks for one; a tile with no saved
    // frequency says the doctor chooses it in the sheet.
    val meta = if (tile.dosage.isBlank()) {
        stringResource(R.string.set_dose_in_editor)
    } else {
        val frequency = if (tile.frequency.isBlank()) {
            stringResource(R.string.medication_choose_frequency)
        } else {
            medicationFrequencyLabel(tile.frequency)
        }
        medicationDoseRouteFrequency(tile.dosage, tile.route, frequency)
    }
    val alreadyOnList = stringResource(R.string.already_on_todays_list, tile.name)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .minimumTouchTarget()
            .testTag("medTile-${tile.id}")
            .semantics { if (isActive) stateDescription = alreadyOnList }
            .alpha(if (enabled || isActive) 1f else MEDICATION_DISABLED_ALPHA)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled && !isActive, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("medTileMeta-${tile.id}"),
                )
            }
            if (isActive) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp).testTag("medTileCheck-${tile.id}"),
                    )
                    Text(
                        stringResource(R.string.medication_status_active),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
