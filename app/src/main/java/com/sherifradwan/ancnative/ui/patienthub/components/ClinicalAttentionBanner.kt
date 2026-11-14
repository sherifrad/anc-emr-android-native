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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.ui.StatusModerateBg
import com.sherifradwan.ancnative.ui.StatusModerateBorder
import com.sherifradwan.ancnative.ui.StatusModerateDarkBg
import com.sherifradwan.ancnative.ui.StatusModerateDarkBorder
import com.sherifradwan.ancnative.ui.StatusModerateDarkText
import com.sherifradwan.ancnative.ui.StatusModerateText
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
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
            container = if (isDark) StatusModerateDarkBg else StatusModerateBg,
            content = if (isDark) StatusModerateDarkText else StatusModerateText,
            accent = if (isDark) StatusModerateDarkText else StatusModerateText,
            border = if (isDark) StatusModerateDarkBorder else StatusModerateBorder,
            borderWidth = 1.dp,
        )
        ClinicalAttentionLevel.NOTE -> AttentionColors(
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = MaterialTheme.colorScheme.onSurface,
            accent = MaterialTheme.colorScheme.onSurfaceVariant,
            border = MaterialTheme.colorScheme.outlineVariant,
            borderWidth = 1.dp,
        )
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
        shape = RecordShapes.Large,
        title = {
            Text(
                text = stringResource(R.string.clinical_attention),
                style = RecordType.SectionTitle,
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
                        style = RecordType.FieldValue,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (canEdit && entries.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.attention_items),
                        style = RecordType.MetricLabel,
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
                                        contentDescription = stringResource(R.string.delete),
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
                                label = { Text(stringResource(R.string.attention_update_item)) },
                                singleLine = false,
                                maxLines = 3,
                                supportingText = {
                                    Text(
                                        text = "${entry.text.length}/$ATTENTION_TEXT_MAX",
                                        style = RecordType.Tag.copy(fontWeight = FontWeight.Normal),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    } else {
                        AttentionEntryBox(
                            level = entry.level,
                            text = entry.text,
                            modifier = Modifier.testTag("attentionEntry-$index"),
                        )
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
                            OutlinedButton(
                                onClick = {
                                    if (newText.isNotBlank() && entries.count { it.text.isNotBlank() } < ATTENTION_ENTRIES_MAX) {
                                        entries.add(AttentionEntry(newLevel, newText.trim()))
                                        newText = ""
                                        newLevel = ClinicalAttentionLevel.ALERT
                                    }
                                },
                                enabled = newText.isNotBlank() && entries.count { it.text.isNotBlank() } < ATTENTION_ENTRIES_MAX,
                                shape = RecordShapes.Small,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("attentionAdd"),
                            ) {
                                Text(
                                    text = stringResource(R.string.attention_add_item),
                                    style = RecordType.Action,
                                )
                            }
                        }
                        OutlinedTextField(
                            value = newText,
                            onValueChange = { updated ->
                                newText = updated.replace("\n", " ").take(ATTENTION_TEXT_MAX)
                            },
                            enabled = entries.count { it.text.isNotBlank() } < ATTENTION_ENTRIES_MAX,
                            label = { Text(stringResource(R.string.attention_add_item)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("attentionNewText"),
                            singleLine = false,
                            maxLines = 3,
                            supportingText = {
                                Text(
                                    text = "${newText.length}/$ATTENTION_TEXT_MAX",
                                    style = RecordType.Tag.copy(fontWeight = FontWeight.Normal),
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
                    shape = RecordShapes.Small,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("attentionSave"),
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        style = RecordType.Action,
                    )
                }
            } else {
                Button(
                    onClick = onDismiss,
                    shape = RecordShapes.Small,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("attentionClose"),
                ) {
                    Text(
                        text = stringResource(R.string.close),
                        style = RecordType.Action,
                    )
                }
            }
        },
        dismissButton = if (canEdit) {
            {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RecordShapes.Small,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("attentionCancel"),
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = RecordType.Action,
                    )
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
    val label = levelWord(selectedLevel)

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RecordShapes.Small,
            color = colors.container,
            border = BorderStroke(colors.borderWidth, colors.border),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .minimumTouchTarget(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = label,
                    style = RecordType.Action.copy(fontWeight = FontWeight.Bold),
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
                Pair(ClinicalAttentionLevel.CRITICAL, "attentionLevelOption-critical"),
                Pair(ClinicalAttentionLevel.ALERT, "attentionLevelOption-alert"),
                Pair(ClinicalAttentionLevel.NOTE, "attentionLevelOption-note"),
            )
            options.forEach { (level, tag) ->
                val optionColors = attentionColors(level)
                val descRes = when (level) {
                    ClinicalAttentionLevel.CRITICAL -> R.string.attention_desc_critical
                    ClinicalAttentionLevel.ALERT -> R.string.attention_desc_alert
                    ClinicalAttentionLevel.NOTE -> R.string.attention_desc_note
                }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = levelWord(level),
                                style = RecordType.FieldValue,
                                color = optionColors.content,
                            )
                            Text(
                                text = stringResource(descRes),
                                style = RecordType.Tag,
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
