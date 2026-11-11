package com.sherifradwan.ancnative.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.formatDmy
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.activeFlowForRoute
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.DatingDraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.datingDraftToDating
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
    val inFlow = state.activeFlowForRoute() != null

    // Compute live gestational age and EDD preview from current draft inputs
    val currentDatingPreview = remember(draft.values) {
        datingDraftToDating(patient.id, draft.values)
    }
    val calculatedEdd = remember(currentDatingPreview) {
        GestationalAgeEngine.calculateEstimatedDueDate(currentDatingPreview)
    }
    val calculatedGa = remember(currentDatingPreview) {
        GestationalAgeEngine.calculate(currentDatingPreview, LocalDate.now())
    }

    EditorPage(
        title = stringResource(R.string.dating_editor),
        subtitle = "${patient.displayName} · Gestational Age & Estimated Due Date",
        tag = "datingEditor",
        showHeader = !inFlow,
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state)

        // CARD 1: BASELINE DATING PARAMETERS
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
                    text = "1. Baseline Dating Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                val methodOptions = listOf(
                    "LMP" to stringResource(R.string.lmp_date),
                    "Ultrasound" to stringResource(R.string.ultrasound),
                    "IVF_TRANSFER" to stringResource(R.string.ivf_transfer_date),
                    "Clinical" to stringResource(R.string.dating_method_clinical),
                )
                val visibility = datingFieldVisibility(draft.values.datingMethod)
                val selectedMethod = visibility.selectedMethod
                ClinicalDropdownField(
                    label = stringResource(R.string.dating_method),
                    selectedValue = methodOptions.firstOrNull { it.first == selectedMethod }?.second ?: "",
                    options = methodOptions.map { it.second },
                    onSelect = { picked ->
                        methodOptions.firstOrNull { it.second == picked }?.let { (key, _) ->
                            onEvent(AncEvent.DatingDraftChanged(DatingDraftField.DATING_METHOD, key))
                        }
                    },
                    enabled = enabled,
                    tag = "datingMethodDropdown",
                    modifier = Modifier.fillMaxWidth(),
                )

                if (visibility.showLmp) {
                    ClinicalDateField(
                        value = draft.values.lmpDate,
                        onValueChange = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.LMP_DATE, it)) },
                        label = stringResource(R.string.lmp_date),
                        tag = "lmpDate",
                        enabled = enabled,
                        error = draft.errors.contains(ClinicalValidationError.INVALID_DATE),
                        showTodayButton = true,
                    )
                }

                if (visibility.showUltrasoundEdd) {
                    ClinicalDateField(
                        value = draft.values.ultrasoundEdd,
                        onValueChange = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.ULTRASOUND_EDD, it)) },
                        label = stringResource(R.string.ultrasound_edd),
                        tag = "ultrasoundEdd",
                        enabled = enabled,
                        error = draft.errors.contains(ClinicalValidationError.INVALID_DATE),
                        showTodayButton = false,
                    )
                }

                // IVF Specific inputs
                if (visibility.showIvf) {
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
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
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
                            FilterChip(
                                selected = draft.values.embryoAgeDays == "6",
                                onClick = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.EMBRYO_AGE_DAYS, "6")) },
                                label = { Text(stringResource(R.string.embryo_day_6)) },
                                enabled = enabled,
                                modifier = Modifier.testTag("embryoDay6"),
                            )
                        }
                    }
                }


                if (!visibility.showLmp) RecordedDateNote(R.string.lmp_on_record, draft.values.lmpDate)
                if (!visibility.showUltrasoundEdd) RecordedDateNote(R.string.ultrasound_edd_on_record, draft.values.ultrasoundEdd)
                if (!visibility.showIvf) RecordedDateNote(R.string.transfer_date_on_record, draft.values.transferDate)

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
            }
        }

        // CARD 2: DATING METHOD & ESTIMATED DUE DATE PREVIEW
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
                    text = "2. Dating Method & GA Calculation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Live calculation card if EDD or GA is available
                AnimatedVisibility(visible = calculatedEdd != null || calculatedGa != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (calculatedEdd != null) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = stringResource(R.string.calculated_edd) + ":",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
            }
        }

        // CARD 3: CLINICAL NOTES
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
                    text = "3. Clinical Notes & Observations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                EditorField(
                    value = draft.values.notes,
                    onValueChange = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.NOTES, it)) },
                    label = stringResource(R.string.notes),
                    tag = "datingNotes",
                    enabled = enabled,
                    singleLine = false,
                )
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
                text = "Gestational age and dating records encrypted locally (AES-256 GCM) · Local Vault Security",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!inFlow) {
            EditorActions(
                enabled = enabled,
                saveTag = "saveDating",
                onCancel = { onEvent(AncEvent.CancelEditor) },
                onSave = { onEvent(AncEvent.SubmitDating) },
            )
        }
    }
}

@Composable
private fun RecordedDateNote(labelRes: Int, isoValue: String) {
    val shown = formatDmy(isoValue) ?: return
    Text(
        text = stringResource(labelRes, shown),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
