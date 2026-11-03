package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.SavedNotice
import com.sherifradwan.ancnative.state.WorkspaceSection

object MedicationDeleteAdapter : DeletableRecordAdapter<DemoMedicationRecord> {
    override val type: DeletableType = DeletableType.MEDICATION
    override val fingerprintTag: String = "MED"
    override val deleteReceiptKind: String = "MEDICATION_DELETE"
    override val restoreReceiptKind: String = "MEDICATION_RESTORE"
    override val section: WorkspaceSection = WorkspaceSection.MEDICATIONS
    override val deletedNotice: SavedNotice = SavedNotice.MEDICATIONS
    override val restoredNotice: SavedNotice = SavedNotice.MEDICATIONS
    override val saveUndoneNotice: SavedNotice = SavedNotice.MEDICATIONS

    override fun label(record: DemoMedicationRecord): String = record.name

    override fun canonical(record: DemoMedicationRecord): String =
        "${record.patientId}|${record.id}|${record.name}|${record.dosage}|${record.route}|${record.frequency}|${record.startDate}|${record.endDate}|${record.status}|${record.notes}|${record.isProphylaxis}|${record.isNonCompliant}|${record.adverseNotes}"

    override suspend fun load(dao: RecordsDao, id: String): DemoMedicationRecord? =
        dao.medication(id)?.let {
            DemoMedicationRecord(
                id = it.id,
                patientId = it.patientId,
                name = it.name,
                dosage = it.dosage,
                route = it.route,
                frequency = it.frequency,
                startDate = it.startDate,
                endDate = it.endDate,
                status = it.status,
                notes = it.notes,
                isProphylaxis = it.isProphylaxis,
                isNonCompliant = it.isNonCompliant,
                adverseNotes = it.adverseEffects,
                revision = it.revision,
            )
        }

    override suspend fun deleteRow(dao: RecordsDao, id: String, patientId: String, expectedRevision: Long): Int =
        dao.deleteMedication(id, patientId, expectedRevision)

    override suspend fun insertRow(dao: RecordsDao, record: DemoMedicationRecord, revision: Long) {
        dao.insertMedication(
            MedicationEntity(
                id = record.id,
                patientId = record.patientId,
                name = record.name,
                dosage = record.dosage,
                route = record.route,
                frequency = record.frequency,
                startDate = record.startDate,
                endDate = record.endDate,
                status = record.status,
                notes = record.notes,
                isProphylaxis = record.isProphylaxis,
                isNonCompliant = record.isNonCompliant,
                adverseEffects = record.adverseNotes,
                revision = revision,
            ),
        )
    }

    override fun record(patient: DemoPatient, id: String): DemoMedicationRecord? =
        patient.medications.firstOrNull { it.id == id }
}
