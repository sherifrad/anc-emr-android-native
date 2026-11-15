package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.storage.AttachmentCategories
import com.sherifradwan.ancnative.storage.AttachmentSyncManager
import com.sherifradwan.ancnative.storage.BoundedBitmap
import com.sherifradwan.ancnative.storage.PatientAttachmentVault
import com.sherifradwan.ancnative.ui.attachmentDisplayTitle
import com.sherifradwan.ancnative.ui.components.FieldFamilies
import com.sherifradwan.ancnative.ui.components.RecordFilterChip
import com.sherifradwan.ancnative.ui.components.RecordSectionCard
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.SectionTextAction
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttachmentsSectionCard(
    patient: DemoPatient,
    modifier: Modifier = Modifier,
    onAddAttachment: () -> Unit,
    onViewAttachment: (DemoAttachment) -> Unit,
) {
    val totalCount = patient.attachments.size
    val totalBytes = remember(patient.attachments) { patient.attachments.sumOf { it.fileSizeBytes } }
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    RecordSectionCard(
        title = stringResource(R.string.attachments_title),
        family = FieldFamilies.patient(),
        modifier = modifier.testTag("attachmentsSectionCard"),
        action = if (patient.status != "archived") {
            {
                SectionTextAction(
                    text = stringResource(R.string.attachment_add_action),
                    contentDescription = stringResource(R.string.attachment_add_description),
                    onClick = onAddAttachment,
                    modifier = Modifier.testTag("addAttachmentBtn"),
                )
            }
        } else {
            null
        },
    ) {
        if (totalCount == 0) {
            Text(
                text = stringResource(R.string.attachments_empty),
                style = RecordType.FieldValue,
                color = onSurfaceVariant,
            )
        } else {
            var selectedFilter by remember { mutableStateOf("ALL") }
            val filteredAttachments = remember(patient.attachments, selectedFilter) {
                if (selectedFilter == "ALL") patient.attachments
                else patient.attachments.filter { it.category == selectedFilter }
            }

            Text(
                text = pluralStringResource(
                    R.plurals.attachments_summary,
                    totalCount,
                    totalCount,
                    AttachmentSyncManager.formatFileSize(totalBytes),
                ),
                style = RecordType.Secondary,
                color = onSurfaceVariant,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RecordFilterChip(
                    selected = selectedFilter == "ALL",
                    label = stringResource(R.string.attachment_filter_all, totalCount),
                    onClick = { selectedFilter = "ALL" },
                    tag = "attachmentFilter-ALL",
                )
                RecordFilterChip(
                    selected = selectedFilter == AttachmentCategories.ULTRASOUND,
                    label = stringResource(
                        R.string.attachment_filter_ultrasound,
                        patient.attachments.count { it.category == AttachmentCategories.ULTRASOUND },
                    ),
                    onClick = { selectedFilter = AttachmentCategories.ULTRASOUND },
                    tag = "attachmentFilter-${AttachmentCategories.ULTRASOUND}",
                )
                RecordFilterChip(
                    selected = selectedFilter == AttachmentCategories.LAB,
                    label = stringResource(
                        R.string.attachment_filter_labs,
                        patient.attachments.count { it.category == AttachmentCategories.LAB },
                    ),
                    onClick = { selectedFilter = AttachmentCategories.LAB },
                    tag = "attachmentFilter-${AttachmentCategories.LAB}",
                )
                RecordFilterChip(
                    selected = selectedFilter == AttachmentCategories.PRESCRIPTION,
                    label = stringResource(
                        R.string.attachment_filter_prescriptions,
                        patient.attachments.count { it.category == AttachmentCategories.PRESCRIPTION },
                    ),
                    onClick = { selectedFilter = AttachmentCategories.PRESCRIPTION },
                    tag = "attachmentFilter-${AttachmentCategories.PRESCRIPTION}",
                )
                RecordFilterChip(
                    selected = selectedFilter == AttachmentCategories.CLINICAL_PHOTO,
                    label = stringResource(
                        R.string.attachment_filter_photos,
                        patient.attachments.count { it.category == AttachmentCategories.CLINICAL_PHOTO },
                    ),
                    onClick = { selectedFilter = AttachmentCategories.CLINICAL_PHOTO },
                    tag = "attachmentFilter-${AttachmentCategories.CLINICAL_PHOTO}",
                )
            }

            if (filteredAttachments.isEmpty()) {
                Text(
                    text = stringResource(R.string.attachments_filter_empty),
                    style = RecordType.FieldValue,
                    color = onSurfaceVariant,
                )
            } else {
                Column {
                    filteredAttachments.forEachIndexed { index, attachment ->
                        AttachmentRow(
                            patientId = patient.id,
                            attachment = attachment,
                            showDivider = index > 0,
                            onClick = { onViewAttachment(attachment) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentRow(
    patientId: String,
    attachment: DemoAttachment,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val categoryName = AttachmentCategories.localizedName(attachment.category)

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showDivider) HorizontalDivider(color = colors.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("attachmentCard-${attachment.id}")
                .clickable(onClickLabel = stringResource(R.string.attachment_open), onClick = onClick)
                .heightIn(min = 72.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val thumbBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = attachment.id) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        val file = PatientAttachmentVault.resolveAttachmentFile(
                            context = context,
                            patientId = patientId,
                            fileName = attachment.fileName,
                        )
                        if (file != null && file.exists()) {
                            BoundedBitmap.decodeFile(file.absolutePath, 2048)?.asImageBitmap()
                        } else null
                    }.getOrNull()
                }
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RecordShapes.Small)
                    .background(colors.surfaceContainerHigh),
            ) {
                thumbBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = attachmentDisplayTitle(
                        title = attachment.title,
                        category = categoryName,
                        capturedAt = attachment.capturedAt,
                    ),
                    style = RecordType.KeyValue,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.attachment_row_detail,
                        categoryName,
                        AttachmentSyncManager.formatFileSize(attachment.fileSizeBytes),
                    ),
                    style = RecordType.Secondary,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.attachment_saved_on_phone, ltrIsolate(attachment.capturedAt.take(10))),
                    style = RecordType.Secondary,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
