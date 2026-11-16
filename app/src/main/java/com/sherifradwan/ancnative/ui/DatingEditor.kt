package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.formatDmy
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.DatingDraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.activeFlowForRoute
import com.sherifradwan.ancnative.state.datingDraftToDating
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.FormFieldLabel
import com.sherifradwan.ancnative.ui.components.RecordFilterChip
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.StatusTone
import com.sherifradwan.ancnative.ui.components.StatusWord
import com.sherifradwan.ancnative.ui.components.currentFieldFamily
import com.sherifradwan.ancnative.ui.patienthub.components.SheetCancelSaveRow
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

    val currentDatingPreview = remember(draft.values) {
        datingDraftToDating(patient.id, draft.values)
    }
    val calculatedEdd = remember(currentDatingPreview) {
        GestationalAgeEngine.calculateEstimatedDueDate(currentDatingPreview)
    }
    val calculatedGa = remember(currentDatingPreview) {
        GestationalAgeEngine.calculate(currentDatingPreview, LocalDate.now())
    }

    val obstetricFamily = FieldFamilies.obstetric()
    val notesLabel = stringResource(R.string.notes)

    EditorPage(
        title = stringResource(R.string.dating_editor),
        subtitle = patient.displayName,
        tag = "datingEditor",
        showHeader = !inFlow,
        useRecordStyle = true,
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)

        RecordSectionCard(
            title = stringResource(R.string.dating_baseline_section),
            family = obstetricFamily,
        ) {
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
                shape = RecordShapes.Small,
                externalLabel = true,
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
                    externalLabel = true,
                    useRecordStyle = true,
                    allowClear = true,
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
                    externalLabel = true,
                    useRecordStyle = true,
                    allowClear = true,
                )
            }

            if (visibility.showIvf) {
                ClinicalDateField(
                    value = draft.values.transferDate,
                    onValueChange = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.TRANSFER_DATE, it)) },
                    label = stringResource(R.string.ivf_transfer_date),
                    tag = "ivfTransferDate",
                    enabled = enabled,
                    error = draft.errors.contains(ClinicalValidationError.INVALID_DATE),
                    showTodayButton = false,
                    externalLabel = true,
                    useRecordStyle = true,
                    allowClear = true,
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormFieldLabel(stringResource(R.string.embryo_age))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val embryoChoices = listOf(
                            Triple(3, stringResource(R.string.embryo_day_3), "embryoDay3"),
                            Triple(5, stringResource(R.string.embryo_day_5), "embryoDay5"),
                            Triple(6, stringResource(R.string.embryo_day_6), "embryoDay6"),
                        )
                        embryoChoices.forEach { (day, label, chipTag) ->
                            val isSelected = (day == 5 && (draft.values.embryoAgeDays.isEmpty() || draft.values.embryoAgeDays == "5")) ||
                                draft.values.embryoAgeDays == day.toString()
                            RecordFilterChip(
                                selected = isSelected,
                                label = label,
                                onClick = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.EMBRYO_AGE_DAYS, day.toString())) },
                                tag = chipTag,
                                enabled = enabled,
                                wrap = true,
                                minTouchTarget = true,
                            )
                        }
                    }
                }
            }

            if (!visibility.showLmp) RecordedDateNote(R.string.lmp_on_record, draft.values.lmpDate)
            if (!visibility.showUltrasoundEdd) RecordedDateNote(R.string.ultrasound_edd_on_record, draft.values.ultrasoundEdd)
            if (!visibility.showIvf) RecordedDateNote(R.string.transfer_date_on_record, draft.values.transferDate)

            ClinicalDateField(
                value = draft.values.agreedEdd,
                onValueChange = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.AGREED_EDD, it)) },
                label = stringResource(R.string.agreed_edd),
                tag = "agreedEdd",
                enabled = enabled,
                error = draft.errors.contains(ClinicalValidationError.INVALID_DATE),
                showTodayButton = false,
                externalLabel = true,
                useRecordStyle = true,
                allowClear = true,
            )
        }

        if (calculatedEdd != null || calculatedGa != null) {
            val stackMetrics = LocalDensity.current.fontScale > 1.5f
            RecordSectionCard(
                title = stringResource(R.string.dating_calculated_section),
                family = obstetricFamily,
            ) {
                val tiles: @Composable (Modifier) -> Unit = { tileModifier ->
                    if (calculatedEdd != null) {
                        CalculatedDatingTile(
                            label = stringResource(R.string.calculated_edd),
                            value = formatDmy(calculatedEdd),
                            isPostTerm = false,
                            modifier = tileModifier,
                        )
                    }
                    if (calculatedGa != null) {
                        CalculatedDatingTile(
                            label = stringResource(R.string.current_gestational_age),
                            value = calculatedGa.shortWeeksText,
                            isPostTerm = calculatedGa.isPostTerm,
                            modifier = tileModifier,
                        )
                    }
                }
                if (stackMetrics) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        tiles(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        tiles(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }

        RecordSectionCard(
            title = notesLabel,
            family = obstetricFamily,
        ) {
            EditorField(
                value = draft.values.notes,
                onValueChange = { onEvent(AncEvent.DatingDraftChanged(DatingDraftField.NOTES, it)) },
                label = "",
                tag = "datingNotes",
                modifier = Modifier.semantics { contentDescription = notesLabel },
                enabled = enabled,
                singleLine = false,
                shape = RecordShapes.Small,
                textStyle = RecordType.FieldValue,
                externalLabel = false,
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
                onSave = { onEvent(AncEvent.SubmitDating) },
                cancelTag = "cancelDating",
                saveTag = "saveDating",
                spinnerTag = "datingSaveSpinner",
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalculatedDatingTile(
    label: String,
    value: String,
    isPostTerm: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = currentFieldFamily().container,
        shape = RecordShapes.Small,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FormFieldLabel(label)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = RecordType.KeyValue,
                    color = if (isPostTerm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                if (isPostTerm) {
                    StatusWord(
                        text = stringResource(R.string.post_term_badge),
                        tone = StatusTone.High,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordedDateNote(labelRes: Int, isoValue: String) {
    val shown = formatDmy(isoValue) ?: return
    Text(
        text = stringResource(labelRes, shown),
        style = RecordType.Secondary,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
