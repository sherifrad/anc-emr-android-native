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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.catalog.ClinicalCatalogRepository
import com.sherifradwan.ancnative.model.catalog.CustomChoiceList
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.MedicationPrefill
import com.sherifradwan.ancnative.ui.EditorField
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.medicationDoseRouteFrequency
import com.sherifradwan.ancnative.ui.medicationFrequencyLabel
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.ui.patienthub.model.MedicationTile
import com.sherifradwan.ancnative.ui.patienthub.model.PICKER_SOURCE_COMMON
import com.sherifradwan.ancnative.ui.patienthub.model.PICKER_SOURCE_RECENT
import com.sherifradwan.ancnative.ui.patienthub.model.activeMedicationKeys
import com.sherifradwan.ancnative.ui.patienthub.model.filterMedicationTiles
import com.sherifradwan.ancnative.ui.patienthub.model.isAlreadyActive
import com.sherifradwan.ancnative.ui.patienthub.model.medicationNameKey
import com.sherifradwan.ancnative.ui.patienthub.model.medicationTiles
import java.util.UUID

/** With no search text the picker shows this many compact chips; "Show all" reveals the rest. */
internal const val PICKER_CHIP_LIMIT = 8

/** With search text the picker shows this many full rows; "Show all" reveals the rest. */
internal const val PICKER_ROW_LIMIT = 5

/** The tiles to show: names starting with the query first, cut to [limit] unless [showAll]. */
internal fun pickerVisibleTiles(
    filtered: List<MedicationTile>,
    query: String,
    showAll: Boolean,
    limit: Int,
): List<MedicationTile> {
    val q = medicationNameKey(query)
    val ranked = if (q.isEmpty()) filtered else filtered.sortedBy { if (medicationNameKey(it.name).startsWith(q)) 0 else 1 }
    return if (showAll) ranked else ranked.take(limit)
}

/** How much of the results is kept above the keyboard while searching: the first tile and its gap. */
private val PICKER_FIRST_ROW = 64.dp

/**
 * What the picker shows: open or closed, the search text and the source chip. The Meds tab keeps one per patient
 * in saved state, so adding, stopping, editing, rotation and tab swipes leave it as it was.
 */
@Stable
class MedicationPickerState(
    expanded: Boolean = false,
    query: String = "",
    source: String = PICKER_SOURCE_COMMON,
    showAll: Boolean = false,
) {
    var expanded by mutableStateOf(expanded)
    var query by mutableStateOf(query)
    var source by mutableStateOf(source)
    var showAll by mutableStateOf(showAll)

    companion object {
        val Saver: Saver<MedicationPickerState, Any> = listSaver(
            save = { listOf(it.expanded, it.query, it.source, it.showAll) },
            restore = { MedicationPickerState(it[0] as Boolean, it[1] as String, it[2] as String, it[3] as Boolean) },
        )
    }
}

@Composable
fun rememberMedicationPickerState(patientId: String): MedicationPickerState =
    rememberSaveable(patientId, saver = MedicationPickerState.Saver) { MedicationPickerState() }

/**
 * The add picker: one "Add medication" row that opens to a search field, the Common and Recent chips and a short
 * list: compact chips with no search text, a few rows with it, and "Show all" for the rest. A tile opens the medication sheet filled with its dose, route
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
    /** Dimmed look of every control; differs from "not enabled" only while the sheet covers the tab. */
    dimmed: Boolean = !enabled,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("medPicker"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PickerToggle(expanded = state.expanded, enabled = enabled, dimmed = dimmed, onToggle = { state.expanded = !state.expanded })
        if (state.expanded) {
            PickerPanel(patient = patient, enabled = enabled, dimmed = dimmed, state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun PickerToggle(expanded: Boolean, enabled: Boolean, dimmed: Boolean, onToggle: () -> Unit) {
    val stateText = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    val actionText = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand)
    // Dimmed as a whole (border and background too) while disabled.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (dimmed) MEDICATION_DISABLED_ALPHA else 1f)
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
    dimmed: Boolean,
    state: MedicationPickerState,
    onEvent: (AncEvent) -> Unit,
) {
    val context = LocalContext.current
    val catalogRepo = remember { ClinicalCatalogRepository.getInstance(context) }
    val catalogMedItems by catalogRepo.medicationCatalogItems.collectAsState()
    val customChoicesFlow = remember(catalogRepo) { catalogRepo.customChoices(CustomChoiceList.MEDICATION_NAME) }
    val customChoices by customChoicesFlow.collectAsState()

    // While the doctor types with the keyboard up, the top of the results is scrolled into view above it.
    val keyboard = LocalSoftwareKeyboardController.current
    val resultsRequester = remember { BringIntoViewRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible
    val firstRowPx = with(LocalDensity.current) { PICKER_FIRST_ROW.toPx() }
    // The field keeps its normal look under the sheet, but it lets go of focus as soon as the picker is disabled (a
    // draft opens or a save runs), as a disabled field would. Otherwise, when the sheet closes, the still-focused field
    // would bring the keyboard back and scroll the list to itself, and keep the Meds page alive after a tab swipe.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(enabled) {
        if (!enabled && searchFocused) focusManager.clearFocus(force = true)
    }
    LaunchedEffect(state.query, state.source, imeVisible, searchFocused) {
        if (searchFocused && imeVisible) {
            withFrameNanos { }
            resultsRequester.bringIntoView(Rect(0f, 0f, 1f, firstRowPx))
        }
    }

    EditorField(
        value = state.query,
        onValueChange = { state.query = it; state.showAll = false },
        label = stringResource(R.string.search_drugs_placeholder),
        tag = "medSearchField",
        modifier = Modifier.onFocusChanged { searchFocused = it.isFocused },
        // Under the sheet (not dimmed) the field keeps its normal look: Material greys any disabled field.
        enabled = enabled || !dimmed,
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
            onClick = { state.source = PICKER_SOURCE_COMMON; state.showAll = false },
            enabled = enabled,
            colors = pickerFilterChipColors(state.source == PICKER_SOURCE_COMMON, dimmed),
            border = pickerFilterChipBorder(state.source == PICKER_SOURCE_COMMON, dimmed),
            label = { Text(stringResource(R.string.tab_common), maxLines = 1, softWrap = false) },
            modifier = Modifier.minimumTouchTarget().testTag("medTab-common"),
        )
        FilterChip(
            selected = state.source == PICKER_SOURCE_RECENT,
            onClick = { state.source = PICKER_SOURCE_RECENT; state.showAll = false },
            enabled = enabled,
            colors = pickerFilterChipColors(state.source == PICKER_SOURCE_RECENT, dimmed),
            border = pickerFilterChipBorder(state.source == PICKER_SOURCE_RECENT, dimmed),
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
        val searching = state.query.isNotBlank()
        val limit = if (searching) PICKER_ROW_LIMIT else PICKER_CHIP_LIMIT
        val showAllState = stringResource(if (state.showAll) R.string.state_expanded else R.string.state_collapsed)
        val visible = pickerVisibleTiles(filteredTiles, state.query, state.showAll, limit)
        val startEditor = { tile: MedicationTile ->
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
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(resultsRequester)
                .testTag("medTilesGrid"),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (searching) {
                visible.forEach { tile ->
                    MedicationPickerTile(
                        tile = tile,
                        isActive = tile.isAlreadyActive(activeKeys),
                        enabled = enabled,
                        dimmed = dimmed,
                        onClick = { startEditor(tile) },
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    visible.forEach { tile ->
                        MedicationQuickChip(
                            tile = tile,
                            isActive = tile.isAlreadyActive(activeKeys),
                            enabled = enabled,
                            dimmed = dimmed,
                            onClick = { startEditor(tile) },
                        )
                    }
                }
            }
            if (filteredTiles.size > limit) {
                TextButton(
                    onClick = { state.showAll = !state.showAll },
                    modifier = Modifier
                        .minimumTouchTarget()
                        .testTag("medPickerShowAll")
                        .semantics { stateDescription = showAllState },
                ) {
                    Text(
                        if (state.showAll) {
                            stringResource(R.string.medication_picker_show_less)
                        } else {
                            pluralStringResource(R.plurals.medication_picker_show_all, filteredTiles.size, filteredTiles.size)
                        },
                    )
                }
            }
        }
    }
}

/** A compact chip: the drug name only. One tap opens the prefilled sheet; an active drug shows its check and is disabled. */
@Composable
private fun MedicationQuickChip(tile: MedicationTile, isActive: Boolean, enabled: Boolean, dimmed: Boolean, onClick: () -> Unit) {
    val alreadyOnList = stringResource(R.string.already_on_todays_list, tile.name)
    // Material greys any disabled chip, so the chip always keeps its normal colours and border; like a tile, it is
    // dimmed only by the alpha below (a real dim), and an active drug stays at full colour with its check.
    val normal = AssistChipDefaults.assistChipColors()
    AssistChip(
        onClick = onClick,
        enabled = enabled && !isActive,
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = normal.containerColor,
            disabledLabelColor = normal.labelColor,
            disabledLeadingIconContentColor = normal.leadingIconContentColor,
        ),
        border = AssistChipDefaults.assistChipBorder(enabled = true),
        label = { Text(tile.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = if (isActive) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).testTag("medTileCheck-${tile.id}"),
                )
            }
        } else {
            null
        },
        shape = RecordShapes.Small,
        modifier = Modifier
            .minimumTouchTarget()
            .testTag("medTile-${tile.id}")
            .semantics { if (isActive) stateDescription = alreadyOnList }
            .alpha(if (dimmed && !isActive) MEDICATION_DISABLED_ALPHA else 1f),
    )
}

@Composable
private fun MedicationPickerTile(tile: MedicationTile, isActive: Boolean, enabled: Boolean, dimmed: Boolean, onClick: () -> Unit) {
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
            .alpha(if (dimmed && !isActive) MEDICATION_DISABLED_ALPHA else 1f)
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

/** Filter chip colours that match the enabled look while the sheet covers the tab (only a real dim greys them). */
@Composable
private fun pickerFilterChipColors(selected: Boolean, dimmed: Boolean) =
    if (dimmed) {
        FilterChipDefaults.filterChipColors()
    } else {
        val scheme = MaterialTheme.colorScheme
        FilterChipDefaults.filterChipColors(
            disabledContainerColor = Color.Transparent,
            // Material has one disabled label colour for both states, so it is picked by the selection.
            disabledLabelColor = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
            disabledSelectedContainerColor = scheme.secondaryContainer,
        )
    }

/** The matching border: unchanged under the sheet, Material's disabled border only for a real dim. */
@Composable
private fun pickerFilterChipBorder(selected: Boolean, dimmed: Boolean) =
    FilterChipDefaults.filterChipBorder(
        enabled = !dimmed,
        selected = selected,
    )
