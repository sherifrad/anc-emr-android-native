package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.HistoriesDraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.activeFlowForRoute
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.patienthub.components.SheetCancelSaveRow

private data class HistorySectionSpec(
    val title: String,
    val value: String,
    val field: HistoriesDraftField,
    val fieldTag: String,
    val presets: List<String>,
    val presetTagPrefix: String,
)

@Composable
fun OtherHistoriesEditor(
    state: AncState,
    route: AncRoute.OtherHistoriesEditor,
    onEvent: (AncEvent) -> Unit,
) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.historiesDraft?.takeIf { it.patientId == route.patientId } ?: return
    val enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR
    val inFlow = state.activeFlowForRoute() != null

    val noMedical = stringResource(R.string.no_medical_history)
    val noSurgical = stringResource(R.string.no_surgical_history)
    val noFamily = stringResource(R.string.no_family_history)
    val noAllergy = stringResource(R.string.no_allergy_history)
    val presetsTitle = stringResource(R.string.common_presets)
    val backgroundFamily = FieldFamilies.background()

    val sections = listOf(
        HistorySectionSpec(
            title = stringResource(R.string.medical_history),
            value = draft.values.medicalHistory,
            field = HistoriesDraftField.MEDICAL,
            fieldTag = "medicalHistory",
            presets = listOf(noMedical, "Chronic hypertension", "Type 2 Diabetes", "Asthma", "Hypothyroidism"),
            presetTagPrefix = "medHistoryPreset",
        ),
        HistorySectionSpec(
            title = stringResource(R.string.surgical_history),
            value = draft.values.surgicalHistory,
            field = HistoriesDraftField.SURGICAL,
            fieldTag = "surgicalHistory",
            presets = listOf(noSurgical, "Appendectomy", "Previous Cesarean Section", "Myomectomy", "Laparoscopy"),
            presetTagPrefix = "surgHistoryPreset",
        ),
        HistorySectionSpec(
            title = stringResource(R.string.family_history),
            value = draft.values.familyHistory,
            field = HistoriesDraftField.FAMILY,
            fieldTag = "familyHistory",
            presets = listOf(
                noFamily,
                "Family history of Diabetes",
                "Family history of Hypertension",
                "Family history of Twins",
                "Thalassemia trait",
            ),
            presetTagPrefix = "famHistoryPreset",
        ),
        HistorySectionSpec(
            title = stringResource(R.string.allergy_history),
            value = draft.values.allergyHistory,
            field = HistoriesDraftField.ALLERGY,
            fieldTag = "allergyHistory",
            presets = listOf(noAllergy, "Penicillin allergy", "Sulfa drugs allergy", "NSAIDs allergy", "Latex allergy"),
            presetTagPrefix = "allergyHistoryPreset",
        ),
    )

    EditorPage(
        title = stringResource(R.string.edit_other_histories),
        subtitle = patient.displayName,
        tag = "otherHistoriesEditor",
        showHeader = !inFlow,
        useRecordStyle = true,
    ) {
        GenericEditorErrorCard(draft.errors.isNotEmpty(), state, useRecordStyle = true)

        OutlinedCard(
            modifier = Modifier.fillMaxWidth().testTag("negativeHistoriesCard"),
            shape = RecordShapes.Large,
            colors = CardDefaults.outlinedCardColors(containerColor = backgroundFamily.container),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.negative_histories_title),
                    style = RecordType.CardName,
                    color = backgroundFamily.label,
                )
                OutlinedButton(
                    onClick = {
                        if (draft.values.medicalHistory.isBlank()) {
                            onEvent(AncEvent.HistoriesDraftChanged(HistoriesDraftField.MEDICAL, noMedical))
                        }
                        if (draft.values.surgicalHistory.isBlank()) {
                            onEvent(AncEvent.HistoriesDraftChanged(HistoriesDraftField.SURGICAL, noSurgical))
                        }
                        if (draft.values.familyHistory.isBlank()) {
                            onEvent(AncEvent.HistoriesDraftChanged(HistoriesDraftField.FAMILY, noFamily))
                        }
                        if (draft.values.allergyHistory.isBlank()) {
                            onEvent(AncEvent.HistoriesDraftChanged(HistoriesDraftField.ALLERGY, noAllergy))
                        }
                    },
                    enabled = enabled,
                    shape = RecordShapes.Small,
                    border = BorderStroke(
                        1.dp,
                        if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumTouchTarget()
                        .testTag("fillAllNegativeHistories"),
                ) {
                    Text(
                        text = stringResource(R.string.fill_all_negative),
                        style = RecordType.Action,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        sections.forEach { section ->
            RecordSectionCard(
                title = section.title,
                family = backgroundFamily,
            ) {
                EditorField(
                    value = section.value,
                    onValueChange = { onEvent(AncEvent.HistoriesDraftChanged(section.field, it)) },
                    label = "",
                    tag = section.fieldTag,
                    modifier = Modifier.semantics { contentDescription = section.title },
                    enabled = enabled,
                    shape = RecordShapes.Small,
                    textStyle = RecordType.FieldValue,
                    externalLabel = false,
                )
                QuickPresetChips(
                    title = presetsTitle,
                    items = section.presets,
                    enabled = enabled,
                    onSelect = { onEvent(AncEvent.HistoriesDraftChanged(section.field, it)) },
                    tagPrefix = section.presetTagPrefix,
                    useRecordStyle = true,
                )
            }
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
                onSave = { onEvent(AncEvent.SubmitOtherHistories) },
                cancelTag = "cancelHistories",
                saveTag = "saveHistories",
                spinnerTag = "historiesSaveSpinner",
            )
        }
    }
}
