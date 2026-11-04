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

### Phase 7: Attachment delete cleanup (report and plan: `evidence/attachment-audit/NEXT_MISSION_PLAN.md`)
Done so far (committed: `f1aca51` cleanup-on-every-exit-path, then `b6af671` 2026-11-04 message + sweep + revision fix; test-lane work included): save-undo edit discarded; payload file cleanup wired to undo expiry and every other exit path; dead delete code removed; failed cleanup surfaced (`DeleteMessageKind.CLEANUP_FAILED`); unlock sweep added (`AttachmentOrphanSweeper`, owner decision #1); restore revision fixed. Release APK install on 192.168.1.4:42545 is a ledger claim, not re-checked.
- [x] Re-check of the last round (2026-11-04): unit tests did not compile (`AttachmentErrorStateTest` used removed `DELETE_FAILED`)
- [x] Compile fixed (verified 2026-11-04 by orchestrator run): 914 tests ran, 4 fail = `SavedNoticeTextTest` x2, `DeletableRegistryTest` x2 (ATTACHMENT notices 2 of 3, no fixture)
- [x] Owner decisions: app-kill sweep on unlock (default yes), failed cleanup handling (default keep deleted, notice, retry in sweep), remove unused `attachment_save_undone` string (default yes) — recorded in `evidence/attachment-audit/OWNER_DECISIONS.md` (approved 2026-11-04)
- [x] Test lane: attachment fixture (golden canonical string and hash computed outside the code — re-verified 2026-11-04 by orchestrator, sha256 matches) and notice keys; cleanup-on-every-exit-path tests; red check achieved (orchestrator run 2026-11-04: 923 tests, 5 failed — all new exit-path tests, red for the right reason; 4 old failures fixed)
- [x] Core lane: one cleanup function called from every exit path; failure reported via AncEvent.PayloadCleanupFailed; installed on device (DNY-NX9) — DONE 2026-11-04 2026-11-04: opencode free tier exhausted (429 FreeUsageLimitError, glm-5.3-flash-free and deepseek-v4.1-flash-free both refused); owner chose to wait for quota reset. Brief ready (`brief-attachment-cleanup.md` in the session temp dir); implement via opencode-delegate when quota returns, or owner picks another lane
- [x] Remove unused strings `attachment_delete_failed` and `attachment_save_undone` (removed; `f1aca51` removed the first but left the second — that was wrong, fixed in `b6af671`)
- [ ] Independent review (different vendor family), including open F4 and F5; saved under `evidence/attachment-audit/`
- [x] `./gradlew :app:testDebugUnitTest` fully green — 923 tests pass (2026-11-04, `f1aca51`); 938 tests in 86 classes, 0 failures after `b6af671` (gate OK, static checks OK)
- [x] Device check on the QA copy (2026-11-04, Honor DNY-NX9, QA build of `b6af671`): delete → wait → file gone; delete → Undo → record and file intact; unlock sweep removed a planted orphan. NOT done on the phone: two deletes in a row, lock inside the window, 60 s guard
- [x] Instrumented suites on a disposable emulator (2026-11-04, `anc-test-api26`, API 26, 360 dp, `.qa`): `DeleteUndoUiTest` 33/33 and `ClinicalDeleteRestoreDbTest` 140/140, 0 failures. Connected runs stay off the owner's phone
- [x] Local commit of the verified files (never push) — `f1aca51` + `b6af671` (2026-11-04)

- [x] Two defects found on the phone are fixed and retested (2026-11-04): single confirmation for attachment delete (viewer delegates to the shared dialog; check R6C8) and a human attachment label ("Ultrasound Scan · 2026-10-30" instead of `att_<uuid>.webp`)

- [ ] **Frontier review 2026-11-05: FAIL — fix before anything else.** (1) sweep must also treat `ultrasound_scans.image_uris` and persisted drafts as references (data loss today); (2) do not clean a pending payload while an Undo restore is in flight; (3) make the `CLEANUP_FAILED` message survive the replacement path (move payload I/O off the main thread); plus `lastModified()==0`, stranded `pendingUndo`, main-thread I/O, viewer Cancel behaviour, checker gaps, and attachment cases in `kinds()`/`DeleteUndoUiTest`

Next after that: remaining phone-session items in `.agents/pipeline/POSTPONED_DEVICE_TESTS.md`, then print preview fixes, then the older audit items (`minSdk`, whole-database load).
