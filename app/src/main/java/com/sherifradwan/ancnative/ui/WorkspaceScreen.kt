package com.sherifradwan.ancnative.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.sherifradwan.ancnative.model.LongitudinalTimelineProjection
import com.sherifradwan.ancnative.model.LongitudinalTimelineEvent
import com.sherifradwan.ancnative.model.TimelineEventKind
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import com.sherifradwan.ancnative.model.previousDeliverySummary
import androidx.compose.runtime.collectAsState
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_DISCONTINUED
import com.sherifradwan.ancnative.model.codex.ClinicalCodexRepository
import com.sherifradwan.ancnative.model.codex.CustomChoiceList
import com.sherifradwan.ancnative.model.codex.CustomChoiceValidationResult
import com.sherifradwan.ancnative.model.codex.validateCustomChoice
import com.sherifradwan.ancnative.state.MedicationDraftField
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.storage.AttachmentCategories
import com.sherifradwan.ancnative.storage.AttachmentSyncManager
import com.sherifradwan.ancnative.storage.BoundedBitmap
import com.sherifradwan.ancnative.state.AttachmentError
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import com.sherifradwan.ancnative.state.PatientDraftField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.sherifradwan.ancnative.security.SecuritySettingsRepository
import com.sherifradwan.ancnative.ui.report.PatientTwoPageReportDialog
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.sherifradwan.ancnative.model.ClinicalSummaryGenerator
import android.widget.Toast
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.medicationInstructions
import com.sherifradwan.ancnative.model.medicationStatusLabel
import com.sherifradwan.ancnative.model.isOngoingMedication
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.SevereBpAttention
import com.sherifradwan.ancnative.model.severeBpAttention
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.state.stringRes
import com.sherifradwan.ancnative.state.undoRunning
import com.sherifradwan.ancnative.storage.DeletableType
import java.time.LocalDate
import java.util.UUID

@Composable
fun WorkspaceScreen(
    state: AncState,
    route: AncRoute.Workspace,
    onEvent: (AncEvent) -> Unit,
    onStartVisit: (String, String?) -> Unit,
    asOfDate: LocalDate = LocalDate.now(),
) {
    val patient = state.patient(route.patientId) ?: return
    val currentTab = WorkspaceBedsideTab.fromSection(route.section)
    val pagerState = rememberPagerState(
        initialPage = currentTab.ordinal,
        pageCount = { WorkspaceBedsideTab.entries.size },
    )
    val coroutineScope = rememberCoroutineScope()

    // Durable external route -> Pager synchronization
    LaunchedEffect(route.section) {
        val targetTab = WorkspaceBedsideTab.fromSection(route.section)
        if (pagerState.currentPage != targetTab.ordinal) {
            pagerState.animateScrollToPage(targetTab.ordinal)
        }
    }

    // Pager swipe gesture -> AncState synchronization (loop-free via settledPage)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { settledPage ->
                val settledTab = WorkspaceBedsideTab.entries[settledPage]
                val currentTabNow = WorkspaceBedsideTab.fromSection(route.section)
                if (settledTab != currentTabNow) {
                    onEvent(AncEvent.SelectSection(settledTab.defaultSection))
                }
            }
    }

    var viewingPhotoScan by remember(patient.id) { mutableStateOf<DemoUltrasoundScan?>(null) }
    var viewingPhotoIndex by remember(patient.id) { mutableIntStateOf(0) }
    var viewingAttachment by remember(patient.id) { mutableStateOf<DemoAttachment?>(null) }
    var showAddAttachmentDialog by remember(patient.id) { mutableStateOf(false) }

    var showReportDialog by remember(patient.id) { mutableStateOf(false) }
    var showAttentionDialog by remember(patient.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val securitySettings = remember(patient.id) {
        SecuritySettingsRepository.getInstance(context).settings.value
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag("workspaceScreen")) {
        val horizontalPadding = maxOf(16.dp, (maxWidth - 840.dp) / 2)
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WorkspaceTopBar(
                    patient = patient,
                    asOfDate = asOfDate,
                    onEvent = onEvent,
                    onOpenReport = { showReportDialog = true },
                    onOpenAttentionDialog = { showAttentionDialog = true },
                )
                val severeBp = remember(patient.visits) { severeBpAttention(patient.visits) }
                if (!patient.clinicalAttention.isNullOrBlank() || severeBp != null) {
                    ClinicalAttentionBanner(
                        attention = patient.clinicalAttention,
                        severeBp = severeBp,
                        onEdit = { showAttentionDialog = true },
                    )
                }
            }

            val timelineEvents = remember(patient) {
                LongitudinalTimelineProjection.build(patient)
            }

            var encounterFilter by rememberSaveable { mutableStateOf(EncounterFilter.ALL) }

            WorkspaceSectionTabBar(
                selectedTab = WorkspaceBedsideTab.entries[pagerState.currentPage],
                onSelectTab = { tab ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(tab.ordinal)
                    }
                    if (tab != currentTab) {
                        onEvent(AncEvent.SelectSection(tab.defaultSection))
                    }
                },
                patient = patient,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
            )

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                val pageTab = WorkspaceBedsideTab.entries[page]
                val isCurrent = page == pagerState.currentPage
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(if (isCurrent) "workspaceList" else "workspaceList-${pageTab.name}"),
                    contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, top = 14.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    workspaceBedsideContentItems(
                        patient = patient,
                        tab = pageTab,
                        timelineEvents = timelineEvents,
                        state = state,
                        onEvent = onEvent,
                        onStartVisit = onStartVisit,
                        onViewPhotos = { scan, idx ->
                            viewingPhotoScan = scan
                            viewingPhotoIndex = idx
                        },
                        onAddAttachment = { showAddAttachmentDialog = true },
                        onViewAttachment = { viewingAttachment = it },
                        encounterFilter = encounterFilter,
                        onSelectEncounterFilter = { encounterFilter = it },
                    )
                }
            }
        }

        if (viewingPhotoScan != null) {
            UltrasoundPhotoViewerDialog(
                patientId = patient.id,
                scan = viewingPhotoScan!!,
                initialIndex = viewingPhotoIndex,
                onDismiss = { viewingPhotoScan = null },
            )
        }

        // The viewer closes only when its record is really gone: a cancelled confirmation returns to the image.
        LaunchedEffect(patient.attachments) {
            val open = viewingAttachment ?: return@LaunchedEffect
            if (patient.attachments.none { it.id == open.id }) viewingAttachment = null
        }
        if (viewingAttachment != null) {
            ClinicalAttachmentViewerDialog(
                patient = patient,
                attachment = viewingAttachment!!,
                canDelete = patient.status != "archived",
                onDismiss = { viewingAttachment = null },
                onDelete = {
                    val att = viewingAttachment ?: return@ClinicalAttachmentViewerDialog
                    onEvent(
                        AncEvent.RequestDelete(
                            patientId = patient.id,
                            type = DeletableType.ATTACHMENT,
                            recordId = att.id,
                        ),
                    )
                },
            )
        }

        if (showAddAttachmentDialog) {
            AddAttachmentDialog(
                patient = patient,
                onDismiss = { showAddAttachmentDialog = false },
                onSave = { uri, category, title, preset ->
                    onEvent(
                        AncEvent.AddAttachment(
                            patientId = patient.id,
                            sourceUri = uri,
                            category = category,
                            title = title,
                            preset = preset,
                        ),
                    )
                    showAddAttachmentDialog = false
                },
            )
        }

        if (showReportDialog) {
            PatientTwoPageReportDialog(
                patient = patient,
                securitySettings = securitySettings,
                onDismiss = { showReportDialog = false },
            )
        }

        if (showAttentionDialog) {
            ClinicalAttentionDialog(
                currentAttention = patient.clinicalAttention.orEmpty(),
                onDismiss = { showAttentionDialog = false },
                onSave = { updatedAttention ->
                    onEvent(
                        AncEvent.StartPatientEditor(
                            patientId = patient.id,
                            newPatientId = patient.id,
                            editorSessionId = UUID.randomUUID().toString(),
                        ),
                    )
                    onEvent(AncEvent.PatientDraftChanged(PatientDraftField.CLINICAL_ATTENTION, updatedAttention))
                    onEvent(AncEvent.SubmitPatientProfile)
                    showAttentionDialog = false
                },
            )
        }
    }
}

@Composable
private fun WorkspaceTopBar(
    patient: DemoPatient,
    asOfDate: LocalDate,
    onEvent: (AncEvent) -> Unit,
    onOpenReport: () -> Unit,
    onOpenAttentionDialog: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Row 1: Back + Patient Name & Age & GPA + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    IconButton(
                        onClick = { onEvent(AncEvent.Back) },
                        modifier = Modifier.minimumTouchTarget(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = patient.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.semantics { heading() },
                            )
                            if (formatAgeYears(patient.ageText) != null) {
                                PatientAgeBadge(ageText = patient.ageText)
                            }
                        }
                        val caseDisplay = patient.caseId.ifBlank { patient.id }
                        Text(
                            text = caseDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    IconButton(
                        onClick = onOpenAttentionDialog,
                        modifier = Modifier.minimumTouchTarget().testTag("workspaceAttentionButton"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Clinical Attention & Alerts",
                            tint = if (patient.clinicalAttention.isNullOrBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    IconButton(
                        onClick = onOpenReport,
                        modifier = Modifier.minimumTouchTarget().testTag("workspaceReportButton"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = stringResource(R.string.print_patient_report),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    IconButton(
                        onClick = { onEvent(AncEvent.OpenProfile(patient.id)) },
                        modifier = Modifier.minimumTouchTarget().testTag("workspaceProfile"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = stringResource(R.string.profile),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // Row 2: Metadata Strip (Risk Badge, GPA, Blood Group, GA, EDD, Allergy)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 1. Risk Level Badge
                ClinicalRiskBadge(isHighRisk = isHighRisk(patient.riskLevel))

                // 2. Parity Pill (G/P/A)
                val g = patient.gravida ?: 1L
                val p = patient.parity ?: 0L
                val a = patient.abortion ?: 0L
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "G$g P$p A$a",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }

                // 2. Blood Group
                if (!patient.bloodGroupText.isNullOrBlank()) {
                    val isRhNeg = isRhNegative(patient.bloodGroupText)
                    val isRhPos = isRhPositive(patient.bloodGroupText)
                    val bgSurface = when {
                        isRhNeg -> if (isDark) TagBloodNegativeDarkBg else TagBloodNegativeBg
                        isRhPos -> if (isDark) TagBloodPositiveDarkBg else TagBloodPositiveBg
                        else -> if (isDark) TagBloodNeutralDarkBg else TagBloodNeutralBg
                    }
                    val bgText = when {
                        isRhNeg -> if (isDark) TagBloodNegativeDarkText else TagBloodNegativeText
                        isRhPos -> if (isDark) TagBloodPositiveDarkText else TagBloodPositiveText
                        else -> if (isDark) TagBloodNeutralDarkText else TagBloodNeutralText
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = bgSurface) {
                        Text(
                            text = formatBloodGroup(patient.bloodGroupText).orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = bgText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                // 3. Gestational Age
                val gaResult = patient.dating?.calculateGestationalAgeResult()
                val ga = patient.dating?.calculateGestationalAge(asOfDate)
                val gaText = gaResult?.shortWeeksText ?: ga?.toString()
                if (gaText != null) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(
                            text = "GA $gaText",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                // 4. EDD
                val edd = patient.dating?.agreedEdd ?: patient.dating?.calculateEstimatedDueDate()?.toString()
                if (!edd.isNullOrBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Text(
                            text = "EDD $edd",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                // 5. Allergy Status
                val allergies = patient.allergyHistory
                val allergyClean = allergies?.trim()
                val isNoAllergy = allergyClean.isNullOrBlank() ||
                    allergyClean.contains("NKDA", ignoreCase = true) ||
                    allergyClean.contains("No known", ignoreCase = true) ||
                    allergyClean.equals("None", ignoreCase = true) ||
                    allergyClean.equals("No", ignoreCase = true)
                val (allergyLabel, isAlert) = if (isNoAllergy) {
                    "NKDA" to false
                } else {
                    allergyClean!! to true
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isAlert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = allergyLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isAlert) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

enum class WorkspaceBedsideTab(
    val title: String,
    val defaultSection: WorkspaceSection,
    val testTagKey: String,
) {
    HISTORY("History", WorkspaceSection.DATING, "tab-history"),
    ENCOUNTERS("Encounters", WorkspaceSection.VISITS, "tab-encounters"),
    LABS("Labs", WorkspaceSection.LABORATORY, "tab-labs"),
    MEDS("Meds", WorkspaceSection.MEDICATIONS, "tab-meds"),
    PLAN("Plan", WorkspaceSection.DELIVERY, "tab-plan");

    companion object {
        fun fromSection(section: WorkspaceSection): WorkspaceBedsideTab = when (section) {
            WorkspaceSection.OBSTETRIC_HISTORY,
            WorkspaceSection.DATING,
            WorkspaceSection.OTHER_HISTORIES,
            WorkspaceSection.EPISODE_OVERVIEW -> HISTORY

            WorkspaceSection.VISITS,
            WorkspaceSection.ULTRASOUND -> ENCOUNTERS

            WorkspaceSection.LABORATORY -> LABS

            WorkspaceSection.MEDICATIONS -> MEDS

            WorkspaceSection.DELIVERY,
            WorkspaceSection.ATTACHMENTS -> PLAN
        }
    }
}

@Composable
private fun WorkspaceSectionTabBar(
    selectedTab: WorkspaceBedsideTab,
    onSelectTab: (WorkspaceBedsideTab) -> Unit,
    patient: DemoPatient,
    modifier: Modifier = Modifier,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)
        },
    ) {
        WorkspaceBedsideTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val badgeCount = when (tab) {
                WorkspaceBedsideTab.ENCOUNTERS -> patient.visits.size + patient.ultrasoundScans.size
                WorkspaceBedsideTab.LABS -> patient.labInvestigations.size
                WorkspaceBedsideTab.MEDS -> patient.medications.count { isOngoingMedication(it.status) }
                WorkspaceBedsideTab.HISTORY -> patient.previousPregnancies.size
                WorkspaceBedsideTab.PLAN -> patient.attachments.size
            }
            Tab(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                modifier = Modifier
                    .minimumTouchTarget()
                    .testTag(tab.testTagKey),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Box(
                            modifier = Modifier.testTag("section-${tab.defaultSection.name}")
                        )
                        val extraTag = when (tab) {
                            WorkspaceBedsideTab.HISTORY -> "section-OBSTETRIC_HISTORY"
                            WorkspaceBedsideTab.ENCOUNTERS -> "section-ULTRASOUND"
                            WorkspaceBedsideTab.PLAN -> "section-ATTACHMENTS"
                            else -> null
                        }
                        if (extraTag != null) {
                            Box(modifier = Modifier.testTag(extraTag))
                        }
                        Text(
                            text = tab.title,
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = (-0.3).sp,
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (badgeCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = badgeCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class EncounterFilter {
    ALL,
    VISITS,
    SCANS,
}

@Composable
private fun FilterPillChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .minimumTouchTarget()
            .testTag(tag),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private fun LazyListScope.workspaceBedsideContentItems(
    patient: DemoPatient,
    tab: WorkspaceBedsideTab,
    timelineEvents: List<LongitudinalTimelineEvent>,
    state: AncState,
    onEvent: (AncEvent) -> Unit,
    onStartVisit: (String, String?) -> Unit,
    onViewPhotos: (DemoUltrasoundScan, Int) -> Unit = { _, _ -> },
    onAddAttachment: () -> Unit = {},
    onViewAttachment: (DemoAttachment) -> Unit = {},
    encounterFilter: EncounterFilter = EncounterFilter.ALL,
    onSelectEncounterFilter: (EncounterFilter) -> Unit = {},
) {
    if (patient.status == "archived") {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.archived_read_only_notice),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
    state.attachmentError?.let { error ->
        item {
            NoticeCard(
                message = stringResource(
                    when (error) {
                        AttachmentError.SAVE_FAILED -> R.string.attachment_save_failed
                    },
                ),
                onClose = { onEvent(AncEvent.DismissAttachmentError) },
                modifier = Modifier.testTag("attachment_error_banner"),
                messageColor = MaterialTheme.colorScheme.error,
            )
        }
    }
    if (state.savedNotice) {
        item {
            NoticeCard(
                message = stringResource(state.notice.stringRes()),
                onClose = { onEvent(AncEvent.DismissNotice) },
                messageColor = MaterialTheme.colorScheme.tertiary,
            )
        }
    }

    when (tab) {
        WorkspaceBedsideTab.HISTORY -> {
            // 1. Pregnancy Dating Card
            item {
                DatingCard(
                    patient = patient,
                    onEdit = if (patient.status != "archived") {
                        {
                            onEvent(
                                AncEvent.StartDatingEditor(
                                    patientId = patient.id,
                                    editorSessionId = UUID.randomUUID().toString(),
                                ),
                            )
                        }
                    } else null,
                )
            }

            // 2. Obstetric History & Parity (G/P/A)
            item {
                ObstetricParityCard(
                    patient = patient,
                    onAddPregnancy = if (patient.status != "archived") {
                        {
                            onEvent(
                                AncEvent.StartPregnancyEditor(
                                    patientId = patient.id,
                                    pregnancyId = null,
                                    newPregnancyId = UUID.randomUUID().toString(),
                                    editorSessionId = UUID.randomUUID().toString(),
                                ),
                            )
                        }
                    } else null,
                )
            }

            if (patient.previousPregnancies.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_previous_pregnancies),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            } else {
                items(patient.previousPregnancies, key = { it.id }) { pregnancy ->
                    PreviousPregnancyCard(
                        pregnancy = pregnancy,
                        canEdit = patient.status != "archived",
                        onEdit = {
                            onEvent(
                                AncEvent.StartPregnancyEditor(
                                    patientId = patient.id,
                                    pregnancyId = pregnancy.id,
                                    newPregnancyId = pregnancy.id,
                                    editorSessionId = UUID.randomUUID().toString(),
                                ),
                            )
                        },
                        onDelete = {
                            onEvent(AncEvent.RequestDelete(patient.id, DeletableType.PREVIOUS_PREGNANCY, pregnancy.id))
                        },
                    )
                }
            }

            // 3. Medical, Surgical, Family Histories
            item {
                HistoriesCard(
                    patient = patient,
                    onEdit = if (patient.status != "archived") {
                        {
                            onEvent(AncEvent.StartHistoriesEditor(patient.id, UUID.randomUUID().toString()))
                        }
                    } else null,
                )
            }

            // 4. Episode Overview
            item { EpisodeOverviewContent(patient, onEvent) }
        }

        WorkspaceBedsideTab.ENCOUNTERS -> {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Filter row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val allCount = timelineEvents.size
                        val visitsCount = patient.visits.size
                        val scansCount = patient.ultrasoundScans.size

                        FilterPillChip(
                            label = "All ($allCount)",
                            isSelected = encounterFilter == EncounterFilter.ALL,
                            onClick = { onSelectEncounterFilter(EncounterFilter.ALL) },
                            tag = "filterEncounters-all",
                        )
                        FilterPillChip(
                            label = "Visits ($visitsCount)",
                            isSelected = encounterFilter == EncounterFilter.VISITS,
                            onClick = { onSelectEncounterFilter(EncounterFilter.VISITS) },
                            tag = "filterEncounters-visits",
                        )
                        FilterPillChip(
                            label = "Scans ($scansCount)",
                            isSelected = encounterFilter == EncounterFilter.SCANS,
                            onClick = { onSelectEncounterFilter(EncounterFilter.SCANS) },
                            tag = "filterEncounters-scans",
                        )
                    }

                    // Action buttons (if not archived)
                    if (patient.status != "archived") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = { onStartVisit(patient.id, null) },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumTouchTarget()
                                    .testTag("addVisit"),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.add_visit), maxLines = 1, softWrap = false)
                            }
                            OutlinedButton(
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
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumTouchTarget()
                                    .testTag("addUltrasound"),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.add_ultrasound), maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }

            val displayedEvents = when (encounterFilter) {
                EncounterFilter.ALL -> timelineEvents
                EncounterFilter.VISITS -> timelineEvents.filter { it.kind == TimelineEventKind.VISIT }
                EncounterFilter.SCANS -> timelineEvents.filter {
                    it.kind == TimelineEventKind.STANDALONE_ULTRASOUND || (it.kind == TimelineEventKind.VISIT && it.linkedScan != null)
                }
            }

            if (displayedEvents.isEmpty()) {
                item {
                    val emptyMessage = when (encounterFilter) {
                        EncounterFilter.VISITS -> "No antenatal visits recorded yet"
                        EncounterFilter.SCANS -> "No ultrasound scans recorded yet"
                        EncounterFilter.ALL -> stringResource(R.string.no_visits)
                    }
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            } else {
                itemsIndexed(displayedEvents, key = { _, event -> event.eventKey }) { index, event ->
                    val isFirst = index == 0
                    val isLast = index == displayedEvents.lastIndex
                    TimelineSpineItem(
                        isFirst = isFirst,
                        isLast = isLast,
                    ) {
                        when (event.kind) {
                            TimelineEventKind.VISIT -> {
                                event.visit?.let { visit ->
                                    VisitCard(
                                        visit = visit,
                                        patient = patient,
                                        canEdit = patient.status != "archived",
                                        onEdit = { onStartVisit(patient.id, visit.id) },
                                        onViewPhotos = onViewPhotos,
                                        onEvent = onEvent,
                                        onDelete = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.VISIT, visit.id)) },
                                        linkIntegrityWarning = event.linkIntegrityWarning,
                                    )
                                }
                            }
                            TimelineEventKind.STANDALONE_ULTRASOUND -> {
                                event.standaloneScan?.let { scan ->
                                    UltrasoundCard(
                                        scan = scan,
                                        patient = patient,
                                        canEdit = patient.status != "archived",
                                        onStartVisit = onStartVisit,
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
                                        onViewPhotos = onViewPhotos,
                                        onDelete = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.ULTRASOUND, scan.id)) },
                                        linkIntegrityWarning = event.linkIntegrityWarning,
                                        cardTag = "ultrasound-${scan.id}",
                                        deleteTag = "delete-ultrasound-${scan.id}",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        WorkspaceBedsideTab.LABS -> {
            if (patient.status != "archived") {
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().testTag("labsActionsRow"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.minimumTouchTarget().testTag("addLab"),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.add_lab), maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = { onEvent(AncEvent.StartLabBatchRequest(patient.id)) },
                            modifier = Modifier.minimumTouchTarget().testTag("startLabBatch"),
                        ) {
                            Text("New laboratory batch", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
            if (patient.labInvestigations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("emptyLabsCard"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.no_lab_recorded),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(patient.labInvestigations, key = { it.id }) { lab ->
                    LabCard(
                        lab = lab,
                        patient = patient,
                        canEdit = patient.status != "archived",
                        onDelete = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.LAB, lab.id)) },
                    ) {
                        onEvent(
                            AncEvent.StartLabEditor(
                                patientId = patient.id,
                                labId = lab.id,
                                newLabId = lab.id,
                                editorSessionId = UUID.randomUUID().toString(),
                            ),
                        )
                    }
                }
            }
        }

        WorkspaceBedsideTab.MEDS -> {
            val canEdit = patient.status != "archived"
            val isSaveBlocked = state.savePhase == com.sherifradwan.ancnative.state.SavePhase.PENDING ||
                state.savePhase == com.sherifradwan.ancnative.state.SavePhase.RECONCILING ||
                state.savePhase == com.sherifradwan.ancnative.state.SavePhase.UNRESOLVED
            val canStartEditor = canEdit && !state.hasDirtyDraft && !isSaveBlocked && !state.deleteRunning && !state.undoRunning
            if (canEdit) {
                item {
                    MedicationTapToAddSection(
                        patient = patient,
                        canStartEditor = canStartEditor,
                        onEvent = onEvent,
                    )
                }
            }
            item {
                MedicationsListContent(
                    patient = patient,
                    canEdit = canEdit,
                    canStartEditor = canStartEditor,
                    onEvent = onEvent,
                    onDeleteMedication = { med ->
                        onEvent(AncEvent.RequestDelete(patient.id, DeletableType.MEDICATION, med.id))
                    },
                    onEditMedication = { med ->
                        if (canStartEditor) {
                            dispatchMedicationDraft(
                                onEvent = onEvent,
                                patientId = patient.id,
                                medicationId = med.id,
                            )
                        }
                    },
                )
            }
        }

        WorkspaceBedsideTab.PLAN -> {
            item {
                DeliveryPlanningCard(
                    patient = patient,
                    onEditPatient = {
                        onEvent(
                            AncEvent.StartPatientEditor(
                                patientId = patient.id,
                                newPatientId = patient.id,
                                editorSessionId = UUID.randomUUID().toString(),
                            ),
                        )
                    },
                )
            }
            item {
                AttachmentsSectionCard(
                    patient = patient,
                    onAddAttachment = onAddAttachment,
                    onViewAttachment = onViewAttachment,
                    onViewPhotos = onViewPhotos,
                )
            }
        }
    }
}

@Composable
private fun VisitCard(
    visit: DemoVisit,
    patient: DemoPatient,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onViewPhotos: (DemoUltrasoundScan, Int) -> Unit = { _, _ -> },
    onEvent: (AncEvent) -> Unit,
    onDelete: () -> Unit = {},
    linkIntegrityWarning: String? = null,
) {
    val gaAtVisit = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, visit.date)?.displayText

    Card(
        modifier = Modifier.fillMaxWidth().testTag("visit-${visit.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header Row: Date + GA + Badges
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
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (!gaAtVisit.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF176B70).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF176B70).copy(alpha = 0.3f)),
                        ) {
                            Text(
                                text = gaAtVisit,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF176B70),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BpAlertLabel(visit.bloodPressure)

                    if (visit.linkedUltrasoundId != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                        ) {
                            Text(
                                text = "Scan Linked",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            if (!linkIntegrityWarning.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = linkIntegrityWarning,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Maternal Vitals Grid
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BLOOD PRESSURE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = visit.bloodPressure?.let { "$it mmHg" } ?: "Not recorded",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Box(
                        Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MATERNAL WEIGHT",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatWeightKg(visit.weight) ?: "Not recorded",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            FactRow(R.string.findings, visit.findings)
            FactRow(R.string.notes, visit.notes)
            if (visit.isSyntheticEdit) {
                Text(
                    stringResource(R.string.synthetic_edit),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            // Inline linked ultrasound scan card
            if (visit.linkedUltrasoundId != null) {
                val linkedScan = patient.ultrasoundScans.firstOrNull { it.id == visit.linkedUltrasoundId }
                if (linkedScan != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("inlineLinkedScan-${visit.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.linked_ultrasound),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                                Text(
                                    text = linkedScan.date,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                            val scanGa = when {
                                linkedScan.gestationalAgeWeeks != null && linkedScan.gestationalAgeDays != null -> "${linkedScan.gestationalAgeWeeks}w ${linkedScan.gestationalAgeDays}d"
                                linkedScan.gestationalAgeWeeks != null -> "${linkedScan.gestationalAgeWeeks}w"
                                else -> null
                            }
                            if (scanGa != null) {
                                Text("GA: $scanGa" + (linkedScan.presentation?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall)
                            }
                            if (linkedScan.fetalHeartRate != null || linkedScan.estimatedFetalWeight != null) {
                                Text(
                                    formatFetalMetrics(linkedScan.fetalHeartRate, linkedScan.estimatedFetalWeight),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (linkedScan.imageUris.isNotEmpty()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(vertical = 2.dp),
                                ) {
                                    Text(
                                        pluralStringResource(R.plurals.photos_count, linkedScan.imageUris.size, linkedScan.imageUris.size),
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
                                                    .testTag("wsVisitPhotoChip-${linkedScan.id}-$idx")
                                                    .clickable { onViewPhotos(linkedScan, idx) },
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    Text(
                                                        text = "Photo ${idx + 1}",
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
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(
                                    onClick = { onEvent(AncEvent.SelectSection(WorkspaceSection.ULTRASOUND)) },
                                    modifier = Modifier.testTag("openLinkedScan-${linkedScan.id}"),
                                ) {
                                    Text(stringResource(R.string.view_linked_ultrasound))
                                }
                                if (canEdit) {
                                    IconButton(
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
                                        modifier = Modifier
                                            .minimumTouchTarget()
                                            .testTag("editLinkedScan-${linkedScan.id}"),
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit_ultrasound),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (canEdit) {
                PencilEditButton(
                    contentDescription = stringResource(R.string.edit_visit),
                    tag = "editVisit-${visit.id}",
                    onClick = onEdit,
                    trailing = { DeleteIconButton(DeletableType.VISIT, visit, onDelete) },
                )
            }
        }
    }
}

@Composable
private fun HistoriesCard(
    patient: DemoPatient,
    onEdit: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Medical & Surgical History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (onEdit != null) {
                    PencilEditButton(
                        contentDescription = stringResource(R.string.edit_other_histories),
                        tag = "editHistories",
                        onClick = onEdit,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

            FactRow(R.string.medical_history, patient.medicalHistory)
            FactRow(R.string.surgical_history, patient.surgicalHistory)
            FactRow(R.string.family_history, patient.familyHistory)
            FactRow(R.string.allergy_history, patient.allergyHistory)

            val isNoKnownAllergies = patient.allergyHistory.isNullOrBlank() ||
                patient.allergyHistory.contains("NKDA", ignoreCase = true) ||
                patient.allergyHistory.contains("No known", ignoreCase = true) ||
                patient.allergyHistory.contains("none", ignoreCase = true)

            if (isNoKnownAllergies) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF176B70).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF176B70).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF176B70),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "No Known Drug Allergies (NKDA)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF176B70),
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Allergies: ${patient.allergyHistory}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ObstetricParityCard(
    patient: DemoPatient,
    onAddPregnancy: (() -> Unit)? = null,
) {
    val g = patient.gravida ?: 1L
    val p = patient.parity ?: 0L
    val a = patient.abortion ?: 0L
    val summary = previousDeliverySummary(patient)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Obstetric Parity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "G$g P$p" + if (a > 0) " A$a" else "",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                if (onAddPregnancy != null) {
                    Button(
                        onClick = onAddPregnancy,
                        modifier = Modifier
                            .minimumTouchTarget()
                            .testTag("addPreviousPregnancy"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.add_previous_pregnancy), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GRAVIDA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$g", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PARITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$p", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LOSSES (A)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$a", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!patient.obstetricNotes.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(10.dp)) {
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
        }
    }
}

@Composable
private fun PreviousPregnancyCard(
    pregnancy: DemoPreviousPregnancy,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pregnancy-${pregnancy.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val headerTitle = pregnancy.outcomeText ?: pregnancy.outcome ?: pregnancy.pregnancyType ?: "Previous Pregnancy"
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!pregnancy.dateText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = pregnancy.dateText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

            FactRow(R.string.entered_date_text, pregnancy.dateText)
            FactRow(R.string.entered_outcome_text, pregnancy.outcomeText)
            FactRow(R.string.pregnancy_type, pregnancy.pregnancyType)
            FactRow(R.string.structured_outcome, pregnancy.outcome)
            FactRow(R.string.historical_delivery_mode, pregnancy.deliveryBy)
            FactRow(R.string.other_details, pregnancy.otherDetails)
            FactRow(R.string.place_and_doctor, pregnancy.placeAndDoctor)
            FactRow(R.string.birth_method, pregnancy.birthMethod)
            FactRow(R.string.loss_management, pregnancy.lossManagement)
            FactRow(R.string.termination_method, pregnancy.terminationMethod)
            FactRow(R.string.clinician_id, pregnancy.clinicianId)
            if (canEdit) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DeleteIconButton(DeletableType.PREVIOUS_PREGNANCY, pregnancy, onDelete)
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .minimumTouchTarget()
                            .testTag("editPregnancy-${pregnancy.id}"),
                    ) {
                        Text(stringResource(R.string.correct_previous_pregnancy))
                    }
                }
            }
        }
    }
}

@Composable
private fun DatingCard(
    patient: DemoPatient,
    onEdit: (() -> Unit)? = null,
) {
    val dating = patient.dating
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("datingCard"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Pregnancy Dating",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (!dating?.datingMethod.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = dating?.datingMethod.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (onEdit != null) {
                    PencilEditButton(
                        contentDescription = stringResource(R.string.edit_dating),
                        tag = "editDating",
                        onClick = onEdit,
                    )
                }
            }

            if (dating == null) {
                Text(
                    text = stringResource(R.string.no_dating_recorded),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                return@Column
            }

            val ga = dating.calculateGestationalAge()
            val edd = dating.agreedEdd ?: dating.ultrasoundEdd ?: patient.dating?.calculateEstimatedDueDate()?.toString()
            if (!ga.isNullOrBlank() || !edd.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "GESTATIONAL AGE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = ga ?: "—",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ESTIMATED DUE DATE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = edd ?: "—",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            if (!ga.isNullOrBlank()) {
                FactRow(R.string.current_gestational_age, ga)
            }

            if (!dating.datingMethod.isNullOrBlank()) {
                FactRow(R.string.dating_method, dating.datingMethod)
            }

            when (dating.datingMethod?.uppercase()) {
                "ULTRASOUND", "US" -> {
                    if (!dating.ultrasoundEdd.isNullOrBlank()) {
                        FactRow(R.string.ultrasound_edd, dating.ultrasoundEdd)
                    }
                }
                "LMP" -> {
                    if (!dating.lmpDate.isNullOrBlank()) {
                        FactRow(R.string.lmp_date, dating.lmpDate)
                    }
                }
                "IVF_TRANSFER", "IVF" -> {
                    if (!dating.transferDate.isNullOrBlank()) {
                        FactRow(R.string.ivf_transfer_date, dating.transferDate)
                    }
                    dating.embryoAgeDays?.let { age ->
                        FactRow(R.string.embryo_age, "Day $age")
                    }
                }
                "CLINICAL" -> {
                    if (!dating.agreedEdd.isNullOrBlank()) {
                        FactRow(R.string.agreed_edd, dating.agreedEdd)
                    }
                }
                else -> {
                    if (!dating.ultrasoundEdd.isNullOrBlank()) {
                        FactRow(R.string.ultrasound_edd, dating.ultrasoundEdd)
                    }
                    if (!dating.lmpDate.isNullOrBlank()) {
                        FactRow(R.string.lmp_date, dating.lmpDate)
                    }
                    if (!dating.transferDate.isNullOrBlank()) {
                        FactRow(R.string.ivf_transfer_date, dating.transferDate)
                    }
                    dating.embryoAgeDays?.let { age ->
                        FactRow(R.string.embryo_age, "Day $age")
                    }
                    if (!dating.agreedEdd.isNullOrBlank()) {
                        FactRow(R.string.agreed_edd, dating.agreedEdd)
                    }
                }
            }

            if (!dating.notes.isNullOrBlank()) {
                FactRow(R.string.notes, dating.notes)
            }
        }
    }
}

@Composable
private fun UltrasoundCard(
    scan: DemoUltrasoundScan,
    patient: DemoPatient,
    canEdit: Boolean,
    onStartVisit: (String, String?) -> Unit,
    onEdit: () -> Unit,
    onViewPhotos: (DemoUltrasoundScan, Int) -> Unit = { _, _ -> },
    onDelete: () -> Unit = {},
    linkIntegrityWarning: String? = null,
    cardTag: String = "ultrasound-${scan.id}",
    deleteTag: String = "delete-ultrasound-${scan.id}",
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(cardTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!linkIntegrityWarning.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = linkIntegrityWarning,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        scan.date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (scan.linkedVisitId != null) {
                    val linkedVisit = patient.visits.firstOrNull { it.id == scan.linkedVisitId }
                    if (linkedVisit != null && canEdit) {
                        TextButton(
                            onClick = { onStartVisit(patient.id, scan.linkedVisitId) },
                            modifier = Modifier.testTag("openLinkedVisit-${scan.id}"),
                        ) {
                            Text(
                                text = "Visit: ${linkedVisit.date}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        Text(
                            text = linkedVisit?.date ?: stringResource(R.string.link_to_visit),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = stringResource(R.string.standalone_scan),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            val ga = when {
                scan.gestationalAgeWeeks != null && scan.gestationalAgeDays != null -> "${scan.gestationalAgeWeeks}w ${scan.gestationalAgeDays}d"
                scan.gestationalAgeWeeks != null -> "${scan.gestationalAgeWeeks}w"
                else -> null
            }
            if (ga != null) FactRow(R.string.ga_at_scan, ga)
            if (!scan.fetalNumber.isNullOrBlank()) FactRow(R.string.fetal_number, scan.fetalNumber)
            if (!scan.presentation.isNullOrBlank()) FactRow(R.string.fetal_presentation, scan.presentation)
            if (!scan.placentalSite.isNullOrBlank()) FactRow(R.string.placental_site, scan.placentalSite)
            if (!scan.amnioticFluid.isNullOrBlank()) FactRow(R.string.amniotic_fluid, scan.amnioticFluid)

            // Quantitative Biometry: Only display if actually recorded by clinician
            if (!scan.fetalHeartRate.isNullOrBlank()) FactRow(R.string.fetal_heart_rate, scan.fetalHeartRate)
            if (!scan.biometryBpd.isNullOrBlank()) FactRow(R.string.biometry_bpd, scan.biometryBpd)
            if (!scan.biometryHc.isNullOrBlank()) FactRow(R.string.biometry_hc, scan.biometryHc)
            if (!scan.biometryAc.isNullOrBlank()) FactRow(R.string.biometry_ac, scan.biometryAc)
            if (!scan.biometryFl.isNullOrBlank()) FactRow(R.string.biometry_fl, scan.biometryFl)
            if (!scan.estimatedFetalWeight.isNullOrBlank()) FactRow(R.string.estimated_fetal_weight, scan.estimatedFetalWeight)

            if (!scan.impression.isNullOrBlank()) FactRow(R.string.scan_impression, scan.impression)
            if (scan.imageUris.isNotEmpty()) {
                FactRow(R.string.scan_attachments, pluralStringResource(R.plurals.photos_count, scan.imageUris.size, scan.imageUris.size))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    scan.imageUris.forEachIndexed { idx, _ ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .testTag("wsScanPhotoChip-${scan.id}-$idx")
                                .clickable { onViewPhotos(scan, idx) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "Photo ${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }
            if (canEdit) {
                PencilEditButton(
                    contentDescription = stringResource(R.string.edit_ultrasound),
                    tag = "editUltrasound-${scan.id}",
                    onClick = onEdit,
                    trailing = { DeleteIconButton(DeletableType.ULTRASOUND, scan, onDelete, tag = deleteTag) },
                )
            }
        }
    }
}

@Composable
private fun EpisodeOverviewContent(patient: DemoPatient, onEvent: (AncEvent) -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("overviewMilestones"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.milestones),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                val ga = patient.dating?.calculateGestationalAge()
                FactRow(R.string.current_gestational_age, ga)
                FactRow(R.string.agreed_edd, patient.dating?.agreedEdd ?: patient.dating?.ultrasoundEdd)
                FactRow(R.string.manual_risk, patient.riskLevel)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag("overviewQuickActions"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.quick_actions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { onEvent(AncEvent.SelectSection(WorkspaceSection.VISITS)) },
                        modifier = Modifier.minimumTouchTarget(),
                    ) {
                        Text("+ " + stringResource(R.string.visits))
                    }
                    OutlinedButton(
                        onClick = { onEvent(AncEvent.SelectSection(WorkspaceSection.ULTRASOUND)) },
                        modifier = Modifier.minimumTouchTarget(),
                    ) {
                        Text("+ " + stringResource(R.string.ultrasound))
                    }
                    OutlinedButton(
                        onClick = { onEvent(AncEvent.SelectSection(WorkspaceSection.LABORATORY)) },
                        modifier = Modifier.minimumTouchTarget(),
                    ) {
                        Text("+ " + stringResource(R.string.laboratory))
                    }
                    OutlinedButton(
                        onClick = { onEvent(AncEvent.SelectSection(WorkspaceSection.MEDICATIONS)) },
                        modifier = Modifier.minimumTouchTarget(),
                    ) {
                        Text("+ " + stringResource(R.string.medications))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag("overviewClinicalSummary"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.episode_overview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                FactRow(R.string.total_visits, patient.visits.size.toString())
                val latestVisit = patient.visits.maxByOrNull { it.date }
                FactRow(R.string.latest_visit, latestVisit?.let { "${it.date} (${it.bloodPressure ?: "BP unrecorded"})" })
                val latestScan = patient.ultrasoundScans.maxByOrNull { it.date }
                FactRow(R.string.latest_scan, latestScan?.let { "${it.date} (${it.presentation ?: "Presentation unrecorded"})" })
                FactRow(R.string.laboratory, "${patient.labInvestigations.size} recorded (${patient.labInvestigations.count { it.isAbnormal }} abnormal)")
                FactRow(R.string.medications, "${patient.medications.count { isOngoingMedication(it.status, it.isNonCompliant) }} active")

                Button(
                    onClick = {
                        val text = ClinicalSummaryGenerator.generateTextSummary(patient)
                        clipboardManager.setText(AnnotatedString(text))
                        copied = true
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .minimumTouchTarget()
                        .testTag("copyClinicalSummary"),
                ) {
                    Text(
                        text = if (copied) stringResource(R.string.summary_copied) else stringResource(R.string.copy_clinical_summary),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryPlanningCard(
    patient: DemoPatient,
    onEditPatient: () -> Unit,
) {
    val sections = remember(patient.obstetricNotes) { parseDeliveryNotes(patient.obstetricNotes) }
    val latestScan = patient.ultrasoundScans.maxByOrNull { it.date }
    val rhNegative = isRhNegative(patient.bloodGroupText)

    Card(
        modifier = Modifier.fillMaxWidth().testTag("deliveryPlanningCard"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.delivery_planning),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                IconButton(
                    onClick = onEditPatient,
                    modifier = Modifier.minimumTouchTarget().testTag("editDeliveryPlan"),
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_delivery_plan),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.delivery_planning_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Key clinical facts: label above, bold value, each in its own tile
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                KeyFactTile(
                    label = stringResource(R.string.blood_group),
                    value = formatBloodGroup(patient.bloodGroupText),
                    emphasize = rhNegative,
                    modifier = Modifier.weight(1f),
                )
                KeyFactTile(
                    label = stringResource(R.string.current_gestational_age),
                    value = patient.dating?.calculateGestationalAge(),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                KeyFactTile(
                    label = stringResource(R.string.agreed_edd),
                    value = patient.dating?.agreedEdd ?: patient.dating?.ultrasoundEdd,
                    modifier = Modifier.weight(1f),
                )
                KeyFactTile(
                    label = stringResource(R.string.manual_risk),
                    value = patient.riskLevel,
                    emphasize = isHighRisk(patient.riskLevel),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                KeyFactTile(
                    label = stringResource(R.string.fetal_presentation),
                    value = latestScan?.presentation,
                    modifier = Modifier.weight(1f),
                )
                KeyFactTile(
                    label = stringResource(R.string.placental_site),
                    value = latestScan?.placentalSite,
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider()

            DeliverySectionBox(
                title = "Agreed Delivery Hospital",
                value = sections.hospital,
                tone = DeliveryTone.HOSPITAL,
                placeholder = "Hospital not agreed yet",
                modifier = Modifier.testTag("deliveryHospital"),
            )
            DeliverySectionBox(
                title = "Agreed Delivery Fees",
                value = sections.fees,
                tone = DeliveryTone.FEE,
                placeholder = "To be agreed based on hospital and delivery mode (NVD / CS)",
                modifier = Modifier.testTag("deliveryFees"),
            )
            DeliverySectionBox(
                title = "Delivery Plan",
                value = sections.plan,
                tone = DeliveryTone.PLAN,
                placeholder = "No delivery plan recorded yet",
                modifier = Modifier.testTag("deliveryPlan"),
            )
            DeliverySectionBox(
                title = "Postpartum Note",
                value = sections.postpartum,
                tone = DeliveryTone.POSTPARTUM,
                placeholder = "No postpartum note recorded yet",
                modifier = Modifier.testTag("deliveryPostpartum"),
            )
        }
    }
}

@Composable
private fun LabCard(
    lab: DemoLabInvestigation,
    patient: DemoPatient,
    canEdit: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("lab-${lab.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        text = lab.date,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val ga = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, lab.date)
                    if (ga?.isPostConception == true) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF176B70).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF176B70).copy(alpha = 0.35f)),
                        ) {
                            Text(
                                text = ga.displayText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF176B70),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (lab.isAbnormal) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = stringResource(R.string.abnormal_badge),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Text(lab.testName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val resultWithUnit = if (lab.unit.isNullOrBlank()) lab.result else "${lab.result} ${lab.unit}"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (lab.isAbnormal) MaterialTheme.colorScheme.error.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.test_result).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!lab.referenceRange.isNullOrBlank()) {
                            Text(
                                text = "${stringResource(R.string.reference_range)}: ${lab.referenceRange}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = resultWithUnit,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (lab.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (!lab.labName.isNullOrBlank()) {
                FactRow(R.string.lab_name, lab.labName)
            }
            if (!lab.testCategory.isNullOrBlank()) {
                FactRow(R.string.test_category, lab.testCategory)
            }
            if (!lab.notes.isNullOrBlank()) {
                FactRow(R.string.notes, lab.notes)
            }
            if (canEdit) {
                PencilEditButton(
                    contentDescription = stringResource(R.string.edit_lab),
                    tag = "editLab-${lab.id}",
                    onClick = onEdit,
                    trailing = { DeleteIconButton(DeletableType.LAB, lab, onDelete) },
                )
            }
        }
    }
}

enum class ClinicalAttentionLevel {
    CRITICAL,
    ALERT,
    NOTE;

    companion object {
        fun parse(raw: String): Pair<ClinicalAttentionLevel, String> {
            val trimmed = raw.trim()
            val prefixes = listOf(
                "CRITICAL:" to CRITICAL,
                "[CRITICAL]" to CRITICAL,
                "ALERT:" to ALERT,
                "[ALERT]" to ALERT,
                "WARNING:" to ALERT,
                "[WARNING]" to ALERT,
                "NOTE:" to NOTE,
                "[NOTE]" to NOTE,
            )
            for ((prefix, level) in prefixes) {
                if (trimmed.startsWith(prefix, ignoreCase = true)) {
                    val narrative = trimmed.substring(prefix.length).trim()
                    return level to narrative.ifEmpty { trimmed }
                }
            }
            return NOTE to trimmed
        }
    }
}

@Composable
private fun ClinicalAttentionBanner(
    attention: String?,
    severeBp: SevereBpAttention? = null,
    onEdit: () -> Unit,
) {
    val hasStored = !attention.isNullOrBlank()
    val (parsedLevel, narrative) = if (hasStored) {
        ClinicalAttentionLevel.parse(attention!!)
    } else {
        ClinicalAttentionLevel.NOTE to ""
    }
    val level = if (severeBp != null) ClinicalAttentionLevel.CRITICAL else parsedLevel
    val isDark = isSystemInDarkTheme()

    val containerColor = if (isDark) TagBloodNegativeDarkBg else TagBloodNegativeBg
    val borderColor = if (isDark) TagBloodNegativeDarkBorder else TagBloodNegativeBorder
    val accentBarColor = ActionRose
    val textColor = if (isDark) TagBloodNegativeDarkText else TagBloodNegativeText

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("clinicalAttentionBanner"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(28.dp)
                    .background(accentBarColor, RoundedCornerShape(2.dp)),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = accentBarColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.clinical_attention).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentBarColor,
                            letterSpacing = 0.8.sp,
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumTouchTarget(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_clinical_alerts),
                            tint = accentBarColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                if (severeBp != null) {
                    Text(
                        text = stringResource(R.string.bp_attention_severe, severeBp.bloodPressure, severeBp.date),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("severeBpAttention"),
                    )
                }

                if (hasStored && narrative.isNotBlank()) {
                    Text(
                        text = narrative,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClinicalAttentionDialog(
    currentAttention: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val (parsedLevel, parsedNarrative) = ClinicalAttentionLevel.parse(currentAttention)
    var selectedLevel by remember { mutableStateOf(parsedLevel) }
    var text by remember { mutableStateOf(parsedNarrative) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Clinical Attention & Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Select alert severity, and enter clinical notes (one per line):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val levels = listOf(
                        Triple(ClinicalAttentionLevel.CRITICAL, "CRITICAL", MaterialTheme.colorScheme.error),
                        Triple(ClinicalAttentionLevel.ALERT, "ALERT", MaterialTheme.colorScheme.tertiary),
                        Triple(ClinicalAttentionLevel.NOTE, "NOTE", MaterialTheme.colorScheme.primary),
                    )
                    levels.forEach { (lvl, title, color) ->
                        val isSelected = selectedLevel == lvl
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) color else color.copy(alpha = 0.08f),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) color else color.copy(alpha = 0.35f),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedLevel = lvl },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.surface else color,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Alerts narrative (one item per line)") },
                    placeholder = { Text("Example:\n• Mild anemia at 34w (Hb 10.6 g/dL)\n• Non-compliant with Calcium") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prefix = when (selectedLevel) {
                        ClinicalAttentionLevel.CRITICAL -> "[CRITICAL] "
                        ClinicalAttentionLevel.ALERT -> "[ALERT] "
                        ClinicalAttentionLevel.NOTE -> "[NOTE] "
                    }
                    val fullText = if (text.isBlank()) "" else "$prefix${text.trim()}"
                    onSave(fullText)
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun AttachmentsSectionCard(
    patient: DemoPatient,
    onAddAttachment: () -> Unit,
    onViewAttachment: (DemoAttachment) -> Unit,
    onViewPhotos: (DemoUltrasoundScan, Int) -> Unit,
) {
    val totalCount = patient.attachments.size
    val totalBytes = remember(patient.attachments) { patient.attachments.sumOf { it.fileSizeBytes } }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attachmentsSectionCard"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Clinical Documents & Attachments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (patient.status != "archived") {
                    Button(
                        onClick = onAddAttachment,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.minimumTouchTarget().testTag("addAttachmentBtn"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Attachment", maxLines = 1, softWrap = false)
                    }
                }
            }

        if (totalCount == 0) {
            Text(
                text = "No saved attachments or clinical images",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            var selectedFilter by remember { mutableStateOf("ALL") }
            val filteredAttachments = remember(patient.attachments, selectedFilter) {
                if (selectedFilter == "ALL") patient.attachments
                else patient.attachments.filter { it.category == selectedFilter }
            }

            Text(
                text = "$totalCount saved files (${AttachmentSyncManager.formatFileSize(totalBytes)})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
            )

            // Category Filter Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All ($totalCount)") },
                )
                FilterChip(
                    selected = selectedFilter == AttachmentCategories.ULTRASOUND,
                    onClick = { selectedFilter = AttachmentCategories.ULTRASOUND },
                    label = { Text("Ultrasound (${patient.attachments.count { it.category == AttachmentCategories.ULTRASOUND }})") },
                )
                FilterChip(
                    selected = selectedFilter == AttachmentCategories.LAB,
                    onClick = { selectedFilter = AttachmentCategories.LAB },
                    label = { Text("Labs (${patient.attachments.count { it.category == AttachmentCategories.LAB }})") },
                )
                FilterChip(
                    selected = selectedFilter == AttachmentCategories.PRESCRIPTION,
                    onClick = { selectedFilter = AttachmentCategories.PRESCRIPTION },
                    label = { Text("Prescriptions (${patient.attachments.count { it.category == AttachmentCategories.PRESCRIPTION }})") },
                )
                FilterChip(
                    selected = selectedFilter == AttachmentCategories.CLINICAL_PHOTO,
                    onClick = { selectedFilter = AttachmentCategories.CLINICAL_PHOTO },
                    label = { Text("Clinical Photos (${patient.attachments.count { it.category == AttachmentCategories.CLINICAL_PHOTO }})") },
                )
            }

            if (filteredAttachments.isEmpty()) {
                Text(
                    text = "No attachments in this selected category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredAttachments.forEach { attachment ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onViewAttachment(attachment) }
                                .testTag("attachmentCard-${attachment.id}"),
                            tonalElevation = 1.dp,
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Thumbnail preview
                                val thumbBitmap = remember(attachment.id) {
                                    runCatching {
                                        val file = com.sherifradwan.ancnative.storage.PatientAttachmentVault.resolveAttachmentFile(
                                            context = context,
                                            patientId = patient.id,
                                            fileName = attachment.fileName,
                                        )
                                        if (file != null && file.exists()) {
                                            BoundedBitmap.decodeFile(file.absolutePath, 2048)?.asImageBitmap()
                                        } else null
                                    }.getOrNull()
                                }

                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (thumbBitmap != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = thumbBitmap,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        )
                                    } else {
                                        Text("—", fontSize = 24.sp)
                                    }
                                }

                                // Details
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = attachmentDisplayTitle(
                                            title = attachment.title,
                                            category = AttachmentCategories.localizedName(attachment.category),
                                            capturedAt = attachment.capturedAt,
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                        ) {
                                            Text(
                                                text = AttachmentCategories.localizedName(attachment.category),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            )
                                        }
                                        Text(
                                            text = AttachmentSyncManager.formatFileSize(attachment.fileSizeBytes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Text(
                                            text = "Saved on this phone · ${attachment.capturedAt.take(10)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Legacy Scan Photos if any
        val scansWithPhotos = remember(patient.ultrasoundScans) {
            patient.ultrasoundScans.filter { it.imageUris.isNotEmpty() }
        }
        if (scansWithPhotos.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Previous Linked Ultrasound Scan Photos:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            scansWithPhotos.forEach { scan ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Scan date: ${scan.date} (${scan.gestationalAgeWeeks?.let { "${it}w" } ?: "Gynecological"}) - ${pluralStringResource(R.plurals.photos_count, scan.imageUris.size, scan.imageUris.size)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        scan.imageUris.forEachIndexed { idx, _ ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .testTag("wsAttachmentPhotoChip-${scan.id}-$idx")
                                    .clickable { onViewPhotos(scan, idx) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "Photo ${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
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
}

private val StringSetSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() },
)

private data class MedicationTileItem(
    val id: String,
    val name: String,
    val dosage: String,
    val route: String,
    val isCustom: Boolean = false,
)

private fun dispatchMedicationDraft(
    onEvent: (AncEvent) -> Unit,
    patientId: String,
    medicationId: String? = null,
    name: String? = null,
    dosage: String? = null,
    route: String? = null,
    status: String? = null,
    startDate: String? = null,
    endDate: String? = null,
    submit: Boolean = false,
) {
    onEvent(
        AncEvent.StartMedicationEditor(
            patientId = patientId,
            medicationId = medicationId,
            newMedicationId = medicationId ?: UUID.randomUUID().toString(),
            editorSessionId = UUID.randomUUID().toString(),
        ),
    )
    if (name != null) onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.NAME, name))
    if (dosage != null) onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.DOSAGE, dosage))
    if (route != null) onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.ROUTE, route))
    if (status != null) onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.STATUS, status))
    if (startDate != null) onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.START_DATE, startDate))
    if (endDate != null) onEvent(AncEvent.MedicationDraftChanged(MedicationDraftField.END_DATE, endDate))
    if (submit) onEvent(AncEvent.SubmitMedication)
}

@Composable
private fun MedicationTapToAddSection(
    patient: DemoPatient,
    canStartEditor: Boolean,
    onEvent: (AncEvent) -> Unit,
) {
    val context = LocalContext.current
    val codexRepo = remember { ClinicalCodexRepository.getInstance(context) }
    val codexMedItems by codexRepo.medicationCodexItems.collectAsState()
    val customChoices by codexRepo.customChoices(CustomChoiceList.MEDICATION_NAME).collectAsState(initial = emptyList())

    val alreadyOnListFormat = stringResource(R.string.already_on_todays_list)
    val blankError = stringResource(R.string.custom_drug_blank_error)
    val tooLongError = stringResource(R.string.custom_drug_too_long_error)
    val duplicateError = stringResource(R.string.custom_drug_duplicate_error)
    val invalidCharsError = stringResource(R.string.custom_drug_invalid_chars_error)
    val notFoundError = stringResource(R.string.custom_drug_not_found_error)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf("common") }
    var showAddOtherDialog by rememberSaveable { mutableStateOf(false) }

    SectionCard(
        title = stringResource(R.string.tap_to_add),
        tag = "medTapToAddCard",
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

        // HB1: Build tiles dynamically from codexRepo.medicationCodexItems, plus custom choices
        val allTiles = remember(codexMedItems, customChoices) {
            val codex = codexMedItems.map {
                MedicationTileItem(
                    id = it.id,
                    name = it.name,
                    dosage = it.dosage,
                    route = it.route,
                    isCustom = it.isCustom,
                )
            }
            val custom = customChoices.map {
                MedicationTileItem(
                    id = "custom_${it.id}",
                    name = it.label,
                    dosage = "",
                    route = "Oral",
                    isCustom = true,
                )
            }
            codex + custom
        }

        // HB2: Recent tab shows only the patient's real medication history
        val pastMeds = remember(patient.medications) {
            patient.medications.map { it.name.trim() }.distinctBy { it.lowercase() }
        }

        val filteredTiles = remember(searchQuery, selectedTab, allTiles, pastMeds) {
            val q = searchQuery.trim().lowercase()
            val baseList = if (selectedTab == "recent") {
                val pastNames = pastMeds.map { it.lowercase() }.toSet()
                allTiles.filter { it.name.trim().lowercase() in pastNames }
            } else {
                allTiles
            }
            if (q.isEmpty()) baseList else baseList.filter { it.name.lowercase().contains(q) }
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
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumTouchTarget()
                            .testTag("medTile-${tile.id}")
                            .clickable(role = Role.Button) {
                                if (!canStartEditor) return@clickable
                                if (isActive) {
                                    Toast.makeText(
                                        context,
                                        String.format(alreadyOnListFormat, tile.name),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else if (tile.isCustom || tile.dosage.isBlank()) {
                                    // HB3: Custom drug or empty dosage opens editor so clinician sets real dosage safely
                                    dispatchMedicationDraft(
                                        onEvent = onEvent,
                                        patientId = patient.id,
                                        name = tile.name,
                                        route = tile.route.ifBlank { "Oral" },
                                        status = MEDICATION_STATUS_ACTIVE,
                                        startDate = LocalDate.now().toString(),
                                        submit = false,
                                    )
                                } else {
                                    // Verified codex item with standard dosage: 1-tap add
                                    dispatchMedicationDraft(
                                        onEvent = onEvent,
                                        patientId = patient.id,
                                        name = tile.name,
                                        dosage = tile.dosage,
                                        route = tile.route,
                                        status = MEDICATION_STATUS_ACTIVE,
                                        startDate = LocalDate.now().toString(),
                                        submit = true,
                                    )
                                }
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
                                    Icons.Default.Check,
                                    contentDescription = stringResource(R.string.medication_status_active),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp),
                                )
                            } else {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_medication),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { showAddOtherDialog = true },
                modifier = Modifier.minimumTouchTarget().testTag("medAddOtherChip"),
            ) {
                Text(stringResource(R.string.add_other_choice), maxLines = 1, softWrap = false)
            }
            OutlinedButton(
                onClick = {
                    if (canStartEditor) {
                        dispatchMedicationDraft(
                            onEvent = onEvent,
                            patientId = patient.id,
                            submit = false,
                        )
                    }
                },
                modifier = Modifier.minimumTouchTarget().testTag("addMedication"),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_medication), maxLines = 1, softWrap = false)
            }
        }
    }

    if (showAddOtherDialog) {
        var customInput by rememberSaveable { mutableStateOf("") }
        var inputError by rememberSaveable { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showAddOtherDialog = false },
            title = { Text(stringResource(R.string.custom_drug_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorField(
                        value = customInput,
                        onValueChange = {
                            customInput = it
                            inputError = null
                        },
                        label = stringResource(R.string.custom_drug_label),
                        tag = "addOtherChoiceInput",
                        error = inputError != null,
                        singleLine = true,
                    )
                    if (inputError != null) {
                        Text(
                            text = inputError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = customInput.trim()
                        val res = codexRepo.addCustomChoice(
                            list = CustomChoiceList.MEDICATION_NAME,
                            label = trimmed,
                            existingDefaults = codexMedItems.map { it.name },
                        )
                        when (res) {
                            is CustomChoiceValidationResult.Valid -> {
                                if (canStartEditor) {
                                    dispatchMedicationDraft(
                                        onEvent = onEvent,
                                        patientId = patient.id,
                                        name = res.sanitizedLabel,
                                        route = "Oral",
                                        status = MEDICATION_STATUS_ACTIVE,
                                        startDate = LocalDate.now().toString(),
                                        submit = false,
                                    )
                                }
                                showAddOtherDialog = false
                            }
                            is CustomChoiceValidationResult.Invalid -> {
                                inputError = when (res.reason) {
                                    CustomChoiceValidationResult.Invalid.Reason.BLANK -> blankError
                                    CustomChoiceValidationResult.Invalid.Reason.TOO_LONG -> tooLongError
                                    CustomChoiceValidationResult.Invalid.Reason.DUPLICATE -> duplicateError
                                    CustomChoiceValidationResult.Invalid.Reason.INVALID_CHARACTERS -> invalidCharsError
                                    CustomChoiceValidationResult.Invalid.Reason.NOT_FOUND -> notFoundError
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("addOtherChoiceConfirm"),
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddOtherDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            modifier = Modifier.testTag("addOtherChoiceDialog"),
        )
    }
}

@Composable
private fun MedicationsListContent(
    patient: DemoPatient,
    canEdit: Boolean,
    canStartEditor: Boolean,
    onEvent: (AncEvent) -> Unit,
    onDeleteMedication: (DemoMedicationRecord) -> Unit,
    onEditMedication: (DemoMedicationRecord) -> Unit,
) {
    val medications = patient.medications
    val activeMeds = medications.filter { isOngoingMedication(it.status, it.isNonCompliant) }
    val todayStr = LocalDate.now().toString()
    val stoppedInSessionMeds = medications.filter {
        !isOngoingMedication(it.status, it.isNonCompliant) && it.endDate == todayStr
    }

    var expandedIds by rememberSaveable(
        patient.id,
        stateSaver = StringSetSaver,
    ) {
        mutableStateOf(
            if (activeMeds.size <= 3) {
                activeMeds.map { it.id }.toSet()
            } else {
                emptySet()
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard(
            title = stringResource(R.string.todays_list),
            tag = "medTodayList",
        ) {
            if (activeMeds.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.active_count_summary, activeMeds.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (activeMeds.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_medications_recorded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (activeMeds.size > 3) {
                    val allExpanded = activeMeds.all { it.id in expandedIds }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = {
                                expandedIds = if (allExpanded) {
                                    emptySet()
                                } else {
                                    activeMeds.map { it.id }.toSet()
                                }
                            },
                            modifier = Modifier
                                .minimumTouchTarget()
                                .testTag("medicationsToggleAll"),
                        ) {
                            Text(
                                text = stringResource(if (allExpanded) R.string.collapse_all else R.string.expand_all),
                            )
                        }
                    }
                }

                activeMeds.forEach { med ->
                    val isExpanded = med.id in expandedIds
                    MedicationCard(
                        med = med,
                        isExpanded = isExpanded,
                        canEdit = canEdit,
                        onToggle = {
                            expandedIds = if (isExpanded) {
                                expandedIds - med.id
                            } else {
                                expandedIds + med.id
                            }
                        },
                        onStop = {
                            if (canStartEditor) {
                                dispatchMedicationDraft(
                                    onEvent = onEvent,
                                    patientId = patient.id,
                                    medicationId = med.id,
                                    status = MEDICATION_STATUS_DISCONTINUED,
                                    endDate = LocalDate.now().toString(),
                                    submit = true,
                                )
                            }
                        },
                        onEdit = { onEditMedication(med) },
                        onDelete = { onDeleteMedication(med) },
                    )
                }
            }
        }

        if (stoppedInSessionMeds.isNotEmpty()) {
            SectionCard(
                title = stringResource(R.string.stopped_in_this_session),
                tag = "medStoppedSection",
            ) {
                stoppedInSessionMeds.forEach { med ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stoppedMedication-${med.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    med.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                val details = listOfNotNull(med.dosage.takeIf { it.isNotBlank() }, med.route.takeIf { it.isNotBlank() })
                                val detailsStr = if (details.isNotEmpty()) " · ${details.joinToString(" · ")}" else ""
                                Text(
                                    "${stringResource(R.string.medication_status_stopped)}$detailsStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (canEdit) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    DeleteIconButton(DeletableType.MEDICATION, med, onClick = { onDeleteMedication(med) })
                                    TextButton(
                                        onClick = {
                                            if (canStartEditor) {
                                                dispatchMedicationDraft(
                                                    onEvent = onEvent,
                                                    patientId = patient.id,
                                                    medicationId = med.id,
                                                    status = MEDICATION_STATUS_ACTIVE,
                                                    endDate = "",
                                                    submit = true,
                                                )
                                            }
                                        },
                                        modifier = Modifier.minimumTouchTarget().testTag("restartMedication-${med.id}"),
                                    ) {
                                        Text(stringResource(R.string.restart_medication), maxLines = 1, softWrap = false)
                                    }
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
private fun MedicationCard(
    med: DemoMedicationRecord,
    isExpanded: Boolean,
    canEdit: Boolean,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val status = medicationStatusLabel(med.status, med.isNonCompliant)
    val instructions = medicationInstructions(med.dosage, med.route, med.notes)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("medication-${med.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(bottom = if (isExpanded) 12.dp else 0.dp)) {
            // Header: toggle area (name, status, summary) takes the free width; delete and edit keep 48 dp targets.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .minimumTouchTarget()
                        .testTag("medicationToggle-${med.id}")
                        .clickable(onClick = onToggle)
                        .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(if (isExpanded) R.string.hide_details else R.string.show_details),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // A drug name is never cut: it wraps to a second line instead.
                        Text(med.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        MedicationStatusBadge(status)
                        // Instructions line (dose, route, note): lighter and smaller than the bold name, still readable.
                        if (instructions.isNotBlank()) {
                            Text(
                                text = instructions,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("medicationInstructions-${med.id}"),
                            )
                        }
                    }
                }
                if (canEdit) {
                    if (isOngoingMedication(med.status, med.isNonCompliant)) {
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier.minimumTouchTarget().testTag("stopMedication-${med.id}"),
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = stringResource(R.string.stop_medication),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    DeleteIconButton(DeletableType.MEDICATION, med, onDelete)
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumTouchTarget().testTag("editMedication-${med.id}"),
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_medication),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else {
                    Spacer(Modifier.width(12.dp))
                }
            }
            if (isExpanded) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    FactRow(R.string.dosage, med.dosage)
                    FactRow(R.string.route, med.route)
                    FactRow(R.string.start_date, med.startDate)
                    if (!med.adverseNotes.isNullOrBlank()) {
                        FactRow(R.string.adverse_effects, med.adverseNotes)
                    }
                    FactRow(R.string.notes, med.notes)
                }
            }
        }
    }
}

@StringRes
fun WorkspaceSection.labelResource(): Int = when (this) {
    WorkspaceSection.OBSTETRIC_HISTORY -> R.string.obstetric_history
    WorkspaceSection.DATING -> R.string.dating
    WorkspaceSection.OTHER_HISTORIES -> R.string.other_histories
    WorkspaceSection.EPISODE_OVERVIEW -> R.string.episode_overview
    WorkspaceSection.VISITS -> R.string.visits
    WorkspaceSection.ULTRASOUND -> R.string.ultrasound
    WorkspaceSection.LABORATORY -> R.string.laboratory
    WorkspaceSection.MEDICATIONS -> R.string.medications_tab
    WorkspaceSection.DELIVERY -> R.string.delivery
    WorkspaceSection.ATTACHMENTS -> R.string.attachments
}

/** Global compact edit affordance: a right-aligned pencil icon button. */
@Composable
internal fun PencilEditButton(
    contentDescription: String,
    tag: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        trailing?.invoke()
        IconButton(onClick = onClick, modifier = Modifier.testTag(tag)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
