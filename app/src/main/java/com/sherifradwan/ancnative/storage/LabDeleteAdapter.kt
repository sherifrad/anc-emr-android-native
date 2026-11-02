package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.SavedNotice
import com.sherifradwan.ancnative.state.WorkspaceSection

object LabDeleteAdapter : DeletableRecordAdapter<DemoLabInvestigation> {
    override val type: DeletableType = DeletableType.LAB
    override val fingerprintTag: String = "LAB"
    override val deleteReceiptKind: String = "LAB_DELETE"
    override val restoreReceiptKind: String = "LAB_RESTORE"
    override val section: WorkspaceSection = WorkspaceSection.LABORATORY
    override val deletedNotice: SavedNotice = SavedNotice.LAB_DELETED
    override val restoredNotice: SavedNotice = SavedNotice.LAB_RESTORED
    override val saveUndoneNotice: SavedNotice = SavedNotice.LAB_SAVE_UNDONE

    override fun label(record: DemoLabInvestigation): String = record.testName

    override fun canonical(record: DemoLabInvestigation): String =
        "${record.patientId}|${record.id}|${record.date}|${record.testCategory}|${record.testName}|${record.result}|${record.unit}|${record.referenceRange}|${record.isAbnormal}|${record.notes}|${record.labName}"

    override suspend fun load(dao: RecordsDao, id: String): DemoLabInvestigation? =
        dao.labInvestigation(id)?.let {
            DemoLabInvestigation(
                id = it.id,
                patientId = it.patientId,
                date = it.date,
                testCategory = it.testCategory,
                testName = it.testName,
                result = it.result,
                unit = it.unit,
                referenceRange = it.referenceRange,
                isAbnormal = it.isAbnormal,
                notes = it.notes,
                labName = it.labName,
                revision = it.revision,
            )
        }

    override suspend fun deleteRow(dao: RecordsDao, id: String, patientId: String, expectedRevision: Long): Int =
        dao.deleteLabInvestigation(id, patientId, expectedRevision)

    override suspend fun insertRow(dao: RecordsDao, record: DemoLabInvestigation, revision: Long) {
        dao.insertLabInvestigation(
            LabInvestigationEntity(
                id = record.id,
                patientId = record.patientId,
                date = record.date,
                testCategory = record.testCategory,
                testName = record.testName,
                result = record.result,
                unit = record.unit,
                referenceRange = record.referenceRange,
                isAbnormal = record.isAbnormal,
                notes = record.notes,
                labName = record.labName,
                revision = revision,
            ),
        )
    }

    override fun record(patient: DemoPatient, id: String): DemoLabInvestigation? =
        patient.labInvestigations.firstOrNull { it.id == id }
}
