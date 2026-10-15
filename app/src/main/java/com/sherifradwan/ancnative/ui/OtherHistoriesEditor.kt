package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.HistoriesDraftField
import com.sherifradwan.ancnative.state.SavePhase

@Composable
fun OtherHistoriesEditor(
    state: AncState,
    route: AncRoute.OtherHistoriesEditor,
    onEvent: (AncEvent) -> Unit,
) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.historiesDraft?.takeIf { it.patientId == route.patientId } ?: return
    val enabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR

    val noMedical = stringResource(R.string.no_medical_history)
    val noSurgical = stringResource(R.string.no_surgical_history)
    val noFamily = stringResource(R.string.no_family_history)
    val noAllergy = stringResource(R.string.no_allergy_history)

    EditorPage(
        title = stringResource(R.string.edit_other_histories),
        subtitle = "${patient.displayName} · ${patient.caseId}",
        tag = "otherHistoriesEditor",
    ) {
        EditorErrorCard(state, draft.errors)

        // 1-Tap Fill All Explicit Negative Histories
        Card(
            modifier = Modifier.fillMaxWidth().testTag("negativeHistoriesCard"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.histories_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                    modifier = Modifier.fillMaxWidth().testTag("fillAllNegativeHistories"),
                ) {
                    Text("✓ " + stringResource(R.string.fill_all_negative))
                }
            }
        }

        // Medical History
        EditorField(
            value = draft.values.medicalHistory,
            onValueChange = { onEvent(AncEvent.HistoriesDraftChanged(HistoriesDraftField.MEDICAL, it)) },
            label = stringResource(R.string.medical_history),
            tag = "medicalHistory",
            enabled = enabled,
        )
        QuickPresetChips(
            title = stringResource(R.string.medical_history),
            items = listOf(noMedical, "Chronic hypertension", "Type 2 Diabetes", "Asthma", "Hypothyroidism"),
            enabled = enabled,
            onSelect = { onEvent(AncEvent.HistoriesDraftChanged(HistoriesDraftField.MEDICAL, it)) },
            tagPrefix = "medHistoryPreset",
        )

        // Surgical History
        EditorField(
            value = draft.values.surgicalHistory,
            onValueChange = { onEvent(AncEvent.HistoriesDraftChanged(HistoriesDraftField.SURGICAL, it)) },
            label = stringResource(R.string.surgical_history),
            tag = "surgicalHistory",
            enabled = enabled,
        )
        QuickPresetChips(
            title = stringResource(R.string.surgical_history),
            items = listOf(noSurgical, "Appendectomy", "Previous Cesarean Section", "Myomectomy", "Laparoscopy"),
            enabled = enabled,
            onSelect = { onEvent(AncEvent.HistoriesDraftChanged(HistoriesDraftField.SURGICAL, it)) },
            tagPrefix = "surgHistoryPreset",
        )

        // Family History
        EditorField(
            value = draft.values.familyHistory,
            onValueChange = { onEvent(AncEvent.HistoriesDraftChanged(HistoriesDraftField.FAMILY, it)) },
            label = stringResource(R.string.family_history),
            tag = "familyHistory",
            enabled = enabled,
        )
        QuickPresetChips(
            title = stringResource(R.string.family_history),
            items = listOf(noFamily, "Family history of Diabetes", "Family history of Hypertension", "Family history of Twins", "Thalassemia trait"),
            enabled = enabled,
            onSelect = { onEvent(AncEvent.HistoriesDraftChanged(HistoriesDraftField.FAMILY, it)) },
            tagPrefix = "famHistoryPreset",
        )

        // Allergy History
        EditorField(
            value = draft.values.allergyHistory,
            onValueChange = { onEvent(AncEvent.HistoriesDraftChanged(HistoriesDraftField.ALLERGY, it)) },
            label = stringResource(R.string.allergy_history),
            tag = "allergyHistory",
            enabled = enabled,
        )
        QuickPresetChips(
            title = stringResource(R.string.allergy_history),
            items = listOf(noAllergy, "Penicillin allergy", "Sulfa drugs allergy", "NSAIDs allergy", "Latex allergy"),
            enabled = enabled,
            onSelect = { onEvent(AncEvent.HistoriesDraftChanged(HistoriesDraftField.ALLERGY, it)) },
            tagPrefix = "allergyHistoryPreset",
        )

        EditorActions(
            enabled = enabled,
            saveTag = "saveHistories",
            onCancel = { onEvent(AncEvent.CancelEditor) },
            onSave = { onEvent(AncEvent.SubmitOtherHistories) },
        )
    }
}
