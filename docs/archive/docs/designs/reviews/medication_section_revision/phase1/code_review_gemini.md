# Independent Code Review: Medication Form & Sheet

**Design Document:** [`medication_section_revision.md`](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/medication_section_revision.md)  
**Files Audited:**  
- [`MedicationForm.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt)
- [`MedicationSheet.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt)
- [`AncDatePicker.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt)
- [`strings.xml`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/res/values/strings.xml)

---

## Blocker

### 1. Permanent UI Freeze / Locked Sheet on Unresolved Save
- **Location:** [`MedicationSheet.kt:65, 75, 113, 121, 140`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt#L65)
- **Failing Scenario:** When a save enters `SavePhase.UNRESOLVED` (e.g. database timeout or ambiguous disk state), `enabled` evaluates to `false` (`enabled = savePhase == SavePhase.IDLE || savePhase == SavePhase.ERROR`). `saving` evaluates to `false`. Every interactive button (Cancel, Save, Save & Add Another) is disabled. Furthermore, line 75 gates sheet dismissal behind `if (enabled) requestCancel()`, so tapping outside the sheet or triggering a back gesture does nothing. The doctor is trapped inside the bottom sheet with no recovery path short of force-killing the app.
- **Design/Rule Violation:** Technical Plan §2 & §7 state rejected saves must keep the sheet operable and allow the doctor to retry or cancel.

### 2. Status Switch to "Stopped" Wipes Out Date Field and Rejects Save
- **Location:** [`MedicationForm.kt:105-107, 227-230, 238-241`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L105-L107)
- **Failing Scenario:** A doctor edits an active medication (or fills a new form) and toggles the status button to "Stopped". Line 106 switches `dateField` from `START_DATE` to `END_DATE`. Because `values.endDate` starts blank, the date input field instantly becomes empty `""`. Toggling status emits only `AncEvent.MedicationDraftChanged(STATUS, ...)`, which does not auto-populate `endDate` to today's date. When the doctor taps Save, the command fails validation with `REQUIRED_DATE` / `INVALID_DATE`.
- **Design/Rule Violation:** Technical Plan §3 & §4 require the date field to default to today's date when stopping.

---

## Major

### 3. Frequency State Desynchronization Across Draft Reset and Rotation
- **Location:** [`MedicationForm.kt:88-90, 166-170, 178-184`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L88-L90)
- **Failing Scenario:** The doctor enters a custom frequency ("Every 8 hrs"), making `otherMode = true`. The doctor taps "Save and add another". The draft resets to blank defaults (`values.frequency = ""`), but `otherMode` is remembered via `rememberSaveable` without being keyed to the medication or draft instance. The sheet presents the custom text input instead of the preset chips for the new drug. Moreover, caching `otherMode` locally in the composable violates AGENTS.md Unidirectional MVI rules ("no local business logic mutations inside composables").

### 4. Broken Enter-to-Next Navigation & Missing `ImeAction.Done`
- **Location:** [`MedicationForm.kt:124-126, 137-140, 246-248, 259`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L124-L126)
- **Failing Scenario:** When on the Dose field (`medDosage`) with `otherMode == false`, pressing Enter (Next) invokes `(if (otherMode) otherFocus else dateFocus).requestFocus()`. This skips Route, Frequency chips, and Status buttons entirely, jumping to `ClinicalDateField`. On `medNotes`, `imeAction = ImeAction.Default` simply inserts a newline. No field uses `ImeAction.Done`, and pressing Enter on the final field never submits the form.
- **Design/Rule Violation:** Violates Technical Plan §6 ("gives each field the Next keyboard action, and the last one Done") and CUJ 2 ("doctor taps Save (or presses Enter on the last field)").

### 5. Broken Focus on "Other" Frequency Chip (`focusOtherBox` Exception Swallowed)
- **Location:** [`MedicationForm.kt:91-101`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L91-L101)
- **Failing Scenario:** Tapping the "Other" chip sets `focusOtherBox = true`. `LaunchedEffect(focusOtherBox)` fires before the newly rendered `EditorField` is attached to the layout tree. `otherFocus.requestFocus()` throws `IllegalStateException`, which is silently caught ("Box not attached yet; the doctor can tap it"). The input field fails to gain focus, the soft keyboard does not appear, and the doctor is forced to tap the field again manually.

### 6. 360dp Viewport Text Truncation & Overflow in Action Buttons
- **Location:** [`MedicationSheet.kt:107-154`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt#L107-L154)
- **Failing Scenario:** On a 360dp narrow viewport (312dp available width), the Save button receives ~82dp. During saving (`saving = true`), the button hosts an 18dp spinner + 8dp spacing + 16dp horizontal padding, leaving ~40dp for text. The string "Saving…" (`R.string.medication_saving`) with `maxLines = 1, softWrap = false` overflows and truncates without an ellipsis. On 1.3x accessibility text scaling, "Save & add another" also clips.
- **Design/Rule Violation:** AGENTS.md 360dp Viewport Invariant ("render without critical text truncation on 360dp").

### 7. State Lost on Screen Rotation / Recomposition
- **Location:** [`MedicationSheet.kt:67, 158-180`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt#L67), [`AncDatePicker.kt:99`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt#L99)
- **Failing Scenario:** 
  1. User edits a field, taps Cancel, and the "Discard changes?" confirmation dialog opens. Rotating the screen resets `confirmDiscard` (plain `remember`), dismissing the confirmation dialog unexpectedly.
  2. In `ClinicalDateField`, tapping the calendar icon sets `showDatePicker = true` (plain `remember`). Rotating the screen resets `showDatePicker` to `false`, discarding the active date picker dialog and in-progress selection.

---

## Minor

### 8. Sub-48dp Touch Target on "⚡ Today" Button
- **Location:** [`AncDatePicker.kt:138-144`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt#L138-L144)
- **Failing Scenario:** The "⚡ Today" `FilterChip` lacks `Modifier.minimumTouchTarget()`. Unlike the frequency chips in `MedicationForm.kt:173, 189`, it defaults to 32dp height, violating AGENTS.md 48dp touch target rule and WCAG 2.5.5.

### 9. Hard-Coded Strings & BiDi/RTL Layout Distortion
- **Location:** [`AncDatePicker.kt:141`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt#L141), [`MedicationForm.kt:144, 148`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L144)
- **Failing Scenario:** 
  1. `AncDatePicker.kt:141` concatenates raw strings `"⚡ "`, `" ("`, `")"`. In Arabic RTL mode, raw ASCII parentheses and symbols invert and distort bidirectional text rendering.
  2. `MedicationForm.kt:144, 148` hard-codes the fallback string `"Oral"`.

### 10. `revealOnFocus` Excludes "Today" Date Shortcut
- **Location:** [`MedicationForm.kt:245, 296-302`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L245), [`AncDatePicker.kt:129, 132-145`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt#L129)
- **Failing Scenario:** `revealOnFocus()` is attached only to the `OutlinedTextField` via `fieldModifier`. When `ClinicalDateField` gains focus and scrolls into view, the "⚡ Today" chip directly underneath remains positioned outside the visible scroll container, hidden behind the virtual keyboard.

### 11. Accessibility Semantics Deficiencies
- **Location:** [`MedicationSheet.kt:81`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt#L81), [`MedicationForm.kt:276-281`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L276-L281), [`AncDatePicker.kt:141`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt#L141)
- **Failing Scenario:** `LinearProgressIndicator` has no accessibility label; the `Lock` icon on `RouteReadOnly` has `contentDescription = null` without semantic indications that the route is immutable; TalkBack reads out literal unicode names ("High voltage sign") for the "⚡" glyph in `ClinicalDateField`.

### 12. Dead Code for Optional Frequency Validation
- **Location:** [`MedicationForm.kt:192, 206-213`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt#L192)
- **Failing Scenario:** `MedicationForm` checks for `ClinicalValidationError.REQUIRED_FREQUENCY`. In `ClinicalCommands.kt:445`, frequency is explicitly optional. The validator never generates `REQUIRED_FREQUENCY`, rendering this error UI dead code.
