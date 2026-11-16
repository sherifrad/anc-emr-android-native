# Goldfish Review: Medication Section Revision (Stage 1)

**Persona:** Skeptical Architect (`egm-zoom-out`)  
**Standard:** Zoom-Out Standard (`resources/document_standards/zoom-out-standard.md`)  
**Target Document:** `docs/designs/medication_section_revision.md` (Lines 1–372)  
**Repository Verified:** `/Users/sherifgamalradwan/anc-emr-android-native`  

---

## Executive Summary

The core architectural motivation of this design is sound and directly addresses real usability defects: eliminating the disruptive full-screen `AncRoute.MedicationEditor` route swap, consolidating duplicate form definitions into a single reusable `MedicationForm`, and collapsing the space-consuming tile list on 360dp viewports.

However, from a **Zoom-Out (YAGNI, simplicity, minimalism, and codebase health)** perspective, the design suffers from **scope creep and premature complexity**. It bundles three high-risk, speculative features into what should be a clean UI/navigation refactor:
1. An un-architected dose-change audit trail simulated by parsing free-text notes (`ReplaceMedicationCommand`).
2. An automated expiry/course-length calculation that breaks function signatures across the entire app.
3. A bespoke keyboard focus framework (`FormFocusChain`) for a single consuming form.

These additions expand the blast radius into Level-3 clinical safety and core logic files (`ClinicalCommands.kt`, `RecordsRepository.kt`, `UndoCoordinator.kt`, `ClinicalModels.kt`), multiplying risk without proportional Stage 1 value.

---

## Findings

### 1. [Blocker] YAGNI & Fragile Workaround: "Changed" Mark via Free-Text Notes and Dual-Record Replace Command

* **Affected Scope:** Goals (§34, lines 34), Decisions (line 58, 61), Technical Plan §4 (lines 126–129), Technical Plan §8 (lines 150–153), Target Files (lines 238–250, 274, 334, 350–351).
* **Codebase Verification:**
  * `app/src/main/java/com/sherifradwan/ancnative/storage/ClinicalCommands.kt` (lines 384–490): Currently defines single-entity commands (`SaveMedicationCommand`, `DeleteRecordCommand`).
  * `app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt` (line 353, lines 1453–1490): Command dispatcher and `saveMedication` operate on individual records within a single transaction.
* **Simplicity Rationale:**
  The design acknowledges that Room schema v6 is frozen (AGENTS.md invariant: zero migrations permitted without clinical sign-off). To circumvent this, it invents a complex workaround:
  1. Splitting a single user action (dose/frequency change) into two separate database records (stopping the old row and inserting a new row) via a new dual-record transaction: `ReplaceMedicationCommand`.
  2. Generating an un-validated, un-protected human-readable string (`"Changed from 1 tab · once daily on 2026-11-10 14:05"`) and appending it to the free-text `notes` field.
  3. Reading that free-text string prefix with `hasMedicationChangeNote(notes)` in `MedicationCard.kt` to trigger UI badges ("Changed" mark).
  4. Building `UndoMedicationChangeCommand` to coordinate the rollback of two rows (restoring old row, deleting new row, validating two content hashes and revisions).

  This is a fragile hack masquerading as an architectural feature. As the document admits (line 165), if the clinician edits or clears the notes, the "Changed" mark disappears. In a Software as a Medical Device (SAMD) system, clinical history cannot rely on substring parsing of user-editable text fields.
* **Simplification:**
  Drop the dual-record replace transaction, notes string parsing, and dual-record undo command from Stage 1. If dose/frequency history is required, defer it to Stage 2 with a proper schema migration (foreign key / audit table) when clinical sign-off is available. For Stage 1, let dose/frequency edits update the existing medication row in place (Alternative D, lines 185–189).

---

### 2. [Blocker] YAGNI & Blast Radius: Course Length Feature Belongs in Stage 2

* **Affected Scope:** Goals (§33, line 33), Technical Plan §3 (lines 117–122), Detailed Plan (lines 236–239, 309, 344, 348).
* **Codebase Verification:**
  * `app/src/main/java/com/sherifradwan/ancnative/model/ClinicalModels.kt` (line 58): `isOngoingMedication(status: String, isNonCompliant: Boolean = false): Boolean`
  * Consumed across 4 key UI locations:
    * `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt` (lines 54, 57)
    * `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt` (line 399)
    * `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationCard.kt` (line 103)
    * `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt` (line 183)
* **Simplicity Rationale:**
  The design states in line 15: *"Favourites, dose/frequency variants and the medication settings screen are Stage 2"*. Yet course length (pre-set chips for 3, 5, 7 days, custom day inputs, end-date calculation) is bundled into Stage 1.
  This introduces temporal expiration logic into what was previously a static status check: a drug's active status is computed on-the-fly (`endDate < today`) without an explicit state transition event or database update.
  More critically, changing `isOngoingMedication` to require `endDate` and `today` breaks call-sites across `ClinicalModels.kt`, `PatientHubHeader.kt`, `MedsTab.kt`, `MedicationCard.kt`, and `MedicationTapToAddSection.kt`.
* **Simplification:**
  Defer course length to Stage 2 alongside dose/frequency variants. In Stage 1, keep `startDate` (defaulting to today) and `endDate` as standard date fields. Maintain the existing 2-parameter signature of `isOngoingMedication(status, isNonCompliant)` without injecting temporal date calculations into the UI draw pass.

---

### 3. [Major] Premature Abstraction: `FormFocusChain` as a Shared Helper

* **Affected Scope:** Goals (§38, line 38), Technical Plan §6 (lines 138–142), Target Files (lines 296–305, 355).
* **Codebase Verification:**
  * `app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt` (lines 395–421): `EditorField` already exposes `modifier: Modifier`, `imeAction: ImeAction = ImeAction.Default`, and `keyboardActions: KeyboardActions = KeyboardActions.Default`.
  * `app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt` (lines 77, 91, 287): Currently the only place in the entire app using Compose `FocusRequester`.
* **Simplicity Rationale:**
  The design proposes extracting a generic framework helper `FormFocusChain.kt` in `ui/components/`, adding instrumentation tests (`FormFocusChainTest.kt`), and altering `Components.kt`. Yet line 38 explicitly states: *"This is built for medications first and as a shared behavior that other screens can adopt"*, and line 49 confirms that no other screen will use it in Stage 1.
  Building reusable infrastructure for $N=1$ consumers is a textbook premature abstraction (violating the Rule of Three). Furthermore, `EditorField` already accepts `modifier` (for `Modifier.focusRequester(...)`) and `keyboardActions` (for `onNext`).
* **Simplification:**
  Implement focus navigation inline within `MedicationForm.kt` using standard Compose primitives (`remember { FocusRequester() }` and `KeyboardActions(onNext = { ... })`). Do not create `FormFocusChain.kt` or introduce framework wrappers until multiple screens require identical focus behaviors.

---

### 4. [Major] Reinventing the Wheel / Niche UX: Date Chips ("3 days ago") vs Existing Codebase Patterns

* **Affected Scope:** Goals (§32, line 32), Technical Plan §3 (line 119), Target Files (lines 309, 344).
* **Codebase Verification:**
  * `app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt` (lines 43–60): `AncDatePickerDialog` already exists and wraps Material 3 `DatePickerDialog`.
  * `app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt` (lines 115–151): `AncDatePickerField` already exists and includes a built-in "⚡ Today" shortcut chip (`tag + "TodayButton"`).
* **Simplicity Rationale:**
  The design specifies a bespoke "date chip" UI offering three options: *Today*, *3 days ago*, and *pick-a-date*.
  While using `AncDatePickerDialog` is good reuse of the existing project dialog, inventing a "3 days ago" quick-chip is an arbitrary, niche optimization not supported by any documented clinical workflow in the problem statement.
* **Simplification:**
  Reuse the established `AncDatePickerField` or a simple date selector using `AncDatePickerDialog` with a default of today. Eliminate the bespoke "3 days ago" chip and its associated string resources.

---

### 5. [Major] Altitude Drop: Implementation Checklist in Architectural Design Document

* **Affected Scope:** Detailed Implementation Plan (lines 215–356).
* **Simplicity Rationale:**
  The document drops altitude dramatically in Section 12, listing exact line numbers (`AncState.kt:53`, `AncState.kt:357`, `AncState.kt:1483`, `AncState.kt:1716`, `AncApp.kt:238`, `Components.kt:403`, `RecordsRepository.kt:353`), concrete DAO method calls, internal test function names, and string keys.
  As dictated by the Zoom-Out Standard (§3 Anti-Weeds): *"High-level sections must focus on architecture, interfaces, and data flows, not code-level implementation details."* Exact line citations and step-by-step code edits belong in a pull request task list or engineering notebook, not a design document where they create brittle clutter.
* **Simplification:**
  Elevate the implementation plan to component-level responsibilities (e.g., "Retire `AncRoute.MedicationEditor`", "Extract `MedicationForm`", "Implement bottom sheet hosting in `MedsTab`"). Remove exact line numbers and concrete test enumerations.

---

### 6. [Nit] Premature Optimization: Quick Action Serialization Guard

* **Affected Scope:** Goals (§37, line 37), Technical Plan §5 (line 135).
* **Codebase Verification:**
  * `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt` (line 1471): `if (state.savePhase.blocksEditing() || state.hasDirtyDraft) return state` already prevents starting an editor while a save is pending.
* **Simplicity Rationale:**
  The design introduces explicit tile disabling across the picker to prevent fast taps from failing the revision check. Since opening the bottom sheet already creates a modal interaction barrier that blocks tapping background tiles, adding proactive button disabling logic to the picker is redundant.
* **Simplification:**
  Rely on the modal sheet's natural focus capture. Do not wire auxiliary disabled states through the picker unless rapid double-tap race conditions are actively observed.

---

## Codebase Claims Verification

The following files and line ranges were opened and verified in `/Users/sherifgamalradwan/anc-emr-android-native`:

| Claim in Design Document | Verified File & Lines | Status | Note |
|---|---|---|---|
| `AncRoute.MedicationEditor` route definition | [`AncState.kt#L53`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt#L53) | **Verified** | Route exists and is mapped to `patientId` at line 357. |
| `startMedicationEditor` swaps route | [`AncState.kt#L1470-L1490`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt#L1470-L1490) | **Verified** | Line 1483 sets `route = AncRoute.MedicationEditor(...)`. |
| Route back handling maps to Meds tab | [`AncState.kt#L1716`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt#L1716) | **Verified** | Returns `AncRoute.Workspace(..., MEDICATIONS)`. |
| Route dispatch in UI root | [`AncApp.kt#L238`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt#L238) | **Verified** | `is AncRoute.MedicationEditor -> MedicationEditor(...)`. |
| Multi-event tile tap dispatch | [`MedicationTapToAddSection.kt#L51-L78`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt#L51-L78) | **Verified** | `dispatchMedicationDraft` dispatches `StartMedicationEditor`, up to 6 `MedicationDraftChanged` events, and `SubmitMedication`. |
| `MedicationEditor.kt` line count & fields | [`MedicationEditor.kt#L1-L396`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/MedicationEditor.kt#L1-L396) | **Verified** | Exactly 396 lines. Start date is not asked for; sets `LocalDate.now().toString()`. |
| Existing `ModalBottomSheet` usage | [`PatientListScreen.kt#L381`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt#L381), [`PatientActionsModalSheet.kt#L49-L65`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientActionsModalSheet.kt#L49-L65) | **Verified** | Uses Material 3 `ModalBottomSheet` with `rememberModalBottomSheetState(skipPartiallyExpanded = true)`. |
| Existing date picker dialog | [`AncDatePicker.kt#L43-L60`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt#L43-L60) | **Verified** | `AncDatePickerDialog` wraps Material 3 `DatePickerDialog`. |
| `EditorHooks.kt` route reset | [`EditorHooks.kt#L16-L24`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/state/EditorHooks.kt#L16-L24) | **Verified** | `closeEditor` resets route if `is AncRoute.MedicationEditor`. |
| Repository save dispatch | [`RecordsRepository.kt#L353`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/storage/RecordsRepository.kt#L353) | **Verified** | Single-command dispatch for `SaveMedicationCommand`. |

---

## Architectural Wins (What Works Well)

1. **Route Elimination Solves the Root Cause:**
   Eliminating `AncRoute.MedicationEditor` and managing editor visibility strictly through `medicationDraft != null` directly solves the screen rebuild and lost scroll/search state.
2. **Strict Adherence to Rule of Reusability:**
   Deleting `MedicationEditor.kt` (396 lines) and extracting a single canonical `MedicationForm.kt` strictly enforces the architectural constitution (*"Build Once, Use Everywhere"*, AGENTS.md §1).
3. **Viewport Ergonomics:**
   Collapsing the vertical tile list into an expandable search picker directly prevents 360dp vertical space exhaustion.
4. **Draft Autosave & Lifecycle Preservation:**
   Reusing `medicationDraft`, `EditorHooks.kt`, and the existing draft recovery pipeline preserves unsaved edits during app backgrounding without inventing a new state machine.

---

## Recommendation

**Reject design as written; approve conditional on de-scoping to core navigation goals.**

The design correctly solves the navigation problem, but bundles unacceptable clinical safety hacks (parsing free-text notes for dose history) and speculative scope creep (course length, dual-record replace commands, custom focus chains).

### Recommended Stage 1 Architecture:
1. **Scope to Navigation & Viewport Fixes:**
   * Delete `AncRoute.MedicationEditor` and `MedicationEditor.kt`.
   * Create `MedicationSheet.kt` hosting the single canonical `MedicationForm.kt`.
   * Collapse the tile picker in `MedicationTapToAddSection.kt`.
2. **Prune Speculative Features to Stage 2:**
   * **No dual-record `ReplaceMedicationCommand`**: Dose/frequency edits update the active row in place (Alternative D).
   * **No note-parsing or "Changed" badges**: Defer audit trails until a schema migration can introduce proper relational tracking.
   * **No automated course length**: Keep `isOngoingMedication` signature untouched.
   * **No `FormFocusChain` library**: Use standard Compose `FocusRequester` and `KeyboardActions` directly in `MedicationForm`.

This reduces the Level-3 blast radius exclusively to `AncState.kt` and `EditorHooks.kt`, completely removing risky modifications from `ClinicalCommands.kt`, `RecordsRepository.kt`, `UndoCoordinator.kt`, and `ClinicalModels.kt`.
