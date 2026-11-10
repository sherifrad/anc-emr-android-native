# Implementation Readiness Audit Report: Medication Section Revision (Stage 1)

**Target Document:** [`medication_section_revision.md`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/medication_section_revision.md#L215-L372) (Lines 215–372)  
**Standard Applied:** [`implementation-readiness-standard.md`](file:///Users/sherifgamalradwan/.claude/plugins/cache/egm/egm/0.1.1/resources/document_standards/implementation-readiness-standard.md)  
**Auditor Persona:** [`codebase_details.md`](file:///Users/sherifgamalradwan/.claude/plugins/cache/egm/egm/0.1.1/agents/codebase_details.md) (Mode: REVIEW)  
**Repository Verified:** `/Users/sherifgamalradwan/anc-emr-android-native`

---

## Executive Summary

The proposed design in lines 215–372 establishes a sound architectural direction by eliminating the full-screen route swap and unifying the medication form into an inline `ModalBottomSheet`. However, when audited against the **Zero-Decision Rule** and the actual implementation of [`anc-emr-android-native`](file:///Users/sherifgamalradwan/anc-emr-android-native), the plan contains several critical gaps:
1. It mandates structural validation rules on `ReplaceMedicationCommand` that cannot execute because the required input property is omitted from the command payload.
2. It underspecifies the state and coordinator payloads needed to execute atomic undo and rollback for replaced medications.
3. The phasing plan leaves intermediate stages in an uncompilable state.
4. Error propagation from `ClinicalCommandFailure` through `SaveUiError` to the UI is incomplete and leaves state representation choices to the implementer.

---

## Findings

### 1. 🔴 Blocker: `ReplaceMedicationCommand.isStructurallyValid()` References Property Omitted from Command
* **Affected Scope:** [`medication_section_revision.md:244`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/medication_section_revision.md#L244), [`ClinicalCommands.kt:75-87`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/ClinicalCommands.kt#L75-L87)
* **Discrepancy Description & Rationale:**
  Line 244 specifies:
  > "...`ReplaceMedicationCommand` with: operation id, patient id, expected patient revision, the old medication id with its expected revision and the stop date, and the new medication's id and fields... `isStructurallyValid` checks both ids differ, the stop date is a valid ISO date and not before the old start date..."
  
  In [`ClinicalCommands.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/ClinicalCommands.kt#L85), `isStructurallyValid()` is a pure, zero-argument member function (`fun isStructurallyValid(): Boolean`). It has no access to the database or external state. If `ReplaceMedicationCommand` only stores `oldMedicationId`, `expectedRevision`, and `stopDate`, it does not possess the old row's `startDate`. Consequently, evaluating whether `stopDate` is "not before the old start date" inside `isStructurallyValid()` is computationally impossible unless `oldStartDate: String` is explicitly added to `ReplaceMedicationCommand`. An implementer would have to choose whether to omit the check from structural validation or alter the command signature.

---

### 2. 🔴 Blocker: Inability to Construct `UndoMedicationChangeCommand` from `offerMedicationChangeUndo`
* **Affected Scope:** [`medication_section_revision.md:244`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/medication_section_revision.md#L244), [`medication_section_revision.md:269-275`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/medication_section_revision.md#L269-L275), [`UndoCoordinator.kt:179-213`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/UndoCoordinator.kt#L179-L213)
* **Discrepancy Description & Rationale:**
  Line 274 states:
  > "Add `offerMedicationChangeUndo(patientId, receipt, command)` that builds an `UndoMedicationChangeCommand` and arms it with `armUndo`, as `offerSaveUndo` does for a create."
  
  Line 244 states:
  > "`UndoMedicationChangeCommand` ... with the old row's id and revision, the earlier status and end date to restore, an optional new row id with its revision and content hash to delete, and a fingerprint."
  
  When a drug is stopped via `SaveMedicationCommand` or replaced via `ReplaceMedicationCommand`, the command passed to `offerMedicationChangeUndo` only carries the *new* state (`status = Discontinued`, `endDate = today`). Neither `command` nor `receipt` contains the row's *earlier* status (whether it was `Active` or `Patient non-compliant`), its *earlier* `endDate` (which may have been a custom date or null), or the old row's `contentHash`.
  
  Furthermore, while line 151 and line 249 require that `undoMedicationChange` "checks the revisions and content hash of every row it touches," line 244 omits `oldContentHash` from the definition of `UndoMedicationChangeCommand` (only listing "an optional new row id with its revision and content hash to delete").
  
  `offerMedicationChangeUndo(patientId, receipt, command)` cannot construct a valid `UndoMedicationChangeCommand` without the pre-change values (e.g., `previousRecord: DemoMedicationRecord` or `original: MedicationDraftValues`).

---

### 3. 🔴 Blocker: Non-Buildable Intermediate Checkpoints in Phasing
* **Affected Scope:** [`medication_section_revision.md:217-224`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/medication_section_revision.md#L217-L224), [`AncApp.kt:238`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt#L238), [`MedicationEditor.kt:70`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt#L70)
* **Discrepancy Description & Rationale:**
  The "Order of work" defines:
  > "3. State changes (draft without route, hook, back handling)."  
  > "4. UI (form, sheet, picker, tab, card, header)."  
  > "5. Delete the old editor, rewrite the tests that named the old route, run the full test sets."
  
  The Implementation Readiness Standard mandates: *"Each phase must leave the codebase in a stable, buildable, and testable state."*
  
  In Step 3, deleting `AncRoute.MedicationEditor` from [`AncState.kt:53`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt#L53) will immediately break compilation in [`AncApp.kt:238`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt#L238), [`MedicationEditor.kt:70`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt#L70), [`AncStateTest.kt:296`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/AncStateTest.kt#L296), [`AncDeleteUndoStateTest.kt:40`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/AncDeleteUndoStateTest.kt#L40), and [`DraftRecoveryTest.kt:60`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/DraftRecoveryTest.kt#L60). Leaving deletion of `MedicationEditor.kt` and test rewrites to Step 5 leaves the build broken throughout Steps 3 and 4.

---

### 4. 🟡 Major: Underspecified Repository Execution and Transaction Rollback Invariant
* **Affected Scope:** [`medication_section_revision.md:249`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/medication_section_revision.md#L249), [`RecordsRepository.kt:337-368, 1501-1523`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt#L337-L368), [`RecordsDao.kt:460-475`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDao.kt#L460-L475)
* **Discrepancy Description & Rationale:**
  Line 249 describes `replaceMedication`:
  > "...update the old row to `Discontinued` with the stop date using `dao.updateMedication` and its expected revision; insert the new row at revision 1; call `dao.incrementPatientRevision` once; write one `ClinicalCommandReceiptEntity`."
  
  In [`RecordsDao.kt:460-475`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDao.kt#L460-L475), `updateMedication` requires all 14 columns of the entity:
  ```kotlin
  suspend fun updateMedication(
      medicationId: String, patientId: String, expectedRevision: Long,
      name: String, dosage: String, route: String, frequency: String,
      startDate: String, endDate: String?, status: String, notes: String?,
      isProphylaxis: Boolean, isNonCompliant: Boolean, adverseEffects: String?
  ): Int
  ```
  Because `ReplaceMedicationCommand` does not supply the old row's unchanged clinical properties (`name`, `dosage`, `route`, `notes`, `isProphylaxis`, etc.), `replaceMedication` must explicitly load the existing row (`val old = dao.medication(command.oldMedicationId)`). The plan omits this step and omits the failure checks:
  - `old == null -> ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.RECORD_NOT_FOUND)`
  - `old.patientId != command.patientId -> ClinicalCommandOutcome.Rejected(ClinicalCommandFailure.CROSS_PATIENT_WRITE)`
  
  Crucially, inside Room's [`database.withTransaction`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt#L338), returning a `ClinicalCommandOutcome.Rejected` after a successful `dao.updateMedication` will **commit** the update rather than roll it back. To ensure atomicity across both rows, any failure occurring after the first write (such as a failed `dao.incrementPatientRevision` or failed insert) must `throw ClinicalCommandAbort(failure)` (as implemented in [`RecordsRepository.kt:1522`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt#L1522)). The plan does not document this invariant.

---

### 5. 🟡 Major: State Propagation Gap for Granular `ClinicalCommandFailure` in `MedicationSheet`
* **Affected Scope:** [`medication_section_revision.md:154-159, 263-270, 314, 344`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/medication_section_revision.md#L154-L159), [`AncState.kt:195-204`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt#L195-L204), [`AncViewModel.kt:1028-1042`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/AncViewModel.kt#L1028-L1042)
* **Discrepancy Description & Rationale:**
  Lines 154–159 and line 344 state that when a save is rejected:
  > "The sheet shows one message that names the cause, mapped from `ClinicalCommandFailure`: for example 'This patient's record changed since you opened it' for an outdated patient or medication revision... 'This medication was removed' for a missing record..."
  
  In the codebase, `AncViewModel` communicates save failures to `AncState` exclusively by dispatching `AncEvent.SaveFailed(token, failure.toUiError(), unresolved)` ([`AncViewModel.kt:806`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/AncViewModel.kt#L806)).
  [`ClinicalCommandFailure.toUiError()`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/AncViewModel.kt#L1028) maps `RECORD_NOT_FOUND`, `PATIENT_NOT_FOUND`, and `INVALID_COMMAND` indiscriminately to `SaveUiError.STORAGE_ERROR`, and maps `STALE_REVISION`, `STALE_PATIENT_REVISION`, and `RECORD_CONFLICT` to `SaveUiError.STALE_OR_CONFLICT`.
  
  `AncState` only stores `saveError: SaveUiError?` ([`AncState.kt:195`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt#L195)). The plan modifies neither `SaveUiError`, `AncEvent.SaveFailed`, nor `AncState`/`MedicationDraft` to hold `ClinicalCommandFailure` or an explicit error message. Therefore, `MedicationSheet` cannot distinguish `RECORD_NOT_FOUND` from generic `STORAGE_ERROR`. The mechanism for preserving this granularity is completely missing from Section 6 and Section 7.

---

### 6. 🟡 Major: `DeleteUndoUi.kt` and `UndoAction` Omitted from Scope for Stop/Change Undo Messages
* **Affected Scope:** [`medication_section_revision.md:341-345`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/medication_section_revision.md#L341-L345), [`DeleteUndoUi.kt:190-197`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/DeleteUndoUi.kt#L190-L197), [`AncState.kt:284-287`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt#L284-L287)
* **Discrepancy Description & Rationale:**
  Line 344 plans to:
  > "Add strings for ... the undo messages for stop and change..."
  
  In [`DeleteUndoUi.kt:186-197`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/DeleteUndoUi.kt#L186-L197), the headline string for the undo bar is hardcoded via an exhaustive `when (offer.action)`:
  ```kotlin
  val head = when (offer.action) {
      UndoAction.DELETE -> stringResource(R.string.undo_message_deleted_no_patient, offer.label)
      UndoAction.SAVE -> stringResource(R.string.undo_message_saved_no_patient, offer.label)
  }
  ```
  `UndoAction` ([`AncState.kt:284`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt#L284)) only defines `DELETE` and `SAVE`. Neither `UndoAction` nor `DeleteUndoUi.kt` is included in the list of files to modify. Without modifying `UndoAction` and adding branches in `DeleteUndoUi.kt`, any stop or change undo offer with `UndoAction.SAVE` will produce `"Saved <label>"`, rendering the planned strings unreachable.

---

### 7. 🟡 Major: Omission of Affected Test Suite: `MedicationStatusDraftTest.kt`
* **Affected Scope:** [`medication_section_revision.md:347-356`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/medication_section_revision.md#L347-L356), [`MedicationStatusDraftTest.kt:28, 98`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/MedicationStatusDraftTest.kt#L28)
* **Discrepancy Description & Rationale:**
  Section 22 enumerates test files to modify (`MedicationStatusTest.kt`, `AncStateTest.kt`, `AncDeleteUndoStateTest.kt`, `DraftRecoveryTest.kt`).
  It completely omits [`MedicationStatusDraftTest.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/test/java/com/sherifradwan/ancnative/state/MedicationStatusDraftTest.kt#L28), which directly asserts draft behavior and dispatches `AncEvent.StartMedicationEditor("p1", "m1", "m1", "session-1")` (lines 28, 98).
  
  When line 261 modifies `StartMedicationEditor` to add `prefill` and `stopNow`, all call sites in `MedicationStatusDraftTest.kt` will fail to compile unless `prefill = null` and `stopNow = false` are specified as default arguments or `MedicationStatusDraftTest.kt` is added to the modification list.

---

### 8. 🔵 Nit: Underspecified Types and Parsing Edge Cases in `isOngoingMedication`
* **Affected Scope:** [`medication_section_revision.md:236`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/medication_section_revision.md#L236), [`ClinicalModels.kt:58-61`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/model/ClinicalModels.kt#L58-L61)
* **Discrepancy Description & Rationale:**
  The plan specifies `isOngoingMedication(status, isNonCompliant, endDate, today)` without declaring the exact Kotlin parameter types (`endDate: String?`, `today: LocalDate = LocalDate.now()` vs `today: String`).
  
  In clinical safety, unhandled `DateTimeParseException` on an unparseable or legacy `endDate` string will cause a fatal crash during composable rendering across `PatientHubHeader.kt`, `MedsTab.kt`, and `MedicationCard.kt`. The plan should explicitly mandate safe parsing (e.g., `runCatching { LocalDate.parse(endDate) }`) falling back to non-expired / stored status.

---

### 9. 🔵 Nit: `ModalBottomSheet` Ergonomic Configuration
* **Affected Scope:** [`medication_section_revision.md:314`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/medication_section_revision.md#L314), [`PatientListScreen.kt:381`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt#L381)
* **Discrepancy Description & Rationale:**
  Line 314 cites `ModalBottomSheet` as used in [`PatientListScreen.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt#L381). To satisfy the project's 360dp narrow viewport requirement without awkward half-expansion cutoffs over the on-screen keyboard, the plan should explicitly specify `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.

---

## Conclusion & Verdict

**Status: FAILED Implementation Readiness Standard (Zero-Decision Rule)**

The plan cannot be implemented by a fresh AI session or engineer without making independent architectural decisions regarding:
1. How `ReplaceMedicationCommand` acquires the old start date during `isStructurallyValid()`.
2. How `offerMedicationChangeUndo` retrieves the pre-change medication state to build `UndoMedicationChangeCommand`.
3. How `ClinicalCommandFailure` is preserved across `AncState` and `SaveUiError` into `MedicationSheet`.
4. How `DeleteUndoUi.kt` and `UndoAction` are updated to render stop and change undo notifications.
5. Reordering phased execution so that intermediate steps do not break the build.
