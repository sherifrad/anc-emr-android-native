# Code Review: Medication Section Revision (Stage 1)

**Files Reviewed:**
* `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt` (lines 1–300)
* `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt` (lines 1–230)
* `app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt` (lines 1–158)
* Design reference: `docs/designs/medication_section_revision.md`

---

## Blocker

### 1. Flipping status to Stopped leaves `endDate` blank and fails validation on Save
* **Location:** [`MedicationForm.kt:104-105, 225-227`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L104-L105)
* **Concrete Failing Scenario:** A clinician opens an active medication to stop it and taps the "Stopped" `BigChoiceButton`. Line 227 dispatches `change(MedicationDraftField.STATUS, next)`. Lines 104–105 switch the date field binding: `dateField` becomes `END_DATE` and `dateValue` becomes `values.endDate`. For an active row, `values.endDate` is `""`. Neither `MedicationForm` nor the reducer populates `endDate` with today's date upon status toggle. The date input suddenly goes blank in the UI. When the clinician taps Save, save fails immediately with `ClinicalValidationError.REQUIRED_DATE`. This directly violates Design §3 (lines 134–135) and Goals (line 32), which mandate that the single date field always defaults to today.

### 2. Missing `ImeAction.Done` and Enter-to-Save broken
* **Location:** [`MedicationForm.kt:257`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L257)
* **Concrete Failing Scenario:** The Notes field configures `imeAction = ImeAction.Default`. No field in `MedicationForm` specifies `ImeAction.Done`, and no keyboard action triggers Save. When a clinician enters data via soft keyboard or hardware keyboard, pressing Enter on the final field inserts a newline instead of submitting. This violates CUJ 2 (line 87: *"and the doctor taps Save (or presses Enter on the last field)"*), Design §6 (line 153), and Detailed Implementation §2 (line 259: *"every field ImeAction.Next (the last Done)"*).

---

## Major

### 3. State desynchronization across drafts: `otherMode` is sticky in `rememberSaveable` without key
* **Location:** [`MedicationForm.kt:89-91, 164, 175`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L89-L91)
* **Concrete Failing Scenario:** A clinician edits a medication with custom frequency "PRN" (`otherMode = true`). They then tap "Save and add another" or load a catalog tile prefilled with "OD". Because `rememberSaveable` lacks an instance key, `otherMode` remains `true` from the previous session. The "OD" chip evaluates `!otherMode && values.frequency == frequency` to `false` (unselected), the "Other" chip stays selected, the custom text box opens with "OD", and pressing Next on Dosage jumps to the custom frequency box instead of Date.

### 4. State loss on rotation: Discard confirmation dialog dismissed without confirmation
* **Location:** [`MedicationSheet.kt:69, 175-197`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt#L69)
* **Concrete Failing Scenario:** The clinician modifies a field (`touched = true`) and presses Cancel or swipes the sheet down. The `AlertDialog` ("Discard changes?") appears. If device orientation changes (screen rotation), `confirmDiscard` resets to `false` because it uses `remember` instead of `rememberSaveable`. The dialog disappears silently, leaving the dirty sheet displayed.

### 5. State loss on rotation: Date picker dialog dismissed and input lost
* **Location:** [`AncDatePicker.kt:99, 148-156`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt#L99)
* **Concrete Failing Scenario:** The clinician taps the calendar icon (`${tag}CalendarButton`) in `ClinicalDateField` to select a date. While `AncDatePickerDialog` is displayed, the phone rotates. Because `showDatePicker` is `remember { mutableStateOf(false) }`, it resets to `false`. The dialog vanishes and the clinician's in-progress date navigation is discarded.

### 6. Zero contrast on Save button spinner in disabled state
* **Location:** [`MedicationSheet.kt:140-147`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt#L140-L147)
* **Concrete Failing Scenario:** While saving (`saving == true`), `enabled` is `false` (line 67). In Material 3, a disabled `Button` renders with `disabledContainerColor` (`onSurface` at 12% opacity, light gray). Line 146 hardcodes `CircularProgressIndicator` color to `MaterialTheme.colorScheme.onPrimary` (white). A white spinner on a light gray background produces ~1.1:1 contrast, rendering the spinner virtually invisible and failing WCAG 3:1 graphical element contrast requirements.

### 7. Action buttons overflow and text truncates on 360dp narrow viewports
* **Location:** [`MedicationSheet.kt:123-171`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt#L123-L171)
* **Concrete Failing Scenario:** On a 360dp viewport with `!isExistingRow`, the bottom bar has 312dp available width across 3 buttons (Cancel weight 1f = 82dp, Save weight 1f = 82dp, Save & Add Another weight 1.8f = 148dp). When saving, the Save button contains an 18dp spinner, 8dp padding, and "Saving…" text (~55dp), requiring >=97dp. Line 152 enforces `maxLines = 1, softWrap = false`, which forcibly truncates or clips the text on 360dp devices.

### 8. Focus defect: Next on Dosage skips Route and Frequency
* **Location:** [`MedicationForm.kt:136-138`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L136-L138)
* **Concrete Failing Scenario:** In `Dosage`, `onNext = { (if (otherMode) otherFocus else dateFocus).requestFocus() }`. When `otherMode` is false, pressing Next skips Route and Frequency chips entirely, jumping straight to Date. When `otherMode` is true, focus jumps to the custom frequency box, skipping Route entirely.

---

## Minor

### 9. AGENTS.md hit area violation: Today chip and dialog buttons lack 48dp target
* **Location:** [`AncDatePicker.kt:69, 75, 138-143`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt#L138-L143)
* **Concrete Failing Scenario:** `FilterChip` in `ClinicalDateField` does not set `Modifier.minimumTouchTarget()`, defaulting to 32dp height. `DatePickerDialog` Confirm and Dismiss `TextButton`s also omit `minimumTouchTarget()`, violating the AGENTS.md 48dp hit area rule.

### 10. Hardcoded strings and RTL bidirectional layout defect in Today button
* **Location:** [`AncDatePicker.kt:141`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt#L141)
* **Concrete Failing Scenario:** `label = { Text("⚡ " + stringResource(R.string.today) + " (" + LocalDate.now().toString() + ")") }` hardcodes `"⚡ "`, `" ("`, and `")"`. In Arabic (RTL), concatenating LTR symbols and dates directly causes bidirectional punctuation inversion and text corruption.

### 11. Hardcoded string `"Oral"` in route selection
* **Location:** [`MedicationForm.kt:142, 146`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L142)
* **Concrete Failing Scenario:** `values.route.ifBlank { "Oral" }` and `values.route.ifEmpty { "Oral" }` hardcode `"Oral"` instead of referencing `ClinicalVocabulary.medicationRoutes.first()` or string resources.

### 12. Non-interactive `RouteReadOnly` row uses `minimumTouchTarget`
* **Location:** [`MedicationForm.kt:272`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L272)
* **Concrete Failing Scenario:** `RouteReadOnly` adds `Modifier.minimumTouchTarget()` to an unclickable, non-semantic row, producing an invisible 48dp touch container for accessibility services on static text.
