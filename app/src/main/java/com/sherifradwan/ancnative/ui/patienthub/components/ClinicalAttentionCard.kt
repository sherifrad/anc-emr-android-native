package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.SevereBpAttention
import com.sherifradwan.ancnative.ui.minimumTouchTarget

@Composable
fun ClinicalAttentionCard(
    attention: String?,
    severeBp: SevereBpAttention?,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val entries = remember(attention) { parseAttentionEntries(attention) }
    val isEmpty = entries.isEmpty() && severeBp == null
    if (isEmpty && onEdit == null) {
        return
    }

    val overallLevel = clinicalAttentionLevel(attention, severeBp)
    val overallColors = attentionColors(overallLevel)

    ClinicalGroupSection(
        title = "Clinical attention",
        icon = if (overallLevel == ClinicalAttentionLevel.NOTE) Icons.Default.Info else Icons.Default.Warning,
        iconTint = overallColors.accent,
        iconBgColor = overallColors.container,
        actionButton = onEdit?.let {
            {
                IconButton(
                    onClick = it,
                    modifier = Modifier
                        .minimumTouchTarget()
                        .testTag("editClinicalAttention"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_clinical_alerts),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        modifier = modifier.testTag("clinicalAttentionCard"),
    ) {
        if (isEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.no_clinical_attention_recorded),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onEdit != null) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("addClinicalAttention"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text(stringResource(R.string.add_clinical_attention))
                    }
                }
            }
            return@ClinicalGroupSection
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (severeBp != null) {
                val bpColors = attentionColors(ClinicalAttentionLevel.CRITICAL)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bpColors.container,
                    border = BorderStroke(bpColors.borderWidth, bpColors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("severeBpAttention"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = bpColors.accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.attention_level_critical),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = bpColors.accent,
                        )
                        Text(
                            text = stringResource(R.string.bp_attention_severe, severeBp.bloodPressure, severeBp.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = bpColors.content,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            val sorted = remember(entries) { sortedForDisplay(entries) }
            sorted.forEachIndexed { index, entry ->
                val colors = attentionColors(entry.level)
                val levelLabel = when (entry.level) {
                    ClinicalAttentionLevel.CRITICAL -> stringResource(R.string.attention_level_critical)
                    ClinicalAttentionLevel.ALERT -> stringResource(R.string.attention_level_alert)
                    ClinicalAttentionLevel.NOTE -> stringResource(R.string.attention_level_note)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.container,
                    border = BorderStroke(colors.borderWidth, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attentionEntry-$index"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = if (entry.level == ClinicalAttentionLevel.NOTE) Icons.Default.Info else Icons.Default.Warning,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = levelLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                        )
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.content,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
