package com.sherifradwan.ancnative.state

import com.sherifradwan.ancnative.model.parseBloodPressure
import com.sherifradwan.ancnative.model.parseVisitFindings
import com.sherifradwan.ancnative.storage.DraftEditorKind
import com.sherifradwan.ancnative.storage.DraftRecord
import com.sherifradwan.ancnative.storage.DraftScreenKind
import com.sherifradwan.ancnative.storage.NEW_PATIENT_DRAFT_KEY
import com.sherifradwan.ancnative.storage.VaultState
import java.util.UUID

data class DraftSlot(
    val patientId: String,
    val kind: DraftScreenKind,
)

fun activeDraftSlot(state: AncState): DraftSlot? {
    val visitDraft = state.draft
    if (visitDraft != null) {
        return DraftSlot(visitDraft.patientId, DraftScreenKind.VISIT)
    }
    val patientDraft = state.patientDraft
    if (patientDraft != null) {
        val patientId = if (patientDraft.baseRevision == null) NEW_PATIENT_DRAFT_KEY else patientDraft.patientId
        return DraftSlot(patientId, DraftScreenKind.PATIENT_PROFILE)
    }
    val historiesDraft = state.historiesDraft
    if (historiesDraft != null) {
        return DraftSlot(historiesDraft.patientId, DraftScreenKind.HISTORIES)
    }
    val pregnancyDraft = state.pregnancyDraft
    if (pregnancyDraft != null) {
        return DraftSlot(pregnancyDraft.patientId, DraftScreenKind.PREVIOUS_PREGNANCY)
    }
    val statusDraft = state.statusDraft
    if (statusDraft != null) {
        return DraftSlot(statusDraft.patientId, DraftScreenKind.PATIENT_STATUS)
    }
    val datingDraft = state.datingDraft
    if (datingDraft != null) {
        return DraftSlot(datingDraft.patientId, DraftScreenKind.DATING)
    }
    val ultrasoundDraft = state.ultrasoundDraft
    if (ultrasoundDraft != null) {
        return DraftSlot(ultrasoundDraft.patientId, DraftScreenKind.ULTRASOUND)
    }
    val labDraft = state.labDraft
    if (labDraft != null) {
        return DraftSlot(labDraft.patientId, DraftScreenKind.LAB)
    }
    val labBatchDraft = state.labBatchDraft
    if (labBatchDraft != null) return DraftSlot(labBatchDraft.patientId, DraftScreenKind.LAB_BATCH)
    val medicationDraft = state.medicationDraft
    if (medicationDraft != null) {
        return DraftSlot(medicationDraft.patientId, DraftScreenKind.MEDICATION)
    }
    return null
}

fun draftRecordFor(state: AncState, nowMillis: Long): DraftRecord? {
    val baseRevision = activeEditorBaseRevision(state)

    state.draft?.let { draft ->
        if (!draft.isDirty) return null
        return DraftRecord(
            patientId = draft.patientId,
            screenKind = DraftScreenKind.VISIT,
            editorKind = if (draft.visitId == null) DraftEditorKind.CREATE else DraftEditorKind.EDIT,
            recordId = draft.visitId,
            baseRevision = baseRevision,
            fields = mapOf(
                "date" to draft.values.date,
                "bloodPressure" to draft.values.bloodPressure,
                "weight" to draft.values.weight,
                "findings" to draft.values.findings,
                "notes" to draft.values.notes,
                "linkedUltrasoundId" to draft.values.linkedUltrasoundId,
                "systolic" to draft.values.systolic,
                "diastolic" to draft.values.diastolic,
                "fundalHeight" to draft.values.fundalHeight,
                "oedema" to draft.values.oedema,
                "fetalMovements" to draft.values.fetalMovements,
                "urineProtein" to draft.values.urineProtein,
            ),
            updatedAtMillis = nowMillis,
        )
    }

    state.patientDraft?.let { draft ->
        if (!draft.isDirty) return null
        val isNew = draft.baseRevision == null
        val patientId = if (isNew) NEW_PATIENT_DRAFT_KEY else draft.patientId
        val fields = mapOf(
            "caseId" to draft.values.caseId,
            "displayName" to draft.values.displayName,
            "dateOfBirth" to draft.values.dateOfBirth,
            "phone" to draft.values.phone,
            "ageText" to draft.values.ageText,
            "addressText" to draft.values.addressText,
            "bloodGroupText" to draft.values.bloodGroupText,
            "riskLevel" to draft.values.riskLevel,
            "gravida" to draft.values.gravida,
            "parity" to draft.values.parity,
            "abortion" to draft.values.abortion,
            "obstetricNotes" to draft.values.obstetricNotes,
            "clinicalAttention" to draft.values.clinicalAttention,
        )
        return DraftRecord(
            patientId = patientId,
            screenKind = DraftScreenKind.PATIENT_PROFILE,
            editorKind = if (isNew) DraftEditorKind.CREATE else DraftEditorKind.EDIT,
            recordId = null,
            baseRevision = baseRevision,
            fields = fields,
            updatedAtMillis = nowMillis,
        )
    }

    state.historiesDraft?.let { draft ->
        if (!draft.isDirty) return null
        return DraftRecord(
            patientId = draft.patientId,
            screenKind = DraftScreenKind.HISTORIES,
            editorKind = DraftEditorKind.EDIT,
            recordId = null,
            baseRevision = baseRevision,
            fields = mapOf(
                "medicalHistory" to draft.values.medicalHistory,
                "surgicalHistory" to draft.values.surgicalHistory,
                "familyHistory" to draft.values.familyHistory,
                "allergyHistory" to draft.values.allergyHistory,
            ),
            updatedAtMillis = nowMillis,
        )
    }

    state.pregnancyDraft?.let { draft ->
        if (!draft.isDirty) return null
        val isNew = draft.basePregnancyRevision == null
        return DraftRecord(
            patientId = draft.patientId,
            screenKind = DraftScreenKind.PREVIOUS_PREGNANCY,
            editorKind = if (isNew) DraftEditorKind.CREATE else DraftEditorKind.EDIT,
            recordId = draft.pregnancyId,
            baseRevision = baseRevision,
            fields = mapOf(
                "dateText" to draft.values.dateText,
                "outcomeText" to draft.values.outcomeText,
                "pregnancyType" to draft.values.pregnancyType,
                "otherDetails" to draft.values.otherDetails,
                "placeAndDoctor" to draft.values.placeAndDoctor,
                "deliveryBy" to draft.values.deliveryBy,
                "outcome" to draft.values.outcome,
                "birthMethod" to draft.values.birthMethod,
                "lossManagement" to draft.values.lossManagement,
                "terminationMethod" to draft.values.terminationMethod,
                "clinicianId" to draft.values.clinicianId,
            ),
            updatedAtMillis = nowMillis,
        )
    }

    state.statusDraft?.let { draft ->
        if (!draft.isDirty) return null
        return DraftRecord(
            patientId = draft.patientId,
            screenKind = DraftScreenKind.PATIENT_STATUS,
            editorKind = DraftEditorKind.EDIT,
            recordId = null,
            baseRevision = baseRevision,
            fields = mapOf(
                "targetStatus" to draft.targetStatus,
                "reason" to draft.reason,
            ),
            updatedAtMillis = nowMillis,
        )
    }

    state.datingDraft?.let { draft ->
        if (!draft.isDirty) return null
        return DraftRecord(
            patientId = draft.patientId,
            screenKind = DraftScreenKind.DATING,
            editorKind = DraftEditorKind.EDIT,
            recordId = null,
            baseRevision = baseRevision,
            fields = mapOf(
                "lmpDate" to draft.values.lmpDate,
                "ultrasoundEdd" to draft.values.ultrasoundEdd,
                "agreedEdd" to draft.values.agreedEdd,
                "datingMethod" to draft.values.datingMethod,
                "transferDate" to draft.values.transferDate,
                "embryoAgeDays" to draft.values.embryoAgeDays,
                "notes" to draft.values.notes,
            ),
            updatedAtMillis = nowMillis,
        )
    }

    state.ultrasoundDraft?.let { draft ->
        if (!draft.isDirty) return null
        val isNew = draft.baseScanRevision == null
        return DraftRecord(
            patientId = draft.patientId,
            screenKind = DraftScreenKind.ULTRASOUND,
            editorKind = if (isNew) DraftEditorKind.CREATE else DraftEditorKind.EDIT,
            recordId = draft.scanId,
            baseRevision = baseRevision,
            fields = mapOf(
                "date" to draft.values.date,
                "gaWeeks" to draft.values.gaWeeks,
                "gaDays" to draft.values.gaDays,
                "fetalNumber" to draft.values.fetalNumber,
                "presentation" to draft.values.presentation,
                "placentalSite" to draft.values.placentalSite,
                "amnioticFluid" to draft.values.amnioticFluid,
                "fetalHeartRate" to draft.values.fetalHeartRate,
                "biometryBpd" to draft.values.biometryBpd,
                "biometryHc" to draft.values.biometryHc,
                "biometryAc" to draft.values.biometryAc,
                "biometryFl" to draft.values.biometryFl,
                "estimatedFetalWeight" to draft.values.estimatedFetalWeight,
                "impression" to draft.values.impression,
                "linkedVisitId" to draft.values.linkedVisitId,
            ),
            updatedAtMillis = nowMillis,
        )
    }

    state.labDraft?.let { draft ->
        if (!draft.isDirty) return null
        val isNew = draft.baseLabRevision == null
        return DraftRecord(
            patientId = draft.patientId,
            screenKind = DraftScreenKind.LAB,
            editorKind = if (isNew) DraftEditorKind.CREATE else DraftEditorKind.EDIT,
            recordId = draft.labId,
            baseRevision = baseRevision,
            fields = mapOf(
                "date" to draft.values.date,
                "testCategory" to draft.values.testCategory,
                "testName" to draft.values.testName,
                "result" to draft.values.result,
                "unit" to draft.values.unit,
                "referenceRange" to draft.values.referenceRange,
                "isAbnormal" to draft.values.isAbnormal.toString(),
                "notes" to draft.values.notes,
                "labName" to draft.values.labName,
            ),
            updatedAtMillis = nowMillis,
        )
    }

    state.labBatchDraft?.let { draft ->
        if (draft.items.isEmpty()) return null
        return DraftRecord(
            patientId = draft.patientId,
            screenKind = DraftScreenKind.LAB_BATCH,
            editorKind = DraftEditorKind.CREATE,
            recordId = null,
            baseRevision = draft.basePatientRevision,
            fields = mapOf(
                "date" to draft.date,
                "labName" to draft.labName.orEmpty(),
                "cursor" to draft.cursor.toString(),
                "tab" to draft.tab.name,
                "items" to encodeLabBatchItems(draft.items),
            ),
            updatedAtMillis = nowMillis,
        )
    }

    state.medicationDraft?.let { draft ->
        if (!draft.isDirty) return null
        val isNew = draft.baseMedicationRevision == null
        return DraftRecord(
            patientId = draft.patientId,
            screenKind = DraftScreenKind.MEDICATION,
            editorKind = if (isNew) DraftEditorKind.CREATE else DraftEditorKind.EDIT,
            recordId = draft.medicationId,
            baseRevision = baseRevision,
            fields = mapOf(
                "name" to draft.values.name,
                "dosage" to draft.values.dosage,
                "route" to draft.values.route,
                "frequency" to draft.values.frequency,
                "startDate" to draft.values.startDate,
                "endDate" to draft.values.endDate,
                "status" to draft.values.status,
                "isProphylaxis" to draft.values.isProphylaxis.toString(),
                "notes" to draft.values.notes,
                "isNonCompliant" to draft.values.isNonCompliant.toString(),
                "adverseNotes" to draft.values.adverseNotes,
            ),
            updatedAtMillis = nowMillis,
        )
    }

    return null
}

/**
 * The pregnancy slot is shared by every pregnancy of a patient, so a stored pregnancy record belongs to the open
 * editor only when it names the same pregnancy. Every other kind is identified by its slot alone.
 */
fun draftRecordBelongsToOpenEditor(state: AncState, record: DraftRecord): Boolean =
    record.screenKind != DraftScreenKind.PREVIOUS_PREGNANCY || record.recordId == state.pregnancyDraft?.pregnancyId

fun applyDraftRecord(state: AncState, record: DraftRecord): AncState {
    val slot = activeDraftSlot(state) ?: return state
    if (slot != DraftSlot(record.patientId, record.screenKind)) return state
    if (!draftRecordBelongsToOpenEditor(state, record)) return state
    val newSessionId = UUID.randomUUID().toString()

    return when (record.screenKind) {
        DraftScreenKind.VISIT -> {
            val d = state.draft ?: return state
            val v = d.values
            val bp = record.fields["bloodPressure"] ?: v.bloodPressure
            val parsedBp = parseBloodPressure(bp)
            val findings = record.fields["findings"] ?: v.findings
            val parsedFindings = parseVisitFindings(findings)
            state.copy(
                draft = d.copy(
                    editorSessionId = newSessionId,
                    values = v.copy(
                        date = record.fields["date"] ?: v.date,
                        bloodPressure = bp,
                        weight = record.fields["weight"] ?: v.weight,
                        findings = findings,
                        notes = record.fields["notes"] ?: v.notes,
                        linkedUltrasoundId = record.fields["linkedUltrasoundId"] ?: v.linkedUltrasoundId,
                        systolic = record.fields["systolic"] ?: (parsedBp?.systolic?.toString() ?: v.systolic),
                        diastolic = record.fields["diastolic"] ?: (parsedBp?.diastolic?.toString() ?: v.diastolic),
                        fundalHeight = record.fields["fundalHeight"] ?: (parsedFindings.fundalHeight ?: v.fundalHeight),
                        oedema = record.fields["oedema"] ?: (parsedFindings.oedema ?: v.oedema),
                        fetalMovements = record.fields["fetalMovements"] ?: (parsedFindings.fetalMovements ?: v.fetalMovements),
                        urineProtein = record.fields["urineProtein"] ?: (parsedFindings.urineProtein ?: v.urineProtein),
                    ),
                    errors = emptySet(),
                ),
            )
        }
        DraftScreenKind.PATIENT_PROFILE -> {
            val d = state.patientDraft ?: return state
            val v = d.values
            state.copy(
                patientDraft = d.copy(
                    editorSessionId = newSessionId,
                    values = v.copy(
                        caseId = record.fields["caseId"] ?: v.caseId,
                        displayName = record.fields["displayName"] ?: v.displayName,
                        dateOfBirth = record.fields["dateOfBirth"] ?: v.dateOfBirth,
                        phone = record.fields["phone"] ?: v.phone,
                        ageText = record.fields["ageText"] ?: v.ageText,
                        addressText = record.fields["addressText"] ?: v.addressText,
                        bloodGroupText = record.fields["bloodGroupText"] ?: v.bloodGroupText,
                        riskLevel = record.fields["riskLevel"] ?: v.riskLevel,
                        gravida = record.fields["gravida"] ?: v.gravida,
                        parity = record.fields["parity"] ?: v.parity,
                        abortion = record.fields["abortion"] ?: v.abortion,
                        obstetricNotes = record.fields["obstetricNotes"] ?: v.obstetricNotes,
                        clinicalAttention = record.fields["clinicalAttention"] ?: v.clinicalAttention,
                    ),
                    errors = emptySet(),
                ),
            )
        }
        DraftScreenKind.HISTORIES -> {
            val d = state.historiesDraft ?: return state
            val v = d.values
            state.copy(
                historiesDraft = d.copy(
                    editorSessionId = newSessionId,
                    values = v.copy(
                        medicalHistory = record.fields["medicalHistory"] ?: v.medicalHistory,
                        surgicalHistory = record.fields["surgicalHistory"] ?: v.surgicalHistory,
                        familyHistory = record.fields["familyHistory"] ?: v.familyHistory,
                        allergyHistory = record.fields["allergyHistory"] ?: v.allergyHistory,
                    ),
                    errors = emptySet(),
                ),
            )
        }
        DraftScreenKind.PREVIOUS_PREGNANCY -> {
            val d = state.pregnancyDraft ?: return state
            val v = d.values
            state.copy(
                pregnancyDraft = d.copy(
                    editorSessionId = newSessionId,
                    values = v.copy(
                        dateText = record.fields["dateText"] ?: v.dateText,
                        outcomeText = record.fields["outcomeText"] ?: v.outcomeText,
                        pregnancyType = record.fields["pregnancyType"] ?: v.pregnancyType,
                        otherDetails = record.fields["otherDetails"] ?: v.otherDetails,
                        placeAndDoctor = record.fields["placeAndDoctor"] ?: v.placeAndDoctor,
                        deliveryBy = record.fields["deliveryBy"] ?: v.deliveryBy,
                        outcome = record.fields["outcome"] ?: v.outcome,
                        birthMethod = record.fields["birthMethod"] ?: v.birthMethod,
                        lossManagement = record.fields["lossManagement"] ?: v.lossManagement,
                        terminationMethod = record.fields["terminationMethod"] ?: v.terminationMethod,
                        clinicianId = record.fields["clinicianId"] ?: v.clinicianId,
                    ),
                    errors = emptySet(),
                ),
            )
        }
        DraftScreenKind.PATIENT_STATUS -> {
            val d = state.statusDraft ?: return state
            state.copy(
                statusDraft = d.copy(
                    editorSessionId = newSessionId,
                    targetStatus = record.fields["targetStatus"] ?: d.targetStatus,
                    reason = record.fields["reason"] ?: d.reason,
                    errors = emptySet(),
                ),
            )
        }
        DraftScreenKind.DATING -> {
            val d = state.datingDraft ?: return state
            val v = d.values
            state.copy(
                datingDraft = d.copy(
                    editorSessionId = newSessionId,
                    values = v.copy(
                        lmpDate = record.fields["lmpDate"] ?: v.lmpDate,
                        ultrasoundEdd = record.fields["ultrasoundEdd"] ?: v.ultrasoundEdd,
                        agreedEdd = record.fields["agreedEdd"] ?: v.agreedEdd,
                        datingMethod = record.fields["datingMethod"] ?: v.datingMethod,
                        transferDate = record.fields["transferDate"] ?: v.transferDate,
                        embryoAgeDays = record.fields["embryoAgeDays"] ?: v.embryoAgeDays,
                        notes = record.fields["notes"] ?: v.notes,
                    ),
                    errors = emptySet(),
                ),
            )
        }
        DraftScreenKind.ULTRASOUND -> {
            val d = state.ultrasoundDraft ?: return state
            val v = d.values
            state.copy(
                ultrasoundDraft = d.copy(
                    editorSessionId = newSessionId,
                    values = v.copy(
                        date = record.fields["date"] ?: v.date,
                        gaWeeks = record.fields["gaWeeks"] ?: v.gaWeeks,
                        gaDays = record.fields["gaDays"] ?: v.gaDays,
                        fetalNumber = record.fields["fetalNumber"] ?: v.fetalNumber,
                        presentation = record.fields["presentation"] ?: v.presentation,
                        placentalSite = record.fields["placentalSite"] ?: v.placentalSite,
                        amnioticFluid = record.fields["amnioticFluid"] ?: v.amnioticFluid,
                        fetalHeartRate = record.fields["fetalHeartRate"] ?: v.fetalHeartRate,
                        biometryBpd = record.fields["biometryBpd"] ?: v.biometryBpd,
                        biometryHc = record.fields["biometryHc"] ?: v.biometryHc,
                        biometryAc = record.fields["biometryAc"] ?: v.biometryAc,
                        biometryFl = record.fields["biometryFl"] ?: v.biometryFl,
                        estimatedFetalWeight = record.fields["estimatedFetalWeight"] ?: v.estimatedFetalWeight,
                        impression = record.fields["impression"] ?: v.impression,
                        linkedVisitId = record.fields["linkedVisitId"] ?: v.linkedVisitId,
                        imageUris = v.imageUris,
                    ),
                    errors = emptySet(),
                ),
            )
        }
        DraftScreenKind.LAB -> {
            val d = state.labDraft ?: return state
            val v = d.values
            val isAbnormal = record.fields["isAbnormal"]?.let { it == "true" } ?: v.isAbnormal
            state.copy(
                labDraft = d.copy(
                    editorSessionId = newSessionId,
                    values = v.copy(
                        date = record.fields["date"] ?: v.date,
                        testCategory = record.fields["testCategory"] ?: v.testCategory,
                        testName = record.fields["testName"] ?: v.testName,
                        result = record.fields["result"] ?: v.result,
                        unit = record.fields["unit"] ?: v.unit,
                        referenceRange = record.fields["referenceRange"] ?: v.referenceRange,
                        isAbnormal = isAbnormal,
                        notes = record.fields["notes"] ?: v.notes,
                        labName = record.fields["labName"] ?: v.labName,
                    ),
                    errors = emptySet(),
                ),
            )
        }
        DraftScreenKind.LAB_BATCH -> {
            val d = state.labBatchDraft ?: return state
            state.copy(
                labBatchDraft = d.copy(
                    editorSessionId = newSessionId,
                    date = record.fields["date"] ?: d.date,
                    labName = record.fields["labName"]?.ifBlank { null } ?: d.labName,
                    items = record.fields["items"]?.let(::decodeLabBatchItems) ?: d.items,
                    cursor = record.fields["cursor"]?.toIntOrNull() ?: d.cursor,
                    tab = record.fields["tab"]?.let { name -> LabBatchTab.entries.firstOrNull { it.name == name } } ?: d.tab,
                    errors = emptySet(),
                ),
            )
        }
        DraftScreenKind.MEDICATION -> {
            val d = state.medicationDraft ?: return state
            val v = d.values
            val isProphylaxis = record.fields["isProphylaxis"]?.let { it == "true" } ?: v.isProphylaxis
            val isNonCompliant = record.fields["isNonCompliant"]?.let { it == "true" } ?: v.isNonCompliant
            state.copy(
                medicationDraft = d.copy(
                    editorSessionId = newSessionId,
                    values = v.copy(
                        name = record.fields["name"] ?: v.name,
                        dosage = record.fields["dosage"] ?: v.dosage,
                        route = record.fields["route"] ?: v.route,
                        frequency = record.fields["frequency"] ?: v.frequency,
                        startDate = record.fields["startDate"] ?: v.startDate,
                        endDate = record.fields["endDate"] ?: v.endDate,
                        status = record.fields["status"] ?: v.status,
                        isProphylaxis = isProphylaxis,
                        notes = record.fields["notes"] ?: v.notes,
                        isNonCompliant = isNonCompliant,
                        adverseNotes = record.fields["adverseNotes"] ?: v.adverseNotes,
                    ),
                    errors = emptySet(),
                ),
            )
        }
    }
}

private fun isSlotDirty(state: AncState, slot: DraftSlot): Boolean {
    return when (slot.kind) {
        DraftScreenKind.VISIT -> state.draft?.let { it.patientId == slot.patientId && it.isDirty } == true
        DraftScreenKind.PATIENT_PROFILE -> state.patientDraft?.let {
            val patientId = if (it.baseRevision == null) NEW_PATIENT_DRAFT_KEY else it.patientId
            patientId == slot.patientId && it.isDirty
        } == true
        DraftScreenKind.HISTORIES -> state.historiesDraft?.let { it.patientId == slot.patientId && it.isDirty } == true
        DraftScreenKind.PREVIOUS_PREGNANCY -> state.pregnancyDraft?.let { it.patientId == slot.patientId && it.isDirty } == true
        DraftScreenKind.PATIENT_STATUS -> state.statusDraft?.let { it.patientId == slot.patientId && it.isDirty } == true
        DraftScreenKind.DATING -> state.datingDraft?.let { it.patientId == slot.patientId && it.isDirty } == true
        DraftScreenKind.ULTRASOUND -> state.ultrasoundDraft?.let { it.patientId == slot.patientId && it.isDirty } == true
        DraftScreenKind.LAB -> state.labDraft?.let { it.patientId == slot.patientId && it.isDirty } == true
        DraftScreenKind.MEDICATION -> state.medicationDraft?.let { it.patientId == slot.patientId && it.isDirty } == true
        DraftScreenKind.LAB_BATCH -> state.labBatchDraft?.let { it.patientId == slot.patientId && it.items.isNotEmpty() } == true
    }
}

fun closedDraftSlot(before: AncState, after: AncState): DraftSlot? {
    if (before.vaultState !is VaultState.Unlocked || after.vaultState !is VaultState.Unlocked) {
        return null
    }
    val beforeSlot = activeDraftSlot(before) ?: return null
    if (!isSlotDirty(before, beforeSlot)) {
        return null
    }
    val afterSlot = activeDraftSlot(after)
    if (beforeSlot != afterSlot) {
        return beforeSlot
    }
    if (!isSlotDirty(after, beforeSlot)) {
        return beforeSlot
    }
    return null
}

// The batch carries a whole list inside one draft field. Unit/record separators keep it unambiguous; a value that
// contains one of them is flattened rather than allowed to corrupt the list.
private const val LAB_BATCH_FIELD_SEPARATOR = "\u001F"
private const val LAB_BATCH_ITEM_SEPARATOR = "\n"

fun encodeLabBatchItems(items: List<LabBatchItem>): String = items.joinToString(LAB_BATCH_ITEM_SEPARATOR) { item ->
    listOf(
        item.labId, item.testName, item.category, item.unit, item.result,
        item.referenceRange, item.isAbnormal.toString(), item.notes,
    ).joinToString(LAB_BATCH_FIELD_SEPARATOR) { value ->
        value.replace(LAB_BATCH_FIELD_SEPARATOR, " ").replace(LAB_BATCH_ITEM_SEPARATOR, " ")
    }
}

fun decodeLabBatchItems(raw: String): List<LabBatchItem> = raw
    .split(LAB_BATCH_ITEM_SEPARATOR)
    .filter { it.isNotBlank() }
    .mapNotNull { line ->
        val parts = line.split(LAB_BATCH_FIELD_SEPARATOR)
        if (parts.size < 8) return@mapNotNull null
        LabBatchItem(
            labId = parts[0],
            testName = parts[1],
            category = parts[2],
            unit = parts[3],
            result = parts[4],
            referenceRange = parts[5],
            isAbnormal = parts[6].toBoolean(),
            notes = parts[7],
        )
    }
