package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.state.SavedNotice
import com.sherifradwan.ancnative.state.WorkspaceSection

enum class DeletableType {
    MEDICATION,
    LAB,
}

interface DeletableRecord {
    val id: String
    val patientId: String
    val revision: Long
}

interface DeletableRecordAdapter<T : DeletableRecord> {
    val type: DeletableType
    val fingerprintTag: String
    val deleteReceiptKind: String
    val restoreReceiptKind: String
    val section: WorkspaceSection
    val deletedNotice: SavedNotice
    val restoredNotice: SavedNotice
    val saveUndoneNotice: SavedNotice
    fun label(record: T): String
    fun canonical(record: T): String
    suspend fun load(dao: RecordsDao, id: String): T?
    suspend fun deleteRow(dao: RecordsDao, id: String, patientId: String, expectedRevision: Long): Int
    suspend fun insertRow(dao: RecordsDao, record: T, revision: Long)
    fun record(patient: DemoPatient, id: String): T?
}

object DeletableRegistry {
    private val adapters: Map<DeletableType, DeletableRecordAdapter<*>> = listOf(
        MedicationDeleteAdapter,
        LabDeleteAdapter,
    ).associateBy { it.type }

    fun all(): List<DeletableRecordAdapter<*>> = adapters.values.toList()

    @Suppress("UNCHECKED_CAST")
    fun <T : DeletableRecord> of(type: DeletableType): DeletableRecordAdapter<T> =
        (adapters[type] ?: throw IllegalArgumentException("No adapter registered for $type")) as DeletableRecordAdapter<T>

    /** True for the receipt kinds of every registered delete and restore command (they have no draft). */
    fun isDeleteOrRestoreKind(kind: ClinicalRecordKind): Boolean =
        adapters.values.any { it.deleteReceiptKind == kind.name || it.restoreReceiptKind == kind.name }

    /** The record's label, or null when [record] is not of [type]: a mismatch can never throw. */
    fun labelOrNull(type: DeletableType, record: DeletableRecord): String? {
        val adapter = adapterFor(record)
        return if (adapter.type == type) adapter.label(record) else null
    }

    @Suppress("UNCHECKED_CAST")
    fun adapterFor(record: DeletableRecord): DeletableRecordAdapter<DeletableRecord> = when (record) {
        is DemoMedicationRecord -> of(DeletableType.MEDICATION)
        is DemoLabInvestigation -> of(DeletableType.LAB)
        else -> throw IllegalArgumentException("Unknown record kind: ${record::class}")
    }
}

fun contentHash(record: DeletableRecord): String =
    sha256Hex(DeletableRegistry.adapterFor(record).canonical(record))

fun medicationContentHash(record: DemoMedicationRecord): String = contentHash(record)

fun labContentHash(record: DemoLabInvestigation): String = contentHash(record)
