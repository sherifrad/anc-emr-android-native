package com.sherifradwan.ancnative.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R

/** Title row of a record list tab with its primary record action. */
@Composable
fun RecordListHeader(
    title: String,
    actionLabel: String,
    actionTag: String,
    onAction: () -> Unit,
    showAction: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = RecordType.SectionTitle,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (showAction) {
            Button(
                onClick = onAction,
                shape = RecordShapes.Small,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(actionTag),
            ) {
                Text(actionLabel, style = RecordType.Action)
            }
        }
    }
}

@Composable
fun RecordEmptyCard(modifier: Modifier = Modifier) {
    OutlinedCard(
        shape = RecordShapes.Large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.no_records),
            style = RecordType.FieldValue,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}
