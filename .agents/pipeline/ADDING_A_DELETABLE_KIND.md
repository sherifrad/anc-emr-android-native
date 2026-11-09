# Adding a deletable kind (ultrasound, previous pregnancy, visit, attachment)

Delete, Undo, restore, the 8 s Undo bar and the confirm dialog are ONE mechanism keyed by `DeletableType`.
The only per-kind code is the adapter and a few table entries. Never copy medication or lab code.
Stored data is frozen: do not change `canonical()` of an existing kind, content hashes, receipt kind strings, DB schema or migrations.

## Steps (package `com.sherifradwan.ancnative`)
1. `storage/DeletableRecords.kt`: add the entry to `enum DeletableType` and the adapter object to the `listOf(...)` in `DeletableRegistry`; add the `is DemoXxx -> of(...)` line in `adapterFor`. Make the domain model implement `DeletableRecord` (id, patientId, revision).
2. `storage/XxxDeleteAdapter.kt` (copy the shape of `MedicationDeleteAdapter`): implement `DeletableRecordAdapter<T>`: `type`, `fingerprintTag` (new, unique), `deleteReceiptKind`, `restoreReceiptKind`, `section`, three notices, `label`, `canonical` (every user-editable field, never the revision), `load`, `deleteRow`, `insertRow`, `record(patient, id)`. The DAO methods it calls (`delete..(id, patientId, expectedRevision)`, insert) must already exist; adding a DAO query is fine, a schema change is not without an owner decision.
3. Receipt kinds: add `XXX_DELETE` and `XXX_RESTORE` to `ClinicalRecordKind` in `storage/ClinicalCommands.kt`. The names must match the adapter strings exactly.
4. `state/AncState.kt`: add `XXX_DELETED`, `XXX_RESTORED`, `XXX_SAVE_UNDONE` to `SavedNotice` and their `R.string` lines in `stringRes()`.
5. `state/EditorHooks.kt`: add an `EditorHook` object and its `DeletableType.XXX to ...` map entry (tells the shared layer how to close or rebase that kind's open editor).
6. Strings in `values/strings.xml` (and `values-ar` if present): `deletable_type_xxx` (the kind's name), `xxx_deleted`, `xxx_restored`, `xxx_save_undone`. Generic delete/Undo strings already exist and take the kind name as argument.
7. `ui/DeleteUndoUi.kt`: add the `DeletableType.XXX -> R.string.deletable_type_xxx` branch in `labelResource()`.
8. Call sites: on the card or row put `trailing = { DeleteIconButton(DeletableType.XXX, id, label, onDelete) }` with `onDelete = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.XXX, id)) }` (see `ui/patienthub/tabs/VisitsTab.kt` and `ui/patienthub/components/MedicationCard.kt`; `check_r6c.py` allows kind names on call-site lines under `ui/patienthub/`). To offer Undo after a save, call `undoCoordinator.offerSaveUndo(type = DeletableType.XXX, ...)` from the save success path in `AncViewModel`.
9. Tests: JVM `DeletableRegistryTest`: add a `DeleteFixture` to `DeleteFixtures.all` with a hard-coded golden canonical, hash and fingerprints computed outside the code; `everyDeletableTypeValueHasAFullyDescribedAdapter` then covers tag, kinds, notices. Device: add `"xxx"` to `kinds()` in `ClinicalDeleteRestoreDbTest` (with its `Kind` implementation) and a UI case in `DeleteUndoUiTest`.
10. Run `python3 .agents/checks/check_r6c.py` (wiring and no-kind-names check) plus the Gradle gate. This is a level-3 area: needs a frontier review and an independent test review.

## Rules
- One global Undo: one pending Undo, one 8 s timer (`UndoCoordinator`), one `UndoBarHost`, one `DeleteConfirmDialog`. A new delete replaces the offer; never add a second timer, bar or dialog.
- No per-kind copies or branches in `state/UndoCoordinator.kt`, `AncViewModel` delete logic, `RecordsRepository`, `ClinicalDeleteRule` or the shared UI. Kind-specific code lives only in the adapter, registry, strings table and call sites. `check_r6c.py` fails otherwise.
- Never touch another patient or section: every command carries `patientId`, expected patient revision and expected record revision; the Undo bar, notices and failures are scoped to the patient and section of the offer; the cross-patient write is rejected in storage.
- Delete and Undo failures must be visible (never silent); Undo of a save must not clobber an open editor with unsaved changes.
- Lock clears the pending Undo (`UndoCoordinator.clear()`).
