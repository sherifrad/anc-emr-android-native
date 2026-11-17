package com.sherifradwan.ancnative.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.bpLevel

/**
 * Alert badge for abnormal blood pressure (Q87). Renders nothing when bpAlertResource is null.
 * Severe-range BP uses errorContainer/onErrorContainer; raised BP uses tertiaryContainer/onTertiaryContainer.
 */
@Composable
fun BpAlertLabel(raw: String?, modifier: Modifier = Modifier) {
    val res = bpAlertResource(raw) ?: return
    val isSevere = bpLevel(raw) == BpLevel.SEVERE
    BpAlertChip(textRes = res, isSevere = isSevere, modifier = modifier)
}

/** Single BP badge look, shared by BpAlertLabel and callers that already know the severity. */
@Composable
internal fun BpAlertChip(@StringRes textRes: Int, isSevere: Boolean, modifier: Modifier = Modifier) {
    val containerColor = if (isSevere) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = if (isSevere) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        modifier = modifier.testTag("bpAlertLabel"),
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/**
 * Medication status badge (Q205):
 * Design language: only the risk badge is coloured, so Active, Completed and Discontinued are neutral
 * (surfaceContainerHigh / onSurface, outlined). Patient non-compliant stays error-coloured: it is a safety flag.
 * Uses theme tokens only; contrast >= 4.5:1 in light and dark.
 */
@Composable
fun MedicationStatusBadge(status: String, modifier: Modifier = Modifier) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.testTag("medicationStatusBadge"),
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** Red "??" shown after the current gestational age when the LMP and scan due dates disagree. */
@Composable
fun GaDiscordanceMarker(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
) {
    val description = stringResource(R.string.ga_discordance_marker_cd)
    Text(
        text = "??",
        style = style,
        fontWeight = FontWeight.Black,
        color = if (isDarkTheme()) ClinicalCardTokens.CriticalDarkAccent else ClinicalCardTokens.CriticalAccent,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .testTag("gaDiscordanceMarker")
            .semantics { contentDescription = description },
    )
}
