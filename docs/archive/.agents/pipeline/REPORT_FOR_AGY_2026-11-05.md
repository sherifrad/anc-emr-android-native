# Consultant report — ANC EMR (Android) — 2026-11-05

Written by the AutoCoder orchestrator for an independent consultant (agy / Gemini) taking over.
It describes the repository, the rules that must not be broken, exactly what was done in the last
session, what is verified, what is not, and where to resume. Nothing here is aspirational: every
claim is either backed by a command that was run or marked as unverified.

---

## 1. The product in one paragraph

An offline-first antenatal-care EMR for a single private obstetrics clinic (owner: Dr. Sherif).
Native Android, Kotlin + Jetpack Compose, Room + SQLCipher, schema **v6 (frozen)**. No `INTERNET`
permission, `allowBackup=false`, encrypted vault with PIN/biometric unlock. One clinician, one phone.
Wrong data can affect care, so the project treats evidence discipline as part of the product.

## 2. Where things live

| Path | What it is |
|---|---|
| `app/src/main/java/com/sherifradwan/ancnative/` | 101 Kotlin files: `state/` (reducer + drafts + coordinator), `storage/` (Room, commands, vault, delete adapters), `ui/` (~20 screens + `DesignSystem`/`Components`), `model/` (+ `model/codex` catalogue), `security/` |
| `app/src/test/` | 92 JVM test files — **979 tests**, all green at `884be6a` |
| `app/src/androidTest/` | 17 instrumented files — delete/restore DB matrix, UI undo tests, batch DB test |
| `.agents/pipeline/PROGRESS_LOG.md` | Append-only source of truth. **Read the last ~10 entries first.** |
| `.agents/AI_HANDOFF_LEDGER.md` | Task handoffs `H-*`, review verdicts, approvals |
| `.agents/pipeline/OWNER_DECISIONS.md` | D1–D25; clinical/product decisions. D25 is the lab batch |
| `.agents/pipeline/SPEC_LOGIC_B1_LAB_BATCH.md` | The batch spec and its tests |
| `.agents/SPEC_AND_AUDIT.md` §6 | Outcome oracles ORACLE-B1..B8 |
| `.agents/pipeline/HANDOFF_AGY.md` | Long-running handoff; the 2026-11-05 status block supersedes earlier sections |
| `.agents/checks/check_r*.py` | 48 textual invariant checkers, run by `run_static_checks.sh` |
| `.agents/pipeline/design/rev-b/lab-batch.html` | **The approved mock** for the batch screen (6 slices) |
| `app/build/print-sample/report.html` | Printed-report sample produced by `PrintSampleDumpTest` |

## 3. Commands that matter

```bash
./gradlew --offline :app:testDebugUnitTest          # 979 tests, ~30 s
bash .agents/scripts/run_build_gate.sh              # unit + lint + release compile + androidTest compile
bash .agents/scripts/run_static_checks.sh           # 48 checkers; must print STATIC CHECKS OK
bash .agents/scripts/run_emulator_test_class.sh <FQCN> emulator-5554   # keeps app installed
bash .agents/scripts/install_qa_build_on_device.sh <serial>            # QA copy, in place, never uninstall
```

Emulator: AVD `anc-test-api26`, 360 dp. Start it **with a window** when the owner wants to watch:
`/opt/homebrew/share/android-commandlinetools/emulator/emulator -avd anc-test-api26 -port 5554`.

## 4. Invariants a consultant must not break

1. **Never push.** The branch is local-only (`maca-round2`, 152 commits ahead of `origin/main`).
2. **Never touch the clinic app** `com.sherifradwan.anc`. Only the QA copy `com.sherifradwan.anc.qa`
   (PIN 121212, demo data). Update it in place; never uninstall.
3. **Schema v6 is frozen.** No entity, table, column or migration change. Features are built around it
   (the batch is an in-memory draft + one receipt; its id lives only in the receipt/undo offer).
4. **One rule, one function.** The shared layers (`UndoCoordinator`, `RecordsRepository`, `AncState`)
   must not name a record kind; call sites supply `DeletableType`. `check_r6c` enforces it.
5. **Revision + content-hash guards.** Every delete/restore checks the expected revision and the
   content hash inside one transaction; restore is *tombstone + 1*. Do not weaken this.
6. **Tests are evidence.** A passing test that cannot fail is a defect. The evidence registry
   (`.agents/EVIDENCE.json`) hashes dependencies — re-run after a fix, never before.
7. **QA copy only on the phone; connected instrumented runs are forbidden there.**

## 5. What was accomplished in the last session

### 5.1 Attachment delete / confirm / Undo — mission closed

- `b6af671`: cleanup failure surfaced (`DeleteMessageKind.CLEANUP_FAILED`), every exit path reports,
  unlock sweep (`AttachmentOrphanSweeper`), restore revision fixed.
- `cff3841`, `773131f`, `0b417c8`: single confirmation for attachment delete, human label instead of
  `att_<uuid>.webp`, viewer strings moved to `strings.xml`, shared `image_unavailable`.
- Independent review **FAILED** it (3 real blockers): the sweep deleted **ultrasound scan photos**
  (references live in `ultrasound_scans.image_uris`, not `patient_attachments`), `clear()` during an
  in-flight Undo deleted the restored file, and the cleanup message was wiped in the same frame.
- `7ac25c5`: all three fixed, red-before-green tests. `4adb5e0`: attachments added to both suites,
  viewer Cancel fixed, checker gaps closed. `69cdc57`: re-review nits.
- Re-review: **PASS WITH NITS, 0 blockers**. Reports tracked at
  `.agents/pipeline/review_attachment_cleanup_2026-11-05.md` and `..._rereview_2026-11-05.md`.
- Device verification on the owner's phone (QA copy): delete → wait → file gone; Undo → record and file
  intact; unlock sweep removed a planted orphan. 36/36 UI and 168/168 DB tests on the emulator.

### 5.2 Print defects P-1/P-2 — already fixed, backlog corrected

`c7f1031` (2026-10-31) had already fixed the mid-word breaks and the duplicate stamp; the backlog was
stale. Verified by measuring line boxes in headless Chrome at 794 px and 380 px and by counting the
stamp string (exactly one).

### 5.3 Laboratory batch (owner-approved mock; D25; B = batch model)

Implemented without a schema change. Commits in order:

| Commit | Slice | What it does |
|---|---|---|
| `fcc7a02` | storage | `SaveLabBatchCommand` (N rows, one receipt, patient +1), `DeleteLabBatchCommand` / `RestoreLabBatchCommand` (all-or-nothing, revision + hash per row) |
| `111056f`, `0e66cdd` | undo | One offer for the whole batch; executes exactly one command; shared layer stays kind-free |
| `da6ce5c` | draft | `LabBatchDraft` + events (add/remove/result/range/flag/note/tab/cursor/date/lab) |
| `cc32bb6` | wiring | `startLabBatch`, `saveLabBatch`, validation → draft errors, one-action Undo offer with pluralised label, `hasDirtyDraft` |
| `7ccadb1` | persistence | `DraftScreenKind.LAB_BATCH`, autosaved, encoded list, restore; recovery prompt label |
| `884be6a` | UI | `LabBatchScreen` (Add/Enter/Review), entry button, catalogue split |

Catalogue split (`884be6a`): **CBC = Hb, Hct, MCV, MCH, WBC, PLT** (no default ranges — typed from
each report), **Urine = protein, glucose, ketones, blood, nitrite, leukocyte esterase, pus cells, RBCs**.
Booking/pre-eclampsia/GDM/anaemia panels list the elements; two new panels (CBC, Urine) can be added alone.

### 5.4 MACA audit of the batch work

- Oracles B1–B8 added to `SPEC_AND_AUDIT.md`; four batch tests registered in `TEST_MANIFEST.json`;
  evidence re-recorded after the fix commits.
- Round 1 **FAIL** (local subagent, same-model): blank batch date accepted (`isValidIsoDate` returns
  true for blank; the batch lacked the `isBlank()` check every other command has) and no draft
  persistence. Fixed in `cc6239b`.
- Round 2 **FAIL on evidence only**: my evidence entries predated the fix and hashed the parent blobs.
  Correct catch. Re-ran after the fix; also derived the restore revision from the delete receipt
  (`cf89342`) instead of trusting the caller. Two-round limit reached.

## 6. Verified vs not verified (at `884be6a`)

**Verified**
- `run_build_gate.sh` → GATE OK; 979 unit tests, 0 failures.
- `run_static_checks.sh` → STATIC CHECKS OK.
- Earlier in the session, on the API 26 emulator at 360 dp: `DeleteUndoUiTest` 36/36,
  `ClinicalDeleteRestoreDbTest` 168/168, `LabBatchDbTest` 5/5 (11-row delete/restore, archived patient).

**Not verified — do not assume**
- **The batch screen has never been launched** on a device or emulator. It compiles; no UI test, no
  screenshot, no navigation run since it was written.
- Instrumented suites were **not re-run after** the catalogue split and the screen commit.
- The draft-recovery prompt for a batch still requires an open editor with that session; with the screen
  now present, a cold-start recovery has not been exercised.
- **`cf89342`, `cc32bb6`, `7ccadb1`, `884be6a` are unreviewed.** The platform subagent endpoint returns
  HTTP 403 (three attempts); the owner authorised `agy` as a reviewer and it was dispatched, but the run
  was stopped before producing output.
- No batch UI tests exist (add/enter/review/save/undo through Compose).
- Batch undo cannot rebase a stale patient revision (fails closed, window expires).
- `LabBatchScreen` labels are hardcoded English (app is English-only per D16, but `strings.xml` is the
  house rule).

## 7. Where to resume (in order)

1. **Smoke the batch screen** on the emulator: start it, add CBC, type a result, Review, Save, Undo.
   Fix what breaks. This is the first thing to do.
2. **Batch UI tests** in `DeleteUndoUiTest` style (add → enter → review → save → one-action undo), plus
   an 11-row case.
3. **Catalogue tests**: defaults contain 6 CBC + 8 urine elements, panels resolve, no default CBC range.
4. **Draft recovery for a batch** end to end (kill the app mid-batch, reopen, Continue).
5. Deferred audit items: stale-revision rebase, `prepare()`-style validation surface for delete/restore,
   restore-archived and revision-branch guard tests.
6. Then the older queue: `loadSnapshot()` whole-DB read, git history hygiene before any push, R8.

## 8. Known traps

- `check_r6c` R6C1 flags any `DeletableType.<KIND>` outside an allowed call-site shape; the batch offer
  therefore takes `type` as a parameter.
- The static runner includes every `check_r*.py`; a new checker must be named that way to be picked up.
- `.agents/EVIDENCE.json` hashes dependencies: after changing a file, re-run the manifest entry; recording
  by hand is not evidence.
- The git index tracks ~71,900 agent artefacts (backups/runs) from an accidental 2026-11-03 commit; do not
  add more, and do not push before the owner decides on history hygiene.
- `adb` USB drops on this machine; a "Use USB to" dialog appears after replug and must be cancelled without
  changing settings.

## 9. Evidence index

| Evidence | Where |
|---|---|
| Progress log (authoritative) | `.agents/pipeline/PROGRESS_LOG.md` |
| Ledger (handoffs, reviews, approvals) | `.agents/AI_HANDOFF_LEDGER.md` |
| Attachment review + re-review | `.agents/pipeline/review_attachment_cleanup_2026-11-05.md`, `..._rereview_2026-11-05.md` |
| Batch review round 1 | `.agents/review-r1.md` |
| Batch spec + oracles | `.agents/pipeline/SPEC_LOGIC_B1_LAB_BATCH.md`, `SPEC_AND_AUDIT.md` §6 |
| Test manifest / evidence | `.agents/TEST_MANIFEST.json`, `.agents/EVIDENCE.json` |
| Approved batch mock | `.agents/pipeline/design/rev-b/lab-batch.html` |
| Device screenshots (git-ignored) | `evidence/attachment-audit/device-2026-11-04/` |
