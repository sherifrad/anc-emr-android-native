package com.sherifradwan.ancnative.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.ui.minimumTouchTarget

@Composable
fun LabeledValue(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
    stretch: Boolean = false,
    family: FieldFamily = currentFieldFamily(),
    missingText: String = stringResource(R.string.not_recorded),
    boxTag: String? = null,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(RecordSpacing.XS),
    ) {
        Text(
            text = label,
            style = RecordType.FieldLabel,
            color = family.label,
        )
        Surface(
            shape = RecordShapes.Small,
            color = family.container,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (stretch) Modifier.weight(1f, fill = true) else Modifier)
                .heightIn(min = 44.dp)
                .then(if (boxTag != null) Modifier.testTag(boxTag) else Modifier),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isNullOrBlank()) {
                    Text(
                        text = missingText,
                        style = RecordType.Missing,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = value,
                        style = if (emphasis) RecordType.KeyValue else RecordType.FieldValue,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/**
 * Equal-width fields side by side (pairs, the G P A triple).
 *
 * Callers pass `stretch = true` and `Modifier.weight(1f).fillMaxHeight()` to each
 * [LabeledValue] so boxes in one row are equal height.
 */
@Composable
fun LabeledValueRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(RecordSpacing.S),
        content = content,
    )
}

/** Tappable record (previous pregnancy, later visits and scans). */
@Composable
fun RecordRow(
    onClick: (() -> Unit)?,
    onClickLabel: String?,
    modifier: Modifier = Modifier,
    family: FieldFamily = currentFieldFamily(),
    clickTag: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickModifier = if (onClick != null) {
        Modifier
            .clickable(
                onClickLabel = onClickLabel,
                role = Role.Button,
                onClick = onClick,
            )
            .minimumTouchTarget()
    } else {
        Modifier
    }
    val tagModifier = if (clickTag != null) Modifier.testTag(clickTag) else Modifier

    Surface(
        shape = RecordShapes.Small,
        color = family.container,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(clickModifier)
                    .then(tagModifier)
                    .heightIn(min = 56.dp)
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = content,
            )
            trailing?.invoke()
        }
    }
}
