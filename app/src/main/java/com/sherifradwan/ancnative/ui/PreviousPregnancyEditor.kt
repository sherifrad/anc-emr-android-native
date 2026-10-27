package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        subtitle = "${patient.displayName} · تسجيل وتوثيق تفاصيل الأحمال السابقة",
        tag = "previousPregnancyEditor",
    ) {
        EditorErrorCard(state, draft.errors)

        // CARD 1: TIMING & PREGNANCY BASICS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "1. توقيت الحمل والنوع ومكان الولادة · Timing & Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

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
            }
        }

        // CARD 2: DELIVERY MODE & CLINICAL OUTCOME
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "2. طريقة الولادة والنتيجة السريرية · Delivery Mode & Outcome",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
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
            }
        }

        // CARD 3: CLINICIAN AUDIT & VERIFICATION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "3. التوثيق السريري والتدقيق · Clinician & Audit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

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
                    Text(
                        stringResource(R.string.outcome_method_mismatch),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Security Vault Indicator
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "سجلات الأحمال السابقة مشفرة وموقعة برمجياً · Tamper-Evident Local Storage",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        EditorActions(
            enabled = enabled,
            saveTag = "savePregnancy",
            onCancel = { onEvent(AncEvent.CancelEditor) },
            onSave = { onEvent(AncEvent.SubmitPreviousPregnancy) },
        )
    }
}
