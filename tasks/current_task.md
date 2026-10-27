# Task Queue & Audit Plan (MACA v2)

## Active Feature
**Deep Clinical & Architectural Audit, Refactoring Categorization, Linting & E2E Verification**

### Phase 0: Preflight Diagnostic & Scarcity Tier Setup
- [x] Run `./scripts/bootstrap.sh` diagnostic (fast path verified, zero missing tools) (Verified: 2026-10-26T19:13:22Z)
- [x] Lock Skill Ambassador Auditor Charter (`.agents/SKILL_COMPLIANCE_LOG.md`) (Verified: 2026-10-26T19:13:42Z)

### Phase 1: Zero-Presumption Codebase Revision & Architectural Call-Chain Mapping
- [x] Complete full-codebase zero-presumption audit across UI, Core Logic, Data/Storage, Security & Regulatory domains (`.agents/SPEC_AND_AUDIT.md`) (Verified: 2026-10-26T19:24:05Z)
- [x] Map Cross-Function Call-Chains from UI/User actions down to SQLite/SQLCipher and Cryptographic Keyrings (Verified: 2026-10-26T19:24:05Z)
- [x] Categorize all classes, functions, and models into: (1) Usable & Essential, (2) Redundant / Duplicated, (3) Non-Usable / Dead / Demo-only, with safe removal plan (Verified: 2026-10-26T19:24:05Z)

### Phase 2: Static Analysis, Invariants & Design Token Linting
- [x] Execute `lint_samd_invariants.py` (SaMD Zero-Delete, JVM Param Budget < 35 args, Identity claims) (Evidence: 0 hard blockers, 9 parameter warnings recorded) (Verified: 2026-10-26T19:14:22Z)
- [x] Execute `lint_design_and_ux.py` (Design tokens, RTL mirrored icons, 5-State UI Matrix, Touch targets >= 48dp) (Evidence: 31 hard blockers detected and cataloged) (Verified: 2026-10-26T19:14:27Z)
- [x] Audit JVM memory footprint: In-memory patient snapshot vs. scalable SQLite/Room paging (Documented in SPEC_AND_AUDIT.md) (Verified: 2026-10-26T19:24:05Z)

### Phase 3: Tabula Rasa Diff & Independent Lane Audit
- [x] Conduct multi-lane independent review (`.agents/AI_HANDOFF_LEDGER.md`) across UI/UX, Core Logic, Data/SaMD, and QA (Verified: 2026-10-26T19:24:20Z)

### Phase 4: E2E Verification, Device Diagnostics & Maestro Testing
- [x] Build & install debug APK onto physical device (`DNY-NX9` / Android 16) (Evidence: adb install -r -> Success) (Verified: 2026-10-26T19:18:24Z)
- [x] Collect and analyze low-level ADB Logcat and high-level application events (Monitored PID 19343, dump xml verified) (Verified: 2026-10-26T19:18:43Z)
- [x] Execute Maestro E2E flow verifying clinic day workflows (Intake, Biometry, Lab, Security PIN, Attachment) (Evidence: `maestro test tests/maestro/clinical_audit_flow.yaml` -> exit code 0) (Verified: 2026-10-26T19:23:24Z)

### Phase 5: Comprehensive Report & Refactoring Roadmap Delivery
- [x] Synthesize findings, critical clinical bugs, redundancy triage, and actionable refactoring plan
- [x] Run `skill_ambassador_auditor.py --verify` and present full report to user

### Phase 6: Modern Material 3 Remodifications, Negative Controls & Focus Isolation
- [x] Modernize UI with Material 3 semantic tokens, system dark theme support, and auto-mirrored RTL icons (Evidence: `MainActivity.kt`, `ProfileScreen.kt`, `WorkspaceScreen.kt`, `PatientEditor.kt`) (Verified: 2026-10-26T20:25:04Z)
- [x] Implement "Don't Lose Focus Principle" in Patient Editor (single initialization in `startPatientEditor`, eliminated disruptive `LaunchedEffect` fighting keystrokes) (Verified: 2026-10-26T20:25:04Z)
- [x] Fix backstack navigation hierarchy: Workspace -> Profile -> PatientList (Evidence: `AncState.kt:1048`) (Verified: 2026-10-26T20:21:57Z)
- [x] Decouple database `isSynthetic` lock in `RecordsRepository.kt` & `RecordsMigration.kt` to allow clinical database initialization (Verified: 2026-10-26T20:23:14Z)
- [x] Author & execute dedicated negative control & clinical output test suite (`NegativeControlAndClinicalValidationTest.kt`, 106 unit tests passed) (Verified: 2026-10-26T20:27:09Z)
- [x] Build debug APK, install on connected phone (`Honor 90 / DNY-NX9`), and execute Maestro E2E suite capturing 11 clinical workflow screenshots (Evidence: `evidence/audit_screenshots/`, `01_` through `11_`) (Verified: 2026-10-26T20:33:15Z)
