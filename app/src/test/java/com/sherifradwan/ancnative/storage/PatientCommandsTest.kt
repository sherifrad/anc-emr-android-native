package com.sherifradwan.ancnative.storage

import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.PatientValidationError
import com.sherifradwan.ancnative.model.PatientVocabulary
import com.sherifradwan.ancnative.model.PreviousPregnancyFormRules
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncReducer
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.PatientDraft
import com.sherifradwan.ancnative.state.PatientDraftValues
import com.sherifradwan.ancnative.state.PregnancyDraftField
import com.sherifradwan.ancnative.state.PreviousPregnancyDraft
import com.sherifradwan.ancnative.state.PreviousPregnancyDraftValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientCommandsTest {
    @Test
    fun profilePreparationTrimsRequiredIdentityAndPreservesUnknownFields() {
        val values = PatientDraftValues(
            caseId = "  CASE-9  ",
            displayName = "  Synthetic Person  ",
            dateOfBirth = "   ",
            phone = "",
            ageText = " 28 ",
            gravida = "",
            parity = "  ",
            abortion = "",
        )
        val state = AncState(
            patients = emptyList(),
            patientDraft = PatientDraft(
                editorSessionId = "editor-1",
                patientId = PATIENT_ID,
                baseRevision = null,
                createdAt = null,
                original = PatientDraftValues(),
                values = values,
            ),
        )

        val prepared = SavePatientProfileCommand.prepare(state, OPERATION_ID, SUBMITTED_AT)
        assertTrue(prepared is PatientCommandPreparation.Ready)
        val command = (prepared as PatientCommandPreparation.Ready).command as SavePatientProfileCommand
        assertEquals("CASE-9", command.caseId)
        assertEquals("Synthetic Person", command.displayName)
        assertNull(command.dateOfBirth)
        assertNull(command.phone)
        assertNull(command.gravida)
        assertNull(command.parity)
        assertNull(command.abortion)
        assertTrue(command.isStructurallyValid())
    }

    @Test
    fun profilePreparationRejectsPartialOrOutOfRangeGpa() {
        val partial = profileState(PatientDraftValues(caseId = "CASE-1", displayName = "Patient", ageText = "30", gravida = "1"))
        assertTrue(
            SavePatientProfileCommand.prepare(partial, OPERATION_ID, SUBMITTED_AT) is
                PatientCommandPreparation.Rejected,
        )

        val outOfRange = profileState(
            PatientDraftValues(
                caseId = "CASE-1",
                displayName = "Patient",
                ageText = "30",
                gravida = "9007199254740992",
                parity = "0",
                abortion = "0",
            ),
        )
        assertTrue(
            SavePatientProfileCommand.prepare(outOfRange, OPERATION_ID, SUBMITTED_AT) is
                PatientCommandPreparation.Rejected,
        )
    }

    @Test
    fun outcomeChangeRetainsOnlyCompatibleConditionalMethod() {
        val starting = pregnancyState(
            PreviousPregnancyDraftValues(
                outcome = "LiveBirth",
                deliveryBy = "CS",
                lossManagement = "Surgical",
                terminationMethod = "Medical",
                clinicianId = "clinician-1",
            ),
        )

        val loss = AncReducer.reduce(
            starting,
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "EarlyPregnancyLoss"),
        ).pregnancyDraft!!.values
        assertEquals("", loss.deliveryBy)
        assertEquals("Surgical", loss.lossManagement)
        assertEquals("", loss.terminationMethod)

        val termination = AncReducer.reduce(
            starting,
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "TerminationOfPregnancy"),
        ).pregnancyDraft!!.values
        assertEquals("", termination.deliveryBy)
        assertEquals("", termination.lossManagement)
        assertEquals("Medical", termination.terminationMethod)

        val stillbirth = AncReducer.reduce(
            starting,
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "Stillbirth"),
        ).pregnancyDraft!!.values
        assertEquals("CS", stillbirth.deliveryBy)
        assertEquals("", stillbirth.lossManagement)
        assertEquals("", stillbirth.terminationMethod)
    }

    @Test
    fun pf1_oneDeliveryModeIsWrittenToBothColumns() {
        val cs = ready(picked(draftValues(outcome = "LiveBirth"), "CS"))
        assertEquals("CS", cs.deliveryBy)
        assertEquals("Cesarean", cs.birthMethod)
        val vaginal = ready(picked(draftValues(outcome = "Stillbirth"), "Vaginal"))
        assertEquals("Vaginal", vaginal.deliveryBy)
        assertEquals("Vaginal", vaginal.birthMethod)
        val loss = ready(draftValues(outcome = "EarlyPregnancyLoss", deliveryBy = "CS", birthMethod = "Cesarean", lossManagement = "Medical"))
        assertNull(loss.deliveryBy)
        assertNull(loss.birthMethod)
        assertTrue(loss.isStructurallyValid())
    }

    @Test
    fun explicitPickOverwritesBothStoredColumns() {
        val old = draftValues(outcome = "LiveBirth", deliveryBy = "Hysterotomy", birthMethod = "Vaginal")
        val repicked = ready(picked(old, "CS"))
        assertEquals("CS", repicked.deliveryBy)
        assertEquals("Cesarean", repicked.birthMethod)
    }

    @Test
    fun oldRowWithBlankOutcomeKeepsItsDeliveryColumnsWhenSavedUntouched() {
        val saved = ready(
            pregnancyState(
                draftValues(deliveryBy = "CS", birthMethod = "Cesarean"),
                original = draftValues(deliveryBy = "CS", birthMethod = "Cesarean", placeAndDoctor = ""),
                baseRevision = 2L,
            ),
        )
        assertNull(saved.outcome)
        assertEquals("CS", saved.deliveryBy)
        assertEquals("Cesarean", saved.birthMethod)
    }

    @Test
    fun hysterotomyUnderLiveBirthSurvivesAnUnrelatedEdit() {
        val values = draftValues(outcome = "LiveBirth", deliveryBy = "Hysterotomy", otherDetails = "edited note")
        val saved = ready(pregnancyState(values, original = values.copy(otherDetails = ""), baseRevision = 2L))
        assertEquals("Hysterotomy", saved.deliveryBy)
        assertNull(saved.birthMethod)
        assertEquals("", PreviousPregnancyFormRules.modeOf("Hysterotomy", null))
    }

    @Test
    fun changingOutcomeToLossOrTerminationClearsTheStoredColumns() {
        val stored = draftValues(outcome = "LiveBirth", deliveryBy = "CS", birthMethod = "Cesarean")
        val old = pregnancyState(stored, original = stored, baseRevision = 2L)
        for (outcome in listOf("EarlyPregnancyLoss", "TerminationOfPregnancy")) {
            val values = AncReducer.reduce(old, AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, outcome))
                .pregnancyDraft!!.values
            assertEquals("", values.deliveryBy)
            assertEquals("", values.birthMethod)
        }
    }

    @Test
    fun storedLossAndTerminationRowsKeepDeliveryColumnsOnUnrelatedEdit() {
        val loss = draftValues(outcome = "EarlyPregnancyLoss", deliveryBy = "CS", lossManagement = "Medical")
        val lossSaved = ready(pregnancyState(loss.copy(otherDetails = "note"), original = loss, baseRevision = 2L))
        assertEquals("CS", lossSaved.deliveryBy)
        assertNull(lossSaved.birthMethod)
        val termination = draftValues(outcome = "TerminationOfPregnancy", deliveryBy = "Hysterotomy", terminationMethod = "Hysterotomy")
        val terminationSaved = ready(pregnancyState(termination.copy(otherDetails = "note"), original = termination, baseRevision = 2L))
        assertEquals("Hysterotomy", terminationSaved.deliveryBy)
        assertNull(terminationSaved.birthMethod)
        assertEquals("Hysterotomy", terminationSaved.terminationMethod)
    }

    @Test
    fun leavingAndReturningToABirthOutcomeRestoresTheStoredMode() {
        for (outcome in listOf("LiveBirth", "Stillbirth")) {
            val stored = draftValues(outcome = outcome, deliveryBy = "CS", birthMethod = "Cesarean")
            var state = pregnancyState(stored, original = stored, baseRevision = 2L)
            state = AncReducer.reduce(state, AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "EarlyPregnancyLoss"))
            assertEquals("", state.pregnancyDraft!!.values.deliveryBy)
            state = AncReducer.reduce(state, AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, outcome))
            assertEquals("CS", state.pregnancyDraft!!.values.deliveryBy)
            assertEquals("Cesarean", state.pregnancyDraft!!.values.birthMethod)
            val saved = ready(state)
            assertEquals("CS", saved.deliveryBy)
            assertEquals("Cesarean", saved.birthMethod)
        }
    }

    @Test
    fun rePickingBirthOutcomeKeepsStoredHysterotomy() {
        val stored = draftValues(outcome = "LiveBirth", deliveryBy = "Hysterotomy")
        val values = AncReducer.reduce(
            pregnancyState(stored, original = stored, baseRevision = 2L),
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "Stillbirth"),
        ).pregnancyDraft!!.values
        assertEquals("Hysterotomy", values.deliveryBy)
    }

    @Test
    fun hysterotomyIsValidAsTerminationMethodOnlyNotAsBirthMethod() {
        assertEquals("Hysterotomy", ready(draftValues(outcome = "TerminationOfPregnancy", terminationMethod = "Hysterotomy")).terminationMethod)
        val rejected = SavePreviousPregnancyCommand.prepare(
            pregnancyState(draftValues(outcome = "LiveBirth", birthMethod = "Hysterotomy")), OPERATION_ID, SUBMITTED_AT,
        ) as PatientCommandPreparation.Rejected
        assertEquals(setOf(PatientValidationError.INVALID_BIRTH_METHOD), rejected.errors)
    }

    @Test
    fun birthMethodWithoutOutcomeIsAllowedOnlyWhenEditingAStoredRow() {
        val values = draftValues(birthMethod = "Cesarean")
        val rejected = SavePreviousPregnancyCommand.prepare(pregnancyState(values), OPERATION_ID, SUBMITTED_AT)
        assertEquals(
            setOf(PatientValidationError.OUTCOME_METHOD_MISMATCH),
            (rejected as PatientCommandPreparation.Rejected).errors,
        )
        val saved = ready(pregnancyState(values, original = values, baseRevision = 2L))
        assertEquals("Cesarean", saved.birthMethod)
        assertTrue(saved.isStructurallyValid())
    }

    @Test
    fun combinedOldRowEditKeepsEverythingStored() {
        val stored = draftValues(
            outcome = "LiveBirth", deliveryBy = "Hysterotomy", placeAndDoctor = "", clinicianId = " ",
        )
        val saved = ready(pregnancyState(stored.copy(otherDetails = "note"), original = stored, baseRevision = 2L))
        assertEquals("Hysterotomy", saved.deliveryBy)
        assertNull(saved.placeAndDoctor)
        assertEquals(PreviousPregnancyFormRules.DEFAULT_CLINICIAN_ID, saved.clinicianId)
    }

    @Test
    fun clinicianIdOnEditComesFromTheOriginalRowNotTheRestoredDraft() {
        val original = draftValues(clinicianId = "old-doctor")
        val saved = ready(pregnancyState(original.copy(clinicianId = "tampered"), original = original, baseRevision = 2L))
        assertEquals("old-doctor", saved.clinicianId)
    }

    @Test
    fun modeOfFollowsReaderPriorityCesareanWins() {
        assertEquals("CS", PreviousPregnancyFormRules.modeOf("Vaginal", "Cesarean"))
        assertEquals("CS", PreviousPregnancyFormRules.modeOf("CS", "Vaginal"))
        assertEquals("Vaginal", PreviousPregnancyFormRules.modeOf(null, "Vaginal"))
        assertEquals("", PreviousPregnancyFormRules.modeOf("Hysterotomy", null))
    }

    @Test
    fun pf1_changingOutcomeAwayFromBirthClearsTheModeAndPickingOutcomeClearsOldFreeText() {
        val starting = pregnancyState(draftValues(outcome = "LiveBirth", deliveryBy = "CS", birthMethod = "Cesarean", outcomeText = "Live birth by CS"))
        val picked = AncReducer.reduce(starting, AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, "EarlyPregnancyLoss"))
            .pregnancyDraft!!.values
        assertEquals("", picked.deliveryBy)
        assertEquals("", picked.outcomeText)
        val unpicked = AncReducer.reduce(
            pregnancyState(draftValues(outcomeText = "Live birth by CS")),
            AncEvent.PregnancyDraftChanged(PregnancyDraftField.OUTCOME, ""),
        ).pregnancyDraft!!.values
        assertEquals("Live birth by CS", unpicked.outcomeText)
    }

    @Test
    fun pf2_newPregnancyRequiresPlaceAndDoctor() {
        for (blank in listOf("", "   ")) {
            val rejected = SavePreviousPregnancyCommand.prepare(
                pregnancyState(draftValues(placeAndDoctor = blank)), OPERATION_ID, SUBMITTED_AT,
            ) as PatientCommandPreparation.Rejected
            assertEquals(setOf(PatientValidationError.PLACE_AND_DOCTOR_REQUIRED), rejected.errors)
        }
        assertEquals("Clinic A, Dr. B", ready(draftValues(placeAndDoctor = "  Clinic A, Dr. B ")).placeAndDoctor)
    }

    @Test
    fun pf2_existingRowWithoutPlaceStaysEditableButAFilledOneCannotBeBlanked() {
        val oldRow = pregnancyState(
            draftValues(placeAndDoctor = ""),
            original = draftValues(placeAndDoctor = ""),
            baseRevision = 2L,
        )
        val saved = SavePreviousPregnancyCommand.prepare(oldRow, OPERATION_ID, SUBMITTED_AT)
        assertTrue(saved is PatientCommandPreparation.Ready)
        assertNull((saved as PatientCommandPreparation.Ready).command.let { it as SavePreviousPregnancyCommand }.placeAndDoctor)

        val cleared = pregnancyState(
            draftValues(placeAndDoctor = "  "),
            original = draftValues(placeAndDoctor = "Clinic A"),
            baseRevision = 2L,
        )
        val rejected = SavePreviousPregnancyCommand.prepare(cleared, OPERATION_ID, SUBMITTED_AT) as PatientCommandPreparation.Rejected
        assertEquals(setOf(PatientValidationError.PLACE_AND_DOCTOR_REQUIRED), rejected.errors)
    }

    @Test
    fun pf3_clinicianIdIsDefaultForNewAndKeptOnEditAndNeverBlocksSave() {
        assertEquals(PreviousPregnancyFormRules.DEFAULT_CLINICIAN_ID, ready(draftValues(clinicianId = "")).clinicianId)
        assertEquals(PreviousPregnancyFormRules.DEFAULT_CLINICIAN_ID, ready(draftValues(clinicianId = "someone")).clinicianId)

        val edit = pregnancyState(draftValues(clinicianId = "old-doctor"), original = draftValues(clinicianId = "old-doctor"), baseRevision = 3L)
        assertEquals("old-doctor", (ready(edit)).clinicianId)
        val blankStored = pregnancyState(draftValues(clinicianId = " "), original = draftValues(clinicianId = " "), baseRevision = 3L)
        val result = SavePreviousPregnancyCommand.prepare(blankStored, OPERATION_ID, SUBMITTED_AT)
        assertTrue(result is PatientCommandPreparation.Ready)
        assertEquals(PatientVocabulary.CLINICIAN_ENTRY, ready(draftValues()).provenanceKind)
    }

    @Test
    fun pf4_preparationStillRejectsMismatchedMethods() {
        val rejected = SavePreviousPregnancyCommand.prepare(
            pregnancyState(draftValues(outcome = "EarlyPregnancyLoss", lossManagement = "Medical", terminationMethod = "Surgical")),
            OPERATION_ID,
            SUBMITTED_AT,
        ) as PatientCommandPreparation.Rejected
        assertEquals(setOf(PatientValidationError.OUTCOME_METHOD_MISMATCH), rejected.errors)
        val unknown = SavePreviousPregnancyCommand.prepare(
            pregnancyState(draftValues(outcome = "Unknown")), OPERATION_ID, SUBMITTED_AT,
        ) as PatientCommandPreparation.Rejected
        assertEquals(setOf(PatientValidationError.INVALID_PREGNANCY_OUTCOME), unknown.errors)
    }

    @Test
    fun pf5_editorKeepsAnOldFreeTextOutcomeAndShowsModeFromBirthMethod() {
        val row = DemoPreviousPregnancy(
            id = PREGNANCY_ID,
            patientId = DemoFixtures.patients.first().id,
            modelVersion = 3,
            dateText = null,
            outcomeText = "Live birth by CS",
            pregnancyType = null,
            otherDetails = null,
            placeAndDoctor = null,
            deliveryBy = null,
            outcome = null,
            birthMethod = "Cesarean",
            lossManagement = null,
            terminationMethod = null,
            provenanceKind = PatientVocabulary.CLINICIAN_ENTRY,
            clinicianId = "old-doctor",
            recordedAt = SUBMITTED_AT,
            updatedAt = SUBMITTED_AT,
            revision = 2,
        )
        val old = DemoFixtures.patients.first().copy(previousPregnancies = listOf(row))
        val state = AncReducer.reduce(
            AncState(patients = listOf(old)),
            AncEvent.StartPregnancyEditor(
                patientId = old.id,
                pregnancyId = old.previousPregnancies.first().id,
                newPregnancyId = "unused",
                editorSessionId = "editor-old",
            ),
        )
        val values = state.pregnancyDraft!!.values
        assertEquals("Live birth by CS", values.outcomeText)
        assertEquals("Cesarean", values.birthMethod)
        assertEquals("CS", PreviousPregnancyFormRules.modeOf(values.deliveryBy, values.birthMethod))
        assertEquals("Live birth by CS", ready(state).outcomeText)
        assertEquals("Cesarean", ready(state).birthMethod)
    }

    @Test
    fun canonicalFingerprintMakesUuidAndPayloadReplayImmutable() {
        val command = SavePatientProfileCommand(
            operationUuid = OPERATION_ID,
            editorSessionId = "editor-1",
            patientId = PATIENT_ID,
            expectedPatientRevision = null,
            createdAt = SUBMITTED_AT,
            caseId = "CASE-1",
            displayName = "Synthetic Patient",
            dateOfBirth = null,
            phone = null,
            riskLevel = null,
            ageText = "28",
            addressText = null,
            bloodGroupText = null,
            gravida = null,
            parity = null,
            abortion = null,
            obstetricNotes = null,
            fingerprint = "",
        ).withCanonicalFingerprint() as SavePatientProfileCommand

        assertTrue(command.isStructurallyValid())
        assertFalse(command.copy(displayName = "Different").isStructurallyValid())
        assertFalse(command.copy(operationUuid = "not-a-uuid").withCanonicalFingerprint().isStructurallyValid())
    }

    @Test
    fun newProfileWithNameAndAgeOnlyIsReadyAndPhoneStaysOptional() {
        val state = profileState(PatientDraftValues(caseId = "CASE-1", displayName = "Patient", ageText = "31"))
        val prepared = SavePatientProfileCommand.prepare(state, OPERATION_ID, SUBMITTED_AT)
        assertTrue(prepared is PatientCommandPreparation.Ready)
        assertNull(((prepared as PatientCommandPreparation.Ready).command as SavePatientProfileCommand).phone)
    }

    @Test
    fun newProfileRejectsBlankAndWhitespaceAge() {
        for (age in listOf("", "   ")) {
            val state = profileState(PatientDraftValues(caseId = "CASE-1", displayName = "Patient", ageText = age))
            val prepared = SavePatientProfileCommand.prepare(state, OPERATION_ID, SUBMITTED_AT)
            assertEquals(
                setOf(PatientValidationError.AGE_REQUIRED),
                (prepared as PatientCommandPreparation.Rejected).errors,
            )
        }
    }

    @Test
    fun editOfLegacyRowWithoutStoredAgeSavesWithoutAge() {
        val state = editState(original = "", values = "")
        assertTrue(SavePatientProfileCommand.prepare(state, OPERATION_ID, SUBMITTED_AT) is PatientCommandPreparation.Ready)
    }

    @Test
    fun editOfRowWithStoredAgeCannotBlankIt() {
        for (age in listOf("", "  ")) {
            val prepared = SavePatientProfileCommand.prepare(editState(original = "29", values = age), OPERATION_ID, SUBMITTED_AT)
            assertEquals(
                setOf(PatientValidationError.AGE_REQUIRED),
                (prepared as PatientCommandPreparation.Rejected).errors,
            )
        }
        assertTrue(
            SavePatientProfileCommand.prepare(editState(original = "29", values = "30"), OPERATION_ID, SUBMITTED_AT) is
                PatientCommandPreparation.Ready,
        )
    }

    @Test
    fun storedCommandReplayRequiresAgeOnlyForCreate() {
        val create = SavePatientProfileCommand(
            OPERATION_ID, "editor-1", PATIENT_ID, null, SUBMITTED_AT, "CASE-1", "Patient", null, null, null,
            null, null, null, null, null, null, null, fingerprint = "",
        )
        assertFalse(create.withCanonicalFingerprint().isStructurallyValid())
        val update = create.copy(expectedPatientRevision = 2)
        assertTrue(update.withCanonicalFingerprint().isStructurallyValid())
    }

    private fun editState(original: String, values: String) = AncState(
        patients = emptyList(),
        patientDraft = PatientDraft(
            editorSessionId = "editor-1",
            patientId = PATIENT_ID,
            baseRevision = 2,
            createdAt = SUBMITTED_AT,
            original = PatientDraftValues(caseId = "CASE-1", displayName = "Patient", ageText = original),
            values = PatientDraftValues(caseId = "CASE-1", displayName = "Patient", ageText = values),
        ),
    )

    private fun profileState(values: PatientDraftValues) = AncState(
        patients = emptyList(),
        patientDraft = PatientDraft(
            editorSessionId = "editor-1",
            patientId = PATIENT_ID,
            baseRevision = null,
            createdAt = null,
            original = PatientDraftValues(),
            values = values,
        ),
    )

    private fun draftValues(
        outcome: String = "",
        deliveryBy: String = "",
        birthMethod: String = "",
        otherDetails: String = "",
        lossManagement: String = "",
        terminationMethod: String = "",
        placeAndDoctor: String = "Clinic A, Dr. B",
        clinicianId: String = "",
        outcomeText: String = "",
    ) = PreviousPregnancyDraftValues(
        outcome = outcome,
        deliveryBy = deliveryBy,
        birthMethod = birthMethod,
        otherDetails = otherDetails,
        lossManagement = lossManagement,
        terminationMethod = terminationMethod,
        placeAndDoctor = placeAndDoctor,
        clinicianId = clinicianId,
        outcomeText = outcomeText,
    )

    private fun picked(values: PreviousPregnancyDraftValues, mode: String): AncState = AncReducer.reduce(
        pregnancyState(values),
        AncEvent.PregnancyDraftChanged(PregnancyDraftField.DELIVERY_BY, mode),
    )

    private fun ready(values: PreviousPregnancyDraftValues): SavePreviousPregnancyCommand = ready(pregnancyState(values))

    private fun ready(state: AncState): SavePreviousPregnancyCommand {
        val prepared = SavePreviousPregnancyCommand.prepare(state, OPERATION_ID, SUBMITTED_AT)
        assertTrue("expected Ready but was $prepared", prepared is PatientCommandPreparation.Ready)
        return (prepared as PatientCommandPreparation.Ready).command as SavePreviousPregnancyCommand
    }

    private fun pregnancyState(
        values: PreviousPregnancyDraftValues,
        original: PreviousPregnancyDraftValues = PreviousPregnancyDraftValues(),
        baseRevision: Long? = null,
    ) = AncState(
        patients = emptyList(),
        pregnancyDraft = PreviousPregnancyDraft(
            editorSessionId = "editor-1",
            patientId = PATIENT_ID,
            pregnancyId = PREGNANCY_ID,
            basePatientRevision = 1,
            basePregnancyRevision = baseRevision,
            recordedAt = null,
            original = original,
            values = values,
        ),
    )

    private companion object {
        const val OPERATION_ID = "00000000-0000-4000-8000-000000000901"
        const val PATIENT_ID = "00000000-0000-4000-8000-000000000902"
        const val PREGNANCY_ID = "00000000-0000-4000-8000-000000000903"
        const val SUBMITTED_AT = "2026-10-11T08:00:00Z"
    }
}
