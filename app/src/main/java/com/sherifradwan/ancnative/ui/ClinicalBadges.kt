package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.bpLevel

/**
 * Dedicated maternal-age badge: a solid indigo "AGE" label segment joined to
 * a soft indigo value segment, so age reads as its own clinical identity.
 */
@Composable
fun PatientAgeBadge(ageText: String?, modifier: Modifier = Modifier, large: Boolean = false) {
    val cleanAge = formatAgeYears(ageText) ?: return
    val dark = isDarkTheme()
    val bg = if (dark) TagAgeDarkBg else TagAgeBg
    val border = if (dark) TagAgeDarkBorder else TagAgeBorder
    val text = if (dark) TagAgeDarkText else TagAgeText
    val labelBg = if (dark) TagAgeLabelDarkBg else TagAgeLabelBg
    val valueStyle = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier
            .testTag("patientAgeBadge")
            .clearAndSetSemantics { contentDescription = "Age $cleanAge years" },
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "AGE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TagAgeLabelText,
                modifier = Modifier
                    .background(labelBg)
                    .padding(horizontal = 6.dp, vertical = if (large) 5.dp else 3.dp),
            )
            Text(
                text = "$cleanAge yrs",
                style = valueStyle,
                fontWeight = FontWeight.Bold,
                color = text,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = if (large) 4.dp else 2.dp),
            )
        }
    }
}

enum class DeliveryTone { HOSPITAL, FEE, PLAN, POSTPARTUM }

private data class ToneColors(val bg: Color, val border: Color, val text: Color)

@Composable
private fun DeliveryTone.colors(): ToneColors {
    val dark = isDarkTheme()
    return when (this) {
        DeliveryTone.HOSPITAL -> if (dark) ToneColors(DeliveryHospitalDarkBg, DeliveryHospitalDarkBorder, DeliveryHospitalDarkText)
        else ToneColors(DeliveryHospitalBg, DeliveryHospitalBorder, DeliveryHospitalText)
        DeliveryTone.FEE -> if (dark) ToneColors(DeliveryFeeDarkBg, DeliveryFeeDarkBorder, DeliveryFeeDarkText)
        else ToneColors(DeliveryFeeBg, DeliveryFeeBorder, DeliveryFeeText)
        DeliveryTone.PLAN -> if (dark) ToneColors(DeliveryPlanDarkBg, DeliveryPlanDarkBorder, DeliveryPlanDarkText)
        else ToneColors(DeliveryPlanBg, DeliveryPlanBorder, DeliveryPlanText)
        DeliveryTone.POSTPARTUM -> if (dark) ToneColors(DeliveryPostpartumDarkBg, DeliveryPostpartumDarkBorder, DeliveryPostpartumDarkText)
        else ToneColors(DeliveryPostpartumBg, DeliveryPostpartumBorder, DeliveryPostpartumText)
    }
}

/**
 * A titled delivery subsection: small uppercase colored title, value in its own tinted box.
 */
@Composable
fun DeliverySectionBox(
    title: String,
    value: String?,
    tone: DeliveryTone,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val c = tone.colors()
    val hasValue = !value.isNullOrBlank()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = c.text,
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = c.bg,
            border = BorderStroke(1.dp, c.border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (hasValue) value!! else placeholder,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (hasValue) FontWeight.SemiBold else FontWeight.Normal,
                fontStyle = if (hasValue) FontStyle.Normal else FontStyle.Italic,
                color = c.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

/** Compact label-over-value tile used for key delivery facts (blood group, EDD, GA...). */
@Composable
fun KeyFactTile(label: String, value: String?, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Text(
                text = value?.takeIf { it.isNotBlank() } ?: "-",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (emphasize) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Alert badge for abnormal blood pressure (Q87). Renders nothing when bpAlertResource is null.
 * Severe-range BP uses errorContainer/onErrorContainer; raised BP uses tertiaryContainer/onTertiaryContainer.
 */
@Composable
fun BpAlertLabel(raw: String?, modifier: Modifier = Modifier) {
    val res = bpAlertResource(raw) ?: return
    val isSevere = bpLevel(raw) == BpLevel.SEVERE
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
            text = stringResource(res),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun ClinicalTagBadge(
    text: String,
    showWarningIcon: Boolean = false,
    isError: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val isDark = isDarkTheme()
    val (bgColor, borderColor, textColor) = if (isError) {
        if (isDark) {
            Triple(TagHighRiskDarkBg, TagHighRiskDarkBorder, TagHighRiskDarkText)
        } else {
            Triple(TagHighRiskBg, TagHighRiskBorder, TagHighRiskText)
        }
    } else {
        if (isDark) {
            Triple(TagNormalRiskDarkBg, TagNormalRiskDarkBorder, TagNormalRiskDarkText)
        } else {
            Triple(TagNormalRiskBg, TagNormalRiskBorder, TagNormalRiskText)
        }
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            if (showWarningIcon) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isError) FontWeight.Bold else FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun ClinicalRiskBadge(isHighRisk: Boolean, modifier: Modifier = Modifier) {
    if (isHighRisk) {
        ClinicalTagBadge(
            text = stringResource(R.string.high_risk),
            showWarningIcon = true,
            isError = true,
            modifier = modifier,
        )
    } else {
        ClinicalTagBadge(
            text = stringResource(R.string.normal_risk_filter),
            showWarningIcon = false,
            isError = false,
            modifier = modifier,
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

/**
 * Canonical Blood Group Badge.
 * Highlights Rh-negative status prominently with TagBloodNegative tokens as a critical clinical flag.
 * Supports dark mode automatically via isDarkTheme().
 */
@Composable
fun BloodGroupBadge(bloodGroup: String?, modifier: Modifier = Modifier) {
    val formatted = formatBloodGroup(bloodGroup) ?: return
    val dark = isDarkTheme()
    val isRhNeg = isRhNegative(bloodGroup)
    val bg = if (isRhNeg) {
        if (dark) TagBloodNegativeDarkBg else TagBloodNegativeBg
    } else {
        if (dark) TagBloodPositiveDarkBg else TagBloodPositiveBg
    }
    val border = if (isRhNeg) {
        if (dark) TagBloodNegativeDarkBorder else TagBloodNegativeBorder
    } else {
        if (dark) TagBloodPositiveDarkBorder else TagBloodPositiveBorder
    }
    val text = if (isRhNeg) {
        if (dark) TagBloodNegativeDarkText else TagBloodNegativeText
    } else {
        if (dark) TagBloodPositiveDarkText else TagBloodPositiveText
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier.testTag("bloodGroupBadge"),
    ) {
        Text(
            text = formatted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isRhNeg) FontWeight.Bold else FontWeight.SemiBold,
            color = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

/**
 * Canonical Gestational Age Badge.
 * Renders gestational age (e.g., "28w 4d") with softWrap = false to prevent 360dp text wrapping.
 * Resolves dark mode automatically via isDarkTheme().
 */
@Composable
fun GestationalAgeBadge(gaText: String?, modifier: Modifier = Modifier, discordant: Boolean = false) {
    if (gaText.isNullOrBlank()) return
    val dark = isDarkTheme()
    val bg = if (dark) TagGestationalAgeDarkBg else TagGestationalAgeBg
    val border = if (dark) TagGestationalAgeDarkBorder else TagGestationalAgeBorder
    val text = if (dark) TagGestationalAgeDarkText else TagGestationalAgeText

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier.testTag("gestationalAgeBadge"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = gaText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = text,
                maxLines = 1,
                softWrap = false,
            )
            if (discordant) GaDiscordanceMarker()
        }
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

/**
 * Canonical Obstetric Parity (GPA) Badge.
 * Formats Gravida, Parity, Abortion (e.g. "G2 P1 A0" or "G1 P0").
 * Automatically resolves dark mode via isDarkTheme().
 */
@Composable
fun ObstetricParityBadge(
    gravida: Long?,
    parity: Long?,
    abortion: Long?,
    modifier: Modifier = Modifier,
) {
    val g = gravida ?: 1L
    val p = parity ?: 0L
    val a = abortion ?: 0L
    val text = "G$g P$p" + if (a > 0) " A$a" else ""
    ObstetricParityBadge(summaryText = text, modifier = modifier)
}

@Composable
fun ObstetricParityBadge(
    summaryText: String,
    modifier: Modifier = Modifier,
) {
    if (summaryText.isBlank()) return
    val dark = isDarkTheme()
    val bg = if (dark) TagObstetricDarkBg else TagObstetricBg
    val border = if (dark) TagObstetricDarkBorder else TagObstetricBorder
    val text = if (dark) TagObstetricDarkText else TagObstetricText

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier.testTag("obstetricParityBadge"),
    ) {
        Text(
            text = summaryText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}
