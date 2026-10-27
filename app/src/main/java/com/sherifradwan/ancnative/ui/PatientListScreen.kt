package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.SavedNotice
import java.util.UUID

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
    val clinicalAttention: String?,
)

fun projectDashboardRow(patient: DemoPatient): PatientDashboardRowProjection {
    val g = patient.gravida ?: 1L
    val p = patient.parity ?: 0L

    val prevCS = patient.previousPregnancies.count {
        it.birthMethod.equals("Cesarean", ignoreCase = true) ||
            it.deliveryBy.equals("CS", ignoreCase = true)
    }
    val prevVaginal = patient.previousPregnancies.count {
        it.birthMethod.equals("Vaginal", ignoreCase = true) ||
            it.deliveryBy.equals("Vaginal", ignoreCase = true)
    }

    val prevSummary = when {
        g == 1L && p == 0L && patient.previousPregnancies.isEmpty() -> "Primigravida"
        p == 0L && patient.previousPregnancies.isEmpty() -> "P0 · No prior deliveries"
        prevCS > 0 && prevVaginal > 0 -> "Prev $prevCS CS, $prevVaginal SVD"
        prevCS > 0 -> "Prev $prevCS CS"
        prevVaginal > 0 -> "Prev $prevVaginal SVD"
        p > 0 -> "Parous ($p)"
        else -> "P0"
    }

    val obstetricFormatted = "G$g P$p · $prevSummary"

    val gaResult = patient.dating?.calculateGestationalAgeResult()
    val gaFormatted = if (gaResult != null) {
        "${gaResult.weeks}+${gaResult.days} wks"
    } else {
        "—"
    }

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
    val isRhNeg = bg != null && bg.contains("-")
    val isRhPos = bg != null && bg.contains("+")

    val isHigh = patient.riskLevel?.contains("High", ignoreCase = true) == true

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
        clinicalAttention = patient.clinicalAttention?.takeIf { it.isNotBlank() },
    )
}

@Composable
fun PatientListScreen(state: AncState, onEvent: (AncEvent) -> Unit) {
    val visibleList = remember(state.patients, state.searchQuery) { state.visiblePatients() }
    var riskFilter by remember { mutableStateOf("all") } // "all", "high", "normal"

    fun isPatientHighRisk(risk: String?): Boolean =
        risk?.contains("High", ignoreCase = true) == true

    val filteredList = remember(visibleList, riskFilter) {
        when (riskFilter) {
            "high" -> visibleList.filter { isPatientHighRisk(it.riskLevel) }
            "normal" -> visibleList.filter { !isPatientHighRisk(it.riskLevel) }
            else -> visibleList
        }
    }

    val activeCount = remember(state.patients) { state.patients.count { it.status != "archived" } }
    val highRiskCount = remember(state.patients) { state.patients.count { isPatientHighRisk(it.riskLevel) } }
    val normalRiskCount = remember(state.patients) { state.patients.count { !isPatientHighRisk(it.riskLevel) } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 640.dp
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
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("patientSearch"),
                )
            }

            // Filter Chips Bar (All, High Risk, Normal Risk)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = riskFilter == "all",
                        onClick = { riskFilter = "all" },
                        label = { Text("${stringResource(R.string.all_filter)} (${visibleList.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                    FilterChip(
                        selected = riskFilter == "high",
                        onClick = { riskFilter = "high" },
                        label = { Text("${stringResource(R.string.high_risk_filter)} ($highRiskCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    )
                    FilterChip(
                        selected = riskFilter == "normal",
                        onClick = { riskFilter = "normal" },
                        label = { Text("${stringResource(R.string.normal_risk_filter)} ($normalRiskCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                    )
                }
            }

            // Optional Table Column Header in Wide View
            if (isWide && filteredList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "PATIENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(2.2f),
                        )
                        Text(
                            text = "OBSTETRIC HISTORY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(2.0f),
                        )
                        Text(
                            text = "GESTATIONAL AGE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.3f),
                        )
                        Text(
                            text = "BLOOD GROUP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.0f),
                        )
                        Text(
                            text = "RISK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.1f),
                        )
                        Text(
                            text = "ACTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.4f),
                        )
                    }
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
                    PatientClinicalRow(
                        patient = patient,
                        row = projection,
                        isWide = isWide,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun PatientClinicalRow(
    patient: DemoPatient,
    row: PatientDashboardRowProjection,
    isWide: Boolean,
    onEvent: (AncEvent) -> Unit,
) {
    var showEddPopup by remember { mutableStateOf(false) }

    val rowBgColor = if (row.isHighRisk) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val rowBorder = if (row.isHighRisk) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("patient-${patient.caseId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = rowBgColor),
        border = rowBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .clickable { onEvent(AncEvent.OpenProfile(patient.id)) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (isWide) {
                // Wide Table Row: True Matrix View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 1. Patient Identifier
                    Column(
                        modifier = Modifier.weight(2.2f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val isWideArabic = row.displayName.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }
                            Text(
                                text = row.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = if (isWideArabic) TextAlign.End else TextAlign.Start,
                            )
                            if (row.isArchived) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                ) {
                                    Text(
                                        text = "ARCHIVED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                        if (row.ageText != null) {
                            Text(
                                text = "${row.ageText} yrs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // 2. Obstetric History
                    Box(modifier = Modifier.weight(2.0f)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        ) {
                            Text(
                                text = row.obstetricSummary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // 3. Gestational Age (Clickable EDD Pop-up)
                    Box(modifier = Modifier.weight(1.3f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showEddPopup = true }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "View EDD",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = row.gestationalAgeText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        // Anchored EDD Dropdown / Tooltip
                        DropdownMenu(
                            expanded = showEddPopup,
                            onDismissRequest = { showEddPopup = false },
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Estimated Due Date (EDD)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = row.eddText ?: "Not calculated",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (row.datingMethodText != null) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Method: ${row.datingMethodText}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // 4. Blood Group
                    Box(modifier = Modifier.weight(1.0f)) {
                        BloodGroupBadge(
                            bloodGroup = row.bloodGroup,
                            isRhNegative = row.isRhNegative,
                            isRhPositive = row.isRhPositive,
                        )
                    }

                    // 5. Risk
                    Box(modifier = Modifier.weight(1.1f)) {
                        ClinicalRiskBadge(isHighRisk = row.isHighRisk)
                    }

                    // 6. Compact Action Icons
                    Row(
                        modifier = Modifier.weight(1.4f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ActionIconButtons(patient = patient, onEvent = onEvent)
                    }
                }
            } else {
                // Compact Screen (Phones < 640dp): Highly optimized clinical matrix row
                val isArabic = row.displayName.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }
                val cardAlignment = if (isArabic) Alignment.End else Alignment.Start
                val rowArrangement = if (isArabic) Arrangement.End else Arrangement.Start

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = cardAlignment,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 1. Patient Name (Full Width, Script-Adaptive Alignment)
                    Text(
                        text = row.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // 2. Risk Tag with Age beside it (Age NOT in a color tag!)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = rowArrangement,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ClinicalRiskBadge(isHighRisk = row.isHighRisk)
                        if (row.ageText != null) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "• ${row.ageText} yrs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        if (row.isArchived) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    text = "ARCHIVED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    softWrap = false,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    // 3. Clinical Data Multi-line Tags with distinct colors & proper alignment
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isArabic) Arrangement.End else Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Tag A: Obstetric GPA & Past Deliveries (Soft Lavender/Purple tag)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TagObstetricBg,
                            border = BorderStroke(1.dp, TagObstetricBorder),
                        ) {
                            Text(
                                text = row.obstetricSummary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TagObstetricText,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            )
                        }

                        // Tag B: Gestational Age & EDD (Soft Cyan/Sky tag with tap for EDD)
                        Box {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TagGestationalAgeBg,
                                border = BorderStroke(1.dp, TagGestationalAgeBorder),
                                modifier = Modifier.clickable { showEddPopup = true },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = "Tap to view EDD",
                                        tint = TagGestationalAgeText,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = row.gestationalAgeText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TagGestationalAgeText,
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showEddPopup,
                                onDismissRequest = { showEddPopup = false },
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Estimated Due Date (EDD)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = row.eddText ?: "Not calculated",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (row.datingMethodText != null) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "Method: ${row.datingMethodText}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        // Tag C: Blood Group & Rh (Soft Blue or Rose tag)
                        BloodGroupBadge(
                            bloodGroup = row.bloodGroup,
                            isRhNegative = row.isRhNegative,
                            isRhPositive = row.isRhPositive,
                        )
                    }

                    // 4. Clinical Attention note if present
                    if (row.clinicalAttention != null) {
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(TagAttentionBg)
                                .border(1.dp, TagAttentionBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Clinical Attention",
                                tint = TagAttentionIcon,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = row.clinicalAttention,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TagAttentionText,
                                modifier = Modifier.weight(1f),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // 5. Action Buttons (Positioned below Name & Data tags)
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isArabic) Arrangement.Start else Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ActionIconButtons(patient = patient, onEvent = onEvent)
                    }
                }
            }
        }
    }
}

@Composable
private fun BloodGroupBadge(
    bloodGroup: String?,
    isRhNegative: Boolean,
    isRhPositive: Boolean,
) {
    if (bloodGroup.isNullOrBlank()) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = "—",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        return
    }

    val (bgColor, borderColor, textColor) = when {
        isRhNegative -> Triple(TagBloodNegativeBg, TagBloodNegativeBorder, TagBloodNegativeText)
        isRhPositive -> Triple(TagBloodPositiveBg, TagBloodPositiveBorder, TagBloodPositiveText)
        else -> Triple(TagBloodNeutralBg, TagBloodNeutralBorder, TagBloodNeutralText)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = "🩸 $bloodGroup",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ClinicalRiskBadge(isHighRisk: Boolean) {
    if (isHighRisk) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = TagHighRiskBg,
            border = BorderStroke(1.dp, TagHighRiskBorder),
        ) {
            Text(
                text = "⚠️ " + stringResource(R.string.high_risk),
                color = TagHighRiskText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    } else {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = TagNormalRiskBg,
            border = BorderStroke(1.dp, TagNormalRiskBorder),
        ) {
            Text(
                text = stringResource(R.string.normal_risk_filter),
                color = TagNormalRiskText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun ActionIconButtons(
    patient: DemoPatient,
    onEvent: (AncEvent) -> Unit,
) {
    // Workspace action icon button (Pen/Edit Note)
    IconButton(
        onClick = { onEvent(AncEvent.OpenWorkspace(patient.id)) },
        modifier = Modifier
            .size(36.dp)
            .testTag("workspace-${patient.caseId}"),
    ) {
        Icon(
            Icons.Default.EditNote,
            contentDescription = stringResource(R.string.workspace),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }

    // Profile action icon button (Person)
    IconButton(
        onClick = { onEvent(AncEvent.OpenProfile(patient.id)) },
        modifier = Modifier
            .size(36.dp)
            .testTag("profile-${patient.caseId}"),
    ) {
        Icon(
            Icons.Default.AccountCircle,
            contentDescription = stringResource(R.string.profile),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }

    // Edit patient action icon button
    if (patient.status != "archived") {
        IconButton(
            onClick = {
                onEvent(
                    AncEvent.StartPatientEditor(
                        patientId = patient.id,
                        newPatientId = patient.id,
                        editorSessionId = UUID.randomUUID().toString(),
                    ),
                )
            },
            modifier = Modifier
                .size(36.dp)
                .testTag("editPatient-${patient.caseId}"),
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit_patient),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    // Archive / Restore action icon button
    IconButton(
        onClick = {
            onEvent(
                AncEvent.StartStatusEditor(
                    patientId = patient.id,
                    targetStatus = if (patient.status == "archived") "active" else "archived",
                    editorSessionId = UUID.randomUUID().toString(),
                ),
            )
        },
        modifier = Modifier
            .size(36.dp)
            .testTag("statusPatient-${patient.caseId}"),
    ) {
        Icon(
            if (patient.status == "archived") Icons.Default.Unarchive else Icons.Default.Archive,
            contentDescription = stringResource(
                if (patient.status == "archived") R.string.unarchive_patient else R.string.archive_patient,
            ),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
