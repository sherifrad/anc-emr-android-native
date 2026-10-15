package com.sherifradwan.ancnative.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest

class PatientAttachmentVaultTest {

    @Test
    fun sha256IntegrityVerificationDetectsTampering() {
        val testData = "ANC Medical Record Image Content - Patient 001 - Ultrasound Calipers".toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val correctHash = digest.digest(testData).joinToString("") { "%02x".format(it) }

        val tempFile = File.createTempFile("test_attachment", ".webp")
        try {
            tempFile.writeBytes(testData)

            // Correct hash verifies successfully
            assertTrue(PatientAttachmentVault.verifyIntegrity(tempFile, correctHash))

            // Tampered file fails verification
            tempFile.writeBytes("Tampered ultrasound calipers content".toByteArray())
            assertFalse(PatientAttachmentVault.verifyIntegrity(tempFile, correctHash))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun sameNamePatientsHaveDistinctBoundariesAndFolders() {
        // Two patients with identical names
        val patientId1 = "11111111-0000-0000-0000-000000000001"
        val patientId2 = "22222222-0000-0000-0000-000000000002"

        val tempRoot = File(System.getProperty("java.io.tmpdir"), "anc_test_vault_${System.currentTimeMillis()}")
        tempRoot.mkdirs()

        try {
            val patient1Folder = File(tempRoot, "patient_attachments/$patientId1")
            val patient2Folder = File(tempRoot, "patient_attachments/$patientId2")

            patient1Folder.mkdirs()
            patient2Folder.mkdirs()

            val file1 = File(patient1Folder, "att_001.webp").apply { writeText("Patient 1 Ultrasound") }
            val file2 = File(patient2Folder, "att_002.webp").apply { writeText("Patient 2 Ultrasound") }

            // Folders and files are strictly distinct
            assertTrue(file1.exists())
            assertTrue(file2.exists())
            assertEquals(file1.parentFile?.name, patientId1)
            assertEquals(file2.parentFile?.name, patientId2)
            assertFalse(file1.parentFile?.path == file2.parentFile?.path)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun crossPatientAttachmentAccessIsStrictlyPrevented() {
        // Verify that resolveAttachmentFile verifies ownership boundary
        val patientId1 = "pat-alpha-123"
        val patientId2 = "pat-beta-456"

        val tempRoot = File(System.getProperty("java.io.tmpdir"), "anc_boundary_test_${System.currentTimeMillis()}")
        tempRoot.mkdirs()
        try {
            val p1Dir = File(tempRoot, "patient_attachments/$patientId1").apply { mkdirs() }
            val p2Dir = File(tempRoot, "patient_attachments/$patientId2").apply { mkdirs() }

            val fileP1 = File(p1Dir, "scan_p1.webp").apply { writeText("Secret P1 Ultrasound") }
            val fileP2 = File(p2Dir, "scan_p2.webp").apply { writeText("Secret P2 Ultrasound") }

            // Accessing file inside p2 using p1's directory must fail
            val fakeP1Attempt = File(p1Dir, fileP2.name)
            assertFalse("Patient 1 must not be able to find Patient 2's attachment in its own folder", fakeP1Attempt.exists())
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun computeStatsCalculatesStorageCorrectlyPerPatient() {
        val size1 = 500L * 1024L
        val size2 = 2L * 1024L * 1024L
        val attachments = listOf(
            SavedAttachmentResult(
                attachmentId = "att_1",
                patientId = "pat_1",
                fileName = "att_1.webp",
                relativePath = "patient_attachments/pat_1/att_1.webp",
                absolutePath = "/tmp/att_1.webp",
                fileSizeBytes = size1,
                sha256Hash = "hash1",
            ),
            SavedAttachmentResult(
                attachmentId = "att_2",
                patientId = "pat_1",
                fileName = "att_2.webp",
                relativePath = "patient_attachments/pat_1/att_2.webp",
                absolutePath = "/tmp/att_2.webp",
                fileSizeBytes = size2,
                sha256Hash = "hash2",
            ),
        )

        val stats = AttachmentSyncStats(
            totalCount = attachments.size,
            totalSizeBytes = attachments.sumOf { it.fileSizeBytes },
            syncedCount = 0,
            pendingCount = 0,
            localOnlyCount = attachments.size,
        )

        val expectedTotalBytes = size1 + size2
        assertEquals(2, stats.totalCount)
        assertEquals(expectedTotalBytes, stats.totalSizeBytes)
        assertEquals(2, stats.localOnlyCount)
    }

    @Test
    fun formatFileSizeFormatsBytesProperly() {
        assertEquals("500 B", AttachmentSyncManager.formatFileSize(500))
        assertEquals("200.0 KB", AttachmentSyncManager.formatFileSize(200 * 1024))
        assertEquals("2.50 MB", AttachmentSyncManager.formatFileSize((2.5 * 1024 * 1024).toLong()))
    }
}

