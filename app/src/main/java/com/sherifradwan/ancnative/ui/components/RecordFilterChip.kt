package com.sherifradwan.ancnative.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.sherifradwan.ancnative.ui.minimumTouchTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    wrap: Boolean = false,
    minTouchTarget: Boolean = false,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = {
            Text(
                text = label,
                style = RecordType.Action.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                maxLines = if (wrap) Int.MAX_VALUE else 1,
                softWrap = wrap,
            )
        },
        shape = RecordShapes.Small,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier
            .then(if (minTouchTarget) Modifier.minimumTouchTarget() else Modifier)
            .testTag(tag),
    )
}
