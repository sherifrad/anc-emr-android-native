package com.sherifradwan.ancnative.state

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.LABEL_FEES
import com.sherifradwan.ancnative.storage.FakeVaultKeys
import com.sherifradwan.ancnative.storage.PatientCommandOutcome
import com.sherifradwan.ancnative.storage.PatientCommandPreparation
import com.sherifradwan.ancnative.storage.SavePatientProfileCommand
import com.sherifradwan.ancnative.storage.VaultController
import com.sherifradwan.ancnative.storage.VaultFiles
import com.sherifradwan.ancnative.storage.VaultState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/**
 * The delivery plan sheet through the real [AncViewModel] and its real profile save path, on an isolated test vault with
 * software keys: a real encrypted Room store, no PIN. This is the path the phone runs; the screen tests use the reducer alone.
 */
@RunWith(AndroidJUnit4::class)
class DeliveryPlanViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val files = VaultFiles.isolatedTest(context, UUID.randomUUID().toString())
    private val keys = FakeVaultKeys()
    private lateinit var controller: VaultController
    private lateinit var vm: AncViewModel
    private val patientId = UUID.randomUUID().toString()
    private val freeNotes = "Delivery at Cleopatra Hospital\nFees: 20000 EGP\nPostpartum: IUD at 6 weeks"

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
        seedPatient()
        awaitVm { s -> s.patient(patientId) != null }
        onMain {
            vm.dispatch(AncEvent.OpenWorkspace(patientId))
            vm.dispatch(AncEvent.SelectSection(WorkspaceSection.DELIVERY))
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
    private val planTab get() = AncRoute.Workspace(patientId, WorkspaceSection.DELIVERY)
    private fun stored() = controller.view.value.snapshot!!.patients.first { it.id == patientId }
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

    private suspend fun seedPatient() {
        var s = AncState(patients = emptyList(), vaultState = VaultState.Unlocked(epoch))
        s = AncReducer.reduce(s, AncEvent.StartPatientEditor(null, patientId, "vm-seed-session"))
        s = AncReducer.reduce(s, AncEvent.PatientDraftChanged(PatientDraftField.CASE_ID, "VM-PLAN-1"))
        s = AncReducer.reduce(s, AncEvent.PatientDraftChanged(PatientDraftField.DISPLAY_NAME, "Plan Test"))
        s = AncReducer.reduce(s, AncEvent.PatientDraftChanged(PatientDraftField.AGE_TEXT, "30"))
        s = AncReducer.reduce(s, AncEvent.PatientDraftChanged(PatientDraftField.OBSTETRIC_NOTES, freeNotes))
        val prepared = SavePatientProfileCommand.prepare(s, UUID.randomUUID().toString(), Instant.now().toString())
        assertTrue("patient seed rejected: $prepared", prepared is PatientCommandPreparation.Ready)
        val created = controller.execute((prepared as PatientCommandPreparation.Ready).command, epoch) {}
        assertTrue("patient seed not committed: $created", created is PatientCommandOutcome.Committed)
    }

    @Test
    fun startingTheSheetThroughTheViewModelOpensTheDraftAndKeepsTheRoute() = runBlocking {
        onMain { vm.dispatch(AncEvent.StartDeliveryPlanEditor(patientId, UUID.randomUUID().toString())) }
        val opened = awaitVm { it.patientDraft?.deliveryPlan != null }
        assertEquals(planTab, opened.route)
        assertNotNull(opened.deliveryPlanSheetDraft(patientId))
        assertTrue(opened.patientDraft!!.deliveryPlan!!.rewritesNotes)
    }

    @Test
    fun savingAFieldThroughTheViewModelWritesLabelledNotesAndClosesTheSheetOnTheSameTab() = runBlocking {
        onMain { vm.dispatch(AncEvent.StartDeliveryPlanEditor(patientId, UUID.randomUUID().toString())) }
        awaitVm { it.patientDraft?.deliveryPlan != null }
        val before = stored()
        onMain { vm.dispatch(AncEvent.DeliveryPlanFieldChanged(DeliveryPlanField.FEES, "25000 EGP")) }
        awaitVm { it.patientDraft?.deliveryPlan?.touched == true }
        onMain { vm.dispatch(AncEvent.SubmitPatientProfile) }

        val done = awaitVm { it.patientDraft == null && it.patient(patientId)!!.obstetricNotes?.contains(LABEL_FEES) == true }
        assertEquals(planTab, done.route)
        assertEquals(SavePhase.IDLE, done.savePhase)
        val after = stored()
        assertTrue(after.obstetricNotes!!.contains("$LABEL_FEES: 25000 EGP"))
        assertEquals("only the notes change", before.displayName, after.displayName)
        assertEquals(before.caseId, after.caseId)
        assertEquals(before.dating, after.dating)
        assertEquals(before.revision + 1, after.revision)
        assertNull(done.draftRecoveryPrompt)
    }
}
