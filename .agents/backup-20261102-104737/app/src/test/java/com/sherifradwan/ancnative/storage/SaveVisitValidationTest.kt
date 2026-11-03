package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DraftError
import com.sherifradwan.ancnative.state.DraftField
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Oracle Q85 (SPEC_FIX_R3.md, Round 3m): prepare() must refuse a future visit date
// and non-blank BP/weight that do not parse into plausible vitals; blank stays
// optional and the stored command text is still what the doctor typed.
// Runs against the production prepare(state, operationUuid, newVisitId, today);
// until the core lane adds the `today` parameter and the new DraftError values this
// class cannot compile (planned).
class SaveVisitValidationTest {

    private val patient = DemoFixtures.patients[0]
    private val today: LocalDate = LocalDate.parse("2026-10-31")

    private fun stateWithDraft(
        date: String = "",
        bloodPressure: String,
        weight: String,
    ): AncState {
        var state = AncState(patients = DemoFixtures.patients)
            .reduce(AncEvent.StartVisit(patient.id, null, "editor-3m"))
        state = state.reduce(AncEvent.DraftChanged(DraftField.DATE, date))
        state = state.reduce(AncEvent.DraftChanged(DraftField.BLOOD_PRESSURE, bloodPressure))
        state = state.reduce(AncEvent.DraftChanged(DraftField.WEIGHT, weight))
        return state
    }

    private fun AncState.reduce(event: AncEvent): AncState = AncReducer.reduce(this, event)

    private fun prepare(state: AncState): SavePreparation =
        SaveVisitCommand.prepare(
            state,
            "00000000-0000-4000-8000-000000000301",
            "00000000-0000-4000-8000-000000000320",
            today,
        )

    @Test
    fun futureDateIsRejected() {
        val state = stateWithDraft(
            date = "2026-11-10",
            bloodPressure = "",
            weight = "",
        )
        val rejected = prepare(state) as SavePreparation.Rejected
        assertTrue(DraftError.DATE_IN_FUTURE in rejected.errors)
    }

    @Test
    fun visitDatedTodayAndInThePastIsAccepted() {
        val stateToday = stateWithDraft(date = "2026-10-31", bloodPressure = "", weight = "")
        val readyToday = prepare(stateToday) as SavePreparation.Ready
        assertEquals("2026-10-31", readyToday.command.date)

        val statePast = stateWithDraft(date = "2026-10-01", bloodPressure = "", weight = "")
        val readyPast = prepare(statePast) as SavePreparation.Ready
        assertEquals("2026-10-01", readyPast.command.date)
    }

    @Test
    fun unparseableBloodPressureIsRejected() {
        val digitsOnly = stateWithDraft(date = "2026-10-31", bloodPressure = "12080", weight = "")
        assertTrue(
            DraftError.BLOOD_PRESSURE_INVALID in (prepare(digitsOnly) as SavePreparation.Rejected).errors,
        )

        val text = stateWithDraft(date = "2026-10-31", bloodPressure = "BP high", weight = "")
        assertTrue(
            DraftError.BLOOD_PRESSURE_INVALID in (prepare(text) as SavePreparation.Rejected).errors,
        )
    }

    @Test
    fun implausibleBloodPressureIsRejected() {
        val parsedButImplausible = stateWithDraft(date = "2026-10-31", bloodPressure = "300/80", weight = "")
        assertTrue(
            DraftError.BLOOD_PRESSURE_INVALID in (prepare(parsedButImplausible) as SavePreparation.Rejected).errors,
        )

        val inverted = stateWithDraft(date = "2026-10-31", bloodPressure = "80/200", weight = "")
        assertTrue(
            DraftError.BLOOD_PRESSURE_INVALID in (prepare(inverted) as SavePreparation.Rejected).errors,
        )
    }

    @Test
    fun invalidWeightIsRejectedBothUnparsedAndImplausible() {
        val tooLow = stateWithDraft(date = "2026-10-31", bloodPressure = "", weight = "5")
        assertTrue(DraftError.WEIGHT_INVALID in (prepare(tooLow) as SavePreparation.Rejected).errors)

        val twoDecimals = stateWithDraft(date = "2026-10-31", bloodPressure = "", weight = "68.55")
        assertTrue(DraftError.WEIGHT_INVALID in (prepare(twoDecimals) as SavePreparation.Rejected).errors)

        val garbage = stateWithDraft(date = "2026-10-31", bloodPressure = "", weight = "abc")
        assertTrue(DraftError.WEIGHT_INVALID in (prepare(garbage) as SavePreparation.Rejected).errors)
    }

    @Test
    fun blankBloodPressureAndWeightStayOptional() {
        val state = stateWithDraft(date = "2026-10-31", bloodPressure = "   ", weight = "")
        val ready = prepare(state) as SavePreparation.Ready
        assertTrue(ready.command.bloodPressure.isNullOrBlank())
        assertTrue(ready.command.weight.isNullOrBlank())
    }

    @Test
    fun allApplicableErrorsComeBackTogether() {
        val state = stateWithDraft(
            date = "2027-01-01",
            bloodPressure = "12080",
            weight = "5",
        )
        val rejected = prepare(state) as SavePreparation.Rejected
        assertTrue(DraftError.DATE_IN_FUTURE in rejected.errors)
        assertTrue(DraftError.BLOOD_PRESSURE_INVALID in rejected.errors)
        assertTrue(DraftError.WEIGHT_INVALID in rejected.errors)
        assertEquals(
            emptySet<DraftError>(),
            rejected.errors - setOf(
                DraftError.DATE_IN_FUTURE,
                DraftError.BLOOD_PRESSURE_INVALID,
                DraftError.WEIGHT_INVALID,
            ),
        )
    }

    @Test
    fun validValuesAreStoredExactlyAsTypedAndTrimmed() {
        val state = stateWithDraft(
            date = " 2026-10-31 ",
            bloodPressure = " 120/80 ",
            weight = " 68,5 ",
        )
        val ready = prepare(state) as SavePreparation.Ready
        assertEquals("2026-10-31", ready.command.date)
        assertEquals("120/80", ready.command.bloodPressure)
        assertEquals("68,5", ready.command.weight)
    }
}
