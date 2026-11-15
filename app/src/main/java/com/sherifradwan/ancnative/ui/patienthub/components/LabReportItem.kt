package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.GestationalAgeEngine
import com.sherifradwan.ancnative.ui.components.BandChip
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.RecordBandCard
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.SectionTextAction
import com.sherifradwan.ancnative.ui.components.StatusTone
import com.sherifradwan.ancnative.ui.components.StatusWord
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.ui.patienthub.model.ClinicalLabReport
import com.sherifradwan.ancnative.ui.patienthub.model.CompositeTestGroup
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
    val gaShort = GestationalAgeEngine.calculateGestationalAgeAt(patient.dating, report.date)?.shortWeeksText
    val subtitle = listOfNotNull(
        report.labName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.lab_report_default_name),
        pluralStringResource(R.plurals.lab_tests_count, report.totalCount, report.totalCount),
        gaShort?.let(::ltrIsolate),
    ).joinToString(stringResource(R.string.lab_subtitle_separator))

    RecordBandCard(
        title = report.date,
        family = FieldFamilies.background(),
        subtitle = subtitle,
        modifier = modifier.testTag("labReportCard-${report.date}"),
        chips = {
            if (report.isAnyAbnormal) {
                BandChip(
                    pluralStringResource(R.plurals.lab_abnormal_count, report.abnormalCount, report.abnormalCount),
                    StatusTone.High,
                )
            } else {
                BandChip(stringResource(R.string.lab_all_normal), StatusTone.Neutral)
            }
        },
        actions = if (canEdit) {
            {
                SectionTextAction(
                    text = stringResource(R.string.add_test_action),
                    contentDescription = stringResource(R.string.add_test_to_report),
                    onClick = onAddTestToReport,
                    modifier = Modifier.testTag("addTestToReportBtn-${report.date}"),
                )
            }
        } else {
            null
        },
    ) {
        compositeGroups.forEach { group ->
            key(group.title) {
                if (group.isComposite) {
                    PanelGroup(group, canEdit, onEditTest, onDeleteTest)
                } else {
                    IndividualGroup(group, showHeading = compositeGroups.size > 1, canEdit, onEditTest, onDeleteTest)
                }
            }
        }
    }
}

@Composable
private fun PanelGroup(
    group: CompositeTestGroup,
    canEdit: Boolean,
    onEditTest: (DemoLabInvestigation) -> Unit,
    onDeleteTest: (String) -> Unit,
) {
    var isExpanded by rememberSaveable(group.title) { mutableStateOf(true) }
    val stateText = stringResource(if (isExpanded) R.string.state_expanded else R.string.state_collapsed)

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .minimumTouchTarget()
                .clickable(role = Role.Button) { isExpanded = !isExpanded }
                .semantics { stateDescription = stateText },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.lab_panel_title, group.title, group.items.size),
                style = RecordType.Action.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (group.isAnyAbnormal) {
                StatusWord(
                    pluralStringResource(R.plurals.lab_abnormal_count, group.abnormalCount, group.abnormalCount),
                    StatusTone.High,
                )
            } else {
                StatusWord(stringResource(R.string.lab_group_normal), StatusTone.Neutral)
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        if (isExpanded) {
            group.items.forEachIndexed { index, lab ->
                LabRowItem(
                    lab = lab,
                    canEdit = canEdit,
                    onEdit = { onEditTest(lab) },
                    onDelete = { onDeleteTest(lab.id) },
                    showDivider = index > 0,
                )
            }
        }
    }
}

@Composable
private fun IndividualGroup(
    group: CompositeTestGroup,
    showHeading: Boolean,
    canEdit: Boolean,
    onEditTest: (DemoLabInvestigation) -> Unit,
    onDeleteTest: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (showHeading) {
            Text(
                text = stringResource(R.string.lab_individual_investigations),
                style = RecordType.Action.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        group.items.forEachIndexed { index, lab ->
            LabRowItem(
                lab = lab,
                canEdit = canEdit,
                onEdit = { onEditTest(lab) },
                onDelete = { onDeleteTest(lab.id) },
                showDivider = index > 0,
            )
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
    showDivider: Boolean = true,
) {
    val refLine = lab.referenceRange?.takeIf { it.isNotBlank() }
        ?.let { stringResource(R.string.lab_ref_line, lab.testCategory, it) }
        ?: lab.testCategory
    val resultText = if (lab.result.isBlank()) {
        stringResource(R.string.not_recorded)
    } else {
        lab.result + (lab.unit?.takeIf { it.isNotBlank() }?.let { " $it" } ?: "")
    }

    Column(modifier.fillMaxWidth()) {
        if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("lab-${lab.id}")
                .clickable(enabled = canEdit, onClick = onEdit)
                .heightIn(min = 48.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = lab.testName,
                    style = RecordType.FieldValue.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = refLine,
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f, fill = false), horizontalAlignment = Alignment.End) {
                Text(
                    text = ltrIsolate(resultText),
                    textAlign = TextAlign.End,
                    style = RecordType.MetricValue.copy(fontWeight = FontWeight.Bold),
                    color = if (lab.isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                if (lab.isAbnormal) StatusWord(stringResource(R.string.lab_abnormal), StatusTone.High)
            }
            if (canEdit) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumTouchTarget().testTag("deleteLab-${lab.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_test, lab.testName),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
