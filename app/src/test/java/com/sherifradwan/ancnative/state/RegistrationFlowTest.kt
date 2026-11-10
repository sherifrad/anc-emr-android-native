package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.state.RegistrationStep.DATING
import com.sherifradwan.ancnative.state.RegistrationStep.HISTORIES
import com.sherifradwan.ancnative.state.RegistrationStep.IDENTITY
import com.sherifradwan.ancnative.state.RegistrationStep.OBSTETRIC
import com.sherifradwan.ancnative.state.RegistrationStep.PREGNANCIES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RegistrationFlowTest {
    private fun begin(step: RegistrationStep = IDENTITY): RegistrationFlow =
        (reduce(null, FlowEvent.Begin("p1", step)) as FlowResult.Continue).flow

    private fun step(flow: RegistrationFlow, event: FlowEvent): RegistrationFlow =
        (reduce(flow, event) as FlowResult.Continue).flow

    @Test
    fun rf1_begin_gives_step_one_of_five_all_pending() {
        val flow = begin()
        assertEquals("p1", flow.patientId)
        assertEquals(IDENTITY, flow.current)
        assertEquals(5, flow.statuses.size)
        assertEquals(RegistrationStep.entries.associateWith { StepStatus.PENDING }, flow.statuses)
        assertEquals("Step 1 of 5: Identity", registrationHeaderText(flow))
    }

    @Test
    fun rf1_begin_at_a_later_step_keeps_all_pending() {
        val flow = begin(OBSTETRIC)
        assertEquals(OBSTETRIC, flow.current)
        assertEquals("Step 3 of 5: Obstetric history and risk", registrationHeaderText(flow))
        assertEquals(setOf(StepStatus.PENDING), flow.statuses.values.toSet())
    }

    @Test
    fun rf2_next_advances_and_marks_current_done() {
        val flow = step(begin(), FlowEvent.Next(hasData = true))
        assertEquals(DATING, flow.current)
        assertEquals(StepStatus.DONE, flow.statuses[IDENTITY])
        assertEquals(StepStatus.PENDING, flow.statuses[DATING])
    }

    @Test
    fun rf2_next_walks_all_five_steps_in_order() {
        var flow = begin()
        val seen = mutableListOf(flow.current)
        repeat(4) {
            flow = step(flow, FlowEvent.Next(hasData = true))
            seen += flow.current
        }
        assertEquals(listOf(IDENTITY, DATING, OBSTETRIC, HISTORIES, PREGNANCIES), seen)
        assertEquals(
            listOf(StepStatus.DONE, StepStatus.DONE, StepStatus.DONE, StepStatus.DONE, StepStatus.PENDING),
            RegistrationStep.entries.map { flow.statuses[it] },
        )
    }

    @Test
    fun rf2_finish_on_last_step_ends_the_flow() {
        val flow = begin(PREGNANCIES)
        assertEquals(FlowResult.Exit, reduce(flow, FlowEvent.Finish))
        assertEquals(FlowResult.Exit, reduce(flow, FlowEvent.Next(hasData = true)))
    }

    @Test
    fun rf3_skip_marks_skipped_and_advances_on_steps_two_to_five() {
        val expectedNext = mapOf(DATING to OBSTETRIC, OBSTETRIC to HISTORIES, HISTORIES to PREGNANCIES)
        for ((from, to) in expectedNext) {
            val flow = step(begin(from), FlowEvent.Skip)
            assertEquals(to, flow.current)
            assertEquals(StepStatus.SKIPPED, flow.statuses[from])
        }
        assertEquals(FlowResult.Exit, reduce(begin(PREGNANCIES), FlowEvent.Skip))
    }

    @Test
    fun rf3_skip_on_step_one_is_rejected_and_changes_nothing() {
        val flow = begin()
        val result = reduce(flow, FlowEvent.Skip) as FlowResult.Continue
        assertEquals(flow, result.flow)
        assertEquals(IDENTITY, result.flow.current)
        assertEquals(StepStatus.PENDING, result.flow.statuses[IDENTITY])
    }

    @Test
    fun rf4_back_moves_one_step_without_changing_statuses() {
        val atThree = step(step(begin(), FlowEvent.Next(hasData = true)), FlowEvent.Skip)
        assertEquals(OBSTETRIC, atThree.current)
        val back = step(atThree, FlowEvent.Back)
        assertEquals(DATING, back.current)
        assertEquals(atThree.statuses, back.statuses)
        assertEquals(StepStatus.SKIPPED, back.statuses[DATING])
    }

    @Test
    fun rf4_back_on_step_one_exits() {
        assertEquals(FlowResult.Exit, reduce(begin(), FlowEvent.Back))
    }

    @Test
    fun events_without_a_flow_exit() {
        assertEquals(FlowResult.Exit, reduce(null, FlowEvent.Next(hasData = true)))
        assertEquals(FlowResult.Exit, reduce(null, FlowEvent.Back))
    }

    @Test
    fun pregnancy_count_is_stored_clamped_and_resets_index() {
        val flow = begin(PREGNANCIES)
        assertEquals(3, step(flow, FlowEvent.PregnancyCountChosen(3)).pregnancyTarget)
        assertEquals(20, step(flow, FlowEvent.PregnancyCountChosen(99)).pregnancyTarget)
        assertEquals(0, step(flow, FlowEvent.PregnancyCountChosen(-4)).pregnancyTarget)
        assertEquals(0, step(flow.copy(pregnancyIndex = 2), FlowEvent.PregnancyCountChosen(3)).pregnancyIndex)
    }

    @Test
    fun rf5_skipped_and_done_render_different_text() {
        val done = stepStatusText(StepStatus.DONE)
        val skipped = stepStatusText(StepStatus.SKIPPED)
        val pending = stepStatusText(StepStatus.PENDING)
        assertEquals("Done", done)
        assertEquals("Skipped", skipped)
        assertEquals(3, setOf(done, skipped, pending).size)
        assertNotEquals(done, skipped)
    }

    @Test
    fun rf5_flow_does_not_mutate_input() {
        val flow = begin()
        val next = step(flow, FlowEvent.Next(hasData = true))
        assertEquals(StepStatus.PENDING, flow.statuses[IDENTITY])
        assertEquals(StepStatus.DONE, next.statuses[IDENTITY])
        assertSame(flow.patientId, next.patientId)
    }

    @Test
    fun next_without_data_marks_skipped_and_advances() {
        val flow = step(begin(DATING), FlowEvent.Next(hasData = false))
        assertEquals(OBSTETRIC, flow.current)
        assertEquals(StepStatus.SKIPPED, flow.statuses[DATING])
    }

    @Test
    fun next_without_data_on_identity_is_rejected() {
        val flow = begin()
        val result = reduce(flow, FlowEvent.Next(hasData = false)) as FlowResult.Continue
        assertEquals(flow, result.flow)
        assertEquals(IDENTITY, result.flow.current)
        assertEquals(StepStatus.PENDING, result.flow.statuses[IDENTITY])
    }

    @Test
    fun finish_before_the_last_step_is_rejected_and_keeps_the_flow() {
        val flow = begin(HISTORIES)
        val result = reduce(flow, FlowEvent.Finish) as FlowResult.Continue
        assertEquals(flow, result.flow)
        assertEquals(HISTORIES, result.flow.current)
    }

    @Test
    fun skip_finish_and_count_with_a_null_flow_exit() {
        assertEquals(FlowResult.Exit, reduce(null, FlowEvent.Skip))
        assertEquals(FlowResult.Exit, reduce(null, FlowEvent.Finish))
        assertEquals(FlowResult.Exit, reduce(null, FlowEvent.PregnancyCountChosen(2)))
    }

    @Test
    fun pregnancy_count_is_ignored_outside_the_pregnancies_step() {
        val flow = begin(HISTORIES)
        val result = reduce(flow, FlowEvent.PregnancyCountChosen(3)) as FlowResult.Continue
        assertEquals(flow, result.flow)
        assertEquals(null, result.flow.pregnancyTarget)
    }
}
