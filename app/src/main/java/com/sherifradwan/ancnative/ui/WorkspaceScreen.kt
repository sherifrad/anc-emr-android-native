package com.sherifradwan.ancnative.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.storage.AttachmentCategories
import com.sherifradwan.ancnative.storage.AttachmentSyncManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.sherifradwan.ancnative.model.ClinicalSummaryGenerator
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.state.SavedNotice
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
    val pagerState = rememberPagerState(
        initialPage = route.section.ordinal,
        pageCount = { WorkspaceSection.entries.size },
    )
    val coroutineScope = rememberCoroutineScope()

    // Durable external route -> Pager synchronization
    LaunchedEffect(route.section) {
        if (pagerState.currentPage != route.section.ordinal) {
            pagerState.animateScrollToPage(route.section.ordinal)
        }
    }

    // Pager swipe gesture -> AncState synchronization (loop-free via settledPage)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { settledPage ->
                val settledSection = WorkspaceSection.entries[settledPage]
                if (settledSection != route.section) {
                    onEvent(AncEvent.SelectSection(settledSection))
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = maxOf(16.dp, (maxWidth - 840.dp) / 2)
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WorkspaceTopBar(
                    patient = patient,
                    asOfDate = asOfDate,
                    onEvent = onEvent,
                    onOpenReport = { showReportDialog = true },
                    onOpenAttentionDialog = { showAttentionDialog = true },
                )
                if (!patient.clinicalAttention.isNullOrBlank()) {
                    ClinicalAttentionBanner(
                        attention = patient.clinicalAttention,
                        onEdit = { showAttentionDialog = true },
                    )
                }
            }

            val timelineEvents = remember(patient) {
                LongitudinalTimelineProjection.build(patient)
            }

            WorkspaceSectionTabBar(
                selected = route.section,
                onSelect = { section ->
                    if (section != route.section) {
                        onEvent(AncEvent.SelectSection(section))
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
                val pageSection = WorkspaceSection.entries[page]
                val isCurrent = pageSection == route.section
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(if (isCurrent) "workspaceList" else "workspaceList-${pageSection.name}"),
                    contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    workspaceContentItems(
                        patient = patient,
                        section = pageSection,
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
                    )
                }
            }
        }

        if (viewingPhotoScan != null) {
            UltrasoundPhotoViewerDialog(
                scan = viewingPhotoScan!!,
                initialIndex = viewingPhotoIndex,
                onDismiss = { viewingPhotoScan = null },
            )
        }

        if (viewingAttachment != null) {
            ClinicalAttachmentViewerDialog(
                patient = patient,
                attachment = viewingAttachment!!,
                onDismiss = { viewingAttachment = null },
                onDelete = {
                    val att = viewingAttachment ?: return@ClinicalAttachmentViewerDialog
                    onEvent(
                        AncEvent.DeleteAttachment(
                            patientId = patient.id,
                            attachmentId = att.id,
                            fileName = att.fileName,
                        ),
                    )
                    viewingAttachment = null
                },
            )
        }

        if (showAddAttachmentDialog) {
            AddAttachmentDialog(
                patient = patient,
                onDismiss = { showAddAttachmentDialog = false },
                onSave = { uri, category, title ->
                    onEvent(
                        AncEvent.AddAttachment(
                            patientId = patient.id,
                            sourceUri = uri,
                            category = category,
                            title = title,
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
            IconButton(
                onClick = { onEvent(AncEvent.Back) },
                modifier = Modifier.minimumTouchTarget(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                val ga = patient.dating?.calculateGestationalAge(asOfDate)
                if (ga != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = ga.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onOpenAttentionDialog,
                modifier = Modifier.minimumTouchTarget().testTag("workspaceAttentionButton"),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Clinical Attention & Alerts",
                    tint = if (patient.clinicalAttention.isNullOrBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
            }
            IconButton(
                onClick = onOpenReport,
                modifier = Modifier.minimumTouchTarget().testTag("workspaceReportButton"),
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = "Print 2-Page Clinical Report",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp),
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
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun WorkspaceSectionTabBar(
    selected: WorkspaceSection,
    onSelect: (WorkspaceSection) -> Unit,
    patient: DemoPatient,
    modifier: Modifier = Modifier,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selected.ordinal,
        modifier = modifier.fillMaxWidth(),
        edgePadding = 16.dp,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
    ) {
        WorkspaceSection.entries.forEachIndexed { index, section ->
            val isSelected = section == selected
            val badgeCount = when (section) {
                WorkspaceSection.VISITS -> patient.visits.size
                WorkspaceSection.ULTRASOUND -> patient.ultrasoundScans.size
                WorkspaceSection.LABORATORY -> patient.labInvestigations.size
                WorkspaceSection.MEDICATIONS -> patient.medications.size
                WorkspaceSection.OBSTETRIC_HISTORY -> patient.previousPregnancies.size
                else -> null
            }
            Tab(
                selected = isSelected,
                onClick = { onSelect(section) },
                modifier = Modifier
                    .minimumTouchTarget()
                    .testTag("section-${section.name}"),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(section.labelResource()),
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (badgeCount != null && badgeCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = badgeCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}

private fun LazyListScope.workspaceContentItems(
    patient: DemoPatient,
    section: WorkspaceSection,
    timelineEvents: List<LongitudinalTimelineEvent>,
    state: AncState,
    onEvent: (AncEvent) -> Unit,
    onStartVisit: (String, String?) -> Unit,
    onViewPhotos: (DemoUltrasoundScan, Int) -> Unit = { _, _ -> },
    onAddAttachment: () -> Unit = {},
    onViewAttachment: (DemoAttachment) -> Unit = {},
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
    if (state.savedNotice) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(state.noticeResource()), color = MaterialTheme.colorScheme.tertiary)
                    TextButton(onClick = { onEvent(AncEvent.DismissNotice) }) {
                        Text(stringResource(R.string.close), maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
    if (section == WorkspaceSection.VISITS) {
        if (patient.status != "archived") {
            item {
                Button(
                    onClick = { onStartVisit(patient.id, null) },
                    modifier = Modifier.minimumTouchTarget().testTag("addVisit"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_visit), maxLines = 1, softWrap = false)
                }
            }
        }
        if (timelineEvents.isEmpty()) {
            item { Text(stringResource(R.string.no_visits), style = MaterialTheme.typography.bodyLarge) }
        } else {
            itemsIndexed(timelineEvents, key = { _, event -> event.eventKey }) { index, event ->
                val isFirst = index == 0
                val isLast = index == timelineEvents.lastIndex
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
                                    linkIntegrityWarning = event.linkIntegrityWarning,
                                )
                            }
                        }
                    }
                }
            }
        }
    } else if (section == WorkspaceSection.OTHER_HISTORIES) {
        item {
            HistoriesCard(patient)
        }
        if (patient.status != "archived") {
            item {
                Button(
                    onClick = {
                        onEvent(AncEvent.StartHistoriesEditor(patient.id, UUID.randomUUID().toString()))
                    },
                    modifier = Modifier.minimumTouchTarget().testTag("editHistories"),
                ) {
                    Text(stringResource(R.string.edit_other_histories))
                }
            }
        }
    } else if (section == WorkspaceSection.OBSTETRIC_HISTORY) {
        if (patient.status != "archived") {
            item {
                Button(
                    onClick = {
                        onEvent(
                            AncEvent.StartPregnancyEditor(
                                patientId = patient.id,
                                pregnancyId = null,
                                newPregnancyId = UUID.randomUUID().toString(),
                                editorSessionId = UUID.randomUUID().toString(),
                            ),
                        )
                    },
                    modifier = Modifier.minimumTouchTarget().testTag("addPreviousPregnancy"),
                ) {
                    Text(stringResource(R.string.add_previous_pregnancy))
                }
            }
        }
        if (patient.previousPregnancies.isEmpty()) {
            item { Text(stringResource(R.string.no_previous_pregnancies)) }
        } else {
            items(patient.previousPregnancies, key = { it.id }) { pregnancy ->
                PreviousPregnancyCard(pregnancy, patient.status != "archived") {
                    onEvent(
                        AncEvent.StartPregnancyEditor(
                            patientId = patient.id,
                            pregnancyId = pregnancy.id,
                            newPregnancyId = pregnancy.id,
                            editorSessionId = UUID.randomUUID().toString(),
                        ),
                    )
                }
            }
        }
    } else if (section == WorkspaceSection.DATING) {
        item {
            DatingCard(patient)
        }
        if (patient.status != "archived") {
            item {
                Button(
                    onClick = {
                        onEvent(
                            AncEvent.StartDatingEditor(
                                patientId = patient.id,
                                editorSessionId = UUID.randomUUID().toString(),
                            ),
                        )
                    },
                    modifier = Modifier.minimumTouchTarget().testTag("editDating"),
                ) {
                    Text(stringResource(R.string.edit_dating))
                }
            }
        }
    } else if (section == WorkspaceSection.ULTRASOUND) {
        if (patient.status != "archived") {
            item {
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
                    modifier = Modifier.minimumTouchTarget().testTag("addUltrasound"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_ultrasound), maxLines = 1, softWrap = false)
                }
            }
        }
        if (patient.ultrasoundScans.isEmpty()) {
            item { Text(stringResource(R.string.no_ultrasound_recorded)) }
        } else {
            items(patient.ultrasoundScans, key = { it.id }) { scan ->
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
                )
            }
        }
    } else if (section == WorkspaceSection.LABORATORY) {
        if (patient.status != "archived") {
            item {
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
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_lab), maxLines = 1, softWrap = false)
                }
            }
        }
        if (patient.labInvestigations.isEmpty()) {
            item { Text(stringResource(R.string.no_lab_recorded)) }
        } else {
            items(patient.labInvestigations, key = { it.id }) { lab ->
                LabCard(
                    lab = lab,
                    patient = patient,
                    canEdit = patient.status != "archived",
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
    } else if (section == WorkspaceSection.MEDICATIONS) {
        if (patient.status != "archived") {
            item {
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
                    modifier = Modifier.minimumTouchTarget().testTag("addMedication"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_medication), maxLines = 1, softWrap = false)
                }
            }
        }
        if (patient.medications.isEmpty()) {
            item { Text(stringResource(R.string.no_medications_recorded)) }
        } else {
            items(patient.medications, key = { it.id }) { med ->
                MedicationCard(med, patient.status != "archived") {
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
    } else if (section == WorkspaceSection.EPISODE_OVERVIEW) {
        item {
            EpisodeOverviewContent(patient, onEvent)
        }
    } else if (section == WorkspaceSection.DELIVERY) {
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
    } else if (section == WorkspaceSection.ATTACHMENTS) {
        item {
            AttachmentsSectionCard(
                patient = patient,
                onAddAttachment = onAddAttachment,
                onViewAttachment = onViewAttachment,
                onViewPhotos = onViewPhotos,
            )
        }
    } else {
        item { PlaceholderCard() }
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
    linkIntegrityWarning: String? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("visit-${visit.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(visit.date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!linkIntegrityWarning.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "⚠️ $linkIntegrityWarning",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            FactRow(R.string.blood_pressure, visit.bloodPressure, isEmphasized = true)
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

            // Inline linked ultrasound scan card
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
                                linkedScan.gestationalAgeWeeks != null && linkedScan.gestationalAgeDays != null -> "${linkedScan.gestationalAgeWeeks}w ${linkedScan.gestationalAgeDays}d"
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
                                                    .testTag("wsVisitPhotoChip-${linkedScan.id}-$idx")
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
                                    onClick = { onEvent(AncEvent.SelectSection(WorkspaceSection.ULTRASOUND)) },
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

@Composable
private fun HistoriesCard(patient: DemoPatient) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FactRow(R.string.medical_history, patient.medicalHistory)
            FactRow(R.string.surgical_history, patient.surgicalHistory)
            FactRow(R.string.family_history, patient.familyHistory)
            FactRow(R.string.allergy_history, patient.allergyHistory)
        }
    }
}

@Composable
private fun PreviousPregnancyCard(
    pregnancy: DemoPreviousPregnancy,
    canEdit: Boolean,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("pregnancy-${pregnancy.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.minimumTouchTarget().testTag("editPregnancy-${pregnancy.id}"),
                ) {
                    Text(stringResource(R.string.correct_previous_pregnancy))
                }
            }
        }
    }
}

@Composable
private fun DatingCard(patient: DemoPatient) {
    val dating = patient.dating
    Card(
        modifier = Modifier.fillMaxWidth().testTag("datingCard"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (dating == null) {
                Text(
                    text = stringResource(R.string.no_dating_recorded),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                return@Column
            }

            val ga = dating.calculateGestationalAge()
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
    linkIntegrityWarning: String? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("ultrasound-${scan.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!linkIntegrityWarning.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "⚠️ $linkIntegrityWarning",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(scan.date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (scan.linkedVisitId != null) {
                    val linkedVisit = patient.visits.firstOrNull { it.id == scan.linkedVisitId }
                    if (linkedVisit != null && canEdit) {
                        TextButton(
                            onClick = { onStartVisit(patient.id, scan.linkedVisitId) },
                            modifier = Modifier.testTag("openLinkedVisit-${scan.id}"),
                        ) {
                            Text(
                                text = "🔗 " + linkedVisit.date,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    } else {
                        Text(
                            text = "🔗 " + (linkedVisit?.date ?: stringResource(R.string.link_to_visit)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.standalone_scan),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
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
                FactRow(R.string.scan_attachments, stringResource(R.string.photos_count, scan.imageUris.size) + " 📷")
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
            if (canEdit) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.minimumTouchTarget().testTag("editUltrasound-${scan.id}"),
                ) {
                    Text(stringResource(R.string.edit_ultrasound))
                }
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.milestones),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                val ga = patient.dating?.calculateGestationalAge()
                FactRow(R.string.current_gestational_age, ga)
                FactRow(R.string.agreed_edd, patient.dating?.agreedEdd ?: patient.dating?.ultrasoundEdd)
                FactRow(R.string.manual_risk, patient.riskLevel)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag("overviewQuickActions"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.quick_actions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.episode_overview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                FactRow(R.string.total_visits, patient.visits.size.toString())
                val latestVisit = patient.visits.maxByOrNull { it.date }
                FactRow(R.string.latest_visit, latestVisit?.let { "${it.date} (${it.bloodPressure ?: "BP unrecorded"})" })
                val latestScan = patient.ultrasoundScans.maxByOrNull { it.date }
                FactRow(R.string.latest_scan, latestScan?.let { "${it.date} (${it.presentation ?: "Presentation unrecorded"})" })
                FactRow(R.string.laboratory, "${patient.labInvestigations.size} recorded (${patient.labInvestigations.count { it.isAbnormal }} abnormal)")
                FactRow(R.string.medications, "${patient.medications.count { it.status == "Active" }} active (${patient.medications.count { it.isProphylaxis }} prophylaxis)")

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
    Card(
        modifier = Modifier.fillMaxWidth().testTag("deliveryPlanningCard"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.delivery_planning),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(
                    onClick = onEditPatient,
                    modifier = Modifier.minimumTouchTarget(),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit Delivery Plan")
                }
            }
            Text(
                text = stringResource(R.string.delivery_planning_notice),
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodyMedium,
            )
            FactRow(R.string.blood_group, patient.bloodGroupText)
            FactRow(R.string.agreed_edd, patient.dating?.agreedEdd ?: patient.dating?.ultrasoundEdd)
            FactRow(R.string.current_gestational_age, patient.dating?.calculateGestationalAge())
            FactRow(R.string.manual_risk, patient.riskLevel)
            val latestScan = patient.ultrasoundScans.maxByOrNull { it.date }
            FactRow(R.string.fetal_presentation, latestScan?.presentation)
            FactRow(R.string.placental_site, latestScan?.placentalSite)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Financial Agreement & Delivery Fee Section
            Text(
                text = "Delivery Fee & Financial Agreement",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            val feeInfo = patient.obstetricNotes?.lineSequence()
                ?.firstOrNull { it.contains("fee", ignoreCase = true) || it.contains("agreement", ignoreCase = true) || it.contains("cost", ignoreCase = true) }

            FactRow(
                label = "Agreed Delivery Fee & Hospital",
                value = feeInfo ?: "To be agreed at clinic based on hospital and delivery mode (NVD / CS)",
            )
            FactRow(
                label = "Delivery Plan & Clinical Notes",
                value = patient.obstetricNotes?.takeIf { it.isNotBlank() } ?: "No additional notes recorded yet",
            )
        }
    }
}

@Composable
private fun LabCard(
    lab: DemoLabInvestigation,
    patient: DemoPatient,
    canEdit: Boolean,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("lab-${lab.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(lab.testName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (lab.isAbnormal) {
                    Text(
                        text = stringResource(R.string.abnormal_badge),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (!lab.labName.isNullOrBlank()) {
                FactRow(R.string.lab_name, lab.labName)
            }
            FactRow(R.string.test_date, lab.date)
            val ga = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, lab.date)
            if (ga?.isPostConception == true) {
                FactRow(R.string.calculated_ga_at_visit, ga.displayText)
            }
            FactRow(R.string.test_category, lab.testCategory)
            val resultWithUnit = if (lab.unit.isNullOrBlank()) lab.result else "${lab.result} ${lab.unit}"
            FactRow(
                label = stringResource(R.string.test_result),
                value = resultWithUnit,
                isEmphasized = lab.isAbnormal,
                valueColor = if (lab.isAbnormal) MaterialTheme.colorScheme.error else null,
            )
            FactRow(R.string.reference_range, lab.referenceRange)
            FactRow(R.string.notes, lab.notes)
            if (canEdit) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.minimumTouchTarget().testTag("editLab-${lab.id}"),
                ) {
                    Text(stringResource(R.string.edit_lab))
                }
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
    attention: String,
    onEdit: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val (level, narrative) = ClinicalAttentionLevel.parse(attention)
    val isDark = isSystemInDarkTheme()

    val (badgeRes, containerColor, contentColor) = when (level) {
        ClinicalAttentionLevel.CRITICAL -> Triple(
            R.string.critical_badge,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        ClinicalAttentionLevel.ALERT -> Triple(
            R.string.alert_badge,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        ClinicalAttentionLevel.NOTE -> Triple(
            R.string.note_badge,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }

    val items = remember(narrative) {
        narrative.split("\n")
            .flatMap { it.split("•") }
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotEmpty() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("clinicalAttentionBanner"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        color = contentColor,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = stringResource(badgeRes),
                            color = containerColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    if (!isExpanded) {
                        Text(
                            text = items.firstOrNull() ?: narrative,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            text = "${items.size} clinical alerts recorded",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Clinical Alerts",
                            tint = contentColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HorizontalDivider(color = contentColor.copy(alpha = 0.2f))
                    items.forEach { alertItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                            )
                            Text(
                                text = alertItem,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = selectedLevel == ClinicalAttentionLevel.CRITICAL,
                        onClick = { selectedLevel = ClinicalAttentionLevel.CRITICAL },
                        label = { Text("CRITICAL") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = selectedLevel == ClinicalAttentionLevel.ALERT,
                        onClick = { selectedLevel = ClinicalAttentionLevel.ALERT },
                        label = { Text("ALERT") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = selectedLevel == ClinicalAttentionLevel.NOTE,
                        onClick = { selectedLevel = ClinicalAttentionLevel.NOTE },
                        label = { Text("NOTE") },
                        modifier = Modifier.weight(1f),
                    )
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attachmentsSectionCard"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (patient.status != "archived") {
            Button(
                onClick = onAddAttachment,
                modifier = Modifier.minimumTouchTarget().testTag("addAttachmentBtn"),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Attachment", maxLines = 1, softWrap = false)
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
                                            android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
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
                                        Text("📷", fontSize = 24.sp)
                                    }
                                }

                                // Details
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = attachment.title ?: attachment.fileName,
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
                                            text = "Encrypted & Verified · ${attachment.capturedAt.take(10)}",
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
                        text = "Scan date: ${scan.date} (${scan.gestationalAgeWeeks?.let { "${it}w" } ?: "Gynecological"}) - ${scan.imageUris.size} photos",
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
                                    Text("📷", style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun MedicationCard(med: DemoMedicationRecord, canEdit: Boolean, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("medication-${med.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(med.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (med.isNonCompliant) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.non_compliant_badge),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    val badgeText = if (med.isProphylaxis) stringResource(R.string.prophylaxis_badge) else stringResource(R.string.therapeutic_badge)
                    Text(
                        text = badgeText,
                        color = if (med.isProphylaxis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            FactRow(R.string.dosage, med.dosage)
            FactRow(R.string.route, med.route)
            FactRow(R.string.frequency, med.frequency)
            FactRow(R.string.start_date, med.startDate)
            FactRow(R.string.end_date, med.endDate)
            FactRow(R.string.status, med.status)
            if (!med.adverseNotes.isNullOrBlank()) {
                FactRow(R.string.adverse_effects, med.adverseNotes)
            }
            FactRow(R.string.notes, med.notes)
            if (canEdit) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.minimumTouchTarget().testTag("editMedication-${med.id}"),
                ) {
                    Text(stringResource(R.string.edit_medication))
                }
            }
        }
    }
}

private fun AncState.noticeResource(): Int = when (notice) {
    SavedNotice.PATIENT -> R.string.patient_saved
    SavedNotice.HISTORIES -> R.string.histories_saved
    SavedNotice.PREGNANCY -> R.string.pregnancy_saved
    SavedNotice.ARCHIVED -> R.string.patient_archived_notice
    SavedNotice.UNARCHIVED -> R.string.patient_unarchived_notice
    SavedNotice.DATING -> R.string.dating_saved
    SavedNotice.ULTRASOUND -> R.string.ultrasound_saved
    SavedNotice.LABORATORY -> R.string.lab_saved
    SavedNotice.MEDICATIONS -> R.string.medication_saved
    SavedNotice.VISIT, null -> R.string.visit_saved
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
    WorkspaceSection.MEDICATIONS -> R.string.medications
    WorkspaceSection.DELIVERY -> R.string.delivery
    WorkspaceSection.ATTACHMENTS -> R.string.attachments
}
