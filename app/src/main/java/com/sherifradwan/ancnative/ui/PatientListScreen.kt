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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import java.time.LocalDate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.SevereBpAttention
import com.sherifradwan.ancnative.model.datingSummaryOf
import com.sherifradwan.ancnative.model.formatDmy
import com.sherifradwan.ancnative.model.previousDeliverySummary
import com.sherifradwan.ancnative.model.severeBpAttention
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.isRegistrationIncomplete
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.SavedNotice
import com.sherifradwan.ancnative.ui.components.RecordFilterChip
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.patientdirectory.PatientActionsModalSheet
import com.sherifradwan.ancnative.ui.patientdirectory.PatientDirectoryCard
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
    val isDatingDiscordant: Boolean = false,
    val bloodGroup: String?,
    val isRhNegative: Boolean,
    val isRhPositive: Boolean,
    val isHighRisk: Boolean,
    val riskTier: PatientRiskTier,
    val clinicalAttention: String?,
    val latestVitalsBp: String? = null,
    val isSevereBp: Boolean = false,
    val isLateOrPostTerm: Boolean = false,
    val isDelivered: Boolean = false,
    val deliveryDate: String? = null,
    val isIncomplete: Boolean = false,
)

fun projectDashboardRow(patient: DemoPatient): PatientDashboardRowProjection {
    val g = patient.gravida ?: 1L
    val p = patient.parity ?: 0L

    val prevSummary = previousDeliverySummary(patient)
    val obstetricFormatted = "G$g P$p · $prevSummary"

    val gaResult = patient.dating?.calculateGestationalAgeResult()
    val gaFormatted = gaResult?.shortWeeksText ?: "-"

    val datingSummary = datingSummaryOf(patient, LocalDate.now())
    val edd = datingSummary.edd?.let(::formatDmy)

    val bg = patient.bloodGroupText?.trim()
    val isRhNeg = isRhNegative(bg)
    val isRhPos = isRhPositive(bg)
    val isHigh = isHighRisk(patient.riskLevel)
    val tier = parseRiskTier(patient.riskLevel)

    val latestVisit = patient.visits.maxByOrNull { it.date }
    val latestBp = latestVisit?.bloodPressure?.trim()?.takeIf { it.isNotBlank() }
    val isSevere = severeBpAttention(patient.visits) != null
    val isLateOrPost = !patient.delivered && gaResult != null && gaResult.weeks >= 41

    return PatientDashboardRowProjection(
        id = patient.id,
        caseId = patient.caseId,
        displayName = patient.displayName,
        ageText = patient.ageText,
        isArchived = patient.status == "archived",
        obstetricSummary = obstetricFormatted,
        gestationalAgeText = gaFormatted,
        eddText = edd,
        isDatingDiscordant = datingSummary.discordance != null,
        bloodGroup = bg,
        isRhNegative = isRhNeg,
        isRhPositive = isRhPos,
        isHighRisk = isHigh,
        riskTier = tier,
        clinicalAttention = patient.clinicalAttention?.takeIf { it.isNotBlank() },
        latestVitalsBp = latestBp,
        isSevereBp = isSevere,
        isLateOrPostTerm = isLateOrPost,
        isDelivered = patient.delivered,
        deliveryDate = patient.deliveryDate,
        isIncomplete = isRegistrationIncomplete(patient),
    )
}

/**
 * The rows of the main dashboard. Delivered patients leave the main list and show under the Delivered filter; a search
 * still finds them so nobody is lost.
 */
fun dashboardPatients(visible: List<DemoPatient>, filter: String, searching: Boolean): List<DemoPatient> = when {
    filter == "archived" -> visible.filter { it.status == "archived" }
    filter == "delivered" -> visible.filter { it.delivered && it.status != "archived" }
    filter == "all" && searching -> visible.filter { it.status != "archived" }
    else -> visible.filter { !it.delivered && it.status != "archived" }
}

@Composable
private fun dashboardSortLabel(sort: DashboardSort): String = when (sort) {
    DashboardSort.NEWEST_ADDED -> stringResource(R.string.dashboard_sort_newest)
    DashboardSort.RECENTLY_SEEN -> stringResource(R.string.dashboard_sort_recent)
    DashboardSort.NAME -> stringResource(R.string.dashboard_sort_name)
    DashboardSort.DUE_DATE -> stringResource(R.string.dashboard_sort_due)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(state: AncState, onEvent: (AncEvent) -> Unit) {
    val visibleList = remember(state.patients, state.searchQuery) { state.visiblePatients() }
    var riskFilter by rememberSaveable { mutableStateOf("all") }
    var currentSort by rememberSaveable { mutableStateOf(DashboardSort.NEWEST_ADDED) }
    var sheetPatient by remember { mutableStateOf<DemoPatient?>(null) }

    val searching = state.searchQuery.isNotBlank()
    val filteredList = remember(visibleList, riskFilter, searching) {
        when (riskFilter) {
            "archived" -> dashboardPatients(visibleList, "archived", searching)
            "delivered" -> dashboardPatients(visibleList, "delivered", searching)
            "high" -> dashboardPatients(visibleList, "high", searching).filter { isHighRisk(it.riskLevel) }
            "medium" -> dashboardPatients(visibleList, "medium", searching).filter { parseRiskTier(it.riskLevel) == PatientRiskTier.MEDIUM }
            "low" -> dashboardPatients(visibleList, "low", searching).filter { parseRiskTier(it.riskLevel) == PatientRiskTier.LOW }
            else -> dashboardPatients(visibleList, "all", searching)
        }
    }
    val sortedList = remember(filteredList, currentSort) {
        sortDashboardPatients(filteredList, currentSort)
    }

    val archivedCount = remember(visibleList) { visibleList.count { it.status == "archived" } }
    val deliveredCount = remember(visibleList) { visibleList.count { it.delivered && it.status != "archived" } }
    val activePatients = remember(visibleList) { visibleList.filter { !it.delivered && it.status != "archived" } }
    val allCount = if (searching) visibleList.count { it.status != "archived" } else activePatients.size
    val highRiskCount = remember(activePatients) { activePatients.count { isHighRisk(it.riskLevel) } }
    val mediumRiskCount = remember(activePatients) { activePatients.count { parseRiskTier(it.riskLevel) == PatientRiskTier.MEDIUM } }
    val lowRiskCount = remember(activePatients) { activePatients.count { parseRiskTier(it.riskLevel) == PatientRiskTier.LOW } }

    val activeCount = remember(state.patients) { state.patients.count { it.status != "archived" && !it.delivered } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = maxOf(16.dp, (maxWidth - 880.dp) / 2)

        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("patientListTable"),
            contentPadding = PaddingValues(start = horizontalPadding, top = 12.dp, end = horizontalPadding, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Dashboard Header with Active Count & New Patient Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = stringResource(R.string.patients),
                            style = RecordType.HeaderName,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = stringResource(R.string.active_patients_count, activeCount),
                            style = RecordType.Secondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
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
                        shape = RecordShapes.Small,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("newPatient"),
                    ) {
                        Text(
                            text = stringResource(R.string.new_patient),
                            style = RecordType.Action,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            // Save / Archive Notices
            if (state.savedNotice && state.notice in setOf(SavedNotice.ARCHIVED, SavedNotice.UNARCHIVED)) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RecordShapes.Large,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            text = stringResource(
                                if (state.notice == SavedNotice.ARCHIVED) R.string.patient_archived_notice
                                else R.string.patient_unarchived_notice,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = RecordType.Secondary,
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
                            IconButton(
                                onClick = { onEvent(AncEvent.ClearSearch) },
                                modifier = Modifier.minimumTouchTarget(),
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    },
                    shape = RecordShapes.Small,
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
                    RecordFilterChip(
                        selected = riskFilter == "all",
                        label = "${stringResource(R.string.all_filter)} ($allCount)",
                        onClick = { riskFilter = "all" },
                        tag = "riskFilter-all",
                    )
                    RecordFilterChip(
                        selected = riskFilter == "high",
                        label = "${stringResource(R.string.high_risk_filter)} ($highRiskCount)",
                        onClick = { riskFilter = "high" },
                        tag = "riskFilter-high",
                    )
                    RecordFilterChip(
                        selected = riskFilter == "medium",
                        label = "${stringResource(R.string.medium_risk_filter)} ($mediumRiskCount)",
                        onClick = { riskFilter = "medium" },
                        tag = "riskFilter-medium",
                    )
                    RecordFilterChip(
                        selected = riskFilter == "low",
                        label = "${stringResource(R.string.low_risk_filter)} ($lowRiskCount)",
                        onClick = { riskFilter = "low" },
                        tag = "riskFilter-low",
                    )
                    RecordFilterChip(
                        selected = riskFilter == "delivered",
                        label = "${stringResource(R.string.patients_filter_delivered)} ($deliveredCount)",
                        onClick = { riskFilter = "delivered" },
                        tag = "riskFilter-delivered",
                    )
                    RecordFilterChip(
                        selected = riskFilter == "archived",
                        label = "${stringResource(R.string.patients_filter_archived)} ($archivedCount)",
                        onClick = { riskFilter = "archived" },
                        tag = "riskFilter-archived",
                    )
                }
            }

            item {
                var sortMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(
                        onClick = { sortMenuExpanded = true },
                        shape = RecordShapes.Small,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .minimumTouchTarget()
                            .heightIn(min = 48.dp)
                            .testTag("dashboardSortButton"),
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_sort_format, dashboardSortLabel(currentSort)),
                            style = RecordType.Action,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dashboard_sort_newest)) },
                            onClick = {
                                currentSort = DashboardSort.NEWEST_ADDED
                                sortMenuExpanded = false
                            },
                            modifier = Modifier.testTag("dashboardSort-newest"),
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dashboard_sort_recent)) },
                            onClick = {
                                currentSort = DashboardSort.RECENTLY_SEEN
                                sortMenuExpanded = false
                            },
                            modifier = Modifier.testTag("dashboardSort-recent"),
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dashboard_sort_name)) },
                            onClick = {
                                currentSort = DashboardSort.NAME
                                sortMenuExpanded = false
                            },
                            modifier = Modifier.testTag("dashboardSort-name"),
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dashboard_sort_due)) },
                            onClick = {
                                currentSort = DashboardSort.DUE_DATE
                                sortMenuExpanded = false
                            },
                            modifier = Modifier.testTag("dashboardSort-due"),
                        )
                    }
                }
            }

            // Patient List Items
            if (sortedList.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RecordShapes.Large,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(
                                    if (state.searchQuery.isNotBlank()) R.string.no_matching_patients
                                    else R.string.no_patients_in_category,
                                ),
                                style = RecordType.FieldValue,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                items(sortedList, key = { it.id }) { patient ->
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
        PatientActionsModalSheet(
            patient = p,
            sheetState = sheetState,
            onDismissRequest = { sheetPatient = null },
            onEvent = onEvent,
        )
    }
}

private val RecordType.HeaderName: TextStyle
    get() = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)


