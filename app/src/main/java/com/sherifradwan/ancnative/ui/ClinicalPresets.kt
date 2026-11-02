package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.sherifradwan.ancnative.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

val COMMON_MEDICATION_FREQUENCIES = listOf("Once daily", "Twice daily", "Three times daily", "At bedtime", "As needed (PRN)")

/** Long suggestion lists (my medication list, previous prescriptions) show this many chips until expanded. */
const val PRESAVED_COLLAPSE_AFTER = 5

object ClinicalPresets {
    val commonBloodPressures = listOf("110/70", "120/80", "130/80", "140/90")

    val commonVisitFindings = listOf(
        "Normal progress",
        "Cephalic, FHR positive",
        "No pedal edema",
        "Mild pedal edema",
        "Reduced fetal movement",
    )
}

@Composable
fun QuickPresetChips(
    title: String,
    items: List<String>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    tagPrefix: String,
    /** When set and `items` is longer, only the first `collapseAfter` show until the toggle (tag `toggleTag`) is used. */
    collapseAfter: Int? = null,
    toggleTag: String = "${tagPrefix}Toggle",
) {
    val collapsible = collapseAfter != null && items.size > collapseAfter
    var expanded by rememberSaveable(tagPrefix) { mutableStateOf(false) }
    val visibleCount = if (collapsible && !expanded) collapseAfter!! else items.size
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "⚡ $title",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            // Each chip already carries a 48 dp touch area, so no extra row gap.
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items.forEachIndexed { index, item ->
                if (index >= visibleCount) return@forEachIndexed
                FilterChip(
                    selected = false,
                    onClick = { onSelect(item) },
                    enabled = enabled,
                    label = { Text(item) },
                    modifier = Modifier.testTag("$tagPrefix-$index"),
                )
            }
        }
        if (collapsible) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.minimumTouchTarget().testTag(toggleTag),
            ) {
                Text(
                    text = if (expanded) stringResource(R.string.show_fewer) else stringResource(R.string.show_all_count, items.size),
                )
            }
        }
    }
}
