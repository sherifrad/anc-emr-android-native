package com.sherifradwan.ancnative.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun MetricCell(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = RecordType.MetricLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        content()
    }
}

@Composable
fun MetricValue(
    value: String,
    modifier: Modifier = Modifier,
    isMissing: Boolean = false,
    color: Color = if (isMissing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = value,
        style = if (isMissing) RecordType.Missing else RecordType.MetricValue,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

fun parityText(gravida: Long?, parity: Long?, abortion: Long?): String {
    val g = gravida ?: 1L
    val p = parity ?: 0L
    val a = abortion ?: 0L
    return if (a > 0L) "G$g P$p A$a" else "G$g P$p"
}
