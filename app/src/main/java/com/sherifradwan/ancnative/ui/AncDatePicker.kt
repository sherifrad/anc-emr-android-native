package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.parseIsoDateOrNull
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.FormFieldLabel
import com.sherifradwan.ancnative.ui.components.RecordFilterChip
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AncDatePickerDialog(
    initialDateText: String = "",
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.pick_date),
) {
    val initialMillis = remember(initialDateText) {
        (parseIsoDateOrNull(initialDateText) ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    val dateFormatter = remember { DatePickerDefaults.dateFormatter() }
    val pickerColors = DatePickerDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = FieldFamilies.patient().label,
        headlineContentColor = MaterialTheme.colorScheme.primary,
        weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        subheadContentColor = MaterialTheme.colorScheme.primary,
        navigationContentColor = MaterialTheme.colorScheme.primary,
        yearContentColor = MaterialTheme.colorScheme.onSurface,
        currentYearContentColor = MaterialTheme.colorScheme.primary,
        selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
        selectedYearContainerColor = MaterialTheme.colorScheme.primary,
        dayContentColor = MaterialTheme.colorScheme.onSurface,
        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
        todayContentColor = MaterialTheme.colorScheme.primary,
        todayDateBorderColor = MaterialTheme.colorScheme.primary,
        dividerColor = MaterialTheme.colorScheme.outlineVariant,
    )

    DatePickerDialog(
        modifier = Modifier.testTag("ancDatePickerDialog"),
        onDismissRequest = onDismiss,
        shape = RecordShapes.Large,
        tonalElevation = 0.dp,
        colors = pickerColors,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onDateSelected(selectedDate.toString())
                    }
                    onDismiss()
                },
                shape = RecordShapes.Small,
                modifier = Modifier.minimumTouchTarget().testTag("datePickerConfirm"),
            ) {
                Text(stringResource(R.string.save), style = RecordType.Action)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RecordShapes.Small,
                modifier = Modifier.minimumTouchTarget().testTag("datePickerDismiss"),
            ) {
                Text(
                    stringResource(R.string.cancel),
                    style = RecordType.Action,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            colors = pickerColors,
            title = {
                Text(
                    text = title,
                    style = RecordType.FieldLabel,
                    color = FieldFamilies.patient().label,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            headline = {
                ProvideTextStyle(RecordType.HeaderName) {
                    DatePickerDefaults.DatePickerHeadline(
                        selectedDateMillis = datePickerState.selectedDateMillis,
                        displayMode = datePickerState.displayMode,
                        dateFormatter = dateFormatter,
                        contentColor = pickerColors.headlineContentColor,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClinicalDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    enabled: Boolean,
    error: Boolean = false,
    errorMessage: String? = null,
    showTodayButton: Boolean = true,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    externalLabel: Boolean = false,
    shape: Shape = RecordShapes.Small,
    textStyle: TextStyle = RecordType.FieldValue,
    useRecordStyle: Boolean = false,
    allowClear: Boolean = false,
    helperText: String? = null,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val pickDateCd = stringResource(R.string.date_picker_open_cd, label)
    val externalContentDescription = if (externalLabel && value.isNotBlank()) {
        stringResource(R.string.date_field_value_cd, label, value)
    } else {
        label
    }
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, enabled) {
        if (enabled) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    showDatePicker = true
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier.fillMaxWidth()) {
        if (externalLabel) {
            Box(modifier = Modifier.clearAndSetSemantics {}) {
                if (error) {
                    Text(
                        text = label,
                        style = RecordType.FieldLabel,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    FormFieldLabel(text = label)
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = true,
            label = if (externalLabel) null else ({ Text(label) }),
            placeholder = {
                Text(
                    text = stringResource(R.string.pick_date),
                    style = RecordType.FieldValue,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            supportingText = when {
                error && errorMessage != null -> ({
                    Text(
                        text = errorMessage,
                        style = RecordType.Tag,
                    )
                })
                error -> ({
                    Text(
                        text = stringResource(R.string.correct_highlighted_field),
                        style = RecordType.Tag,
                    )
                })
                helperText != null -> ({
                    Text(
                        text = helperText,
                        style = RecordType.Tag,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                })
                else -> null
            },
            isError = error,
            singleLine = true,
            enabled = enabled,
            shape = if (useRecordStyle) RecordShapes.Small else shape,
            textStyle = if (useRecordStyle) RecordType.FieldValue else textStyle,
            colors = editorFieldColors(),
            interactionSource = interactionSource,
            trailingIcon = {
                IconButton(
                    onClick = { showDatePicker = true },
                    enabled = enabled,
                    modifier = Modifier
                        .minimumTouchTarget()
                        .testTag("${tag}CalendarButton"),
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = stringResource(R.string.pick_date),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = fieldModifier
                .fillMaxWidth()
                .onKeyEvent { event ->
                    if (
                        enabled &&
                        event.type == KeyEventType.KeyUp &&
                        (
                            event.key == Key.Enter ||
                                event.key == Key.NumPadEnter ||
                                event.key == Key.DirectionCenter
                            )
                    ) {
                        showDatePicker = true
                        true
                    } else {
                        false
                    }
                }
                .semantics {
                    if (externalLabel) contentDescription = externalContentDescription
                    onClick(label = pickDateCd) {
                        if (enabled) {
                            showDatePicker = true
                            true
                        } else {
                            false
                        }
                    }
                }
                .testTag(tag),
        )

        if (enabled && (showTodayButton || (allowClear && value.isNotBlank()))) {
            val todayLabel = stringResource(R.string.today) + " (" + LocalDate.now().toString() + ")"
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (allowClear && value.isNotBlank()) {
                    RecordFilterChip(
                        selected = false,
                        label = stringResource(R.string.clear_date),
                        onClick = { onValueChange("") },
                        tag = "${tag}ClearButton",
                        wrap = true,
                        minTouchTarget = true,
                    )
                }
                if (showTodayButton) {
                    RecordFilterChip(
                        selected = false,
                        label = todayLabel,
                        onClick = { onValueChange(LocalDate.now().toString()) },
                        tag = "${tag}TodayButton",
                        wrap = true,
                        minTouchTarget = true,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        AncDatePickerDialog(
            initialDateText = value,
            onDateSelected = { selected ->
                onValueChange(selected)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            title = label,
        )
    }
}
