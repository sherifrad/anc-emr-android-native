package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.state.SavedNotice
import com.sherifradwan.ancnative.state.WorkspaceSection

object VisitDeleteAdapter : DeletableRecordAdapter<DemoVisit> {
    override val type: DeletableType = DeletableType.VISIT
    override val fingerprintTag: String = "VIS"
    override val deleteReceiptKind: String = "VISIT_DELETE"
    override val restoreReceiptKind: String = "VISIT_RESTORE"
    override val section: WorkspaceSection = WorkspaceSection.VISITS
    override val deletedNotice: SavedNotice = SavedNotice.VISIT_DELETED
    override val restoredNotice: SavedNotice = SavedNotice.VISIT_RESTORED
    override val saveUndoneNotice: SavedNotice = SavedNotice.VISIT_SAVE_UNDONE

    override fun label(record: DemoVisit): String = record.date

    override fun canonical(record: DemoVisit): String =
        "${record.patientId}|${record.id}|${record.date}|${record.bloodPressure}|${record.weight}|${record.findings}|${record.notes}|${record.linkedUltrasoundId}"

    override suspend fun load(dao: RecordsDao, id: String): DemoVisit? =
        dao.visit(id)?.let {
            DemoVisit(
                id = it.id,
                patientId = it.patientId,
                date = it.date,
                bloodPressure = it.bloodPressure,
                weight = it.weight,
                findings = it.findings,
                notes = it.notes,
                revision = it.revision.toLong(),
                isSyntheticEdit = it.isSyntheticEdit,
                linkedUltrasoundId = it.linkedUltrasoundId,
            )
        }

    override suspend fun deleteRow(dao: RecordsDao, id: String, patientId: String, expectedRevision: Long): Int {
        dao.deleteSaveReceiptsForVisit(id)
        val deleted = dao.deleteVisit(id, patientId, expectedRevision)
        if (deleted != 1) {
            throw RecordsRepository.ClinicalCommandAbort(ClinicalCommandFailure.STALE_REVISION)
        }
        return deleted
    }

    override suspend fun insertRow(dao: RecordsDao, record: DemoVisit, revision: Long) {
        dao.insertVisit(
            VisitEntity(
                id = record.id,
                patientId = record.patientId,
                date = record.date,
                bloodPressure = record.bloodPressure,
                weight = record.weight,
                findings = record.findings,
                notes = record.notes,
                revision = revision.toInt(),
                isSyntheticEdit = record.isSyntheticEdit,
                linkedUltrasoundId = record.linkedUltrasoundId,
            ),
        )
    }

    override fun record(patient: DemoPatient, id: String): DemoVisit? =
        patient.visits.firstOrNull { it.id == id }
}
