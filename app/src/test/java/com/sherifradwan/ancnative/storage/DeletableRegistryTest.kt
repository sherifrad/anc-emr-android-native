package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoMedicationRecord
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One fixture per deletable type. Every expected value below is a hard-coded literal computed outside the
 * production code (sha256 of the commit-19c36fc text), never derived from the code under test.
 */
internal class DeleteFixture(
    val type: DeletableType,
    val tag: String,
    val deleteKind: ClinicalRecordKind,
    val restoreKind: ClinicalRecordKind,
    val updateKind: ClinicalRecordKind,
    val otherDeleteKind: ClinicalRecordKind,
    val recordId: String,
    val otherRecordId: String,
    val goldenCanonical: String,
    val goldenHash: String,
    val goldenDeleteFingerprint: String,
    val goldenRestoreFingerprint: String,
    val recordOf: (revision: Long) -> DeletableRecord,
    val fieldVariants: List<DeletableRecord>,
    val contentHash: (DeletableRecord) -> String,
    val defaultPatientRevision: Long = 4,
) {
    fun delete(
        op: String = DeleteFixtures.DELETE_OP,
        patientId: String = "patient-1",
        patientRevision: Long = defaultPatientRevision,
        recordId: String = this.recordId,
        recordRevision: Long = 3,
        hash: String = goldenHash,
    ) = DeleteRecordCommand(
        type = type,
        operationUuid = op,
        patientId = patientId,
        expectedPatientRevision = patientRevision,
        recordId = recordId,
        expectedRecordRevision = recordRevision,
        deletedContentHash = hash,
    )

    fun restore(
        source: DeleteRecordCommand = delete(),
        op: String = DeleteFixtures.RESTORE_OP,
        patientId: String = "patient-1",
        patientRevision: Long = 5,
        record: DeletableRecord = recordOf(3),
    ) = RestoreRecordCommand(
        type = type,
        operationUuid = op,
        patientId = patientId,
        expectedPatientRevision = patientRevision,
        record = record,
        sourceDelete = source,
    )

    fun deleteReceipt(cmd: DeleteRecordCommand = delete()) = ClinicalCommandReceipt(
        operationUuid = cmd.operationUuid,
        patientId = cmd.patientId,
        recordKind = deleteKind.name,
        recordId = cmd.recordId,
        committedRevision = 4,
        committedPatientRevision = 5,
        fingerprint = cmd.computeFingerprint(),
    )
}

internal object DeleteFixtures {
    const val DELETE_OP = "11111111-1111-4111-8111-111111111111"
    const val RESTORE_OP = "22222222-2222-4222-8222-222222222222"

    fun med(revision: Long = 3) = DemoMedicationRecord(
        id = "med-1", patientId = "patient-1", name = "Ferrous Sulfate", dosage = "200 mg", route = "Oral",
        frequency = "Once daily", startDate = "2026-03-15", endDate = "2026-06-15", status = "Active",
        notes = "With juice", isProphylaxis = false, isNonCompliant = false, adverseNotes = "Nausea", revision = revision,
    )

    fun lab(revision: Long = 3) = DemoLabInvestigation(
        id = "lab-1", patientId = "patient-1", date = "2026-02-10", testCategory = "Hematology", testName = "Hemoglobin",
        result = "11.2", unit = "g/dL", referenceRange = "11-14", isAbnormal = false, notes = "Fasting",
        labName = "Central Lab", revision = revision,
    )

    fun us(revision: Long = 3) = DemoUltrasoundScan(
        id = "us-1",
        patientId = "patient-1",
        date = "2026-03-20",
        gestationalAgeWeeks = 20,
        gestationalAgeDays = 3,
        fetalNumber = "Single",
        presentation = "Cephalic",
        placentalSite = "Posterior",
        amnioticFluid = "Normal",
        fetalHeartRate = "142",
        biometryBpd = "48",
        biometryHc = "175",
        biometryAc = "150",
        biometryFl = "32",
        estimatedFetalWeight = "350g",
        impression = "Normal anatomy",
        linkedVisitId = "v-1",
        imageUris = emptyList(),
        revision = revision,
    )

    fun visit(revision: Long = 3) = DemoVisit(
        id = "visit-1",
        patientId = "patient-1",
        date = "2026-03-25",
        bloodPressure = "120/80",
        weight = "65.5",
        findings = "Normal fetal heart tones audible",
        notes = "Next visit in 4 weeks",
        linkedUltrasoundId = "us-1",
        revision = revision,
        isSyntheticEdit = false,
    )

    val medication = DeleteFixture(
        type = DeletableType.MEDICATION,
        tag = "MED",
        deleteKind = ClinicalRecordKind.MEDICATION_DELETE,
        restoreKind = ClinicalRecordKind.MEDICATION_RESTORE,
        updateKind = ClinicalRecordKind.MEDICATION_UPDATE,
        otherDeleteKind = ClinicalRecordKind.LAB_DELETE,
        recordId = "med-1",
        otherRecordId = "med-2",
        goldenCanonical = "patient-1|med-1|Ferrous Sulfate|200 mg|Oral|Once daily|2026-03-15|2026-06-15|Active|With juice|false|false|Nausea",
        goldenHash = "d8835f527304292b850cc67680f2810921a7a9b91ddf6ff2e6f75283191492d0",
        goldenDeleteFingerprint = "204252814f114fcdebb00bce26d41221979de68b4d3bf813df7e00b5254db004",
        goldenRestoreFingerprint = "4a11d69add3b3c74f5ff148ff2b9bae008ed0a703196cecb6fde14ff1536066e",
        recordOf = { med(it) },
        fieldVariants = med().let { base ->
            listOf(
                base.copy(id = "med-2"),
                base.copy(patientId = "patient-2"),
                base.copy(name = "Other"),
                base.copy(dosage = "400 mg"),
                base.copy(route = "IV"),
                base.copy(frequency = "Twice daily"),
                base.copy(startDate = "2026-03-16"),
                base.copy(endDate = null),
                base.copy(status = "Completed"),
                base.copy(notes = null),
                base.copy(isProphylaxis = true),
                base.copy(isNonCompliant = true),
                base.copy(adverseNotes = null),
            )
        },
        contentHash = { medicationContentHash(it as DemoMedicationRecord) },
    )

    val laboratory = DeleteFixture(
        type = DeletableType.LAB,
        tag = "LAB",
        deleteKind = ClinicalRecordKind.LAB_DELETE,
        restoreKind = ClinicalRecordKind.LAB_RESTORE,
        updateKind = ClinicalRecordKind.LAB_UPDATE,
        otherDeleteKind = ClinicalRecordKind.MEDICATION_DELETE,
        recordId = "lab-1",
        otherRecordId = "lab-2",
        goldenCanonical = "patient-1|lab-1|2026-02-10|Hematology|Hemoglobin|11.2|g/dL|11-14|false|Fasting|Central Lab",
        goldenHash = "1d5193862187f603c40a0254aab26dbb73d835698bca21c8804f4fe561a87639",
        goldenDeleteFingerprint = "c5304b70382fc405e1bd38c26f5d490c3486e74be603305777717c6f6422cebd",
        goldenRestoreFingerprint = "fc0817e251d15bde610fdc3bb13299b1192a7ff4389f48a1d7a65539b19990fe",
        recordOf = { lab(it) },
        fieldVariants = lab().let { base ->
            listOf(
                base.copy(id = "lab-2"),
                base.copy(patientId = "patient-2"),
                base.copy(date = "2026-02-11"),
                base.copy(testCategory = "Serology"),
                base.copy(testName = "Ferritin"),
                base.copy(result = "9.9"),
                base.copy(unit = null),
                base.copy(referenceRange = null),
                base.copy(isAbnormal = true),
                base.copy(notes = null),
                base.copy(labName = null),
            )
        },
        contentHash = { labContentHash(it as DemoLabInvestigation) },
    )

    val ultrasound = DeleteFixture(
        type = DeletableType.ULTRASOUND,
        tag = "US",
        deleteKind = ClinicalRecordKind.ULTRASOUND_DELETE,
        restoreKind = ClinicalRecordKind.ULTRASOUND_RESTORE,
        updateKind = ClinicalRecordKind.ULTRASOUND_UPDATE,
        otherDeleteKind = ClinicalRecordKind.MEDICATION_DELETE,
        recordId = "us-1",
        otherRecordId = "us-2",
        goldenCanonical = "patient-1|us-1|2026-03-20|20|3|Single|Cephalic|Posterior|Normal|142|48|175|150|32|350g|Normal anatomy|v-1|null",
        goldenHash = "47f569aa8d8d2276d8fbd4b530e52de28195e3c853c75919bab31fb6cd0d6967",
        goldenDeleteFingerprint = "c2fb13af17866841797b8883207daa43f8264f9d42b2ca59b4f5aa8ca6a0fb33",
        goldenRestoreFingerprint = "824630e871a347d62666d205f8e2816894026c605d8d61f84d169bdf09a27f81",
        recordOf = { us(it) },
        fieldVariants = us().let { base ->
            listOf(
                base.copy(id = "us-2"),
                base.copy(patientId = "patient-2"),
                base.copy(date = "2026-03-21"),
                base.copy(gestationalAgeWeeks = 21),
                base.copy(gestationalAgeDays = 4),
                base.copy(fetalNumber = "Twins"),
                base.copy(presentation = "Breech"),
                base.copy(placentalSite = "Anterior"),
                base.copy(amnioticFluid = "Oligohydramnios"),
                base.copy(fetalHeartRate = "150"),
                base.copy(biometryBpd = "50"),
                base.copy(biometryHc = "180"),
                base.copy(biometryAc = "160"),
                base.copy(biometryFl = "35"),
                base.copy(estimatedFetalWeight = "400g"),
                base.copy(impression = "Variant impression"),
                base.copy(linkedVisitId = null),
                base.copy(imageUris = listOf("vault:photo1.jpg")),
            )
        },
        contentHash = { ultrasoundContentHash(it as DemoUltrasoundScan) },
        defaultPatientRevision = 5,
    )

    val visit = DeleteFixture(
        type = DeletableType.VISIT,
        tag = "VIS",
        deleteKind = ClinicalRecordKind.VISIT_DELETE,
        restoreKind = ClinicalRecordKind.VISIT_RESTORE,
        updateKind = ClinicalRecordKind.DATING,
        otherDeleteKind = ClinicalRecordKind.MEDICATION_DELETE,
        recordId = "visit-1",
        otherRecordId = "visit-2",
        goldenCanonical = "patient-1|visit-1|2026-03-25|120/80|65.5|Normal fetal heart tones audible|Next visit in 4 weeks|us-1",
        goldenHash = "782418d369e77fd29e07ff9ee7abb72732a2023185600ba6a56cc11336466caa",
        goldenDeleteFingerprint = "3dcc3fe77968f999b113258d02e18829adec80916d144edb1bd45118bc9f9a76",
        goldenRestoreFingerprint = "03305bde8f66de6db1f5982454c3b507d83fa2fde1e1f9b85982c7b436de5d4e",
        recordOf = { visit(it) },
        fieldVariants = visit().let { base ->
            listOf(
                base.copy(id = "visit-2"),
                base.copy(patientId = "patient-2"),
                base.copy(date = "2026-03-26"),
                base.copy(bloodPressure = "130/85"),
                base.copy(weight = "66.0"),
                base.copy(findings = "Different findings"),
                base.copy(notes = null),
                base.copy(linkedUltrasoundId = null),
            )
        },
        contentHash = { visitContentHash(it as DemoVisit) },
    )

    val all = listOf(medication, laboratory, ultrasound, visit)
}

class DeletableRegistryTest {
    private val fixtures = DeleteFixtures.all

    private fun adapterOf(f: DeleteFixture) = DeletableRegistry.of<DeletableRecord>(f.type)

    private fun sha256(text: String) =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    @Test
    fun everyDeletableTypeIsRegisteredExactlyOnce() {
        val registered = DeletableRegistry.all().map { it.type }
        assertEquals(DeletableType.values().toSet(), registered.toSet())
        assertEquals(registered.size, registered.toSet().size)
        assertEquals("every registered type must have a test fixture", registered.toSet(), fixtures.map { it.type }.toSet())
        fixtures.forEach { assertEquals(it.type, adapterOf(it).type) }
    }

    @Test
    fun fingerprintTagsAndReceiptKindsAreUniqueAcrossAdapters() {
        val adapters = DeletableRegistry.all()
        assertEquals(adapters.size, adapters.map { it.fingerprintTag }.toSet().size)
        val kinds = adapters.flatMap { listOf(it.deleteReceiptKind, it.restoreReceiptKind) }
        assertEquals(kinds.size, kinds.toSet().size)
        val validNames = ClinicalRecordKind.values().map { it.name }.toSet()
        kinds.forEach { assertTrue("$it is not a ClinicalRecordKind", it in validNames) }
    }

    @Test
    fun tagsAndReceiptKindsAreTheExactStoredStrings() {
        fixtures.forEach { f ->
            val adapter = adapterOf(f)
            assertEquals(f.tag, adapter.fingerprintTag)
            assertEquals(f.deleteKind.name, adapter.deleteReceiptKind)
            assertEquals(f.restoreKind.name, adapter.restoreReceiptKind)
        }
        assertEquals("MED", adapterOf(DeleteFixtures.medication).fingerprintTag)
        assertEquals("LAB", adapterOf(DeleteFixtures.laboratory).fingerprintTag)
        assertEquals("US", adapterOf(DeleteFixtures.ultrasound).fingerprintTag)
        assertEquals("VIS", adapterOf(DeleteFixtures.visit).fingerprintTag)
        assertEquals("MEDICATION_DELETE", adapterOf(DeleteFixtures.medication).deleteReceiptKind)
        assertEquals("MEDICATION_RESTORE", adapterOf(DeleteFixtures.medication).restoreReceiptKind)
        assertEquals("LAB_DELETE", adapterOf(DeleteFixtures.laboratory).deleteReceiptKind)
        assertEquals("LAB_RESTORE", adapterOf(DeleteFixtures.laboratory).restoreReceiptKind)
        assertEquals("ULTRASOUND_DELETE", adapterOf(DeleteFixtures.ultrasound).deleteReceiptKind)
        assertEquals("ULTRASOUND_RESTORE", adapterOf(DeleteFixtures.ultrasound).restoreReceiptKind)
        assertEquals("VISIT_DELETE", adapterOf(DeleteFixtures.visit).deleteReceiptKind)
        assertEquals("VISIT_RESTORE", adapterOf(DeleteFixtures.visit).restoreReceiptKind)
    }

    @Test
    fun canonicalIsByteIdenticalAndContentHashIsSha256OfCanonical() {
        fixtures.forEach { f ->
            val record = f.recordOf(3)
            assertEquals(f.type.name, f.goldenCanonical, adapterOf(f).canonical(record))
            assertEquals(f.type.name, f.goldenHash, sha256(f.goldenCanonical))
            assertEquals(f.type.name, f.goldenHash, f.contentHash(record))
            assertEquals(f.type.name, sha256(adapterOf(f).canonical(record)), f.contentHash(record))
            assertEquals("revision must not enter the hash", f.goldenHash, f.contentHash(f.recordOf(99)))
        }
    }

    @Test
    fun canonicalAndHashCoverEveryUserEditableField() {
        fixtures.forEach { f ->
            val adapter = adapterOf(f)
            val canonicals = f.fieldVariants.map { adapter.canonical(it) }
            val hashes = f.fieldVariants.map { f.contentHash(it) }
            assertEquals(f.type.name, f.fieldVariants.size, canonicals.toSet().size)
            assertEquals(f.type.name, f.fieldVariants.size, hashes.toSet().size)
            canonicals.forEach { assertNotEquals(f.type.name, f.goldenCanonical, it) }
            hashes.forEach { assertNotEquals(f.type.name, f.goldenHash, it) }
        }
    }

    @Test
    fun deleteWithPatientRevisionChangesOnlyTheExpectedPatientRevision() {
        fixtures.forEach { f ->
            val cmd = f.delete()
            val moved: RecordUndoCommand = cmd.withPatientRevision(9)
            assertEquals(f.type.name, cmd.copy(expectedPatientRevision = 9), moved)
            assertEquals(f.type.name, 9L, moved.expectedPatientRevision)
            assertEquals(f.type.name, cmd.operationUuid, moved.operationUuid)
            assertEquals(f.type.name, cmd.recordId, moved.recordId)
            assertNotEquals(f.type.name, cmd.computeFingerprint(), moved.computeFingerprint())
            assertEquals(f.type.name, f.delete(patientRevision = 9).computeFingerprint(), moved.computeFingerprint())
            assertEquals(f.type.name, f.goldenDeleteFingerprint, cmd.computeFingerprint())
        }
    }

    @Test
    fun restoreWithPatientRevisionChangesOnlyTheExpectedPatientRevision() {
        fixtures.forEach { f ->
            val cmd = f.restore()
            val moved: RecordUndoCommand = cmd.withPatientRevision(9)
            assertEquals(f.type.name, cmd.copy(expectedPatientRevision = 9), moved)
            assertEquals(f.type.name, 9L, moved.expectedPatientRevision)
            assertEquals(f.type.name, cmd.operationUuid, moved.operationUuid)
            assertEquals(f.type.name, cmd.sourceDelete, (moved as RestoreRecordCommand).sourceDelete)
            assertEquals(f.type.name, cmd.record, moved.record)
            assertNotEquals(f.type.name, cmd.computeFingerprint(), moved.computeFingerprint())
            assertEquals(f.type.name, f.restore(patientRevision = 9).computeFingerprint(), moved.computeFingerprint())
            assertEquals(f.type.name, f.goldenRestoreFingerprint, cmd.computeFingerprint())
        }
    }

    @Test
    fun commandsOfDifferentTypesNeverShareAFingerprint() {
        val med = DeleteFixtures.medication
        val lab = DeleteFixtures.laboratory
        val sameFields = lab.delete(recordId = "med-1", hash = med.goldenHash)
        assertNotEquals(med.delete().computeFingerprint(), sameFields.computeFingerprint())
    }

    @Test
    fun everyDeletableTypeValueHasAFullyDescribedAdapter() {
        val tags = mutableListOf<String>()
        val kinds = mutableListOf<String>()
        val validKinds = ClinicalRecordKind.values().map { it.name }.toSet()
        DeletableType.values().forEach { type ->
            val adapter = DeletableRegistry.of<DeletableRecord>(type) // throws when no adapter is registered
            assertEquals(type, adapter.type)
            assertTrue("$type tag", adapter.fingerprintTag.isNotBlank())
            assertTrue("$type delete kind", adapter.deleteReceiptKind in validKinds)
            assertTrue("$type restore kind", adapter.restoreReceiptKind in validKinds)
            assertNotEquals("$type", adapter.deleteReceiptKind, adapter.restoreReceiptKind)
            assertEquals("$type notices must be distinct", 3, setOf(adapter.deletedNotice, adapter.restoredNotice, adapter.saveUndoneNotice).size)
            tags += adapter.fingerprintTag
            kinds += listOf(adapter.deleteReceiptKind, adapter.restoreReceiptKind)
        }
        assertEquals("fingerprintTag must be distinct", tags.size, tags.toSet().size)
        assertEquals("receipt kinds must be distinct", kinds.size, kinds.toSet().size)
    }

    @Test
    fun deleteAndRestoreKindsAreExactlyTheRegistryReceiptKinds() {
        val expected = DeletableRegistry.all().flatMap { listOf(it.deleteReceiptKind, it.restoreReceiptKind) }.toSet()
        val actual = ClinicalRecordKind.values().filter { DeletableRegistry.isDeleteOrRestoreKind(it) }.map { it.name }.toSet()
        assertEquals(expected, actual)
        // Every other kind is an edit kind the validation-failed branch names; DATING and the *_CREATE / *_UPDATE kinds.
        ClinicalRecordKind.values().filter { it.name !in expected }.forEach {
            assertTrue(it.name, it == ClinicalRecordKind.DATING || it.name.endsWith("_CREATE") || it.name.endsWith("_UPDATE"))
        }
    }

    @Test
    fun labelOfARecordOfAnotherTypeIsNullInsteadOfThrowing() {
        val med = DeleteFixtures.med()
        val lab = DeleteFixtures.lab()
        val us = DeleteFixtures.us()
        val v = DeleteFixtures.visit()
        assertTrue(DeletableRegistry.labelOrNull(DeletableType.MEDICATION, med) != null)
        assertEquals(null, DeletableRegistry.labelOrNull(DeletableType.LAB, med))
        assertEquals(null, DeletableRegistry.labelOrNull(DeletableType.MEDICATION, lab))
        assertEquals(null, DeletableRegistry.labelOrNull(DeletableType.ULTRASOUND, med))
        assertEquals(null, DeletableRegistry.labelOrNull(DeletableType.MEDICATION, us))
        assertTrue(DeletableRegistry.labelOrNull(DeletableType.ULTRASOUND, us) != null)
        assertEquals(null, DeletableRegistry.labelOrNull(DeletableType.VISIT, med))
        assertTrue(DeletableRegistry.labelOrNull(DeletableType.VISIT, v) != null)
    }
}
