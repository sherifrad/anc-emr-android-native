package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.SevereBpAttention
import com.sherifradwan.ancnative.model.previousDeliverySummary
import com.sherifradwan.ancnative.model.severeBpAttention
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.SavedNotice
import java.util.UUID

enum class PatientRiskTier { HIGH, MEDIUM, LOW, UNRECORDED }

internal fun parseRiskTier(risk: String?): PatientRiskTier {
    val raw = risk?.trim()?.lowercase() ?: return PatientRiskTier.UNRECORDED
    if (raw.isBlank()) return PatientRiskTier.UNRECORDED
    if (isHighRisk(risk)) return PatientRiskTier.HIGH
    if (Regex("\\b(moderate|medium)\\b").containsMatchIn(raw)) return PatientRiskTier.MEDIUM
    if (Regex("\\b(low|normal)\\b").containsMatchIn(raw)) return PatientRiskTier.LOW
    return PatientRiskTier.UNRECORDED
}

data class PatientDashboardRowProjection(
    val id: String,
    val caseId: String,
    val displayName: String,
    val ageText: String?,
    val isArchived: Boolean,
    val obstetricSummary: String,
    val gestationalAgeText: String,
    val eddText: String?,
    val datingMethodText: String?,
    val bloodGroup: String?,
    val isRhNegative: Boolean,
    val isRhPositive: Boolean,
    val isHighRisk: Boolean,
    val riskTier: PatientRiskTier,
    val clinicalAttention: String?,
)

fun projectDashboardRow(patient: DemoPatient): PatientDashboardRowProjection {
    val g = patient.gravida ?: 1L
    val p = patient.parity ?: 0L

    val prevSummary = previousDeliverySummary(patient)
    val obstetricFormatted = "G$g P$p · $prevSummary"

    val gaResult = patient.dating?.calculateGestationalAgeResult()
    val gaFormatted = gaResult?.shortWeeksText ?: "—"

    val edd = patient.dating?.calculateEstimatedDueDate()?.toString()
        ?: patient.dating?.ultrasoundEdd
        ?: patient.dating?.agreedEdd

    val datingMethod = when {
        patient.dating?.agreedEdd != null -> "Agreed Clinical EDD"
        patient.dating?.ultrasoundEdd != null -> "Ultrasound Dating"
        patient.dating?.lmpDate != null -> "LMP Dating"
        else -> null
    }

    val bg = patient.bloodGroupText?.trim()
    val isRhNeg = isRhNegative(bg)
    val isRhPos = isRhPositive(bg)
    val isHigh = isHighRisk(patient.riskLevel)
    val tier = parseRiskTier(patient.riskLevel)

    return PatientDashboardRowProjection(
        id = patient.id,
        caseId = patient.caseId,
        displayName = patient.displayName,
        ageText = patient.ageText,
        isArchived = patient.status == "archived",
        obstetricSummary = obstetricFormatted,
        gestationalAgeText = gaFormatted,
        eddText = edd,
        datingMethodText = datingMethod,
        bloodGroup = bg,
        isRhNegative = isRhNeg,
        isRhPositive = isRhPos,
        isHighRisk = isHigh,
        riskTier = tier,
        clinicalAttention = patient.clinicalAttention?.takeIf { it.isNotBlank() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(state: AncState, onEvent: (AncEvent) -> Unit) {
    val visibleList = remember(state.patients, state.searchQuery) { state.visiblePatients() }
    var riskFilter by remember { mutableStateOf("all") } // "all", "high", "medium", "low"
    var sheetPatient by remember { mutableStateOf<DemoPatient?>(null) }

    val filteredList = remember(visibleList, riskFilter) {
        when (riskFilter) {
            "high" -> visibleList.filter { isHighRisk(it.riskLevel) }
            "medium" -> visibleList.filter { parseRiskTier(it.riskLevel) == PatientRiskTier.MEDIUM }
            "low" -> visibleList.filter { parseRiskTier(it.riskLevel) == PatientRiskTier.LOW }
            else -> visibleList
        }
    }

    val allCount = visibleList.size
    val highRiskCount = remember(visibleList) { visibleList.count { isHighRisk(it.riskLevel) } }
    val mediumRiskCount = remember(visibleList) { visibleList.count { parseRiskTier(it.riskLevel) == PatientRiskTier.MEDIUM } }
    val lowRiskCount = remember(visibleList) { visibleList.count { parseRiskTier(it.riskLevel) == PatientRiskTier.LOW } }

    val activeCount = remember(state.patients) { state.patients.count { it.status != "archived" } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = maxOf(16.dp, (maxWidth - 880.dp) / 2)

        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("patientListTable"),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Dashboard Header with Active Count & New Patient Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.patients),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() },
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = stringResource(R.string.active_patients_count, activeCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Button(
                        onClick = {
                            onEvent(
                                AncEvent.StartPatientEditor(
                                    patientId = null,
                                    newPatientId = UUID.randomUUID().toString(),
                                    editorSessionId = UUID.randomUUID().toString(),
                                ),
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("newPatient"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.new_patient), maxLines = 1, softWrap = false)
                    }
                }
            }

            // Save / Archive Notices
            if (state.savedNotice && state.notice in setOf(SavedNotice.ARCHIVED, SavedNotice.UNARCHIVED)) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Text(
                            text = stringResource(
                                if (state.notice == SavedNotice.ARCHIVED) R.string.patient_archived_notice
                                else R.string.patient_unarchived_notice,
                            ),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { onEvent(AncEvent.SearchChanged(it)) },
                    label = { Text(stringResource(R.string.search_patients)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onEvent(AncEvent.ClearSearch) }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("patientSearch"),
                )
            }

            // Filter Chips Bar (All, High, Medium, Low)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = riskFilter == "all",
                        onClick = { riskFilter = "all" },
                        label = { Text("${stringResource(R.string.all_filter)} ($allCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.testTag("riskFilter-all"),
                    )
                    FilterChip(
                        selected = riskFilter == "high",
                        onClick = { riskFilter = "high" },
                        label = { Text("${stringResource(R.string.high_risk_filter)} ($highRiskCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier.testTag("riskFilter-high"),
                    )
                    FilterChip(
                        selected = riskFilter == "medium",
                        onClick = { riskFilter = "medium" },
                        label = { Text("${stringResource(R.string.medium_risk_filter)} ($mediumRiskCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        modifier = Modifier.testTag("riskFilter-medium"),
                    )
                    FilterChip(
                        selected = riskFilter == "low",
                        onClick = { riskFilter = "low" },
                        label = { Text("${stringResource(R.string.low_risk_filter)} ($lowRiskCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                        modifier = Modifier.testTag("riskFilter-low"),
                    )
                }
            }

            // Patient List Items
            if (filteredList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (state.searchQuery.isNotBlank()) "No matching patients found" else "No patients in this category",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { patient ->
                    val projection = remember(patient) { projectDashboardRow(patient) }
                    PatientDirectoryCard(
                        patient = patient,
                        row = projection,
                        onEvent = onEvent,
                        onOpenMenu = { sheetPatient = patient },
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet for Patient Card "⋯" Actions
    sheetPatient?.let { p ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { sheetPatient = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = p.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${p.ageText ?: "—"} · G${p.gravida ?: 1} P${p.parity ?: 0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Action 1: Edit Patient Record
                if (p.status != "archived") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                sheetPatient = null
                                onEvent(
                                    AncEvent.StartPatientEditor(
                                        patientId = p.id,
                                        newPatientId = p.id,
                                        editorSessionId = UUID.randomUUID().toString(),
                                    ),
                                )
                            }
                            .testTag("editPatient-${p.caseId}"),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text(stringResource(R.string.edit_patient), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                // Action 2: Archive / Restore Patient
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            sheetPatient = null
                            onEvent(
                                AncEvent.StartStatusEditor(
                                    patientId = p.id,
                                    targetStatus = if (p.status == "archived") "active" else "archived",
                                    editorSessionId = UUID.randomUUID().toString(),
                                ),
                            )
                        }
                        .testTag("statusPatient-${p.caseId}"),
                    color = if (p.status == "archived") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else if (isDark) TagHighRiskDarkBg else TagHighRiskBg,
                    border = BorderStroke(1.dp, if (p.status == "archived") MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                if (p.status == "archived") Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = null,
                                tint = if (p.status == "archived") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Column {
                                Text(
                                    text = stringResource(if (p.status == "archived") R.string.unarchive_patient else R.string.archive_patient),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (p.status == "archived") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = stringResource(if (p.status == "archived") R.string.unarchive_patient_subtitle else R.string.archive_patient_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Action 3: Open Workspace
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            sheetPatient = null
                            onEvent(AncEvent.OpenWorkspace(p.id))
                        }
                        .testTag("workspace-${p.caseId}"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.open_workspace), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Action 4: Open Profile / Chart
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            sheetPatient = null
                            onEvent(AncEvent.OpenWorkspace(p.id))
                        }
                        .testTag("profile-${p.caseId}"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.profile), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(Modifier.height(4.dp))

                TextButton(
                    onClick = { sheetPatient = null },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PatientDirectoryCard(
    patient: DemoPatient,
    row: PatientDashboardRowProjection,
    onEvent: (AncEvent) -> Unit,
    onOpenMenu: () -> Unit,
) {
    val severeBp = remember(patient.visits) { severeBpAttention(patient.visits) }
    val isArabic = row.displayName.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val rowBgColor = if (row.isHighRisk) {
        lerp(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.errorContainer,
            0.04f,
        )
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("patient-${patient.caseId}")
            .clickable { onEvent(AncEvent.OpenWorkspace(patient.id)) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = rowBgColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Line 1: Patient Name (Bold) + Risk Pill + Severe BP Badge (if any) + 48dp "⋯" menu button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = row.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    RiskPillBadge(tier = row.riskTier, isHigh = row.isHighRisk)

                    if (severeBp != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) TagHighRiskDarkBg else TagHighRiskBg,
                            border = BorderStroke(1.dp, if (isDark) TagHighRiskDarkBorder else TagHighRiskBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(11.dp),
                                )
                                Text(
                                    text = stringResource(R.string.bp_badge_severe),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier
                            .minimumTouchTarget()
                            .testTag("patientMenu-${patient.caseId}"),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_actions),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Line 2: Clinical tags (Obstetric GPA, Age, Blood Group) wrapped in FlowRow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isArabic) Arrangement.spacedBy(8.dp, Alignment.End) else Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Obstetric GPA & Previous Deliveries
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) TagObstetricDarkBg else TagObstetricBg,
                    border = BorderStroke(1.dp, if (isDark) TagObstetricDarkBorder else TagObstetricBorder),
                ) {
                    Text(
                        text = row.obstetricSummary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) TagObstetricDarkText else TagObstetricText,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }

                // Maternal Age with standard validated PatientAgeBadge
                if (row.ageText != null) {
                    PatientAgeBadge(ageText = row.ageText)
                }

                // Blood Group
                val bloodGroupFormatted = formatBloodGroup(row.bloodGroup)
                if (bloodGroupFormatted != null) {
                    val isRhNeg = isRhNegative(row.bloodGroup)
                    val bgSurfaceColor = if (isRhNeg) {
                        if (isDark) TagBloodNegativeDarkBg else TagBloodNegativeBg
                    } else {
                        if (isDark) TagBloodPositiveDarkBg else TagBloodPositiveBg
                    }
                    val bgBorderColor = if (isRhNeg) {
                        if (isDark) TagBloodNegativeDarkBorder else TagBloodNegativeBorder
                    } else {
                        if (isDark) TagBloodPositiveDarkBorder else TagBloodPositiveBorder
                    }
                    val bgTextColor = if (isRhNeg) {
                        if (isDark) TagBloodNegativeDarkText else TagBloodNegativeText
                    } else {
                        if (isDark) TagBloodPositiveDarkText else TagBloodPositiveText
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = bgSurfaceColor,
                        border = BorderStroke(1.dp, bgBorderColor),
                    ) {
                        Text(
                            text = bloodGroupFormatted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isRhNeg) FontWeight.Bold else FontWeight.Medium,
                            color = bgTextColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }

                if (row.isArchived) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            text = stringResource(R.string.archived),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // Line 3: Gestational Age & EDD
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.ga_prefix, row.gestationalAgeText),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = " · " + stringResource(R.string.edd_prefix, row.eddText ?: stringResource(R.string.not_recorded)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Line 4: Prominent Clinical Attention Callout Box (up to 140 chars / 2 visible lines)
            DirectoryClinicalAttentionBox(
                clinicalAttention = row.clinicalAttention,
                severeBp = severeBp,
            )

            // Line 5: Quick Card Actions (Workspace and Profile)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onEvent(AncEvent.OpenWorkspace(patient.id)) },
                    modifier = Modifier
                        .minimumTouchTarget()
                        .testTag("workspace-${patient.caseId}"),
                ) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = stringResource(R.string.open_workspace),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                IconButton(
                    onClick = { onEvent(AncEvent.OpenWorkspace(patient.id)) },
                    modifier = Modifier
                        .minimumTouchTarget()
                        .testTag("profile-${patient.caseId}"),
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = stringResource(R.string.profile),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskPillBadge(
    tier: PatientRiskTier,
    isHigh: Boolean,
    modifier: Modifier = Modifier,
) {
    if (tier == PatientRiskTier.UNRECORDED && !isHigh) return

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val (bgColor, textColor, label) = when {
        isHigh || tier == PatientRiskTier.HIGH -> Triple(
            if (isDark) TagHighRiskDarkBg else TagHighRiskBg,
            if (isDark) TagHighRiskDarkText else TagHighRiskText,
            stringResource(R.string.high_risk),
        )
        tier == PatientRiskTier.MEDIUM -> Triple(
            if (isDark) TagAttentionDarkBg else TagAttentionBg,
            if (isDark) TagAttentionDarkText else TagAttentionText,
            stringResource(R.string.medium_risk_filter),
        )
        else -> Triple(
            if (isDark) TagNormalRiskDarkBg else TagNormalRiskBg,
            if (isDark) TagNormalRiskDarkText else TagNormalRiskText,
            stringResource(R.string.low_risk_filter),
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun DirectoryClinicalAttentionBox(
    clinicalAttention: String?,
    severeBp: SevereBpAttention?,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val hasContent = !clinicalAttention.isNullOrBlank() || severeBp != null

    if (hasContent) {
        val containerColor = if (isDark) TagBloodNegativeDarkBg else TagBloodNegativeBg
        val borderColor = if (isDark) TagBloodNegativeDarkBorder else TagBloodNegativeBorder
        val accentBarColor = ActionRose
        val textColor = if (isDark) TagBloodNegativeDarkText else TagBloodNegativeText

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(containerColor)
                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(28.dp)
                    .background(accentBarColor, RoundedCornerShape(2.dp)),
            )

            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = accentBarColor,
                modifier = Modifier.size(15.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                if (severeBp != null) {
                    Text(
                        text = stringResource(R.string.bp_attention_severe, severeBp.bloodPressure, severeBp.date),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!clinicalAttention.isNullOrBlank()) {
                    Text(
                        text = clinicalAttention,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    } else {
        // Calm muted placeholder
        val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerHighest
        val borderColor = MaterialTheme.colorScheme.outlineVariant
        val accentBarColor = MaterialTheme.colorScheme.outline

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(containerColor)
                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(accentBarColor, RoundedCornerShape(2.dp)),
            )
            Text(
                text = stringResource(R.string.no_clinical_attention_recorded),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
