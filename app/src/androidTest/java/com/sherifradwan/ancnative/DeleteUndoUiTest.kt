package com.sherifradwan.ancnative

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.model.catalog.ClinicalCatalogRepository
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.UndoCoordinator
import com.sherifradwan.ancnative.state.WorkspaceSection
import com.sherifradwan.ancnative.storage.ClinicalCommand
import com.sherifradwan.ancnative.storage.ClinicalCommandFailure
import com.sherifradwan.ancnative.storage.ClinicalCommandOutcome
import com.sherifradwan.ancnative.storage.ClinicalCommandReceipt
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.storage.DeleteRecordCommand
import com.sherifradwan.ancnative.storage.RecordSnapshot
import com.sherifradwan.ancnative.storage.RecordUndoCommand
import com.sherifradwan.ancnative.storage.RestoreRecordCommand
import com.sherifradwan.ancnative.storage.VaultState
import com.sherifradwan.ancnative.ui.AncApp
import com.sherifradwan.ancnative.ui.AncTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The global delete / Undo UI (DeleteConfirmDialog, UndoBarHost, DeleteIconButton) mounted through the real AncApp,
 * driven by the real reducer and the real UndoCoordinator on an in-memory store (no vault, no PIN).
 */
@RunWith(AndroidJUnit4::class)
class DeleteUndoUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var state by mutableStateOf(AncState(patients = emptyList()))
    private lateinit var coordinator: UndoCoordinator

    @After
    fun tearDown() {
        coordinator.clear()
        scope.cancel()
    }

    private class Kind(val type: DeletableType, val section: WorkspaceSection, val card: String, val id: String)

    private val med = Kind(DeletableType.MEDICATION, WorkspaceSection.MEDICATIONS, "medication-", "ui-med-1")
    private val lab = Kind(DeletableType.LAB, WorkspaceSection.LABORATORY, "lab-", "ui-lab-1")
    private val us = Kind(DeletableType.ULTRASOUND, WorkspaceSection.ULTRASOUND, "ultrasound-", "ui-us-1")
    private val visitKind = Kind(DeletableType.VISIT, WorkspaceSection.VISITS, "visit-", "ui-visit-1")
    private val prevKind = Kind(DeletableType.PREVIOUS_PREGNANCY, WorkspaceSection.OBSTETRIC_HISTORY, "pregnancy-", "ui-prev-1")

    private fun medication(patientId: String, n: Int) = DemoMedicationRecord(
        id = "ui-med-$n", patientId = patientId, name = "UiDrug $n", dosage = "${n}00 mg", route = "Oral",
        frequency = "Twice daily", startDate = "2026-03-01", endDate = null, status = MEDICATION_STATUS_ACTIVE,
        notes = null, isProphylaxis = false, revision = 1,
    )

    private fun labResult(patientId: String, n: Int) = DemoLabInvestigation(
        id = "ui-lab-$n", patientId = patientId, date = "2026-03-02", testCategory = "Hematology", testName = "UiTest $n",
        result = "1$n", unit = "g/dL", referenceRange = null, isAbnormal = false, notes = null, revision = 1,
    )

    private fun ultrasound(patientId: String, n: Int) = DemoUltrasoundScan(
        id = "ui-us-$n", patientId = patientId, date = "2026-03-0$n", gestationalAgeWeeks = 20,
        gestationalAgeDays = n, fetalNumber = "Singleton", presentation = "Cephalic",
        placentalSite = "Posterior", amnioticFluid = "Normal", fetalHeartRate = "140",
        biometryBpd = null, biometryHc = null, biometryAc = null, biometryFl = null,
        estimatedFetalWeight = null, impression = "Scan $n", linkedVisitId = null,
        imageUris = emptyList(), revision = 1,
    )

    private fun visitRecord(patientId: String, n: Int) = DemoVisit(
        id = "ui-visit-$n", patientId = patientId, date = "2026-03-0$n", bloodPressure = "120/80",
        weight = "65 kg", findings = "Visit $n", notes = null, linkedUltrasoundId = null, revision = 1,
    )

    private fun attachment(patientId: String, n: Int) = DemoAttachment(
        id = "ui-att-$n", patientId = patientId, fileName = "att_ui_$n.webp",
        filePath = "patient_attachments/$patientId/att_ui_$n.webp", category = "ULTRASOUND",
        fileSizeBytes = 1024, sha256Hash = "00", title = "Scan $n", capturedAt = "2026-03-0$n",
        revision = 1,
    )

    private fun previousPregnancy(patientId: String, n: Int) = DemoPreviousPregnancy(
        id = "ui-prev-$n", patientId = patientId, modelVersion = 1, dateText = "2024-03-0$n",
        outcomeText = "Outcome $n", pregnancyType = "Singleton", otherDetails = null,
        placeAndDoctor = null, deliveryBy = "Vaginal", outcome = "LiveBirth",
        birthMethod = "Spontaneous", lossManagement = null, terminationMethod = null,
        provenanceKind = "CLINICAL", clinicianId = "dr1", recordedAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z", revision = 1,
    )

    private val base = DemoFixtures.patients.first()
    private val patientA = base.copy(
        medications = listOf(medication(base.id, 1), medication(base.id, 2)),
        labInvestigations = listOf(labResult(base.id, 1), labResult(base.id, 2)),
        ultrasoundScans = listOf(ultrasound(base.id, 1), ultrasound(base.id, 2)),
        visits = listOf(visitRecord(base.id, 1), visitRecord(base.id, 2)),
        previousPregnancies = listOf(previousPregnancy(base.id, 1), previousPregnancy(base.id, 2)),
        attachments = listOf(attachment(base.id, 1), attachment(base.id, 2)),
        status = "active",
    )
    private val patientB = DemoFixtures.patients[1].copy(
        medications = listOf(medication(DemoFixtures.patients[1].id, 1)),
        labInvestigations = listOf(labResult(DemoFixtures.patients[1].id, 1)),
        ultrasoundScans = listOf(ultrasound(DemoFixtures.patients[1].id, 1)),
        visits = listOf(visitRecord(DemoFixtures.patients[1].id, 1)),
        previousPregnancies = listOf(previousPregnancy(DemoFixtures.patients[1].id, 1)),
        attachments = listOf(attachment(DemoFixtures.patients[1].id, 1)),
    )

    /** In-memory stand-in for the vault: applies a delete/restore command to `state` and returns the receipt. */
    private var failAll = false

    /** While set, storage answers only after the test completes it (lets a test act while a command is in flight). */
    private var gate: CompletableDeferred<Unit>? = null

    private fun commit(cmd: ClinicalCommand): ClinicalCommandOutcome {
        if (failAll) return ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_NOT_FOUND)
        val undo = cmd as RecordUndoCommand
        val patient = state.patient(undo.patientId)!!
        val updated = when (undo) {
            is DeleteRecordCommand -> when (undo.type) {
                DeletableType.MEDICATION -> patient.copy(medications = patient.medications.filterNot { it.id == undo.recordId })
                DeletableType.LAB -> patient.copy(labInvestigations = patient.labInvestigations.filterNot { it.id == undo.recordId })
                DeletableType.ULTRASOUND -> patient.copy(ultrasoundScans = patient.ultrasoundScans.filterNot { it.id == undo.recordId })
                DeletableType.VISIT -> patient.copy(visits = patient.visits.filterNot { it.id == undo.recordId })
                DeletableType.PREVIOUS_PREGNANCY -> patient.copy(previousPregnancies = patient.previousPregnancies.filterNot { it.id == undo.recordId })
                DeletableType.ATTACHMENT -> patient.copy(attachments = patient.attachments.filterNot { it.id == undo.recordId })
            }
            is RestoreRecordCommand -> when (val record = undo.record) {
                is DemoMedicationRecord -> patient.copy(medications = patient.medications + record)
                is DemoLabInvestigation -> patient.copy(labInvestigations = patient.labInvestigations + record)
                is DemoUltrasoundScan -> patient.copy(ultrasoundScans = patient.ultrasoundScans + record)
                is DemoVisit -> patient.copy(visits = patient.visits + record)
                is DemoPreviousPregnancy -> patient.copy(previousPregnancies = patient.previousPregnancies + record)
                is DemoAttachment -> patient.copy(attachments = patient.attachments + record)
                else -> error("unexpected record ${record::class}")
            }
        }.copy(revision = patient.revision + 1)
        val receipt = ClinicalCommandReceipt(
            operationUuid = undo.operationUuid,
            patientId = undo.patientId,
            recordKind = undo.recordKind.name,
            recordId = undo.recordId,
            committedRevision = 1,
            committedPatientRevision = updated.revision,
            fingerprint = undo.computeFingerprint(),
        )
        return ClinicalCommandOutcome.Committed(
            RecordSnapshot(state.patients.map { if (it.id == patient.id) updated else it }),
            receipt,
        )
    }

    private fun host(route: AncRoute, patients: List<com.sherifradwan.ancnative.model.DemoPatient> = listOf(patientA, patientB)) {
        state = AncState(
            patients = patients,
            vaultState = VaultState.Unlocked(sessionEpoch = 6600L),
            route = route,
        )
        coordinator = UndoCoordinator(
            scope = scope,
            execute = { cmd, _ ->
                gate?.await()
                commit(cmd)
            },
            reduce = { state = AncReducer.reduce(state, it) },
            currentState = { state },
            snapshot = { RecordSnapshot(state.patients) },
        )
        val onEvent: (AncEvent) -> Unit = { event ->
            when (event) {
                AncEvent.ConfirmDelete -> {
                    val pending = state.deleteConfirmation
                    state = AncReducer.reduce(state, event)
                    if (pending != null && state.deleteConfirmation == null) {
                        coordinator.beginDelete(pending.type, pending.patientId, pending.recordId)
                    }
                }
                is AncEvent.UndoRequested -> coordinator.request(event.token)
                else -> state = AncReducer.reduce(state, event)
            }
        }
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AncTheme {
                    AncApp(
                        state = state,
                        onEvent = onEvent,
                        onStartVisit = { _, _ -> },
                        onSaveVisit = {},
                        onInitialize = {},
                        onUnlock = {},
                        onLock = {},
                        onRetryStorage = {},
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun workspace(kind: Kind) = host(AncRoute.Workspace(patientA.id, kind.section))

    private fun scrollTo(tag: String) {
        compose.onNodeWithTag("workspaceList", useUnmergedTree = true).performScrollToNode(hasTestTag(tag))
    }

    private fun exists(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
    private fun missing(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).assertDoesNotExist()
    private fun click(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).performClick()
        compose.waitForIdle()
    }

    private fun deleteTag(kind: Kind) = "delete-${kind.type.name.lowercase()}-${kind.id}"

    private fun openDialog(kind: Kind) {
        scrollTo(deleteTag(kind))
        click(deleteTag(kind))
        exists("deleteConfirmDialog")
    }

    private fun deleteConfirmsRemovesCardAndShowsUndoBar(kind: Kind) {
        workspace(kind)
        scrollTo(kind.card + kind.id)
        exists(kind.card + kind.id)
        missing("undoBar")
        openDialog(kind)
        click("deleteConfirm")
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("undoBar"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        missing("deleteConfirmDialog")
        missing(kind.card + kind.id)
        compose.onNodeWithTag("undoBar", useUnmergedTree = true).assertIsDisplayed()
        exists("undoButton")
    }

    private fun cancelKeepsTheCard(kind: Kind) {
        workspace(kind)
        openDialog(kind)
        click("deleteCancel")
        missing("deleteConfirmDialog")
        missing("undoBar")
        scrollTo(kind.card + kind.id)
        exists(kind.card + kind.id)
    }

    private fun undoRestoresTheCard(kind: Kind) {
        deleteConfirmsRemovesCardAndShowsUndoBar(kind)
        click("undoButton")
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("undoBar"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
        scrollTo(kind.card + kind.id)
        exists(kind.card + kind.id)
    }

    private fun barDisappearsAfterAboutEightSeconds(kind: Kind) {
        deleteConfirmsRemovesCardAndShowsUndoBar(kind)
        // The 8 s timer runs on the coordinator's real-time scope; wait for real.
        compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("undoBar"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
        missing(kind.card + kind.id)
    }

    @Test
    fun medication_deleteConfirmRemovesCardAndShowsUndoBar() = deleteConfirmsRemovesCardAndShowsUndoBar(med)

    @Test
    fun medication_cancelKeepsTheCard() = cancelKeepsTheCard(med)

    @Test
    fun medication_undoRestoresTheCard() = undoRestoresTheCard(med)

    @Test
    fun medication_barDisappearsAfterAboutEightSeconds() = barDisappearsAfterAboutEightSeconds(med)

    @Test
    fun lab_deleteConfirmRemovesCardAndShowsUndoBar() = deleteConfirmsRemovesCardAndShowsUndoBar(lab)

    @Test
    fun lab_cancelKeepsTheCard() = cancelKeepsTheCard(lab)

    @Test
    fun lab_undoRestoresTheCard() = undoRestoresTheCard(lab)

    @Test
    fun lab_barDisappearsAfterAboutEightSeconds() = barDisappearsAfterAboutEightSeconds(lab)

    @Test
    fun ultrasound_deleteConfirmRemovesCardAndShowsUndoBar() = deleteConfirmsRemovesCardAndShowsUndoBar(us)

    @Test
    fun ultrasound_cancelKeepsTheCard() = cancelKeepsTheCard(us)

    @Test
    fun ultrasound_undoRestoresTheCard() = undoRestoresTheCard(us)

    @Test
    fun ultrasound_barDisappearsAfterAboutEightSeconds() = barDisappearsAfterAboutEightSeconds(us)

    @Test
    fun visit_deleteConfirmRemovesCardAndShowsUndoBar() = deleteConfirmsRemovesCardAndShowsUndoBar(visitKind)

    @Test
    fun visit_cancelKeepsTheCard() = cancelKeepsTheCard(visitKind)

    @Test
    fun visit_undoRestoresTheCard() = undoRestoresTheCard(visitKind)

    @Test
    fun visit_barDisappearsAfterAboutEightSeconds() = barDisappearsAfterAboutEightSeconds(visitKind)

    @Test
    fun previousPregnancy_deleteConfirmRemovesCardAndShowsUndoBar() = deleteConfirmsRemovesCardAndShowsUndoBar(prevKind)

    @Test
    fun previousPregnancy_cancelKeepsTheCard() = cancelKeepsTheCard(prevKind)

    @Test
    fun previousPregnancy_undoRestoresTheCard() = undoRestoresTheCard(prevKind)

    @Test
    fun previousPregnancy_barDisappearsAfterAboutEightSeconds() = barDisappearsAfterAboutEightSeconds(prevKind)

    // Attachments delete from inside the viewer, not from a card affordance, so they need their own driver.

    private fun openAttachmentViewer(id: String) {
        scrollTo("attachmentCard-$id")
        click("attachmentCard-$id")
        exists("deleteAttachmentBtn")
    }

    @Test
    fun attachment_deleteConfirmRemovesCardAndShowsUndoBar() {
        host(AncRoute.Workspace(patientA.id, WorkspaceSection.ATTACHMENTS))
        openAttachmentViewer("ui-att-1")
        click("deleteAttachmentBtn")
        exists("deleteConfirmDialog")
        // The dialog must name the record the way a clinician reads it, never the internal file name.
        assertTrue(
            "the confirm dialog must show the attachment label",
            compose.onAllNodes(hasText("Scan 1", substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty(),
        )
        click("deleteConfirm")
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("undoBar"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        missing("deleteConfirmDialog")
        missing("attachmentCard-ui-att-1")
        missing("deleteAttachmentBtn")
        exists("undoButton")
    }

    @Test
    fun attachment_cancelKeepsTheViewerOpen() {
        host(AncRoute.Workspace(patientA.id, WorkspaceSection.ATTACHMENTS))
        openAttachmentViewer("ui-att-1")
        click("deleteAttachmentBtn")
        click("deleteCancel")
        missing("deleteConfirmDialog")
        missing("undoBar")
        // The viewer stays: a cancelled confirmation returns the clinician to the image they were looking at.
        exists("deleteAttachmentBtn")
        exists("attachmentCard-ui-att-1")
    }

    @Test
    fun attachment_undoRestoresTheCard() {
        host(AncRoute.Workspace(patientA.id, WorkspaceSection.ATTACHMENTS))
        openAttachmentViewer("ui-att-1")
        click("deleteAttachmentBtn")
        click("deleteConfirm")
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("undoBar"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        click("undoButton")
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("undoBar"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
        scrollTo("attachmentCard-ui-att-1")
        exists("attachmentCard-ui-att-1")
    }

    @Test
    fun undoButtonIsDisabledWhileADeleteRuns() {
        workspace(med)
        compose.runOnIdle {
            state = state.copy(
                undoOffer = com.sherifradwan.ancnative.state.UndoOffer(
                    token = "ui-t1", patientId = patientA.id, type = DeletableType.LAB,
                    action = com.sherifradwan.ancnative.state.UndoAction.DELETE, label = "UiTest 1",
                    phase = com.sherifradwan.ancnative.state.UndoPhase.READY,
                ),
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("undoButton", useUnmergedTree = true).assertIsEnabled()
        compose.runOnIdle { state = state.copy(deleteRunning = true) }
        compose.waitForIdle()
        compose.onNodeWithTag("undoButton", useUnmergedTree = true).assertIsNotEnabled()
    }

    @Test
    fun deletingAMedicationLeavesLabsAndTheOtherPatientUntouched() {
        workspace(med)
        openDialog(med)
        click("deleteConfirm")
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("undoBar"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        val a = state.patient(patientA.id)!!
        assertEquals(listOf("ui-med-2"), a.medications.map { it.id })
        assertEquals(patientA.labInvestigations, a.labInvestigations)
        assertEquals(patientB, state.patient(patientB.id))
    }

    @Test
    fun deleteNoticeSaysDeletedNotSaved() {
        workspace(med)
        openDialog(med)
        click("deleteConfirm")
        compose.waitUntil(5_000) { state.savedNotice }
        compose.onNodeWithTag("workspaceList", useUnmergedTree = true)
            .performScrollToNode(hasText("Medication deleted."))
        compose.onAllNodes(hasText("Medication record saved."), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun archivedPatientHasNoDeleteIcons() {
        val archived = patientA.copy(status = "archived")
        host(AncRoute.Workspace(archived.id, med.section), listOf(archived, patientB))
        scrollTo(med.card + med.id)
        missing(deleteTag(med))
    }

    @Test
    fun undoBarShowsOverBlankMedicationEditorWithoutCoveringTheFormButtons() {
        host(AncRoute.Workspace(patientA.id, med.section))
        compose.runOnIdle {
            state = AncReducer.reduce(
                state,
                AncEvent.StartMedicationEditor(
                    patientId = patientA.id,
                    medicationId = null,
                    newMedicationId = "ui-med-new",
                    editorSessionId = "ui-session",
                    keepSavedNotice = true,
                ),
            )
            val saved = patientA.medications.first()
            coordinator.offerSaveUndo(
                type = DeletableType.MEDICATION,
                patientId = patientA.id,
                recordId = saved.id,
                receipt = ClinicalCommandReceipt("op", patientA.id, "MEDICATION_CREATE", saved.id, 1, patientA.revision + 1, "fp"),
                savedRecord = saved,
            )
        }
        compose.waitForIdle()
        exists("medName")
        compose.onNodeWithTag("undoBar", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("undoButton", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("addAnotherMedication", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("saveMedication", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        val barTop = compose.onNodeWithTag("undoBar", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        val buttonBottom = compose.onNodeWithTag("saveMedication", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.bottom
        assertTrue("bar ($barTop) must not cover the Save button (bottom $buttonBottom)", buttonBottom <= barTop + 1f)
    }

    @Test
    fun longPresavedMedicationListCollapsesToFiveWithAShowAllToggle() {
        host(AncRoute.Workspace(patientA.id, med.section))
        compose.runOnIdle {
            state = AncReducer.reduce(
                state,
                AncEvent.StartMedicationEditor(patientA.id, null, "ui-med-new", "ui-session"),
            )
        }
        compose.waitForIdle()
        val total = ClinicalCatalogRepository.getInstance(compose.activity).medicationCatalogItems.value.size
        if (total > 5) {
            compose.onNodeWithTag("presavedToggle", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            exists("medPreset-4")
            missing("medPreset-5")
            click("presavedToggle")
            exists("medPreset-${total - 1}")
            // The editor has a pinned action bar, so the shorter form area needs a scroll before the click.
            compose.onNodeWithTag("presavedToggle", useUnmergedTree = true).performScrollTo()
            click("presavedToggle")
            missing("medPreset-5")
        } else {
            missing("presavedToggle")
        }
    }

    @Test
    fun deleteIconDescriptionNamesTheRecord() {
        workspace(med)
        scrollTo(deleteTag(med))
        compose.onNodeWithTag(deleteTag(med), useUnmergedTree = true).assertExists()
        compose.onAllNodes(hasContentDescription("Delete medication: UiDrug 1", substring = true), useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun undoBarNamesThePatient() {
        deleteConfirmsRemovesCardAndShowsUndoBar(med)
        compose.onNodeWithTag("undoBar", useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodes(hasText(patientA.displayName, substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun failedDeleteShowsAMessageAndKeepsTheCard() {
        workspace(med)
        failAll = true
        openDialog(med)
        click("deleteConfirm")
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("deleteMessage"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        missing("undoBar")
        compose.onNodeWithTag("deleteMessage", useUnmergedTree = true).assertIsDisplayed()
        scrollTo(med.card + med.id)
        exists(med.card + med.id)
        click("deleteMessageDismiss")
        missing("deleteMessage")
    }

    @Test
    fun failedUndoSaysTheRecordIsStillDeleted() {
        deleteConfirmsRemovesCardAndShowsUndoBar(med)
        failAll = true
        click("undoButton")
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("deleteMessage"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText("is still deleted", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
        missing(med.card + med.id)
    }

    @Test
    fun undoBarDisappearsWhenAnotherPatientIsOpened() {
        deleteConfirmsRemovesCardAndShowsUndoBar(med)
        compose.runOnIdle { state = AncReducer.reduce(state, AncEvent.OpenWorkspace(patientB.id)) }
        compose.waitForIdle()
        missing("undoBar")
    }

    @Test
    fun deleteFailureIsStillShownAfterAnotherPatientIsOpened() {
        workspace(med)
        failAll = true
        val held = CompletableDeferred<Unit>()
        gate = held
        openDialog(med)
        click("deleteConfirm")
        compose.runOnIdle { state = AncReducer.reduce(state, AncEvent.OpenWorkspace(patientB.id)) }
        compose.waitForIdle()
        held.complete(Unit)
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("deleteMessage"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("deleteMessage", useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodes(hasText(patientA.displayName, substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
        compose.onAllNodes(hasText("still in the record", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
        click("deleteMessageDismiss")
        missing("deleteMessage")
    }

    @Test
    fun medicationEditorDoesNotOpenWhileItsSaveIsBeingUndone() {
        host(AncRoute.Workspace(patientA.id, med.section))
        val saved = patientA.medications.first()
        compose.runOnIdle {
            coordinator.offerSaveUndo(
                type = DeletableType.MEDICATION,
                patientId = patientA.id,
                recordId = saved.id,
                receipt = ClinicalCommandReceipt("op", patientA.id, "MEDICATION_CREATE", saved.id, 1, patientA.revision + 1, "fp"),
                savedRecord = saved,
            )
        }
        compose.waitForIdle()
        val held = CompletableDeferred<Unit>()
        gate = held
        click("undoButton")
        scrollTo("editMedication-${saved.id}")
        click("editMedication-${saved.id}")
        missing("medName")
        assertTrue(state.route is AncRoute.Workspace)
        held.complete(Unit)
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("undoBar"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
        assertTrue(state.route is AncRoute.Workspace)
    }
}
