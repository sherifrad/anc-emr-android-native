# Global Delete / Confirm / Undo Report & Continuation Handoff

**Date:** 2026-11-03  
**Repository:** `/Users/sherifgamalradwan/anc-emr-android-native` (branch `maca-round2`)  
**Owner:** Dr. Sherif Radwan  
**Guide Spec:** `.agents/pipeline/ADDING_A_DELETABLE_KIND.md` & `SPEC_LOGIC_R6B.md` (D600–D654)

---

## 1. Global Deletable Mechanism Status Matrix

Delete, confirmation dialog, 8-second Undo countdown bar, and encrypted Room restore are implemented as **ONE global mechanism** keyed by `DeletableType`.

| Kind (`DeletableType`) | Tag | Adapter Class | Receipt Kinds (`ClinicalRecordKind`) | Section (`WorkspaceSection`) | Status | Commit / State |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `MEDICATION` | `"MED"` | `MedicationDeleteAdapter` | `MEDICATION_DELETE` / `MEDICATION_RESTORE` | `MEDICATIONS` | **DONE & VERIFIED** | `9ceef0d` / `cdd0308` |
| `LAB` | `"LAB"` | `LabDeleteAdapter` | `LAB_DELETE` / `LAB_RESTORE` | `LABORATORY` | **DONE & VERIFIED** | `9ceef0d` / `cdd0308` |
| `ULTRASOUND` | `"US"` | `UltrasoundDeleteAdapter` | `ULTRASOUND_DELETE` / `ULTRASOUND_RESTORE` | `ULTRASOUND` | **DONE & VERIFIED** | `fcff3bc` |
| `VISIT` | `"VIS"` | `VisitDeleteAdapter` | `VISIT_DELETE` / `VISIT_RESTORE` | `VISITS` | **DONE & VERIFIED** | `d9dff55` |
| `PREVIOUS_PREGNANCY` | `"PRV"` | `PreviousPregnancyDeleteAdapter` | `PREVIOUS_PREGNANCY_DELETE` / `PREVIOUS_PREGNANCY_RESTORE` | `OBSTETRIC_HISTORY` | **CODE & JVM GATE DONE** (Awaiting Emulator Run + Review + Commit) | Uncommitted in Working Tree |
| `ATTACHMENT` (Future `r6b6`) | TBD | `AttachmentDeleteAdapter` | `ATTACHMENT_DELETE` / `ATTACHMENT_RESTORE` | `ATTACHMENTS` | **NOT STARTED** (Requires spec addendum: file shredding deferred until Undo window expires) | Pending |
| `DATING` | N/A | *Not deletable by design* | Edit-only (`DATING`) | `DATING` | **N/A (Edit-Only)** | By Owner Spec |

---

## 2. What Was Completed Before Credits Depleted

### A. Visit Delete Adapter (`DeletableType.VISIT`) — Committed Locally (`d9dff55`)
1. **Root Cause Fixed:** The user noticed that on the Visits timeline tab, only standalone ultrasound cards showed the trash delete icon while routine visits did not. This was because `VisitCard` had not yet been wired to `DeletableType.VISIT`.
2. **Full 10-Step Adapter Implementation:**
   - `DemoVisit` now implements `DeletableRecord` (`id`, `patientId`, `revision: Long` with backwards-compatible `Int` constructor).
   - Populated `patientId` across all 25 `DemoVisit` fixtures in `DemoFixtures.kt`.
   - Created `VisitDeleteAdapter.kt` (`tag = "VIS"`, `VISIT_DELETE`, `VISIT_RESTORE`, `WorkspaceSection.VISITS`).
   - Handled SQLite foreign-key constraint (`save_receipts.visit_id -> visits.id ON DELETE RESTRICT`) by calling `dao.deleteSaveReceiptsForVisit(id)` inside `VisitDeleteAdapter.deleteRow` and throwing `RecordsRepository.ClinicalCommandAbort(ClinicalCommandFailure.STALE_REVISION)` if `deleteVisit != 1` so the transaction rolls back atomically.
   - Added tombstone replay protection in `RecordsRepository.save(SaveVisitCommand)`: when `expectedRevision == null`, if `dao.latestClinicalReceiptForRecord(patientId, visitId)?.recordKind?.endsWith("_DELETE") == true`, rejects with `SaveFailure.VISIT_CONFLICT`.
   - Disambiguated `HorizontalPager` pre-composed semantics tags between `VISITS` (page 4) and `ULTRASOUND` (page 5) by giving timeline standalone ultrasound cards `cardTag = "timelineUltrasound-${scan.id}"` and `deleteTag = "delete-timelineUltrasound-${scan.id}"`.
3. **Verification:**
   - `python3 .agents/checks/check_r6c.py`: **PASS** (R6C0–R6C6).
   - `.agents/scripts/run_build_gate.sh`: **GATE OK**.
   - `ClinicalDeleteRestoreDbTest` on `emulator-5554`: **112/112 PASSED** (including `replaySaveOfDeletedVisitIsRejected`).
   - `DeleteUndoUiTest` on `emulator-5554`: **29/29 PASSED**.
   - Independent Frontier Review (Claude CLI): All findings remediated and committed in `d9dff55`.

### B. Previous Pregnancy Delete Adapter (`DeletableType.PREVIOUS_PREGNANCY`) — In Working Tree (90% Complete)
All 16 source and test files are written and pass the JVM build gate and static R6C checks:
- **Untracked (New) File:**
  - `app/src/main/java/com/sherifradwan/ancnative/storage/PreviousPregnancyDeleteAdapter.kt`
- **Modified Files (15):**
  1. `.agents/checks/check_r6c.py` — Added `PREVIOUS_PREGNANCY` to `KIND` and `PreviousPregnancyDeleteAdapter.kt` to `FREE`.
  2. `app/src/main/java/com/sherifradwan/ancnative/model/DemoModels.kt` — `DemoPreviousPregnancy` implements `DeletableRecord`.
  3. `app/src/main/java/com/sherifradwan/ancnative/storage/DeletableRecords.kt` — Added `PREVIOUS_PREGNANCY` to `DeletableType`, registered `PreviousPregnancyDeleteAdapter`, updated `adapterFor`, added `previousPregnancyContentHash`.
  4. `app/src/main/java/com/sherifradwan/ancnative/storage/RecordsDao.kt` — Added `deletePreviousPregnancy(id, patientId, expectedRevision)`.
  5. `app/src/main/java/com/sherifradwan/ancnative/storage/ClinicalCommands.kt` — Added `PREVIOUS_PREGNANCY_DELETE` and `PREVIOUS_PREGNANCY_RESTORE`.
  6. `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt` — Added `PREVIOUS_PREGNANCY_DELETED`, `PREVIOUS_PREGNANCY_RESTORED`, `PREVIOUS_PREGNANCY_SAVE_UNDONE` to `SavedNotice` + `stringRes()`, guarded `startPregnancyEditor` with `undoRunningFor`.
  7. `app/src/main/java/com/sherifradwan/ancnative/state/EditorHooks.kt` — Added `PreviousPregnancyEditorHook` (`WorkspaceSection.OBSTETRIC_HISTORY`) and registered in `EditorHooks`.
  8. `app/src/main/res/values/strings.xml` — Added `deletable_type_previous_pregnancy`, `previous_pregnancy_deleted`, `previous_pregnancy_restored`, `previous_pregnancy_save_undone`.
  9. `app/src/main/java/com/sherifradwan/ancnative/ui/DeleteUndoUi.kt` — Mapped `DeletableType.PREVIOUS_PREGNANCY` in `labelResource()`.
  10. `app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt` — Added `DeleteIconButton(DeletableType.PREVIOUS_PREGNANCY, pregnancy, onDelete)` to `PreviousPregnancyCard` and wired in `WorkspaceSection.OBSTETRIC_HISTORY`.
  11. `app/src/test/java/com/sherifradwan/ancnative/state/SavedNoticeTextTest.kt` — Added 3 notices.
  12. `app/src/test/java/com/sherifradwan/ancnative/state/UndoCoordinatorTest.kt` — Added `DeletableType.PREVIOUS_PREGNANCY` and test fixtures.
  13. `app/src/test/java/com/sherifradwan/ancnative/storage/DeletableRegistryTest.kt` — Added `previousPregnancy` golden fixture (`tag = "PRV"`, golden SHA-256 hash & fingerprints) and field variants.
  14. `app/src/androidTest/java/com/sherifradwan/ancnative/storage/ClinicalDeleteRestoreDbTest.kt` — Added `"previous_pregnancy"` to `kinds()` and `PreviousPregnancyKind` (fixed `modelVersion = 3`, `birthMethod = "Vaginal"`, `provenanceKind = "CLINICIAN_ENTRY"`).
  15. `app/src/androidTest/java/com/sherifradwan/ancnative/DeleteUndoUiTest.kt` — Added `prevKind` and 4 UI tests (`deleteConfirmRemovesCardAndShowsUndoBar`, `cancelKeepsTheCard`, `undoRestoresTheCard`, `barDisappearsAfterAboutEightSeconds`).

---

## 3. What Remains To Be Completed

1. **Finish & Commit `PREVIOUS_PREGNANCY` Delete Adapter:**
   - Ensure `emulator-5554` is running.
   - Run `.agents/scripts/run_emulator_test_class.sh com.sherifradwan.ancnative.storage.ClinicalDeleteRestoreDbTest` (140 tests) and `.agents/scripts/run_emulator_test_class.sh com.sherifradwan.ancnative.DeleteUndoUiTest` (33 tests).
   - Run independent frontier review on the `PREVIOUS_PREGNANCY` diff (`AGENTS.md` Level-3 requirement).
   - Stage only the 16 files and commit locally: `logic(obstetric): previous pregnancy delete adapter, undo bar, and delete icon button`.
2. **Part B Visual Polish & Remaining Roadmap Items:**
   - **Lab & Ultrasound Polish** (`.agents/pipeline/design/REDESIGN_SPECS.md` §4.1 & §4.2): compare 360 dp emulator screenshots against `.agents/pipeline/design/d4/index.html` side by side, fix visual gaps (including the floating "Close" button on the status notice card noted on phone).
   - **Patient Directory & Patient Chart** (`REDESIGN_SPECS.md` §4.5 & §4.6).
   - **Obstetric History, Previous Pregnancies, Dating & Clinical Attention Entry** (`REDESIGN_SPECS.md` §4.7).
   - **Attachments Delete Adapter (`r6b6`)**: write spec addendum first (encrypted attachment files on disk are shredded only after the 8s Undo window expires; if undone, file is retained), then implement `AttachmentDeleteAdapter`.

---

## 4. Copy-Paste Continuation Prompt (Antigravity / Claude Style)

Copy and paste the prompt below into the next session/model:

```markdown
Read these files first, in order, before running any commands or making edits:
1. `AGENTS.md` (Level-3 risk map & review rules)
2. `.agents/pipeline/HANDOFF_AGY.md` (Section 0 hard rules, Section 8 delete/Undo architecture, and the latest 2026-11-03 status at the bottom)
3. `.agents/pipeline/DELETABLE_REPORT_AND_HANDOFF.md` (Exact stopping point, 16 modified/untracked files in working tree, and remaining tasks)
4. `.agents/pipeline/PROGRESS_LOG.md` (Last 60 lines)
5. `.agents/pipeline/ADDING_A_DELETABLE_KIND.md` and `.agents/pipeline/design/REDESIGN_SPECS.md`

### Hard Rules (Never Break)
- **NEVER `git push`**. All commits are strictly local on branch `maca-round2`.
- **Database Schema v6 is FROZEN**. Never alter Room entities, table schemas, or migrations.
- **Protect Clinic Data**: Never touch `com.sherifradwan.anc` on any physical phone. Only test against `com.sherifradwan.anc.qa` (via `.agents/scripts/run_emulator_test_class.sh` or `install_qa_build_on_device.sh`).
- **Level-3 Path Rule (`AGENTS.md`)**: Every edit under `app/src` requires an independent frontier-tier review and independent test verification before committing.
- **Script Reuse Rule**: Any command or step needed more than twice must use or become a script in `.agents/scripts/`.
- **Leave Stray Untracked Folders Alone**: Do not stage or delete `.agents/backup-*`, `.agents/fleet-runs/`, `.agents/work/`, etc. Stage only explicit task files.

### Mission — Start Exactly Where We Stopped
1. **Complete & Commit `DeletableType.PREVIOUS_PREGNANCY`**:
   - Inspect `git status -s app/ .agents/checks/`. All 16 files for `PreviousPregnancyDeleteAdapter` are already edited in the working tree and pass `python3 .agents/checks/check_r6c.py` and `.agents/scripts/run_build_gate.sh` (`GATE OK`).
   - Verify `emulator-5554` is online (start `anc-test-api26` or `anc-test-api36` if stopped after server restart).
   - Run `.agents/scripts/run_emulator_test_class.sh com.sherifradwan.ancnative.storage.ClinicalDeleteRestoreDbTest` (140 tests) and `.agents/scripts/run_emulator_test_class.sh com.sherifradwan.ancnative.DeleteUndoUiTest` (33 tests). Fix any failures if found.
   - Run an independent frontier review on the uncommitted diff (`git diff HEAD app/src .agents/checks` + `PreviousPregnancyDeleteAdapter.kt`), resolve any findings, re-verify `check_r6c.py` + `run_build_gate.sh`.
   - Stage the 16 files and commit locally: `logic(obstetric): previous pregnancy delete adapter, undo bar, and delete icon button`.
   - Append verification evidence to `.agents/pipeline/PROGRESS_LOG.md` and `.agents/pipeline/HANDOFF_AGY.md`.

2. **Next Roadmap Tasks (Use `/multi-agent-collaborative-audit` MACA workflow)**:
   - Proceed to **Part B Visual Polish** per `.agents/pipeline/design/REDESIGN_SPECS.md` (§4.1 Laboratory, §4.2 Ultrasound, §4.5–4.6 Directory & Chart, §4.7 Obstetric History / Dating / Clinical Attention) and fix the floating Close button on the deleted status notice card.
   - Compare 360 dp emulator screenshots against `.agents/pipeline/design/d4/index.html` side by side before committing each design round.
```
