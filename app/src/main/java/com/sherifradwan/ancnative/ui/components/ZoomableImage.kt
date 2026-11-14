package com.sherifradwan.ancnative.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R

const val ZOOM_MIN = 1f
const val ZOOM_MAX = 5f

internal fun clampZoom(scale: Float): Float = scale.coerceIn(ZOOM_MIN, ZOOM_MAX)

internal fun clampOffset(offset: Offset, scale: Float, container: Size): Offset {
    if (scale <= 1f) return Offset.Zero
    val maxX = (container.width * (scale - 1f) / 2f).coerceAtLeast(0f)
    val maxY = (container.height * (scale - 1f) / 2f).coerceAtLeast(0f)
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}

internal fun nextZoomOnDoubleTap(current: Float): Float {
    return if (current < 2f) 2.5f else 1f
}

@Composable
fun ZoomableImage(
    bitmap: ImageBitmap,
    contentDescription: String,
    modifier: Modifier = Modifier,
    resetKey: Any? = null,
) {
    var scale by remember(resetKey) { mutableFloatStateOf(1f) }
    var offset by remember(resetKey) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(resetKey) {
        scale = 1f
        offset = Offset.Zero
    }

    val isZoomed = scale > 1f
    val resetLabel = stringResource(R.string.zoom_reset)

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { size ->
                containerSize = Size(size.width.toFloat(), size.height.toFloat())
            }
            .pointerInput(resetKey) {
                detectTapGestures(
                    onDoubleTap = { centroid ->
                        val targetScale = nextZoomOnDoubleTap(scale)
                        if (targetScale > 1f) {
                            scale = targetScale
                            val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                            val diff = center - centroid
                            offset = clampOffset(diff * (targetScale - 1f), targetScale, containerSize)
                        } else {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    }
                )
            }
            .pointerInput(resetKey, isZoomed) {
                if (isZoomed) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = clampZoom(oldScale * zoom)
                        scale = newScale
                        if (newScale > 1f) {
                            val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                            val newOffset = if (zoom != 1f) {
                                (offset + centroid - center) * (newScale / oldScale) - (centroid - center) + pan
                            } else {
                                offset + pan
                            }
                            offset = clampOffset(newOffset, newScale, containerSize)
                        } else {
                            offset = Offset.Zero
                        }
                    }
                } else {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val downCount = event.changes.count { it.pressed }
                            if (downCount >= 2) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid()
                                if (zoom != 1f || pan != Offset.Zero) {
                                    val oldScale = scale
                                    val newScale = clampZoom(oldScale * zoom)
                                    scale = newScale
                                    if (newScale > 1f) {
                                        val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                        val newOffset = if (zoom != 1f) {
                                            (offset + centroid - center) * (newScale / oldScale) - (centroid - center) + pan
                                        } else {
                                            offset + pan
                                        }
                                        offset = clampOffset(newOffset, newScale, containerSize)
                                    } else {
                                        offset = Offset.Zero
                                    }
                                    event.changes.forEach {
                                        if (it.positionChanged()) it.consume()
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )

        if (scale > 1f) {
            Surface(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { this.contentDescription = resetLabel }
                    .testTag("zoomResetButton")
                    .testTag("zoom_reset"),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "1x",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
