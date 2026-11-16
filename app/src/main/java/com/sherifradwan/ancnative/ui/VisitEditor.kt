package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.model.bpLevel
import com.sherifradwan.ancnative.model.catalog.ClinicalCatalogRepository
import com.sherifradwan.ancnative.model.catalog.CustomChoiceList
import com.sherifradwan.ancnative.model.catalog.CustomChoiceValidationResult
import com.sherifradwan.ancnative.model.resolveLastVisitVitals
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncRoute
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.DraftError
import com.sherifradwan.ancnative.state.DraftField
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.stringRes
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType

@Composable
fun VisitEditor(
    state: AncState,
    route: AncRoute.VisitEditor,
    onEvent: (AncEvent) -> Unit,
    onSave: () -> Unit,
) {
    val patient = state.patient(route.patientId) ?: return
    val draft = state.draft?.takeIf { it.patientId == route.patientId } ?: return
    val editingEnabled = state.savePhase == SavePhase.IDLE || state.savePhase == SavePhase.ERROR
    val context = LocalContext.current
    val catalogRepo = remember { ClinicalCatalogRepository.getInstance(context) }

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 4

    var showCustomDialog by remember { mutableStateOf<CustomChoiceList?>(null) }
    var customInput by remember { mutableStateOf("") }
    var customInputError by remember { mutableStateOf<String?>(null) }

    val calculatedGa = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, draft.values.date)

    val lastVitals = remember(patient.visits, draft.visitId) {
        resolveLastVisitVitals(patient.visits, draft.visitId)
    }

    val stepTitles = listOf(
        R.string.step_date_bp,
        R.string.step_wt_fh,
        R.string.step_findings,
        R.string.step_review_save,
    )

    val dateError = when {
        DraftError.DATE_REQUIRED in draft.errors -> stringResource(R.string.required_date)
        DraftError.INVALID_DATE in draft.errors -> stringResource(R.string.invalid_date)
        DraftError.DATE_IN_FUTURE in draft.errors -> stringResource(R.string.visit_date_in_future)
        else -> null
    }
    val bpError = when {
        DraftError.BLOOD_PRESSURE_INVALID in draft.errors -> stringResource(R.string.blood_pressure_invalid)
        else -> null
    }
    val weightError = when {
        DraftError.WEIGHT_INVALID in draft.errors -> stringResource(R.string.weight_invalid)
        else -> null
    }

    val customBlankError = stringResource(R.string.custom_drug_blank_error)
    val customTooLongError = stringResource(R.string.custom_drug_too_long_error)
    val customDuplicateError = stringResource(R.string.custom_drug_duplicate_error)
    val customInvalidCharsError = stringResource(R.string.custom_drug_invalid_chars_error)
    val customNotFoundError = stringResource(R.string.custom_drug_not_found_error)

    val bpChoices by catalogRepo.customChoices(CustomChoiceList.VISIT_BP).collectAsState(emptyList())
    val weightChoices by catalogRepo.customChoices(CustomChoiceList.VISIT_WEIGHT).collectAsState(emptyList())
    val oedemaChoices by catalogRepo.customChoices(CustomChoiceList.VISIT_OEDEMA).collectAsState(emptyList())
    val movementsChoices by catalogRepo.customChoices(CustomChoiceList.VISIT_MOVEMENTS).collectAsState(emptyList())
    val urineChoices by catalogRepo.customChoices(CustomChoiceList.VISIT_URINE).collectAsState(emptyList())

    val onDraftChanged: (DraftField, String) -> Unit = { field, value ->
        onEvent(AncEvent.DraftChanged(field, value))
    }
    val onOpenCustomDialog: (CustomChoiceList) -> Unit = { list ->
        showCustomDialog = list
        customInput = ""
        customInputError = null
    }

    val pinnedBottomBar: @Composable () -> Unit = {
        if (state.savePhase != SavePhase.IDLE) {
            VisitBottomActionRow(
                editingEnabled = editingEnabled,
                secondaryLabel = stringResource(R.string.cancel),
                secondaryTag = "cancelVisit",
                onSecondaryClick = { onEvent(AncEvent.CancelEditor) },
                primaryLabel = stringResource(R.string.save),
                primaryTag = "saveVisit",
                onPrimaryClick = onSave,
            )
        } else {
            when (currentStep) {
                0 -> VisitBottomActionRow(
                    editingEnabled = editingEnabled,
                    secondaryLabel = stringResource(R.string.cancel),
                    secondaryTag = "cancelVisit",
                    onSecondaryClick = { onEvent(AncEvent.CancelEditor) },
                    primaryLabel = stringResource(R.string.next_step),
                    primaryTag = "nextStep",
                    onPrimaryClick = { currentStep = 1 },
                )
                in 1..2 -> VisitBottomActionRow(
                    editingEnabled = editingEnabled,
                    secondaryLabel = stringResource(R.string.back_step),
                    secondaryTag = "backStep",
                    onSecondaryClick = { currentStep-- },
                    primaryLabel = stringResource(R.string.next_step),
                    primaryTag = "nextStep",
                    onPrimaryClick = { currentStep++ },
                )
                else -> VisitBottomActionRow(
                    editingEnabled = editingEnabled,
                    secondaryLabel = stringResource(R.string.back_step),
                    secondaryTag = "backStep",
                    onSecondaryClick = { currentStep-- },
                    primaryLabel = stringResource(R.string.save_visit),
                    primaryTag = "saveVisit",
                    onPrimaryClick = onSave,
                )
            }
        }
    }

    EditorPage(
        title = if (draft.visitId == null) stringResource(R.string.add_visit) else stringResource(R.string.edit_visit),
        subtitle = "",
        tag = "visitEditor",
        bottomBar = pinnedBottomBar,
        useRecordStyle = true,
    ) {
        VisitPatientBanner(patient = patient, gaResult = calculatedGa)

        if (DraftError.STALE_REVISION in draft.errors) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().testTag("staleError"),
                shape = RecordShapes.Large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    text = stringResource(R.string.stale_visit),
                    style = RecordType.Action,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }

        if (state.savePhase != SavePhase.IDLE) {
            val isFailure = state.savePhase == SavePhase.ERROR || state.savePhase == SavePhase.UNRESOLVED
            val bgColor = if (isFailure) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
            val textColor = if (isFailure) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            val borderColor = if (isFailure) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().testTag("saveStatus"),
                shape = RecordShapes.Large,
                border = BorderStroke(1.dp, borderColor),
                colors = CardDefaults.outlinedCardColors(containerColor = bgColor),
            ) {
                Text(
                    text = stringResource(state.saveStatusResource()),
                    style = RecordType.Action,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }

        if (state.savePhase != SavePhase.IDLE) {
            RecordSectionCard(
                title = stringResource(R.string.visit_recorded_values_section),
                family = FieldFamilies.patient(),
            ) {
                ClinicalDateField(
                    value = draft.values.date,
                    onValueChange = { onDraftChanged(DraftField.DATE, it) },
                    label = stringResource(R.string.visit_date),
                    tag = "visitDate",
                    enabled = editingEnabled,
                    error = dateError != null,
                    errorMessage = dateError,
                    showTodayButton = true,
                    externalLabel = true,
                    useRecordStyle = true,
                )
                EditorField(
                    value = draft.values.bloodPressure,
                    onValueChange = { onDraftChanged(DraftField.BLOOD_PRESSURE, it) },
                    label = "${stringResource(R.string.blood_pressure)} (${stringResource(R.string.mmhg)})",
                    tag = "visitBloodPressure",
                    errorMessage = bpError,
                    singleLine = true,
                    enabled = editingEnabled,
                    externalLabel = true,
                    shape = RecordShapes.Small,
                    textStyle = RecordType.FieldValue,
                )
                if (bpLevel(draft.values.bloodPressure) != BpLevel.NORMAL) {
                    BpAlertLabel(draft.values.bloodPressure)
                }
                EditorField(
                    value = draft.values.weight,
                    onValueChange = { onDraftChanged(DraftField.WEIGHT, it) },
                    label = "${stringResource(R.string.weight)} (kg)",
                    tag = "visitWeight",
                    errorMessage = weightError,
                    singleLine = true,
                    enabled = editingEnabled,
                    externalLabel = true,
                    shape = RecordShapes.Small,
                    textStyle = RecordType.FieldValue,
                )
                EditorField(
                    value = draft.values.findings,
                    onValueChange = { onDraftChanged(DraftField.FINDINGS, it) },
                    label = stringResource(R.string.findings),
                    tag = "visitFindings",
                    enabled = editingEnabled,
                    externalLabel = true,
                    shape = RecordShapes.Small,
                    textStyle = RecordType.FieldValue,
                )
                EditorField(
                    value = draft.values.notes,
                    onValueChange = { onDraftChanged(DraftField.NOTES, it) },
                    label = stringResource(R.string.notes),
                    tag = "visitNotes",
                    enabled = editingEnabled,
                    externalLabel = true,
                    shape = RecordShapes.Small,
                    textStyle = RecordType.FieldValue,
                )
            }
        } else {
            VisitStepHeader(
                currentStep = currentStep,
                totalSteps = totalSteps,
                stepTitleRes = stepTitles[currentStep],
                stepTitles = stepTitles,
                editingEnabled = editingEnabled,
                onStepClick = { if (editingEnabled) currentStep = it },
            )

            when (currentStep) {
                0 -> VisitStepDateAndBp(
                    patient = patient,
                    draftValues = draft.values,
                    lastVitals = lastVitals,
                    bpChoices = bpChoices,
                    dateError = dateError,
                    bpError = bpError,
                    editingEnabled = editingEnabled,
                    onDraftChanged = onDraftChanged,
                    onOpenCustomDialog = onOpenCustomDialog,
                )
                1 -> VisitStepWeightAndFhr(
                    draftValues = draft.values,
                    lastVitals = lastVitals,
                    weightChoices = weightChoices,
                    weightError = weightError,
                    editingEnabled = editingEnabled,
                    onDraftChanged = onDraftChanged,
                    onOpenCustomDialog = onOpenCustomDialog,
                )
                2 -> VisitStepFindings(
                    draftValues = draft.values,
                    oedemaChoices = oedemaChoices,
                    movementsChoices = movementsChoices,
                    urineChoices = urineChoices,
                    editingEnabled = editingEnabled,
                    onDraftChanged = onDraftChanged,
                    onOpenCustomDialog = onOpenCustomDialog,
                )
                else -> VisitStepReview(
                    patient = patient,
                    draftValues = draft.values,
                    calculatedGa = calculatedGa,
                    editingEnabled = editingEnabled,
                    onStepClick = { if (editingEnabled) currentStep = it },
                    onDraftChanged = onDraftChanged,
                )
            }
        }
    }

    if (showCustomDialog != null) {
        val list = showCustomDialog!!
        val titleRes = when (list) {
            CustomChoiceList.VISIT_BP -> R.string.custom_bp_title
            CustomChoiceList.VISIT_WEIGHT -> R.string.custom_weight_title
            CustomChoiceList.VISIT_FUNDAL_HEIGHT -> R.string.custom_fh_title
            CustomChoiceList.VISIT_OEDEMA -> R.string.custom_oedema_title
            CustomChoiceList.VISIT_MOVEMENTS -> R.string.custom_movements_title
            CustomChoiceList.VISIT_URINE -> R.string.custom_urine_title
            else -> R.string.add_other_choice
        }
        AlertDialog(
            onDismissRequest = { showCustomDialog = null },
            shape = RecordShapes.Large,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(titleRes),
                    style = RecordType.SectionTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = {
                            customInput = it
                            customInputError = null
                        },
                        label = { Text(stringResource(titleRes)) },
                        singleLine = true,
                        isError = customInputError != null,
                        shape = RecordShapes.Small,
                        textStyle = RecordType.FieldValue,
                        colors = editorFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("customChoiceInput"),
                    )
                    if (customInputError != null) {
                        Text(
                            text = customInputError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = RecordType.Tag,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = customInput.trim()
                        val res = catalogRepo.addCustomChoice(list, trimmed, emptyList())
                        when (res) {
                            is CustomChoiceValidationResult.Valid -> {
                                val label = res.sanitizedLabel
                                when (list) {
                                    CustomChoiceList.VISIT_BP -> onDraftChanged(DraftField.BLOOD_PRESSURE, label)
                                    CustomChoiceList.VISIT_WEIGHT -> onDraftChanged(DraftField.WEIGHT, label)
                                    CustomChoiceList.VISIT_FUNDAL_HEIGHT -> onDraftChanged(DraftField.FUNDAL_HEIGHT, label)
                                    CustomChoiceList.VISIT_OEDEMA -> onDraftChanged(DraftField.OEDEMA, label)
                                    CustomChoiceList.VISIT_MOVEMENTS -> onDraftChanged(DraftField.FETAL_MOVEMENTS, label)
                                    CustomChoiceList.VISIT_URINE -> onDraftChanged(DraftField.URINE_PROTEIN, label)
                                    else -> Unit
                                }
                                showCustomDialog = null
                            }
                            is CustomChoiceValidationResult.Invalid -> {
                                customInputError = when (res.reason) {
                                    CustomChoiceValidationResult.Invalid.Reason.BLANK -> customBlankError
                                    CustomChoiceValidationResult.Invalid.Reason.TOO_LONG -> customTooLongError
                                    CustomChoiceValidationResult.Invalid.Reason.DUPLICATE -> customDuplicateError
                                    CustomChoiceValidationResult.Invalid.Reason.INVALID_CHARACTERS -> customInvalidCharsError
                                    CustomChoiceValidationResult.Invalid.Reason.NOT_FOUND -> customNotFoundError
                                }
                            }
                        }
                    },
                    shape = RecordShapes.Small,
                    modifier = Modifier.minimumTouchTarget().testTag("customChoiceConfirm"),
                ) {
                    Text(stringResource(R.string.save), style = RecordType.Action)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCustomDialog = null },
                    shape = RecordShapes.Small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.minimumTouchTarget().testTag("customChoiceCancel"),
                ) {
                    Text(stringResource(R.string.cancel), style = RecordType.Action)
                }
            },
        )
    }
}

@Composable
private fun VisitBottomActionRow(
    editingEnabled: Boolean,
    secondaryLabel: String,
    secondaryTag: String,
    onSecondaryClick: () -> Unit,
    primaryLabel: String,
    primaryTag: String,
    onPrimaryClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onSecondaryClick,
            enabled = editingEnabled,
            shape = RecordShapes.Small,
            border = BorderStroke(
                1.dp,
                if (editingEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .minimumTouchTarget()
                .testTag(secondaryTag),
        ) {
            Text(
                text = secondaryLabel,
                style = RecordType.Action,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onPrimaryClick,
            enabled = editingEnabled,
            shape = RecordShapes.Small,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .minimumTouchTarget()
                .testTag(primaryTag),
        ) {
            Text(
                text = primaryLabel,
                style = RecordType.Action,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun AncState.saveStatusResource(): Int = when (savePhase) {
    SavePhase.IDLE -> R.string.save
    SavePhase.PENDING -> R.string.save_pending
    SavePhase.RECONCILING -> R.string.save_reconciling
    SavePhase.UNRESOLVED -> R.string.save_unresolved
    SavePhase.ERROR -> saveError.stringRes()
}
