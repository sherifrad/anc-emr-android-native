package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.GestationalAgeEngine

enum class RegistrationStep(val title: String) {
    IDENTITY("Identity"),
    DATING("Dating"),
    OBSTETRIC("Obstetric history and risk"),
    HISTORIES("Histories"),
    PREGNANCIES("Previous pregnancies"),
}

enum class StepStatus(val label: String) {
    PENDING("Pending"),
    DONE("Done"),
    SKIPPED("Skipped"),
}

const val MAX_SUGGESTED_PREGNANCIES = 20

data class RegistrationFlow(
    val patientId: String,
    val current: RegistrationStep,
    val statuses: Map<RegistrationStep, StepStatus>,
    val pregnancyTarget: Int? = null,
    val pregnancyIndex: Int = 0,
    /** Session of the editor the flow opened last; null while the pregnancy count prompt is showing. */
    val activeEditorSessionId: String? = null,
)

sealed interface FlowEvent {
    /** [patient] is the stored patient (statuses are derived from it); null for a patient not saved yet. */
    data class Begin(
        val patientId: String,
        val startStep: RegistrationStep = RegistrationStep.IDENTITY,
        val patient: DemoPatient? = null,
    ) : FlowEvent
    data class Next(val hasData: Boolean) : FlowEvent
    data object Back : FlowEvent
    data object Skip : FlowEvent
    data object Finish : FlowEvent
    data class PregnancyCountChosen(val n: Int, val existingCount: Int = 0) : FlowEvent
    data object PregnancyCommitted : FlowEvent
}

sealed interface FlowResult {
    data class Continue(val flow: RegistrationFlow) : FlowResult
    data object Exit : FlowResult
}

private val STEPS = RegistrationStep.entries

fun registrationStepNumber(step: RegistrationStep): Int = STEPS.indexOf(step) + 1

fun registrationHeaderText(flow: RegistrationFlow): String =
    "Step ${registrationStepNumber(flow.current)} of ${STEPS.size}: ${flow.current.title}"

fun stepStatusText(status: StepStatus): String = status.label

/** The one rule for how many pregnancy editors still have to open: the chosen total minus those already stored. */
fun remainingPregnancies(chosen: Int, existing: Int): Int =
    (chosen.coerceIn(0, MAX_SUGGESTED_PREGNANCIES) - existing).coerceAtLeast(0)

fun reduce(flow: RegistrationFlow?, event: FlowEvent): FlowResult {
    if (event is FlowEvent.Begin) {
        val patient = event.patient
        if (patient?.status == "archived") return FlowResult.Exit
        return FlowResult.Continue(
            RegistrationFlow(
                patientId = event.patientId,
                current = event.startStep,
                statuses = patient?.let(::deriveStepStatuses) ?: STEPS.associateWith { StepStatus.PENDING },
            ),
        )
    }
    if (flow == null) return FlowResult.Exit
    val index = STEPS.indexOf(flow.current)
    val isLast = index == STEPS.lastIndex
    return when (event) {
        is FlowEvent.Begin -> error("handled above")
        is FlowEvent.Next ->
            if (event.hasData) advance(flow, index, StepStatus.DONE)
            else if (index == 0) FlowResult.Continue(flow)
            else advance(flow, index, StepStatus.SKIPPED)
        FlowEvent.Finish -> if (isLast) FlowResult.Exit else FlowResult.Continue(flow)
        FlowEvent.Skip ->
            if (index == 0) FlowResult.Continue(flow) else advance(flow, index, StepStatus.SKIPPED)
        FlowEvent.Back -> when {
            index == 0 -> FlowResult.Exit
            // Inside the pregnancy walk Back returns to the count prompt; the index is left as it is.
            flow.current == RegistrationStep.PREGNANCIES && flow.pregnancyTarget != null ->
                FlowResult.Continue(flow.copy(pregnancyTarget = null))
            else -> FlowResult.Continue(enter(flow, STEPS[index - 1]))
        }
        is FlowEvent.PregnancyCountChosen ->
            if (flow.current != RegistrationStep.PREGNANCIES || flow.pregnancyTarget != null) {
                FlowResult.Continue(flow)
            } else {
                val remaining = remainingPregnancies(event.n, event.existingCount)
                if (remaining == 0) FlowResult.Exit
                else FlowResult.Continue(flow.copy(pregnancyTarget = remaining, pregnancyIndex = 0))
            }
        FlowEvent.PregnancyCommitted -> {
            val target = flow.pregnancyTarget
            if (flow.current != RegistrationStep.PREGNANCIES || target == null) {
                FlowResult.Continue(flow)
            } else if (flow.pregnancyIndex + 1 >= target) {
                FlowResult.Exit
            } else {
                FlowResult.Continue(flow.copy(pregnancyIndex = flow.pregnancyIndex + 1))
            }
        }
    }
}

private fun advance(flow: RegistrationFlow, index: Int, status: StepStatus): FlowResult {
    val marked = flow.statuses + (flow.current to status)
    if (index == STEPS.lastIndex) return FlowResult.Exit
    return FlowResult.Continue(enter(flow.copy(statuses = marked), STEPS[index + 1]))
}

/** Moving onto a step always resets the pregnancy walk, so the count prompt reappears on entering the last step. */
private fun enter(flow: RegistrationFlow, step: RegistrationStep): RegistrationFlow =
    flow.copy(current = step, pregnancyTarget = null, pregnancyIndex = 0)

fun suggestedPregnancyCount(gravida: Long?, parity: Long?, abortion: Long?): Int {
    val raw = if (gravida != null) maxOf(gravida - 1, 0L) else (parity ?: 0L) + (abortion ?: 0L)
    return raw.coerceIn(0L, MAX_SUGGESTED_PREGNANCIES.toLong()).toInt()
}

fun deriveStepStatuses(patient: DemoPatient): Map<RegistrationStep, StepStatus> {
    val expectedPregnancies = suggestedPregnancyCount(patient.gravida, patient.parity, patient.abortion)
    val done = mapOf(
        RegistrationStep.IDENTITY to
            (patient.displayName.isNotBlank() && !patient.ageText.isNullOrBlank()),
        RegistrationStep.DATING to (GestationalAgeEngine.calculateEstimatedDueDate(patient.dating) != null),
        RegistrationStep.OBSTETRIC to
            (patient.gravida != null && patient.parity != null && patient.abortion != null),
        RegistrationStep.HISTORIES to listOf(
            patient.medicalHistory,
            patient.surgicalHistory,
            patient.familyHistory,
            patient.allergyHistory,
        ).any { !it.isNullOrBlank() },
        RegistrationStep.PREGNANCIES to
            (patient.gravida != null && patient.previousPregnancies.size >= expectedPregnancies),
    )
    return done.mapValues { (_, isDone) -> if (isDone) StepStatus.DONE else StepStatus.PENDING }
}

private val REQUIRED_STEPS = listOf(RegistrationStep.IDENTITY, RegistrationStep.DATING, RegistrationStep.OBSTETRIC)

fun isRegistrationIncomplete(patient: DemoPatient): Boolean {
    if (patient.status == "archived") return false
    val statuses = deriveStepStatuses(patient)
    return REQUIRED_STEPS.any { statuses[it] != StepStatus.DONE }
}
