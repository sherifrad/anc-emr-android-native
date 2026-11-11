package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.DeliveryPlanField
import com.sherifradwan.ancnative.state.PatientDraft
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.SaveUiError

/**
 * Bottom sheet that edits the four delivery plan boxes held in [PatientDraft.deliveryPlan].
 * It mirrors [MedicationSheet]: while a save runs the fields, Cancel and Save are disabled, and the
 * app's Undo bar rides in the [footer] slot. Save stays disabled until a field change makes the
 * draft touched (A16), Cancel closes an untouched sheet at once and asks first when touched, and
 * drag-down and back follow the same guarded rules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPlanSheet(
    draft: PatientDraft,
    savePhase: SavePhase,
    saveError: SaveUiError?,
    onEvent: (AncEvent) -> Unit,
    footer: @Composable () -> Unit = {},
) {
    val fields = draft.deliveryPlan ?: return
    val enabled = savePhase == SavePhase.IDLE || savePhase == SavePhase.ERROR
    val saving = savePhase == SavePhase.PENDING || savePhase == SavePhase.RECONCILING
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val requestCancel = {
        if (fields.touched) confirmDiscard = true else onEvent(AncEvent.CancelEditor)
    }

    ModalBottomSheet(
        onDismissRequest = { if (enabled) requestCancel() },
        sheetState = rememberGuardedSheetState(
            dismissAllowed = enabled,
            needsConfirm = fields.touched,
            onNeedsConfirm = { confirmDiscard = true },
        ),
        modifier = Modifier.testTag("deliveryPlanSheet"),
    ) {
        Column(modifier = Modifier.fillMaxWidth().imePadding()) {
            if (saving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag("deliveryPlanSaveProgress"))
            }
            Text(
                text = stringResource(R.string.edit_delivery_plan),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { heading() },
            )
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SheetSaveMessage(
                    savePhase = savePhase,
                    saveError = saveError,
                    fieldErrorMessage = if (draft.errors.isEmpty()) null else stringResource(R.string.delivery_plan_profile_invalid),
                    failedTitle = stringResource(R.string.delivery_plan_save_failed_title),
                    tag = "deliveryPlanSaveMessage",
                )
                if (fields.rewritesNotes) {
                    Text(
                        text = stringResource(R.string.delivery_plan_rewrite_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().testTag("deliveryPlanRewriteNotice"),
                    )
                }
                PlanField(
                    label = stringResource(R.string.delivery_hospital_title),
                    value = fields.hospital,
                    minLines = 1,
                    enabled = enabled,
                    fieldTag = "deliveryPlanHospitalField",
                    hint = { Text(stringResource(R.string.delivery_hospital_hint)) },
                ) { onEvent(AncEvent.DeliveryPlanFieldChanged(DeliveryPlanField.HOSPITAL, it)) }
                PlanField(
                    label = stringResource(R.string.delivery_fees_title),
                    value = fields.fees,
                    minLines = 1,
                    enabled = enabled,
                    fieldTag = "deliveryPlanFeesField",
                    hint = { Text(stringResource(R.string.delivery_fees_hint)) },
                ) { onEvent(AncEvent.DeliveryPlanFieldChanged(DeliveryPlanField.FEES, it)) }
                PlanField(
                    label = stringResource(R.string.delivery_plan_title),
                    value = fields.plan,
                    minLines = 3,
                    enabled = enabled,
                    fieldTag = "deliveryPlanPlanField",
                ) { onEvent(AncEvent.DeliveryPlanFieldChanged(DeliveryPlanField.PLAN, it)) }
                PlanField(
                    label = stringResource(R.string.delivery_postpartum_title),
                    value = fields.postpartum,
                    minLines = 1,
                    enabled = enabled,
                    fieldTag = "deliveryPlanPostpartumField",
                ) { onEvent(AncEvent.DeliveryPlanFieldChanged(DeliveryPlanField.POSTPARTUM, it)) }
            }
            SheetCancelSaveRow(
                enabled = enabled,
                saving = saving,
                saveEnabled = fields.touched,
                savingLabel = stringResource(R.string.delivery_plan_saving),
                onCancel = requestCancel,
                onSave = { onEvent(AncEvent.SubmitPatientProfile) },
                cancelTag = "cancelDeliveryPlan",
                saveTag = "saveDeliveryPlan",
                spinnerTag = "deliveryPlanSaveSpinner",
            )
            footer()
        }
    }

    if (confirmDiscard) {
        DiscardChangesDialog(
            onDiscard = {
                confirmDiscard = false
                onEvent(AncEvent.CancelEditor)
            },
            onKeep = { confirmDiscard = false },
            dialogTag = "deliveryPlanDiscardDialog",
            confirmTag = "deliveryPlanDiscardConfirm",
            keepTag = "deliveryPlanDiscardKeep",
        )
    }
}

/** One labelled multi-line box of the sheet; the visible label above is the field's accessibility name. */
@Composable
private fun PlanField(
    label: String,
    value: String,
    minLines: Int,
    enabled: Boolean,
    fieldTag: String,
    hint: (@Composable () -> Unit)? = null,
    onValueChange: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        // One accessibility node for the label and its field, so the label is read with the value.
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            minLines = minLines,
            placeholder = { Text(stringResource(R.string.no_record)) },
            supportingText = hint,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .revealOnFocus()
                .testTag(fieldTag),
        )
    }
}
