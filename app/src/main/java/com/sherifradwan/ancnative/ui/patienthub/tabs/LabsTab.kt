package com.sherifradwan.ancnative.ui.patienthub.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.patienthub.components.LabReportItem
import com.sherifradwan.ancnative.ui.patienthub.model.groupLabsIntoReports
import java.util.UUID

@Composable
fun LabsTab(
    patient: DemoPatient,
    canEdit: Boolean,
    onEvent: (AncEvent) -> Unit,
    onAddInvestigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reports = remember(patient.labInvestigations) {
        groupLabsIntoReports(patient.labInvestigations)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("labsTabContent"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top Action: + Add Investigation
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Laboratory Reports (${reports.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (canEdit && patient.status != "archived") {
                    Button(
                        onClick = onAddInvestigation,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("addInvestigationBtn"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Test", fontWeight = FontWeight.Bold, softWrap = false)
                    }
                }
            }
        }

        if (reports.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No records",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            items(
                items = reports,
                key = { "${it.date}-${it.labName}" },
            ) { report ->
                LabReportItem(
                    report = report,
                    patient = patient,
                    canEdit = canEdit,
                    onEditTest = { test ->
                        onEvent(
                            AncEvent.StartLabEditor(
                                patientId = patient.id,
                                labId = test.id,
                                newLabId = test.id,
                                editorSessionId = UUID.randomUUID().toString(),
                            ),
                        )
                    },
                    onDeleteTest = { testId ->
                        onEvent(AncEvent.RequestDelete(patient.id, DeletableType.LAB, testId))
                    },
                    onAddTestToReport = onAddInvestigation,
                )
            }
        }
    }
}
