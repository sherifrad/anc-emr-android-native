# Slice 19: Shell and Shared Dialogs — Test Report

### Test File Created
- [`SharedDialogsRedesignTest.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/SharedDialogsRedesignTest.kt) (29 tests)

### Test Coverage Summary
- **No text under 12sp**: Verified across all shell dialogs (`deleteConfirmDialog`, `newPatientInfoDialog`, `shellDiscardDialog`, `draftRecoveryDialog`, `ancDatePickerDialog`), notification bars (`deleteMessage`, `undoBar`), field layouts (`ClinicalDateField` with error and helper text), and `InAppCameraDialog`. Every text element satisfies `style.fontSize >= 11.99.sp`.
- **48dp minimum touch targets**: Asserted on all interactive elements listed in the build oracle (`width >= 47.99.dp && height >= 47.99.dp`):
  - `deleteConfirm` & `deleteCancel` (TextButtons in `DeleteConfirmDialog`)
  - `deleteMessageDismiss` (TextButton in `DeleteMessageBar`)
  - `undoButton` (TextButton in `UndoOfferBar`)
  - `newPatientInfoClose` (Button in `showNewPatientInfo`)
  - `discardDialog` & `keepEditingDialog` (Buttons in `DiscardChangesDialog`)
  - `draftContinue` & `draftDiscard` (Buttons in `draftRecoveryPrompt`)
  - `datePickerConfirm` & `datePickerDismiss` (Buttons in `AncDatePickerDialog`)
  - `${tag}CalendarButton`, `${tag}TodayButton`, `${tag}ClearButton` (IconButton & `RecordFilterChip` in `ClinicalDateField`)
  - `camera_close` (OutlinedButton / Button in `InAppCameraDialog`)
- **Strings and layout elements**:
  - `DeleteConfirmDialog`: verified across all 6 `DeletableType`s (`MEDICATION`, `LAB`, `ULTRASOUND`, `VISIT`, `PREVIOUS_PREGNANCY`, `ATTACHMENT`) with correct type labels and patient record labels.
  - `DeleteMessageBar`: verified for `DELETE_FAILED`, `UNDO_FAILED` (both save and delete), `UNDO_BLOCKED`, and `CLEANUP_FAILED` with assertive live region.
  - `UndoOfferBar`: verified for `DELETE` and `SAVE` actions, running/progress state (`UndoPhase.RUNNING`), and polite live region.
  - `showNewPatientInfo`: verified with `newPatientInfoDialog` and `newPatientInfoClose`.
  - `showDiscardDialog`: verified with shared `DiscardChangesDialog` (`shellDiscardDialog`, `discardDialog`, `keepEditingDialog`).
  - `draftRecoveryPrompt`: verified with clean state and `draftRecoveryStaleBanner` (`draft_recovery_stale` callout) when `isStale = true`.
  - `AncDatePickerDialog`: verified `showModeToggle = false` (header mode switch pencil absent), headline and title typography, and UTC ISO `YYYY-MM-DD` formatting.
  - `ClinicalDateField`: verified `readOnly = true` picker enforcement (full box tap and calendar icon both open `AncDatePickerDialog`), `showTodayButton` / `${tag}TodayButton`, `allowClear` / `${tag}ClearButton`, placeholder `"Select date"`, error state, helper text, and disabled behavior.
  - `InAppCameraDialog`: verified `camera_close` dismissal and `cameraPermissionCard` title/description when camera permission is not granted.
  - Editor date field integration: verified `allowClear` chips and picker interactions in `DatingEditor` (`lmpDate`, `ultrasoundEdd`), `PreviousPregnancyEditor` (`pregnancyDateText`), and `DeliveryPlanSheet` (`deliveryOutcomeDateField`).
- **Behavior parity & event dispatching**:
  - `AncEvent.ConfirmDelete`, `AncEvent.CancelDelete`, `AncEvent.DismissDeleteMessage`, `AncEvent.UndoRequested`, `AncEvent.HideNewPatientInfo`, `AncEvent.ConfirmDiscard`, `AncEvent.KeepEditing`, `AncEvent.AcceptDraftRecovery`, `AncEvent.DiscardDraftRecovery`, `AncEvent.DatingDraftChanged`.
  - Conditional appearance: `${tag}ClearButton` only visible when `allowClear && value.isNotBlank() && enabled`; `draftRecoveryStaleBanner` only rendered when `recoveryPrompt.isStale`.
- **Responsive 360dp viewport & fontScale 1.3 / 2.0**:
  - `undoOfferBar_360dp_fontScale2_0_wrapsText_noClipping`: verifies text wrapping at `fontScale = 2.0f` to 3 lines without truncation or clipping.
  - `deleteConfirmDialog_360dp_fontScale1_3_noClipping` & `deleteConfirmDialog_360dp_fontScale2_0_noClipping`.
  - `draftRecoveryDialog_360dp_fontScale2_0_withStaleBanner_noClipping`.
  - `ancDatePickerDialog_360dp_fontScale2_0_noClipping`.
  - `newPatientInfoDialog_360dp_fontScale2_0_noClipping`.
  - `shellDiscardDialog_360dp_fontScale2_0_noClipping`.

### Existing Test Impact
- No existing strings or test tags were altered or removed; all existing assertions in `DeleteUndoUiTest.kt`, `DraftRecoveryDialogTest.kt`, `AncWorkflowTest.kt`, and editor redesign tests remain intact without requiring modifications.

### Verification
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q`: passed (exit code `0`).
- `./gradlew --offline :app:testDebugUnitTest`: passed (28 actionable tasks, exit code `0`).
