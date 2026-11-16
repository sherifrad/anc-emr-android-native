# Review — laboratory batch (a22c059..HEAD)

Reviewer: local subagent, fresh context, same model family as the author (independence: same-model).
Scope: the four batch commits (storage, undo offer, draft) against SPEC_LOGIC_B1_LAB_BATCH.md and ORACLE-B1..B8.

Verdict: **FAIL** — 2 hard blockers, 7 should-fix, several nits.

## Hard blockers

1. `storage/LabBatchCommands.kt:74/:138` — a blank batch date passes validation and is written. `ClinicalVocabulary.isValidIsoDate` returns true for blank (written for optional dates); every other dated command adds `date.isBlank()`. Save/restore omitted it, so a cleared date reaches the DB as an empty string with a success receipt.
2. `state/LabBatchState.kt` (+ DraftMapping/AncState) — the batch draft is never persisted and `hasDirtyDraft` omits it, contrary to SPEC §2/D9. Unreachable from the UI today, but the slice was declared complete without listing the omission.

## Should-fix

S1. Batch delete/restore skip the archived-patient guard.
S2. A batch undo cannot recover from a stale patient revision (DeleteLabBatchCommand is not a RecordUndoCommand, so the coordinator cannot rebase it).
S3. Restore revision is caller-supplied and contradicts the delete receipt (tombstone vs tombstone+1).
S4. `add()` de-duplicates only against existing rows, not inside the incoming list.
S5. Cancel wipes every editor draft via `clearDrafts()`.
S6. A batch validation failure has no user-visible effect (errors dropped).
S7. Coverage: no 11-row save+undo, no receipt-kind assertions, revision branch of the delete guard untested.

## Nits
- Misplaced `PayloadCleanupFailed` KDoc above `StartLabBatch`.
- `LabBatchDraftTest.kt:71` assertion can never fail; remove-last-row case untested.
- `LabBatchUndoTest` asserts the label it passed in; no production label builder.
- `labBatchEntryHash` has no production caller.
- Removing the last row leaves `tab = ENTER` with an empty list; REVIEW allowed with 0 items.
- `batchId` is decorative: refs are not tied to the batch.
- Restore reports RECORD_CONFLICT where save distinguishes CROSS_PATIENT_WRITE.
- `saveLabBatch` stores `labName = ""` verbatim.
- No static check fails when a command kind has no ... (report truncated in transit).

## Oracle coverage
B1–B5 storage-level covered; B6–B8 unit-level covered; none reachable from the app yet (no ViewModel/UI caller), as the log states.

## Fixes applied (round 1 -> commit `cc6239b`)
Blank-date guard on save and restore; archived guard on batch delete/restore; cancel now clears only the batch draft; validation errors land on the batch draft; `add()` de-duplicates within one call; restore is tombstone + 1 (revision 3); KDoc moved; tests: 11-row delete/restore with receipt kinds, archived patient, blank date, duplicate-in-one-call, real remove-last-row.
Deferred and now stated explicitly: draft persistence + `hasDirtyDraft` (must land before the B2 screen), stale-revision rebase for the batch undo (fail-closed, undo dies until the window ends), and a static check for command kinds.
