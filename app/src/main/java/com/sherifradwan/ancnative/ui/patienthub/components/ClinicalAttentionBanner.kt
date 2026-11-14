package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.SevereBpAttention
import com.sherifradwan.ancnative.ui.ClinicalCardTokens
import com.sherifradwan.ancnative.ui.isDarkTheme
import com.sherifradwan.ancnative.ui.minimumTouchTarget

enum class ClinicalAttentionLevel {
    CRITICAL,
    ALERT,
    NOTE;

    companion object {
        fun parse(raw: String): Pair<ClinicalAttentionLevel, String> {
            val trimmed = raw.trim()
            val prefixes = listOf(
                "[CRITICAL ALERT]" to CRITICAL,
                "CRITICAL ALERT:" to CRITICAL,
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

data class AttentionColors(
    val container: Color,
    val content: Color,
    val accent: Color,
    val border: Color,
    val borderWidth: Dp = 1.dp,
) {
    val containerColor: Color get() = container
    val textColor: Color get() = content
    val iconColor: Color get() = accent
    val borderColor: Color get() = border
}

typealias ClinicalAttentionColors = AttentionColors

@Composable
fun clinicalAttentionColors(level: ClinicalAttentionLevel): AttentionColors = attentionColors(level)

@Composable
fun attentionColors(level: ClinicalAttentionLevel): AttentionColors {
    val isDark = isDarkTheme()
    return when (level) {
        ClinicalAttentionLevel.CRITICAL -> AttentionColors(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
            accent = MaterialTheme.colorScheme.error,
            border = MaterialTheme.colorScheme.error,
            borderWidth = 1.5.dp,
        )
        ClinicalAttentionLevel.ALERT -> AttentionColors(
            container = if (isDark) ClinicalCardTokens.WarningDarkBg else ClinicalCardTokens.WarningBg,
            content = if (isDark) ClinicalCardTokens.WarningDarkText else ClinicalCardTokens.WarningText,
            accent = if (isDark) ClinicalCardTokens.WarningDarkAccent else ClinicalCardTokens.WarningAccent,
            border = if (isDark) ClinicalCardTokens.WarningDarkBorder else ClinicalCardTokens.WarningBorder,
            borderWidth = 1.dp,
        )
        ClinicalAttentionLevel.NOTE -> AttentionColors(
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = MaterialTheme.colorScheme.onSurface,
            accent = MaterialTheme.colorScheme.onSurfaceVariant,
            border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            borderWidth = 1.dp,
        )
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
    val narrative = if (hasStored) {
        ClinicalAttentionLevel.parse(attention!!).second
    } else {
        ""
    }
    val level = clinicalAttentionLevel(attention, severeBp)
    val colors = attentionColors(level)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("clinicalAttentionBanner"),
        colors = CardDefaults.cardColors(containerColor = colors.container),
        border = BorderStroke(colors.borderWidth, colors.border),
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
                        val levelLabel = when (level) {
                            ClinicalAttentionLevel.CRITICAL -> stringResource(R.string.attention_level_critical)
                            ClinicalAttentionLevel.ALERT -> stringResource(R.string.attention_level_alert)
                            ClinicalAttentionLevel.NOTE -> stringResource(R.string.attention_level_note)
                        }
                        Icon(
                            imageVector = if (level == ClinicalAttentionLevel.NOTE) Icons.Default.Info else Icons.Default.Warning,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = levelLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                        )
                        Text(
                            text = stringResource(R.string.clinical_attention),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumTouchTarget(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_clinical_alerts),
                            tint = colors.accent,
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
                        color = colors.content,
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
    canEdit: Boolean = true,
) {
    val entries = remember(currentAttention) {
        parseAttentionEntries(currentAttention).toMutableStateList()
    }
    var newLevel by remember { mutableStateOf(ClinicalAttentionLevel.ALERT) }
    var newText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.clinical_attention),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (entries.isEmpty() && !canEdit) {
                    Text(
                        text = stringResource(R.string.no_clinical_attention_recorded),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                entries.forEachIndexed { index, entry ->
                    if (canEdit) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AttentionLevelDropdown(
                                    selectedLevel = entry.level,
                                    onLevelSelected = { newLvl ->
                                        entries[index] = entry.copy(level = newLvl)
                                    },
                                    modifier = Modifier.testTag("attentionLevel-$index"),
                                )
                                IconButton(
                                    onClick = { entries.removeAt(index) },
                                    modifier = Modifier
                                        .minimumTouchTarget()
                                        .testTag("attentionDelete-$index"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete_attention_entry),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = entry.text,
                                onValueChange = { updated ->
                                    val clean = updated.replace("\n", " ").take(ATTENTION_TEXT_MAX)
                                    entries[index] = entry.copy(text = clean)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("attentionText-$index"),
                                singleLine = false,
                                maxLines = 3,
                                supportingText = {
                                    Text(
                                        text = "${entry.text.length}/$ATTENTION_TEXT_MAX",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    } else {
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

                if (canEdit) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AttentionLevelDropdown(
                                selectedLevel = newLevel,
                                onLevelSelected = { newLevel = it },
                                modifier = Modifier.testTag("attentionNewLevel"),
                            )
                            Button(
                                onClick = {
                                    if (newText.isNotBlank() && entries.count { it.text.isNotBlank() } < ATTENTION_ENTRIES_MAX) {
                                        entries.add(AttentionEntry(newLevel, newText.trim()))
                                        newText = ""
                                        newLevel = ClinicalAttentionLevel.ALERT
                                    }
                                },
                                enabled = newText.isNotBlank() && entries.count { it.text.isNotBlank() } < ATTENTION_ENTRIES_MAX,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("attentionAdd"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                ),
                            ) {
                                Text(stringResource(R.string.attention_add_button))
                            }
                        }
                        OutlinedTextField(
                            value = newText,
                            onValueChange = { updated ->
                                newText = updated.replace("\n", " ").take(ATTENTION_TEXT_MAX)
                            },
                            enabled = entries.count { it.text.isNotBlank() } < ATTENTION_ENTRIES_MAX,
                            label = { Text(stringResource(R.string.attention_new_label)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("attentionNewText"),
                            singleLine = false,
                            maxLines = 3,
                            supportingText = {
                                Text(
                                    text = "${newText.length}/$ATTENTION_TEXT_MAX",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (canEdit) {
                Button(
                    onClick = {
                        val all = entries.filter { it.text.isNotBlank() }.toMutableList()
                        if (newText.isNotBlank() && all.size < ATTENTION_ENTRIES_MAX) {
                            all.add(AttentionEntry(newLevel, newText.trim()))
                        }
                        onSave(composeAttentionEntries(all))
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("attentionSave"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(stringResource(R.string.save))
                }
            } else {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("attentionClose"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        },
        dismissButton = if (canEdit) {
            {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("attentionCancel"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        } else null,
    )
}

@Composable
private fun AttentionLevelDropdown(
    selectedLevel: ClinicalAttentionLevel,
    onLevelSelected: (ClinicalAttentionLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = attentionColors(selectedLevel)
    val label = when (selectedLevel) {
        ClinicalAttentionLevel.CRITICAL -> stringResource(R.string.attention_level_critical)
        ClinicalAttentionLevel.ALERT -> stringResource(R.string.attention_level_alert)
        ClinicalAttentionLevel.NOTE -> stringResource(R.string.attention_level_note)
    }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = colors.container,
            border = BorderStroke(colors.borderWidth, colors.border),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .minimumTouchTarget(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = if (selectedLevel == ClinicalAttentionLevel.NOTE) Icons.Default.Info else Icons.Default.Warning,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.content,
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.content,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            val options = listOf(
                Triple(ClinicalAttentionLevel.CRITICAL, "attentionLevelOption-critical", R.string.attention_level_critical),
                Triple(ClinicalAttentionLevel.ALERT, "attentionLevelOption-alert", R.string.attention_level_alert),
                Triple(ClinicalAttentionLevel.NOTE, "attentionLevelOption-note", R.string.attention_level_note),
            )
            options.forEach { (level, tag, textRes) ->
                val optionColors = attentionColors(level)
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (level == ClinicalAttentionLevel.NOTE) Icons.Default.Info else Icons.Default.Warning,
                                contentDescription = null,
                                tint = optionColors.accent,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(textRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = optionColors.content,
                            )
                        }
                    },
                    onClick = {
                        onLevelSelected(level)
                        expanded = false
                    },
                    modifier = Modifier.testTag(tag),
                )
            }
        }
    }
}
