package com.sherifradwan.ancnative.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun TimelineSpineItem(
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spineLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val nodePrimaryColor = MaterialTheme.colorScheme.primary
    val nodeCenterColor = MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val isRtl = layoutDirection == LayoutDirection.Rtl
                val railWidth = 24.dp.toPx()
                val centerX = if (isRtl) size.width - (railWidth / 2f) else railWidth / 2f
                val nodeCenterY = 24.dp.toPx()
                val lineWidth = 2.dp.toPx()
                val outerRadius = 6.dp.toPx() // 12dp diameter
                val innerRadius = 3.dp.toPx()

                // Top segment line (connects from top of card down to node)
                if (!isFirst) {
                    drawLine(
                        color = spineLineColor,
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, nodeCenterY - outerRadius),
                        strokeWidth = lineWidth,
                    )
                }

                // Bottom segment line (connects from node down to bottom of card)
                if (!isLast) {
                    drawLine(
                        color = spineLineColor,
                        start = Offset(centerX, nodeCenterY + outerRadius),
                        end = Offset(centerX, size.height),
                        strokeWidth = lineWidth,
                    )
                }

                // Outer Node Circle (Primary solid circle)
                drawCircle(
                    color = nodePrimaryColor,
                    radius = outerRadius,
                    center = Offset(centerX, nodeCenterY),
                )

                // Inner Hollow Donut Center (Surface background color)
                drawCircle(
                    color = nodeCenterColor,
                    radius = innerRadius,
                    center = Offset(centerX, nodeCenterY),
                )
            },
    ) {
        // Spine Rail spacer (24dp rail + 8dp gap)
        Spacer(modifier = Modifier.width(32.dp))

        // Content Card (takes remaining horizontal space)
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}
