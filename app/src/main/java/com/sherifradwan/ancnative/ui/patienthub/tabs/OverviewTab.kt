package com.sherifradwan.ancnative.ui.patienthub.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.storage.DeletableType
import com.sherifradwan.ancnative.ui.patienthub.components.AttachmentsSectionCard
import com.sherifradwan.ancnative.ui.patienthub.components.DatingSection
import com.sherifradwan.ancnative.ui.patienthub.components.DeliveryPlanningCard
import com.sherifradwan.ancnative.ui.patienthub.components.MedicalHistoriesSection
import com.sherifradwan.ancnative.ui.patienthub.components.ParityPregnanciesSection
import java.util.UUID

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        thickness = 0.75.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
fun OverviewTab(
    patient: DemoPatient,
    canEdit: Boolean,
    onEvent: (AncEvent) -> Unit,
    onAddAttachment: () -> Unit,
    onViewAttachment: (DemoAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun startPatientEditor() = onEvent(
        AncEvent.StartPatientEditor(
            patientId = patient.id,
            newPatientId = patient.id,
            editorSessionId = UUID.randomUUID().toString(),
        ),
    )

    fun startPregnancyEditor(pregnancyId: String?) = onEvent(
        AncEvent.StartPregnancyEditor(
            patientId = patient.id,
            pregnancyId = pregnancyId,
            newPregnancyId = pregnancyId ?: UUID.randomUUID().toString(),
            editorSessionId = UUID.randomUUID().toString(),
        ),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp)
            .testTag("overviewTabContent"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                DatingSection(
                    patient = patient,
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
                SectionDivider()
                ParityPregnanciesSection(
                    patient = patient,
                    onAddPregnancy = if (canEdit) ({ startPregnancyEditor(null) }) else null,
                    onEditPregnancy = if (canEdit) ({ startPregnancyEditor(it) }) else null,
                    onDeletePregnancy = if (canEdit) {
                        {
                            onEvent(
                                AncEvent.RequestDelete(
                                    patientId = patient.id,
                                    type = DeletableType.PREVIOUS_PREGNANCY,
                                    recordId = it,
                                ),
                            )
                        }
                    } else null,
                )
                SectionDivider()
                MedicalHistoriesSection(
                    patient = patient,
                    onEdit = if (canEdit) ({ startPatientEditor() }) else null,
                )
            }
        }

        DeliveryPlanningCard(
            patient = patient,
            canEdit = canEdit,
            onEditPatient = { startPatientEditor() },
        )

        AttachmentsSectionCard(
            patient = patient,
            onAddAttachment = onAddAttachment,
            onViewAttachment = onViewAttachment,
        )
    }
}
