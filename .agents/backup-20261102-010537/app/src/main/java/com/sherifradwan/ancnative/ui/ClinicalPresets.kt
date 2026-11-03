package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

val COMMON_MEDICATION_FREQUENCIES = listOf("Once daily", "Twice daily", "Three times daily", "At bedtime", "As needed (PRN)")

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
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "⚡ $title",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items.forEachIndexed { index, item ->
                FilterChip(
                    selected = false,
                    onClick = { onSelect(item) },
                    enabled = enabled,
                    label = { Text(item) },
                    modifier = Modifier.testTag("$tagPrefix-$index"),
                )
            }
        }
    }
}
