package com.sherifradwan.ancnative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.RegistrationFlow
import com.sherifradwan.ancnative.state.RegistrationStep
import com.sherifradwan.ancnative.state.StepStatus

/** Above this font scale the status words are dropped; the glyph and the spoken description stay. */
private const val WORDS_MAX_FONT_SCALE = 1.3f

/**
 * Where the user is in registration: the step title (wraps, never clipped) and, unless [compact] or the font is very
 * large, five read-only segments. Steps move only through the footer, so the segments are not tappable.
 */
@Composable
fun RegistrationStepHeader(flow: RegistrationFlow, compact: Boolean, modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale
    Surface(
        modifier = modifier.fillMaxWidth().testTag("registrationStepHeader"),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 10.dp),
            ) {
                Text(
                    text = registrationHeaderText(flow),
                    style = RecordType.SectionTitle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
                )
                val target = flow.pregnancyTarget
                if (flow.current == RegistrationStep.PREGNANCIES && target != null) {
                    Text(
                        text = stringResource(R.string.registration_pregnancy_progress, flow.pregnancyIndex + 1, target),
                        style = RecordType.Secondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!compact) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        RegistrationStep.entries.forEachIndexed { index, step ->
                            RegistrationSegment(
                                step = step,
                                status = flow.statuses[step] ?: StepStatus.PENDING,
                                isCurrent = step == flow.current,
                                showWords = fontScale <= WORDS_MAX_FONT_SCALE,
                                modifier = Modifier.weight(1f).testTag("registrationSegment-${index + 1}"),
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun RegistrationSegment(
    step: RegistrationStep,
    status: StepStatus,
    isCurrent: Boolean,
    showWords: Boolean,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(step.titleRes())
    val statusWord = stringResource(status.labelRes())
    val description = if (isCurrent) {
        stringResource(R.string.registration_segment_current_cd, title, statusWord)
    } else {
        stringResource(R.string.registration_segment_cd, title, statusWord)
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = description
            selected = isCurrent
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SegmentBar(status = status, isCurrent = isCurrent)
        Text(
            text = status.glyph(),
            style = RecordType.Tag,
            color = if (status == StepStatus.PENDING) muted else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clearAndSetSemantics {},
        )
        if (showWords) {
            Text(
                text = statusWord,
                style = RecordType.Tag.copy(fontWeight = FontWeight.Medium),
                color = muted,
                textAlign = TextAlign.Center,
            )
            if (isCurrent) {
                Text(
                    text = stringResource(R.string.registration_current),
                    style = RecordType.Tag.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Solid for Done, dashed for Skipped, hollow for To do; the current segment is taller. */
@Composable
private fun SegmentBar(status: StepStatus, isCurrent: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RecordShapes.Small
    val barHeight = if (isCurrent) 8.dp else 6.dp
    val base = Modifier.fillMaxWidth().height(barHeight)
    when (status) {
        StepStatus.DONE -> Box(base.background(primary, shape))
        StepStatus.SKIPPED -> Spacer(
            base.drawBehind {
                val strokePx = 1.5.dp.toPx()
                val half = strokePx / 2f
                val radius = 10.dp.toPx()
                drawRoundRect(
                    color = outline,
                    topLeft = Offset(half, half),
                    size = Size(
                        width = (size.width - strokePx).coerceAtLeast(0f),
                        height = (size.height - strokePx).coerceAtLeast(0f),
                    ),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = strokePx, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))),
                )
            },
        )
        StepStatus.PENDING -> Box(
            base
                .background(
                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape,
                )
                .border(1.5.dp, primary, shape),
        )
    }
}
