package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.ui.components.BandChip
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.LocalFieldFamily
import com.sherifradwan.ancnative.ui.components.PatientNameText
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.StatusKind
import com.sherifradwan.ancnative.ui.components.StatusTag
import com.sherifradwan.ancnative.ui.components.StatusTone
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.components.parityText
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LabPatientBanner(
    patient: DemoPatient,
    asOfDate: LocalDate,
    modifier: Modifier = Modifier,
) {
    val bgFamily = FieldFamilies.background()
    val isHigh = isHighRisk(patient.riskLevel)
    val isModerate = !isHigh && parseRiskTier(patient.riskLevel) == PatientRiskTier.MEDIUM

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RecordShapes.Large,
        color = bgFamily.container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PatientNameText(
                    name = patient.displayName,
                    style = RecordType.CardName,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (isHigh || isModerate) {
                    StatusTag(kind = if (isHigh) StatusKind.HighRisk else StatusKind.ModerateRisk)
                }
            }
            CompositionLocalProvider(LocalFieldFamily provides bgFamily) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val gaResult = GestationalAgeEngine.calculate(patient.dating, asOfDate)
                        ?.takeIf { it.isPostConception }
                        ?: patient.dating?.calculateGestationalAgeResult()?.takeIf { it.isPostConception }
                    if (gaResult != null) {
                        BandChip(ltrIsolate(gaResult.shortWeeksText), tone = null)
                    }
                    BandChip(
                        text = ltrIsolate(parityText(patient.gravida, patient.parity, patient.abortion)),
                        tone = null,
                    )
                    formatBloodGroup(patient.bloodGroupText)?.let { bg ->
                        BandChip(
                            text = ltrIsolate(bg),
                            tone = if (isRhNegative(patient.bloodGroupText)) StatusTone.High else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LabReadoutStrip(
    resultText: String,
    unitText: String,
    flagContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    valueTag: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RecordShapes.Small,
        color = FieldFamilies.background().container,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            ) {
                Text(
                    text = resultText.trim().ifBlank { "-" },
                    style = RecordType.HeaderName,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .let { if (valueTag != null) it.testTag(valueTag) else it },
                )
                if (unitText.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = unitText,
                        style = RecordType.Secondary,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
            flagContent?.invoke()
        }
    }
}

@Composable
internal fun LabMetadataDialog(
    title: String,
    dateValue: String,
    onDateChange: (String) -> Unit,
    dateTag: String,
    labNameValue: String,
    onLabNameChange: (String) -> Unit,
    labNameTag: String,
    labPresetTagPrefix: String,
    onDismiss: () -> Unit,
    enabled: Boolean = true,
    dateError: Boolean = false,
    dateFirst: Boolean = false,
    calculatedGaShortText: String? = null,
    doneTag: String? = null,
) {
    val bgFamily = FieldFamilies.background()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RecordShapes.Large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = title,
                style = RecordType.SectionTitle,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        text = {
            CompositionLocalProvider(LocalFieldFamily provides bgFamily) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val renderLabFields: @Composable () -> Unit = {
                        EditorField(
                            value = labNameValue,
                            onValueChange = onLabNameChange,
                            label = stringResource(R.string.lab_name),
                            tag = labNameTag,
                            enabled = enabled,
                            singleLine = true,
                            keyboardType = KeyboardType.Text,
                            externalLabel = true,
                            shape = RecordShapes.Small,
                            textStyle = RecordType.FieldValue,
                        )
                        QuickPresetChips(
                            title = stringResource(R.string.common_presets),
                            items = ClinicalVocabulary.labNamePresets,
                            enabled = enabled,
                            onSelect = onLabNameChange,
                            tagPrefix = labPresetTagPrefix,
                            useRecordStyle = true,
                        )
                    }
                    val renderDateField: @Composable () -> Unit = {
                        ClinicalDateField(
                            value = dateValue,
                            onValueChange = onDateChange,
                            label = stringResource(R.string.test_date),
                            tag = dateTag,
                            enabled = enabled,
                            error = dateError,
                            showTodayButton = true,
                            externalLabel = true,
                            useRecordStyle = true,
                        )
                    }

                    if (dateFirst) {
                        renderDateField()
                        renderLabFields()
                    } else {
                        renderLabFields()
                        renderDateField()
                    }

                    if (!calculatedGaShortText.isNullOrBlank()) {
                        Surface(
                            color = bgFamily.container,
                            shape = RecordShapes.Small,
                            modifier = Modifier.fillMaxWidth().testTag("labCalculatedGaCard"),
                        ) {
                            Text(
                                text = stringResource(R.string.calculated_ga_at_visit, ltrIsolate(calculatedGaShortText)),
                                style = RecordType.KeyValue,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RecordShapes.Small,
                modifier = Modifier
                    .minimumTouchTarget()
                    .let { if (doneTag != null) it.testTag(doneTag) else it },
            ) {
                Text(stringResource(R.string.done), style = RecordType.Action)
            }
        },
    )
}
