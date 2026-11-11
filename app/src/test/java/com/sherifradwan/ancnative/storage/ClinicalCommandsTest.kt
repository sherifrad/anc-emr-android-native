package com.sherifradwan.ancnative.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ClinicalCommandsTest {

    @Test
    fun saveDatingCommandValidatesDatesAndControlledMethod() {
        val valid = SaveDatingCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "patient-1",
            expectedPatientRevision = 1,
            lmpDate = "2026-01-01",
            ultrasoundEdd = "2026-10-08",
            agreedEdd = "2026-10-08",
            datingMethod = "Ultrasound",
            notes = "Dating confirmed by 12w scan",
        )
        assertTrue(valid.isStructurallyValid())

        val invalidDate = valid.copy(lmpDate = "01-01-2026")
        assertFalse(invalidDate.isStructurallyValid())

        val invalidMethod = valid.copy(datingMethod = "InventedMethod")
        assertFalse(invalidMethod.isStructurallyValid())

        val invalidRevision = valid.copy(expectedPatientRevision = 0)
        assertFalse(invalidRevision.isStructurallyValid())
    }

    @Test
    fun saveUltrasoundScanCommandValidatesGaBoundsAndControlledVocab() {
        val valid = SaveUltrasoundScanCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "patient-1",
            expectedPatientRevision = 1,
            scanId = UUID.randomUUID().toString(),
            expectedScanRevision = null,
            date = "2026-05-15",
            gaWeeks = 20,
            gaDays = 3,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = "Anterior",
            amnioticFluid = "Normal",
            fetalHeartRate = "142 bpm",
            biometryBpd = "48 mm",
            biometryHc = "175 mm",
            biometryAc = "150 mm",
            biometryFl = "32 mm",
            estimatedFetalWeight = "350 g",
            impression = "Normal morphology scan",
        )
        assertTrue(valid.isStructurallyValid())

        val outOfBoundsGaWeeks = valid.copy(gaWeeks = 46)
        assertFalse(outOfBoundsGaWeeks.isStructurallyValid())

        val outOfBoundsGaDays = valid.copy(gaDays = 7)
        assertFalse(outOfBoundsGaDays.isStructurallyValid())

        val invalidPresentation = valid.copy(presentation = "UnknownPresentation")
        assertFalse(invalidPresentation.isStructurallyValid())
    }

    @Test
    fun saveLabInvestigationCommandValidatesCategoryAndRequiredFields() {
        val valid = SaveLabInvestigationCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "patient-1",
            expectedPatientRevision = 1,
            labId = UUID.randomUUID().toString(),
            expectedLabRevision = null,
            date = "2026-02-10",
            testCategory = "Hematology",
            testName = "Hemoglobin",
            result = "11.8",
            unit = "g/dL",
            referenceRange = "11.0 - 15.0",
            isAbnormal = false,
            notes = null,
        )
        assertTrue(valid.isStructurallyValid())

        val invalidCategory = valid.copy(testCategory = "Radiology")
        assertFalse(invalidCategory.isStructurallyValid())

        val blankResult = valid.copy(result = "")
        assertFalse(blankResult.isStructurallyValid())
    }

    @Test
    fun saveMedicationCommandValidatesRouteAndStatus() {
        val valid = SaveMedicationCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "patient-1",
            expectedPatientRevision = 1,
            medicationId = UUID.randomUUID().toString(),
            expectedMedicationRevision = null,
            name = "Ferrous Sulfate",
            dosage = "200 mg",
            route = "Oral",
            frequency = "Once daily",
            startDate = "2026-02-01",
            endDate = null,
            status = "Active",
            notes = null,
            isProphylaxis = true,
        )
        assertTrue(valid.isStructurallyValid())

        val invalidRoute = valid.copy(route = "Inhalation")
        assertFalse(invalidRoute.isStructurallyValid())

        val invalidStatus = valid.copy(status = "Pending")
        assertFalse(invalidStatus.isStructurallyValid())
    }

    @Test
    fun fingerprintsVaryDeterministicallyWithPayloadChanges() {
        val op = UUID.randomUUID().toString()
        val c1 = SaveDatingCommand(
            operationUuid = op,
            patientId = "p1",
            expectedPatientRevision = 1,
            lmpDate = "2026-01-01",
            ultrasoundEdd = "2026-10-08",
            agreedEdd = "2026-10-08",
            datingMethod = "Ultrasound",
            notes = "Notes A",
        )
        val c2 = c1.copy(notes = "Notes B")
        assertNotEquals(c1.computeFingerprint(), c2.computeFingerprint())
    }

    @Test
    fun saveDatingCommandPrepareValidatesAndConstructsCommand() {
        val stateWithInvalid = com.sherifradwan.ancnative.state.AncState(
            patients = emptyList(),
            datingDraft = com.sherifradwan.ancnative.state.DatingDraft(
                editorSessionId = "session-1",
                patientId = "p1",
                basePatientRevision = 1,
                original = com.sherifradwan.ancnative.state.DatingDraftValues(),
                values = com.sherifradwan.ancnative.state.DatingDraftValues(
                    lmpDate = "invalid-date",
                    datingMethod = "InvalidMethod",
                ),
            ),
        )
        val rejected = SaveDatingCommand.prepare(stateWithInvalid, "op-1")
        assertTrue(rejected is ClinicalCommandPreparation.Rejected)
        val errors = (rejected as ClinicalCommandPreparation.Rejected).errors
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.INVALID_DATE))
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.INVALID_METHOD))

        val stateWithValid = com.sherifradwan.ancnative.state.AncState(
            patients = emptyList(),
            datingDraft = com.sherifradwan.ancnative.state.DatingDraft(
                editorSessionId = "session-2",
                patientId = "p1",
                basePatientRevision = 2,
                original = com.sherifradwan.ancnative.state.DatingDraftValues(),
                values = com.sherifradwan.ancnative.state.DatingDraftValues(
                    lmpDate = "2026-01-01",
                    agreedEdd = "2026-10-08",
                    datingMethod = "Ultrasound",
                ),
            ),
        )
        val ready = SaveDatingCommand.prepare(stateWithValid, "op-2")
        assertTrue(ready is ClinicalCommandPreparation.Ready)
        val cmd = (ready as ClinicalCommandPreparation.Ready).command as SaveDatingCommand
        org.junit.Assert.assertEquals("p1", cmd.patientId)
        org.junit.Assert.assertEquals(2L, cmd.expectedPatientRevision)
        org.junit.Assert.assertEquals("2026-01-01", cmd.lmpDate)
        org.junit.Assert.assertEquals("2026-10-08", cmd.agreedEdd)
    }

    @Test
    fun saveUltrasoundScanCommandPrepareValidatesAndConstructsCommand() {
        val stateWithInvalid = com.sherifradwan.ancnative.state.AncState(
            patients = emptyList(),
            ultrasoundDraft = com.sherifradwan.ancnative.state.UltrasoundDraft(
                editorSessionId = "session-1",
                patientId = "p1",
                scanId = "scan-1",
                basePatientRevision = 1,
                baseScanRevision = null,
                original = com.sherifradwan.ancnative.state.UltrasoundDraftValues(),
                values = com.sherifradwan.ancnative.state.UltrasoundDraftValues(
                    date = "",
                    gaWeeks = "50",
                    gaDays = "8",
                ),
            ),
        )
        val rejected = SaveUltrasoundScanCommand.prepare(stateWithInvalid, "op-1", "scan-1")
        assertTrue(rejected is ClinicalCommandPreparation.Rejected)
        val errors = (rejected as ClinicalCommandPreparation.Rejected).errors
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.REQUIRED_DATE))
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.INVALID_GA_WEEKS))
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.INVALID_GA_DAYS))

        val stateWithValid = com.sherifradwan.ancnative.state.AncState(
            patients = emptyList(),
            ultrasoundDraft = com.sherifradwan.ancnative.state.UltrasoundDraft(
                editorSessionId = "session-2",
                patientId = "p1",
                scanId = "scan-2",
                basePatientRevision = 1,
                baseScanRevision = null,
                original = com.sherifradwan.ancnative.state.UltrasoundDraftValues(),
                values = com.sherifradwan.ancnative.state.UltrasoundDraftValues(
                    date = "2026-05-15",
                    gaWeeks = "20",
                    gaDays = "3",
                    fetalNumber = "Singleton",
                    presentation = "Cephalic",
                ),
            ),
        )
        val ready = SaveUltrasoundScanCommand.prepare(stateWithValid, "op-2", "scan-2")
        assertTrue(ready is ClinicalCommandPreparation.Ready)
        val cmd = (ready as ClinicalCommandPreparation.Ready).command as SaveUltrasoundScanCommand
        org.junit.Assert.assertEquals("p1", cmd.patientId)
        org.junit.Assert.assertEquals("2026-05-15", cmd.date)
        org.junit.Assert.assertEquals(20, cmd.gaWeeks)
        org.junit.Assert.assertEquals(3, cmd.gaDays)
        org.junit.Assert.assertEquals("Singleton", cmd.fetalNumber)
        org.junit.Assert.assertEquals("Cephalic", cmd.presentation)
    }

    @Test
    fun saveLabInvestigationCommandPrepareValidatesAndConstructsCommand() {
        val stateWithInvalid = com.sherifradwan.ancnative.state.AncState(
            patients = emptyList(),
            labDraft = com.sherifradwan.ancnative.state.LabDraft(
                editorSessionId = "session-1",
                patientId = "p1",
                labId = "lab-1",
                basePatientRevision = 1,
                baseLabRevision = null,
                original = com.sherifradwan.ancnative.state.LabDraftValues(),
                values = com.sherifradwan.ancnative.state.LabDraftValues(
                    date = "",
                    testCategory = "InvalidCategory",
                    testName = "",
                    result = "",
                ),
            ),
        )
        val rejected = SaveLabInvestigationCommand.prepare(stateWithInvalid, "op-1", "lab-1")
        assertTrue(rejected is ClinicalCommandPreparation.Rejected)
        val errors = (rejected as ClinicalCommandPreparation.Rejected).errors
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.REQUIRED_DATE))
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.INVALID_CATEGORY))
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.REQUIRED_NAME))
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.REQUIRED_RESULT))

        val stateWithValid = com.sherifradwan.ancnative.state.AncState(
            patients = emptyList(),
            labDraft = com.sherifradwan.ancnative.state.LabDraft(
                editorSessionId = "session-2",
                patientId = "p1",
                labId = "lab-2",
                basePatientRevision = 1,
                baseLabRevision = null,
                original = com.sherifradwan.ancnative.state.LabDraftValues(),
                values = com.sherifradwan.ancnative.state.LabDraftValues(
                    date = "2026-06-01",
                    testCategory = "Hematology",
                    testName = "Hemoglobin",
                    result = "10.5",
                    unit = "g/dL",
                    isAbnormal = true,
                ),
            ),
        )
        val ready = SaveLabInvestigationCommand.prepare(stateWithValid, "op-2", "lab-2")
        assertTrue(ready is ClinicalCommandPreparation.Ready)
        val cmd = (ready as ClinicalCommandPreparation.Ready).command as SaveLabInvestigationCommand
        org.junit.Assert.assertEquals("p1", cmd.patientId)
        org.junit.Assert.assertEquals("2026-06-01", cmd.date)
        org.junit.Assert.assertEquals("Hematology", cmd.testCategory)
        org.junit.Assert.assertEquals("Hemoglobin", cmd.testName)
        org.junit.Assert.assertEquals("10.5", cmd.result)
        assertTrue(cmd.isAbnormal)
    }

    @Test
    fun saveMedicationCommandPrepareValidatesAndConstructsCommand() {
        val stateWithInvalid = com.sherifradwan.ancnative.state.AncState(
            patients = emptyList(),
            medicationDraft = com.sherifradwan.ancnative.state.MedicationDraft(
                editorSessionId = "session-1",
                patientId = "p1",
                medicationId = "med-1",
                basePatientRevision = 1,
                baseMedicationRevision = null,
                original = com.sherifradwan.ancnative.state.MedicationDraftValues(),
                values = com.sherifradwan.ancnative.state.MedicationDraftValues(
                    name = "",
                    dosage = "",
                    route = "UnknownRoute",
                    frequency = "",
                    startDate = "bad-date",
                    status = "InvalidStatus",
                ),
            ),
        )
        val rejected = SaveMedicationCommand.prepare(stateWithInvalid, "op-1", "med-1")
        assertTrue(rejected is ClinicalCommandPreparation.Rejected)
        val errors = (rejected as ClinicalCommandPreparation.Rejected).errors
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.REQUIRED_NAME))
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.REQUIRED_DOSAGE))
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.INVALID_ROUTE))
        assertTrue("a new medication needs a frequency", errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.REQUIRED_FREQUENCY))
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.INVALID_DATE))
        assertTrue(errors.contains(com.sherifradwan.ancnative.state.ClinicalValidationError.INVALID_STATUS))

        val stateWithValid = com.sherifradwan.ancnative.state.AncState(
            patients = emptyList(),
            medicationDraft = com.sherifradwan.ancnative.state.MedicationDraft(
                editorSessionId = "session-2",
                patientId = "p1",
                medicationId = "med-2",
                basePatientRevision = 1,
                baseMedicationRevision = null,
                original = com.sherifradwan.ancnative.state.MedicationDraftValues(),
                values = com.sherifradwan.ancnative.state.MedicationDraftValues(
                    name = "Folic Acid",
                    dosage = "5 mg",
                    route = "Oral",
                    frequency = "Once daily",
                    startDate = "2026-01-01",
                    status = "Active",
                    isProphylaxis = true,
                ),
            ),
        )
        val ready = SaveMedicationCommand.prepare(stateWithValid, "op-2", "med-2")
        assertTrue(ready is ClinicalCommandPreparation.Ready)
        val cmd = (ready as ClinicalCommandPreparation.Ready).command as SaveMedicationCommand
        org.junit.Assert.assertEquals("p1", cmd.patientId)
        org.junit.Assert.assertEquals("Folic Acid", cmd.name)
        org.junit.Assert.assertEquals("5 mg", cmd.dosage)
        org.junit.Assert.assertEquals("Oral", cmd.route)
        org.junit.Assert.assertEquals("Once daily", cmd.frequency)
        org.junit.Assert.assertEquals("2026-01-01", cmd.startDate)
        org.junit.Assert.assertEquals("Active", cmd.status)
        assertTrue(cmd.isProphylaxis)
    }

    @Test
    fun saveUltrasoundCommandPreparesLinkedVisitAndImages() {
        val state = com.sherifradwan.ancnative.state.AncState(
            patients = emptyList(),
            ultrasoundDraft = com.sherifradwan.ancnative.state.UltrasoundDraft(
                editorSessionId = "session-us",
                patientId = "p1",
                scanId = "scan-1",
                basePatientRevision = 1,
                baseScanRevision = null,
                original = com.sherifradwan.ancnative.state.UltrasoundDraftValues(),
                values = com.sherifradwan.ancnative.state.UltrasoundDraftValues(
                    date = "2026-03-01",
                    gaWeeks = "12",
                    gaDays = "2",
                    linkedVisitId = "visit-123",
                    imageUris = listOf("file:///us1.jpg", "file:///us2.jpg"),
                ),
            ),
        )
        val prep = SaveUltrasoundScanCommand.prepare(state, "op-us", "scan-1")
        assertTrue(prep is ClinicalCommandPreparation.Ready)
        val cmd = (prep as ClinicalCommandPreparation.Ready).command as SaveUltrasoundScanCommand
        org.junit.Assert.assertEquals("visit-123", cmd.linkedVisitId)
        org.junit.Assert.assertEquals(listOf("file:///us1.jpg", "file:///us2.jpg"), cmd.imageUris)
    }

    @Test
    fun saveLabInvestigationPreparesLabName() {
        val state = com.sherifradwan.ancnative.state.AncState(
            patients = emptyList(),
            labDraft = com.sherifradwan.ancnative.state.LabDraft(
                editorSessionId = "session-lab",
                patientId = "p1",
                labId = "lab-1",
                basePatientRevision = 1,
                baseLabRevision = null,
                original = com.sherifradwan.ancnative.state.LabDraftValues(),
                values = com.sherifradwan.ancnative.state.LabDraftValues(
                    date = "2026-03-01",
                    testCategory = "Hematology",
                    testName = "CBC",
                    result = "Normal",
                    labName = "Al-Borg Diagnostics",
                ),
            ),
        )
        val prep = SaveLabInvestigationCommand.prepare(state, "op-lab", "lab-1")
        assertTrue(prep is ClinicalCommandPreparation.Ready)
        val cmd = (prep as ClinicalCommandPreparation.Ready).command as SaveLabInvestigationCommand
        org.junit.Assert.assertEquals("Al-Borg Diagnostics", cmd.labName)
    }

    @Test
    fun saveMedicationPreparesNonComplianceAndAdverseNotes() {
        val state = com.sherifradwan.ancnative.state.AncState(
            patients = emptyList(),
            medicationDraft = com.sherifradwan.ancnative.state.MedicationDraft(
                editorSessionId = "session-med",
                patientId = "p1",
                medicationId = "med-1",
                basePatientRevision = 1,
                baseMedicationRevision = null,
                original = com.sherifradwan.ancnative.state.MedicationDraftValues(),
                values = com.sherifradwan.ancnative.state.MedicationDraftValues(
                    name = "Ferrous Sulfate",
                    dosage = "200 mg",
                    route = "Oral",
                    frequency = "Once daily",
                    startDate = "2026-03-01",
                    status = "Active",
                    isNonCompliant = true,
                    adverseNotes = "Mild nausea and constipation reported",
                ),
            ),
        )
        val prep = SaveMedicationCommand.prepare(state, "op-med", "med-1")
        assertTrue(prep is ClinicalCommandPreparation.Ready)
        val cmd = (prep as ClinicalCommandPreparation.Ready).command as SaveMedicationCommand
        assertTrue(cmd.isNonCompliant)
        org.junit.Assert.assertEquals("Mild nausea and constipation reported", cmd.adverseNotes)
    }

    /** A draft for one [frequency] and an optional stored revision (null = a new medication). */
    private fun medicationState(
        frequency: String,
        baseMedicationRevision: Long?,
        status: String = "Active",
        endDate: String = "",
    ) = com.sherifradwan.ancnative.state.AncState(
        patients = emptyList(),
        medicationDraft = com.sherifradwan.ancnative.state.MedicationDraft(
            editorSessionId = "session-freq",
            patientId = "p1",
            medicationId = "med-freq",
            basePatientRevision = 1,
            baseMedicationRevision = baseMedicationRevision,
            original = com.sherifradwan.ancnative.state.MedicationDraftValues(),
            values = com.sherifradwan.ancnative.state.MedicationDraftValues(
                name = "Folic Acid",
                dosage = "5 mg",
                route = "Oral",
                frequency = frequency,
                startDate = "2026-01-01",
                endDate = endDate,
                status = status,
            ),
        ),
    )

    @Test
    fun saveMedicationPrepareRejectsNewMedicationWithoutFrequency() {
        listOf("", "   ").forEach { blank ->
            val prep = SaveMedicationCommand.prepare(medicationState(blank, baseMedicationRevision = null), "op-f", "med-freq")
            assertTrue("blank '$blank' is rejected", prep is ClinicalCommandPreparation.Rejected)
            assertEquals(
                "only the frequency error",
                setOf(com.sherifradwan.ancnative.state.ClinicalValidationError.REQUIRED_FREQUENCY),
                (prep as ClinicalCommandPreparation.Rejected).errors,
            )
        }
    }

    @Test
    fun saveMedicationPrepareAcceptsNewMedicationWithEachFixedFrequencyAndOtherText() {
        val accepted = com.sherifradwan.ancnative.ui.COMMON_MEDICATION_FREQUENCIES + "Every other day after food"
        assertTrue(com.sherifradwan.ancnative.ui.COMMON_MEDICATION_FREQUENCIES.isNotEmpty())
        accepted.forEach { frequency ->
            val prep = SaveMedicationCommand.prepare(medicationState(" $frequency ", baseMedicationRevision = null), "op-f", "med-freq")
            assertTrue("'$frequency' is accepted", prep is ClinicalCommandPreparation.Ready)
            val cmd = (prep as ClinicalCommandPreparation.Ready).command as SaveMedicationCommand
            org.junit.Assert.assertEquals(frequency, cmd.frequency)
            org.junit.Assert.assertEquals(null, cmd.expectedMedicationRevision)
        }
    }

    @Test
    fun saveMedicationPrepareAcceptsUpdateOfStoredRowWithoutFrequency() {
        val prep = SaveMedicationCommand.prepare(
            medicationState("", baseMedicationRevision = 3),
            "00000000-0000-4000-8000-0000000000f3",
            "med-freq",
        )
        assertTrue("an update keeps a stored blank frequency", prep is ClinicalCommandPreparation.Ready)
        val cmd = (prep as ClinicalCommandPreparation.Ready).command as SaveMedicationCommand
        org.junit.Assert.assertEquals("", cmd.frequency)
        org.junit.Assert.assertEquals(3L, cmd.expectedMedicationRevision)
        assertTrue(cmd.isStructurallyValid())
    }

    @Test
    fun saveMedicationPrepareAcceptsStopAndUndoOfStoredRowWithoutFrequency() {
        // What the one-tap stop builds for a legacy row with no frequency.
        val stop = SaveMedicationCommand.prepare(
            medicationState("", baseMedicationRevision = 2, status = "Discontinued", endDate = "2026-02-01"),
            "op-stop",
            "med-freq",
        )
        assertTrue("a legacy row can be stopped", stop is ClinicalCommandPreparation.Ready)
        val stopCmd = (stop as ClinicalCommandPreparation.Ready).command as SaveMedicationCommand
        org.junit.Assert.assertEquals("Discontinued", stopCmd.status)
        org.junit.Assert.assertEquals("2026-02-01", stopCmd.endDate)
        org.junit.Assert.assertEquals("", stopCmd.frequency)

        // What its Undo builds: the second save that restores the earlier status.
        val undo = SaveMedicationCommand.prepare(medicationState("", baseMedicationRevision = 3), "op-undo", "med-freq")
        assertTrue("a legacy row can be restored", undo is ClinicalCommandPreparation.Ready)
        val undoCmd = (undo as ClinicalCommandPreparation.Ready).command as SaveMedicationCommand
        org.junit.Assert.assertEquals("Active", undoCmd.status)
        org.junit.Assert.assertEquals(null, undoCmd.endDate)
        org.junit.Assert.assertEquals(3L, undoCmd.expectedMedicationRevision)
    }

    @Test
    fun imageUriCodecRoundTripsLosslesslyWithCommasAndSpecialChars() {
        val sampleUris = listOf(
            "content://media/external/images/media/1234?filter=a,b&c=d",
            "file:///storage/emulated/0/DCIM/scan,with,comma.jpg",
            "https://cloud.storage.test/scan%201.png",
        )
        val serialized = ImageUriCodec.serialize(sampleUris)
        org.junit.Assert.assertNotNull(serialized)
        val restored = ImageUriCodec.deserialize(serialized)
        org.junit.Assert.assertEquals(sampleUris, restored)
        org.junit.Assert.assertTrue(ImageUriCodec.deserialize(null).isEmpty())
        org.junit.Assert.assertTrue(ImageUriCodec.deserialize("").isEmpty())
    }

    @Test
    fun saveDatingCommandStrictlyEnforcesIvfRules() {
        val validIvf = SaveDatingCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "patient-ivf",
            expectedPatientRevision = 1,
            datingMethod = "IVF_TRANSFER",
            transferDate = "2026-02-01",
            embryoAgeDays = 5,
        )
        assertTrue(validIvf.isStructurallyValid())

        val invalidEmbryoAge = validIvf.copy(embryoAgeDays = 4)
        assertFalse(invalidEmbryoAge.isStructurallyValid())

        val missingTransferDate = validIvf.copy(transferDate = null)
        assertFalse(missingTransferDate.isStructurallyValid())
    }

    @Test
    fun saveVisitCommandFingerprintsAndValidatesLinkedUltrasoundId() {
        val command = SaveVisitCommand(
            operationUuid = UUID.randomUUID().toString(),
            editorSessionId = "session-1",
            patientId = "patient-1",
            visitId = "visit-1",
            expectedRevision = null,
            date = "2026-03-15",
            bloodPressure = "120/80",
            weight = "65.5",
            findings = "Normal fetal movements",
            notes = "Routine ANC visit",
            linkedUltrasoundId = "scan-999",
            fingerprint = "",
        )
        val fingerprinted = command.copy(fingerprint = command.canonicalFingerprint())
        assertTrue(fingerprinted.fingerprint.isNotEmpty())
        org.junit.Assert.assertEquals("scan-999", fingerprinted.linkedUltrasoundId)

        val unlinked = fingerprinted.copy(linkedUltrasoundId = null)
        assertNotEquals(fingerprinted.fingerprint, unlinked.canonicalFingerprint())
    }

    @Test
    fun saveUltrasoundScanCommandFingerprintsAndValidatesLinkedVisitAndImageUris() {
        val command = SaveUltrasoundScanCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "patient-1",
            expectedPatientRevision = 1,
            scanId = "scan-1",
            expectedScanRevision = null,
            date = "2026-03-15",
            gaWeeks = 24,
            gaDays = 2,
            fetalNumber = "Singleton",
            presentation = "Cephalic",
            placentalSite = "Posterior",
            amnioticFluid = "Normal",
            fetalHeartRate = "140",
            biometryBpd = "60",
            biometryHc = "220",
            biometryAc = "195",
            biometryFl = "43",
            estimatedFetalWeight = "650",
            impression = "Normal anatomy scan",
            linkedVisitId = "visit-888",
            imageUris = listOf("content://media/photo1.jpg", "content://media/photo2.jpg"),
        )
        assertTrue(command.isStructurallyValid())
        val fp = command.computeFingerprint()
        assertTrue(fp.isNotEmpty())
        org.junit.Assert.assertEquals("visit-888", command.linkedVisitId)
        org.junit.Assert.assertEquals(2, command.imageUris.size)

        val unlinked = command.copy(linkedVisitId = null)
        assertNotEquals(fp, unlinked.computeFingerprint())
    }

    @Test
    fun saveMedicationCommandValidatesComplianceAndAdverseNotes() {
        val command = SaveMedicationCommand(
            operationUuid = UUID.randomUUID().toString(),
            patientId = "patient-1",
            expectedPatientRevision = 1,
            medicationId = "med-1",
            expectedMedicationRevision = null,
            name = "Ferrous Sulfate",
            dosage = "200 mg",
            route = "Oral",
            frequency = "Once daily",
            startDate = "2026-03-15",
            endDate = null,
            status = "Active",
            notes = "Take with orange juice",
            isProphylaxis = true,
            isNonCompliant = true,
            adverseNotes = "Reports epigastric burning and nausea",
        )
        assertTrue(command.isStructurallyValid())
        val fp = command.computeFingerprint()
        assertTrue(fp.isNotEmpty())
        assertTrue(command.isNonCompliant)
        org.junit.Assert.assertEquals("Reports epigastric burning and nausea", command.adverseNotes)

        val compliant = command.copy(isNonCompliant = false)
        assertNotEquals(fp, compliant.computeFingerprint())

        val noAdverse = command.copy(adverseNotes = null)
        assertNotEquals(fp, noAdverse.computeFingerprint())
    }

    private val hex64 = Regex("[0-9a-f]{64}")

    private fun med(revision: Long = 3) = DeleteFixtures.med(revision)

    private fun lab(revision: Long = 3) = DeleteFixtures.lab(revision)

    private fun assertAllDistinct(base: String, variants: List<String>) {
        assertEquals(variants.size, variants.toSet().size)
        variants.forEach { assertNotEquals(base, it) }
    }

    @Test
    fun deleteCommandsValidateRevisionIdAndHashForEveryType() {
        DeleteFixtures.all.forEach { f ->
            val name = f.type.name
            val valid = f.delete()
            assertTrue(name, valid.isStructurallyValid())
            assertEquals(name, f.deleteKind, valid.recordKind)
            assertEquals(name, f.recordId, valid.recordId)
            assertEquals(name, "", valid.editorSessionId)

            assertFalse(name, valid.copy(expectedPatientRevision = 0).isStructurallyValid())
            assertFalse(name, valid.copy(expectedRecordRevision = 0).isStructurallyValid())
            assertFalse(name, valid.copy(operationUuid = "not-a-uuid").isStructurallyValid())
            assertFalse(name, valid.copy(patientId = " ").isStructurallyValid())
            assertFalse(name, valid.copy(patientId = "").isStructurallyValid())
            assertFalse(name, valid.copy(recordId = "").isStructurallyValid())
            assertFalse(name, valid.copy(recordId = " ").isStructurallyValid())
            assertFalse(name, valid.copy(deletedContentHash = "abc").isStructurallyValid())
            assertFalse(name, valid.copy(deletedContentHash = "z".repeat(64)).isStructurallyValid())
            assertFalse(name, valid.copy(deletedContentHash = "a".repeat(65)).isStructurallyValid())
        }
    }

    @Test
    fun deleteFingerprintsBindEveryFieldForEveryType() {
        DeleteFixtures.all.forEach { f ->
            val cmd = f.delete()
            assertEquals(f.type.name, cmd.computeFingerprint(), cmd.copy().computeFingerprint())
            assertAllDistinct(
                cmd.computeFingerprint(),
                listOf(
                    cmd.copy(operationUuid = UUID.randomUUID().toString()),
                    cmd.copy(patientId = "patient-2"),
                    cmd.copy(expectedPatientRevision = if (cmd.expectedPatientRevision == 5L) 6L else 5L),
                    cmd.copy(recordId = f.otherRecordId),
                    cmd.copy(expectedRecordRevision = 4),
                    cmd.copy(deletedContentHash = "b".repeat(64)),
                ).map { it.computeFingerprint() },
            )
        }
    }

    @Test
    fun restoreFingerprintBindsSourceDeleteAndPayloadForEveryType() {
        DeleteFixtures.all.forEach { f ->
            val name = f.type.name
            val source = f.delete()
            val restore = f.restore(source)
            assertTrue(name, restore.isStructurallyValid())
            assertEquals(name, f.restoreKind, restore.recordKind)
            assertEquals(name, f.recordId, restore.recordId)
            assertEquals(name, restore.computeFingerprint(), restore.copy().computeFingerprint())

            assertAllDistinct(
                restore.computeFingerprint(),
                (
                    listOf(
                        restore.copy(operationUuid = UUID.randomUUID().toString()),
                        restore.copy(patientId = "patient-2"),
                        restore.copy(expectedPatientRevision = 6),
                        restore.copy(sourceDelete = source.copy(operationUuid = UUID.randomUUID().toString())),
                        restore.copy(sourceDelete = source.copy(expectedRecordRevision = 9)),
                    ) + f.fieldVariants.map { restore.copy(record = it) }
                    ).map { it.computeFingerprint() },
            )
            assertFalse(name, restore.copy(expectedPatientRevision = 0).isStructurallyValid())
            assertFalse(name, restore.copy(operationUuid = "not-a-uuid").isStructurallyValid())
            assertFalse(name, restore.copy(patientId = " ").isStructurallyValid())
            assertFalse(name, restore.copy(sourceDelete = source.copy(deletedContentHash = "abc")).isStructurallyValid())
        }
    }

    @Test
    fun deleteAndRestoreFingerprintsMatchTheGoldenCommit19c36fcStrings() {
        DeleteFixtures.all.forEach { f ->
            assertEquals(f.type.name, f.goldenDeleteFingerprint, f.delete().computeFingerprint())
            assertEquals(f.type.name, f.goldenRestoreFingerprint, f.restore().computeFingerprint())
        }
    }

    @Test
    fun contentHashesMatchTheGoldenCommit19c36fcValues() {
        DeleteFixtures.all.forEach { f ->
            assertEquals(f.type.name, f.goldenHash, f.contentHash(f.recordOf(3)))
        }
    }

    @Test
    fun contentHashIgnoresRevisionButCoversAllFields() {
        val base = med()
        val hash = medicationContentHash(base)
        assertTrue(hex64.matches(hash))
        assertEquals(hash, medicationContentHash(base.copy(revision = 99)))
        assertAllDistinct(
            hash,
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
            ).map { medicationContentHash(it) },
        )
    }

    @Test
    fun labContentHashIgnoresRevisionButCoversAllFields() {
        val base = lab()
        val hash = labContentHash(base)
        assertTrue(hex64.matches(hash))
        assertEquals(hash, labContentHash(base.copy(revision = 99)))
        assertAllDistinct(
            hash,
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
            ).map { labContentHash(it) },
        )
    }
}
