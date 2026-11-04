package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoAttachment
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure planning half of the unlock sweep: no Android, no disk. */
class AttachmentOrphanSweeperTest {
    private val now = 1_000_000_000L
    private val old = 10 * 60_000L

    private fun file(dir: String, name: String, ageMillis: Long) = PayloadFile(dir, name, now - ageMillis)

    private fun patient(id: String, files: List<String>) = DemoPatient(
        id = id,
        caseId = id.uppercase(),
        displayName = "Patient $id",
        dateOfBirth = "1995-01-01",
        phone = "01000000000",
        visits = emptyList(),
        attachments = files.map { name ->
            DemoAttachment(
                id = "att-$name",
                patientId = id,
                fileName = name,
                filePath = "patient_attachments/$id/$name",
                category = "Ultrasound",
                fileSizeBytes = 1024,
                sha256Hash = "00",
                title = null,
                capturedAt = "2026-01-05T00:00:00Z",
            )
        },
    )

    @Test
    fun aReferencedFileIsNeverAnOrphan() {
        val present = listOf(file("p1", "kept.webp", old))
        assertTrue(AttachmentOrphanSweeper.planOrphans(present, setOf("p1/kept.webp"), now).isEmpty())
    }

    @Test
    fun anOldUnreferencedFileIsAnOrphan() {
        val present = listOf(file("p1", "debris.webp", old))
        assertEquals(listOf("p1/debris.webp"), AttachmentOrphanSweeper.planOrphans(present, emptySet(), now).map { it.key })
    }

    @Test
    fun aFreshFileIsKeptUntilItsRowCanExist() {
        val present = listOf(file("p1", "just-written.webp", AttachmentOrphanSweeper.MIN_AGE_MILLIS - 1))
        assertTrue(AttachmentOrphanSweeper.planOrphans(present, emptySet(), now).isEmpty())
        val atLimit = listOf(file("p1", "just-written.webp", AttachmentOrphanSweeper.MIN_AGE_MILLIS))
        assertEquals(1, AttachmentOrphanSweeper.planOrphans(atLimit, emptySet(), now).size)
    }

    @Test
    fun aRowOfOnePatientProtectsOnlyThatPatientsFolder() {
        val present = listOf(file("p2", "someone-else.webp", old))
        val referenced = AttachmentOrphanSweeper.referencedKeys(listOf(patient("p1", listOf("my-scan.webp"))))
        assertEquals(listOf("p2/someone-else.webp"), AttachmentOrphanSweeper.planOrphans(present, referenced, now).map { it.key })
    }

    @Test
    fun referencedKeysComeFromTheAttachmentRows() {
        val keys = AttachmentOrphanSweeper.referencedKeys(listOf(patient("p1", listOf("a.webp", "b.webp"))))
        assertEquals(setOf("p1/a.webp", "p1/b.webp"), keys)
    }

    @Test
    fun aBlankFileNameNeverProtectsTheWholeFolder() {
        val keys = AttachmentOrphanSweeper.referencedKeys(listOf(patient("p1", listOf(" "))))
        assertTrue(keys.isEmpty())
    }

    @Test
    fun anUltrasoundScanPhotoIsReferencedEvenWithoutAnAttachmentRow() {
        val scanPatient = DemoPatient(
            id = "p1",
            caseId = "P1",
            displayName = "Patient p1",
            dateOfBirth = "1995-01-01",
            phone = "01000000000",
            visits = emptyList(),
            ultrasoundScans = listOf(
                DemoUltrasoundScan(
                    id = "u1",
                    patientId = "p1",
                    date = "2026-10-31",
                    gestationalAgeWeeks = 36,
                    gestationalAgeDays = 0,
                    fetalNumber = "Single",
                    presentation = "Cephalic",
                    placentalSite = "Posterior",
                    amnioticFluid = "Normal",
                    fetalHeartRate = "140",
                    biometryBpd = "90",
                    biometryHc = "320",
                    biometryAc = "310",
                    biometryFl = "70",
                    estimatedFetalWeight = "2700g",
                    impression = "Normal",
                    imageUris = listOf("vault:att_scan.webp"),
                ),
            ),
        )
        val keys = AttachmentOrphanSweeper.referencedKeys(listOf(scanPatient))
        assertEquals(setOf("p1/att_scan.webp"), keys)
        val present = listOf(file("p1", "att_scan.webp", old))
        assertTrue("a scan photo must never be an orphan", AttachmentOrphanSweeper.planOrphans(present, keys, now).isEmpty())
    }

    @Test
    fun anExternalScanPhotoUriProtectsNothing() {
        val external = DemoPatient(
            id = "p1",
            caseId = "P1",
            displayName = "Patient p1",
            dateOfBirth = "1995-01-01",
            phone = "01000000000",
            visits = emptyList(),
            ultrasoundScans = listOf(
                DemoUltrasoundScan(
                    id = "u1",
                    patientId = "p1",
                    date = "2026-10-31",
                    gestationalAgeWeeks = 36,
                    gestationalAgeDays = 0,
                    fetalNumber = "Single",
                    presentation = "Cephalic",
                    placentalSite = "Posterior",
                    amnioticFluid = "Normal",
                    fetalHeartRate = "140",
                    biometryBpd = "90",
                    biometryHc = "320",
                    biometryAc = "310",
                    biometryFl = "70",
                    estimatedFetalWeight = "2700g",
                    impression = "Normal",
                    imageUris = listOf("content://media/external/images/1"),
                ),
            ),
        )
        assertTrue(AttachmentOrphanSweeper.referencedKeys(listOf(external)).isEmpty())
    }

    @Test
    fun anUnknownModificationTimeIsNeverTreatedAsOld() {
        val present = listOf(PayloadFile("p1", "unknown-mtime.webp", 0L))
        assertTrue(AttachmentOrphanSweeper.planOrphans(present, emptySet(), now).isEmpty())
    }

    @Test
    fun anEmptyReferencedSetRemovesEveryOldFile() {
        val present = listOf(file("p1", "a.webp", old), file("p1", "b.webp", 0), file("p2", "c.webp", old))
        assertEquals(
            listOf("p1/a.webp", "p2/c.webp"),
            AttachmentOrphanSweeper.planOrphans(present, emptySet(), now).map { it.key }.sorted(),
        )
    }
}
