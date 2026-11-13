package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoPatient

/**
 * The single rule every "edit patient" control uses: resume an incomplete registration
 * at the first unfinished step, or open the editor for a complete patient.
 * Archived patients are not editable and return null.
 */
fun editPatientEvent(patient: DemoPatient, editorSessionId: String, newPregnancyId: String): AncEvent? {
    if (patient.status == "archived") return null
    if (isRegistrationIncomplete(patient)) {
        return AncEvent.Flow(
            FlowEvent.Begin(
                patient.id,
                firstNotDoneStep(patient) ?: RegistrationStep.IDENTITY,
            ),
            editorSessionId,
            newPregnancyId,
        )
    }
    return AncEvent.StartPatientEditor(patient.id, patient.id, editorSessionId)
}
