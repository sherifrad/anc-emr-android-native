package com.sherifradwan.ancnative.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.model.PatientVocabulary
import com.sherifradwan.ancnative.model.PreviousPregnancyFormRules
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.PregnancyDraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.activeFlowForRoute
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.patienthub.components.SheetCancelSaveRow

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
    val inFlow = state.activeFlowForRoute() != null

    val obstetricFamily = FieldFamilies.obstetric()
    val liveBirthLabel = stringResource(R.string.outcome_live_birth)
    val stillbirthLabel = stringResource(R.string.outcome_stillbirth)
    val earlyLossLabel = stringResource(R.string.outcome_early_loss)
    val terminationLabel = stringResource(R.string.outcome_termination)
    val vaginalLabel = stringResource(R.string.delivery_mode_vaginal)
    val cesareanLabel = stringResource(R.string.delivery_mode_cesarean)

    EditorPage(
        title = if (draft.basePregnancyRevision == null) {
            stringResource(R.string.add_previous_pregnancy)
        } else {
            stringResource(R.string.correct_previous_pregnancy)
        },
        subtitle = patient.displayName,
        tag = "previousPregnancyEditor",
        showHeader = !inFlow,
        useRecordStyle = true,
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)

        RecordSectionCard(
            title = stringResource(R.string.pregnancy_core_facts_title),
            family = obstetricFamily,
        ) {
            ClinicalDateField(
                value = draft.values.dateText,
                onValueChange = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.DATE_TEXT, it)) },
                label = stringResource(R.string.entered_date_text),
                tag = "pregnancyDateText",
                enabled = enabled,
                showTodayButton = false,
                externalLabel = true,
                useRecordStyle = true,
                allowClear = true,
            )

            ChoiceGroup(
                label = stringResource(R.string.structured_outcome),
                selected = draft.values.outcome,
                choices = PatientVocabulary.pregnancyOutcomes.toList(),
                enabled = enabled,
                tagPrefix = "pregnancyOutcome",
                onSelected = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, it)) },
                labelStyle = RecordType.FieldLabel,
                labelColor = obstetricFamily.label,
                labelOf = { outcome ->
                    when (outcome) {
                        "LiveBirth" -> liveBirthLabel
                        "Stillbirth" -> stillbirthLabel
                        "EarlyPregnancyLoss" -> earlyLossLabel
                        "TerminationOfPregnancy" -> terminationLabel
                        else -> outcome
                    }
                },
                useRecordChip = true,
            )

            if (draft.values.outcomeText.isNotBlank() && draft.values.outcome.isBlank()) {
                Text(
                    text = stringResource(R.string.earlier_outcome_note, draft.values.outcomeText),
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (PreviousPregnancyFormRules.hasDeliveryMode(draft.values.outcome)) {
                ChoiceGroup(
                    label = stringResource(R.string.delivery_mode),
                    selected = PreviousPregnancyFormRules.modeOf(draft.values.deliveryBy, draft.values.birthMethod),
                    choices = PreviousPregnancyFormRules.deliveryModeChoices,
                    enabled = enabled,
                    tagPrefix = "deliveryBy",
                    onSelected = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.DELIVERY_BY, it)) },
                    labelStyle = RecordType.FieldLabel,
                    labelColor = obstetricFamily.label,
                    labelOf = { mode ->
                        when (mode) {
                            PreviousPregnancyFormRules.MODE_CS -> cesareanLabel
                            PreviousPregnancyFormRules.MODE_VAGINAL -> vaginalLabel
                            else -> mode
                        }
                    },
                    useRecordChip = true,
                )
            }

            if (
                PreviousPregnancyFormRules.hasDeliveryMode(draft.values.outcome) &&
                draft.values.deliveryBy.isNotBlank() &&
                draft.values.deliveryBy !in PreviousPregnancyFormRules.deliveryModeChoices
            ) {
                Text(
                    text = stringResource(R.string.stored_delivery_mode, draft.values.deliveryBy),
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    labelStyle = RecordType.FieldLabel,
                    labelColor = obstetricFamily.label,
                    useRecordChip = true,
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
                    labelStyle = RecordType.FieldLabel,
                    labelColor = obstetricFamily.label,
                    useRecordChip = true,
                )
            }

            if (PatientValidationError.OUTCOME_METHOD_MISMATCH in draft.errors) {
                Text(
                    stringResource(R.string.outcome_method_mismatch),
                    color = MaterialTheme.colorScheme.error,
                    style = RecordType.Action,
                )
            }
        }

        RecordSectionCard(
            title = stringResource(R.string.pregnancy_details_title),
            family = obstetricFamily,
        ) {
            EditorField(
                value = draft.values.placeAndDoctor,
                onValueChange = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.PLACE_AND_DOCTOR, it)) },
                label = stringResource(R.string.place_and_doctor),
                tag = "pregnancyPlaceDoctor",
                enabled = enabled,
                errorMessage = if (PatientValidationError.PLACE_AND_DOCTOR_REQUIRED in draft.errors) {
                    stringResource(R.string.place_and_doctor_required)
                } else {
                    null
                },
                shape = RecordShapes.Small,
                textStyle = RecordType.FieldValue,
                externalLabel = true,
            )

            ChoiceGroup(
                label = stringResource(R.string.pregnancy_type),
                selected = draft.values.pregnancyType,
                choices = PatientVocabulary.pregnancyTypes.toList(),
                enabled = enabled,
                tagPrefix = "pregnancyType",
                onSelected = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.PREGNANCY_TYPE, it)) },
                labelStyle = RecordType.FieldLabel,
                labelColor = obstetricFamily.label,
                useRecordChip = true,
            )

            EditorField(
                value = draft.values.otherDetails,
                onValueChange = { onEvent(AncEvent.PregnancyDraftChanged(PregnancyDraftField.OTHER_DETAILS, it)) },
                label = stringResource(R.string.other_details),
                tag = "pregnancyOtherDetails",
                enabled = enabled,
                shape = RecordShapes.Small,
                textStyle = RecordType.FieldValue,
                externalLabel = true,
            )
        }

        if (!inFlow) {
            SheetCancelSaveRow(
                enabled = enabled,
                saving = state.savePhase == SavePhase.PENDING,
                saveEnabled = enabled,
                savingLabel = stringResource(
                    if (state.savePhase == SavePhase.PENDING) R.string.saving_label else R.string.save,
                ),
                onCancel = { onEvent(AncEvent.CancelEditor) },
                onSave = { onEvent(AncEvent.SubmitPreviousPregnancy) },
                cancelTag = "cancelPregnancy",
                saveTag = "savePregnancy",
                spinnerTag = "pregnancySaveSpinner",
            )
        }
    }
}
