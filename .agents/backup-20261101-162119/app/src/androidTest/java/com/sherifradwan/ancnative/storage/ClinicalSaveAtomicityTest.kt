package com.sherifradwan.ancnative.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoFixtures
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClinicalSaveAtomicityTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun staleLabSaveReportsRejectedWithoutCommittingAnyRow() = withDatabase { opened, vaultUuid ->
        runBlocking {
            val repository = RecordsRepository(opened.database, vaultUuid)
            repository.seedInitialSyntheticFixtures(includeDemoPatients = true)
            val patient = (repository.validateAndLoad() as RepositoryOpenResult.Success)
                .snapshot.patients.first { it.id == DemoFixtures.patients[0].id }

            val labId = UUID.randomUUID().toString()
            val command = SaveLabInvestigationCommand(
                editorSessionId = "atomicity-lab",
                operationUuid = UUID.randomUUID().toString(),
                patientId = patient.id,
                expectedPatientRevision = patient.revision + 99,
                labId = labId,
                expectedLabRevision = null,
                date = "2026-10-30",
                testCategory = "Hematology",
                testName = "Hemoglobin",
                result = "10.5",
                unit = "g/dL",
                referenceRange = "11.0 - 15.0",
                isAbnormal = true,
                notes = "must not persist",
            )

            assertEquals(
                ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION),
                repository.execute(command),
            )
            assertNull(opened.database.recordsDao().labInvestigation(labId))
            assertNull(opened.database.recordsDao().clinicalCommandReceipt(command.operationUuid))
            assertEquals(patient.revision, opened.database.recordsDao().patient(patient.id)?.revision)
        }
    }

    @Test
    fun staleMedicationSaveReportsRejectedWithoutCommittingAnyRow() = withDatabase { opened, vaultUuid ->
        runBlocking {
            val repository = RecordsRepository(opened.database, vaultUuid)
            repository.seedInitialSyntheticFixtures(includeDemoPatients = true)
            val patient = (repository.validateAndLoad() as RepositoryOpenResult.Success)
                .snapshot.patients.first { it.id == DemoFixtures.patients[0].id }

            val medicationId = UUID.randomUUID().toString()
            val command = SaveMedicationCommand(
                editorSessionId = "atomicity-med",
                operationUuid = UUID.randomUUID().toString(),
                patientId = patient.id,
                expectedPatientRevision = patient.revision + 99,
                medicationId = medicationId,
                expectedMedicationRevision = null,
                name = "Ferrous Sulfate",
                dosage = "200 mg",
                route = "Oral",
                frequency = "Once daily",
                startDate = "2026-05-20",
                endDate = "2026-06-20",
                status = "Active",
                notes = "must not persist",
                isProphylaxis = false,
            )

            assertEquals(
                ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.STALE_PATIENT_REVISION),
                repository.execute(command),
            )
            assertNull(opened.database.recordsDao().medication(medicationId))
            assertNull(opened.database.recordsDao().clinicalCommandReceipt(command.operationUuid))
            assertEquals(patient.revision, opened.database.recordsDao().patient(patient.id)?.revision)
        }
    }

    private fun withDatabase(block: (OpenedRecordsDatabase, String) -> Unit) {
        val namespace = UUID.randomUUID().toString()
        val files = VaultFiles.isolatedTest(context, namespace)
        val vaultUuid = UUID.randomUUID().toString()
        files.prepareDescriptor(VaultDescriptor(vaultUuid, UUID.randomUUID().toString()))
        val passphrase = ByteArray(32).also(SecureRandom()::nextBytes)
        try {
            RecordsDatabase.open(context, files.databaseFile(vaultUuid).absolutePath, passphrase).use { opened ->
                block(opened, vaultUuid)
            }
        } finally {
            passphrase.fill(0)
            files.cleanupRegisteredTestNamespace()
        }
    }
}
