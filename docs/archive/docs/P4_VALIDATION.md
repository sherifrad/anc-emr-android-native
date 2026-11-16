# Native ANC P4 Clinical Modules Validation

**Status: PASS - Scoped P4 Engineering Acceptance** (updated 2026-10-11, Africa/Cairo)

P3 engineering acceptance is preserved at `docs/P3_VALIDATION.md` and `evidence/p3/`. This Phase 4 (P4) slice implements the first wave of clinical modules: Pregnancy Dating & Obstetric Ultrasound Scans, Room Schema 2→3 encrypted migration, transactional clinical command receipts, Compose clinical editors, and workspace section projections.

Full offline host gates, unit tests, and connected API 36 instrumentation tests have passed with zero failures and zero lint errors. Physical device verification on HONOR 400 (`AX3C025527011668`, Android 16 / API 36) succeeded via same-signer non-destructive update and cold-start inspection.

## What is Present and Verified

P4 source implements:
1. **Clinical Models and Controlled Vocabulary**:
   - `DemoPatientDating`: Calendar Gestational Age calculation (`calculateGestationalAge`) based on agreed EDD or LMP with 40-week gestation rules; controlled dating methods (`LMP`, `Ultrasound`, `Agreed`).
   - `DemoUltrasoundScan`: Biometry measurements (BPD, HC, AC, FL), EFW, GA weeks/days, FHR, amniotic fluid, presentation, and placental location.
   - `DemoLabInvestigation` and `DemoMedicationRecord`: Initial model definitions and controlled vocabularies.
   - Controlled vocabulary sets in `ClinicalVocabulary` and strict ISO 8601 calendar date validation.
2. **Encrypted Storage Schema 3 & Migration**:
   - Schema 2→3 migration (`from2To3`) in `RecordsMigration.kt` with strict pre-migration table and metadata assertions, creating `patient_dating`, `ultrasound_scans`, `lab_investigations`, `medication_records`, and `clinical_command_receipts`.
   - Exported Schema 3 JSON matching entity models and indices.
   - Fail-closed metadata verification updating `schema_version = 3`.
3. **Clinical Commands & Transactional Storage**:
   - `SaveDatingCommand` and `SaveUltrasoundScanCommand` with structural validation, deterministic SHA-256 fingerprints, and idempotent replays.
   - Cross-table receipt validation across `save_receipts`, `patient_command_receipts`, and `clinical_command_receipts` in `RecordsRepository.kt`.
   - `VaultController` support for clinical command execution.
4. **State Management & UI Workflows**:
   - Reducers for `StartDatingEditor`, `StartUltrasoundEditor`, draft changes, discard dialog coordination, and clinical validations.
   - `DatingEditor` and `UltrasoundEditor` with field validations, touch target compliance, IME padding, and scroll state coordination.
   - `WorkspaceScreen` active tabs for `DATING` (`DatingCard` + `editDating`) and `ULTRASOUND` (`UltrasoundCard` + `addUltrasound`).
   - `ProfileScreen` read-only projections for dating and ultrasound scans.
   - Paired English and Arabic strings for all clinical fields and errors.

## Verified Artifacts and Hashes

| Artifact | SHA-256 | Evidence Meaning |
| --- | --- | --- |
| `app/build/outputs/apk/debug/app-debug.apk` | `71943353fccf154c0f2744c7dcbefa915e1fd72846e1c560e776b255566bfae8` | Verified Schema 3 debug app APK passing all host gates and connected tests. |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | `2ca962adc44a9d7c50fbbe4d84706419f6bb2f19ba18131704d0060e005ace45` | Verified test APK containing 32 connected instrumented tests. |
| `app/schemas/com.sherifradwan.ancnative.storage.RecordsDatabase/3.json` | `17f8d74365c04aff65cb0a0985ddeba458f3c8ebe2bdb3a13bcdcddcc236d956` | Room compiler exported Schema 3 definition matching entity models and migration. |

## Verification Gates and Results

### 1. Host Gates
Command executed from root:
```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
```
- **Unit Tests**: **39 / 39 PASSED** (0 failures, 0 errors, 0 skipped)
  - `ClinicalValidationTest`: 4 tests passed
  - `ClinicalCommandsTest`: 6 tests passed
  - `AncStateTest`: 10 tests passed
  - `SaveCoordinatorTest`: 4 tests passed
  - `VaultStateTest`: 2 tests passed
  - `PatientValidationTest`: 4 tests passed
  - `PreviousPregnancyValidationTest`: 4 tests passed
  - `PatientCommandsTest`: 5 tests passed
- **Lint**: **0 errors** (12 warnings, 1 hint) - preserved in `evidence/p4/host-gates.log`.
- **Build**: Assemble debug and androidTest APKs successful.

### 2. Connected Instrumentation Tests (Disposable API 36 Emulator)
Device: `anc-native-p3-api36-20261011` (Android 16 / API 36, `arm64-v8a`, `emulator-5586`).
Physical phone (`AX3C025527011668`, HONOR 400) was strictly protected and untouched.
Command executed:
```sh
ANDROID_SERIAL=emulator-5586 ./gradlew :app:connectedDebugAndroidTest --console=plain
```
- **Connected Tests**: **32 / 32 PASSED** (0 failures, 0 errors, 0 skipped) - preserved in `evidence/p4/connected-api36.log`.
  - `com.sherifradwan.ancnative.AncWorkflowTest`: 8 tests passed (including dating & ultrasound editor and workspace flows across narrow phone and Arabic RTL)
  - `com.sherifradwan.ancnative.storage.EncryptedRecordsTest`: 13 tests passed (including Schema 3 structural assertions, 1→2→3 migration, and clinical command persistence & replay)
  - `com.sherifradwan.ancnative.storage.VaultIntegrationTest`: 11 tests passed

### 3. Physical Device Verification (HONOR 400)
Device: `AX3C025527011668` (HONOR `DNY-NX9`, Android 16 / API 36, `arm64-v8a`).
Scope: Same-signer incremental update without user data wipe (`adb install -r`), followed by cold-start launch observation (`evidence/p4/honor-device-install.json`).
- **Install**: Streamed incremental install SUCCESS.
- **Cold Start Launch**: `COLD` start in 881 ms (`com.sherifradwan.ancnative.MainActivity`, PID 29231).
- **Runtime Checks**: 0 fatal crashes, 0 ANRs, synthetic demo banner present, device credential gate active. No failure injections or automated test runners were directed to the physical phone.

## Scope and Safety Boundaries

- **P4 Acceptance is Scoped Engineering Acceptance**: Validates the clinical modules for pregnancy dating and ultrasound scanning, Room Schema 3 migration, idempotent clinical commands, and physical device cold-start compatibility.
- **Not Clinical Acceptance**: The prototype uses synthetic demo data only and prominently displays `SYNTHETIC DEMO DATA — NOT FOR CLINICAL USE`.
- **Physical Device Safety Rules Preserved**: The connected HONOR 400 (`AX3C025527011668`) received an incremental non-destructive update only; no test runners, clears, uninstalls, or failure injections were executed against it.
- **Next Phase**: Subsequent approved slices will address laboratory investigations, medication administration, and protected export/backup workflows.
