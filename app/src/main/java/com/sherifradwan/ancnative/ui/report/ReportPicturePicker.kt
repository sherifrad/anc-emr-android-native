package com.sherifradwan.ancnative.ui.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordSpacing
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.minimumTouchTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface PictureThumbnailState {
    data object Loading : PictureThumbnailState
    data class Loaded(val bitmap: ImageBitmap) : PictureThumbnailState
    data object Unavailable : PictureThumbnailState
}

/**
 * Modal bottom sheet letting the clinician choose which patient pictures are embedded into
 * the printed report, defaulting to none selected until explicitly checked.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ReportPicturePicker(
    patientId: String,
    candidates: List<PictureCandidate>,
    selectedKeys: Set<String>,
    onToggleKey: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (candidates.isEmpty()) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RecordShapes.Sheet,
        modifier = Modifier.testTag("reportPicturePicker"),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RecordSpacing.L)
                    .padding(bottom = RecordSpacing.M),
                verticalArrangement = Arrangement.spacedBy(RecordSpacing.S),
            ) {
                Text(
                    text = stringResource(R.string.report_picture_picker_title),
                    style = RecordType.SectionTitle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(
                        R.string.report_picture_picker_subtitle,
                        selectedKeys.size,
                        candidates.size,
                    ),
                    style = RecordType.Secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RecordSpacing.S),
                    verticalArrangement = Arrangement.spacedBy(RecordSpacing.S),
                ) {
                    OutlinedButton(
                        onClick = onSelectAll,
                        shape = RecordShapes.Small,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .minimumTouchTarget()
                            .testTag("reportPicturesSelectAll"),
                    ) {
                        Text(
                            text = stringResource(R.string.report_pictures_select_all),
                            style = RecordType.Action,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                        )
                    }
                    OutlinedButton(
                        onClick = onClear,
                        shape = RecordShapes.Small,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .minimumTouchTarget()
                            .testTag("reportPicturesClear"),
                    ) {
                        Text(
                            text = stringResource(R.string.report_pictures_clear),
                            style = RecordType.Action,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                candidates.forEachIndexed { index, candidate ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    ReportPictureCandidateRow(
                        patientId = patientId,
                        candidate = candidate,
                        isSelected = candidate.key in selectedKeys,
                        onToggle = { onToggleKey(candidate.key) },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RecordSpacing.L, vertical = RecordSpacing.M),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RecordShapes.Small,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .minimumTouchTarget()
                        .testTag("reportPicturesDone"),
                ) {
                    Text(
                        text = stringResource(R.string.done),
                        style = RecordType.Action,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportPictureCandidateRow(
    patientId: String,
    candidate: PictureCandidate,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val context = LocalContext.current
    val thumbnailDescription = if (candidate.date.isNotBlank()) {
        stringResource(R.string.report_picture_thumbnail_cd, candidate.sourceLabel, candidate.date)
    } else {
        stringResource(R.string.report_picture_thumbnail_undated_cd, candidate.sourceLabel)
    }
    val thumbnailState by produceState<PictureThumbnailState>(
        initialValue = PictureThumbnailState.Loading,
        key1 = patientId,
        key2 = candidate.key,
        key3 = candidate.location,
    ) {
        value = withContext(Dispatchers.IO) {
            val bitmap = decodePictureBitmap(
                context = context,
                patientId = patientId,
                candidate = candidate,
                maxSide = PICKER_THUMBNAIL_MAX_SIDE_PX,
            )?.asImageBitmap()
            if (bitmap != null) {
                PictureThumbnailState.Loaded(bitmap)
            } else {
                PictureThumbnailState.Unavailable
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .minimumTouchTarget()
            .testTag("reportPictureRow_${candidate.key}")
            .toggleable(
                value = isSelected,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(horizontal = RecordSpacing.L, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RecordSpacing.M),
    ) {
        Surface(
            shape = RecordShapes.Small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(56.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when (val state = thumbnailState) {
                    is PictureThumbnailState.Loaded -> {
                        Image(
                            bitmap = state.bitmap,
                            contentDescription = thumbnailDescription,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    PictureThumbnailState.Unavailable -> {
                        Icon(
                            imageVector = Icons.Outlined.ImageNotSupported,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    PictureThumbnailState.Loading -> Unit
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = candidate.sourceLabel,
                style = RecordType.KeyValue,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = candidate.date.takeIf { it.isNotBlank() }?.let(::ltrIsolate)
                    ?: stringResource(R.string.report_dash),
                style = RecordType.Secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (thumbnailState == PictureThumbnailState.Unavailable) {
                Text(
                    text = stringResource(R.string.report_image_not_available),
                    style = RecordType.FieldLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = null,
        )
    }
}
