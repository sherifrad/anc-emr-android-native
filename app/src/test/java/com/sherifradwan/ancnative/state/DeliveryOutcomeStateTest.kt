package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.*
import com.sherifradwan.ancnative.storage.SavePatientProfileCommand
import com.sherifradwan.ancnative.storage.PatientCommandPreparation
import com.sherifradwan.ancnative.storage.VaultState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** The delivery outcome fields in the delivery plan sheet and profile save (design: delivery plan sheet, sections 5.1 to 5.3). */
class DeliveryOutcomeStateTest {
    private val planTab = AncRoute.Workspace("p1")
    private val freeNotes = "Delivery agreed with City Hospital. Fees 5000 EGP, review at six weeks."
    private val lmpDate = "2026-01-01"
    private val submittedAt = "2026-10-10T12:00:00Z"

    private fun composedNotes(
        hospital: String = "City Hospital",
        fees: String = "5000 EGP",
        plan: String = "vaginal",
        postpartum: String = "Review in 6 weeks",
    ) = composeDeliveryNotes(
        DeliveryNoteSections(hospital = hospital, fees = fees, plan = plan, postpartum = postpartum),
    )

    private fun patient(
        notes: String? = freeNotes,
        status: String = "active",
        dating: DemoPatientDating? = DemoPatientDating(patientId = "p1", lmpDate = lmpDate, ultrasoundEdd = null, agreedEdd = "2026-10-08", datingMethod = "LMP", notes = null),
        revision: Long = 5L,
        delivered: Boolean = false,
        deliveryDate: String? = null,
        deliveryMode: String? = null,
        deliveryOutcome: String? = null,
    ) = DemoPatient(
        id = "p1", caseId = "P1", displayName = "Patient One", dateOfBirth = "1995-01-01", phone = "01000000000",
        visits = emptyList(), status = status, obstetricNotes = notes, dating = dating, revision = revision,
        delivered = delivered, deliveryDate = deliveryDate, deliveryMode = deliveryMode, deliveryOutcome = deliveryOutcome,
    )

    private fun base(vararg patients: DemoPatient = arrayOf(patient())) =
        AncState(patients = patients.toList(), vaultState = VaultState.Unlocked(1L), route = planTab)

    private fun AncState.reduce(event: AncEvent) = AncReducer.reduce(this, event)

    private fun start(state: AncState = base(), session: String = "s-plan") =
        state.reduce(AncEvent.StartDeliveryPlanEditor("p1", session))

    private fun AncState.toggleDelivered(delivered: Boolean) =
        reduce(AncEvent.DeliveryOutcomeToggled(delivered))

    private fun AncState.changeOutcome(field: DeliveryOutcomeField, value: String) =
        reduce(AncEvent.DeliveryOutcomeFieldChanged(field, value))

    private fun AncState.outcomeFields(): PatientDraftValues = requireNotNull(patientDraft).values

    @Test
    fun `start copies a delivered patient's values into the draft and an untouched sheet is not touched`() {
        val deliveredPatient = patient(delivered = true, deliveryDate = "2026-10-08", deliveryMode = "vaginal", deliveryOutcome = "live_birth")
        val next = start(base(deliveredPatient))
        val draft = requireNotNull(next.patientDraft)
        val outcome = next.outcomeFields()

        assertEquals(true, outcome.delivered)
        assertEquals("2026-10-08", outcome.deliveryDate)
        assertEquals("vaginal", outcome.deliveryMode)
        assertEquals("live_birth", outcome.deliveryOutcome)
        assertFalse(next.patientDraft!!.deliveryPlanTouched)
        assertFalse(next.hasDirtyDraft)
    }

    @Test
    fun `toggling on makes it touched`() {
        val started = start()
        val toggled = started.toggleDelivered(true)
        val outcome = toggled.outcomeFields()

        assertEquals(true, outcome.delivered)
        assertTrue(toggled.patientDraft!!.deliveryPlanTouched)
        assertTrue(toggled.hasDirtyDraft)
    }

    @Test
    fun `toggling on then off returns to untouched`() {
        val started = start()
        val on = started.toggleDelivered(true)
        val off = on.toggleDelivered(false)
        val outcome = off.outcomeFields()

        assertEquals(false, outcome.delivered)
        assertFalse(off.patientDraft!!.deliveryPlanTouched)
        assertFalse(off.hasDirtyDraft)
    }

    @Test
    fun `toggling off clears date, mode and outcome`() {
        val deliveredPatient = patient(delivered = true, deliveryDate = "2026-10-08", deliveryMode = "vaginal", deliveryOutcome = "live_birth")
        val started = start(base(deliveredPatient))
        val toggled = started.toggleDelivered(false)
        val outcome = toggled.outcomeFields()

        assertEquals(false, outcome.delivered)
        assertEquals("", outcome.deliveryDate)
        assertEquals("", outcome.deliveryMode)
        assertEquals("", outcome.deliveryOutcome)
    }

    @Test
    fun `each field event sets only its value`() {
        val started = start().toggleDelivered(true)
        val afterDate = started.changeOutcome(DeliveryOutcomeField.DATE, "2026-10-09")
        val afterMode = started.changeOutcome(DeliveryOutcomeField.MODE, "C-section")
        val afterOutcome = started.changeOutcome(DeliveryOutcomeField.OUTCOME, "Stillbirth")

        assertEquals("2026-10-09", afterDate.outcomeFields().deliveryDate)
        assertEquals("", afterDate.outcomeFields().deliveryMode)
        assertEquals("", afterDate.outcomeFields().deliveryOutcome)

        assertEquals("", afterMode.outcomeFields().deliveryDate)
        assertEquals("C-section", afterMode.outcomeFields().deliveryMode)
        assertEquals("", afterMode.outcomeFields().deliveryOutcome)

        assertEquals("", afterOutcome.outcomeFields().deliveryDate)
        assertEquals("", afterOutcome.outcomeFields().deliveryMode)
        assertEquals("Stillbirth", afterOutcome.outcomeFields().deliveryOutcome)
    }

    @Test
    fun `field events do nothing while a save runs and when no sheet draft is open`() {
        val started = start().toggleDelivered(true)
        val saving = started.copy(savePhase = SavePhase.PENDING)
        val duringSave = saving.changeOutcome(DeliveryOutcomeField.DATE, "2026-10-09")
        assertEquals(started.outcomeFields(), duringSave.outcomeFields())

        val profile = base().reduce(AncEvent.StartPatientEditor("p1", "new-1", "s-profile"))
        val noSheet = profile.changeOutcome(DeliveryOutcomeField.DATE, "2026-10-09")
        assertEquals(profile.patientDraft, noSheet.patientDraft)
        assertNull(requireNotNull(noSheet.patientDraft).deliveryPlan)
    }

    @Test
    fun `touching only the outcome (not the notes) lets the profile save prepare Ready and the command carries the four values and the notes are unchanged`() {
        val deliveredPatient = patient(delivered = true, deliveryDate = "2026-10-08", deliveryMode = "vaginal", deliveryOutcome = "live_birth")
        val started = start(base(deliveredPatient))
        val touchedOutcome = started.changeOutcome(DeliveryOutcomeField.OUTCOME, "stillbirth")
        val draft = requireNotNull(touchedOutcome.patientDraft)

        val prep = SavePatientProfileCommand.prepare(touchedOutcome, "op-1", submittedAt)
        assertTrue(prep is PatientCommandPreparation.Ready)
        val command = (prep as PatientCommandPreparation.Ready).command as SavePatientProfileCommand

        assertEquals(true, command.delivered)
        assertEquals("2026-10-08", command.deliveryDate)
        assertEquals("vaginal", command.deliveryMode)
        assertEquals("stillbirth", command.deliveryOutcome)
        assertEquals(freeNotes, command.obstetricNotes)
    }

    @Test
    fun `prepare rejects a bad date text, a date two days after the submittedAt day, a date before the stored lmpDate, an unknown mode, an unknown outcome`() {
        val started = start().toggleDelivered(true)

        var prep = SavePatientProfileCommand.prepare(
            started.changeOutcome(DeliveryOutcomeField.DATE, "not-a-date"),
            "op-1", submittedAt
        )
        assertTrue(prep is PatientCommandPreparation.Rejected)
        assertTrue((prep as PatientCommandPreparation.Rejected).errors.contains(PatientValidationError.DELIVERY_DATE_INVALID))

        val futureDate = LocalDate.parse("2026-10-10").plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)
        prep = SavePatientProfileCommand.prepare(
            started.changeOutcome(DeliveryOutcomeField.DATE, futureDate),
            "op-2", submittedAt
        )
        assertTrue(prep is PatientCommandPreparation.Rejected)
        assertTrue((prep as PatientCommandPreparation.Rejected).errors.contains(PatientValidationError.DELIVERY_DATE_IN_FUTURE))

        val beforeLmp = LocalDate.parse(lmpDate).minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        prep = SavePatientProfileCommand.prepare(
            started.changeOutcome(DeliveryOutcomeField.DATE, beforeLmp),
            "op-3", submittedAt
        )
        assertTrue(prep is PatientCommandPreparation.Rejected)
        assertTrue((prep as PatientCommandPreparation.Rejected).errors.contains(PatientValidationError.DELIVERY_DATE_BEFORE_DATING))

        prep = SavePatientProfileCommand.prepare(
            started.changeOutcome(DeliveryOutcomeField.MODE, "UnknownMode"),
            "op-4", submittedAt
        )
        assertTrue(prep is PatientCommandPreparation.Rejected)
        assertTrue((prep as PatientCommandPreparation.Rejected).errors.contains(PatientValidationError.DELIVERY_MODE_NOT_ALLOWED))

        prep = SavePatientProfileCommand.prepare(
            started.changeOutcome(DeliveryOutcomeField.OUTCOME, "UnknownOutcome"),
            "op-5", submittedAt
        )
        assertTrue(prep is PatientCommandPreparation.Rejected)
        assertTrue((prep as PatientCommandPreparation.Rejected).errors.contains(PatientValidationError.DELIVERY_OUTCOME_NOT_ALLOWED))
    }

    @Test
    fun `prepare accepts delivered with no date, no mode, no outcome and the command has nulls`() {
        val started = start().toggleDelivered(true)
        val prep = SavePatientProfileCommand.prepare(started, "op-1", submittedAt)
        assertTrue(prep is PatientCommandPreparation.Ready)
        val command = (prep as PatientCommandPreparation.Ready).command as SavePatientProfileCommand

        assertEquals(true, command.delivered)
        assertNull(command.deliveryDate)
        assertNull(command.deliveryMode)
        assertNull(command.deliveryOutcome)
    }

    @Test
    fun `editing the profile the old way (StartPatientEditor on a delivered patient then prepare) keeps delivered, date, mode and outcome unchanged`() {
        val deliveredPatient = patient(delivered = true, deliveryDate = "2026-10-08", deliveryMode = "vaginal", deliveryOutcome = "live_birth")
        val profileState = base(deliveredPatient).reduce(AncEvent.StartPatientEditor("p1", "new-1", "s-profile"))
        val prep = SavePatientProfileCommand.prepare(profileState, "op-1", submittedAt)
        assertTrue(prep is PatientCommandPreparation.Ready)
        val command = (prep as PatientCommandPreparation.Ready).command as SavePatientProfileCommand

        assertEquals(true, command.delivered)
        assertEquals("2026-10-08", command.deliveryDate)
        assertEquals("vaginal", command.deliveryMode)
        assertEquals("live_birth", command.deliveryOutcome)
    }

    @Test
    fun `the patient with delivered=false and stale text in the draft values is rejected by prepare only if the fields are non blank`() {
        val started = start()
        val draft = requireNotNull(started.patientDraft)
        val stateWithStaleDate = started.copy(
            patientDraft = draft.copy(values = draft.values.copy(deliveryDate = "2026-01-01"))
        )
        val prep = SavePatientProfileCommand.prepare(stateWithStaleDate, "op-1", submittedAt)
        assertTrue(prep is PatientCommandPreparation.Rejected)
        assertTrue((prep as PatientCommandPreparation.Rejected).errors.contains(PatientValidationError.SAVE_FAILED))
    }
}
