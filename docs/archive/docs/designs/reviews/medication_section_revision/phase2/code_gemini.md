## Code Review: Medication Section Revision (Phase 2 Patch)

**Design Reference:** `docs/designs/medication_section_revision.md`  
**Patch Under Review:** `docs/designs/reviews/medication_section_revision/phase2/main.patch`  
**Rules Reference:** `AGENTS.md` (Canonical reusability, viewport ergonomics, MVI purity, zero legacy baggage)

---

### Blocker

#### 1. Dual Stacking Discard Dialogs & Architecture Split
* **File & Line:** `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt:73-89, 140-162`; `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt:446-455`; `app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt:90-91, 281-305`
* **Scenario:** `MedicationSheet` introduces private compose state (`var confirmDiscard by rememberSaveable`) and its own `AlertDialog` (`testTag("medDiscardDialog")`), violating the canonical single-implementation rule (`AGENTS.md` §1). When a clinician edits a field (`touched = true`) and presses Cancel, `confirmDiscard = true` displays the local dialog. If the clinician then presses the system Back button, `AncApp.kt:90` intercepts it because `state.showDiscardDialog` is false, firing `AncEvent.Back`. In `AncState.kt:453`, `requestBack` sets `state.showDiscardDialog = true`, causing `AncApp.kt:281` to display the global `discardDialog` directly over `MedicationSheet`'s local dialog. Two separate discard prompts stack on top of each other.

#### 2. Stop Failure Leaves UI Permanently Blocked in Error State
* **File & Line:** `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationStopBar.kt:1608-1609, 1614-1631`; `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt:1059-1061, 1089-1094`; `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt:78-85`
* **Scenario:** If a one-tap stop save fails (e.g., stale revision or validation error), `AncReducer` sets `savePhase = SavePhase.ERROR` and retains `state.medicationQuickSave`. `StopFailure` renders the error banner, but provides no dismiss action, retry trigger, or event dispatch. Switching tabs, scrolling, or clicking outside does not clear `medicationQuickSave` or `saveError`. The error banner remains permanently stuck on `MedsTab`. Additionally, `clearedForLock()` (`AncState.kt:390-399`) fails to clear `medicationQuickSave`, leaking uncommitted clinical draft data across vault lock boundaries.

---

### Major

#### 3. 10-Second Undo Expiration Reset and State Loss on Rotation / Unmount
* **File & Line:** `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationStopBar.kt:1634-1639`; `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt:105-113`
* **Scenario:** Business logic is improperly implemented in a composable via `LaunchedEffect { delay(MEDICATION_STOP_UNDO_MILLIS) }` (`AGENTS.md` §2). `MedicationStopUndo` has no timestamp. Rotating the device recreates the composable and restarts the 10-second timer from zero. Furthermore, swiping away to `PatientHubTab.VISITS` unmounts `MedsTab`; swiping back 30 seconds later mounts a fresh `StopUndo` with a new 10-second countdown, enabling illegal out-of-window stop undos.

#### 4. IME Focus Traversal Skips Form Controls and Lacks `Done` Action
* **File & Line:** `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt:140-142, 240-250, 261`
* **Scenario:** Pressing Enter/Next on Dosage (`line 140`) executes `(if (otherMode) otherFocus else dateFocus).requestFocus()`. When `otherMode` is false, focus jumps directly past Frequency chips, Route, and Status to `dateFocus`. From `dateFocus`, Next jumps to Notes. Notes is multi-line with `ImeAction.Default` (`line 261`). No form field provides `ImeAction.Done` to trigger save, violating Technical Plan §6.

#### 5. Expanded Medication Card State Lost on Configuration Change
* **File & Line:** `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt:104`
* **Scenario:** `var expandedMedId by remember { mutableStateOf<String?>(null) }` uses transient `remember`. Rotating the phone resets `expandedMedId` to null, collapsing open medication cards and violating the design goal that open cards survive screen changes.

#### 6. Medication Frequency Omitted from Canonical `MedicationCard`
* **File & Line:** `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationCard.kt:60, 142-153`
* **Scenario:** `medicationInstructions` joins only `dosage, route, notes`. Frequency is neither computed into `instructions` nor rendered in the expanded `FactRow`s (`lines 145-152`). A clinician who sets frequency in `MedicationForm` cannot see it in the review list, violating Target 14 ("name, dose and frequency are never truncated").

#### 7. Active Tiles Fail to Visibly Disable During Active Saves
* **File & Line:** `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt:1317`
* **Scenario:** `Modifier.alpha(if (enabled || isActive) 1f else DISABLED_TILE_ALPHA)` evaluates to `1f` when `enabled == false` and `isActive == true`. Active medication tiles remain at 100% opacity instead of dimming to `0.38f`, violating Technical Plan §5.

#### 8. 360dp Viewport Overcrowding and Vertical Keyboard Compression
* **File & Line:** `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationCard.kt:71-136`; `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt:100-137`
* **Scenario:** In `MedicationCard.kt`, three 48dp action buttons (Stop, Delete, Edit) consume 144dp of row width. On a 360dp viewport (328dp content), less than 140dp remains for clinical text, forcing `medicationInstructions` to truncate with ellipsis (`maxLines = 2`). In `MedicationSheet.kt`, hosting `UndoBarHost` beneath action buttons inside an `imePadding()` Column leaves under 65dp of vertical space for `MedicationForm` when the soft keyboard is open.

---

### Minor

#### 9. Hard-Coded Display Strings
* **File & Line:** `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt:132, 157, 171, 215`; `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalPresets.kt:23`
* **Scenario:** `MedsTab.kt` hardcodes `"Active Prescriptions (${activeMeds.size})"`, `"Prescribe"`, `"No records"`, and `"Discontinued / Past Prescriptions (${pastMeds.size})"`. `ClinicalPresets.kt:23` hardcodes common frequencies (`"Once daily"`, etc.) without string resources, breaking RTL/Arabic localization.

#### 10. False Discard Prompt on Reverted Edits
* **File & Line:** `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt:418, 450`
* **Scenario:** `touched = true` is latched upon any field change. If a clinician edits a dose and then reverts it to the stored value (`isDirty == false`), tapping Cancel still prompts to discard unsaved changes.

#### 11. Dead Validation Code for Frequency in `MedicationForm`
* **File & Line:** `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt:194, 209-215`
* **Scenario:** `MedicationForm` checks `ClinicalValidationError.REQUIRED_FREQUENCY`. `SaveMedicationCommand.prepare` treats frequency as optional (`ClinicalCommands.kt:446`) and never emits this error, making this UI check dead code in production.
