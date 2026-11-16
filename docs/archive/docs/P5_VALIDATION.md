# Native ANC P5 Clinical Modules & Session Grace Period Validation

**Status: PASS - Scoped P5 Engineering Acceptance** (2026-10-11, Africa/Cairo)

Phase 4 engineering acceptance is preserved at `docs/P4_VALIDATION.md` and `evidence/p4/`. This Phase 5 (P5) engineering slice implements:
1. **Laboratory Investigations Module**: CBC, Blood Group/Rh, Urinalysis, OGTT, Serology with abnormal result flagging, unit/reference ranges, and transactional persistence.
2. **Medications & Prescriptions Module**: Prophylaxis vs. therapeutics differentiation, route, dosage, frequency, and active/completed/discontinued status tracking.
3. **Session Inactivity Grace Period**: 10-minute non-destructive background grace period eliminating aggressive re-authentication on brief app switching while strictly maintaining device credential security.
4. **Full English & Arabic String Parity**: 100% paired strings across `values/strings.xml` and `values-ar/strings.xml`.

All offline host gates, unit tests, and connected API 36 instrumentation tests passed with zero failures and zero lint errors. Physical device verification on HONOR 400 (`AX3C025527011668`, Android 16 / API 36) succeeded via same-signer non-destructive incremental update and cold-start launch inspection.

---

## What is Present and Verified

1. **Laboratory Investigations Module**:
   - `SaveLabInvestigationCommand` with structural validation, deterministic SHA-256 fingerprinting, and idempotent replays.
   - Compose editor [`LabEditor.kt`](../app/src/main/java/com/sherifradwan/ancnative/ui/LabEditor.kt) with touch-target compliance, IME handling, and abnormal flag toggle.
   - Active workspace section `WorkspaceSection.LABORATORY` with `addLab` and `LabCard` components.
   - Read-only lab projections in `ProfileScreen.kt` and `EpisodeOverviewContent`.

2. **Medications & Prescriptions Module**:
   - `SaveMedicationCommand` with structural validation, deterministic SHA-256 fingerprinting, and idempotent replays.
   - Compose editor [`MedicationEditor.kt`](../app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt) supporting prophylaxis toggling, route selection, and status assignment.
   - Active workspace section `WorkspaceSection.MEDICATIONS` with `addMedication` and `MedicationCard` components.
   - Read-only medication projections in `ProfileScreen.kt` and `EpisodeOverviewContent`.

3. **Session Inactivity Grace Period**:
   - Implemented in `MainActivity.kt` with a 10-minute timeout (`SESSION_GRACE_PERIOD_MS = 600,000 ms`).
   - On backgrounding (`onStop`), the active session is maintained via delayed lock runnable.
   - On foregrounding within 10 minutes (`onStart`), the lock runnable is cancelled and the session resumes seamlessly without re-prompting for credentials.
   - If 10 minutes elapse while backgrounded, or if the user explicitly triggers "Lock Storage", the vault locks immediately.

4. **Localization Parity**:
   - Every lab and medication string resource in `res/values/strings.xml` has an exact semantic counterpart in `res/values-ar/strings.xml`.
   - Verified by connected test `everyNewEnglishStorageStringHasAnArabicValue`.

---

## Verified Artifacts and Hashes

| Artifact | SHA-256 | Evidence Meaning |
| --- | --- | --- |
| `app/build/outputs/apk/debug/app-debug.apk` | `b19c243335c15db3eafaa4d4197a3cb2f1cce365179a41ad3518dfee54c22bef` | Verified P5 debug app APK passing all host gates, connected tests, and physical device cold start. |
| `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | `e440cede6c3baa263391297cbf7d319e2251ccc72b7507f448f8afb0553fafbd` | Verified test APK containing 34 connected instrumented tests. |

---

## Verification Gates and Results

### 1. Host Gates
Command executed from root:
```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
```
- **Unit Tests**: **42 / 42 PASSED** (0 failures, 0 errors, 0 skipped)
  - `ClinicalValidationTest`: 4 tests passed
  - `PatientValidationTest`: 4 tests passed
  - `PreviousPregnancyValidationTest`: 4 tests passed
  - `AncStateTest`: 11 tests passed (including dating, ultrasound, lab, and medication reducer flows)
  - `ClinicalCommandsTest`: 9 tests passed (including lab and medication command preparation & validation)
  - `PatientCommandsTest`: 4 tests passed
  - `SaveCoordinatorTest`: 4 tests passed
  - `VaultStateTest`: 2 tests passed
- **Lint**: **0 errors** - preserved in `evidence/p5/host-gates.log`.
- **Build**: Assembled debug and test APKs successfully.

### 2. Connected Instrumentation Tests (Disposable API 36 Emulator)
Device: `anc-native-p3-api36-20261011` (Android 16 / API 36, `arm64-v8a`, `emulator-5586`).
Physical phone (`AX3C025527011668`, HONOR 400) was strictly protected and untouched.
Command executed:
```sh
ANDROID_SERIAL=emulator-5586 ./gradlew :app:connectedDebugAndroidTest --console=plain
```
- **Connected Tests**: **34 / 34 PASSED** (0 failures, 0 errors, 0 skipped) - preserved in `evidence/p5/connected-api36.log` and `evidence/p5/connected-results.xml`.
  - `AncWorkflowTest`: 9 tests passed (including dating, ultrasound, lab & medication editor and workspace flows across narrow phone and Arabic RTL)
  - `EncryptedRecordsTest`: 14 tests passed (including Schema 3 structural assertions, 1→2→3 migration, and all clinical command persistence & replay)
  - `VaultIntegrationTest`: 11 tests passed

### 3. Physical Device Verification (HONOR 400)
Device: `AX3C025527011668` (HONOR `DNY-NX9`, Android 16 / API 36, `arm64-v8a`).
Scope: Same-signer incremental update without user data wipe (`adb install -r`), followed by cold-start launch observation (`evidence/p5/honor-device-install.json`).
- **Install**: Streamed incremental update `Success`.
- **Cold Start Launch**: `COLD` start in 869 ms (`com.sherifradwan.ancnative.dev/com.sherifradwan.ancnative.MainActivity`).
- **Runtime Checks**: 0 fatal crashes, 0 ANRs, synthetic demo banner active, device credential gate active. No failure injections or automated test runners were directed to the physical phone.

---

## Scope and Safety Boundaries

- **P5 Acceptance is Scoped Engineering Acceptance**: Validates the clinical modules for laboratory investigations, medications & prescriptions, 10-minute session grace period, and physical device cold-start compatibility.
- **Not Clinical Acceptance**: The prototype uses synthetic demo data only and prominently displays `SYNTHETIC DEMO DATA — NOT FOR CLINICAL USE`.
- **Physical Device Safety Rules Preserved**: The connected HONOR 400 (`AX3C025527011668`) received an incremental non-destructive update only; no test runners, clears, uninstalls, or failure injections were executed against it.
