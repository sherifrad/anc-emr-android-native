package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.SevereBpAttention
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.SectionTextAction

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

    RecordSectionCard(
        title = stringResource(R.string.clinical_attention),
        family = FieldFamilies.patient(),
        modifier = modifier.testTag("clinicalAttentionCard"),
        action = onEdit?.let {
            {
                SectionTextAction(
                    text = stringResource(R.string.action_edit),
                    contentDescription = stringResource(R.string.edit_clinical_alerts),
                    onClick = it,
                    modifier = Modifier.testTag("editClinicalAttention"),
                )
            }
        },
    ) {
        if (isEmpty) {
            Text(
                text = stringResource(R.string.no_clinical_attention_recorded),
                style = RecordType.FieldValue,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onEdit != null) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RecordShapes.Small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("addClinicalAttention"),
                ) {
                    Text(
                        text = stringResource(R.string.add_clinical_attention),
                        style = RecordType.Action,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (severeBp != null) {
                    AttentionEntryBox(
                        level = ClinicalAttentionLevel.CRITICAL,
                        text = stringResource(R.string.bp_attention_severe, severeBp.bloodPressure, severeBp.date),
                        modifier = Modifier.testTag("severeBpAttention"),
                    )
                }
                val sorted = remember(entries) { sortedForDisplay(entries) }
                sorted.forEachIndexed { index, entry ->
                    AttentionEntryBox(
                        level = entry.level,
                        text = entry.text,
                        modifier = Modifier.testTag("attentionEntry-$index"),
                    )
                }
            }
        }
    }
}
