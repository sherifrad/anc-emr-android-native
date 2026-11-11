package com.sherifradwan.ancnative.ui.patientdirectory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.ui.patienthub.components.deliveredDaysAgo
import com.sherifradwan.ancnative.ui.patienthub.components.deliveredHeadline
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.ui.GaDiscordanceMarker
import com.sherifradwan.ancnative.model.previousDeliverySummary
import com.sherifradwan.ancnative.ui.PatientDashboardRowProjection
import com.sherifradwan.ancnative.ui.ActionRose

/**
 * Standardized 3-Column Clinical Micro-Grid (~102dp width per column on 360dp).
 * Enforces strict LTR layout direction to prevent column order mirroring on RTL locales.
 */
@Composable
fun PatientDirectoryMetricsRow(
    patient: DemoPatient,
    row: PatientDashboardRowProjection,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Column 1: Gestational Age & EDD
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ga_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                )
                val hasGa = row.gestationalAgeText.isNotBlank() && row.gestationalAgeText != "-"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (row.isDelivered) deliveredHeadline(row.deliveryDate) else if (hasGa) row.gestationalAgeText else stringResource(R.string.pending),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (hasGa && row.isDatingDiscordant) GaDiscordanceMarker(style = MaterialTheme.typography.bodyMedium)
                    if (row.isLateOrPostTerm) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                text = "Post",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(
                    text = if (row.isDelivered) deliveredDaysAgo(row.deliveryDate).orEmpty() else if (!row.eddText.isNullOrBlank()) "Due ${row.eddText}" else stringResource(R.string.edd_pending),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Column 2: Obstetric Parity & History
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Parity",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                )
                val g = patient.gravida ?: 1L
                val p = patient.parity ?: 0L
                val a = patient.abortion ?: 0L
                val parityText = if (a > 0L) "G$g P$p A$a" else "G$g P$p"
                Text(
                    text = parityText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                val prevDelivery = previousDeliverySummary(patient).takeIf { it.isNotBlank() } ?: "Nulliparous"
                Text(
                    text = prevDelivery,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Column 3: Bedside Vitals (Latest BP & Blood Group)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vitals / BP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                )
                Text(
                    text = row.latestVitalsBp ?: "--/--",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (row.isSevereBp) ActionRose else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                val bgText = if (!row.bloodGroup.isNullOrBlank()) "Blood: ${row.bloodGroup}" else "Blood: --"
                Text(
                    text = bgText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
