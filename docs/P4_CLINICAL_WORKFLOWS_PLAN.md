# Native Android ANC — Phase 4 (P4) Clinical Modules Plan

**Status: DRAFT / PENDING REVIEW & ACCEPTANCE** (Date: 2026-10-11, Africa/Cairo)

Project: `/Users/sherifgamalradwan/anc-emr-android-native`  
Target: HONOR 400 (`DNY-NX9`, Android 16 / API 36, `arm64-v8a`), `minSdk 35`, `compile/target 36`.  
Baseline Prerequisites: P2 Engineering PASS (`evidence/p2/`), P3 Engineering PASS (`evidence/p3/`, `docs/P3_VALIDATION.md`).

---

## 1. Objective and Architectural Scope

Following the successful engineering acceptance of **P3** (Schema 2, patient registration, demographics, medical/surgical histories, previous pregnancies, archive/unarchive, and cross-table receipt collision verification), **Phase 4 (P4)** advances the clinical consultation capabilities of the Native ANC EMR.

P4 activates the clinical modules embedded in the patient Workspace, bringing them from static placeholders into fully interactive, encrypted, revisioned clinical workflows:
1. **Dating & Gestational Age Module (`DATING`)**:
   - LMP (Last Menstrual Period), Ultrasound-derived EDD, Agreed Clinical EDD, and chronological Gestational Age calculation (stored factually, calendar-validated).
2. **Obstetric Ultrasound Module (`ULTRASOUND`)**:
   - Ultrasound scan encounters: Scan date, Gestational Age at scan, Fetal number/viability, Presentation, Placental localization, Amniotic fluid volume (liquor), Biometry metrics, and Scan impression.
3. **Laboratory Investigations Module (`LABORATORY`)**:
   - Antenatal lab panels: Blood grouping & Rh confirmation, Complete Blood Count / Hemoglobin (Hb), Urinalysis (protein, glucose), Blood Glucose testing (Fasting / OGTT), and Infectious disease screenings.
4. **Antenatal Medications & Prophylaxis Module (`MEDICATIONS`)**:
   - Routine ANC prophylaxis (Iron, Folic Acid, Calcium, Tetanus Toxoid / Tdap immunization) and clinical prescriptions with dose, frequency, start date, and status.
5. **Episode & Clinical Overview (`EPISODE_OVERVIEW`)**:
   - Synthesizing active pregnancy episode facts, current gestational age milestones, active risk level, and cumulative visit/investigation summaries.

---

## 2. Strict Governance & Clinical Integrity Invariants

- **Synthetic Data Banner Retained**: Every surface, editor, and dialog must display `SYNTHETIC DEMO DATA — NOT FOR CLINICAL USE`.
- **No Guessed Clinical Data**: All clinical entries are explicitly recorded facts. Nullable values remain `null` and render localized `"Not recorded"` (`R.string.not_recorded`). No dummy zeros, invented negative findings, or automated clinical assumptions.
- **Pure Functional Calculations**: Calendar math (e.g. Gestational Age from LMP or agreed EDD) is strictly deterministic and validated via `java.time.LocalDate`.
- **Draft Isolation & Lifecycle Safety**:
  - All clinical editor drafts remain strictly in memory via ViewModel StateFlow.
  - Drafts survive screen rotation while unlocked.
  - Screen lock, backgrounding, or process death discards all uncommitted draft material.
- **Atomic Transaction Receipts**:
  - Each clinical write is backed by a revisioned immutable command and a cryptographically linked receipt.
  - Operation UUIDs are verified for cross-table uniqueness across all receipt tables (`save_receipts`, `patient_command_receipts`, `clinical_command_receipts`).
- **Physical Device Protection Rule**:
  - The connected HONOR 400 (`AX3C025527011668`) must **NEVER** be subjected to automated test runs, app clears, or failure injections. All testing runs against disposable API 36 emulators (`emulator-5586`).

---

## 3. Storage & Schema 2→3 Evolution

### Database Migration: `MIGRATION_2_3`
Room database version increments from `2` to `3`. The migration must:
1. Validate existing Schema 2 tables and metadata.
2. Strictly create the new clinical tables using `CREATE TABLE` (no `IF NOT EXISTS`):
   - `patient_dating`: Dating records (LMP, EDD by LMP, US EDD, Agreed EDD, method, notes, revision, timestamps).
   - `ultrasound_scans`: Scans (patient FK, date, GA weeks/days, presentation, placenta, liquor, biometry, notes, revision, timestamps).
   - `lab_investigations`: Investigations (patient FK, date, test type, result value, unit, reference range, status, notes, revision, timestamps).
   - `medication_records`: Prescriptions & prophylaxis (patient FK, medication name, dosage, route, frequency, start date, status, notes, revision, timestamps).
   - `clinical_command_receipts`: Immutable audit receipts for all P4 clinical commands.
3. Preserve all existing Schema 1 and Schema 2 patient, visit, history, previous pregnancy, and receipt data intact.
4. Atomically update metadata version.

---

## 4. Exact Permitted Source & Resource Paths

### Modify:
- `app/src/main/java/com/sherifradwan/ancnative/model/DemoModels.kt`
- `app/src/main/java/com/sherifradwan/ancnative/storage/RecordEntities.kt`
- `app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDao.kt`
- `app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDatabase.kt`
- `app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt`
- `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt`
- `app/src/main/java/com/sherifradwan/ancnative/state/AncViewModel.kt`
- `app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt`
- `app/src/main/java/com/sherifradwan/ancnative/ui/ProfileScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ar/strings.xml`
- `app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/storage/EncryptedRecordsTest.kt`
- `docs/AI_HANDOFF.md`

### Add:
- `app/src/main/java/com/sherifradwan/ancnative/model/ClinicalModels.kt`
- `app/src/main/java/com/sherifradwan/ancnative/storage/ClinicalCommands.kt`
- `app/src/main/java/com/sherifradwan/ancnative/storage/ClinicalMigration.kt`
- `app/src/main/java/com/sherifradwan/ancnative/ui/DatingEditor.kt`
- `app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundEditor.kt`
- `app/src/main/java/com/sherifradwan/ancnative/ui/LaboratoryEditor.kt`
- `app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/ClinicalValidationTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/storage/ClinicalCommandsTest.kt`
- `app/schemas/com.sherifradwan.ancnative.storage.RecordsDatabase/3.json`
- `docs/P4_CLINICAL_WORKFLOWS_PLAN.md`
- `docs/P4_VALIDATION.md`
- `evidence/p4/`

---

## 5. Verification & Acceptance Criteria

1. **Host Gates**:
   - `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`
   - Complete unit test coverage for dating math, ultrasound vocabulary, lab ranges, prescription models, and clinical command idempotence.
   - 0 lint errors, complete English/Arabic string parity.
2. **Schema 2→3 Encrypted Migration Verification**:
   - Automated test migrating an existing encrypted Schema 2 database (with patients, visits, previous pregnancies, and receipts) to Schema 3 without loss or corruption.
3. **Connected Workflow Verification**:
   - Run on disposable API 36 emulator (`emulator-5586`).
   - Validate adding dating, ultrasound, lab, and medication entries; verify cross-section synchronization in Workspace and Profile; test dirty Back handling and rotation survival.
4. **Evidence & Documentation**:
   - Log files saved to `evidence/p4/`.
   - Artifact hashes recorded in `docs/P4_VALIDATION.md`.
