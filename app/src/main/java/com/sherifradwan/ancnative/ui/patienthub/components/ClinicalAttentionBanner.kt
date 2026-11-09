package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.SevereBpAttention
import com.sherifradwan.ancnative.ui.ActionRose
import com.sherifradwan.ancnative.ui.ClinicalCardTokens
import com.sherifradwan.ancnative.ui.TagAttentionDarkText
import com.sherifradwan.ancnative.ui.TagAttentionText
import com.sherifradwan.ancnative.ui.TagBloodNegativeBg
import com.sherifradwan.ancnative.ui.TagBloodNegativeDarkBg
import com.sherifradwan.ancnative.ui.TagBloodNegativeDarkBorder
import com.sherifradwan.ancnative.ui.TagBloodNegativeDarkText
import com.sherifradwan.ancnative.ui.TagBloodNegativeBorder
import com.sherifradwan.ancnative.ui.TagBloodNegativeText
import com.sherifradwan.ancnative.ui.minimumTouchTarget

enum class ClinicalAttentionLevel {
    CRITICAL,
    ALERT,
    NOTE;

    companion object {
        fun parse(raw: String): Pair<ClinicalAttentionLevel, String> {
            val trimmed = raw.trim()
            val prefixes = listOf(
                "CRITICAL:" to CRITICAL,
                "[CRITICAL]" to CRITICAL,
                "ALERT:" to ALERT,
                "[ALERT]" to ALERT,
                "WARNING:" to ALERT,
                "[WARNING]" to ALERT,
                "NOTE:" to NOTE,
                "[NOTE]" to NOTE,
            )
            for ((prefix, level) in prefixes) {
                if (trimmed.startsWith(prefix, ignoreCase = true)) {
                    val narrative = trimmed.substring(prefix.length).trim()
                    return level to narrative.ifEmpty { trimmed }
                }
            }
            return NOTE to trimmed
        }
    }
}

@Composable
fun ClinicalAttentionBanner(
    attention: String?,
    modifier: Modifier = Modifier,
    severeBp: SevereBpAttention? = null,
    onEdit: () -> Unit,
) {
    val hasStored = !attention.isNullOrBlank()
    val (parsedLevel, narrative) = if (hasStored) {
        ClinicalAttentionLevel.parse(attention!!)
    } else {
        ClinicalAttentionLevel.NOTE to ""
    }
    val level = if (severeBp != null) ClinicalAttentionLevel.CRITICAL else parsedLevel
    val isDark = isSystemInDarkTheme()

    val (containerColor, borderColor, accentBarColor, textColor) = when (level) {
        ClinicalAttentionLevel.CRITICAL -> {
            val bg = if (isDark) ClinicalCardTokens.CriticalDarkBg else ClinicalCardTokens.CriticalBg
            val b = if (isDark) ClinicalCardTokens.CriticalDarkBorder else ClinicalCardTokens.CriticalBorder
            val t = if (isDark) TagBloodNegativeDarkText else TagBloodNegativeText
            listOf(bg, b, ActionRose, t)
        }
        ClinicalAttentionLevel.ALERT -> {
            val bg = if (isDark) ClinicalCardTokens.WarningDarkBg else ClinicalCardTokens.WarningBg
            val b = if (isDark) ClinicalCardTokens.WarningDarkBorder else ClinicalCardTokens.WarningBorder
            val t = if (isDark) TagAttentionDarkText else TagAttentionText
            listOf(bg, b, ClinicalCardTokens.WarningText, t)
        }
        ClinicalAttentionLevel.NOTE -> {
            val bg = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow
            val b = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            val t = MaterialTheme.colorScheme.onSurface
            listOf(bg, b, MaterialTheme.colorScheme.primary, t)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("clinicalAttentionBanner"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (level == ClinicalAttentionLevel.CRITICAL) 1.5.dp else 1.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = accentBarColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.clinical_attention).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentBarColor,
                            letterSpacing = 0.8.sp,
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumTouchTarget(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_clinical_alerts),
                            tint = accentBarColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                if (severeBp != null) {
                    Text(
                        text = stringResource(R.string.bp_attention_severe, severeBp.bloodPressure, severeBp.date),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("severeBpAttention"),
                    )
                }

                if (hasStored && narrative.isNotBlank()) {
                    Text(
                        text = narrative,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@Composable
fun ClinicalAttentionDialog(
    currentAttention: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val (parsedLevel, parsedNarrative) = ClinicalAttentionLevel.parse(currentAttention)
    var selectedLevel by remember { mutableStateOf(parsedLevel) }
    var text by remember { mutableStateOf(parsedNarrative) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Clinical Attention & Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Select alert severity, and enter clinical notes (one per line):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val levels = listOf(
                        Triple(ClinicalAttentionLevel.CRITICAL, "CRITICAL", MaterialTheme.colorScheme.error),
                        Triple(ClinicalAttentionLevel.ALERT, "ALERT", MaterialTheme.colorScheme.tertiary),
                        Triple(ClinicalAttentionLevel.NOTE, "NOTE", MaterialTheme.colorScheme.primary),
                    )
                    levels.forEach { (lvl, title, color) ->
                        val isSelected = selectedLevel == lvl
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) color else color.copy(alpha = 0.08f),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) color else color.copy(alpha = 0.35f),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedLevel = lvl },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.surface else color,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Alerts narrative (one item per line)") },
                    placeholder = { Text("Example:\n• Mild anemia at 34w (Hb 10.6 g/dL)\n• Non-compliant with Calcium") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prefix = when (selectedLevel) {
                        ClinicalAttentionLevel.CRITICAL -> "[CRITICAL] "
                        ClinicalAttentionLevel.ALERT -> "[ALERT] "
                        ClinicalAttentionLevel.NOTE -> "[NOTE] "
                    }
                    val fullText = if (text.isBlank()) "" else "$prefix${text.trim()}"
                    onSave(fullText)
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
