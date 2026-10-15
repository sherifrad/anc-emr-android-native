package com.sherifradwan.ancnative.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.sherifradwan.ancnative.security.SecuritySettingsRepository
import com.sherifradwan.ancnative.ui.report.PatientTwoPageReportDialog
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import java.time.LocalDate
import java.util.UUID

private fun String?.orNotRecorded(notRecorded: String): String = if (this.isNullOrBlank()) notRecorded else this

@Composable
fun ProfileScreen(
    state: AncState,
    route: AncRoute.Profile,
    onEvent: (AncEvent) -> Unit,
    asOfDate: LocalDate = LocalDate.now(),
) {
    val patient = state.patient(route.patientId) ?: return
    val notRecorded = stringResource(R.string.not_recorded)

    // Section expansion state
    var visitsExpanded by remember(patient.id) { mutableStateOf(true) }
    var ultrasoundExpanded by remember(patient.id) { mutableStateOf(true) }
    var labsExpanded by remember(patient.id) { mutableStateOf(true) }
    var medsExpanded by remember(patient.id) { mutableStateOf(true) }

    // Item-level expansion maps
    val expandedVisitIds = remember(patient.id) { mutableStateMapOf<String, Boolean>() }
    val expandedScanIds = remember(patient.id) { mutableStateMapOf<String, Boolean>() }
    val expandedLabDateGroups = remember(patient.id) { mutableStateMapOf<String, Boolean>() }

    // Ultrasound photo viewer state
    var viewingPhotoScan by remember(patient.id) { mutableStateOf<DemoUltrasoundScan?>(null) }
    var viewingPhotoIndex by remember(patient.id) { mutableIntStateOf(0) }

    // 2-Page Patient Report state
    var showReportDialog by remember(patient.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val securitySettings = remember(patient.id) {
        SecuritySettingsRepository.getInstance(context).settings.value
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag("profileScreen")) {
        val horizontalPadding = maxOf(16.dp, (maxWidth - 840.dp) / 2)
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("profileList"),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Navigation Bar
            // Navigation Bar - Streamlined to save vertical space
            item(key = "navBar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { onEvent(AncEvent.Back) },
                        modifier = Modifier.minimumTouchTarget(),
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.back), maxLines = 1, softWrap = false)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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
                                modifier = Modifier.minimumTouchTarget().testTag("editPatientButton"),
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_patient), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
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
                            modifier = Modifier.minimumTouchTarget().testTag("statusPatientButton"),
                        ) {
                            Icon(
                                if (patient.status == "archived") Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = stringResource(
                                    if (patient.status == "archived") R.string.unarchive_patient else R.string.archive_patient,
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // 1. Executive Hero Identity Card with Integrated Symbolic Action Buttons
            item(key = "heroCard") {
                ProfileHeroIdentityCard(
                    patient = patient,
                    notRecorded = notRecorded,
                    onOpenReport = { showReportDialog = true },
                    onOpenWorkspace = { onEvent(AncEvent.OpenWorkspace(patient.id)) },
                )
            }

            // 2. Pregnancy Dating & Gestational Age Card
            item(key = "datingCard") {
                ProfileDatingCard(patient, asOfDate, onEvent)
            }

            // 3. Obstetric History & Previous Pregnancies Card
            item(key = "obstetricCard") {
                ProfileObstetricCard(patient, notRecorded, onEvent)
            }

            // 4. Recent Visits (Collapsible / Snapshot Feed)
            item(key = "visitsSection") {
                ProfileVisitsSection(
                    patient = patient,
                    isExpanded = visitsExpanded,
                    onToggleSection = { visitsExpanded = !visitsExpanded },
                    expandedVisitIds = expandedVisitIds,
                    onToggleVisit = { id -> expandedVisitIds[id] = !(expandedVisitIds[id] ?: false) },
                    onViewPhotos = { scan, idx ->
                        viewingPhotoScan = scan
                        viewingPhotoIndex = idx
                    },
                    onEvent = onEvent,
                )
            }

            // 5. Ultrasound Examinations (Collapsible / Snapshot Feed)
            item(key = "ultrasoundSection") {
                ProfileUltrasoundSection(
                    patient = patient,
                    isExpanded = ultrasoundExpanded,
                    onToggleSection = { ultrasoundExpanded = !ultrasoundExpanded },
                    expandedScanIds = expandedScanIds,
                    onToggleScan = { id -> expandedScanIds[id] = !(expandedScanIds[id] ?: false) },
                    onViewPhotos = { scan, idx ->
                        viewingPhotoScan = scan
                        viewingPhotoIndex = idx
                    },
                    onEvent = onEvent,
                )
            }

            // 6. Laboratory Investigations (Date-Grouped Panels)
            item(key = "labsSection") {
                ProfileLaboratoriesSection(
                    patient = patient,
                    isExpanded = labsExpanded,
                    onToggleSection = { labsExpanded = !labsExpanded },
                    expandedDateGroups = expandedLabDateGroups,
                    onToggleDateGroup = { date ->
                        expandedLabDateGroups[date] = !(expandedLabDateGroups[date] ?: false)
                    },
                    onEvent = onEvent,
                )
            }

            // 7. Current Medications & Prescriptions
            item(key = "medicationsSection") {
                ProfileMedicationsSection(
                    patient = patient,
                    isExpanded = medsExpanded,
                    onToggleSection = { medsExpanded = !medsExpanded },
                    onEvent = onEvent,
                )
            }
        }

        if (viewingPhotoScan != null) {
            UltrasoundPhotoViewerDialog(
                scan = viewingPhotoScan!!,
                initialIndex = viewingPhotoIndex,
                onDismiss = { viewingPhotoScan = null },
            )
        }

        if (showReportDialog) {
            PatientTwoPageReportDialog(
                patient = patient,
                securitySettings = securitySettings,
                onDismiss = { showReportDialog = false },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 1. Executive Hero Identity Card
// ---------------------------------------------------------------------------
@Composable
private fun ProfileHeroIdentityCard(
    patient: DemoPatient,
    notRecorded: String,
    onOpenReport: () -> Unit,
    onOpenWorkspace: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Top Row: Clinic Branding Header + Integrated Symbolic Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.clinic_logo_foreground),
                        contentDescription = "Clinic Logo",
                        modifier = Modifier.size(30.dp),
                    )
                    Column {
                        Text(
                            text = "دكتور شريف رضوان كلينك",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Dr. Sherif Radwan Clinic",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Symbolic Quick Action Buttons incorporated into Identity Card
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Symbolic Print / Report Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.clickable { onOpenReport() },
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("openReportButton"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "طباعة تقرير المريضة",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "طباعة",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }

                    // Symbolic Workspace Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable { onOpenWorkspace() },
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("workspaceButton"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.open_workspace),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "مساحة العمل",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Large Initials Avatar
                val initials = patient.displayName
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercaseChar() }
                    .joinToString("")
                    .ifEmpty { "P" }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(54.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (patient.ageText != null) {
                        Text(
                            text = patient.ageText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Risk Badge
                if (patient.riskLevel.equals("high", ignoreCase = true)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.high_risk),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = androidx.compose.ui.graphics.Color(0xFFE8F5E9),
                    ) {
                        Text(
                            text = stringResource(R.string.normal_risk_filter),
                            color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            // Key Clinical Attributes Chips Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Parity Badge
                val g = patient.gravida ?: "-"
                val p = patient.parity ?: "-"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ) {
                    Text(
                        text = stringResource(R.string.parity_format, g, p),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }

                // Blood Group
                if (!patient.bloodGroupText.isNullOrBlank()) {
                    val isRhNeg = patient.bloodGroupText.contains("-")
                    val isRhPos = patient.bloodGroupText.contains("+")
                    val bgContainer = when {
                        isRhNeg -> MaterialTheme.colorScheme.errorContainer
                        isRhPos -> androidx.compose.ui.graphics.Color(0xFFE8F5E9)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val bgText = when {
                        isRhNeg -> MaterialTheme.colorScheme.onErrorContainer
                        isRhPos -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bgContainer,
                    ) {
                        Text(
                            text = "🩸 ${patient.bloodGroupText}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = bgText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }

                // Phone
                if (!patient.phone.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = "📞 ${patient.phone}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            // Clinical Attention Alert Banner
            if (!patient.clinicalAttention.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "Clinical Attention",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = patient.clinicalAttention,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. Pregnancy Dating & Gestational Age Card
// ---------------------------------------------------------------------------
@Composable
private fun ProfileDatingCard(
    patient: DemoPatient,
    asOfDate: LocalDate,
    onEvent: (AncEvent) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.dating),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(
                    onClick = {
                        onEvent(AncEvent.StartDatingEditor(patient.id, UUID.randomUUID().toString()))
                    },
                    modifier = Modifier.minimumTouchTarget(),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.edit_dating))
                }
            }

            // Integrated Gestational Banner
            GestationalBanner(dating = patient.dating, asOfDate = asOfDate)

            // Selected Dating Authority Details
            val dating = patient.dating
            val hasRecord = dating != null && (!dating.lmpDate.isNullOrBlank() || !dating.ultrasoundEdd.isNullOrBlank() || !dating.agreedEdd.isNullOrBlank() || !dating.transferDate.isNullOrBlank())
            if (dating != null && hasRecord) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Authority: ${dating.datingMethod ?: "Clinical"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        when {
                            dating.datingMethod?.contains("Ultrasound", ignoreCase = true) == true || !dating.ultrasoundEdd.isNullOrBlank() -> {
                                Text(
                                    text = "Scan EDD: ${dating.ultrasoundEdd.orEmpty()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            dating.datingMethod?.contains("LMP", ignoreCase = true) == true || !dating.lmpDate.isNullOrBlank() -> {
                                Text(
                                    text = "LMP: ${dating.lmpDate.orEmpty()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            dating.datingMethod?.contains("IVF", ignoreCase = true) == true || !dating.transferDate.isNullOrBlank() -> {
                                Text(
                                    text = "Transfer: ${dating.transferDate.orEmpty()}${if (dating.embryoAgeDays != null) " (Day-${dating.embryoAgeDays} Embryo)" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            else -> {
                                Text(
                                    text = "Agreed EDD: ${dating.agreedEdd.orEmpty()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Obstetric History & Previous Pregnancies Card
// ---------------------------------------------------------------------------
@Composable
private fun ProfileObstetricCard(
    patient: DemoPatient,
    notRecorded: String,
    onEvent: (AncEvent) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.obstetric_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(
                    onClick = {
                        onEvent(AncEvent.StartHistoriesEditor(patient.id, UUID.randomUUID().toString()))
                    },
                    modifier = Modifier.minimumTouchTarget(),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.edit_history))
                }
            }

            // Obstetric Notes (Preserves test assertions: "P3 parity detail")
            if (!patient.obstetricNotes.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "Obstetric Notes",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = patient.obstetricNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Medical, Surgical, Family, Allergy History Pills
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FactRow(R.string.medical_history, patient.medicalHistory.orNotRecorded(notRecorded))
                FactRow(R.string.surgical_history, patient.surgicalHistory.orNotRecorded(notRecorded))
                FactRow(R.string.family_history, patient.familyHistory.orNotRecorded(notRecorded))
                FactRow(R.string.allergy_history, patient.allergyHistory.orNotRecorded(notRecorded))
            }

            // Prior Pregnancies Feed
            val prevPregnancies = patient.previousPregnancies
            Text(
                text = "Prior Pregnancies (${prevPregnancies.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (prevPregnancies.isEmpty()) {
                Text(
                    text = "No previous pregnancies recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                prevPregnancies.forEach { p ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${p.dateText ?: "Prior"} · ${p.deliveryBy ?: p.birthMethod ?: p.pregnancyType ?: "Delivery"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = p.outcomeText ?: p.outcome ?: "Full term",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            val details = p.otherDetails ?: p.placeAndDoctor
                            if (!details.isNullOrBlank()) {
                                Text(
                                    text = details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4. Visits (Workspace Style with Collapsible Deep Detail)
// ---------------------------------------------------------------------------
@Composable
private fun ProfileVisitsSection(
    patient: DemoPatient,
    isExpanded: Boolean,
    onToggleSection: () -> Unit,
    expandedVisitIds: Map<String, Boolean>,
    onToggleVisit: (String) -> Unit,
    onViewPhotos: (DemoUltrasoundScan, Int) -> Unit,
    onEvent: (AncEvent) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("profileVisitsSection"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onToggleSection() },
                ) {
                    Text(
                        text = stringResource(R.string.visits),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = patient.visits.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                if (patient.status != "archived") {
                    Button(
                        onClick = {
                            onEvent(
                                AncEvent.StartVisit(
                                    patientId = patient.id,
                                    visitId = null,
                                    editorSessionId = UUID.randomUUID().toString(),
                                ),
                            )
                        },
                        modifier = Modifier.minimumTouchTarget().testTag("addVisit"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.add_visit), maxLines = 1, softWrap = false)
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (patient.visits.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_visits),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        patient.visits.forEachIndexed { index, visit ->
                            val isVisitExpanded = expandedVisitIds[visit.id] ?: false
                            val isFirst = index == 0
                            val isLast = index == patient.visits.lastIndex
                            TimelineSpineItem(
                                isFirst = isFirst,
                                isLast = isLast,
                            ) {
                                ProfileVisitCard(
                                    patient = patient,
                                    visit = visit,
                                    isExpanded = isVisitExpanded,
                                    canEdit = patient.status != "archived",
                                    onToggle = { onToggleVisit(visit.id) },
                                    onEdit = {
                                        onEvent(
                                            AncEvent.StartVisit(
                                                patientId = patient.id,
                                                visitId = visit.id,
                                                editorSessionId = UUID.randomUUID().toString(),
                                            ),
                                        )
                                    },
                                    onViewPhotos = onViewPhotos,
                                    onEvent = onEvent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileVisitCard(
    patient: DemoPatient,
    visit: DemoVisit,
    isExpanded: Boolean,
    canEdit: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onViewPhotos: (DemoUltrasoundScan, Int) -> Unit,
    onEvent: (AncEvent) -> Unit,
) {
    val gaAtVisit = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, visit.date)?.displayText

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("visit-${visit.id}")
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header: Date, GA Chip, Expand Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = visit.date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!gaAtVisit.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = gaAtVisit,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (visit.linkedUltrasoundId != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "Ultrasound 📷",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse visit" else "Expand visit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (!isExpanded) {
                // Collapsed Snapshot: Vital Metrics & Findings preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!visit.bloodPressure.isNullOrBlank()) {
                        Text(
                            text = "BP: ${visit.bloodPressure}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (!visit.weight.isNullOrBlank()) {
                        Text(
                            text = "Weight: ${visit.weight} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                if (!visit.findings.isNullOrBlank()) {
                    Text(
                        text = visit.findings,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            } else {
                // Expanded Details: The EXACT Workspace style layout with FactRows!
                FactRow(R.string.blood_pressure, visit.bloodPressure)
                FactRow(R.string.weight, visit.weight)
                FactRow(R.string.findings, visit.findings)
                FactRow(R.string.notes, visit.notes)

                if (visit.isSyntheticEdit) {
                    Text(
                        stringResource(R.string.synthetic_edit),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                // Inline linked ultrasound scan card (Exact Workspace style!)
                if (visit.linkedUltrasoundId != null) {
                    val linkedScan = patient.ultrasoundScans.firstOrNull { it.id == visit.linkedUltrasoundId }
                    if (linkedScan != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("inlineLinkedScan-${visit.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.linked_ultrasound),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    Text(
                                        text = linkedScan.date,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                                val scanGa = when {
                                    linkedScan.gestationalAgeWeeks != null && linkedScan.gestationalAgeDays != null ->
                                        "${linkedScan.gestationalAgeWeeks}w ${linkedScan.gestationalAgeDays}d"
                                    linkedScan.gestationalAgeWeeks != null -> "${linkedScan.gestationalAgeWeeks}w"
                                    else -> null
                                }
                                if (scanGa != null) {
                                    Text("GA: $scanGa" + (linkedScan.presentation?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall)
                                }
                                if (linkedScan.fetalHeartRate != null || linkedScan.estimatedFetalWeight != null) {
                                    Text(
                                        "FHR: ${linkedScan.fetalHeartRate ?: "-"} bpm · EFW: ${linkedScan.estimatedFetalWeight ?: "-"} g",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (linkedScan.imageUris.isNotEmpty()) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(vertical = 2.dp),
                                    ) {
                                        Text(
                                            stringResource(R.string.photos_count, linkedScan.imageUris.size) + " 📷",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            linkedScan.imageUris.forEachIndexed { idx, _ ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    modifier = Modifier
                                                        .testTag("visitPhotoChip-${linkedScan.id}-$idx")
                                                        .clickable { onViewPhotos(linkedScan, idx) },
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    ) {
                                                        Text(
                                                            text = "📷 Photo ${idx + 1}",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TextButton(
                                        onClick = { onEvent(AncEvent.OpenWorkspace(patient.id)) },
                                        modifier = Modifier.testTag("openLinkedScan-${linkedScan.id}"),
                                    ) {
                                        Text(stringResource(R.string.view_linked_ultrasound))
                                    }
                                    if (canEdit) {
                                        TextButton(
                                            onClick = {
                                                onEvent(
                                                    AncEvent.StartUltrasoundEditor(
                                                        patientId = patient.id,
                                                        scanId = linkedScan.id,
                                                        newScanId = linkedScan.id,
                                                        editorSessionId = UUID.randomUUID().toString(),
                                                    ),
                                                )
                                            },
                                            modifier = Modifier.testTag("editLinkedScan-${linkedScan.id}"),
                                        ) {
                                            Text(stringResource(R.string.edit_ultrasound))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (canEdit) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumTouchTarget().testTag("editVisit-${visit.id}"),
                    ) {
                        Text(stringResource(R.string.edit_visit), maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. Ultrasound Examinations (Collapsible / Snapshot Feed)
// ---------------------------------------------------------------------------
@Composable
private fun ProfileUltrasoundSection(
    patient: DemoPatient,
    isExpanded: Boolean,
    onToggleSection: () -> Unit,
    expandedScanIds: Map<String, Boolean>,
    onToggleScan: (String) -> Unit,
    onViewPhotos: (DemoUltrasoundScan, Int) -> Unit,
    onEvent: (AncEvent) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onToggleSection() },
                ) {
                    Text(
                        text = stringResource(R.string.recent_scans),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = patient.ultrasoundScans.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Button(
                    onClick = {
                        onEvent(
                            AncEvent.StartUltrasoundEditor(
                                patientId = patient.id,
                                scanId = null,
                                newScanId = UUID.randomUUID().toString(),
                                editorSessionId = UUID.randomUUID().toString(),
                            ),
                        )
                    },
                    modifier = Modifier.minimumTouchTarget(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.add_ultrasound), maxLines = 1, softWrap = false)
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (patient.ultrasoundScans.isEmpty()) {
                        Text(
                            text = "No ultrasound scans recorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        patient.ultrasoundScans.forEach { scan ->
                            val isScanExpanded = expandedScanIds[scan.id] ?: false
                            ProfileScanSnapshotCard(
                                scan = scan,
                                isExpanded = isScanExpanded,
                                onToggle = { onToggleScan(scan.id) },
                                onViewPhotos = onViewPhotos,
                                onEdit = {
                                    onEvent(
                                        AncEvent.StartUltrasoundEditor(
                                            patientId = patient.id,
                                            scanId = scan.id,
                                            newScanId = scan.id,
                                            editorSessionId = UUID.randomUUID().toString(),
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScanSnapshotCard(
    scan: DemoUltrasoundScan,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onViewPhotos: (DemoUltrasoundScan, Int) -> Unit,
    onEdit: () -> Unit,
) {
    val gaText = if (scan.gestationalAgeWeeks != null) "${scan.gestationalAgeWeeks}+${scan.gestationalAgeDays ?: 0}w" else null

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Line 1: Date + GA + Presentation + Photo count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = scan.date,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (gaText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = gaText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (!scan.presentation.isNullOrBlank()) {
                        Text(
                            text = "· ${scan.presentation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (scan.imageUris.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { onViewPhotos(scan, 0) },
                        ) {
                            Text(
                                text = "📷 ${scan.imageUris.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = if (isExpanded) "Less" else "Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Line 2: Primary Biometrics (FHR & EFW)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!scan.fetalHeartRate.isNullOrBlank()) {
                    Text(
                        text = "💓 FHR: ${scan.fetalHeartRate} bpm",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (!scan.estimatedFetalWeight.isNullOrBlank()) {
                    Text(
                        text = "⚖️ EFW: ${scan.estimatedFetalWeight} g",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Expanded Biometric Grid, Anatomical Details & Clickable Photos
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (!scan.biometryBpd.isNullOrBlank()) Text("BPD: ${scan.biometryBpd} mm", style = MaterialTheme.typography.bodySmall)
                        if (!scan.biometryHc.isNullOrBlank()) Text("HC: ${scan.biometryHc} mm", style = MaterialTheme.typography.bodySmall)
                        if (!scan.biometryAc.isNullOrBlank()) Text("AC: ${scan.biometryAc} mm", style = MaterialTheme.typography.bodySmall)
                        if (!scan.biometryFl.isNullOrBlank()) Text("FL: ${scan.biometryFl} mm", style = MaterialTheme.typography.bodySmall)
                    }

                    if (!scan.placentalSite.isNullOrBlank()) {
                        Text(
                            text = "Placenta: ${scan.placentalSite}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (!scan.amnioticFluid.isNullOrBlank()) {
                        Text(
                            text = "Amniotic Fluid: ${scan.amnioticFluid}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (!scan.impression.isNullOrBlank()) {
                        Text(
                            text = "Impression: ${scan.impression}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Interactive scan photos
                    if (scan.imageUris.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Scan Photos (${scan.imageUris.size}) 📷",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                scan.imageUris.forEachIndexed { idx, _ ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .testTag("scanPhotoChip-${scan.id}-$idx")
                                            .clickable { onViewPhotos(scan, idx) },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = "📷 Photo ${idx + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.padding(top = 4.dp).minimumTouchTarget(),
                    ) {
                        Text("Edit Scan", maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 6. Laboratory Investigations (Date-Grouped Panels)
// ---------------------------------------------------------------------------
@Composable
private fun ProfileLaboratoriesSection(
    patient: DemoPatient,
    isExpanded: Boolean,
    onToggleSection: () -> Unit,
    expandedDateGroups: Map<String, Boolean>,
    onToggleDateGroup: (String) -> Unit,
    onEvent: (AncEvent) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("profileLabsSection"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onToggleSection() },
                ) {
                    Text(
                        text = stringResource(R.string.laboratory),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = patient.labInvestigations.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                if (patient.status != "archived") {
                    Button(
                        onClick = {
                            onEvent(
                                AncEvent.StartLabEditor(
                                    patientId = patient.id,
                                    labId = null,
                                    newLabId = UUID.randomUUID().toString(),
                                    editorSessionId = UUID.randomUUID().toString(),
                                ),
                            )
                        },
                        modifier = Modifier.minimumTouchTarget().testTag("addLab"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.add_lab), maxLines = 1, softWrap = false)
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (patient.labInvestigations.isEmpty()) {
                        Text(
                            text = "No laboratory investigations recorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        val groupedByDate = remember(patient.labInvestigations) {
                            patient.labInvestigations
                                .groupBy { it.date }
                                .toList()
                                .sortedByDescending { it.first }
                        }

                        groupedByDate.forEach { (date, labsOnDate) ->
                            val isDateExpanded = expandedDateGroups[date] ?: false
                            ProfileDateGroupedLabCard(
                                date = date,
                                labs = labsOnDate,
                                isExpanded = isDateExpanded,
                                canEdit = patient.status != "archived",
                                onToggle = { onToggleDateGroup(date) },
                                onEditLab = { lab ->
                                    onEvent(
                                        AncEvent.StartLabEditor(
                                            patientId = patient.id,
                                            labId = lab.id,
                                            newLabId = lab.id,
                                            editorSessionId = UUID.randomUUID().toString(),
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDateGroupedLabCard(
    date: String,
    labs: List<DemoLabInvestigation>,
    isExpanded: Boolean,
    canEdit: Boolean,
    onToggle: () -> Unit,
    onEditLab: (DemoLabInvestigation) -> Unit,
) {
    val hasAbnormal = labs.any { it.isAbnormal }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Date Group Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "${labs.size} ${if (labs.size == 1) "test" else "tests"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (hasAbnormal) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                text = "ABNORMAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse date report" else "Expand date report",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Expanded List of Tests on this Date
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Internal Table Header Row
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "TEST NAME",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(2.0f),
                            )
                            Text(
                                text = "RESULT / UNIT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1.3f),
                            )
                            Text(
                                text = "REFERENCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1.3f),
                            )
                            if (canEdit) {
                                Spacer(Modifier.width(36.dp))
                            }
                        }
                    }

                    // Table Rows
                    labs.forEachIndexed { index, lab ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lab-${lab.id}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Column 1: Test Name (wraps smoothly without truncating or squashing) + Category badge below
                                Column(
                                    modifier = Modifier.weight(2.0f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = lab.testName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    if (lab.testCategory.isNotBlank()) {
                                        Text(
                                            text = lab.testCategory,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        )
                                    }
                                }

                                // Column 2: Result & Unit (Bold, tinted red if abnormal + ABNORMAL badge)
                                Column(
                                    modifier = Modifier.weight(1.3f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = "${lab.result}${if (!lab.unit.isNullOrBlank()) " ${lab.unit}" else ""}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (lab.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (lab.isAbnormal) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.errorContainer,
                                        ) {
                                            Text(
                                                text = "ABNORMAL",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            )
                                        }
                                    }
                                }

                                // Column 3: Reference Range in fixed spot
                                Text(
                                    text = if (!lab.referenceRange.isNullOrBlank()) lab.referenceRange else "—",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1.3f),
                                )

                                // Action: Edit lab button if canEdit
                                if (canEdit) {
                                    IconButton(
                                        onClick = { onEditLab(lab) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("editLab-${lab.id}"),
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit_lab),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            // Beneath the row: Clinical note ONLY IF PRESENT!
                            if (!lab.notes.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = "📝 Note: ${lab.notes}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            if (index < labs.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 7. Current Medications & Prescriptions
// ---------------------------------------------------------------------------
@Composable
private fun ProfileMedicationsSection(
    patient: DemoPatient,
    isExpanded: Boolean,
    onToggleSection: () -> Unit,
    onEvent: (AncEvent) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onToggleSection() },
                ) {
                    Text(
                        text = stringResource(R.string.medications),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = patient.medications.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Button(
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
                    modifier = Modifier.minimumTouchTarget(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.add_medication), maxLines = 1, softWrap = false)
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (patient.medications.isEmpty()) {
                        Text(
                            text = "No medications recorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        val activeMeds = patient.medications.filter { !it.status.equals("Stopped", ignoreCase = true) }
                        val stoppedMeds = patient.medications.filter { it.status.equals("Stopped", ignoreCase = true) }

                        if (activeMeds.isNotEmpty()) {
                            Text(
                                text = "Currently Taking",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            activeMeds.forEach { med ->
                                ProfileMedicationCard(med) {
                                    onEvent(
                                        AncEvent.StartMedicationEditor(
                                            patientId = patient.id,
                                            medicationId = med.id,
                                            newMedicationId = med.id,
                                            editorSessionId = UUID.randomUUID().toString(),
                                        ),
                                    )
                                }
                            }
                        }

                        if (stoppedMeds.isNotEmpty()) {
                            Text(
                                text = "Stopped / Past",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            stoppedMeds.forEach { med ->
                                ProfileMedicationCard(med) {
                                    onEvent(
                                        AncEvent.StartMedicationEditor(
                                            patientId = patient.id,
                                            medicationId = med.id,
                                            newMedicationId = med.id,
                                            editorSessionId = UUID.randomUUID().toString(),
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMedicationCard(med: DemoMedicationRecord, onEdit: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = med.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${med.dosage ?: ""} · ${med.frequency ?: "Daily"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!med.startDate.isNullOrBlank()) {
                    Text(
                        text = "Started: ${med.startDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (med.status.equals("Stopped", ignoreCase = true))
                    MaterialTheme.colorScheme.surfaceContainerHigh
                else
                    MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = med.status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (med.status.equals("Stopped", ignoreCase = true))
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}
