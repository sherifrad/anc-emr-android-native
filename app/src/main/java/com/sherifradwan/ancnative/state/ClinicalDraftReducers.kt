package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.DemoLabInvestigation
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.model.DemoPatientDating
import com.sherifradwan.ancnative.model.DemoPreviousPregnancy
import com.sherifradwan.ancnative.model.DemoUltrasoundScan
import com.sherifradwan.ancnative.model.DemoVisit
import com.sherifradwan.ancnative.model.parseBloodPressure
import com.sherifradwan.ancnative.model.parseVisitFindings

internal fun startVisit(state: AncState, event: AncEvent.StartVisit): AncState {
    if (state.savePhase.blocksEditing()) return state
    val patient = state.patient(event.patientId) ?: return state
    if (patient.status == "archived") return state
    if (event.visitId != null && state.undoRunningFor(event.visitId)) return state
    val visit = event.visitId?.let { id -> patient.visits.firstOrNull { it.id == id } }
    if (event.visitId != null && visit == null) return state
    if (state.hasDirtyDraft) {
        return state.copy(
            showDiscardDialog = true,
            pendingAction = PendingAction.StartVisit(event.patientId, event.visitId, event.editorSessionId),
        )
    }
    val values = visit?.toDraftValues() ?: DraftValues()
    return state.clearDrafts().copy(
        route = AncRoute.VisitEditor(event.patientId),
        draft = VisitDraft(
            editorSessionId = event.editorSessionId,
            patientId = event.patientId,
            visitId = visit?.id,
            baseRevision = visit?.revision?.toInt(),
            original = values,
            values = values,
        ),
        savedNotice = false,
        notice = null,
    )
}

internal fun startPatientEditor(state: AncState, event: AncEvent.StartPatientEditor): AncState {
    if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
    val patient = event.patientId?.let(state::patient)
    if (event.patientId != null && (patient == null || patient.status == "archived")) return state
    val values = patient?.toPatientDraftValues() ?: run {
        val year = java.time.LocalDate.now().year
        val nextSeq = (state.patients.size + 1).toString().padStart(3, '0')
        PatientDraftValues(caseId = "ANC-$year-$nextSeq")
    }
    val patientId = patient?.id ?: event.newPatientId
    return state.clearDrafts().copy(
        route = AncRoute.PatientEditor(patientId, patient == null),
        patientDraft = PatientDraft(
            editorSessionId = event.editorSessionId,
            patientId = patientId,
            baseRevision = patient?.revision,
            createdAt = patient?.createdAt,
            original = values,
            values = values,
        ),
        savedNotice = false,
        notice = null,
    )
}

internal fun startHistoriesEditor(state: AncState, event: AncEvent.StartHistoriesEditor): AncState {
    if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
    val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
    val values = HistoriesDraftValues(
        medicalHistory = patient.medicalHistory.orEmpty(),
        surgicalHistory = patient.surgicalHistory.orEmpty(),
        familyHistory = patient.familyHistory.orEmpty(),
        allergyHistory = patient.allergyHistory.orEmpty(),
    )
    return state.clearDrafts().copy(
        route = AncRoute.OtherHistoriesEditor(patient.id),
        historiesDraft = HistoriesDraft(event.editorSessionId, patient.id, patient.revision, values, values),
        savedNotice = false,
        notice = null,
    )
}

internal fun startPregnancyEditor(state: AncState, event: AncEvent.StartPregnancyEditor): AncState {
    if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
    val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
    if (event.pregnancyId != null && state.undoRunningFor(event.pregnancyId)) return state
    val pregnancy = event.pregnancyId?.let { id -> patient.previousPregnancies.firstOrNull { it.id == id } }
    if (event.pregnancyId != null && pregnancy == null) return state
    val values = pregnancy?.toDraftValues() ?: PreviousPregnancyDraftValues()
    val pregnancyId = pregnancy?.id ?: event.newPregnancyId
    return state.clearDrafts().copy(
        route = AncRoute.PreviousPregnancyEditor(patient.id, pregnancyId),
        pregnancyDraft = PreviousPregnancyDraft(
            editorSessionId = event.editorSessionId,
            patientId = patient.id,
            pregnancyId = pregnancyId,
            basePatientRevision = patient.revision,
            basePregnancyRevision = pregnancy?.revision,
            recordedAt = pregnancy?.recordedAt,
            original = values,
            values = values,
        ),
        savedNotice = false,
        notice = null,
    )
}

internal fun startStatusEditor(state: AncState, event: AncEvent.StartStatusEditor): AncState {
    if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
    val patient = state.patient(event.patientId) ?: return state
    if (
        (event.targetStatus == "archived" && patient.status == "archived") ||
        (event.targetStatus == "active" && patient.status != "archived")
    ) return state
    return state.clearDrafts().copy(
        route = AncRoute.StatusEditor(patient.id, event.targetStatus),
        statusDraft = PatientStatusDraft(
            editorSessionId = event.editorSessionId,
            patientId = patient.id,
            baseRevision = patient.revision,
            targetStatus = event.targetStatus,
        ),
        savedNotice = false,
        notice = null,
    )
}

internal fun startDatingEditor(state: AncState, event: AncEvent.StartDatingEditor): AncState {
    if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
    val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
    val values = patient.dating?.toDraftValues() ?: DatingDraftValues()
    return state.clearDrafts().copy(
        route = AncRoute.DatingEditor(patient.id),
        datingDraft = DatingDraft(
            editorSessionId = event.editorSessionId,
            patientId = patient.id,
            basePatientRevision = patient.revision,
            original = values,
            values = values,
        ),
        savedNotice = false,
        notice = null,
    )
}

internal fun startUltrasoundEditor(state: AncState, event: AncEvent.StartUltrasoundEditor): AncState {
    if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
    if (event.scanId != null && state.undoRunningFor(event.scanId)) return state
    val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
    val scan = event.scanId?.let { id -> patient.ultrasoundScans.firstOrNull { it.id == id } }
    if (event.scanId != null && scan == null) return state
    val values = scan?.toDraftValues() ?: UltrasoundDraftValues()
    val scanId = scan?.id ?: event.newScanId
    return state.clearDrafts().copy(
        route = AncRoute.UltrasoundEditor(patient.id, scanId, isNew = scan == null),
        ultrasoundDraft = UltrasoundDraft(
            editorSessionId = event.editorSessionId,
            patientId = patient.id,
            scanId = scanId,
            basePatientRevision = patient.revision,
            baseScanRevision = scan?.revision,
            original = values,
            values = values,
        ),
        savedNotice = false,
        notice = null,
    )
}

internal fun startLabEditor(state: AncState, event: AncEvent.StartLabEditor): AncState {
    if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state
    if (event.labId != null && state.undoRunningFor(event.labId)) return state
    val patient = state.patient(event.patientId)?.takeIf { it.status != "archived" } ?: return state
    val lab = event.labId?.let { id -> patient.labInvestigations.firstOrNull { it.id == id } }
    if (event.labId != null && lab == null) return state
    val values = lab?.toDraftValues() ?: LabDraftValues(
        date = event.defaultDate,
        labName = event.defaultLabName,
    )
    val labId = lab?.id ?: event.newLabId
    return state.clearDrafts().copy(
        route = AncRoute.LabEditor(patient.id, labId, isNew = lab == null),
        labDraft = LabDraft(
            editorSessionId = event.editorSessionId,
            patientId = patient.id,
            labId = labId,
            basePatientRevision = patient.revision,
            baseLabRevision = lab?.revision,
            original = values,
            values = values,
        ),
        savedNotice = false,
        notice = null,
    )
}

internal fun updateDraft(state: AncState, event: AncEvent.DraftChanged): AncState {
    if (state.savePhase.blocksEditing()) return state
    val draft = state.draft ?: return state
    val values = when (event.field) {
        DraftField.DATE -> draft.values.copy(date = event.value)
        DraftField.BLOOD_PRESSURE -> {
            val raw = event.value.trim()
            if (raw.isEmpty()) {
                draft.values.copy(
                    bloodPressure = "",
                    systolic = "",
                    diastolic = "",
                )
            } else {
                val parsed = parseBloodPressure(raw)
                draft.values.copy(
                    bloodPressure = raw,
                    systolic = parsed?.systolic?.toString() ?: draft.values.systolic,
                    diastolic = parsed?.diastolic?.toString() ?: draft.values.diastolic,
                )
            }
        }
        DraftField.WEIGHT -> draft.values.copy(weight = event.value)
        DraftField.FINDINGS -> draft.values.copy(findings = event.value)
        DraftField.NOTES -> draft.values.copy(notes = event.value)
        DraftField.LINKED_ULTRASOUND -> draft.values.copy(linkedUltrasoundId = event.value)
        DraftField.SYSTOLIC -> {
            val s = event.value.trim()
            val d = draft.values.diastolic.trim()
            val bp = if (s.isNotEmpty() && d.isNotEmpty()) "$s/$d" else ""
            draft.values.copy(systolic = s, bloodPressure = bp)
        }
        DraftField.DIASTOLIC -> {
            val s = draft.values.systolic.trim()
            val d = event.value.trim()
            val bp = if (s.isNotEmpty() && d.isNotEmpty()) "$s/$d" else ""
            draft.values.copy(diastolic = d, bloodPressure = bp)
        }
        DraftField.FUNDAL_HEIGHT -> draft.values.copy(fundalHeight = event.value)
        DraftField.FETAL_HEART_RATE -> draft.values.copy(fetalHeartRate = event.value)
        DraftField.OEDEMA -> draft.values.copy(oedema = event.value)
        DraftField.FETAL_MOVEMENTS -> draft.values.copy(fetalMovements = event.value)
        DraftField.URINE_PROTEIN -> draft.values.copy(urineProtein = event.value)
    }
    return state.copy(draft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
}

internal fun updatePatientDraft(state: AncState, event: AncEvent.PatientDraftChanged): AncState {
    if (state.savePhase.blocksEditing()) return state
    val draft = state.patientDraft ?: return state
    val values = when (event.field) {
        PatientDraftField.CASE_ID -> draft.values.copy(caseId = event.value)
        PatientDraftField.DISPLAY_NAME -> draft.values.copy(displayName = event.value)
        PatientDraftField.DATE_OF_BIRTH -> draft.values.copy(dateOfBirth = event.value)
        PatientDraftField.PHONE -> draft.values.copy(phone = event.value)
        PatientDraftField.AGE_TEXT -> draft.values.copy(ageText = event.value)
        PatientDraftField.ADDRESS_TEXT -> draft.values.copy(addressText = event.value)
        PatientDraftField.BLOOD_GROUP -> draft.values.copy(bloodGroupText = event.value)
        PatientDraftField.RISK_LEVEL -> draft.values.copy(riskLevel = event.value)
        PatientDraftField.GRAVIDA -> draft.values.copy(gravida = event.value)
        PatientDraftField.PARITY -> draft.values.copy(parity = event.value)
        PatientDraftField.ABORTION -> draft.values.copy(abortion = event.value)
        PatientDraftField.OBSTETRIC_NOTES -> draft.values.copy(obstetricNotes = event.value)
        PatientDraftField.CLINICAL_ATTENTION -> draft.values.copy(clinicalAttention = event.value)
        PatientDraftField.LMP_DATE -> draft.values.copy(lmpDate = event.value)
        PatientDraftField.DATING_METHOD -> draft.values.copy(datingMethod = event.value)
        PatientDraftField.TRANSFER_DATE -> draft.values.copy(transferDate = event.value)
        PatientDraftField.EMBRYO_AGE_DAYS -> draft.values.copy(embryoAgeDays = event.value)
        PatientDraftField.AGREED_EDD -> draft.values.copy(agreedEdd = event.value)
        PatientDraftField.SCAN_DATE -> draft.values.copy(scanDate = event.value)
        PatientDraftField.SCAN_GA_WEEKS -> draft.values.copy(scanGaWeeks = event.value)
        PatientDraftField.SCAN_GA_DAYS -> draft.values.copy(scanGaDays = event.value)
    }
    return state.copy(patientDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
}

internal fun updateHistoriesDraft(state: AncState, event: AncEvent.HistoriesDraftChanged): AncState {
    if (state.savePhase.blocksEditing()) return state
    val draft = state.historiesDraft ?: return state
    val values = when (event.field) {
        HistoriesDraftField.MEDICAL -> draft.values.copy(medicalHistory = event.value)
        HistoriesDraftField.SURGICAL -> draft.values.copy(surgicalHistory = event.value)
        HistoriesDraftField.FAMILY -> draft.values.copy(familyHistory = event.value)
        HistoriesDraftField.ALLERGY -> draft.values.copy(allergyHistory = event.value)
    }
    return state.copy(historiesDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
}

internal fun updatePregnancyDraft(state: AncState, event: AncEvent.PregnancyDraftChanged): AncState {
    if (state.savePhase.blocksEditing()) return state
    val draft = state.pregnancyDraft ?: return state
    val values = when (event.field) {
        PregnancyDraftField.DATE_TEXT -> draft.values.copy(dateText = event.value)
        PregnancyDraftField.OUTCOME_TEXT -> draft.values.copy(outcomeText = event.value)
        PregnancyDraftField.PREGNANCY_TYPE -> draft.values.copy(pregnancyType = event.value)
        PregnancyDraftField.OTHER_DETAILS -> draft.values.copy(otherDetails = event.value)
        PregnancyDraftField.PLACE_AND_DOCTOR -> draft.values.copy(placeAndDoctor = event.value)
        PregnancyDraftField.DELIVERY_BY -> draft.values.copy(deliveryBy = event.value)
        PregnancyDraftField.OUTCOME -> draft.values.copy(
            outcome = event.value,
            birthMethod = draft.values.birthMethod.takeIf {
                event.value == "LiveBirth" || event.value == "Stillbirth"
            }.orEmpty(),
            lossManagement = draft.values.lossManagement.takeIf {
                event.value == "EarlyPregnancyLoss"
            }.orEmpty(),
            terminationMethod = draft.values.terminationMethod.takeIf {
                event.value == "TerminationOfPregnancy"
            }.orEmpty(),
        )
        PregnancyDraftField.BIRTH_METHOD -> draft.values.copy(birthMethod = event.value)
        PregnancyDraftField.LOSS_MANAGEMENT -> draft.values.copy(lossManagement = event.value)
        PregnancyDraftField.TERMINATION_METHOD -> draft.values.copy(terminationMethod = event.value)
        PregnancyDraftField.CLINICIAN_ID -> draft.values.copy(clinicianId = event.value)
    }
    return state.copy(pregnancyDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
}

internal fun updateDatingDraft(state: AncState, event: AncEvent.DatingDraftChanged): AncState {
    if (state.savePhase.blocksEditing()) return state
    val draft = state.datingDraft ?: return state
    val values = applyDatingChange(draft.values, event.field, event.value)
    return state.copy(datingDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
}

internal fun updateUltrasoundDraft(state: AncState, event: AncEvent.UltrasoundDraftChanged): AncState {
    if (state.savePhase.blocksEditing()) return state
    val draft = state.ultrasoundDraft ?: return state
    val values = when (event.field) {
        UltrasoundDraftField.DATE -> draft.values.copy(date = event.value)
        UltrasoundDraftField.GA_WEEKS -> draft.values.copy(gaWeeks = event.value)
        UltrasoundDraftField.GA_DAYS -> draft.values.copy(gaDays = event.value)
        UltrasoundDraftField.FETAL_NUMBER -> draft.values.copy(fetalNumber = event.value)
        UltrasoundDraftField.PRESENTATION -> draft.values.copy(presentation = event.value)
        UltrasoundDraftField.PLACENTAL_SITE -> draft.values.copy(placentalSite = event.value)
        UltrasoundDraftField.AMNIOTIC_FLUID -> draft.values.copy(amnioticFluid = event.value)
        UltrasoundDraftField.FETAL_HEART_RATE -> draft.values.copy(fetalHeartRate = event.value)
        UltrasoundDraftField.BIOMETRY_BPD -> draft.values.copy(biometryBpd = event.value)
        UltrasoundDraftField.BIOMETRY_HC -> draft.values.copy(biometryHc = event.value)
        UltrasoundDraftField.BIOMETRY_AC -> draft.values.copy(biometryAc = event.value)
        UltrasoundDraftField.BIOMETRY_FL -> draft.values.copy(biometryFl = event.value)
        UltrasoundDraftField.ESTIMATED_FETAL_WEIGHT -> draft.values.copy(estimatedFetalWeight = event.value)
        UltrasoundDraftField.IMPRESSION -> draft.values.copy(impression = event.value)
        UltrasoundDraftField.LINKED_VISIT -> draft.values.copy(linkedVisitId = event.value)
        UltrasoundDraftField.IMAGE_URIS -> draft.values.copy(
            imageUris = if (event.value.isBlank()) emptyList() else event.value.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        )
    }
    return state.copy(ultrasoundDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
}

internal fun updateLabDraft(state: AncState, event: AncEvent.LabDraftChanged): AncState {
    if (state.savePhase.blocksEditing()) return state
    val draft = state.labDraft ?: return state
    val values = when (event.field) {
        LabDraftField.DATE -> draft.values.copy(date = event.value)
        LabDraftField.CATEGORY -> draft.values.copy(testCategory = event.value)
        LabDraftField.NAME -> draft.values.copy(testName = event.value)
        LabDraftField.RESULT -> draft.values.copy(result = event.value)
        LabDraftField.UNIT -> draft.values.copy(unit = event.value)
        LabDraftField.REFERENCE_RANGE -> draft.values.copy(referenceRange = event.value)
        LabDraftField.NOTES -> draft.values.copy(notes = event.value)
        LabDraftField.LAB_NAME -> draft.values.copy(labName = event.value)
    }
    return state.copy(labDraft = draft.copy(values = values, errors = emptySet()), savePhase = SavePhase.IDLE, saveError = null)
}

internal fun updateLabAbnormal(state: AncState, isAbnormal: Boolean): AncState {
    if (state.savePhase.blocksEditing()) return state
    val draft = state.labDraft ?: return state
    return state.copy(
        labDraft = draft.copy(values = draft.values.copy(isAbnormal = isAbnormal), errors = emptySet()),
        savePhase = SavePhase.IDLE,
        saveError = null,
    )
}

internal fun DemoVisit.toDraftValues(): DraftValues {
    val parsedBp = parseBloodPressure(bloodPressure)
    val parsedFindings = parseVisitFindings(findings)
    return DraftValues(
        date = date,
        bloodPressure = bloodPressure.orEmpty(),
        weight = weight.orEmpty(),
        findings = findings.orEmpty(),
        notes = notes.orEmpty(),
        linkedUltrasoundId = linkedUltrasoundId.orEmpty(),
        systolic = parsedBp?.systolic?.toString().orEmpty(),
        diastolic = parsedBp?.diastolic?.toString().orEmpty(),
        fundalHeight = parsedFindings.fundalHeight.orEmpty(),
        fetalHeartRate = parsedFindings.fetalHeartRate.orEmpty(),
        oedema = parsedFindings.oedema.orEmpty(),
        fetalMovements = parsedFindings.fetalMovements.orEmpty(),
        urineProtein = parsedFindings.urineProtein.orEmpty(),
    )
}

internal fun DemoPatient.toPatientDraftValues() = PatientDraftValues(
    caseId = caseId,
    displayName = displayName,
    dateOfBirth = dateOfBirth.orEmpty(),
    phone = phone.orEmpty(),
    ageText = ageText.orEmpty(),
    addressText = addressText.orEmpty(),
    bloodGroupText = bloodGroupText.orEmpty(),
    riskLevel = riskLevel.orEmpty(),
    gravida = gravida?.toString().orEmpty(),
    parity = parity?.toString().orEmpty(),
    abortion = abortion?.toString().orEmpty(),
    obstetricNotes = obstetricNotes.orEmpty(),
    clinicalAttention = clinicalAttention.orEmpty(),
    lmpDate = dating?.lmpDate.orEmpty(),
)

internal fun DemoPreviousPregnancy.toDraftValues() = PreviousPregnancyDraftValues(
    dateText = dateText.orEmpty(),
    outcomeText = outcomeText.orEmpty(),
    pregnancyType = pregnancyType.orEmpty(),
    otherDetails = otherDetails.orEmpty(),
    placeAndDoctor = placeAndDoctor.orEmpty(),
    deliveryBy = deliveryBy.orEmpty(),
    outcome = outcome.orEmpty(),
    birthMethod = birthMethod.orEmpty(),
    lossManagement = lossManagement.orEmpty(),
    terminationMethod = terminationMethod.orEmpty(),
    clinicianId = clinicianId,
)

internal fun DemoPatientDating.toDraftValues() = DatingDraftValues(
    lmpDate = lmpDate.orEmpty(),
    ultrasoundEdd = ultrasoundEdd.orEmpty(),
    agreedEdd = agreedEdd.orEmpty(),
    datingMethod = datingMethod.orEmpty(),
    transferDate = transferDate.orEmpty(),
    embryoAgeDays = embryoAgeDays?.toString().orEmpty(),
    notes = notes.orEmpty(),
)

internal fun DemoUltrasoundScan.toDraftValues() = UltrasoundDraftValues(
    date = date,
    gaWeeks = gestationalAgeWeeks?.toString().orEmpty(),
    gaDays = gestationalAgeDays?.toString().orEmpty(),
    fetalNumber = fetalNumber.orEmpty(),
    presentation = presentation.orEmpty(),
    placentalSite = placentalSite.orEmpty(),
    amnioticFluid = amnioticFluid.orEmpty(),
    fetalHeartRate = fetalHeartRate.orEmpty(),
    biometryBpd = biometryBpd.orEmpty(),
    biometryHc = biometryHc.orEmpty(),
    biometryAc = biometryAc.orEmpty(),
    biometryFl = biometryFl.orEmpty(),
    estimatedFetalWeight = estimatedFetalWeight.orEmpty(),
    impression = impression.orEmpty(),
    linkedVisitId = linkedVisitId.orEmpty(),
    imageUris = imageUris,
)

internal fun DemoLabInvestigation.toDraftValues() = LabDraftValues(
    date = date,
    testCategory = testCategory,
    testName = testName,
    result = result,
    unit = unit.orEmpty(),
    referenceRange = referenceRange.orEmpty(),
    isAbnormal = isAbnormal,
    notes = notes.orEmpty(),
    labName = labName.orEmpty(),
)
