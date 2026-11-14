package com.sherifradwan.ancnative.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.ui.minimumTouchTarget

@Composable
fun RecordSectionCard(
    title: String,
    family: FieldFamily,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RecordShapes.Large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        CompositionLocalProvider(LocalFieldFamily provides family) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(RecordSpacing.M),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = RecordType.SectionTitle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp)
                            .semantics { heading() },
                    )
                    action?.invoke()
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(RecordSpacing.M),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun SectionTextAction(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .minimumTouchTarget()
            .semantics { this.contentDescription = contentDescription },
        shape = RecordShapes.Small,
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Text(
            text = text,
            style = RecordType.Action,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
