package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DELIVERY_MODE_ASSISTED_VAGINAL
import com.sherifradwan.ancnative.model.DELIVERY_MODE_CAESAREAN
import com.sherifradwan.ancnative.model.DELIVERY_MODE_OTHER
import com.sherifradwan.ancnative.model.DELIVERY_MODE_VAGINAL
import com.sherifradwan.ancnative.model.DELIVERY_OUTCOME_LIVE_BIRTH
import com.sherifradwan.ancnative.model.DELIVERY_OUTCOME_NEONATAL_DEATH
import com.sherifradwan.ancnative.model.DELIVERY_OUTCOME_STILLBIRTH
import com.sherifradwan.ancnative.model.daysSinceDelivery
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun deliveryModeLabel(value: String?): String? {
    return when (value) {
        DELIVERY_MODE_VAGINAL -> stringResource(R.string.delivery_mode_vaginal)
        DELIVERY_MODE_CAESAREAN -> stringResource(R.string.delivery_mode_caesarean)
        DELIVERY_MODE_ASSISTED_VAGINAL -> stringResource(R.string.delivery_mode_assisted_vaginal)
        DELIVERY_MODE_OTHER -> stringResource(R.string.delivery_mode_other)
        else -> null
    }
}

@Composable
fun deliveryResultLabel(value: String?): String? {
    return when (value) {
        DELIVERY_OUTCOME_LIVE_BIRTH -> stringResource(R.string.delivery_result_live_birth)
        DELIVERY_OUTCOME_STILLBIRTH -> stringResource(R.string.delivery_result_stillbirth)
        DELIVERY_OUTCOME_NEONATAL_DEATH -> stringResource(R.string.delivery_result_neonatal_death)
        else -> null
    }
}

@Composable
fun deliveredHeadline(date: String?): String {
    val parsed = date?.let {
        try {
            LocalDate.parse(it.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            null
        }
    }
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
fun DeliveredSummary(
    patient: DemoPatient,
    modifier: Modifier = Modifier
) {
    val headlineText = deliveredHeadline(patient.deliveryDate)
    val daysAgoText = deliveredDaysAgo(patient.deliveryDate)
    val combinedHeadline = if (daysAgoText != null) {
        "$headlineText · $daysAgoText"
    } else {
        headlineText
    }
    val modeLabel = deliveryModeLabel(patient.deliveryMode)
    val resultLabel = deliveryResultLabel(patient.deliveryOutcome)

    Column(
        modifier = modifier.testTag("deliveredSummary"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = combinedHeadline,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("deliveredHeadline")
        )
        modeLabel?.let { mode ->
            val labelText = stringResource(R.string.delivery_outcome_mode_label) + ": $mode"
            Text(
                text = labelText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("deliveredMode")
            )
        }
        resultLabel?.let { result ->
            val labelText = stringResource(R.string.delivery_outcome_result_label) + ": $result"
            Text(
                text = labelText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("deliveredResult")
            )
        }
    }
}
