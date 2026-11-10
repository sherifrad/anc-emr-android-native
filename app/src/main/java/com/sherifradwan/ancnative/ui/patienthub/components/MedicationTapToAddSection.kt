package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.sherifradwan.ancnative.model.isOngoingMedication
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.DEFAULT_MEDICATION_ROUTE
import com.sherifradwan.ancnative.state.MedicationPrefill
import com.sherifradwan.ancnative.ui.EditorField
import com.sherifradwan.ancnative.ui.SectionCard
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.ui.patienthub.model.MedicationTile
import com.sherifradwan.ancnative.ui.patienthub.model.filterMedicationTiles
import java.util.UUID

private const val DISABLED_TILE_ALPHA = 0.38f

/**
 * The add picker. A tile opens the medication sheet filled with its dose, route and frequency through one
 * [AncEvent.StartMedicationEditor]. A tile for a drug that is already active shows its check and does nothing.
 * While [enabled] is false (a save runs or a draft is open) every tile is visibly disabled.
 */
@Composable
fun MedicationTapToAddSection(
    patient: DemoPatient,
    enabled: Boolean,
    onEvent: (AncEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val catalogRepo = remember { ClinicalCatalogRepository.getInstance(context) }
    val catalogMedItems by catalogRepo.medicationCatalogItems.collectAsState()
    val customChoices by catalogRepo.customChoices(CustomChoiceList.MEDICATION_NAME).collectAsState(initial = emptyList())

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf("common") }

    SectionCard(
        title = stringResource(R.string.tap_to_add),
        tag = "medTapToAddCard",
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.tap_to_add_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        EditorField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = stringResource(R.string.search_drugs_placeholder),
            tag = "medSearchField",
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedTab == "common",
                onClick = { selectedTab = "common" },
                label = { Text(stringResource(R.string.tab_common), maxLines = 1, softWrap = false) },
                modifier = Modifier.minimumTouchTarget().testTag("medTab-common"),
            )
            FilterChip(
                selected = selectedTab == "recent",
                onClick = { selectedTab = "recent" },
                label = { Text(stringResource(R.string.tab_recent), maxLines = 1, softWrap = false) },
                modifier = Modifier.minimumTouchTarget().testTag("medTab-recent"),
            )
        }

        val allTiles = remember(catalogMedItems, customChoices) {
            val catalog = catalogMedItems.map {
                MedicationTile(
                    id = it.id,
                    name = it.name,
                    dosage = it.dosage,
                    route = it.route,
                    frequency = it.frequency,
                    isCustom = it.isCustom,
                )
            }
            val custom = customChoices.map {
                MedicationTile(
                    id = "custom_${it.id}",
                    name = it.label,
                    dosage = "",
                    route = DEFAULT_MEDICATION_ROUTE,
                    frequency = "",
                    isCustom = true,
                )
            }
            catalog + custom
        }

        val pastMeds = remember(patient.medications) {
            patient.medications.map { it.name.trim() }.distinctBy { it.lowercase() }.toSet()
        }

        val filteredTiles = remember(searchQuery, selectedTab, allTiles, pastMeds) {
            filterMedicationTiles(
                allTiles = allTiles,
                selectedTab = selectedTab,
                searchQuery = searchQuery,
                pastMedicationNames = pastMeds,
            )
        }

        if (selectedTab == "recent" && filteredTiles.isEmpty()) {
            Text(
                text = stringResource(R.string.no_recent_medications),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().testTag("medTilesGrid"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                filteredTiles.forEach { tile ->
                    val isActive = patient.medications.any {
                        it.name.equals(tile.name, ignoreCase = true) && isOngoingMedication(it.status, it.isNonCompliant)
                    }
                    val subtitleText = if (tile.dosage.isNotBlank()) {
                        "${tile.dosage} · ${tile.route}"
                    } else {
                        stringResource(R.string.set_dose_in_editor)
                    }
                    val alreadyOnList = stringResource(R.string.already_on_todays_list, tile.name)
                    val tileEnabled = enabled && !isActive
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumTouchTarget()
                            .testTag("medTile-${tile.id}")
                            .semantics { if (isActive) stateDescription = alreadyOnList }
                            .alpha(if (enabled || isActive) 1f else DISABLED_TILE_ALPHA)
                            .clickable(enabled = tileEnabled, role = Role.Button) {
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
                        shape = RoundedCornerShape(14.dp),
                        color = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    subtitleText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
