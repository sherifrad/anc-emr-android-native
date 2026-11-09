package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.ui.TagHighRiskBg
import com.sherifradwan.ancnative.ui.TagHighRiskDarkBg
import com.sherifradwan.ancnative.ui.TagHighRiskDarkText
import com.sherifradwan.ancnative.ui.TagHighRiskText
import com.sherifradwan.ancnative.ui.TagNormalRiskBg
import com.sherifradwan.ancnative.ui.TagNormalRiskDarkBg
import com.sherifradwan.ancnative.ui.TagNormalRiskDarkText
import com.sherifradwan.ancnative.ui.TagNormalRiskText
import com.sherifradwan.ancnative.ui.isDarkTheme
import com.sherifradwan.ancnative.ui.patienthub.model.ClinicalLabReport
import com.sherifradwan.ancnative.ui.patienthub.model.partitionReportIntoCompositeGroups

@Composable
fun LabReportItem(
    report: ClinicalLabReport,
    patient: DemoPatient,
    canEdit: Boolean,
    onEditTest: (DemoLabInvestigation) -> Unit,
    onDeleteTest: (String) -> Unit,
    onAddTestToReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compositeGroups = remember(report.items) { partitionReportIntoCompositeGroups(report.items) }
    val gaResult = patient.dating?.let { GestationalAgeEngine.calculateGestationalAgeAt(it, report.date) }
    val gaText = gaResult?.displayText?.let { " · GA $it" }.orEmpty()
    val labDisplay = report.labName?.takeIf { it.isNotBlank() } ?: "Laboratory Report"
    val isDark = isDarkTheme()
    val abnormalBg = if (isDark) TagHighRiskDarkBg else TagHighRiskBg
    val abnormalText = if (isDark) TagHighRiskDarkText else TagHighRiskText
    val normalBg = if (isDark) TagNormalRiskDarkBg else TagNormalRiskBg
    val normalText = if (isDark) TagNormalRiskDarkText else TagNormalRiskText

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("labReportCard-${report.date}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Report Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${report.date}$gaText",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "$labDisplay (${report.totalCount} tests)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (report.isAnyAbnormal) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = abnormalBg,
                        ) {
                            Text(
                                text = "${report.abnormalCount} ABNORMAL ▲",
                                color = abnormalText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = normalBg,
                        ) {
                            Text(
                                text = "ALL NORMAL ✓",
                                color = normalText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }

                    if (canEdit) {
                        TextButton(
                            onClick = onAddTestToReport,
                            modifier = Modifier.minimumTouchTarget().testTag("addTestToReportBtn-${report.date}"),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Add Test", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Composite Groups and Items
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                compositeGroups.forEach { group ->
                    if (group.isComposite) {
                        var isExpanded by rememberSaveable(group.title) { mutableStateOf(true) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumTouchTarget()
                                        .clickable(
                                            role = Role.Button,
                                            onClick = { isExpanded = !isExpanded },
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = group.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = "(${group.items.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    if (group.isAnyAbnormal) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = abnormalBg) {
                                            Text(
                                                text = "${group.abnormalCount} Abnormal",
                                                color = abnormalText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    } else {
                                        Surface(shape = RoundedCornerShape(4.dp), color = normalBg) {
                                            Text(
                                                text = "Normal",
                                                color = normalText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }

                                if (isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 24.dp, end = 10.dp, bottom = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        group.items.forEach { lab ->
                                            LabRowItem(
                                                lab = lab,
                                                canEdit = canEdit,
                                                onEdit = { onEditTest(lab) },
                                                onDelete = { onDeleteTest(lab.id) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Standalone group
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (compositeGroups.size > 1) {
                                Text(
                                    text = "INDIVIDUAL INVESTIGATIONS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                )
                            }
                            group.items.forEach { lab ->
                                LabRowItem(
                                    lab = lab,
                                    canEdit = canEdit,
                                    onEdit = { onEditTest(lab) },
                                    onDelete = { onDeleteTest(lab.id) },
                                )
                            }
                        }
                    }
                }
            }

            // Footer action
            if (canEdit) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onAddTestToReport,
                        modifier = Modifier.minimumTouchTarget(),
                    ) {
                        Text(
                            text = "+ Add Another Test to this Report",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LabRowItem(
    lab: DemoLabInvestigation,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isDarkTheme()
    val abnormalBg = if (isDark) TagHighRiskDarkBg else TagHighRiskBg
    val abnormalText = if (isDark) TagHighRiskDarkText else TagHighRiskText

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lab-${lab.id}")
            .clickable(enabled = canEdit, onClick = onEdit)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lab.testName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val refText = lab.referenceRange?.takeIf { it.isNotBlank() }?.let { " · Ref: $it" }.orEmpty()
            Text(
                text = "${lab.testCategory}$refText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val unitStr = lab.unit?.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
            Text(
                text = "${lab.result}$unitStr",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (lab.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )

            if (lab.isAbnormal) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = abnormalBg,
                ) {
                    Text(
                        text = "ABNORMAL ▲",
                        color = abnormalText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }

            if (canEdit) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumTouchTarget().testTag("deleteLab-${lab.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Test",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
