package com.sherifradwan.ancnative.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.DatingDraftField
import com.sherifradwan.ancnative.state.SavePhase
import java.time.LocalDate

@Composable
fun DatingEditor(
    state: AncState,
    route: AncRoute.DatingEditor,
    onEvent: (AncEvent) -> Unit,
) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.datingDraft?.takeIf { it.patientId == route.patientId } ?: return
    val enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR

    // Compute live gestational age and EDD preview from current draft inputs
    val currentDatingPreview = remember(draft.values) {
        DemoPatientDating(
            patientId = patient.id,
            lmpDate = draft.values.lmpDate.ifBlank { null },
            ultrasoundEdd = draft.values.ultrasoundEdd.ifBlank { null },
            agreedEdd = draft.values.agreedEdd.ifBlank { null },
            datingMethod = draft.values.datingMethod.ifBlank { null },
            transferDate = draft.values.transferDate.ifBlank { null },
            embryoAgeDays = draft.values.embryoAgeDays.toIntOrNull(),
            notes = draft.values.notes.ifBlank { null },
        )
    }
    val calculatedEdd = remember(currentDatingPreview) {
        GestationalAgeEngine.calculateEstimatedDueDate(currentDatingPreview)
    }
    val calculatedGa = remember(currentDatingPreview) {
        GestationalAgeEngine.calculate(currentDatingPreview, LocalDate.now())
    }

    EditorPage(
        title = stringResource(R.string.dating_editor),
        subtitle = "${patient.displayName} · ${patient.caseId}",
        tag = "datingEditor",
    ) {
        ClinicalEditorErrorCard(state, draft.errors)

        ClinicalDateField(
            value = draft.values.lmpDate,
            onValueChange = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.LMP_DATE, it)) },
            label = stringResource(R.string.lmp_date),
            tag = "lmpDate",
            enabled = enabled,
            error = draft.errors.contains(ClinicalValidationError.INVALID_DATE),
            showTodayButton = true,
        )

        ClinicalDateField(
            value = draft.values.ultrasoundEdd,
            onValueChange = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.ULTRASOUND_EDD, it)) },
            label = stringResource(R.string.ultrasound_edd),
            tag = "ultrasoundEdd",
            enabled = enabled,
            error = draft.errors.contains(ClinicalValidationError.INVALID_DATE),
            showTodayButton = false,
        )

        // Agreed / Adjudicated EDD (Allows overriding if clinician agrees on a different date)
        ClinicalDateField(
            value = draft.values.agreedEdd,
            onValueChange = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.AGREED_EDD, it)) },
            label = stringResource(R.string.agreed_edd),
            tag = "agreedEdd",
            enabled = enabled,
            error = draft.errors.contains(ClinicalValidationError.INVALID_DATE),
            showTodayButton = false,
        )

        // Step 1: Dating Method Selector (Progressive Disclosure)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.dating_method_select_hint),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    "LMP" to stringResource(R.string.lmp_date),
                    "Ultrasound" to stringResource(R.string.ultrasound),
                    "IVF_TRANSFER" to stringResource(R.string.ivf_transfer_date),
                    "Clinical" to stringResource(R.string.agreed_edd),
                ).forEach { (methodKey, methodLabel) ->
                    val isSelected = draft.values.datingMethod.equals(methodKey, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onEvent(AncEvent.DatingDraftChanged(DatingDraftField.DATING_METHOD, methodKey))
                        },
                        label = { Text(methodLabel) },
                        enabled = enabled,
                        modifier = Modifier.testTag("datingMethod_$methodKey"),
                    )
                }
            }
        }

        // Live calculation card if EDD or GA is available
        AnimatedVisibility(visible = calculatedEdd != null || calculatedGa != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (calculatedEdd != null) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.calculated_edd) + ":",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = calculatedEdd.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (calculatedGa != null) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.current_gestational_age) + ":",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = calculatedGa.formatted,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (calculatedGa.isPostTerm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        // IVF Specific inputs
        if (draft.values.datingMethod.equals("IVF_TRANSFER", ignoreCase = true) || draft.values.datingMethod.equals("IVF", ignoreCase = true)) {
            ClinicalDateField(
                value = draft.values.transferDate,
                onValueChange = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.TRANSFER_DATE, it)) },
                label = stringResource(R.string.ivf_transfer_date),
                tag = "ivfTransferDate",
                enabled = enabled,
                error = draft.errors.contains(ClinicalValidationError.INVALID_DATE),
                showTodayButton = false,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.embryo_age),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.values.embryoAgeDays == "3",
                        onClick = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.EMBRYO_AGE_DAYS, "3")) },
                        label = { Text(stringResource(R.string.embryo_day_3)) },
                        enabled = enabled,
                        modifier = Modifier.testTag("embryoDay3"),
                    )
                    FilterChip(
                        selected = draft.values.embryoAgeDays == "5" || draft.values.embryoAgeDays.isEmpty(),
                        onClick = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.EMBRYO_AGE_DAYS, "5")) },
                        label = { Text(stringResource(R.string.embryo_day_5)) },
                        enabled = enabled,
                        modifier = Modifier.testTag("embryoDay5"),
                    )
                }
            }
        }

        EditorField(
            value = draft.values.notes,
            onValueChange = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.NOTES, it)) },
            label = stringResource(R.string.notes),
            tag = "datingNotes",
            enabled = enabled,
            singleLine = false,
        )

        EditorActions(
            enabled = enabled,
            saveTag = "saveDating",
            onCancel = { onEvent(AncEvent.CancelEditor) },
            onSave = { onEvent(AncEvent.SubmitDating) },
        )
    }
}
