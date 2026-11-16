# Slice 19: Shell and shared dialogs

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s19_shared_dialogs.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Components as built: `ui/components/RecordTokens.kt` (`RecordType`, `RecordShapes`, `RecordSpacing`, `FieldFamilies`, `LocalFieldFamily`), `ui/components/FormFieldLabel.kt` (`FormFieldLabel`), `ui/components/RecordFilterChip.kt` (`RecordFilterChip`), `ui/patienthub/components/EditorSheetParts.kt` (`DiscardChangesDialog`), `ui/Components.kt` (`editorFieldColors`, `minimumTouchTarget`), and `ui/Theme.kt` (`ClinicalNavy`, `StatusModerateBg`, `StatusModerateBorder`, `StatusModerateText`, `StatusModerateDarkBg`, `StatusModerateDarkBorder`, `StatusModerateDarkText`, `StatusHighDarkBg`, `StatusHighDarkBorder`, `StatusHighDarkText`, `isDarkTheme`). Read them; do not re-spec them.

- **Owner direction**: `antislop: DURING`, dials ENERGY 1 / RHYTHM 1 / MOTION 1 (pressed and focus states only, no new animation). Presentation only: bring `DeleteConfirmDialog`, `UndoBarHost`, the shell dialogs in `AncApp.kt`, `AncDatePickerDialog` / `ClinicalDateField`, and `InAppCameraDialog` onto the Record design system (`10.dp`/`16.dp` radii, `0.dp` elevation, `>= 12.sp`, `>= 48.dp` targets, error-colored destructive actions, high-contrast camera controls), and enforce **Owner rule (2026-11-16)**: every date field in the app opens the date picker; no date is typed by hand (presentation only: zero changes to stored ISO values or EDD/GA calculations).
- **Branch hygiene**: run `.agents/scripts/git_audit.sh` before starting and before pushing; update `docs/PROJECT_STATE.json` + run `.agents/scripts/project_state.py render` + update `.agents/pipeline/HANDOFF_AGY.md` in the same commit on `redesign-2026-11-15`.
- **Never touch**:
  - `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `storage/VaultController.kt`
  - `storage/DeletableRecords.kt`, `storage/*DeleteAdapter.kt`, `storage/CameraCaptureHelper.kt`, `storage/BoundedBitmap.kt`
  - `state/UndoCoordinator.kt` (protected!), `state/AncReducer.kt`, `state/AncState.kt`, `state/AncEvent.kt`, `state/AncViewModel.kt`, `state/DraftRecovery.kt`, `state/ClinicalDraftReducers.kt`, `state/DatingDraftRule.kt`, `state/MedicationReducer.kt`, `state/DraftMapping.kt`
  - `model/**` (`ClinicalModels.kt`, `GestationalAgeEngine.kt`, `DeliveryOutcome.kt`, `DatingSummary.kt`), `*Generator*`
  - `ui/AncApp.kt` lines 1-288 (navigation, `BackHandler`, `AppLockScreen` / `VaultGateScreen` switches, top bar, `PatientHubScreen` / editor route switch, `UndoBarHost` gate, `SecuritySettingsDialog` call)
  - `ui/SecuritySettingsDialog.kt`, `ui/AppLockScreen.kt`, `ui/VaultGateScreen.kt`, `ui/CatalogSettingsSections.kt`, `ui/patienthub/PatientHubScreen.kt`
  - `ui/Theme.kt`, `ui/Components.kt`, `ui/components/**`, `ui/patienthub/components/EditorSheetParts.kt`
  - `ui/PatientEditor.kt`, `ui/PreviousPregnancyEditor.kt`, `ui/DatingEditor.kt`, `ui/VisitEditor.kt`, `ui/VisitEditorSteps.kt`, `ui/UltrasoundEditor.kt`, `ui/LabEditor.kt`, `ui/LabEditorComponents.kt`, `ui/LabBatchScreen.kt`, `ui/patienthub/components/MedicationForm.kt` (`DeliveryPlanSheet.kt` is also untouched unless Decision 2 is approved)

## What MUST NOT change (invariants)

The reviewer will inspect the diff with `git diff -w`. Every behavior, callback, state transition, event dispatch, timing rule, and file-handling path below MUST remain unchanged:

1. **Undo timing and dispatch (`UndoCoordinator.kt` untouched, `DeleteUndoUi.kt`, `AncApp.kt`, `PatientHubScreen.kt`)**:
   - Keep `UndoCoordinator`'s 8-second (`8_000L`) expiry window, `MAX_UNDO_RESTARTS`, `cleanupPending`, and `sessionStillCurrent(epoch)` untouched in `state/UndoCoordinator.kt`.
   - Keep `AncApp.kt` line 270 hosting gate: `if (state.medicationSheetDraft() == null && state.patientDraft?.deliveryPlan == null) UndoBarHost(state, onEvent)` (so sheets own the `UndoBarHost` in their `footer` slot and two bars never appear at once).
   - In `UndoOfferBar`: keep `val running = offer.phase == UndoPhase.RUNNING`, `onClick = { onEvent(AncEvent.UndoRequested(offer.token)) }`, `enabled = !running && !deleteRunning`, `LiveRegionMode.Polite`, and the exact spoken `contentDescription` (`undo_message_deleted` / `undo_message_saved` / `no_patient` variants).
   - Keep `DeleteMessageBar`: `LiveRegionMode.Assertive`, all four `DeleteMessageKind` branches (`DELETE_FAILED`, `UNDO_FAILED` for `SAVE` vs `DELETE`, `UNDO_BLOCKED`, `CLEANUP_FAILED`), `patientClause(message.patientName)`, and `onClick = { onEvent(AncEvent.DismissDeleteMessage) }`.

2. **Delete and restore events (`DeleteUndoUi.kt`)**:
   - Keep `DeletableType.labelResource()` mapping for all 6 types (`MEDICATION`, `LAB`, `ULTRASOUND`, `VISIT`, `PREVIOUS_PREGNANCY`, `ATTACHMENT`).
   - Keep `DeleteIconButton`: `DeletableRegistry.labelOrNull(type, record) ?: return` safety guard, `enabled` and `dimmed` semantics, `delete_record_description` contentDescription, and `tag ?: "delete-${type.name.lowercase()}-$recordId"`.
   - Keep `DeleteConfirmDialog`: `state.deleteConfirmation ?: return`, `DeletableRegistry.of<DeletableRecord>(pending.type).record(it, pending.recordId)` null guard with `LaunchedEffect(pending) { onEvent(AncEvent.CancelDelete) }`, `onDismissRequest = { onEvent(AncEvent.CancelDelete) }`, confirm `onClick = { onEvent(AncEvent.ConfirmDelete) }`, dismiss `onClick = { onEvent(AncEvent.CancelDelete) }`.

3. **Draft save, recovery, and discard logic (`AncApp.kt` dialogs only)**:
   - Keep `unlocked && !isAppLocked && securitySettings?.needsPinSetup != true` guards on all dialogs (`showNewPatientInfo`, `DeleteConfirmDialog`, `showDiscardDialog`, `draftRecoveryPrompt`).
   - Keep `showNewPatientInfo`: `onDismissRequest = { onEvent(AncEvent.HideNewPatientInfo) }` and close button `onClick = { onEvent(AncEvent.HideNewPatientInfo) }`.
   - Keep `showDiscardDialog`: `onDiscard = { onEvent(AncEvent.ConfirmDiscard) }`, `onKeep = { onEvent(AncEvent.KeepEditing) }` (including back press / outside tap calling `KeepEditing`).
   - Keep `draftRecoveryPrompt`: exhaustive `DraftScreenKind` mapping (all 10 enum entries), `draftAge(System.currentTimeMillis(), recoveryPrompt.record.updatedAtMillis)` mapping (`LessThanMinute`, `Minutes`, `Hours`, `Days`), `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)` with `onDismissRequest = {}`, `AcceptDraftRecovery` on Continue, `DiscardDraftRecovery` on Discard and start fresh, and `if (recoveryPrompt.isStale)` gate for the stale warning.

4. **Date value mapping and EDD/GA calculations (`AncDatePicker.kt`)**:
   - Keep `initialMillis` parsing: `(parseIsoDateOrNull(initialDateText) ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()`.
   - Keep `onDateSelected` formatting: `Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().toString()` (strict ISO `YYYY-MM-DD`), followed by `onDismiss()`.
   - Keep `Today` action output: `onValueChange(LocalDate.now().toString())`.
   - Zero changes to stored date values, `DatingDraftRule`, `GestationalAgeEngine`, `DeliveryOutcome`, or `PreviousPregnancyFormRules`.

5. **Camera capture and file handling (`InAppCameraDialog.kt`)**:
   - Keep `CameraSession` (`inFlight`, `gone`, `previewView`), `STILL_MAX_SIDE = 1600`, and `ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY`.
   - Keep `Manifest.permission.CAMERA` check + `ActivityResultContracts.RequestPermission()` launcher + `LaunchedEffect(Unit) { if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA) }`.
   - Keep `DisposableEffect(Unit)` `onDispose` cleanup (`session.gone = true`, delete `session.inFlight`, delete uncommitted `captureFile` via `CameraCaptureHelper.deleteIfCaptureTemp`).
   - Keep `DisposableEffect(lifecycleOwner, live)` `ProcessCameraProvider.getInstance(context)` binding (`CameraSelector.DEFAULT_BACK_CAMERA`, `preview`, `imageCapture`) and `provider?.unbindAll()` on dispose.
   - Keep `discardCapture()`, `takePhoto()` (`if (capturing) return`, `CameraCaptureHelper.createCameraTempFile(context)`, `view.display?.rotation ?: Surface.ROTATION_0`), `BoundedBitmap.decodeFile(shot.absolutePath, STILL_MAX_SIDE)`, and `Use photo` ordering (`committed = true; onCaptured(CameraCaptureHelper.uriFor(context, shot))`).

## Date-field audit table (`app/src/main/java`, OWNER RULE 2026-11-16)

Grep of `AncDatePickerDialog`, `ClinicalDateField`, `DatePicker`, and all `OutlinedTextField` / `EditorField` calls across `app/src/main/java/` (verified 2026-11-16):

| # | File & line | Tag & label | Current widget | Picker-driven? | Accepts typed text today? | Notes & test impact |
|---:|---|---|---|---|---|---|
| 1 | `ui/VisitEditor.kt:218` | `visitDate` (`Visit date (YYYY-MM-DD)`) | `ClinicalDateField(showTodayButton = true, externalLabel = true, useRecordStyle = true)` | Partial (trailing `visitDateCalendarButton` + `visitDateTodayButton`) | **Yes** (`OutlinedTextField` is not `readOnly`) | Error-recovery view; `AncWorkflowTest` checks `SemanticsProperties.EditableText` on `visitDate` |
| 2 | `ui/VisitEditorSteps.kt:317` | `visitDate` (`Visit date (YYYY-MM-DD)`) | `ClinicalDateField(showTodayButton = false, externalLabel = true, useRecordStyle = true)` | Partial (trailing `visitDateCalendarButton` + `visitDateToday`/`Yesterday` chips above) | **Yes** | Guided Step 0 view |
| 3 | `ui/DatingEditor.kt:111` | `lmpDate` (`Last menstrual period (LMP)`) | `ClinicalDateField(showTodayButton = true, externalLabel = true, useRecordStyle = true)` | Partial (`lmpDateCalendarButton` + `lmpDateTodayButton`) | **Yes** | Optional/clearable if user switches or clears |
| 4 | `ui/DatingEditor.kt:125` | `ultrasoundEdd` (`Ultrasound EDD`) | `ClinicalDateField(showTodayButton = false, externalLabel = true, useRecordStyle = true)` | Partial (`ultrasoundEddCalendarButton`) | **Yes** | Optional/clearable |
| 5 | `ui/DatingEditor.kt:139` | `ivfTransferDate` (`Embryo transfer date (IVF)`) | `ClinicalDateField(showTodayButton = false, externalLabel = true, useRecordStyle = true)` | Partial (`ivfTransferDateCalendarButton`) | **Yes** | Optional/clearable |
| 6 | `ui/DatingEditor.kt:183` | `agreedEdd` (`Agreed clinical EDD`) | `ClinicalDateField(showTodayButton = false, externalLabel = true, useRecordStyle = true)` | Partial (`agreedEddCalendarButton`) | **Yes** | Optional/clearable; auto-calculated or manual override via picker |
| 7 | `ui/PreviousPregnancyEditor.kt:61` | `pregnancyDateText` (`Entered date text`) | `ClinicalDateField(showTodayButton = false, externalLabel = true, useRecordStyle = true)` | Partial (`pregnancyDateTextCalendarButton`) | **Yes** | Historical pregnancies can store year-only text (`"2022"`) or blank; optional/clearable (Decision 3) |
| 8 | `ui/UltrasoundEditor.kt:264` | `scanDate` (`Scan date (YYYY-MM-DD)`) | `ClinicalDateField(showTodayButton = true, externalLabel = true, useRecordStyle = true)` | Partial (`scanDateCalendarButton` + `scanDateTodayButton`) | **Yes** | Required scan date |
| 9 | `ui/LabEditorComponents.kt:217` (from `LabEditor.kt:196`) | `labDate` (`Test date`) | `ClinicalDateField(showTodayButton = true, externalLabel = true, useRecordStyle = true)` | Partial (`labDateCalendarButton` + `labDateTodayButton`) | **Yes** | Hosted inside `LabMetadataDialog` |
| 10 | `ui/LabEditorComponents.kt:217` (from `LabBatchScreen.kt:113`) | `labBatchDate` (`Test date`) | `ClinicalDateField(showTodayButton = true, externalLabel = true, useRecordStyle = true)` | Partial (`labBatchDateCalendarButton` + `labBatchDateTodayButton`) | **Yes** | Hosted inside `LabMetadataDialog` |
| 11 | `ui/patienthub/components/MedicationForm.kt:265` | `medDate` (`Start date` / `Stop date`) | `ClinicalDateField(showTodayButton = true, externalLabel = false, useRecordStyle = false)` | Partial (`medDateCalendarButton` + `medDateTodayButton`) | **Yes** | **Test impact**: `MedicationFormTest` lines 235, 243, 426 call `onNodeWithTag("medDate").performTextInput(...)`; `MedsTabSheetTest` line 1197 traverses IME focus through `medDate` |
| 12 | `ui/patienthub/components/DeliveryPlanSheet.kt:248` | `deliveryOutcomeDateField` (`Delivery date (YYYY-MM-DD)`) | Raw `OutlinedTextField` (NOT `ClinicalDateField`) | **No** (no calendar icon, no picker, no Today chip) | **Yes** (typing only) | **Only date field without any picker**. Hint explicitly allows empty (`"Leave empty if the date is not known."`). `DeliveryPlanSheetTest` lines 300, 338, 353 type `"2026-11-05"` / `"not-a-date"` |
| -- | `ui/PatientEditor.kt:132` (non-UI note) | `PatientDraftValues.dateOfBirth` | None (comment: `"Date of birth stays in the draft untouched."`) | N/A | No UI input | UI collects `patientAgeText` (years); `dateOfBirth` has no text field in `app/src/main/java` |
| -- | `ui/AncDatePicker.kt:89` (internal picker mode) | `DatePicker(state = datePickerState)` | M3 `DatePicker` with `showModeToggle = true` (default) | Calendar default | **Yes** (header pencil toggle switches to `MM/DD/YYYY` hand-entry mode) | Setting `showModeToggle = false` disables manual typing inside the picker itself |

## Decisions for the owner (this slice)

1. **Enforce picker-only `ClinicalDateField` (`readOnly = true` + full-box tap) and calendar-only `AncDatePickerDialog` (`showModeToggle = false`)**:
   Today all 11 `ClinicalDateField` callers still let the user tap the text box to open the soft keyboard and type by hand, and `AncDatePickerDialog`'s M3 `DatePicker` exposes a pencil icon to switch into `MM/DD/YYYY` manual typing. Making `ClinicalDateField` `readOnly = true` (preserving `SemanticsProperties.EditableText` for `AncWorkflowTest` line 270) with `PressInteraction.Release` + semantics `onClick` opening `AncDatePickerDialog` and setting `showModeToggle = false` enforces OWNER RULE (2026-11-16) across all 11 callers, while `MedicationFormTest` lines 235, 243, 426 (`performTextInput` on `medDate`) will need a small test-lane update to pick via `medDateTodayButton` or `medDateCalendarButton` -> `datePickerConfirm`.
   **Recommended**: approve `readOnly = true` + full-box tap on `ClinicalDateField` and `showModeToggle = false` on `AncDatePickerDialog`, and include the `MedicationFormTest` update in the test lane.

2. **Convert `DeliveryPlanSheet` `deliveryOutcomeDateField` from raw `OutlinedTextField` to `ClinicalDateField` + Clear action**:
   `ui/patienthub/components/DeliveryPlanSheet.kt` line 248 (`deliveryOutcomeDateField`) is the only date input in the app with no picker at all (manual `YYYY-MM-DD` typing only), and its hint (`delivery_outcome_date_hint`) explicitly states `"Leave empty if the date is not known."`. Without hand-typing, clinicians also need a `Clear` action when clearing an optional date.
   **Recommended**: approve adding `allowClear: Boolean = false` (`${tag}ClearButton` `RecordFilterChip` beside `Today` when `value.isNotBlank()`) to `ClinicalDateField`, migrating `deliveryOutcomeDateField` in `DeliveryPlanSheet.kt` to `ClinicalDateField(..., tag = "deliveryOutcomeDateField", allowClear = true, showTodayButton = true, externalLabel = true, useRecordStyle = true)` (either inside Slice 19 or an immediate Slice 11 follow-up), and updating `DeliveryPlanSheetTest` lines 300, 338, 353 in the test lane to use the picker/clear controls.

3. **Historical `PreviousPregnancyEditor` date field (`pregnancyDateText`)**:
   `PreviousPregnancyEditor.kt` line 61 (`pregnancyDateText`) already uses `ClinicalDateField`, though `PastDeliveryRowModel` and existing fixtures accept year-only historical entries (e.g. `"2022"`). With `ClinicalDateField` set to `readOnly = true`, clinicians pick a calendar date (including jumping years in `AncDatePickerDialog`'s year selector) or leave/clear it blank, rather than typing `"2022"` freehand.
   **Recommended**: keep `pregnancyDateText` picker-only (`ClinicalDateField(showTodayButton = false, allowClear = true)`) for strict uniformity under OWNER RULE (2026-11-16), unless historical year-only free-text entry is kept as an explicit owner exception.

4. **Error-colored destructive actions (`DeleteConfirmDialog`, `AncApp` `showDiscardDialog`, `draftRecoveryPrompt`) and `StatusModerate` stale warning**:
   Today `DeleteConfirmDialog` (`"Delete"`) and `AncApp`'s `showDiscardDialog` (`"Discard"`) render destructive actions as solid primary navy pills, and `draftRecoveryPrompt` renders `"Discard and start fresh"` as a plain neutral `TextButton` plus an unstyled `14.sp` stale warning indistinguishable from body text.
   **Recommended**: reuse `DiscardChangesDialog` (`EditorSheetParts.kt`) directly in `AncApp.kt` for `showDiscardDialog` (preserving `discardDialog` / `keepEditingDialog` tags), style `DeleteConfirmDialog`'s `"Delete"` and `draftRecoveryPrompt`'s `"Discard and start fresh"` as `48.dp` `RecordShapes.Small` `TextButton`s in `MaterialTheme.colorScheme.error`, and render `draft_recovery_stale` inside a `StatusModerateBg`/`Border`/`Text` callout box.

5. **`InAppCameraDialog` contrast fix on `Use photo` and `Close camera` placement**:
   Today `InAppCameraDialog` renders `Button` (`Use photo`) in `MaterialTheme.colorScheme.primary` (`#0D1B3D` in light mode) directly on `Color.Black` (`#000000`), yielding `1.24:1` boundary contrast (invisible button edge in light mode), uses non-token `20.dp` frame corners, and places `Close camera` directly beneath `Take photo` / `Use photo`.
   **Recommended**: use `RecordShapes.Large` (`16.dp`) preview frames, move `Close camera` (`camera_close`, `48.dp` `RecordShapes.Small` `OutlinedButton`) to a top-end row in live/review states (and full-width inside the dark permission card when denied), style `camera_use` with `Color.White` fill + `ClinicalNavy` (`#0D1B3D`) text (`21:1` / `16.92:1`), and frame capture/bind errors in `StatusHighDark` banners.

## Lessons (apply all)

- **Minimum `12.sp` everywhere** (`PLAN.md` 0, 2): lift `DeleteMessageBar` and `UndoOfferBar` from `bodyMedium` (`13.sp`) to `RecordType.Secondary` (`14.sp`) and buttons to `RecordType.Action` (`14.sp`); ensure `AncDatePickerDialog` title uses `RecordType.FieldLabel` (`12.sp`) and headline uses `RecordType.HeaderName` (`22.sp`) so it never clips on `360.dp` at `1.3f`/`2.0f` scale.
- **Two corner radii only (`RecordShapes.Small` `10.dp`, `RecordShapes.Large` `16.dp`) and `0.dp` elevation**: remove `28.dp` M3 `AlertDialog` / `DatePickerDialog` corners, `12.dp` bar corners and `2.dp` `shadowElevation` in `DeleteUndoUi.kt`, `20.dp` preview corners in `InAppCameraDialog.kt`, `4.dp` / `8.dp` shapes on legacy `ClinicalDateField`, and `999.dp` pill buttons across all 4 files.
- **3:1 non-text contrast on interactive controls and bars**: `UndoOfferBar` over `#FFFFFF` sheets or `#FBF8F5` background uses `FieldFamilies.patient().container` (`#E9EEF8` / `#22304A`) + `1.dp` `MaterialTheme.colorScheme.outline` (`4.55:1` / `5.42:1` against surface, `3.91:1` / `4.40:1` against Patient fill); `DeleteMessageBar` uses `errorContainer` + `1.dp` `error` border and `onErrorContainer` `Close` text (`13.26:1` light / `10.33:1` dark); `InAppCameraDialog` `camera_use` uses `Color.White` / `ClinicalNavy` on `Color.Black` (`21:1`) and `camera_retake` / `camera_close` use `Color(0xFF8E95A5)` border (`6.99:1` on black).
- **Minimum `48.dp` width and height (`minimumTouchTarget()`) on every interactive target**: `deleteConfirm`, `deleteCancel`, `deleteMessageDismiss`, `undoButton`, `newPatientInfoClose`, `discardDialog`, `keepEditingDialog`, `draftContinue`, `draftDiscard`, `datePickerConfirm`, `datePickerDismiss`, `${tag}CalendarButton`, `${tag}TodayButton`, `${tag}ClearButton`, `camera_capture`, `camera_retake`, `camera_use`, and `camera_close`.
- **Keep `SemanticsProperties.EditableText` and focus traversal on `ClinicalDateField`**: `AncWorkflowTest` line 270 asserts `SemanticsProperties.EditableText` on `"visitDate"`, and `MedicationFormTest` line 185 / `MedsTabSheetTest` line 1197 assert `FocusRequester` + `performImeAction()` across `"medDate"`. Keep `OutlinedTextField(readOnly = true, ...)` with `.testTag(tag)` directly on the `OutlinedTextField` (NOT on the outer `Column`), rather than replacing `OutlinedTextField` with a plain `Box` / `Text`.
- **Prevent text clipping at `360.dp` and `1.3f`/`2.0f` font scale**: in `UndoOfferBar`, allow wrapping (`maxLines = 2`, dropping fixed `130.dp` tail squeeze above `1.3f` scale); in `InAppCameraDialog` photo review (`shot != null`), stack `camera_retake` and `camera_use` in a `Column` when `LocalDensity.current.fontScale > 1.3f`; in `InAppCameraDialog` permission-denied state, wrap the permission card inside a `verticalScroll` column; in `AncDatePickerDialog`, wrap/size `headline` at `22.sp` (`RecordType.HeaderName`) so long dates do not clip.
- **No `LocalConfiguration.current` hardcoding of English text**: move `"Camera access needed"` and `"Clear"` into `strings.xml`; preserve every existing `strings.xml` key asserted by tests (`delete_confirm_*`, `undo_message_*`, `draft_recovery_*`, `new_patient_*`, `discard*`, `camera_*`).

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Lines | Change |
|---|---:|---|
| `res/values/strings.xml` | 1,027 | Add `camera_permission_title`, `clear_date`, `date_picker_open_cd`; keep all existing Slice 19 keys verbatim (`AncWorkflowTest`, `DeleteUndoUiTest`, `DraftRecoveryDialogTest`) |
| `ui/DeleteUndoUi.kt` | 258 | Restyle `DeleteConfirmDialog` (`16.dp` `surface` `AlertDialog`, `error`-colored `Delete`), `DeleteMessageBar` (`10.dp`, `1.dp` `error` border, `14.sp`, `onErrorContainer` `Close`), and `UndoOfferBar` (`10.dp`, `0.dp` elevation, `FieldFamilies.patient().container` + `1.dp` `outline` border, `14.sp` responsive wrap, `48.dp` `Undo`); keep all state, tags, and `UndoCoordinator` interactions verbatim |
| `ui/AncApp.kt` | 389 | **Dialogs only (lines 289-388)**: restyle `showNewPatientInfo` `AlertDialog` (`16.dp` `surface`, `48.dp` `10.dp` `Close`), replace `showDiscardDialog` `AlertDialog` with shared `DiscardChangesDialog` (`discardDialog` / `keepEditingDialog`), and restyle `draftRecoveryPrompt` `AlertDialog` (`16.dp` `surface`, `StatusModerate` stale callout, `error`-colored `Discard and start fresh`, `10.dp` `Continue`); touch zero lines outside lines 289-388 |
| `ui/AncDatePicker.kt` | 214 | Restyle `AncDatePickerDialog` (`16.dp` `surface`, `DatePickerDefaults.colors(...)`, `showModeToggle = false` per Decision 1, `22.sp` headline, `48.dp` `10.dp` `Cancel`/`Save`), and unify `ClinicalDateField` on Record tokens (`readOnly = true` + full-field tap to open picker per Decision 1, optional `allowClear` / `${tag}ClearButton` per Decision 2, `48.dp` targets, `RecordFilterChip` chips) |
| `ui/InAppCameraDialog.kt` | 307 | Restyle `InAppCameraDialog` (`16.dp` preview/review frames, top-end `48.dp` `10.dp` `Close camera`, `72.dp` ringed shutter, high-contrast `Color.White` / `ClinicalNavy` `Use photo` stacking at `> 1.3f`, `StatusHighDark` error banners, `16.dp` `#1A202E` permission-denied card); keep all CameraX, permission, and temp-file disposal logic verbatim |
| `ui/patienthub/components/DeliveryPlanSheet.kt` *(Decision 2 only)* | 368 | Replace raw `OutlinedTextField` at lines 248-269 (`deliveryOutcomeDateField`) with `ClinicalDateField(..., tag = "deliveryOutcomeDateField", allowClear = true, showTodayButton = true, externalLabel = true, useRecordStyle = true)` keeping exact `revealOnFocus()`, `revealWhen(...)`, `supportingText` error mapping, and `AncEvent.DeliveryOutcomeFieldChanged(DeliveryOutcomeField.DATE, it)` |

No edits needed to `ui/Theme.kt`, `ui/components/*`, `ui/Components.kt`, `ui/patienthub/components/EditorSheetParts.kt`, `state/UndoCoordinator.kt`, `state/**`, `storage/**`, or `model/**`.

## 1. Strings (`app/src/main/res/values/strings.xml`)

### Reuse existing keys verbatim (do NOT alter values):
- `delete`, `cancel`, `close`, `save`, `undo`, `undo_in_progress`, `patient_clause`, `delete_record_description`, `delete_confirm_title`, `delete_confirm_body`, `deletable_type_*`, `delete_failed_message`, `undo_failed_deleted_message`, `undo_failed_saved_message`, `undo_blocked_message`, `cleanup_failed_message`, `undo_message_*`.
- `new_patient_title`, `new_patient_body`, `discard_changes`, `discard_body`, `discard`, `keep_editing`.
- `draft_recovery_title`, `draft_recovery_body`, `draft_recovery_stale`, `draft_recovery_continue`, `draft_recovery_discard`, `draft_screen_*`, `draft_age_less_than_minute`, `draft_age_minutes`, `draft_age_hours`, `draft_age_days`.
- `pick_date`, `today`, `correct_highlighted_field`.
- `camera_capture`, `camera_retake`, `camera_use`, `camera_close`, `camera_permission_needed`, `camera_bind_failed`, `camera_capture_failed`.

### Add to `strings.xml` (sentence case, English default, no em dashes):
```xml
<string name="camera_permission_title">Camera access needed</string>
<string name="clear_date">Clear</string>
<string name="date_picker_open_cd">Select %1$s</string>
```

## 2. `DeleteUndoUi.kt` (`ui/DeleteUndoUi.kt`, signatures unchanged)

1. **`DeleteConfirmDialog(state: AncState, onEvent: (AncEvent) -> Unit)`**:
   - Keep `val pending`, `record` lookup, and `LaunchedEffect(pending) { onEvent(AncEvent.CancelDelete) }` untouched.
   - `AlertDialog`:
     - `modifier = Modifier.testTag("deleteConfirmDialog")`
     - `onDismissRequest = { onEvent(AncEvent.CancelDelete) }`
     - `shape = RecordShapes.Large`
     - `containerColor = MaterialTheme.colorScheme.surface`
     - `title = { Text(stringResource(R.string.delete_confirm_title, typeLabel), style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.onSurface) }`
     - `text = { Text(stringResource(R.string.delete_confirm_body, label), style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant) }`
     - `confirmButton = { TextButton(onClick = { onEvent(AncEvent.ConfirmDelete) }, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("deleteConfirm")) { Text(stringResource(R.string.delete), style = RecordType.Action, color = MaterialTheme.colorScheme.error) } }`
     - `dismissButton = { TextButton(onClick = { onEvent(AncEvent.CancelDelete) }, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("deleteCancel")) { Text(stringResource(R.string.cancel), style = RecordType.Action, color = MaterialTheme.colorScheme.primary) } }`

2. **`DeleteMessageBar(message: DeleteMessage, onEvent: (AncEvent) -> Unit)`**:
   - Keep `typeLabel`, `reason`, `patient`, and `text` `when (message.kind)` mapping verbatim.
   - `Surface`:
     - `color = MaterialTheme.colorScheme.errorContainer`
     - `contentColor = MaterialTheme.colorScheme.onErrorContainer`
     - `shape = RecordShapes.Small`
     - `border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)`
     - `shadowElevation = 0.dp`
     - `modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).testTag("deleteMessage").semantics { liveRegion = LiveRegionMode.Assertive }`
   - Inner `Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp))`:
     - `Text(text = text, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))`
     - `TextButton(onClick = { onEvent(AncEvent.DismissDeleteMessage) }, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("deleteMessageDismiss")) { Text(stringResource(R.string.close), style = RecordType.Action, color = MaterialTheme.colorScheme.onErrorContainer) }`

3. **`UndoOfferBar(offer: UndoOffer, deleteRunning: Boolean, onEvent: (AncEvent) -> Unit)`**:
   - Keep `running`, `patientName`, `head`, `spoken`, `tail`, `progressDescription` verbatim.
   - `val patientBg = FieldFamilies.patient().container`
   - `val wrapLines = LocalDensity.current.fontScale > 1.3f`
   - `Surface`:
     - `color = patientBg`
     - `contentColor = MaterialTheme.colorScheme.onSurface`
     - `shape = RecordShapes.Small`
     - `border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)`
     - `shadowElevation = 0.dp`
     - `modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).testTag("undoBar").semantics { liveRegion = LiveRegionMode.Polite }`
   - Inner `Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp))`:
     - Text container `Modifier.weight(1f).semantics { contentDescription = spoken }`:
       - If `!wrapLines`: keep the `Row` with `head` (`Modifier.weight(1f, fill = false)`, `maxLines = 1`, `TextOverflow.Ellipsis`, `style = RecordType.Secondary`) + optional `tail` (`Modifier.widthIn(max = 130.dp)`, `maxLines = 1`, `TextOverflow.Ellipsis`, `style = RecordType.Secondary`) so patient name stays visible on `360.dp`.
       - If `wrapLines` (`fontScale > 1.3f`): render `Text(text = spoken, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurface, maxLines = 3)` so `"Deleted ..."` never clips to `"Del..."` at `2.0f`.
     - `if (running)`: `CircularProgressIndicator(modifier = Modifier.size(20.dp).semantics { contentDescription = progressDescription }, strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)`.
     - `TextButton(onClick = { onEvent(AncEvent.UndoRequested(offer.token)) }, enabled = !running && !deleteRunning, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("undoButton")) { Text(stringResource(R.string.undo), style = RecordType.Action) }`.

4. **`DeleteIconButton`**: keep unchanged (already uses `minimumTouchTarget()`, `delete-${type}-${recordId}`, `MaterialTheme.colorScheme.error`).

## 3. `AncApp.kt` dialogs only (`ui/AncApp.kt` lines 289-388)

Touch ONLY lines 289-388 (`showNewPatientInfo`, `showDiscardDialog`, `draftRecoveryPrompt`); leave lines 1-288 untouched.

1. **`state.showNewPatientInfo` `AlertDialog` (lines 289-302)**:
   - `AlertDialog`:
     - `modifier = Modifier.testTag("newPatientInfoDialog")`
     - `onDismissRequest = { onEvent(AncEvent.HideNewPatientInfo) }`
     - `shape = RecordShapes.Large`
     - `containerColor = MaterialTheme.colorScheme.surface`
     - `title = { Text(stringResource(R.string.new_patient_title), style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.onSurface) }`
     - `text = { Text(stringResource(R.string.new_patient_body), style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant) }`
     - `confirmButton = { Button(onClick = { onEvent(AncEvent.HideNewPatientInfo) }, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("newPatientInfoClose")) { Text(stringResource(R.string.close), style = RecordType.Action) } }`

2. **`state.showDiscardDialog` (lines 308-332)**:
   - Replace the duplicate inline `AlertDialog` with the canonical shared `DiscardChangesDialog` (`com.sherifradwan.ancnative.ui.patienthub.components.DiscardChangesDialog`):
     ```kotlin
     if (unlocked && !isAppLocked && securitySettings?.needsPinSetup != true && state.showDiscardDialog) {
         DiscardChangesDialog(
             onDiscard = { onEvent(AncEvent.ConfirmDiscard) },
             onKeep = { onEvent(AncEvent.KeepEditing) },
             dialogTag = "shellDiscardDialog",
             confirmTag = "discardDialog",
             keepTag = "keepEditingDialog",
         )
     }
     ```
   - Keeps exact tags `discardDialog` and `keepEditingDialog`, exact strings, `16.dp` `surface` container, and `error`-colored `Discard`.

3. **`state.draftRecoveryPrompt` `AlertDialog` (lines 334-388)**:
   - Keep `screenName` and `ageText` computations verbatim.
   - `val dark = isDarkTheme()`
   - `AlertDialog`:
     - `modifier = Modifier.testTag("draftRecoveryDialog")`
     - `onDismissRequest = {}`
     - `properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`
     - `shape = RecordShapes.Large`
     - `containerColor = MaterialTheme.colorScheme.surface`
     - `title = { Text(stringResource(R.string.draft_recovery_title), style = RecordType.SectionTitle, color = MaterialTheme.colorScheme.onSurface) }`
     - `text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(stringResource(R.string.draft_recovery_body, screenName, ageText), style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant); if (recoveryPrompt.isStale) { Surface(shape = RecordShapes.Small, color = if (dark) StatusModerateDarkBg else StatusModerateBg, border = BorderStroke(1.dp, if (dark) StatusModerateDarkBorder else StatusModerateBorder), modifier = Modifier.fillMaxWidth().testTag("draftRecoveryStaleBanner")) { Text(text = stringResource(R.string.draft_recovery_stale), style = RecordType.Secondary, color = if (dark) StatusModerateDarkText else StatusModerateText, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) } } } }`
     - `confirmButton = { Button(onClick = { onEvent(AncEvent.AcceptDraftRecovery) }, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("draftContinue")) { Text(stringResource(R.string.draft_recovery_continue), style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center) } }`
     - `dismissButton = { TextButton(onClick = { onEvent(AncEvent.DiscardDraftRecovery) }, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("draftDiscard")) { Text(stringResource(R.string.draft_recovery_discard), style = RecordType.Action, color = MaterialTheme.colorScheme.error, maxLines = 2, textAlign = TextAlign.Center) } }`

## 4. `AncDatePicker.kt` (`ui/AncDatePicker.kt`)

1. **`AncDatePickerDialog`** (backward-compatible signature with optional trailing `title: String = stringResource(R.string.pick_date)`):
   - Keep `initialMillis` and `rememberDatePickerState` verbatim.
   - `val dateFormatter = remember { DatePickerDefaults.dateFormatter() }`
   - `val pickerColors = DatePickerDefaults.colors(...)`:
     - `containerColor = MaterialTheme.colorScheme.surface`
     - `titleContentColor = FieldFamilies.patient().label`
     - `headlineContentColor = MaterialTheme.colorScheme.primary`
     - `weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant`
     - `subheadContentColor = MaterialTheme.colorScheme.primary`
     - `navigationContentColor = MaterialTheme.colorScheme.primary`
     - `yearContentColor = MaterialTheme.colorScheme.onSurface`
     - `currentYearContentColor = MaterialTheme.colorScheme.primary`
     - `selectedYearContentColor = MaterialTheme.colorScheme.onPrimary`
     - `selectedYearContainerColor = MaterialTheme.colorScheme.primary`
     - `dayContentColor = MaterialTheme.colorScheme.onSurface`
     - `selectedDayContentColor = MaterialTheme.colorScheme.onPrimary`
     - `selectedDayContainerColor = MaterialTheme.colorScheme.primary`
     - `todayContentColor = MaterialTheme.colorScheme.primary`
     - `todayDateBorderColor = MaterialTheme.colorScheme.primary`
     - `dividerColor = MaterialTheme.colorScheme.outlineVariant`
   - `DatePickerDialog`:
     - `modifier = Modifier.testTag("ancDatePickerDialog")`
     - `onDismissRequest = onDismiss`
     - `shape = RecordShapes.Large`
     - `colors = pickerColors`
     - `confirmButton = { Button(onClick = <unchanged UTC millis -> LocalDate.toString() + onDismiss()>, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("datePickerConfirm")) { Text(stringResource(R.string.save), style = RecordType.Action) } }`
     - `dismissButton = { TextButton(onClick = onDismiss, shape = RecordShapes.Small, modifier = Modifier.minimumTouchTarget().testTag("datePickerDismiss")) { Text(stringResource(R.string.cancel), style = RecordType.Action, color = MaterialTheme.colorScheme.primary) } }`
   - Inside `DatePickerDialog` body:
     - `DatePicker(state = datePickerState, showModeToggle = false` *(Decision 1)*`, colors = pickerColors, title = { Text(text = title, style = RecordType.FieldLabel, color = FieldFamilies.patient().label, modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) }, headline = { DatePickerDefaults.DatePickerHeadline(selectedDateMillis = datePickerState.selectedDateMillis, displayMode = datePickerState.displayMode, dateFormatter = dateFormatter, modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)) })` (or wrap headline typography via `ProvideTextStyle(RecordType.HeaderName)` so selected dates never wrap or clip on `360.dp` at `1.3f`/`2.0f` scale).
     - Note: Material 3 `DatePicker` handles month `<` `>` navigation and year dropdown internally with `48.dp` minimum touch targets and `40.dp`/`48.dp` day cells.

2. **`ClinicalDateField`** (keep all existing parameters, add trailing `allowClear: Boolean = false` if Decision 2 approved):
   - Unify visual styling onto Record tokens across ALL callers (including `MedicationForm` which currently passes `useRecordStyle = false`):
     - `shape = if (useRecordStyle) RecordShapes.Small else RecordShapes.Small` (always `10.dp` `RecordShapes.Small` once Slice 19 lands, or keep `shape` param defaulting to `RecordShapes.Small`).
     - `textStyle = RecordType.FieldValue` (`16.sp`), `colors = editorFieldColors()`.
     - `trailingIcon` `IconButton`: always `.minimumTouchTarget().testTag("${tag}CalendarButton")`.
     - Supporting error text: always `RecordType.Tag` (`12.sp`, never M3 default `12.sp`/`11.sp`).
   - **Picker-only enforcement (Decision 1)**:
     - Set `readOnly = true` on `OutlinedTextField` (keeps `SemanticsProperties.EditableText` for `AncWorkflowTest` and focus traversal for `MedicationFormTest`).
     - Add `placeholder = { Text(stringResource(R.string.pick_date), style = RecordType.FieldValue, color = MaterialTheme.colorScheme.onSurfaceVariant) }`.
     - Attach `val interactionSource = remember { MutableInteractionSource() }` and `LaunchedEffect(interactionSource, enabled) { if (enabled) interactionSource.interactions.collect { if (it is PressInteraction.Release) showDatePicker = true } }`.
     - Add `Modifier.semantics { if (externalLabel) contentDescription = label; onClick(label = pickDateCd) { if (enabled) { showDatePicker = true; true } else false } }` on `OutlinedTextField` before `.testTag(tag)`.
   - **Action chips row (below field)**:
     - Render `FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(4.dp))` when `enabled && (showTodayButton || (allowClear && value.isNotBlank()))`:
       - If `allowClear && value.isNotBlank()`: `RecordFilterChip(selected = false, label = stringResource(R.string.clear_date), onClick = { onValueChange("") }, tag = "${tag}ClearButton", wrap = true, minTouchTarget = true)`.
       - If `showTodayButton`: always use `RecordFilterChip(selected = false, label = todayLabel, onClick = { onValueChange(LocalDate.now().toString()) }, tag = "${tag}TodayButton", wrap = true, minTouchTarget = true)` (dropping legacy M3 rose `FilterChip`).
   - Pass `title = label` to `AncDatePickerDialog(...)`.

## 5. `InAppCameraDialog.kt` (`ui/InAppCameraDialog.kt`, signature unchanged)

Keep `Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false))` on `Color.Black`, plus `.windowInsetsPadding(WindowInsets.safeDrawing)` (or `systemBarsPadding`) on inner columns so controls never collide with status/navigation bars.

1. **Private `CloseButton(onDismiss: () -> Unit, fullWidth: Boolean = false)`**:
   - Uses `OutlinedButton(onClick = onDismiss, shape = RecordShapes.Small, border = BorderStroke(1.dp, Color(0xFF8E95A5)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), modifier = (if (fullWidth) Modifier.fillMaxWidth() else Modifier).minimumTouchTarget().testTag("camera_close")) { Text(stringResource(R.string.camera_close), style = RecordType.Action, color = Color.White, maxLines = 2, textAlign = TextAlign.Center) }`.
2. **Permission denied (`!granted`)**:
   - `Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center)`:
     - When `permissionAsked`: render `Surface(shape = RecordShapes.Large, color = Color(0xFF1A202E), border = BorderStroke(1.dp, Color(0xFF3A4459)), modifier = Modifier.fillMaxWidth().testTag("cameraPermissionCard"))` containing `Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp))`:
       - `Text(stringResource(R.string.camera_permission_title), style = RecordType.SectionTitle, color = Color(0xFFE2E2EA), modifier = Modifier.semantics { heading() })`
       - `Text(stringResource(R.string.camera_permission_needed), style = RecordType.FieldValue, color = Color(0xFFCDC3C6))`
       - `Button(onClick = onDismiss, shape = RecordShapes.Small, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClinicalNavy), modifier = Modifier.fillMaxWidth().minimumTouchTarget().testTag("camera_close")) { Text(stringResource(R.string.camera_close), style = RecordType.Action) }`
     - When `!permissionAsked` (transient initial launch before launcher returns): render `CloseButton(onDismiss)` centered so `camera_close` stays reachable.
3. **Photo review (`shot != null`)**:
   - `Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp))`:
     - Top bar `Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End)`: `CloseButton(onDismiss)` (single `camera_close` node in this branch).
     - Preview frame `Box(Modifier.weight(1f).fillMaxWidth().clip(RecordShapes.Large).border(1.dp, Color(0xFF3A4459), RecordShapes.Large), contentAlignment = Alignment.Center)`: keep `still` `Image` / `CircularProgressIndicator(color = Color.White)`.
     - Action buttons (`Retake` + `Use photo`):
       - `val stackActions = LocalDensity.current.fontScale > 1.3f`
       - Use `Column(spacedBy(8.dp))` when `stackActions`, else `Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp))`:
         - `OutlinedButton(onClick = { discardCapture() }, shape = RecordShapes.Small, border = BorderStroke(1.dp, Color(0xFF8E95A5)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), modifier = (if (stackActions) Modifier.fillMaxWidth() else Modifier.weight(1f)).minimumTouchTarget().testTag("camera_retake")) { Text(stringResource(R.string.camera_retake), style = RecordType.Action, color = Color.White, maxLines = 2, textAlign = TextAlign.Center) }`
         - `Button(onClick = { committed = true; onCaptured(CameraCaptureHelper.uriFor(context, shot)) }, shape = RecordShapes.Small, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ClinicalNavy), modifier = (if (stackActions) Modifier.fillMaxWidth() else Modifier.weight(1f)).minimumTouchTarget().testTag("camera_use")) { Text(stringResource(R.string.camera_use), style = RecordType.Action, maxLines = 2, textAlign = TextAlign.Center) }`
4. **Live preview (`else`)**:
   - `Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp))`:
     - Top bar `Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End)`: `CloseButton(onDismiss)`.
     - `AndroidView(..., modifier = Modifier.weight(1f).fillMaxWidth().clip(RecordShapes.Large).border(1.dp, Color(0xFF3A4459), RecordShapes.Large).testTag("camera_preview"))`.
     - If `failed` or `bindFailed`: render `Surface(shape = RecordShapes.Small, color = StatusHighDarkBg, border = BorderStroke(1.dp, StatusHighDarkBorder), modifier = Modifier.fillMaxWidth().testTag("cameraErrorBanner"))` with `Text(stringResource(if (bindFailed) R.string.camera_bind_failed else R.string.camera_capture_failed), style = RecordType.Secondary, color = StatusHighDarkText, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))`.
     - If `!bindFailed`: shutter `Box(modifier = Modifier.size(72.dp).clip(CircleShape).border(2.dp, if (capturing) Color(0xFF8E95A5) else Color.White, CircleShape).padding(4.dp).clip(CircleShape).background(if (capturing) Color(0xFF8E95A5) else Color.White).clickable(enabled = !capturing, onClickLabel = captureLabel) { takePhoto() }.semantics { contentDescription = captureLabel }.testTag("camera_capture"))` (outer `Box` stays `72.dp` with `testTag("camera_capture")`).

## 6. All existing `testTag`s kept (exhaustive list, 22 tag patterns total)

- **`DeleteUndoUi.kt` (8)**: `delete-${type.name.lowercase()}-$recordId` (or custom `tag`), `deleteConfirmDialog`, `deleteConfirm`, `deleteCancel`, `deleteMessage`, `deleteMessageDismiss`, `undoBar`, `undoButton`.
- **`AncApp.kt` dialogs (4)**: `discardDialog`, `keepEditingDialog`, `draftContinue`, `draftDiscard`.
- **`AncDatePicker.kt` (5)**: `datePickerConfirm`, `datePickerDismiss`, `${tag}` (on `OutlinedTextField`), `${tag}CalendarButton`, `${tag}TodayButton`.
- **`InAppCameraDialog.kt` (5)**: `camera_preview`, `camera_capture`, `camera_retake`, `camera_use`, `camera_close`.
- **Additive new tags (optional, never collide)**: `newPatientInfoDialog`, `newPatientInfoClose`, `shellDiscardDialog`, `draftRecoveryDialog`, `draftRecoveryStaleBanner`, `ancDatePickerDialog`, `${tag}ClearButton`, `cameraPermissionCard`, `cameraErrorBanner`.

## Acceptance checklist

1. All 22 existing `testTag` expressions remain on their exact semantic nodes (`OutlinedTextField` directly for `${tag}` date fields, `AlertDialog` / `DatePickerDialog` / `Surface` / button nodes as today).
2. `git diff -w` shows zero changes to `AncApp.kt` lines 1-288, `UndoCoordinator.kt`, `AncReducer.kt`, `AncState.kt`, `DraftRecovery.kt`, `GestationalAgeEngine.kt`, `CameraCaptureHelper.kt`, `BoundedBitmap.kt`, and zero changes to delete/undo timing, draft recovery rules, date ISO formatting, or camera file cleanup.
3. Zero text nodes under `12.sp` across all Slice 19 dialogs and components; zero hardcoded user-facing English strings; zero `20.dp` / `12.dp` / `28.dp` / `999.dp` pill shapes (only `10.dp` `RecordShapes.Small`, `16.dp` `RecordShapes.Large`, and circular shutter/calendar selection rings); zero `shadowElevation`.
4. Every interactive element (`deleteConfirm`, `deleteCancel`, `deleteMessageDismiss`, `undoButton`, `newPatientInfoClose`, `discardDialog`, `keepEditingDialog`, `draftContinue`, `draftDiscard`, `datePickerConfirm`, `datePickerDismiss`, `${tag}CalendarButton`, `${tag}TodayButton`, `${tag}ClearButton`, `camera_capture`, `camera_retake`, `camera_use`, `camera_close`) measures `>= 48.dp` (`>= 47.9.dp` in instrumented checks) at `360.dp` and `1.0f` / `1.3f` / `2.0f` font scale with zero clipped text.
5. `DeleteConfirmDialog`, `AncApp` `showDiscardDialog` (reusing `DiscardChangesDialog`), and `draftRecoveryPrompt` render destructive actions in `MaterialTheme.colorScheme.error` and safe defaults in `primary`.
6. Under Decisions 1 and 2, every date field across `app/src/main/java` is picker-driven (`readOnly = true` + `showModeToggle = false`), including `DeliveryPlanSheet` `deliveryOutcomeDateField`, with optional date clearing via `${tag}ClearButton`.
7. `InAppCameraDialog` passes `>= 3.0:1` non-text and `>= 4.5:1` text contrast in both light and dark themes (`camera_use` uses `Color.White` / `ClinicalNavy` instead of `#0D1B3D` on `#000000`).
8. Dark theme (`AncTheme(darkTheme = true)`) and `LayoutDirection.Rtl` render cleanly with WCAG AA contrast verified across all pairs in `mock_s19_shared_dialogs.html`.

## Tests (written by a different lane from this spec)

### Existing tests that may need edits (ONLY if Decisions 1 and 2 are approved)

- `app/src/androidTest/java/com/sherifradwan/ancnative/MedicationFormTest.kt`: lines 235, 243, 426 currently invoke `onNodeWithTag("medDate").performTextInput("1")` / `"2"`. If Decision 1 (`readOnly = true`) is approved, update those 3 test calls to pick a date via `medDateTodayButton` or `medDateCalendarButton` -> `datePickerConfirm` (while still verifying `medStatusChoice-1` swaps the label to `medication_stop_date` and writes `END_DATE` vs `START_DATE`).
- `app/src/androidTest/java/com/sherifradwan/ancnative/DeliveryPlanSheetTest.kt`: lines 300, 338, 353 currently invoke `type("deliveryOutcomeDateField", "2026-11-05")` / `"not-a-date"`. If Decision 2 is approved, update the valid-date assertions to select via `deliveryOutcomeDateFieldCalendarButton` / `Today` (and keep invalid-date coverage in unit tests on `DeliveryOutcomeTest`).

If Decisions 1 and 2 are deferred, zero existing test edits are expected.

### Must pass unchanged

- `app/src/androidTest/java/com/sherifradwan/ancnative/DeleteUndoUiTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/DraftRecoveryDialogTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/EditorSheetsRedesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/MedsTabSheetTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/VisitEntryDesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/VisitEditorRedesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/UltrasoundEditorRedesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/UltrasoundGuidedFlowTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/HistoryEditorsRedesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/LabEntryDesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/LabEditorRedesignTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/LabBatchUiTest.kt`
- `app/src/androidTest/java/com/sherifradwan/ancnative/AttachmentsRedesignTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/RecordTokensContrastTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/state/AncDeleteUndoStateTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/state/DraftRecoveryTest.kt`
- `app/src/test/java/com/sherifradwan/ancnative/model/DeliveryOutcomeTest.kt`

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/SharedDialogsRedesignTest.kt`

Mount `AncApp` (for `DeleteConfirmDialog`, `UndoBarHost`, `showNewPatientInfo`, `showDiscardDialog`, `draftRecoveryPrompt`), `ClinicalDateField` / `AncDatePickerDialog`, and `InAppCameraDialog` inside `360.dp x 640.dp` (`MainActivity` + `AncTheme`, single `setContent` per test, using `ComposeTextAsserts`):
- **Typography & unclipped text (`1.0f`, `1.3f`, `2.0f`)**: walk semantics trees of `deleteConfirmDialog`, `undoBar`, `deleteMessage`, `newPatientInfoDialog`, `shellDiscardDialog`, `draftRecoveryDialog`, `ancDatePickerDialog`, and `cameraPermissionCard`; assert zero `TextLayoutResult` nodes have `fontSize < 12.sp`; assert `assertAllTextWhole(...)` on `undoBar` (long medication + patient name), `deleteMessage`, `draftRecoveryDialog` (stale state with `draftDiscard` `"Discard and start fresh"` + `draftContinue`), and `ancDatePickerDialog` header/buttons.
- **Minimum `48.dp` targets**: assert `deleteConfirm`, `deleteCancel`, `deleteMessageDismiss`, `undoButton`, `newPatientInfoClose`, `discardDialog`, `keepEditingDialog`, `draftContinue`, `draftDiscard`, `datePickerConfirm`, `datePickerDismiss`, `${tag}CalendarButton`, `${tag}TodayButton`, `${tag}ClearButton`, and `camera_close` all measure `>= 47.9.dp` inside host bounds.
- **Destructive error coloring & stale callout**: assert `Delete` in `deleteConfirm`, `Discard` in `discardDialog`, and `Discard and start fresh` in `draftDiscard` render with `error` text color; assert `draftRecoveryStaleBanner` renders only when `isStale == true`.
- **Picker-only date interaction (`ClinicalDateField` + `AncDatePickerDialog`)**: tap the `ClinicalDateField` input box directly (`performClick()`), verify `ancDatePickerDialog` opens (`datePickerConfirm` / `datePickerDismiss` displayed), verify keyboard-mode toggle icon is absent (`showModeToggle = false`), tap `datePickerConfirm` and verify ISO `YYYY-MM-DD` output (`onDateSelected`), and verify `${tag}ClearButton` clears optional values to `""`.
- **In-app camera permission-denied state**: mount `InAppCameraDialog` without camera permission granted (`permissionAsked = true` or denied permission grant), verify `camera_permission_needed` text is whole inside `cameraPermissionCard` at `360.dp` (`1.0f`, `1.3f`, `2.0f`), verify `camera_close` measures `>= 48.dp`, and verify tapping `camera_close` invokes `onDismiss`.
- **Dark theme & RTL**: mount `DeleteConfirmDialog`, `UndoOfferBar`, `DraftRecoveryDialog`, and `AncDatePickerDialog` under `AncTheme(darkTheme = true)` and `LayoutDirection.Rtl` with zero overlap or horizontal overflow.

## Risk

**Medium (`AncApp.kt` shell hosting + cross-editor `ClinicalDateField`).** Editing `AncApp.kt` outside lines 289-388 risks breaking app lock, vault fail-closed routing, or `BackHandler` navigation; making `ClinicalDateField` `readOnly = true` without keeping `SemanticsProperties.EditableText` and `FocusRequester` / `imeAction` compatibility breaks `AncWorkflowTest` line 270 and `MedsTabSheetTest` line 1197; making `ClinicalDateField` `readOnly = true` without updating `MedicationFormTest` lines 235, 243, 426 breaks those 3 assertions; altering `UndoBarHost` placement in `AncApp.kt` or `DeleteConfirmDialog` `LaunchedEffect` guard risks duplicating or leaking undo offers; changing `InAppCameraDialog` disposal or `committed` tracking risks leaving temp photos or losing captures. Follow the invariants checklist line by line and keep `AncApp.kt` edits strictly inside lines 289-388.
