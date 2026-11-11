package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.ClinicalVocabulary
import com.sherifradwan.ancnative.model.MEDICATION_STATUS_ACTIVE
import com.sherifradwan.ancnative.model.MedicationOnOff
import com.sherifradwan.ancnative.model.effectiveMedicationStatus
import com.sherifradwan.ancnative.model.medicationEditorChoice
import com.sherifradwan.ancnative.model.medicationStatusAfterChoice
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.ClinicalValidationError
import com.sherifradwan.ancnative.state.DEFAULT_MEDICATION_ROUTE
import com.sherifradwan.ancnative.state.MedicationDraftField
import com.sherifradwan.ancnative.state.MedicationDraftValues
import com.sherifradwan.ancnative.ui.BigChoiceButton
import com.sherifradwan.ancnative.ui.COMMON_MEDICATION_FREQUENCIES
import com.sherifradwan.ancnative.ui.ChoiceGroup
import com.sherifradwan.ancnative.ui.ClinicalDateField
import com.sherifradwan.ancnative.ui.EditorField
import com.sherifradwan.ancnative.ui.medicationFrequencyLabel
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import kotlinx.coroutines.launch

/**
 * The one medication form. It renders immutable draft values and errors and emits only
 * [AncEvent.MedicationDraftChanged]; it holds no business rules.
 *
 * [draftKey] identifies the draft being edited; per-draft UI state (the Other-frequency mode) resets when it changes.
 * With [focusNameOnStart] the name field takes focus for [draftKey], then [onNameFocused] tells the host so it can
 * turn the request off (the host keeps that in saved state, so a rotation does not focus the name again).
 *
 * Fields in order: name, dose, route (read-only on an existing row), frequency, status, one date
 * (Start date while active, Stop date while stopped) and notes. Enter (IME Next) moves focus to the
 * next text field; notes keeps Enter for a new line. Every field asks to be scrolled into view when
 * it takes focus, so the keyboard never hides it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationForm(
    values: MedicationDraftValues,
    errors: Set<ClinicalValidationError>,
    isExistingRow: Boolean,
    enabled: Boolean,
    draftKey: String,
    onEvent: (AncEvent) -> Unit,
    modifier: Modifier = Modifier,
    focusNameOnStart: Boolean = false,
    onNameFocused: () -> Unit = {},
) {
    fun change(field: MedicationDraftField, value: String) =
        onEvent(AncEvent.MedicationDraftChanged(field, value))

    val nameFocus = remember { FocusRequester() }
    val doseFocus = remember { FocusRequester() }
    val otherFocus = remember { FocusRequester() }
    val dateFocus = remember { FocusRequester() }
    val notesFocus = remember { FocusRequester() }

    var otherMode by rememberSaveable(draftKey) {
        mutableStateOf(values.frequency.isNotBlank() && values.frequency !in COMMON_MEDICATION_FREQUENCIES)
    }
    LaunchedEffect(draftKey, focusNameOnStart) {
        if (focusNameOnStart) {
            withFrameNanos { }
            nameFocus.requestFocus()
            onNameFocused()
        }
    }
    var focusOtherBox by remember { mutableStateOf(false) }
    LaunchedEffect(focusOtherBox, otherMode) {
        if (focusOtherBox && otherMode) {
            withFrameNanos { }
            otherFocus.requestFocus()
            focusOtherBox = false
        }
    }

    val status = effectiveMedicationStatus(values.status.ifEmpty { MEDICATION_STATUS_ACTIVE }, values.isNonCompliant)
    val choice = medicationEditorChoice(status)
    val stopped = choice == MedicationOnOff.STOPPED
    val dateField = if (stopped) MedicationDraftField.END_DATE else MedicationDraftField.START_DATE
    val dateValue = if (stopped) values.endDate else values.startDate
    val dateError = errors.contains(ClinicalValidationError.REQUIRED_DATE) ||
        errors.contains(ClinicalValidationError.INVALID_DATE)

    Column(
        modifier = modifier.fillMaxWidth().testTag("medicationForm"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditorField(
            value = values.name,
            onValueChange = { change(MedicationDraftField.NAME, it) },
            label = stringResource(R.string.medication_name),
            tag = "medName",
            modifier = Modifier.focusRequester(nameFocus).revealOnFocus(),
            enabled = enabled,
            error = errors.contains(ClinicalValidationError.REQUIRED_NAME),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { doseFocus.requestFocus() }),
        )

        EditorField(
            value = values.dosage,
            onValueChange = { change(MedicationDraftField.DOSAGE, it) },
            label = stringResource(R.string.dosage),
            tag = "medDosage",
            modifier = Modifier.focusRequester(doseFocus).revealOnFocus(),
            enabled = enabled,
            error = errors.contains(ClinicalValidationError.REQUIRED_DOSAGE),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { (if (otherMode) otherFocus else dateFocus).requestFocus() },
            ),
        )

        if (isExistingRow) {
            RouteReadOnly(route = values.route.ifBlank { DEFAULT_MEDICATION_ROUTE })
        } else {
            ChoiceGroup(
                label = stringResource(R.string.route),
                selected = values.route.ifEmpty { DEFAULT_MEDICATION_ROUTE },
                choices = ClinicalVocabulary.medicationRoutes.toList(),
                enabled = enabled,
                tagPrefix = "medRouteChoice",
                onSelected = { change(MedicationDraftField.ROUTE, it) },
                includeUnknown = false,
                rowGap = 0.dp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.frequency), style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                COMMON_MEDICATION_FREQUENCIES.forEachIndexed { index, frequency ->
                    FilterChip(
                        selected = !otherMode && values.frequency == frequency,
                        onClick = {
                            otherMode = false
                            change(MedicationDraftField.FREQUENCY, frequency)
                        },
                        enabled = enabled,
                        label = { Text(medicationFrequencyLabel(frequency)) },
                        modifier = Modifier.minimumTouchTarget().testTag("medFreqChoice-$index"),
                    )
                }
                FilterChip(
                    selected = otherMode,
                    onClick = {
                        if (!otherMode) {
                            otherMode = true
                            if (values.frequency in COMMON_MEDICATION_FREQUENCIES) {
                                change(MedicationDraftField.FREQUENCY, "")
                            }
                        }
                        focusOtherBox = true
                    },
                    enabled = enabled,
                    label = { Text(stringResource(R.string.medication_frequency_other)) },
                    modifier = Modifier.minimumTouchTarget().testTag("medFreqChoice-other"),
                )
            }
            val frequencyMissing = errors.contains(ClinicalValidationError.REQUIRED_FREQUENCY)
            if (otherMode) {
                EditorField(
                    value = values.frequency,
                    onValueChange = { change(MedicationDraftField.FREQUENCY, it) },
                    label = stringResource(R.string.medication_frequency_other_label),
                    tag = "medFreqOther",
                    modifier = Modifier.focusRequester(otherFocus).revealOnFocus(),
                    enabled = enabled,
                    error = frequencyMissing,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { dateFocus.requestFocus() }),
                )
            } else if (frequencyMissing) {
                Text(
                    text = stringResource(R.string.medication_frequency_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("medFreqError"),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.status), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    MedicationOnOff.ON to stringResource(R.string.medication_status_active),
                    MedicationOnOff.STOPPED to stringResource(R.string.medication_status_stopped),
                ).forEachIndexed { index, (option, label) ->
                    BigChoiceButton(
                        label = label,
                        selected = choice == option,
                        tag = "medStatusChoice-$index",
                        onClick = {
                            val next = medicationStatusAfterChoice(status, option)
                            if (next != status) change(MedicationDraftField.STATUS, next)
                        },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        ClinicalDateField(
            value = dateValue,
            onValueChange = { change(dateField, it) },
            label = stringResource(if (stopped) R.string.medication_stop_date else R.string.start_date),
            tag = "medDate",
            enabled = enabled,
            error = dateError,
            fieldModifier = Modifier.focusRequester(dateFocus).revealOnFocus(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { notesFocus.requestFocus() }),
        )

        // Notes is multi-line: Enter inserts a new line, so it carries no IME action of its own.
        EditorField(
            value = values.notes,
            onValueChange = { change(MedicationDraftField.NOTES, it) },
            label = stringResource(R.string.medication_note),
            tag = "medNotes",
            modifier = Modifier.focusRequester(notesFocus).revealOnFocus(),
            enabled = enabled,
            singleLine = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Default),
        )
    }
}

@Composable
private fun RouteReadOnly(route: String) {
    val label = stringResource(R.string.medication_route_locked_label)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().testTag("medRouteReadOnly"),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier,
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(route, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("medRouteValue"))
        }
        Text(
            text = stringResource(R.string.medication_route_locked_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Asks the enclosing scroll container to bring this field fully into view when it gains focus. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.revealOnFocus(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    return this
        .bringIntoViewRequester(requester)
        .onFocusChanged { if (it.isFocused) scope.launch { requester.bringIntoView() } }
}
