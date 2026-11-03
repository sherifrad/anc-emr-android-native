package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.state.SavedNotice
import com.sherifradwan.ancnative.state.WorkspaceSection

object UltrasoundDeleteAdapter : DeletableRecordAdapter<DemoUltrasoundScan> {
    override val type: DeletableType = DeletableType.ULTRASOUND
    override val fingerprintTag: String = "US"
    override val deleteReceiptKind: String = "ULTRASOUND_DELETE"
    override val restoreReceiptKind: String = "ULTRASOUND_RESTORE"
    override val section: WorkspaceSection = WorkspaceSection.ULTRASOUND
    override val deletedNotice: SavedNotice = SavedNotice.ULTRASOUND_DELETED
    override val restoredNotice: SavedNotice = SavedNotice.ULTRASOUND_RESTORED
    override val saveUndoneNotice: SavedNotice = SavedNotice.ULTRASOUND_SAVE_UNDONE

    override fun label(record: DemoUltrasoundScan): String = record.date

    override fun canonical(record: DemoUltrasoundScan): String =
        "${record.patientId}|${record.id}|${record.date}|${record.gestationalAgeWeeks}|${record.gestationalAgeDays}|${record.fetalNumber}|${record.presentation}|${record.placentalSite}|${record.amnioticFluid}|${record.fetalHeartRate}|${record.biometryBpd}|${record.biometryHc}|${record.biometryAc}|${record.biometryFl}|${record.estimatedFetalWeight}|${record.impression}|${record.linkedVisitId}|${ImageUriCodec.serialize(record.imageUris)}"

    override suspend fun load(dao: RecordsDao, id: String): DemoUltrasoundScan? =
        dao.ultrasoundScan(id)?.let {
            DemoUltrasoundScan(
                id = it.id,
                patientId = it.patientId,
                date = it.date,
                gestationalAgeWeeks = it.gaWeeks,
                gestationalAgeDays = it.gaDays,
                fetalNumber = it.fetalNumber,
                presentation = it.presentation,
                placentalSite = it.placentalSite,
                amnioticFluid = it.amnioticFluid,
                fetalHeartRate = it.fetalHeartRate,
                biometryBpd = it.biometryBpd,
                biometryHc = it.biometryHc,
                biometryAc = it.biometryAc,
                biometryFl = it.biometryFl,
                estimatedFetalWeight = it.estimatedFetalWeight,
                impression = it.impression,
                linkedVisitId = it.linkedVisitId,
                imageUris = ImageUriCodec.deserialize(it.imageUris),
                revision = it.revision,
            )
        }

    override suspend fun deleteRow(dao: RecordsDao, id: String, patientId: String, expectedRevision: Long): Int =
        dao.deleteUltrasoundScan(id, patientId, expectedRevision)

    override suspend fun insertRow(dao: RecordsDao, record: DemoUltrasoundScan, revision: Long) {
        dao.insertUltrasoundScan(
            UltrasoundScanEntity(
                id = record.id,
                patientId = record.patientId,
                date = record.date,
                gaWeeks = record.gestationalAgeWeeks,
                gaDays = record.gestationalAgeDays,
                fetalNumber = record.fetalNumber,
                presentation = record.presentation,
                placentalSite = record.placentalSite,
                amnioticFluid = record.amnioticFluid,
                fetalHeartRate = record.fetalHeartRate,
                biometryBpd = record.biometryBpd,
                biometryHc = record.biometryHc,
                biometryAc = record.biometryAc,
                biometryFl = record.biometryFl,
                estimatedFetalWeight = record.estimatedFetalWeight,
                impression = record.impression,
                linkedVisitId = record.linkedVisitId,
                imageUris = ImageUriCodec.serialize(record.imageUris),
                revision = revision,
            ),
        )
    }

    override fun record(patient: DemoPatient, id: String): DemoUltrasoundScan? =
        patient.ultrasoundScans.firstOrNull { it.id == id }
}
