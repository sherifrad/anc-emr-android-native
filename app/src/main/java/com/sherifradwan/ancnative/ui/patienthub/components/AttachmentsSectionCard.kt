package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.storage.AttachmentCategories
import com.sherifradwan.ancnative.storage.AttachmentSyncManager
import com.sherifradwan.ancnative.storage.BoundedBitmap
import com.sherifradwan.ancnative.storage.PatientAttachmentVault
import com.sherifradwan.ancnative.ui.attachmentDisplayTitle
import com.sherifradwan.ancnative.ui.minimumTouchTarget

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
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("attachmentsSectionCard"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Clinical Documents & Attachments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (patient.status != "archived") {
                    Button(
                        onClick = onAddAttachment,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.minimumTouchTarget().testTag("addAttachmentBtn"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Attachment", maxLines = 1, softWrap = false)
                    }
                }
            }

            if (totalCount == 0) {
                Text(
                    text = "No saved attachments or clinical images",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                var selectedFilter by remember { mutableStateOf("ALL") }
                val filteredAttachments = remember(patient.attachments, selectedFilter) {
                    if (selectedFilter == "ALL") patient.attachments
                    else patient.attachments.filter { it.category == selectedFilter }
                }

                Text(
                    text = "$totalCount saved files (${AttachmentSyncManager.formatFileSize(totalBytes)})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                )

                // Category Filter Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All ($totalCount)") },
                    )
                    FilterChip(
                        selected = selectedFilter == AttachmentCategories.ULTRASOUND,
                        onClick = { selectedFilter = AttachmentCategories.ULTRASOUND },
                        label = { Text("Ultrasound (${patient.attachments.count { it.category == AttachmentCategories.ULTRASOUND }})") },
                    )
                    FilterChip(
                        selected = selectedFilter == AttachmentCategories.LAB,
                        onClick = { selectedFilter = AttachmentCategories.LAB },
                        label = { Text("Labs (${patient.attachments.count { it.category == AttachmentCategories.LAB }})") },
                    )
                    FilterChip(
                        selected = selectedFilter == AttachmentCategories.PRESCRIPTION,
                        onClick = { selectedFilter = AttachmentCategories.PRESCRIPTION },
                        label = { Text("Prescriptions (${patient.attachments.count { it.category == AttachmentCategories.PRESCRIPTION }})") },
                    )
                    FilterChip(
                        selected = selectedFilter == AttachmentCategories.CLINICAL_PHOTO,
                        onClick = { selectedFilter = AttachmentCategories.CLINICAL_PHOTO },
                        label = { Text("Clinical Photos (${patient.attachments.count { it.category == AttachmentCategories.CLINICAL_PHOTO }})") },
                    )
                }

                if (filteredAttachments.isEmpty()) {
                    Text(
                        text = "No attachments in this selected category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredAttachments.forEach { attachment ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onViewAttachment(attachment) }
                                    .testTag("attachmentCard-${attachment.id}"),
                                tonalElevation = 1.dp,
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    // Thumbnail preview
                                    val thumbBitmap = remember(attachment.id) {
                                        runCatching {
                                            val file = PatientAttachmentVault.resolveAttachmentFile(
                                                context = context,
                                                patientId = patient.id,
                                                fileName = attachment.fileName,
                                            )
                                            if (file != null && file.exists()) {
                                                BoundedBitmap.decodeFile(file.absolutePath, 2048)?.asImageBitmap()
                                            } else null
                                        }.getOrNull()
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.DarkGray),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (thumbBitmap != null) {
                                            androidx.compose.foundation.Image(
                                                bitmap = thumbBitmap,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Text("—", fontSize = 24.sp)
                                        }
                                    }

                                    // Details
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = attachmentDisplayTitle(
                                                title = attachment.title,
                                                category = AttachmentCategories.localizedName(attachment.category),
                                                capturedAt = attachment.capturedAt,
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                            ) {
                                                Text(
                                                    text = AttachmentCategories.localizedName(attachment.category),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                )
                                            }
                                            Text(
                                                text = AttachmentSyncManager.formatFileSize(attachment.fileSizeBytes),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.VerifiedUser,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(12.dp),
                                            )
                                            Text(
                                                text = "Saved on this phone · ${attachment.capturedAt.take(10)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
