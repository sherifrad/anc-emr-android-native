package com.sherifradwan.ancnative.ui.patienthub.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.patienthub.components.AttachmentsSectionCard
import com.sherifradwan.ancnative.ui.patienthub.components.DeliveryPlanningCard
import com.sherifradwan.ancnative.ui.patienthub.components.GestationalDatingCard
import com.sherifradwan.ancnative.ui.patienthub.components.MedicalHistoriesCard
import com.sherifradwan.ancnative.ui.patienthub.components.ObstetricParityCard
import com.sherifradwan.ancnative.ui.patienthub.components.PreviousPregnancyCard
import java.util.UUID

@Composable
fun OverviewTab(
    patient: DemoPatient,
    canEdit: Boolean,
    onEvent: (AncEvent) -> Unit,
    onAddAttachment: () -> Unit,
    onViewAttachment: (DemoAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("overviewTabContent"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Authoritative Gestational Dating Card
        GestationalDatingCard(
            dating = patient.dating,
            onEdit = if (canEdit) {
                {
                    onEvent(
                        AncEvent.StartDatingEditor(
                            patientId = patient.id,
                            editorSessionId = UUID.randomUUID().toString(),
                        ),
                    )
                }
            } else null,
        )

        // 2. Obstetric Parity Card
        ObstetricParityCard(
            patient = patient,
            onAddPregnancy = if (canEdit) {
                {
                    onEvent(
                        AncEvent.StartPregnancyEditor(
                            patientId = patient.id,
                            pregnancyId = null,
                            newPregnancyId = UUID.randomUUID().toString(),
                            editorSessionId = UUID.randomUUID().toString(),
                        ),
                    )
                }
            } else null,
        )

        // 3. Medical, Surgical & Family Histories
        MedicalHistoriesCard(
            patient = patient,
            onEdit = if (canEdit) {
                {
                    onEvent(
                        AncEvent.StartPatientEditor(
                            patientId = patient.id,
                            newPatientId = patient.id,
                            editorSessionId = UUID.randomUUID().toString(),
                        ),
                    )
                }
            } else null,
        )

        // 4. Prior Pregnancies Timeline
        val pregnancies = patient.previousPregnancies
        if (pregnancies.isNotEmpty()) {
            Text(
                text = "Prior Pregnancies (${pregnancies.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            pregnancies.forEach { pregnancy ->
                PreviousPregnancyCard(
                    pregnancy = pregnancy,
                    canEdit = canEdit,
                    onEdit = {
                        onEvent(
                            AncEvent.StartPregnancyEditor(
                                patientId = patient.id,
                                pregnancyId = pregnancy.id,
                                newPregnancyId = pregnancy.id,
                                editorSessionId = UUID.randomUUID().toString(),
                            ),
                        )
                    },
                    onDelete = {
                        onEvent(
                            AncEvent.RequestDelete(
                                patientId = patient.id,
                                type = DeletableType.PREVIOUS_PREGNANCY,
                                recordId = pregnancy.id,
                            ),
                        )
                    },
                )
            }
        }

        // 5. Delivery Planning Card Snapshot
        DeliveryPlanningCard(
            patient = patient,
            onEditPatient = {
                onEvent(
                    AncEvent.StartPatientEditor(
                        patientId = patient.id,
                        newPatientId = patient.id,
                        editorSessionId = UUID.randomUUID().toString(),
                    ),
                )
            },
        )

        // 6. Clinical Documents & Attachments Snapshot
        AttachmentsSectionCard(
            patient = patient,
            onAddAttachment = onAddAttachment,
            onViewAttachment = onViewAttachment,
        )
    }
}
