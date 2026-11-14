package com.sherifradwan.ancnative.ui.patienthub

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.severeBpAttention
import com.sherifradwan.ancnative.security.SecuritySettingsRepository
import com.sherifradwan.ancnative.security.SecuritySettingsState
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.PatientDraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.deliveryPlanSheetDraft
import com.sherifradwan.ancnative.state.medicationSheetDraft
import com.sherifradwan.ancnative.state.editPatientEvent
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.AddAttachmentDialog
import com.sherifradwan.ancnative.ui.ClinicalAttachmentViewerDialog
import com.sherifradwan.ancnative.ui.UltrasoundPhotoViewerDialog
import com.sherifradwan.ancnative.ui.UndoBarHost
import com.sherifradwan.ancnative.ui.patienthub.components.ClinicalAttentionDialog
import com.sherifradwan.ancnative.ui.patienthub.components.DeliveryPlanSheet
import com.sherifradwan.ancnative.ui.patienthub.components.MedicationSheet
import com.sherifradwan.ancnative.ui.patienthub.tabs.LabsTab
import com.sherifradwan.ancnative.ui.patienthub.tabs.MedsTab
import com.sherifradwan.ancnative.ui.patienthub.tabs.OverviewTab
import com.sherifradwan.ancnative.ui.patienthub.tabs.PlanTab
import com.sherifradwan.ancnative.ui.patienthub.tabs.ScansTab
import com.sherifradwan.ancnative.ui.patienthub.tabs.VisitsTab
import com.sherifradwan.ancnative.ui.report.PatientTwoPageReportDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@Composable
fun PatientHubScreen(
    state: AncState,
    route: AncRoute.Workspace,
    onEvent: (AncEvent) -> Unit,
    onStartVisit: (String, String?) -> Unit,
    asOfDate: LocalDate = LocalDate.now(),
    securitySettings: SecuritySettingsState? = null,
    modifier: Modifier = Modifier,
) {
    val patient = state.patient(route.patientId) ?: run {
        Box(modifier = modifier.fillMaxSize().testTag("patientHubNotFound"))
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val initialTab = PatientHubTab.fromSection(route.section)
    val pagerState = rememberPagerState(
        initialPage = initialTab.ordinal,
        pageCount = { PatientHubTab.entries.size },
    )

    // Route section -> Pager synchronization
    LaunchedEffect(route.section) {
        val targetTab = PatientHubTab.fromSection(route.section)
        if (pagerState.currentPage != targetTab.ordinal) {
            pagerState.animateScrollToPage(targetTab.ordinal)
        }
    }

    // Pager swipe -> State synchronization (loop-free via settledPage). The effect outlives route changes, so it
    // reads the latest route and callback: with the first route, a return to the opening tab was never reported.
    val currentRoute by rememberUpdatedState(route)
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { page ->
                val settledTab = PatientHubTab.entries[page]
                val currentTabNow = PatientHubTab.fromSection(currentRoute.section)
                if (settledTab != currentTabNow) {
                    currentOnEvent(AncEvent.SelectSection(settledTab.defaultSection))
                }
            }
    }

    val density = LocalDensity.current
    val collapsedBarHeightPx = remember(density) { with(density) { 56.dp.toPx() } }
    val headerState = remember(patient.id) {
        PatientHubHeaderState(collapsedHeightPx = collapsedBarHeightPx)
    }

    val nestedScrollConnection = remember(headerState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // When scrolling UP (delta < 0), collapse header FIRST before child content scrolls
                if (delta < 0f && headerState.offsetPx > -headerState.maxCollapsePx) {
                    val oldOffset = headerState.offsetPx
                    val newOffset = (oldOffset + delta).coerceIn(-headerState.maxCollapsePx, 0f)
                    headerState.offsetPx = newOffset
                    val consumed = newOffset - oldOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val delta = available.y
                // When scrolling DOWN (delta > 0), expand header ONLY AFTER child list
                // has reached the very top (available.y remains unconsumed by child)
                if (delta > 0f && headerState.offsetPx < 0f) {
                    val oldOffset = headerState.offsetPx
                    val newOffset = (oldOffset + delta).coerceIn(-headerState.maxCollapsePx, 0f)
                    headerState.offsetPx = newOffset
                    val consumed = newOffset - oldOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }
        }
    }

    val severeBp = remember(patient.visits) { severeBpAttention(patient.visits) }
    val canEdit = patient.status != "archived"
    val isSaveBlocked = state.savePhase == SavePhase.PENDING ||
        state.savePhase == SavePhase.RECONCILING ||
        state.savePhase == SavePhase.UNRESOLVED
    val canModifyData = canEdit && !state.hasDirtyDraft && !isSaveBlocked && !state.deleteRunning
    val medicationDraft = state.medicationSheetDraft()
    val deliveryPlanDraft = state.deliveryPlanSheetDraft(patient.id)

    // Dialog state holders
    var viewingPhotoScan by remember(patient.id) { mutableStateOf<DemoUltrasoundScan?>(null) }
    var viewingPhotoIndex by remember(patient.id) { mutableIntStateOf(0) }
    var viewingAttachment by remember(patient.id) { mutableStateOf<DemoAttachment?>(null) }
    var showAddAttachmentDialog by remember(patient.id) { mutableStateOf(false) }
    var showReportDialog by remember(patient.id) { mutableStateOf(false) }
    var showAttentionDialog by remember(patient.id) { mutableStateOf(false) }

    val context = LocalContext.current
    val effectiveSecuritySettings = securitySettings ?: remember(patient.id) {
        SecuritySettingsRepository.getInstance(context).settings.value
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("workspaceScreen")
            .nestedScroll(nestedScrollConnection),
    ) {
        val horizontalPadding = maxOf(0.dp, (maxWidth - 840.dp) / 2)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
        ) {
            // Collapsible Header (Hero to Sticky Bar)
            PatientHubHeader(
                patient = patient,
                headerState = headerState,
                selectedTab = PatientHubTab.entries[pagerState.currentPage],
                onTabSelected = { tab ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(tab.ordinal)
                    }
                },
                severeBp = severeBp,
                onBack = { onEvent(AncEvent.Back) },
                onEditPatient = {
                    editPatientEvent(patient, UUID.randomUUID().toString(), UUID.randomUUID().toString())?.let(onEvent)
                },
                onOpenReport = { showReportDialog = true },
                onOpenAttentionDialog = { showAttentionDialog = true },
            )

            // Horizontal Pager with 6 modular tabs
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (PatientHubTab.entries[page]) {
                    PatientHubTab.OVERVIEW -> {
                        OverviewTab(
                            patient = patient,
                            canEdit = canModifyData,
                            onEvent = onEvent,
                            onAddAttachment = { showAddAttachmentDialog = true },
                            onViewAttachment = { viewingAttachment = it },
                            onEditAttention = if (canModifyData) { { showAttentionDialog = true } } else null,
                        )
                    }
                    PatientHubTab.VISITS -> {
                        VisitsTab(
                            patient = patient,
                            canEdit = canModifyData,
                            onStartVisit = onStartVisit,
                            onEvent = onEvent,
                        )
                    }
                    PatientHubTab.SCANS -> {
                        ScansTab(
                            patient = patient,
                            canEdit = canModifyData,
                            onEvent = onEvent,
                            onViewPhotos = { scan, index ->
                                viewingPhotoScan = scan
                                viewingPhotoIndex = index
                            },
                        )
                    }
                    PatientHubTab.LABS -> {
                        LabsTab(
                            patient = patient,
                            canEdit = canModifyData,
                            onEvent = onEvent,
                            onAddInvestigation = {
                                onEvent(AncEvent.StartLabBatchRequest(patientId = patient.id))
                            },
                        )
                    }
                    PatientHubTab.MEDS -> {
                        MedsTab(
                            patient = patient,
                            canEdit = canEdit,
                            actionsAllowed = canModifyData,
                            medicationDraft = medicationDraft,
                            savePhase = state.savePhase,
                            saveError = state.saveError,
                            quickSave = state.medicationQuickSave,
                            stopUndo = state.medicationStopUndo,
                            onEvent = onEvent,
                        )
                    }
                    PatientHubTab.PLAN -> {
                        PlanTab(
                            patient = patient,
                            canEdit = canModifyData,
                            onEvent = onEvent,
                            onAddAttachment = { showAddAttachmentDialog = true },
                            onViewAttachment = { viewingAttachment = it },
                        )
                    }
                }
            }
        }

        // The medication sheet sits above the pager: its scrim blocks tab swipes, and the route never changes.
        if (medicationDraft != null) {
            MedicationSheet(
                draft = medicationDraft,
                savePhase = state.savePhase,
                saveError = state.saveError,
                touched = medicationDraft.touched,
                onEvent = onEvent,
                footer = { UndoBarHost(state, onEvent) },
            )
        }

        // The delivery plan sheet sits above the pager the same way.
        if (deliveryPlanDraft != null) {
            DeliveryPlanSheet(
                draft = deliveryPlanDraft,
                savePhase = state.savePhase,
                saveError = state.saveError,
                onEvent = onEvent,
                footer = { UndoBarHost(state, onEvent) },
            )
        }

        // Modal Dialog Hosts
        if (viewingPhotoScan != null) {
            UltrasoundPhotoViewerDialog(
                patientId = patient.id,
                scan = viewingPhotoScan!!,
                initialIndex = viewingPhotoIndex,
                onDismiss = { viewingPhotoScan = null },
            )
        }

        // Close only when the record is really gone; a cancelled delete confirmation keeps the viewer open.
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
                securitySettings = effectiveSecuritySettings,
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
