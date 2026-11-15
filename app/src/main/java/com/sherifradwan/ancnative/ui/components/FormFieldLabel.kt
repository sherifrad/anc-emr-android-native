package com.sherifradwan.ancnative.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Label above a group of choices or a field in a form: 12 sp, the family label colour inside a family, else onSurfaceVariant. */
@Composable
fun FormFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = RecordType.FieldLabel,
        color = LocalFieldFamily.current?.label ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
