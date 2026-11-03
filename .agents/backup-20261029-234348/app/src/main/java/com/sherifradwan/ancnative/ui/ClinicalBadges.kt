package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
private fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/**
 * Dedicated maternal-age badge: a solid indigo "AGE" label segment joined to
 * a soft indigo value segment, so age reads as its own clinical identity.
 */
@Composable
fun PatientAgeBadge(ageText: String?, modifier: Modifier = Modifier, large: Boolean = false) {
    val cleanAge = formatAgeYears(ageText) ?: return
    val dark = isDarkSurface()
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
    val dark = isDarkSurface()
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
    icon: String,
    value: String?,
    tone: DeliveryTone,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val c = tone.colors()
    val hasValue = !value.isNullOrBlank()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$icon  ${title.uppercase()}",
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
                maxLines = 1,
            )
            Text(
                text = value?.takeIf { it.isNotBlank() } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (emphasize) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
