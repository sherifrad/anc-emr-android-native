package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.state.SavedNotice
import com.sherifradwan.ancnative.state.WorkspaceSection

object PreviousPregnancyDeleteAdapter : DeletableRecordAdapter<DemoPreviousPregnancy> {
    override val type: DeletableType = DeletableType.PREVIOUS_PREGNANCY
    override val fingerprintTag: String = "PRV"
    override val deleteReceiptKind: String = "PREVIOUS_PREGNANCY_DELETE"
    override val restoreReceiptKind: String = "PREVIOUS_PREGNANCY_RESTORE"
    override val section: WorkspaceSection = WorkspaceSection.OBSTETRIC_HISTORY
    override val deletedNotice: SavedNotice = SavedNotice.PREVIOUS_PREGNANCY_DELETED
    override val restoredNotice: SavedNotice = SavedNotice.PREVIOUS_PREGNANCY_RESTORED
    override val saveUndoneNotice: SavedNotice = SavedNotice.PREVIOUS_PREGNANCY_SAVE_UNDONE

    override fun label(record: DemoPreviousPregnancy): String =
        record.dateText?.takeIf { it.isNotBlank() }
            ?: record.outcomeText?.takeIf { it.isNotBlank() }
            ?: record.id

    override fun canonical(record: DemoPreviousPregnancy): String =
        "${record.patientId}|${record.id}|${record.modelVersion}|${record.dateText}|${record.outcomeText}|${record.pregnancyType}|${record.otherDetails}|${record.placeAndDoctor}|${record.deliveryBy}|${record.outcome}|${record.birthMethod}|${record.lossManagement}|${record.terminationMethod}|${record.provenanceKind}|${record.clinicianId}|${record.recordedAt}|${record.updatedAt}"

    override suspend fun load(dao: RecordsDao, id: String): DemoPreviousPregnancy? =
        dao.previousPregnancy(id)?.let {
            DemoPreviousPregnancy(
                id = it.id,
                patientId = it.patientId,
                modelVersion = it.modelVersion,
                dateText = it.dateText,
                outcomeText = it.outcomeText,
                pregnancyType = it.pregnancyType,
                otherDetails = it.otherDetails,
                placeAndDoctor = it.placeAndDoctor,
                deliveryBy = it.deliveryBy,
                outcome = it.outcome,
                birthMethod = it.birthMethod,
                lossManagement = it.lossManagement,
                terminationMethod = it.terminationMethod,
                provenanceKind = it.provenanceKind,
                clinicianId = it.clinicianId,
                recordedAt = it.recordedAt,
                updatedAt = it.updatedAt,
                revision = it.revision,
            )
        }

    override suspend fun deleteRow(dao: RecordsDao, id: String, patientId: String, expectedRevision: Long): Int =
        dao.deletePreviousPregnancy(id, patientId, expectedRevision)

    override suspend fun insertRow(dao: RecordsDao, record: DemoPreviousPregnancy, revision: Long) {
        dao.insertPreviousPregnancy(
            PreviousPregnancyEntity(
                id = record.id,
                patientId = record.patientId,
                modelVersion = record.modelVersion,
                dateText = record.dateText,
                outcomeText = record.outcomeText,
                pregnancyType = record.pregnancyType,
                otherDetails = record.otherDetails,
                placeAndDoctor = record.placeAndDoctor,
                deliveryBy = record.deliveryBy,
                outcome = record.outcome,
                birthMethod = record.birthMethod,
                lossManagement = record.lossManagement,
                terminationMethod = record.terminationMethod,
                provenanceKind = record.provenanceKind,
                clinicianId = record.clinicianId,
                recordedAt = record.recordedAt,
                updatedAt = record.updatedAt,
                revision = revision,
            ),
        )
    }

    override fun record(patient: DemoPatient, id: String): DemoPreviousPregnancy? =
        patient.previousPregnancies.firstOrNull { it.id == id }
}
