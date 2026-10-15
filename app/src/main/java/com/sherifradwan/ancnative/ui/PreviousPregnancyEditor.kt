package com.sherifradwan.ancnative.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.model.PatientVocabulary
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.PregnancyDraftField
import com.sherifradwan.ancnative.state.SavePhase

@Composable
fun PreviousPregnancyEditor(
    state: AncState,
    route: AncRoute.PreviousPregnancyEditor,
    onEvent: (AncEvent) -> Unit,
) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.pregnancyDraft?.takeIf {
        it.patientId == route.patientId && it.pregnancyId == route.pregnancyId
    } ?: return
    val enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR
    EditorPage(
        title = if (draft.basePregnancyRevision == null) {
            stringResource(R.string.add_previous_pregnancy)
        } else {
            stringResource(R.string.correct_previous_pregnancy)
        },
        subtitle = "${patient.displayName} · ${patient.caseId}",
        tag = "previousPregnancyEditor",
    ) {
        EditorErrorCard(state, draft.errors)
        Text(stringResource(R.string.no_date_inference_notice), color = MaterialTheme.colorScheme.tertiary)
        ClinicalDateField(
            value = draft.values.dateText,
            onValueChange = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.DATE_TEXT, it)) },
            label = stringResource(R.string.entered_date_text),
            tag = "pregnancyDateText",
            enabled = enabled,
            showTodayButton = false,
        )
        EditorField(
            value = draft.values.outcomeText,
            onValueChange = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME_TEXT, it)) },
            label = stringResource(R.string.entered_outcome_text),
            tag = "pregnancyOutcomeText",
            enabled = enabled,
        )
        ChoiceGroup(
            label = stringResource(R.string.pregnancy_type),
            selected = draft.values.pregnancyType,
            choices = PatientVocabulary.pregnancyTypes.toList(),
            enabled = enabled,
            tagPrefix = "pregnancyType",
            onSelected = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.PREGNANCY_TYPE, it)) },
        )
        EditorField(
            value = draft.values.otherDetails,
            onValueChange = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.OTHER_DETAILS, it)) },
            label = stringResource(R.string.other_details),
            tag = "pregnancyOtherDetails",
            enabled = enabled,
        )
        EditorField(
            value = draft.values.placeAndDoctor,
            onValueChange = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.PLACE_AND_DOCTOR, it)) },
            label = stringResource(R.string.place_and_doctor),
            tag = "pregnancyPlaceDoctor",
            enabled = enabled,
        )
        ChoiceGroup(
            label = stringResource(R.string.historical_delivery_mode),
            selected = draft.values.deliveryBy,
            choices = PatientVocabulary.deliveryModes.toList(),
            enabled = enabled,
            tagPrefix = "deliveryBy",
            onSelected = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.DELIVERY_BY, it)) },
        )
        ChoiceGroup(
            label = stringResource(R.string.structured_outcome),
            selected = draft.values.outcome,
            choices = PatientVocabulary.pregnancyOutcomes.toList(),
            enabled = enabled,
            tagPrefix = "pregnancyOutcome",
            onSelected = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, it)) },
        )
        if (draft.values.outcome == "LiveBirth" || draft.values.outcome == "Stillbirth") {
            ChoiceGroup(
                label = stringResource(R.string.birth_method),
                selected = draft.values.birthMethod,
                choices = PatientVocabulary.birthMethods.toList(),
                enabled = enabled,
                tagPrefix = "birthMethod",
                onSelected = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.BIRTH_METHOD, it)) },
            )
        }
        if (draft.values.outcome == "EarlyPregnancyLoss") {
            ChoiceGroup(
                label = stringResource(R.string.loss_management),
                selected = draft.values.lossManagement,
                choices = PatientVocabulary.lossManagementMethods.toList(),
                enabled = enabled,
                tagPrefix = "lossManagement",
                onSelected = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.LOSS_MANAGEMENT, it)) },
            )
        }
        if (draft.values.outcome == "TerminationOfPregnancy") {
            ChoiceGroup(
                label = stringResource(R.string.termination_method),
                selected = draft.values.terminationMethod,
                choices = PatientVocabulary.terminationMethods.toList(),
                enabled = enabled,
                tagPrefix = "terminationMethod",
                onSelected = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.TERMINATION_METHOD, it)) },
            )
        }
        EditorField(
            value = draft.values.clinicianId,
            onValueChange = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.CLINICIAN_ID, it)) },
            label = stringResource(R.string.clinician_id_required),
            tag = "pregnancyClinicianId",
            enabled = enabled,
            singleLine = true,
            error = PatientValidationError.CLINICIAN_ID_REQUIRED in draft.errors,
        )
        if (PatientValidationError.OUTCOME_METHOD_MISMATCH in draft.errors) {
            Text(stringResource(R.string.outcome_method_mismatch), color = MaterialTheme.colorScheme.error)
        }
        EditorActions(
            enabled = enabled,
            saveTag = "savePregnancy",
            onCancel = { onEvent(AncEvent.CancelEditor) },
            onSave = { onEvent(AncEvent.SubmitPreviousPregnancy) },
        )
    }
}
