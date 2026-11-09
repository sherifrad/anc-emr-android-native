package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.ui.ClinicalTeal
import com.sherifradwan.ancnative.ui.patienthub.model.BiometryValues

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BiometryGrid(
    values: BiometryValues,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty && !values.hasAnyData) return

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Ultrasound Biometry",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Quantitative Measurements FlowRow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!values.bpd.isNullOrBlank()) {
                    BiometryMetricBadge(label = "BPD", value = "${values.bpd} mm")
                }
                if (!values.hc.isNullOrBlank()) {
                    BiometryMetricBadge(label = "HC", value = "${values.hc} mm")
                }
                if (!values.ac.isNullOrBlank()) {
                    BiometryMetricBadge(label = "AC", value = "${values.ac} mm")
                }
                if (!values.fl.isNullOrBlank()) {
                    BiometryMetricBadge(label = "FL", value = "${values.fl} mm")
                }
                if (!values.efw.isNullOrBlank()) {
                    BiometryMetricBadge(label = "EFW", value = values.efw, isHighlighted = true)
                }
                if (!values.fhr.isNullOrBlank()) {
                    BiometryMetricBadge(label = "FHR", value = "${values.fhr} bpm")
                }
            }

            // Anatomical Details (Placenta & Liquor) wrapped in FlowRow to avoid 360dp overflow
            if (!values.placenta.isNullOrBlank() || !values.amnioticFluid.isNullOrBlank()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (!values.placenta.isNullOrBlank()) {
                        Text(
                            text = "Placenta: ${values.placenta}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (!values.amnioticFluid.isNullOrBlank()) {
                        Text(
                            text = "Amniotic Fluid: ${values.amnioticFluid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BiometryMetricBadge(
    label: String,
    value: String,
    isHighlighted: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
