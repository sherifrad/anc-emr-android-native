package com.sherifradwan.ancnative.ui

import com.sherifradwan.ancnative.ui.components.ZoomableImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.storage.AttachmentCategories
import com.sherifradwan.ancnative.storage.AttachmentSyncManager
import com.sherifradwan.ancnative.storage.BoundedBitmap
import com.sherifradwan.ancnative.storage.PatientAttachmentVault
import com.sherifradwan.ancnative.ui.components.PatientNameText
import com.sherifradwan.ancnative.ui.components.RecordShapes
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.StatusTone
import com.sherifradwan.ancnative.ui.components.StatusWord
import com.sherifradwan.ancnative.ui.components.ltrIsolate

@Composable
fun ClinicalAttachmentViewerDialog(
    patient: DemoPatient,
    attachment: DemoAttachment,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current

    val loadedBitmap = remember(attachment.id) {
        runCatching {
            val file = PatientAttachmentVault.resolveAttachmentFile(
                context = context,
                patientId = patient.id,
                fileName = attachment.fileName,
            )
            if (file != null && file.exists()) {
                BoundedBitmap.decodeFile(file.absolutePath, 2048)?.asImageBitmap()
            } else {
                null
            }
        }.getOrNull()
    }

    val isIntegrityVerified = remember(attachment.id) {
        val file = PatientAttachmentVault.resolveAttachmentFile(context, patient.id, attachment.fileName)
        if (file != null) {
            PatientAttachmentVault.verifyIntegrity(file, attachment.sha256Hash)
        } else false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 16.dp)
                .testTag("clinicalAttachmentViewerDialog"),
            shape = RecordShapes.Large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            // Scrolls so the integrity word, details and hash stay readable when large text outgrows a short window.
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
                        PatientNameText(
                            name = patient.displayName,
                            style = RecordType.CardName,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = AttachmentCategories.localizedName(attachment.category) +
                                (attachment.title?.trim()?.takeIf(String::isNotEmpty)?.let { ", $it" } ?: ""),
                            style = RecordType.Secondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            // The shared DeleteConfirmDialog owns the confirmation: this viewer must not ask twice,
                            // and it stays open until the record is really gone, so Cancel returns to the image.
                            onClick = onDelete,
                            enabled = canDelete,
                            modifier = Modifier.minimumTouchTarget().testTag("deleteAttachmentBtn"),
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.attachment_delete_action),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.minimumTouchTarget().testTag("dismissAttachmentBtn"),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RecordShapes.Small)
                        .background(Color.Black)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RecordShapes.Small),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loadedBitmap != null) {
                        ZoomableImage(
                            bitmap = loadedBitmap,
                            contentDescription = attachmentDisplayTitle(
                                attachment.title,
                                AttachmentCategories.localizedName(attachment.category),
                                attachment.capturedAt,
                            ),
                            resetKey = attachment.id,
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

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    StatusWord(
                        text = stringResource(
                            if (isIntegrityVerified) R.string.attachment_integrity_verified else R.string.attachment_vault_protected,
                        ),
                        tone = if (isIntegrityVerified) StatusTone.Neutral else StatusTone.Moderate,
                    )
                    Text(
                        text = stringResource(
                            R.string.attachment_size_format,
                            AttachmentSyncManager.formatFileSize(attachment.fileSizeBytes),
                        ),
                        style = RecordType.Secondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.attachment_saved_at, ltrIsolate(attachment.capturedAt)),
                        style = RecordType.Secondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.attachment_sha256_label, attachment.sha256Hash),
                        style = RecordType.Tag.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
