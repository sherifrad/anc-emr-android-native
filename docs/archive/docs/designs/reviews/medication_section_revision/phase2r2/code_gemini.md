# Code Review: Phase 2 Revision 2 (`main.patch`)

Review of `docs/designs/reviews/medication_section_revision/phase2r2/main.patch` against `docs/designs/medication_section_revision.md` and `AGENTS.md` rules.

---

### Blocker

#### 1. Stacked Discard Dialogs on System Back
- **Files**:
  - `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt:482-493` (`requestBack`)
  - `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt:73-77, 140-163`
  - `app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt:90-92, 281`
- **Scenario**: When editing a medication (`touched = true`), tapping the sheet's "Cancel" button triggers `MedicationSheet`'s local `AlertDialog` (`confirmDiscard = true`, tag `medDiscardDialog`). If the clinician then presses the Android system Back button/gesture to dismiss this dialog, `MedicationSheet` lacks a `BackHandler`, so `AncApp.kt:90` catches it and dispatches `AncEvent.Back`. In `AncReducer.requestBack` (`AncState.kt:488-492`), because `event == AncEvent.Back` and `medicationDraft.touched == true`, it sets `state.showDiscardDialog = true`. Both `medDiscardDialog` (local) and `discardDraftDialog` (global from `AncApp.kt:281`) are rendered stacked on screen simultaneously. This also violates the "Rule of Reusability (One Canonical Component)" by introducing parallel discard dialog implementations.

---

### Major

#### 2. Missing "Show Past" Toggle (Design Violation)
- **File**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt:213-250`
- **Scenario**: Design Goals (line 29), CUJ 1 (line 82), and Technical Plan §5 (line 148) mandate that stopped drugs sit behind a "Show past" toggle so the tab remains review-first. In `MedsTab.kt:213`, `if (pastMeds.isNotEmpty())` displays all discontinued prescriptions unconditionally as full cards. For patients with lengthy medication histories, past records clutter the screen and cannot be collapsed.

#### 3. Always-Expanded Picker Pushes Active Prescriptions Offscreen
- **Files**:
  - `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt:76-161`
  - `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt:114-123`
- **Scenario**: Design Goal 3 (line 30), Technical Plan §5 (line 145), and Detailed Plan Item 12 (line 309) require a single collapsed "Add medication" row that expands to a search box and a height-limited, self-scrolling list. Instead, `MedicationTapToAddSection` remains an always-open `SectionCard` rendering all catalog tiles into the tab's `LazyColumn`. On a 360dp phone, this long list pushes active prescriptions off-screen, directly recreating "Problem 2" from the design.

#### 4. IME Focus Skips Route and Frequency & Cannot Save via Keyboard
- **File**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt:139-142, 248-262`
- **Scenario**: When prescribing a new drug (`isExistingRow == false`), `Route` is an editable `ChoiceGroup` (lines 148-158) and `Frequency` uses filter chips (lines 160-194). In `EditorField("Dosage")`, `keyboardActions = KeyboardActions(onNext = { (if (otherMode) otherFocus else dateFocus).requestFocus() })`. Pressing IME "Next" on Dosage jumps over Route and Frequency chips straight to Date. When prescribing a custom medication where Frequency is empty and mandatory (`ClinicalValidationError.REQUIRED_FREQUENCY`), the user is skipped past the field. Furthermore, Date's "Next" moves to multi-line Notes (`ImeAction.Default`), making it impossible to submit/save via Enter/IME Done, violating Design CUJ 2 (line 87).

#### 5. 360dp Viewport Horizontal Squeeze from 3 Side-by-Side Action Buttons
- **File**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationCard.kt:123-154`
- **Scenario**: On active rows, three `IconButton`s (Stop, Delete, Edit) with `minimumTouchTarget()` (48x48dp each) consume 144dp of horizontal width. On a 360dp device (32dp outer padding, 12dp card padding, 28dp chevron and spacing), only ~144dp remains for the entire text column. Clinical drug names (e.g., "Amoxicillin / Clavulanate") and status badges wrap aggressively, violating 360dp viewport readability.

#### 6. Screen Reader Ambiguity on Medication Action Buttons
- **File**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationCard.kt:124-154`
- **Scenario**: Stop `IconButton` sets `contentDescription = stringResource(R.string.stop_medication)` and Edit sets `contentDescription = stringResource(R.string.edit_medication)`. Unlike `DeleteIconButton` (`DeleteUndoUi.kt:673`, which formats `R.string.delete_record_description` with `label`), neither includes the medication name. In a list with multiple prescriptions, TalkBack users hear repetitive "Stop medication, button" and "Edit medication, button" with no context on which drug will be modified.

---

### Minor

#### 7. Hard-Coded English Strings & RTL Layout Scrambling
- **File**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt:133, 158, 172, 216`
- **Scenario**: Four UI strings are hard-coded in English rather than in `strings.xml`:
  - Line 133: `"Active Prescriptions (${activeMeds.size})"`
  - Line 158: `"Prescribe"`
  - Line 172: `"No records"`
  - Line 216: `"Discontinued / Past Prescriptions (${pastMeds.size})"`
  In RTL locales (Arabic), interpolating numbers inside unformatted LTR string literals causes parentheses and numerals to reverse directionality.

#### 8. Header Text and Button Collision on 360dp
- **File**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt:127-161`
- **Scenario**: `Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween)` places `Text("Active Prescriptions (${activeMeds.size})")` and `Button("Prescribe")` on the same horizontal line without weight or text truncation handling. On a 360dp screen, when translated or displaying two-digit counts, the title collides with or clips the prescribe button.
