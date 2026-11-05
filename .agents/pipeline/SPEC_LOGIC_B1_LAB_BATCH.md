# SPEC — Logic round B1: laboratory batch entry (approved 2026-11-05)

Mock: `.agents/pipeline/design/rev-b/lab-batch.html` (approved). Owner decisions: D25.
Hard constraint: **Room schema stays v6. No entity, table, column or migration change.** History is not rewritten.

## 1. Goal
Enter a whole lab batch (panels plus single tests), review it, save once, and undo the whole batch as one action.

## 2. Model (in memory only)
```
LabBatchEntry(testId, testName, category, result, unit, referenceRange, isAbnormal, notes)
LabBatchDraft(patientId, labName, date, entries: List<LabBatchEntry>, cursor: Int)
```
- The draft lives in `AncState` (like the other drafts). **Persistence is deferred to the B2 screen slice** — this
  slice is in-memory only, and the screen must not ship until the draft record and `hasDirtyDraft` exist (review r1, blocker 2).
- Nothing is written to the database until Save.

## 3. Save (one command, one receipt)
`SaveLabBatchCommand(operationUuid, patientId, expectedPatientRevision, labName, date, entries)`
- Validates every entry first; one invalid entry rejects the whole command with that entry's reason.
- Inserts N `lab_investigations` rows, each `revision = 1`, in one transaction.
- Bumps the patient revision once.
- Writes **one** `clinical_command_receipts` row: `recordKind = LAB_BATCH_SAVE`, `recordId = batchId` (= operationUuid), `committedRevision = 1`, fingerprint over the ordered canonical entries.
- Returns the inserted records (id, revision, content hash) so the Undo offer can be built without a schema column.

## 4. Undo (one action, 8 s window)
- After a committed batch save, offer one Undo labelled "N lab results" (patient named, as the shared bar does).
- `DeleteLabBatchCommand(operationUuid, patientId, expectedPatientRevision, entries: List<RecordRef>)`
  - Deletes exactly those rows and only if every row still matches its revision and content hash.
  - One receipt: `LAB_BATCH_DELETE`. Patient revision +1.
- `RestoreLabBatchCommand(...)` re-inserts the same N rows, each `revision = 2` (batch tombstone + 1), one receipt: `LAB_BATCH_RESTORE`. Patient revision +1.
- All-or-nothing: any mismatch aborts the transaction; nothing is half-deleted.
- No payload files, so no cleanup path is involved.
- After the window, the batch offer disappears; the rows are deleted individually through the existing per-record delete mechanism.

## 5. Validation and flags
- Result: required for every entry. Numeric entries accept a decimal; qualitative entries accept a codex choice.
- Reference range optional (CBC ranges are typed from the report per D25); when present, the auto flag applies and may be overridden.
- "Mark empty as normal" applies only to qualitative entries, never to numeric CBC elements.

## 6. Panel composition (data, not schema)
- CBC = Hb, Hct, MCV, MCH, WBC, PLT (six `LabCodexItem`s, category Hematology, unit only, no default range).
- Urine Routine = protein, glucose, ketones, blood, nitrite, leukocyte esterase, pus cells, RBCs (eight items; the four microscopy/chemistry ones as choices).
- `LabCategoryGroup` "CBC" and "Urine Routine & Microscopic" list those names; existing panels keep working.
- Per-batch "Choose" is UI only: it filters which entries are added, it does not edit the panel.
- Settings keeps editing items and panels (name + elements). "Save this selection as a panel" creates a `LabCategoryGroup` with `isCustom = true`.

## 7. UI contract (design round B2, after this)
Tabs: **Add → Enter → Review**, starting on Add when the draft is empty. Panel rows carry "Choose" + "＋ Add N". Review groups rows under panel headers, shows pending rows, and ends with one "Save N results". Batch header: one date + one lab for the batch.

## 8. Tests (write first, prove red)
Unit:
- batch command rejects one invalid entry and writes nothing;
- batch insert writes N rows and exactly one receipt, patient revision +1;
- delete batch requires every hash/revision; one mismatch deletes nothing;
- restore writes N rows at revision 2 with one receipt;
- undo coordinator offers one batch undo and executes delete/restore as one command.
Instrumented (emulator):
- 11-row batch save then batch undo leaves zero rows; restore returns all 11 with revision 2;
- cross-patient batch delete is rejected;
- a partially modified row rejects the whole delete.

## 9. Risks
- A batch undo cannot rebase a stale patient revision (the delete command is not a `RecordUndoCommand`); it fails closed and the window expires (review r1, S2).
- Receipt semantics: a batch receipt has no single row, so `latestClinicalReceiptForRecord` must not be used for batch ids.
- The existing per-record delete must keep working; batch ids never appear in `patient_attachments`-style lookups.
- Draft size: 14 entries × fields stays well under the 64 KB draft payload limit; add a test for the largest panel.
