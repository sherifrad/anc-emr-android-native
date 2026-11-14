package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.daysSinceDelivery
import com.sherifradwan.ancnative.model.deliveryModeText
import com.sherifradwan.ancnative.model.deliveryResultText
import com.sherifradwan.ancnative.ui.components.LabeledValue
import com.sherifradwan.ancnative.ui.components.LabeledValueRow
import com.sherifradwan.ancnative.ui.components.RecordSpacing
import com.sherifradwan.ancnative.ui.components.StatusKind
import com.sherifradwan.ancnative.ui.components.StatusTag
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

fun deliveryModeLabel(value: String?): String? = deliveryModeText(value)

fun deliveryResultLabel(value: String?): String? = deliveryResultText(value)

private fun parseDeliveryDate(date: String?): LocalDate? = date?.let {
    try {
        LocalDate.parse(it.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: DateTimeParseException) {
        null
    }
}

@Composable
fun deliveredHeadline(date: String?): String {
    val parsed = parseDeliveryDate(date)
    return if (parsed != null) {
        val formatted = parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
        stringResource(R.string.delivered_headline, formatted)
    } else {
        stringResource(R.string.delivered_date_unknown)
    }
}

@Composable
fun deliveredDaysAgo(date: String?, today: LocalDate = LocalDate.now()): String? {
    val days = daysSinceDelivery(date, today)
    return when (days) {
        null -> null
        0L -> stringResource(R.string.delivered_today)
        1L -> stringResource(R.string.delivered_one_day_ago)
        else -> stringResource(R.string.delivered_days_ago, days.toInt())
    }
}

@Composable
fun deliveredDateText(date: String?): String? {
    val parsed = parseDeliveryDate(date) ?: return null
    val formatted = parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
    val ago = deliveredDaysAgo(date)
    return if (ago != null) "$formatted, $ago" else formatted
}

@Composable
fun DeliveredSummary(
    patient: DemoPatient,
    modifier: Modifier = Modifier,
) {
    val modeLabel = stringResource(R.string.delivery_outcome_mode_label)
    val resultLabel = stringResource(R.string.delivery_outcome_result_label)
    val mode = deliveryModeLabel(patient.deliveryMode)
    val result = deliveryResultLabel(patient.deliveryOutcome)

    Column(
        modifier = modifier.testTag("deliveredSummary"),
        verticalArrangement = Arrangement.spacedBy(RecordSpacing.M),
    ) {
        StatusTag(StatusKind.Delivered)
        LabeledValue(
            label = stringResource(R.string.delivery_date_label),
            value = deliveredDateText(patient.deliveryDate),
            emphasis = true,
            missingText = stringResource(R.string.delivery_date_unknown_value),
            modifier = Modifier.testTag("deliveredHeadline"),
        )
        when {
            mode != null && result != null -> LabeledValueRow {
                LabeledValue(
                    label = modeLabel,
                    value = mode,
                    stretch = true,
                    modifier = Modifier
                        .testTag("deliveredMode")
                        .weight(1f)
                        .fillMaxHeight(),
                )
                LabeledValue(
                    label = resultLabel,
                    value = result,
                    stretch = true,
                    modifier = Modifier
                        .testTag("deliveredResult")
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
            mode != null -> LabeledValue(
                label = modeLabel,
                value = mode,
                modifier = Modifier.testTag("deliveredMode"),
            )
            result != null -> LabeledValue(
                label = resultLabel,
                value = result,
                modifier = Modifier.testTag("deliveredResult"),
            )
        }
    }
}
