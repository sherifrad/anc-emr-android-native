package com.sherifradwan.ancnative.ui.patientdirectory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.ui.ActionRose
import com.sherifradwan.ancnative.ui.GaDiscordanceMarker
import com.sherifradwan.ancnative.ui.PatientDashboardRowProjection
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.patienthub.components.deliveredDaysAgo
import com.sherifradwan.ancnative.ui.patienthub.components.deliveredHeadline

/**
 * Standardized High-Density Clinical Metrics Grid for Patient Directory Card.
 * Mirrors with layout direction (RTL-aware).
 */
@Composable
fun PatientDirectoryMetricsRow(
    patient: DemoPatient,
    row: PatientDashboardRowProjection,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (row.isDelivered) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                DeliveryCell(row = row, modifier = Modifier.weight(2f))
                ParityCell(patient = patient, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                BloodPressureCell(row = row, modifier = Modifier.weight(1f))
                BloodGroupCell(row = row, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.weight(1f))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                GestationalAgeCell(row = row, modifier = Modifier.weight(1f))
                ParityCell(patient = patient, modifier = Modifier.weight(1f))
                BloodPressureCell(row = row, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                BloodGroupCell(row = row, modifier = Modifier.weight(1f))
                DueDateCell(row = row, modifier = Modifier.weight(2f))
            }
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = RecordType.MetricLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        content()
    }
}

@Composable
private fun MetricValue(
    value: String,
    modifier: Modifier = Modifier,
    isMissing: Boolean = false,
    color: Color = if (isMissing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = value,
        style = if (isMissing) RecordType.Missing else RecordType.MetricValue,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun GestationalAgeCell(
    row: PatientDashboardRowProjection,
    modifier: Modifier = Modifier,
) {
    MetricCell(
        label = stringResource(R.string.ga_label),
        modifier = modifier,
    ) {
        val hasGa = row.gestationalAgeText.isNotBlank() && row.gestationalAgeText != "-"
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasGa) {
                MetricValue(
                    value = ltrIsolate(row.gestationalAgeText),
                )
            } else {
                MetricValue(
                    value = stringResource(R.string.pending),
                    isMissing = true,
                )
            }

            if (hasGa && row.isDatingDiscordant) {
                GaDiscordanceMarker(style = RecordType.MetricValue)
            }

            if (row.isLateOrPostTerm) {
                Surface(
                    shape = RecordShapes.Small,
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = "Post",
                        style = RecordType.Tag,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ParityCell(
    patient: DemoPatient,
    modifier: Modifier = Modifier,
) {
    MetricCell(
        label = stringResource(R.string.parity),
        modifier = modifier,
    ) {
        val g = patient.gravida ?: 1L
        val p = patient.parity ?: 0L
        val a = patient.abortion ?: 0L
        val parityText = if (a > 0L) "G$g P$p A$a" else "G$g P$p"
        MetricValue(
            value = ltrIsolate(parityText),
        )
    }
}

@Composable
private fun BloodPressureCell(
    row: PatientDashboardRowProjection,
    modifier: Modifier = Modifier,
) {
    MetricCell(
        label = stringResource(R.string.field_label_bp),
        modifier = modifier,
    ) {
        if (!row.latestVitalsBp.isNullOrBlank()) {
            MetricValue(
                value = ltrIsolate(row.latestVitalsBp),
                color = if (row.isSevereBp) ActionRose else MaterialTheme.colorScheme.onSurface,
            )
        } else {
            MetricValue(
                value = "--/--",
                isMissing = true,
            )
        }
    }
}

@Composable
private fun BloodGroupCell(
    row: PatientDashboardRowProjection,
    modifier: Modifier = Modifier,
) {
    MetricCell(
        label = stringResource(R.string.blood_group),
        modifier = modifier,
    ) {
        if (!row.bloodGroup.isNullOrBlank()) {
            MetricValue(
                value = ltrIsolate(row.bloodGroup),
            )
        } else {
            MetricValue(
                value = "--",
                isMissing = true,
            )
        }
    }
}

@Composable
private fun DueDateCell(
    row: PatientDashboardRowProjection,
    modifier: Modifier = Modifier,
) {
    MetricCell(
        label = stringResource(R.string.field_label_due_date),
        modifier = modifier,
    ) {
        if (!row.eddText.isNullOrBlank()) {
            MetricValue(
                value = ltrIsolate(row.eddText),
            )
        } else {
            MetricValue(
                value = stringResource(R.string.pending),
                isMissing = true,
            )
        }
    }
}

@Composable
private fun DeliveryCell(
    row: PatientDashboardRowProjection,
    modifier: Modifier = Modifier,
) {
    MetricCell(
        label = stringResource(R.string.delivery_outcome_section),
        modifier = modifier,
    ) {
        MetricValue(
            value = deliveredHeadline(row.deliveryDate),
        )
        val daysAgo = deliveredDaysAgo(row.deliveryDate)
        if (daysAgo != null) {
            Text(
                text = daysAgo,
                style = RecordType.MetricLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
