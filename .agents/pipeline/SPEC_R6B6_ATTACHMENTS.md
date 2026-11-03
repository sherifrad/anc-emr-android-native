# R6B6 Attachments Delete Adapter — Spec Addendum (two-phase)

## Context
Attachments (patient_attachments) have a revision column (DemoAttachment.revision, PatientAttachmentEntity.revision), so the generic DeletableRecordAdapter contract (expectedRevision) fits. However, the existing DAO deleteAttachment(id, patientId) lacks a revision guard — a new revision-checked query is needed (DELETE ... AND revision = :expectedRevision). Attachments also reference an encrypted file on disk, requiring two-phase delete: the DB row is deleted and a receipt is written immediately; the encrypted file is removed only when the Undo window closes (UndoExpired, failed-undo, replacement by newer offer, or lock). Restore is refused with RECORD_CONFLICT after finalize.

## Design (D629)

### Interface changes
- DeletableType.ATTACHMENT added to enum + registry.
- New PayloadFileAdapter interface (separate from DeletableRecordAdapter) so the other 5 adapters stay unchanged. Coordinator checks `adapter as? PayloadFileAdapter`.
- PayloadFileAdapter members:
  - val hasPayloadFile = true
  - suspend fun finalizeDelete(record: DemoAttachment, context: Context): FinalizeResult — removes the encrypted file (via PatientAttachmentVault.deleteAttachment) after the Undo window closes. Idempotent: missing file counts as success. Retry once on failure (finalize retry failure), then log and let unlock-time orphan sweep pick it up. Context MUST be application context to avoid Activity leak.
- FinalizeResult sealed class: Success, AlreadyGone, Failed(reason: String).
- DeletableRecordAdapter needs a `fun record(patient: DemoPatient, id: String): T?` branch for DemoAttachment in DeletableRegistry. Add `is DemoAttachment` branch to contentHash and labelOrNull.

### Vault change
- PatientAttachmentVault.deleteAttachment must become idempotent: missing file = success (return true). Distinguish missing (true) from real failure (false). Catch SecurityException internally, return false. Caller (finalizeDelete) retries once then logs.
- File delete does not require the encryption key (plain file delete), so finalize works even when vault is locked.

### DAO change
- New query: `DELETE FROM patient_attachments WHERE id = :attachmentId AND patient_id = :patientId AND revision = :expectedRevision` returning affected count. Used by AttachmentDeleteAdapter.deleteRow. Stale revision returns 0 → STALE_REVISION error.

### ClinicalRecordKind
- Add ATTACHMENT_DELETE and ATTACHMENT_RESTORE values.

### SavedNotice
- Add ATTACHMENT_DELETED, ATTACHMENT_RESTORED, ATTACHMENT_SAVE_UNDONE.

### WorkspaceSection
- Add ATTACHMENTS section (new, not reuse existing).

### Adapter flow
- deleteRow: deletes the patient_attachments row (revision-guarded) + writes receipt. File stays on disk during Undo window.
- insertRow (restore): re-inserts row; refused with RECORD_CONFLICT if file was already removed (finalizeDelete ran). File-exists check inside the DB transaction. Needs shared mutex between restore and finalize to avoid races (filesystem is not transactional). Lock order: mutex first, then DB transaction.
- On UndoExpired / replacement / lock clear -> call finalizeDelete to remove the file.
- Finalize skipped on successful Undo (Committed path).

### UndoCoordinator changes
- "Ambiguous" = the restore command was submitted but its outcome (success/failure) is not yet known (e.g., in-flight coroutine, pending DB transaction).
- PendingUndo must capture the DemoAttachment record at beginDelete (before the row is gone) so finalizeIfNeeded has the fileName.
- UndoCoordinator needs application Context injection (constructor) for finalizeDelete.
- Single private finalizeIfNeeded(pending) function called from expiry, failed-undo (undo operation failure), replacement, and lock-clear paths.
- finalizeIfNeeded under the mutex re-checks that the row is absent (by id and patient) before deleting the file. If the row exists (restore committed), skip file delete. This prevents deleting a file behind a live row.
- Race handling: if finalize fires while undo is in flight or ambiguous, defer finalize until the operation completes. Use a shared mutex between restore and finalize. Lock order: mutex first, then DB transaction.
- Replacement by newer offer: finalize the older offer's file before arming the new one.
- clear() runs finalizeIfNeeded before dropping any state.
- Finalize must NOT be gated by sessionStillCurrent — it must run even when the epoch has changed (lock-clear case). Add rule: finalizeIfNeeded ignores the epoch check.

### Existing delete path removal (r6b6 scope)
- AncEvent.DeleteAttachment (AncState.kt:762 data class, AncState.kt:982 reducer branch, AncViewModel.kt:201 dispatch, AncViewModel.kt:333 handler) currently deletes file immediately with no undo. This event and all its references MUST be removed entirely in r6b6. The only delete path becomes beginDelete(ATTACHMENT, ...).
- VaultController.deleteAttachment (line 477): deprecate — keep for orphan sweep only, remove from all UI paths.
- RecordsRepository.deleteAttachment (line 887): deprecate — keep for orphan sweep only.
- ClinicalAttachmentViewerDialog deleteAttachmentBtn (ui/ClinicalAttachmentViewerDialog.kt): reroute to dispatch(AncEvent.RequestDelete(pid, ATTACHMENT, id)).
- WorkspaceScreen.kt:275 dispatches DeleteAttachment — must be rerouted to RequestDelete.

### Crash recovery (orphan sweep)
- UndoCoordinator holds pending undo only in memory. A process kill during the Undo window orphans the file.
- Mitigation: unlock-time orphan sweep that removes encrypted files with no corresponding patient_attachments row, scoped per patient.
- Age check: only sweep files older than 60 seconds (to avoid deleting in-progress imports/captures). Files younger than 60s are left for the next sweep. Age is measured by file creation time (app-recorded), not filesystem mtime, to avoid preserved-timestamp imports.
- Import/capture must hold the same per-patient mutex across "write file, then insert row" so the sweep (which also takes the mutex) cannot see a half-written file.
- A delete that survives a crash (undo was open at kill time) is purged by the sweep as an orphan — this is INTENDED; the undo cannot be restored after process death.
- Failed finalize (retry exhausted) needs a persisted marker (row in a pending_finalize table or a flag on the row before deletion) so the next unlock sweep picks it up even for patients not opened in that session. The sweep scans all patients, not just opened ones.
- Owner: new function in PatientAttachmentVault or RecordsRepository, called from VaultController on unlock.
- Sweep takes the same mutex as restore/finalize to avoid deleting a file being restored. Mutex owner: per-patient mutex in RecordsRepository. If sweep holds mutex while finalize waits, finalize blocks until sweep completes (no deadlock — finalize acquires mutex after sweep releases).
- Needs its own tests: simulated kill, orphan cleanup, no false positives on in-progress imports.

### Backup check
- EncryptedBackupManager exports attachment rows. Backup checks row existence only (not file existence). An attachment whose row is deleted but whose file remains (in-window) is EXCLUDED from backup.
- If file is missing but row exists: backup includes the row (metadata only), logs warning, does not fail. Row existence is the sole criterion.

### Test plan (required for level-3 paths)
1. No file removal before expiry (file exists after deleteRow, before UndoExpired).
2. Restore refused after finalize (RECORD_CONFLICT).
3. Finalize on lock (file removed after clear()). Finalize ignores epoch check.
4. Finalize skipped on committed undo (file survives successful restore).
5. Cross-patient isolation (patient B's files untouched).
6. Stale-revision guard on new DAO query (returns 0, STALE_REVISION error).
7. Idempotent finalize (missing file = success, no crash).
8. Retry-once then log (simulated finalize failure, orphan sweep picks up).
9. Orphan sweep: simulated kill leaves orphan, sweep removes it; no false positives on in-progress imports.
10. Backup excludes in-window deleted attachments (row gone). Backup includes row when file missing (logs warning).
11. Race: finalize fires while restore is in flight → deferred via mutex, no data loss or crash.
12. Finalize after restore committed → file preserved (re-check under mutex).
13. AncEvent.DeleteAttachment removed (no references in app/src/main). WorkspaceScreen and ClinicalAttachmentViewerDialog dispatch RequestDelete.
14. Lock-clear finalize while vault locked → file removed (no key needed).

## Hard rules
- Never remove the file before Undo window expires.
- Restore refused after finalize (RECORD_CONFLICT).
- Lock clear triggers finalize (file removal), not just forget.
- Finalize skipped on successful Undo (Committed path).
- Finalize ignores session epoch check.
- Finalize re-checks row absence under mutex before file delete.
- Cross-patient isolation still applies (R654).
- finalizeDelete catches SecurityException and false returns; never crashes UI.
- PatientAttachmentVault.deleteAttachment idempotent: missing file = success.
- Shared mutex (per-patient, owned by RecordsRepository) between restore, finalize, and orphan sweep. Lock order: mutex first, then DB transaction.
- Import/capture holds same mutex across write-file-then-insert-row.
- Application context only in UndoCoordinator.