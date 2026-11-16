package com.sherifradwan.ancnative.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.storage.BoundedBitmap
import com.sherifradwan.ancnative.storage.PatientAttachmentVault
import com.sherifradwan.ancnative.storage.scanPhotoFileName
import com.sherifradwan.ancnative.ui.components.PlainField
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.ZoomableImage
import com.sherifradwan.ancnative.ui.components.ltrIsolate

private data class ViewerBiometryItem(
    val label: String,
    val value: String,
    val isMeasurement: Boolean,
)

@Composable
fun UltrasoundPhotoViewerDialog(
    patientId: String,
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
            val fileName = scanPhotoFileName(currentUriString)
            if (fileName != null) {
                PatientAttachmentVault.resolveAttachmentFile(context, patientId, fileName)
                    ?.let { BoundedBitmap.decodeFile(it.absolutePath, 2048)?.asImageBitmap() }
            } else {
                val uri = Uri.parse(currentUriString)
                if (uri.scheme == "content" || uri.scheme == "file") {
                    BoundedBitmap.decodeStream({ context.contentResolver.openInputStream(uri) }, 2048)?.asImageBitmap()
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .heightIn(max = (LocalConfiguration.current.screenHeightDp - 32).dp)
                .testTag("ultrasoundPhotoViewerDialog"),
            shape = RecordShapes.Large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.ultrasound_viewer_title, ltrIsolate(scan.date)),
                            style = RecordType.CardName,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val subtitleText = if (scan.gestationalAgeWeeks != null) {
                            stringResource(
                                R.string.ultrasound_viewer_photo_of_ga,
                                currentIndex + 1,
                                scan.imageUris.size,
                                ltrIsolate("${scan.gestationalAgeWeeks}+${scan.gestationalAgeDays ?: 0} wks"),
                            )
                        } else {
                            stringResource(
                                R.string.ultrasound_viewer_photo_of,
                                currentIndex + 1,
                                scan.imageUris.size,
                            )
                        }
                        Text(
                            text = subtitleText,
                            style = RecordType.Secondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumTouchTarget().testTag("closePhotoViewer"),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RecordShapes.Small)
                        .background(Color.Black)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RecordShapes.Small),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loadedBitmap != null) {
                        ZoomableImage(
                            bitmap = loadedBitmap,
                            contentDescription = stringResource(R.string.ultrasound_viewer_photo_cd, currentIndex + 1),
                            resetKey = currentIndex,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.image_unavailable),
                            style = RecordType.FieldValue,
                            color = Color.LightGray,
                            modifier = Modifier.testTag("image_unavailable"),
                        )
                    }
                }

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
                            modifier = Modifier.minimumTouchTarget().testTag("prevPhotoButton"),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.previous_photo),
                                tint = if (currentIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clearAndSetSemantics {},
                        ) {
                            scan.imageUris.indices.forEach { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(if (idx == currentIndex) 10.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (idx == currentIndex) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                        ),
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (currentIndex < scan.imageUris.lastIndex) currentIndex++
                            },
                            enabled = currentIndex < scan.imageUris.lastIndex,
                            modifier = Modifier.minimumTouchTarget().testTag("nextPhotoButton"),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.next_photo),
                                tint = if (currentIndex < scan.imageUris.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }

                val biometrics = buildList {
                    if (!scan.fetalHeartRate.isNullOrBlank()) {
                        add(
                            ViewerBiometryItem(
                                label = stringResource(R.string.biometry_fhr),
                                value = ltrIsolate(stringResource(R.string.unit_bpm, scan.fetalHeartRate)),
                                isMeasurement = true,
                            ),
                        )
                    }
                    if (!scan.estimatedFetalWeight.isNullOrBlank()) {
                        add(
                            ViewerBiometryItem(
                                label = stringResource(R.string.biometry_efw),
                                value = ltrIsolate(stringResource(R.string.unit_g, scan.estimatedFetalWeight)),
                                isMeasurement = true,
                            ),
                        )
                    }
                    if (!scan.biometryBpd.isNullOrBlank()) {
                        add(
                            ViewerBiometryItem(
                                label = stringResource(R.string.biometry_bpd),
                                value = ltrIsolate(stringResource(R.string.unit_mm, scan.biometryBpd)),
                                isMeasurement = true,
                            ),
                        )
                    }
                    if (!scan.biometryFl.isNullOrBlank()) {
                        add(
                            ViewerBiometryItem(
                                label = stringResource(R.string.biometry_fl),
                                value = ltrIsolate(stringResource(R.string.unit_mm, scan.biometryFl)),
                                isMeasurement = true,
                            ),
                        )
                    }
                    if (!scan.presentation.isNullOrBlank()) {
                        add(
                            ViewerBiometryItem(
                                label = stringResource(R.string.fetal_presentation),
                                value = scan.presentation,
                                isMeasurement = false,
                            ),
                        )
                    }
                }

                if (biometrics.isNotEmpty()) {
                    val columns = if (LocalDensity.current.fontScale > 1.3f) 2 else 3
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        biometrics.chunked(columns).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                rowItems.forEach { item ->
                                    PlainField(
                                        label = item.label,
                                        value = item.value,
                                        modifier = Modifier.weight(1f),
                                        emphasis = item.isMeasurement,
                                    )
                                }
                                repeat(columns - rowItems.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RecordShapes.Small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .minimumTouchTarget()
                        .testTag("dismissPhotoViewer"),
                ) {
                    Text(
                        text = stringResource(R.string.close),
                        style = RecordType.Action,
                    )
                }
            }
        }
    }
}
