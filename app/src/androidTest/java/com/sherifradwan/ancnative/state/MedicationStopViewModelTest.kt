package com.sherifradwan.ancnative.state

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_DISCONTINUED
import com.sherifradwan.ancnative.storage.ClinicalCommandOutcome
import com.sherifradwan.ancnative.storage.FakeVaultKeys
import com.sherifradwan.ancnative.storage.PatientCommandOutcome
import com.sherifradwan.ancnative.storage.PatientCommandPreparation
import com.sherifradwan.ancnative.storage.SaveMedicationCommand
import com.sherifradwan.ancnative.storage.SavePatientProfileCommand
import com.sherifradwan.ancnative.storage.VaultController
import com.sherifradwan.ancnative.storage.VaultFiles
import com.sherifradwan.ancnative.storage.VaultState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * The one-tap stop and its Undo through the real [AncViewModel] and its real save path (`saveMedicationQuick` into
 * `submitPreparedClinicalCommand`), on an isolated test vault with software keys: a real encrypted Room store, no PIN.
 */
@RunWith(AndroidJUnit4::class)
class MedicationStopViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val files = VaultFiles.isolatedTest(context, UUID.randomUUID().toString())
    private val keys = FakeVaultKeys()
    private lateinit var controller: VaultController
    private lateinit var vm: AncViewModel
    private val patientId = UUID.randomUUID().toString()
    private val medicationId = UUID.randomUUID().toString()

    @Before
    fun setUp() = runBlocking {
        controller = VaultController(context, files, keys)
        instrumentation.runOnMainSync { vm = AncViewModel(context.applicationContext as Application, controller) }
        controller.onForegrounded()
        awaitVault { it is VaultState.Uninitialized }
        controller.beginInitialization()
        authenticate()
        awaitVault { it is VaultState.Locked }
        controller.beginUnlock()
        authenticate()
        awaitVault { it is VaultState.Unlocked }
        seedPatientWithActiveMedication()
        awaitVm { s -> s.patient(patientId)?.medications?.any { it.id == medicationId } == true }
        onMain {
            vm.dispatch(AncEvent.OpenWorkspace(patientId))
            vm.dispatch(AncEvent.SelectSection(WorkspaceSection.MEDICATIONS))
        }
    }

    @After
    fun tearDown() = runBlocking {
        controller.lock()
        runCatching { awaitVault { it is VaultState.Locked || it is VaultState.Uninitialized } }
        keys.clearRegisteredTestAliases()
        files.cleanupRegisteredTestNamespace()
    }

    private val epoch get() = (controller.view.value.state as VaultState.Unlocked).sessionEpoch
    private fun stored(): DemoMedicationRecord =
        controller.view.value.snapshot!!.patients.first { it.id == patientId }.medications.first { it.id == medicationId }

    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private suspend fun authenticate() {
        val auth = awaitVault { it is VaultState.Authenticating } as VaultState.Authenticating
        controller.completeAuthentication(auth.request.id)
    }

    private suspend fun awaitVault(predicate: (VaultState) -> Boolean): VaultState = withTimeout(15_000) {
        controller.view.first { predicate(it.state) }.state
    }

    private suspend fun awaitVm(predicate: (AncState) -> Boolean): AncState = withTimeout(15_000) {
        vm.state.first(predicate)
    }

    private suspend fun seedPatientWithActiveMedication() {
        var s = AncState(patients = emptyList(), vaultState = VaultState.Unlocked(epoch))
        s = AncReducer.reduce(s, AncEvent.StartPatientEditor(null, patientId, "vm-seed-session"))
        s = AncReducer.reduce(s, AncEvent.PatientDraftChanged(PatientDraftField.CASE_ID, "VM-STOP-1"))
        s = AncReducer.reduce(s, AncEvent.PatientDraftChanged(PatientDraftField.DISPLAY_NAME, "Stop Test"))
        val prepared = SavePatientProfileCommand.prepare(s, UUID.randomUUID().toString(), Instant.now().toString())
        assertTrue("patient seed rejected: $prepared", prepared is PatientCommandPreparation.Ready)
        val created = controller.execute((prepared as PatientCommandPreparation.Ready).command, epoch) {}
        assertTrue("patient seed not committed: $created", created is PatientCommandOutcome.Committed)
        val patientRevision = controller.view.value.snapshot!!.patients.first { it.id == patientId }.revision
        val med = controller.execute(
            SaveMedicationCommand(
                editorSessionId = "vm-seed-med",
                operationUuid = UUID.randomUUID().toString(),
                patientId = patientId,
                expectedPatientRevision = patientRevision,
                medicationId = medicationId,
                expectedMedicationRevision = null,
                name = "VmIron",
                dosage = "200 mg",
                route = "Oral",
                frequency = "Once daily",
                startDate = "2026-01-01",
                endDate = "2027-01-01",
                status = MEDICATION_STATUS_ACTIVE,
                notes = null,
                isProphylaxis = false,
            ),
            epoch,
        ) {}
        assertTrue("medication seed not committed: $med", med is ClinicalCommandOutcome.Committed)
    }

    @Test
    fun stopSavesTheStoredRowAndUndoSavesItBackWithTheNewRevision() = runBlocking {
        val today = LocalDate.now().toString()
        val before = stored()
        onMain { vm.dispatch(AncEvent.StopMedication(patientId, medicationId, UUID.randomUUID().toString())) }

        val stopped = awaitVm { it.medicationStopUndo != null }
        val afterStop = stored()
        assertEquals(MEDICATION_STATUS_DISCONTINUED, afterStop.status)
        assertEquals("an existing end date is overwritten by today", today, afterStop.endDate)
        assertEquals(before.revision + 1, afterStop.revision)
        assertEquals("200 mg", afterStop.dosage)
        assertEquals(AncRoute.Workspace(patientId, WorkspaceSection.MEDICATIONS), stopped.route)
        assertNull(stopped.medicationDraft)
        assertNull(stopped.medicationQuickSave)
        assertTrue("the view model stamps the commit time", stopped.medicationStopUndo!!.committedAtMillis > 0L)

        val token = stopped.medicationStopUndo!!.token
        onMain { vm.dispatch(AncEvent.UndoMedicationStop(token, UUID.randomUUID().toString())) }
        val undone = awaitVm { s -> s.medicationQuickSave == null && s.patient(patientId)!!.medications.first { it.id == medicationId }.status == MEDICATION_STATUS_ACTIVE }
        val afterUndo = stored()
        assertEquals(MEDICATION_STATUS_ACTIVE, afterUndo.status)
        assertEquals("2027-01-01", afterUndo.endDate)
        assertEquals("the Undo saved at the stop's revision", afterStop.revision + 1, afterUndo.revision)
        assertNull(undone.medicationStopUndo)
        assertEquals(SavePhase.IDLE, undone.savePhase)
    }

    @Test
    fun aStoreRejectionLeavesTheRowActiveAndShowsTheCause() = runBlocking {
        val before = stored()
        // The row changes in the store while the screen still shows the old revision: both run in one main-thread
        // turn, so the view model cannot see the new snapshot before the stop tap and must send a stale revision.
        onMain {
            runBlocking {
                val bump = controller.execute(
                    SaveMedicationCommand(
                        editorSessionId = "vm-bump",
                        operationUuid = UUID.randomUUID().toString(),
                        patientId = patientId,
                        expectedPatientRevision = controller.view.value.snapshot!!.patients.first { it.id == patientId }.revision,
                        medicationId = medicationId,
                        expectedMedicationRevision = before.revision,
                        name = before.name, dosage = before.dosage, route = before.route, frequency = before.frequency,
                        startDate = before.startDate, endDate = before.endDate, status = before.status,
                        notes = "changed elsewhere", isProphylaxis = before.isProphylaxis,
                    ),
                    epoch,
                ) {}
                assertTrue(bump is ClinicalCommandOutcome.Committed)
            }
            assertEquals("the view model has not seen the change yet", before.revision, vm.state.value.patient(patientId)!!.medications.first { it.id == medicationId }.revision)
            vm.dispatch(AncEvent.StopMedication(patientId, medicationId, UUID.randomUUID().toString()))
        }

        val failed = awaitVm { it.savePhase == SavePhase.ERROR }
        assertEquals(SaveUiError.STALE_OR_CONFLICT, failed.saveError)
        assertTrue("kept for the Meds tab message", failed.medicationQuickSave != null)
        assertNull(failed.medicationStopUndo)
        assertEquals(MEDICATION_STATUS_ACTIVE, stored().status)
        assertEquals("changed elsewhere", stored().notes)

        onMain { vm.dispatch(AncEvent.DismissMedicationStopFailure) }
        val dismissed = awaitVm { it.medicationQuickSave == null }
        assertEquals(SavePhase.IDLE, dismissed.savePhase)
        assertNull(dismissed.saveError)
    }
}
