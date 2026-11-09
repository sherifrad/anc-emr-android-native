package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import java.time.LocalDate
import kotlin.math.abs
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.datingSummaryOf
import com.sherifradwan.ancnative.model.datingTextRows
import com.sherifradwan.ancnative.model.previousDeliverySummary
import com.sherifradwan.ancnative.ui.AllergyState
import com.sherifradwan.ancnative.ui.FactRow
import com.sherifradwan.ancnative.ui.ObstetricParityBadge
import com.sherifradwan.ancnative.ui.classifyAllergy
import com.sherifradwan.ancnative.ui.ClinicalCardTokens
import com.sherifradwan.ancnative.ui.isDarkTheme
import com.sherifradwan.ancnative.ui.minimumTouchTarget

@Composable
private fun SectionIconAction(
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
    tag: String,
) {
    IconButton(onClick = onClick, modifier = Modifier.minimumTouchTarget().testTag(tag)) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun DatingSection(
    patient: DemoPatient,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val dating = patient.dating
    val summary = remember(patient) { datingSummaryOf(patient, LocalDate.now()) }
    ClinicalGroupSection(
        title = stringResource(R.string.pregnancy_dating_title),
        icon = Icons.Default.DateRange,
        iconTint = scheme.onPrimaryContainer,
        iconBgColor = scheme.primaryContainer,
        actionButton = onEdit?.let {
            { SectionIconAction(it, Icons.Default.Edit, stringResource(R.string.edit_dating), "editDating") }
        },
        modifier = modifier.testTag("datingCard"),
    ) {
        if (dating == null) {
            Text(
                text = stringResource(R.string.no_dating_recorded),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            return@ClinicalGroupSection
        }
        datingTextRows(summary).forEach { row ->
            FactRow(row.label, row.value, isEmphasized = row.isEdd)
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
                modifier = Modifier.padding(top = 4.dp).testTag("datingDiscordanceAlert"),
            )
        }
        if (!dating.notes.isNullOrBlank()) FactRow(R.string.notes, dating.notes)
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
        shape = RoundedCornerShape(8.dp),
        color = container,
        modifier = modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = content,
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
    val scheme = MaterialTheme.colorScheme
    ClinicalGroupSection(
        title = stringResource(R.string.previous_pregnancies),
        icon = Icons.Default.Person,
        iconTint = scheme.onPrimaryContainer,
        iconBgColor = scheme.primaryContainer,
        actionButton = onAddPregnancy?.let {
            { SectionIconAction(it, Icons.Default.Add, stringResource(R.string.add_previous_pregnancy), "addPregnancyButton") }
        },
        modifier = modifier.testTag("obstetricParityCard"),
    ) {
        ObstetricParityBadge(patient.gravida, patient.parity, patient.abortion)
        if (patient.previousPregnancies.isEmpty()) {
            Text(
                text = previousDeliverySummary(patient),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        } else {
            patient.previousPregnancies.forEach { pregnancy ->
                PastDeliveryMicroRow(
                    pregnancy = pregnancy,
                    onEdit = onEditPregnancy?.let { edit -> { edit(pregnancy.id) } },
                    onDelete = onDeletePregnancy?.let { del -> { del(pregnancy.id) } },
                )
            }
        }
        if (!patient.obstetricNotes.isNullOrBlank()) FactRow(R.string.obstetric_notes, patient.obstetricNotes)
    }
}

@Composable
fun MedicalHistoriesSection(
    patient: DemoPatient,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    ClinicalGroupSection(
        title = stringResource(R.string.medical_histories_title),
        icon = Icons.Default.Healing,
        iconTint = scheme.onPrimaryContainer,
        iconBgColor = scheme.primaryContainer,
        actionButton = onEdit?.let {
            { SectionIconAction(it, Icons.Default.Edit, stringResource(R.string.edit_patient), "editMedicalHistoryButton") }
        },
        modifier = modifier.testTag("medicalHistoriesCard"),
    ) {
        when (val allergy = classifyAllergy(patient.allergyHistory)) {
            is AllergyState.Recorded -> SectionBanner(
                text = stringResource(R.string.allergy_banner, allergy.text),
                container = scheme.errorContainer,
                accent = scheme.error,
                content = scheme.onErrorContainer,
            )
            AllergyState.ExplicitlyNone -> Text(
                text = stringResource(R.string.no_allergy_history),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            AllergyState.NotRecorded -> Text(
                text = stringResource(R.string.not_recorded),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (!patient.medicalHistory.isNullOrBlank()) FactRow(R.string.medical_history, patient.medicalHistory)
            if (!patient.surgicalHistory.isNullOrBlank()) FactRow(R.string.surgical_history, patient.surgicalHistory)
            if (!patient.familyHistory.isNullOrBlank()) FactRow(R.string.family_history, patient.familyHistory)
        }
    }
}
