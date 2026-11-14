package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.datingSummaryOf
import com.sherifradwan.ancnative.model.datingTextRows
import com.sherifradwan.ancnative.model.previousDeliverySummary
import com.sherifradwan.ancnative.ui.AllergyState
import com.sherifradwan.ancnative.ui.ClinicalCardTokens
import com.sherifradwan.ancnative.ui.classifyAllergy
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.LabeledValue
import com.sherifradwan.ancnative.ui.components.LabeledValueRow
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.SectionTextAction
import com.sherifradwan.ancnative.ui.isDarkTheme
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun DatingSection(
    patient: DemoPatient,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val dating = patient.dating
    val summary = remember(patient) { datingSummaryOf(patient, LocalDate.now()) }
    RecordSectionCard(
        title = stringResource(R.string.pregnancy_dating_title),
        family = FieldFamilies.patient(),
        modifier = modifier.testTag("datingCard"),
        action = onEdit?.let {
            {
                SectionTextAction(
                    text = stringResource(R.string.action_edit),
                    contentDescription = stringResource(R.string.edit_dating),
                    onClick = it,
                    modifier = Modifier.testTag("editDating"),
                )
            }
        },
    ) {
        if (dating == null) {
            Text(
                text = stringResource(R.string.no_dating_recorded),
                style = RecordType.Missing,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@RecordSectionCard
        }

        val rows = datingTextRows(summary)
        val originRow = rows.firstOrNull { !it.isEdd && it.label != "Method" }
        val eddRow = rows.firstOrNull { it.isEdd }
        val methodRow = rows.firstOrNull { it.label == "Method" }

        if (originRow != null) {
            LabeledValueRow {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("datingField-${originRow.label}"),
                ) {
                    LabeledValue(
                        label = originRow.label,
                        value = originRow.value,
                        emphasis = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (eddRow != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("datingField-${eddRow.label}"),
                    ) {
                        LabeledValue(
                            label = eddRow.label,
                            value = eddRow.value,
                            emphasis = true,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("dueDateBox"),
                        )
                    }
                }
            }
        } else if (eddRow != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("datingField-${eddRow.label}"),
            ) {
                LabeledValue(
                    label = eddRow.label,
                    value = eddRow.value,
                    emphasis = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dueDateBox"),
                )
            }
        }

        methodRow?.let { row ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("datingField-Method"),
            ) {
                LabeledValue(
                    label = row.label,
                    value = row.value,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        summary.discordance?.let { advice ->
            val dark = isDarkTheme()
            SectionBanner(
                text = stringResource(
                    R.string.dating_discordance_alert,
                    abs(advice.differenceDays),
                    advice.thresholdDays,
                ),
                container = if (dark) ClinicalCardTokens.WarningDarkBg else ClinicalCardTokens.WarningBg,
                accent = if (dark) ClinicalCardTokens.WarningDarkAccent else ClinicalCardTokens.WarningAccent,
                content = if (dark) ClinicalCardTokens.WarningDarkText else ClinicalCardTokens.WarningText,
                modifier = Modifier.testTag("datingDiscordanceAlert"),
            )
        }

        if (!dating.notes.isNullOrBlank()) {
            LabeledValue(
                label = stringResource(R.string.notes),
                value = dating.notes,
                modifier = Modifier.fillMaxWidth().testTag("datingField-Notes"),
            )
        }
    }
}

/** Full-width wrapping banner: warning icon plus bold text on a tinted surface. No actions. */
@Composable
private fun SectionBanner(
    text: String,
    container: Color,
    accent: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RecordShapes.Small,
        color = container,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text(
                text = text,
                style = RecordType.Secondary.copy(fontWeight = FontWeight.Bold),
                color = content,
            )
        }
    }
}

@Composable
fun ObstetricHistorySection(
    patient: DemoPatient,
    modifier: Modifier = Modifier,
) {
    RecordSectionCard(
        title = stringResource(R.string.obstetric_history_title),
        family = FieldFamilies.obstetric(),
        modifier = modifier.testTag("obstetricHistoryCard"),
    ) {
        LabeledValueRow(
            modifier = Modifier.testTag("obstetricParityBadge"),
        ) {
            LabeledValue(
                label = stringResource(R.string.gravida),
                value = (patient.gravida ?: 1L).toString(),
                emphasis = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            LabeledValue(
                label = stringResource(R.string.parity),
                value = (patient.parity ?: 0L).toString(),
                emphasis = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            LabeledValue(
                label = stringResource(R.string.abortion),
                value = (patient.abortion ?: 0L).toString(),
                emphasis = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
fun ParityPregnanciesSection(
    patient: DemoPatient,
    onAddPregnancy: (() -> Unit)?,
    onEditPregnancy: ((String) -> Unit)?,
    onDeletePregnancy: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    RecordSectionCard(
        title = stringResource(R.string.previous_pregnancies),
        family = FieldFamilies.obstetric(),
        modifier = modifier.testTag("obstetricParityCard"),
        action = onAddPregnancy?.let {
            {
                SectionTextAction(
                    text = stringResource(R.string.action_add),
                    contentDescription = stringResource(R.string.add_previous_pregnancy),
                    onClick = it,
                    modifier = Modifier.testTag("addPregnancyButton"),
                )
            }
        },
    ) {
        if (patient.previousPregnancies.isEmpty()) {
            Text(
                text = previousDeliverySummary(patient),
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                patient.previousPregnancies.forEach { pregnancy ->
                    PastDeliveryMicroRow(
                        pregnancy = pregnancy,
                        onEdit = onEditPregnancy?.let { edit -> { edit(pregnancy.id) } },
                        onDelete = onDeletePregnancy?.let { del -> { del(pregnancy.id) } },
                    )
                }
            }
        }
    }
}

@Composable
fun MedicalHistoriesSection(
    patient: DemoPatient,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    RecordSectionCard(
        title = stringResource(R.string.medical_histories_title),
        family = FieldFamilies.background(),
        modifier = modifier.testTag("medicalHistoriesCard"),
        action = onEdit?.let {
            {
                SectionTextAction(
                    text = stringResource(R.string.action_edit),
                    contentDescription = stringResource(R.string.edit_patient),
                    onClick = it,
                    modifier = Modifier.testTag("editMedicalHistoryButton"),
                )
            }
        },
    ) {
        when (val allergy = classifyAllergy(patient.allergyHistory)) {
            is AllergyState.Recorded -> SectionBanner(
                text = stringResource(R.string.allergy_banner, allergy.text),
                container = scheme.errorContainer,
                accent = scheme.error,
                content = scheme.onErrorContainer,
            )
            AllergyState.ExplicitlyNone -> LabeledValue(
                label = stringResource(R.string.allergy_history),
                value = stringResource(R.string.no_allergy_history),
                modifier = Modifier.fillMaxWidth(),
            )
            AllergyState.NotRecorded -> LabeledValue(
                label = stringResource(R.string.allergy_history),
                value = null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!patient.medicalHistory.isNullOrBlank()) {
            LabeledValue(
                label = stringResource(R.string.medical_history),
                value = patient.medicalHistory,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!patient.surgicalHistory.isNullOrBlank()) {
            LabeledValue(
                label = stringResource(R.string.surgical_history),
                value = patient.surgicalHistory,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!patient.familyHistory.isNullOrBlank()) {
            LabeledValue(
                label = stringResource(R.string.family_history),
                value = patient.familyHistory,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
