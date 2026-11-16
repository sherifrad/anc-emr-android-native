### Inventive Catalyst Strategic Review (EGM Mode: REVIEW)

**Target Document:** `docs/designs/medication_section_revision.md` (Lines 1–351)  
**Evaluator:** Inventive Catalyst Goldfish (`egm-inventive-catalyst`)  
**Mission:** High-altitude strategic audit evaluating architectural trade-offs, false compromises, and technical contradictions against TRIZ inventive principles.

---

### Codebase Verification Evidence

The following files and line numbers in `/Users/sherifgamalradwan/anc-emr-android-native` were directly opened and inspected to verify the document's architectural claims:

1. `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt`:
   - L53: Declares `data class MedicationEditor(...) : AncRoute`.
   - L357: Maps `AncRoute.MedicationEditor -> patientId`.
   - L1470–1483: `startMedicationEditor` resets route to `AncRoute.MedicationEditor(patient.id, medicationId, ...)`.
   - L1716: Back-navigation handler mapping `AncRoute.MedicationEditor -> AncRoute.Workspace(route.patientId, WorkspaceSection.MEDICATIONS)`.
2. `app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt`:
   - L238: Renders full-screen `MedicationEditor(state, route, onEvent)` on route match.
3. `app/src/main/java/com/sherifradwan/ancnative/state/EditorHooks.kt`:
   - L12–36: `MedicationEditorHook` queries `state.medicationDraft?.medicationId` and resets route back to `Workspace(MEDICATIONS)`.
4. `app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt`:
   - L48: Verifies existing usage of `androidx.compose.material3.ModalBottomSheet`.
5. `app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt`:
   - L346–356 (specifically L353): Command dispatch handling `is SaveMedicationCommand -> saveMedication(command)`.
   - L1577–1624: Creation of `ClinicalCommandReceiptEntity` for clinical mutations.
6. `app/src/main/java/com/sherifradwan/ancnative/state/UndoCoordinator.kt`:
   - L179–200: `offerSaveUndo` constructing `DeleteRecordCommand` with expected patient and record revisions.
7. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt`:
   - L51–78: `dispatchMedicationDraft` generating `StartMedicationEditor` followed by up to six separate `MedicationDraftChanged` events.
8. `app/src/main/java/com/sherifradwan/ancnative/model/ClinicalModels.kt`:
   - L28–29 & L58–61: Current 2-argument signature of `isOngoingMedication(status: String, isNonCompliant: Boolean = false)`.
9. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt`:
   - L53–58: List partitioning with `isOngoingMedication(it.status, it.isNonCompliant)`.
   - L146–154: `onStop` triggering `dispatchMedicationDraft(..., submit = true)`.
10. `app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt`:
    - L1–396: 396-line full-screen editor holding ad-hoc keyboard and field focus logic.
11. `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/model/MedicationTiles.kt`:
    - L3–9: `MedicationTile` model definition (currently lacks `frequency`).
12. `app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt`:
    - L395–421: `EditorField` composable with `keyboardOptions` and `keyboardActions`.
13. `app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDao.kt`:
    - L454–475: `updateMedication` executing SQL update with `revision = revision + 1`.
14. `app/src/main/java/com/sherifradwan/ancnative/storage/RecordEntities.kt`:
    - L313–322: `ClinicalCommandReceiptEntity` schema definition indexed on `patient_id` and `(record_kind, record_id)`.
15. `app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDatabase.kt`:
    - L17–28: Room schema v6 configuration with exportSchema = true.

---

### Findings and Inventive Audit

#### Finding 1
1. **Severity**: Major
2. **Affected Scope**: Section 4 (*Stop and change*, lines 126–129), Section 10 (*Risks*, line 165), Decisions (line 58), and Alternative E (lines 191–196).
3. **False Compromise**:
   *Parameter X (Clinical Audit Trail & Provenance Integrity) versus Parameter Y (Database Schema Immobility / Zero Migration Overhead).*
   The design prematurely capitulates to a false compromise by storing clinical titration history as unparsed, editable plain text in the doctor-facing `notes` field (`"Changed from <old dose · frequency> on <date> <time>"`), detected via prefix matching (`hasMedicationChangeNote(notes)`). The author explicitly accepts that if the clinician edits or formats their notes, the clinical change marker and UI badge silently disappear forever. Storing critical medical audit state inside an unvalidated, user-editable markdown string violates clinical provenance invariants solely to circumvent a Room schema migration.
4. **Proposed Resolution**:
   *Catalog Pattern 8: Intermediary (Command Receipt as Metadata Anchor) / Pattern 2: Extraction.*
   Schema v6 is already frozen, but `clinical_command_receipts` already exists in Schema v6 (`RecordEntities.kt:313`) with columns `operation_uuid`, `patient_id`, `record_kind`, and `record_id` (indexed on `[record_kind, record_id]`). When `ReplaceMedicationCommand` commits, it already inserts a receipt. Rather than stuffing provenance strings into user-editable `notes`, record the replace operation in `clinical_command_receipts` or query whether the medication's ID was the target of a `MEDICATION_REPLACE` command receipt. The UI can derive the "Changed" badge cleanly through this existing intermediary table without touching user notes and without altering the Room schema.
5. **Secondary Liabilities**:
   Deriving the changed badge via receipt lookup introduces an asynchronous query or joining step when mapping patient domain records, adding slight query complexity compared to inspecting an in-memory string.

---

#### Finding 2
1. **Severity**: Major
2. **Affected Scope**: Section "The Problem" (lines 19–25), Goals (line 31), Section 4 & 5 (lines 125, 135), and Alternatives Considered (lines 171–184).
3. **False Compromise**:
   *Parameter X (Clinician Ergonomics & Interaction Velocity) versus Parameter Y (Concurrency Safety / In-flight Race Elimination).*
   In Alternative A, 1-tap stop and 1-tap formulary add were rejected because "the current path accepts one draft at a time and ignores a start request while a save runs, so two fast taps lose one of them without any message. A queue in the view model is needed... and a bug in the queue would lose taps silently." Consequently, the author accepted Alternative C: forcing a 2-tap bottom sheet on *every* single medication action (even stopping an existing drug or adding a known catalog drug) purely to use the modal dialog as a coarse UI mutex. Sacrificing clinical interaction speed during fast-paced ward rounds to avoid managing in-flight state is a false architectural compromise.
4. **Proposed Resolution**:
   *Catalog Pattern 4: Beforehand Cushioning combined with Pattern 6: Dynamics (Optimistic Row Gating with Instant Undo).*
   Instead of forcing the doctor into a modal sheet just to stop a drug with today's date, provide a direct 1-tap stop: tapping the stop icon immediately transitions the local row state to "Discontinued" (cushioned beforehand) and posts the command, while surfacing the existing `UndoCoordinator` snackbar. The specific row's touch target is dynamically disabled while in flight, eliminating tap collision without penalizing the clinician with a redundant sheet and confirmation tap. A long-press or overflow tap can remain available if backdating the stop date is required.
5. **Secondary Liabilities**:
   Requires managing an in-flight pending status per row in `MedsTab` state to prevent double-dispatch, and wiring an optimistic rollback if the underlying database command aborts.

---

#### Finding 3
1. **Severity**: Nit
2. **Affected Scope**: Section 3 (*Dates and course length*, lines 120–121) and Decisions (lines 33, 60).
3. **False Compromise**:
   *Parameter X (Zero-Mutation Read Simplicity) versus Parameter Y (Cross-Subsystem Data Invariant Consistency).*
   The design specifies that course expiration is calculated purely dynamically at render time (`isOngoingMedication(...)`), leaving the underlying database record permanently marked as `status = "Active"`. Line 60 acknowledges that "the backup export writes the stored fields as they are and is not affected." This accepts an unexamined divergence: external data exports, SQL queries, or third-party integrations will see expired courses (e.g., an antibiotic finished 3 weeks ago) as active prescriptions, while only the app's Compose UI treats them as stopped.
4. **Proposed Resolution**:
   *Catalog Pattern 9: Self-Service (Reconciliation on Access).*
   When a patient's records are loaded into memory (e.g., in repository hydration or when entering `MedsTab`), an automatic Self-Service reconciliation pass should check for expired active courses whose `endDate < today` and execute a background status transition to `Completed`. This ensures physical database state and UI state remain convergent without requiring daemon timers or schema alterations.
5. **Secondary Liabilities**:
   May trigger an automatic write transaction and revision bump upon opening an older patient file, which must be coordinated so it does not conflict with active user drafts.
