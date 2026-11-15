package com.sherifradwan.ancnative.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.ui.StatusHighBg
import com.sherifradwan.ancnative.ui.StatusHighBorder
import com.sherifradwan.ancnative.ui.StatusHighDarkBg
import com.sherifradwan.ancnative.ui.StatusHighDarkBorder
import com.sherifradwan.ancnative.ui.StatusHighDarkText
import com.sherifradwan.ancnative.ui.StatusHighText
import com.sherifradwan.ancnative.ui.StatusIncompleteBg
import com.sherifradwan.ancnative.ui.StatusIncompleteBorder
import com.sherifradwan.ancnative.ui.StatusIncompleteDarkBg
import com.sherifradwan.ancnative.ui.StatusIncompleteDarkBorder
import com.sherifradwan.ancnative.ui.StatusIncompleteDarkText
import com.sherifradwan.ancnative.ui.StatusIncompleteText
import com.sherifradwan.ancnative.ui.StatusModerateBg
import com.sherifradwan.ancnative.ui.StatusModerateBorder
import com.sherifradwan.ancnative.ui.StatusModerateDarkBg
import com.sherifradwan.ancnative.ui.StatusModerateDarkBorder
import com.sherifradwan.ancnative.ui.StatusModerateDarkText
import com.sherifradwan.ancnative.ui.StatusModerateText
import com.sherifradwan.ancnative.ui.isDarkTheme

enum class StatusTone { High, Moderate, Neutral }

enum class StatusKind { HighRisk, ModerateRisk, Incomplete, Delivered }

@Composable
fun defaultText(kind: StatusKind): String = when (kind) {
    StatusKind.HighRisk -> stringResource(R.string.high_risk)
    StatusKind.ModerateRisk -> stringResource(R.string.moderate_risk)
    StatusKind.Incomplete -> stringResource(R.string.registration_incomplete_chip)
    StatusKind.Delivered -> stringResource(R.string.status_delivered)
}

@Composable
fun StatusTag(
    kind: StatusKind,
    modifier: Modifier = Modifier,
    text: String = defaultText(kind),
) {
    val dark = isDarkTheme()
    val (bg, border, fg) = when (kind) {
        StatusKind.HighRisk -> if (dark) {
            Triple(StatusHighDarkBg, StatusHighDarkBorder, StatusHighDarkText)
        } else {
            Triple(StatusHighBg, StatusHighBorder, StatusHighText)
        }
        StatusKind.ModerateRisk -> if (dark) {
            Triple(StatusModerateDarkBg, StatusModerateDarkBorder, StatusModerateDarkText)
        } else {
            Triple(StatusModerateBg, StatusModerateBorder, StatusModerateText)
        }
        StatusKind.Incomplete -> if (dark) {
            Triple(StatusIncompleteDarkBg, StatusIncompleteDarkBorder, StatusIncompleteDarkText)
        } else {
            Triple(StatusIncompleteBg, StatusIncompleteBorder, StatusIncompleteText)
        }
        StatusKind.Delivered -> {
            val scheme = MaterialTheme.colorScheme
            Triple(scheme.surface, scheme.primary, scheme.primary)
        }
    }

    Surface(
        shape = RecordShapes.Small,
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier.heightIn(min = 24.dp),
    ) {
        Text(
            text = text,
            style = RecordType.Tag.copy(
                fontWeight = if (kind == StatusKind.HighRisk) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun StatusWord(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val dark = isDarkTheme()
    val color = when (tone) {
        StatusTone.High -> MaterialTheme.colorScheme.error
        StatusTone.Moderate -> if (dark) StatusModerateDarkText else StatusModerateText
        StatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        style = RecordType.Tag.copy(fontWeight = if (tone == StatusTone.Neutral) FontWeight.SemiBold else FontWeight.Bold),
        color = color,
        modifier = modifier,
    )
}

@Composable
fun BandChip(
    text: String,
    tone: StatusTone?,
    modifier: Modifier = Modifier,
    family: FieldFamily = currentFieldFamily(),
) {
    val dark = isDarkTheme()
    val (bg, border, fg) = when (tone) {
        null -> Triple(MaterialTheme.colorScheme.surface, Color.Transparent, family.label)
        StatusTone.Neutral -> Triple(MaterialTheme.colorScheme.surface, Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant)
        StatusTone.Moderate -> if (dark) {
            Triple(StatusModerateDarkBg, StatusModerateDarkBorder, StatusModerateDarkText)
        } else {
            Triple(StatusModerateBg, StatusModerateBorder, StatusModerateText)
        }
        StatusTone.High -> if (dark) {
            Triple(StatusHighDarkBg, StatusHighDarkBorder, StatusHighDarkText)
        } else {
            Triple(StatusHighBg, StatusHighBorder, StatusHighText)
        }
    }
    Surface(
        shape = RecordShapes.Small,
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier.heightIn(min = 24.dp),
    ) {
        Text(
            text = text,
            style = RecordType.Tag.copy(fontWeight = FontWeight.Bold),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}
