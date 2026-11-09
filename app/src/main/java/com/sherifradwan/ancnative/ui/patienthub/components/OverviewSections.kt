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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.previousDeliverySummary
import com.sherifradwan.ancnative.ui.AllergyState
import com.sherifradwan.ancnative.ui.FactRow
import com.sherifradwan.ancnative.ui.ObstetricParityBadge
import com.sherifradwan.ancnative.ui.classifyAllergy
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
    dating: DemoPatientDating?,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
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
        val agreed = dating.agreedEdd?.trim().orEmpty()
        val us = dating.ultrasoundEdd?.trim().orEmpty()
        val eddSource: Pair<Int, String?> = when {
            agreed.isNotEmpty() -> R.string.agreed_edd to agreed
            us.isNotEmpty() -> R.string.ultrasound_edd to us
            else -> R.string.calculated_edd to dating.calculateEstimatedDueDate()?.toString()
        }
        FactRow(eddSource.first, eddSource.second ?: stringResource(R.string.not_recorded), isEmphasized = true)
        if (agreed.isNotEmpty() && us.isNotEmpty() && !agreed.equals(us, ignoreCase = true)) FactRow(R.string.ultrasound_edd, us)
        FactRow(R.string.dating_method, dating.datingMethod)
        FactRow(R.string.lmp_date, dating.lmpDate)
        if (!dating.transferDate.isNullOrBlank()) FactRow(R.string.ivf_transfer_date, dating.transferDate)
        dating.embryoAgeDays?.let { FactRow(R.string.embryo_age, stringResource(R.string.embryo_age_days, it)) }
        if (!dating.notes.isNullOrBlank()) FactRow(R.string.notes, dating.notes)
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
            is AllergyState.Recorded -> Surface(
                shape = RoundedCornerShape(8.dp),
                color = scheme.errorContainer,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = scheme.error, modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.allergy_banner, allergy.text),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onErrorContainer,
                    )
                }
            }
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
