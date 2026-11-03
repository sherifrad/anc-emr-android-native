package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RecordsMappingRoundTripTest {

    @Test
    fun demoPatient002EntitiesMapSymmetricallyRoundTripToRoomAndBack() {
        // RecordsRepository is internal and its toEntity / toDomain methods test the exact Room seed -> readback path
        // We use a dummy instance to access the internal member extensions
        val repository = RecordsRepository(
            database = createDummyRecordsDatabase(),
            vaultUuid = "00000000-0000-4000-8000-000000000001",
        )

        val demo002 = DemoFixtures.patients.first { it.caseId == "DEMO-ANC-002" }

        with(repository) {
            // 1. Patient Dating round-trip
            assertNotNull(demo002.dating)
            val datingEntity = demo002.dating!!.toEntity(demo002.id)
            val restoredDating = datingEntity.toDomain()
            assertEquals(demo002.dating, restoredDating)

            // 2. Visits round-trip
            assertEquals(5, demo002.visits.size)
            demo002.visits.forEach { visit ->
                val entity = visit.toEntity(demo002.id)
                val restored = entity.toDomain()
                assertEquals(visit, restored)
            }

            // 3. Ultrasound scans round-trip (including image serialization)
            assertEquals(4, demo002.ultrasoundScans.size)
            demo002.ultrasoundScans.forEach { scan ->
                val entity = scan.toEntity(demo002.id)
                val restored = entity.toDomain()
                assertEquals(scan, restored)
            }

            // 4. Lab investigations round-trip
            assertEquals(4, demo002.labInvestigations.size)
            demo002.labInvestigations.forEach { lab ->
                val entity = lab.toEntity(demo002.id)
                val restored = entity.toDomain()
                assertEquals(lab, restored)
            }

            // 5. Medication records round-trip (including adherence and adverse effects)
            assertEquals(4, demo002.medications.size)
            demo002.medications.forEach { med ->
                val entity = med.toEntity(demo002.id)
                val restored = entity.toDomain()
                assertEquals(med, restored)
            }

            // 6. Previous pregnancies round-trip
            demo002.previousPregnancies.forEach { p ->
                val entity = p.toEntity(demo002.id)
                val restored = entity.toDomain()
                assertEquals(p, restored)
            }

            // 7. Attachments round-trip
            val testAttachment = com.sherifradwan.ancnative.model.DemoAttachment(
                id = "att_test_123",
                patientId = demo002.id,
                fileName = "att_test_123.webp",
                filePath = "patient_attachments/${demo002.id}/att_test_123.webp",
                category = "ULTRASOUND",
                mimeType = "image/webp",
                fileSizeBytes = 204800,
                sha256Hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                title = "32w Growth Scan",
                capturedAt = "2026-10-13T10:00:00Z",
                linkedRecordId = demo002.ultrasoundScans.firstOrNull()?.id,
            )
            val attEntity = testAttachment.toEntity(demo002.id)
            val restoredAtt = attEntity.toDomain()
            assertEquals(testAttachment, restoredAtt)

            // 8. Full Patient entity seed and readback round-trip
            val patientEntity = demo002.toEntity()
            val restoredPatient = patientEntity.toDomain(
                visits = demo002.visits,
                pregnancies = demo002.previousPregnancies,
                dating = demo002.dating,
                ultrasoundScans = demo002.ultrasoundScans,
                labInvestigations = demo002.labInvestigations,
                medications = demo002.medications,
                attachments = listOf(testAttachment),
            )
            assertEquals(demo002.copy(attachments = listOf(testAttachment)), restoredPatient)
        }
    }

    private fun createDummyRecordsDatabase(): RecordsDatabase {
        val dummyDao = java.lang.reflect.Proxy.newProxyInstance(
            RecordsDao::class.java.classLoader,
            arrayOf(RecordsDao::class.java),
        ) { _, _, _ -> null } as RecordsDao

        return object : RecordsDatabase() {
            override fun recordsDao(): RecordsDao = dummyDao
            override fun createOpenHelper(config: androidx.room.DatabaseConfiguration) = error("Not needed")
            override fun createInvalidationTracker() = error("Not needed")
            @Deprecated("Deprecated in RoomDatabase")
            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun clearAllTables() {}
        }
    }
}
