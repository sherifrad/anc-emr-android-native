package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.*
import com.sherifradwan.ancnative.storage.VaultState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The delivery plan sheet in the reducer (design: delivery plan sheet, sections 5.1 to 5.3). */
class DeliveryPlanReducerTest {
    private val planTab = AncRoute.Workspace("p1")
    private val freeNotes = "Delivery agreed with City Hospital. Fees 5000 EGP, review at six weeks."

    private fun composedNotes(
        hospital: String = "City Hospital",
        fees: String = "5000 EGP",
        plan: String = "NVD",
        postpartum: String = "Review in 6 weeks",
    ) = composeDeliveryNotes(
        DeliveryNoteSections(hospital = hospital, fees = fees, plan = plan, postpartum = postpartum),
    )

    private fun patient(
        notes: String? = freeNotes,
        status: String = "active",
        dating: DemoPatientDating? = null,
        revision: Long = 5L,
    ) = DemoPatient(
        id = "p1", caseId = "P1", displayName = "Patient One", dateOfBirth = "1995-01-01", phone = "01000000000",
        visits = emptyList(), status = status, obstetricNotes = notes, dating = dating, revision = revision,
    )

    private fun base(vararg patients: DemoPatient = arrayOf(patient())) =
        AncState(patients = patients.toList(), vaultState = VaultState.Unlocked(1L), route = planTab)

    private fun AncState.reduce(event: AncEvent) = AncReducer.reduce(this, event)

    private fun start(state: AncState = base(), session: String = "s-plan") =
        state.reduce(AncEvent.StartDeliveryPlanEditor("p1", session))

    private fun AncState.change(field: DeliveryPlanField, value: String) =
        reduce(AncEvent.DeliveryPlanFieldChanged(field, value))

    private fun AncState.fields(): DeliveryPlanFields =
        requireNotNull(requireNotNull(patientDraft).deliveryPlan)

    @Test
    fun `start builds the draft and keeps the route`() {
        val next = start()
        val draft = requireNotNull(next.patientDraft)
        assertEquals(planTab, next.route)
        assertEquals("s-plan", draft.editorSessionId)
        assertEquals("p1", draft.patientId)
        assertEquals(draft.original, draft.values)
        assertEquals(base().patients.first().toPatientDraftValues(), draft.values)
        assertNotNull(draft.deliveryPlan)
    }

    @Test
    fun `start flags notes that are not composed yet`() {
        assertTrue(start().fields().rewritesNotes)
    }

    @Test
    fun `start does not flag composed notes`() {
        val next = start(base(patient(notes = composedNotes())))
        assertFalse(next.fields().rewritesNotes)
        assertEquals(composedNotes(), requireNotNull(next.patientDraft).values.obstetricNotes)
    }

    @Test
    fun `start does nothing for an archived patient`() {
        val next = start(base(patient(status = "archived")))
        assertNull(next.patientDraft)
        assertEquals(planTab, next.route)
    }

    @Test
    fun `start does nothing for a patient that is not loaded`() {
        val next = base().reduce(AncEvent.StartDeliveryPlanEditor("p9", "s-plan"))
        assertNull(next.patientDraft)
    }

    @Test
    fun `start does nothing while a save is in progress`() {
        val state = base().copy(savePhase = SavePhase.RECONCILING)
        assertNull(start(state).patientDraft)
    }

    @Test
    fun `start does nothing while another draft is dirty`() {
        val dirty = base()
            .reduce(AncEvent.StartPatientEditor("p1", "new-1", "s-profile"))
            .reduce(AncEvent.PatientDraftChanged(PatientDraftField.DISPLAY_NAME, "Patient One Edited"))
        assertTrue(dirty.hasDirtyDraft)
        assertNull(start(dirty).patientDraft?.deliveryPlan)
    }

    @Test
    fun `a field change keeps the other three fields`() {
        val started = start()
        val before = started.fields()
        val after = started.change(DeliveryPlanField.FEES, "6500 EGP").fields()
        assertEquals("6500 EGP", after.fees)
        assertEquals(before.hospital, after.hospital)
        assertEquals(before.plan, after.plan)
        assertEquals(before.postpartum, after.postpartum)
        assertTrue(after.touched)
    }

    @Test
    fun `a field change writes the composed notes into the draft`() {
        val next = start().change(DeliveryPlanField.PLAN, "Elective caesarean")
        val draft = requireNotNull(next.patientDraft)
        assertEquals(composeDeliveryNotes(next.fields().toSections()), draft.values.obstetricNotes)
        assertEquals(emptySet<PatientValidationError>(), draft.errors)
        assertEquals(SavePhase.IDLE, next.savePhase)
        assertNull(next.saveError)
    }

    @Test
    fun `typing and deleting leaves the field untouched`() {
        val started = start()
        val opened = requireNotNull(started.patientDraft)
        val original = requireNotNull(opened.deliveryPlan).postpartum
        val typed = started.change(DeliveryPlanField.POSTPARTUM, "temporary")
        assertTrue(typed.fields().touched)
        val undone = typed.change(DeliveryPlanField.POSTPARTUM, original)
        assertFalse(undone.fields().touched)
        assertEquals(opened.original.obstetricNotes, requireNotNull(undone.patientDraft).values.obstetricNotes)
        assertFalse(undone.hasDirtyDraft)
    }

    @Test
    fun `a field change is ignored while a save is in progress`() {
        val started = start()
        val before = started.fields()
        val next = started.copy(savePhase = SavePhase.RECONCILING).change(DeliveryPlanField.FEES, "7000 EGP")
        assertEquals(before, next.fields())
    }

    @Test
    fun `a field change is ignored without a delivery plan draft`() {
        val profile = base().reduce(AncEvent.StartPatientEditor("p1", "new-1", "s-profile"))
        val next = profile.change(DeliveryPlanField.FEES, "7000 EGP")
        assertEquals(profile.patientDraft, next.patientDraft)
        assertNull(requireNotNull(next.patientDraft).deliveryPlan)
    }

    @Test
    fun `a committed sheet save keeps the route`() {
        val started = start()
        val token = PendingSaveToken(operationUuid = "op-plan", editorSessionId = "s-plan")
        val next = started.copy(pendingSave = token).reduce(
            AncEvent.RecordCommitted(
                token = token,
                patients = listOf(patient(notes = composedNotes())),
                destination = AncRoute.PatientList,
                notice = SavedNotice.VISIT,
            ),
        )
        assertEquals(planTab, next.route)
        assertNull(next.patientDraft)
    }

    @Test
    fun `a committed profile save moves the route`() {
        val profile = base().reduce(AncEvent.StartPatientEditor("p1", "new-1", "s-profile"))
        val token = PendingSaveToken(operationUuid = "op-profile", editorSessionId = "s-profile")
        val next = profile.copy(pendingSave = token).reduce(
            AncEvent.RecordCommitted(
                token = token,
                patients = listOf(patient()),
                destination = planTab,
                notice = SavedNotice.VISIT,
            ),
        )
        assertEquals(planTab, next.route)
    }

    @Test
    fun `Cancel closes an untouched sheet and keeps the route`() {
        val next = start().reduce(AncEvent.CancelEditor)
        assertNull(next.patientDraft)
        assertEquals(planTab, next.route)
    }

    @Test
    fun `Back on a touched sheet asks before discarding`() {
        val changed = start().change(DeliveryPlanField.PLAN, "Elective caesarean")
        val next = changed.reduce(AncEvent.Back)
        assertTrue(next.showDiscardDialog)
        assertEquals(PendingAction.Navigate(planTab), next.pendingAction)
        assertNotNull(next.patientDraft)
    }

    @Test
    fun `Back on an untouched sheet closes it at once`() {
        val next = start().reduce(AncEvent.Back)
        assertNull(next.patientDraft)
        assertFalse(next.showDiscardDialog)
        assertEquals(planTab, next.route)
    }

    @Test
    fun `ConfirmDiscard closes the sheet and keeps the route`() {
        val asked = start()
            .change(DeliveryPlanField.PLAN, "Elective caesarean")
            .reduce(AncEvent.Back)
        val next = asked.reduce(AncEvent.ConfirmDiscard)
        assertNull(next.patientDraft)
        assertEquals(planTab, next.route)
    }

    @Test
    fun `KeepEditing leaves the sheet open`() {
        val asked = start()
            .change(DeliveryPlanField.PLAN, "Elective caesarean")
            .reduce(AncEvent.Back)
        val next = asked.reduce(AncEvent.KeepEditing)
        assertFalse(next.showDiscardDialog)
        assertNotNull(next.patientDraft?.deliveryPlan)
    }

    @Test
    fun `StartPatientEditor still opens the profile editor`() {
        val next = base().reduce(AncEvent.StartPatientEditor("p1", "new-1", "s-profile"))
        assertEquals(AncRoute.PatientEditor("p1", false), next.route)
    }

    @Test
    fun `the sheet draft belongs to its own patient`() {
        val next = start()
        assertNotNull(next.deliveryPlanSheetDraft("p1"))
        assertNull(next.deliveryPlanSheetDraft("p2"))
        assertNull(base().deliveryPlanSheetDraft("p1"))
    }

    @Test
    fun `the sheet is clean after start and dirty after a change`() {
        val started = start()
        assertFalse(started.hasDirtyDraft)
        val changed = started.change(DeliveryPlanField.PLAN, "Elective caesarean")
        assertTrue(changed.hasDirtyDraft)
    }

    @Test
    fun `saving the sheet changes only the notes and never touches dating or other profile fields`() {
        val withDating = patient(
            dating = DemoPatientDating(patientId = "p1", lmpDate = "2026-01-01", ultrasoundEdd = null, agreedEdd = "2026-10-08", datingMethod = "LMP", notes = null),
        ).copy(ageText = "30 years")
        val changed = start(base(withDating)).change(DeliveryPlanField.PLAN, "Elective caesarean")
        val draft = requireNotNull(changed.patientDraft)
        assertEquals(draft.original, draft.values.copy(obstetricNotes = draft.original.obstetricNotes))
        val prepared = com.sherifradwan.ancnative.storage.SavePatientProfileCommand.prepare(changed, "op-1", "2026-11-11T08:00:00Z")
        val command = (prepared as com.sherifradwan.ancnative.storage.PatientCommandPreparation.Ready).command
            as com.sherifradwan.ancnative.storage.SavePatientProfileCommand
        assertEquals(withDating.caseId, command.caseId)
        assertEquals(withDating.displayName, command.displayName)
        assertEquals(withDating.ageText, command.ageText)
    }

    @Test
    fun `start clears a failed save left over from another editor`() {
        val stale = base().copy(savePhase = SavePhase.ERROR, saveError = SaveUiError.STALE_OR_CONFLICT)
        val next = start(stale)
        assertEquals(SavePhase.IDLE, next.savePhase)
        assertNull(next.saveError)
        assertNotNull(next.patientDraft?.deliveryPlan)
    }

    @Test
    fun `a failed save keeps the four fields and the composed notes and shows the error`() {
        val changed = start().change(DeliveryPlanField.PLAN, "Elective caesarean")
        val token = PendingSaveToken(operationUuid = "op-1", editorSessionId = "s-plan")
        val failed = changed.copy(savePhase = SavePhase.PENDING, pendingSave = token)
            .reduce(AncEvent.SaveFailed(token, SaveUiError.STALE_OR_CONFLICT, unresolved = false))
        assertEquals(SavePhase.ERROR, failed.savePhase)
        assertEquals(SaveUiError.STALE_OR_CONFLICT, failed.saveError)
        assertEquals(changed.patientDraft, failed.patientDraft)
        assertEquals("Elective caesarean", failed.fields().plan)
    }

    @Test
    fun `Back and Cancel do nothing while a save runs`() {
        val saving = start().change(DeliveryPlanField.FEES, "6000 EGP").copy(savePhase = SavePhase.PENDING)
        assertEquals(saving, saving.reduce(AncEvent.Back))
        assertEquals(saving, saving.reduce(AncEvent.CancelEditor))
    }

    @Test
    fun `the profile editor cannot start while the sheet holds a real change`() {
        val dirty = start().change(DeliveryPlanField.HOSPITAL, "Nile Hospital")
        assertEquals(dirty, dirty.reduce(AncEvent.StartPatientEditor("p1", "new-1", "s-profile")))
    }
}

