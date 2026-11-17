package com.sherifradwan.ancnative.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.SavePhase
import com.sherifradwan.ancnative.state.stringRes
import com.sherifradwan.ancnative.ui.components.FormFieldLabel
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType

@Composable
fun FactRow(
    @StringRes label: Int,
    value: String?,
    isEmphasized: Boolean = false,
    valueColor: Color? = null,
) {
    FactRow(
        label = stringResource(label),
        value = value,
        isEmphasized = isEmphasized,
        valueColor = valueColor,
    )
}

@Composable
fun FactRow(
    label: String,
    value: String?,
    isEmphasized: Boolean = false,
    valueColor: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.1f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = value?.takeIf(String::isNotBlank) ?: stringResource(R.string.not_recorded),
            style = if (isEmphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor ?: if (value.isNullOrBlank()) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1.3f),
        )
    }
}

fun Modifier.minimumTouchTarget(): Modifier = sizeIn(minWidth = 48.dp, minHeight = 48.dp)

@Composable
internal fun editorFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    errorBorderColor = MaterialTheme.colorScheme.error,
)

@Composable
internal fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    textStyle: TextStyle = LocalTextStyle.current,
    externalLabel: Boolean = false,
) {
    val showError = error || errorMessage != null
    val field: @Composable (Modifier) -> Unit = { fieldModifier ->
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = if (externalLabel || label.isEmpty()) null else ({ Text(label) }),
            supportingText = if (showError) {
                {
                    Text(
                        text = errorMessage ?: stringResource(R.string.correct_highlighted_field),
                        style = if (externalLabel || label.isEmpty()) RecordType.Tag else LocalTextStyle.current,
                    )
                }
            } else {
                null
            },
            isError = showError,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            enabled = enabled,
            shape = shape,
            textStyle = textStyle,
            colors = if (externalLabel || label.isEmpty()) editorFieldColors() else OutlinedTextFieldDefaults.colors(),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = fieldModifier.fillMaxWidth().testTag(tag),
        )
    }
    if (externalLabel) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.clearAndSetSemantics {}) {
                if (showError) {
                    Text(
                        text = label,
                        style = RecordType.FieldLabel,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    FormFieldLabel(text = label)
                }
            }
            field(Modifier.semantics { contentDescription = label })
        }
    } else {
        field(modifier)
    }
}

@Composable
internal fun GenericEditorErrorCard(
    hasErrors: Boolean,
    state: AncState,
    useRecordStyle: Boolean = false,
) {
    if (!hasErrors && state.savePhase == SavePhase.IDLE) return
    val text = when {
        hasErrors -> stringResource(R.string.correct_highlighted_fields)
        state.savePhase == SavePhase.PENDING -> stringResource(R.string.save_pending)
        state.savePhase == SavePhase.RECONCILING -> stringResource(R.string.save_reconciling)
        state.savePhase == SavePhase.UNRESOLVED -> stringResource(R.string.save_unresolved)
        else -> stringResource(state.saveError.stringRes())
    }
    val isErrorState = hasErrors || state.savePhase == SavePhase.ERROR || state.savePhase == SavePhase.UNRESOLVED
    if (useRecordStyle) {
        val bgColor = if (isErrorState) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
        val textColor = if (isErrorState) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
        val borderColor = if (isErrorState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
        OutlinedCard(
            shape = RecordShapes.Large,
            border = BorderStroke(1.dp, borderColor),
            colors = CardDefaults.outlinedCardColors(containerColor = bgColor),
            modifier = Modifier.fillMaxWidth().testTag("editorError"),
        ) {
            Text(
                text = text,
                style = RecordType.Action,
                color = textColor,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth().testTag("editorError")) {
            Text(
                text = text,
                color = if (isErrorState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
