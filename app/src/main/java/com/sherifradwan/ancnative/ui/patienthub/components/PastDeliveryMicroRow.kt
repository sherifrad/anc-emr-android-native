package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.DeleteIconButton
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import com.sherifradwan.ancnative.ui.patienthub.model.OutcomeTone
import com.sherifradwan.ancnative.ui.patienthub.model.RowExtraKind
import com.sherifradwan.ancnative.ui.patienthub.model.pastDeliveryRow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PastDeliveryMicroRow(
    pregnancy: DemoPreviousPregnancy,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val row = pastDeliveryRow(pregnancy)
    val scheme = MaterialTheme.colorScheme
    val (chipBg, chipFg) = when (row.outcomeTone) {
        OutcomeTone.Severe -> scheme.errorContainer to scheme.onErrorContainer
        OutcomeTone.Caution -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        OutcomeTone.Neutral -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
    }
    val yearText = row.year ?: row.dateTextFallback ?: stringResource(R.string.date_not_recorded)
    val extras = row.extras.map { (kind, value) ->
        val label = stringResource(
            when (kind) {
                RowExtraKind.PregnancyType -> R.string.pregnancy_type
                RowExtraKind.LossManagement -> R.string.loss_management
                RowExtraKind.TerminationMethod -> R.string.termination_method
            },
        )
        stringResource(R.string.labeled_value, label, value)
    }
    val separator = stringResource(R.string.detail_separator)
    val line2 = listOfNotNull(row.placeAndDoctor, row.notes).joinToString(separator)
    val line3 = (extras + listOfNotNull(row.outcomeNote)).joinToString(separator)
    val editLabel = stringResource(R.string.correct_previous_pregnancy)
    val outcomeText = row.outcomeLabelRes?.let { stringResource(it) } ?: row.outcomeLabel

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("pregnancy-${pregnancy.id}"),
        verticalAlignment = Alignment.Top,
    ) {
        val editClick = if (onEdit != null) {
            Modifier.clickable(onClickLabel = editLabel, onClick = onEdit).minimumTouchTarget().testTag("editPregnancy-${pregnancy.id}")
        } else {
            Modifier
        }
        Column(
            modifier = Modifier.weight(1f).then(editClick).padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = yearText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                outcomeText?.let {
                    Surface(shape = RoundedCornerShape(6.dp), color = chipBg) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = chipFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                row.mode?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                }
            }
            if (line2.isNotEmpty()) {
                Text(
                    text = line2,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (line3.isNotEmpty()) {
                Text(
                    text = line3,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.testTag("pregnancyExtras-${pregnancy.id}"),
                )
            }
        }
        if (onDelete != null) {
            DeleteIconButton(DeletableType.PREVIOUS_PREGNANCY, pregnancy, onDelete)
        }
    }
}
