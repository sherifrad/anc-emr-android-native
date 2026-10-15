package com.sherifradwan.ancnative.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sherifradwan.ancnative.model.DemoUltrasoundScan

@Composable
fun UltrasoundPhotoViewerDialog(
    scan: DemoUltrasoundScan,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
) {
    if (scan.imageUris.isEmpty()) return

    var currentIndex by remember(scan.id) {
        mutableIntStateOf(initialIndex.coerceIn(0, scan.imageUris.lastIndex))
    }
    val currentUriString = scan.imageUris.getOrNull(currentIndex) ?: ""

    val context = LocalContext.current
    val loadedBitmap = remember(currentUriString) {
        runCatching {
            val uri = Uri.parse(currentUriString)
            if (uri.scheme == "content" || uri.scheme == "file") {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }?.asImageBitmap()
            } else {
                null
            }
        }.getOrNull()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("ultrasoundPhotoViewerDialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header: Scan date, index, and dismiss button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Ultrasound Scan — ${scan.date}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val gaText = if (scan.gestationalAgeWeeks != null) {
                                " · ${scan.gestationalAgeWeeks}+${scan.gestationalAgeDays ?: 0} wks"
                            } else ""
                            Text(
                                text = "Photo ${currentIndex + 1} of ${scan.imageUris.size}$gaText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("closePhotoViewer"),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close photo viewer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Viewport Box: Either real bitmap or high-contrast simulated clinical ultrasound frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF090D14))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loadedBitmap != null) {
                        Image(
                            bitmap = loadedBitmap,
                            contentDescription = "Ultrasound Scan Photo ${currentIndex + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        // High-fidelity clinical ultrasound console visualization
                        UltrasoundMonitorCanvas(
                            scan = scan,
                            uriString = currentUriString,
                            photoIndex = currentIndex + 1,
                            totalPhotos = scan.imageUris.size,
                        )
                    }
                }

                // Pager Controls: If multiple images exist
                if (scan.imageUris.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                if (currentIndex > 0) currentIndex--
                            },
                            enabled = currentIndex > 0,
                            modifier = Modifier.testTag("prevPhotoButton"),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Photo",
                                tint = if (currentIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            )
                        }

                        // Dot indicators
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            scan.imageUris.indices.forEach { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(if (idx == currentIndex) 10.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (idx == currentIndex) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                        )
                                        .clickable { currentIndex = idx },
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (currentIndex < scan.imageUris.lastIndex) currentIndex++
                            },
                            enabled = currentIndex < scan.imageUris.lastIndex,
                            modifier = Modifier.testTag("nextPhotoButton"),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Photo",
                                tint = if (currentIndex < scan.imageUris.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }

                // Biometrics Summary Pills
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!scan.fetalHeartRate.isNullOrBlank()) {
                        FactBadge("💓 FHR: ${scan.fetalHeartRate} bpm")
                    }
                    if (!scan.estimatedFetalWeight.isNullOrBlank()) {
                        FactBadge("⚖️ EFW: ${scan.estimatedFetalWeight} g")
                    }
                    if (!scan.biometryBpd.isNullOrBlank()) {
                        FactBadge("BPD: ${scan.biometryBpd} mm")
                    }
                    if (!scan.biometryFl.isNullOrBlank()) {
                        FactBadge("FL: ${scan.biometryFl} mm")
                    }
                    if (!scan.presentation.isNullOrBlank()) {
                        FactBadge("Presentation: ${scan.presentation}")
                    }
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismissPhotoViewer"),
                ) {
                    Text("Close Viewer", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FactBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun UltrasoundMonitorCanvas(
    scan: DemoUltrasoundScan,
    uriString: String,
    photoIndex: Int,
    totalPhotos: Int,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Sector fan geometry
            val apexX = width / 2f
            val apexY = 28f
            val maxRadius = height - 40f
            val sectorColor = Color(0xFF1E293B)
            val gridColor = Color(0xFF162030)

            // Draw concentric range rings (calipers)
            for (step in 1..4) {
                val r = maxRadius * (step / 4f)
                drawArc(
                    color = gridColor,
                    startAngle = 55f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(apexX - r, apexY - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(width = 1.5f),
                )
            }

            // Draw sector boundary rays
            drawLine(
                color = sectorColor,
                start = Offset(apexX, apexY),
                end = Offset(apexX - maxRadius * 0.57f, apexY + maxRadius * 0.82f),
                strokeWidth = 1.5f,
            )
            drawLine(
                color = sectorColor,
                start = Offset(apexX, apexY),
                end = Offset(apexX + maxRadius * 0.57f, apexY + maxRadius * 0.82f),
                strokeWidth = 1.5f,
            )
        }

        // Ultrasound HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Upper HUD telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "ANC ULTRASOUND · 3.5MHz C-PROBE\nDEPTH: 15cm · TIS: 0.2 · MI: 1.1",
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                )
                Text(
                    text = "GAIN: 68dB\n2D / B-MODE\nFPS: 28",
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                )
            }

            // Center: Graphic indicator & Image tag
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = "CLINICAL ULTRASOUND CAPTURE",
                    color = Color(0xFFE2E8F0),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                val fileName = uriString.substringAfterLast("/")
                Text(
                    text = fileName,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
            }

            // Lower HUD telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "EXAM: ${scan.date}\n${scan.presentation?.uppercase() ?: "CEPHALIC"}",
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E293B),
                ) {
                    Text(
                        text = "FRAME $photoIndex / $totalPhotos",
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
