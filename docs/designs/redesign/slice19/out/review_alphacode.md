Slice 19 review (read-only, based only on the provided diff.patch; line numbers are approximate, derived from hunk headers `@@ +new,count`).

**Verdict: not PASS — 4 findings, 2 of them conditional on full-file context I could not inspect.**

---

### [F1] Behavior/regression — `ClinicalDateField` defaults changed for every caller
**File:** `app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt` — `shape`/`textStyle` defaults ≈ L180-181 (hunk `@@ +169,24`); `readOnly = true` ≈ L225 (hunk `@@ +205,50`)

**Defect:** Old defaults were `shape = OutlinedTextFieldDefaults.shape`, `textStyle = LocalTextStyle.current`, and the field was editable. New defaults are `RecordShapes.Small`, `RecordType.FieldValue`, and `readOnly = true`. This is not opt-in; it applies to every call site that doesn't pass explicit values. Slice 19 updates DatingEditor (×4), PreviousPregnancyEditor (×1), and DeliveryPlanSheet (×1); any other `ClinicalDateField` caller in the app (labs, visits, meds, etc.) is silently restyled and made read-only.

**Failure scenario:** A non-slice date field outside this slice now renders with `RecordShapes.Small`/`RecordType.FieldValue` instead of its previous local style, and the user can no longer type or paste a date — only the picker works. On a 360dp narrow viewport this can also change text wrapping/truncation. Result: inconsistent UI and a blocked data-entry workflow for fields the slice did not intend to touch.

**Fix:** Audit every `ClinicalDateField` call site. Either pass the previous defaults explicitly at non-slice sites, or make the restyle opt-in (keep `OutlinedTextFieldDefaults.shape`/`LocalTextStyle.current` as defaults and require `useRecordStyle = true` for the record look).

---

### [F2] Clinical safety — one-tap Clear on dating / previous-pregnancy dates
**File:** `app/src/main/java/com/sherifradwan/ancnative/ui/DatingEditor.kt` — four `ClinicalDateField` calls with `allowClear = true` ≈ L121 (hunk `@@ +118,7`), ≈ L136 (`@@ +133,7`), ≈ L151 (`@@ +148,7`), ≈ L196 (`@@ +193,7`); `app/src/main/java/com/sherifradwan/ancnative/ui/PreviousPregnancyEditor.kt` — one call ≈ L70 (hunk `@@ +67,7`)

**Defect:** `allowClear = true` adds a "Clear" chip whenever the value is non-blank. Previously blanking a date required deliberately editing the (editable) text field; now a single tap empties it. The slice does not touch any validation or calculation code.

**Failure scenario:** User taps Clear on an LMP/conception/previous-pregnancy date. If save validation accepts a blank value, downstream gestational-age/dating calculations receive an empty date → wrong clinical estimates or a crash on null/empty date parsing. Data loss of a clinically significant field with one mis-tap.

**Fix:** Re-run save validation with empty values for these fields; require a value or block save/repair before calculation. Confirm whether Clear is appropriate on required dating fields at all.

---

### [F3] Accessibility/usability — delete confirm no longer visually distinct from cancel
**File:** `app/src/main/java/com/sherifradwan/ancnative/ui/DeleteUndoUi.kt` — `DeleteConfirmDialog` confirm action ≈ L136-146 (hunk `@@ +115,46`)

**Defect:** The confirm action changed from a filled `Button` to a `TextButton` (both now `RecordType.Action` text buttons, confirm colored `error`, cancel `primary`). The destructive action has no distinct visual affordance.

**Failure scenario:** Under stress, a user may fail to locate the confirm action or confuse it with cancel → delete fails to complete, or the user taps the wrong action and loses a record. Equal prominence weakens the destructive-action affordance that a filled button provided.

**Fix:** Restore a differentiated confirm (filled button, icon, or stronger color contrast) while keeping cancel as a text button; verify with TalkBack that both actions remain separately reachable.

---

### [F4] Build risk — removed imports possibly still referenced (must clear before merge)
**File:** `app/src/main/java/com/sherifradwan/ancnative/ui/DeleteUndoUi.kt` — import block hunk `@@ +24,7` removed `RoundedCornerShape` and `material3.Button`; `app/src/main/java/com/sherifradwan/ancnative/ui/InAppCameraDialog.kt` — import block hunk `@@ +53,15` removed `RoundedCornerShape` and `MaterialTheme`.

**Defect (conditional):** The diff hunks shown do not include the full files. If either removed symbol is referenced anywhere else in those files, the build fails.

**Failure scenario:** Debug/release build breaks on import-resolution; CI fails.

**Fix:** Grep both files for `RoundedCornerShape`, `Button` (material3), and `MaterialTheme`; restore imports or replace usages. Also grep `AncDatePicker.kt` for `FilterChip`/`LocalTextStyle` (imports removed there too) before merging.

---

### Checked — no defect (parity preserved)
- `AncApp.kt`: new-patient and draft-recovery dialogs keep the same `onEvent` dispatch, testTags (`newPatientInfoDialog`, `newPatientInfoClose`, `shellDiscardDialog`/`discardDialog`/`keepEditingDialog`, `draftRecoveryDialog`/`draftContinue`/`draftDiscard`), and dismiss semantics; `DiscardChangesDialog` takes the same three tags — confirm the shared component maps dismiss to `KeepEditing` (component body not in this diff).
- `DeliveryPlanSheet.kt`: delivery-date migration keeps `tag = "deliveryOutcomeDateField"`, `fillMaxWidth` (now internal to `ClinicalDateField`), `revealOnFocus` → `fieldModifier`, `revealWhen(justDelivered || dateError != null)` → outer modifier, and error→`errorMessage`/hint→`helperText` mapping; no field dropped.
- Accessibility: `minimumTouchTarget()` retained on all dialog/field actions; `heading()` added to camera-permission title; date field gets `contentDescription = label` plus a click action (`Select %s`); three new string resources added to `strings.xml` (no hardcoded strings in the hunks).
- `AncDatePickerDialog`: new `title` param appended after `onDismiss` with a default, so existing positional 3-arg call sites are unaffected.

---

### Required before merge
1. Clear [F4] import-resolution check (full-file grep) — otherwise nothing else matters.
2. Resolve [F1]: audit remaining `ClinicalDateField` call sites; decide backward-compatible defaults vs. explicit opt-in.
3. Resolve [F2]: validation behavior with empty dating dates after Clear.
4. Decide [F3] with product/design (restore differentiated delete confirm or accept the text-button parity).

Once those are resolved, re-review the diff.
[Tokens] upload: 28073 download: 16962 cache_read: 0 cache_write: 0

