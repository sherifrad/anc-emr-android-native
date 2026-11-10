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
)

sealed interface FlowEvent {
    data class Begin(val patientId: String, val startStep: RegistrationStep = RegistrationStep.IDENTITY) : FlowEvent
    data class Next(val hasData: Boolean) : FlowEvent
    data object Back : FlowEvent
    data object Skip : FlowEvent
    data object Finish : FlowEvent
    data class PregnancyCountChosen(val n: Int) : FlowEvent
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

fun reduce(flow: RegistrationFlow?, event: FlowEvent): FlowResult {
    if (event is FlowEvent.Begin) {
        return FlowResult.Continue(
            RegistrationFlow(
                patientId = event.patientId,
                current = event.startStep,
                statuses = STEPS.associateWith { StepStatus.PENDING },
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
        FlowEvent.Back ->
            if (index == 0) FlowResult.Exit else FlowResult.Continue(flow.copy(current = STEPS[index - 1]))
        is FlowEvent.PregnancyCountChosen ->
            if (flow.current != RegistrationStep.PREGNANCIES) FlowResult.Continue(flow)
            else FlowResult.Continue(
                flow.copy(pregnancyTarget = event.n.coerceIn(0, MAX_SUGGESTED_PREGNANCIES), pregnancyIndex = 0),
            )
    }
}

private fun advance(flow: RegistrationFlow, index: Int, status: StepStatus): FlowResult {
    val marked = flow.statuses + (flow.current to status)
    if (index == STEPS.lastIndex) return FlowResult.Exit
    return FlowResult.Continue(flow.copy(current = STEPS[index + 1], statuses = marked))
}

fun suggestedPregnancyCount(gravida: Long?, parity: Long?, abortion: Long?): Int {
    val raw = if (gravida != null) maxOf(gravida - 1, 0L) else (parity ?: 0L) + (abortion ?: 0L)
    return raw.coerceIn(0L, MAX_SUGGESTED_PREGNANCIES.toLong()).toInt()
}

fun deriveStepStatuses(patient: DemoPatient): Map<RegistrationStep, StepStatus> {
    val expectedPregnancies = suggestedPregnancyCount(patient.gravida, patient.parity, patient.abortion)
    val done = mapOf(
        RegistrationStep.IDENTITY to
            (!patient.displayName.isBlank() && !patient.phone.isNullOrBlank()),
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
