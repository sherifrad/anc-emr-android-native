# Goldfish Review: Medication Section Revision Design Document

## Executive Summary

This design proposes a significant refactor of the Meds tab to eliminate full-screen navigation, introduce a bottom-sheet-based inline editing flow, and add several clinical features (course length, change tracking via notes, undo for stop/change). While the high-level goals are sound and address real usability pain points, the design contains **multiple YAGNI violations, premature abstractions, and architectural risks** that increase complexity without proportional Stage 1 value.

---

## Findings

### 1. [Blocker] YAGNI: Course Length Feature is Stage 2 Scope Creep

**Affected Scope:** Goals (line 33), Technical Plan §3 (lines 117-122), CUJ 2/3 implications, `ClinicalModels.kt` changes (line 239), `MedicationForm.kt` (line 309), strings.xml (line 344)

**Rationale:** The design explicitly states "Favourites, dose/frequency variants and the medication settings screen are Stage 2" (line 15), yet **course length** — a new clinical concept with storage implications (computed end date), UI controls (picker with 3/5/7/custom), and cross-cutting logic (`isOngoingMedication` now depends on it) — is fully specced for Stage 1. This is a standalone feature that:
- Adds a new form field with validation
- Changes the active/stopped determination logic everywhere (header count, picker, card, tab)
- Requires new strings, test coverage, and edge-case handling (end date = today, past dates)
- Has no dependency on the core navigation/refactor goal

**Alternative:** Defer course length to Stage 2 alongside variants/settings. For Stage 1, keep start/end date as simple manual fields (today default for start, optional end date). This removes ~5 files' worth of changes and reduces the `isOngoingMedication` signature change blast radius.

---

### 2. [Blocker] YAGNI: "Changed" Mark via Free-Text Notes is a Fragile Abstraction

**Affected Scope:** Goals (line 34), Technical Plan §4 (lines 126-129), `ClinicalModels.kt` (lines 238-239), `MedicationCard.kt` (line 334), `ReplaceMedicationCommand` (line 244)

**Rationale:** Storing clinical audit information ("Changed from X on Y") as an unstructured line in `notes` — explicitly unprotected, editable, and deletable by the doctor — violates clinical safety principles for a level-3 area. The design acknowledges this (line 165: "the 'Changed' mark can disappear if the doctor edits the notes") but accepts it to avoid a schema migration.

**Consequences:**
- The "Changed" mark becomes unreliable UI chrome, not clinical evidence
- Downstream consumers (exports, reports, audits) cannot reliably parse change history
- The `hasMedicationChangeNote` heuristic (line 238) will produce false negatives/positives
- This technical debt will require a schema migration later anyway

**Alternative:** If schema v6 is truly frozen, Stage 1 should **not** implement change tracking at all. Defer to Stage 2 when a proper schema change can accompany it. The dose-change CUJ (CUJ 3) can still work by stopping old + creating new row; just omit the "Changed" mark and note append for now.

---

### 3. [Major] Premature Abstraction: `FormFocusChain` as a Shared Helper

**Affected Scope:** Goals (line 38), Technical Plan §6 (lines 138-142), `FormFocusChain.kt` (line 299), `Components.kt` (line 304)

**Rationale:** The design builds a reusable `FormFocusChain` helper in `ui/components/` "for medications first and as a shared behavior that other screens can adopt" (line 38). However:
- Only `MedicationForm` uses it in Stage 1
- No other screen has been identified as needing it
- The helper introduces focus requester chains, IME action mapping, and bring-into-view logic that may not generalize (different field types, conditional fields, nested scrolling)

**Alternative:** Implement the Enter-to-next behavior **inline in `MedicationForm`** using Compose's `FocusRequester` and `onKeyEvent`/`KeyboardActions` directly. Extract to a shared utility only when a second consumer exists with identical requirements. This follows YAGNI and avoids a premature abstraction that may need rewriting.

---

### 4. [Major] Architectural Risk: `ReplaceMedicationCommand` Dual-Record Transaction

**Affected Scope:** Technical Plan §4 (lines 126-128), `ClinicalCommands.kt` (line 244), `RecordsRepository.kt` (line 249), Verification Plan (line 362)

**Rationale:** The replace command writes two medication records (stop old, create new) in one transaction with a single patient revision increment. This is the "largest change" in level-3 code (line 127). Risks:
- **Atomicity failure modes**: If the second insert fails after the first update, the old row is stopped without a replacement (line 163). The design says "the whole body runs inside the same transaction wrapper" but Room transactions in Android can have partial rollback edge cases with DAO calls.
- **Revision complexity**: Two medication revisions + one patient revision must align. The undo command must track both rows' revisions and content hashes (line 151).
- **Testing burden**: Requires extensive unit tests for stale revisions, partial failures, replayed operation IDs (lines 350-351).

**Alternative:** For Stage 1, consider a simpler model: **dose/frequency edits update the same row in place** (Alternative D, line 185), accepting that history is lost, and defer the stop+create pattern to Stage 2 when it can be accompanied by proper audit fields. If history is non-negotiable, at minimum scope the replace command to **only dose/frequency changes on active rows** (not stops, not name/notes edits) to reduce permutation space.

---

### 5. [Major] Reinventing the Wheel: Custom Date Chip + Picker Instead of Material Date Picker

**Affected Scope:** Goals (line 32), Technical Plan §3 (line 119), `MedicationForm.kt` (line 309: "date chip with Today / 3 days ago / pick-a-date using `AncDatePickerDialog`")

**Rationale:** The design specifies a custom "date chip" with three quick options (Today, 3 days ago, date picker) and a custom `AncDatePickerDialog`. Material 3 provides `DatePickerDialog` and `DateRangePicker` with keyboard support, accessibility, and localization built in. Building a custom chip + dialog:
- Duplicates platform functionality
- Adds maintenance burden for edge cases (locale, talkback, landscape, foldables)
- The "3 days ago" shortcut is a niche optimization not justified by evidence

**Alternative:** Use `MaterialDatePicker` with a custom "Today" button in the header. If "3 days ago" is clinically valuable, add it as a preset chip *outside* the picker that pre-fills the field. Do not build a custom dialog.

---

### 6. [Major] Altitude Drop: Over-Specified Implementation Details in "Detailed Implementation Plan"

**Affected Scope:** Lines 215-356 (entire Detailed Implementation Plan section)

**Rationale:** The document specifies:
- Exact line numbers in `AncState.kt` (lines 53, 357, 1483, 1716)
- Exact DAO method names (`dao.updateMedication`, `dao.incrementPatientRevision`)
- Exact test file names and test case descriptions
- Exact string keys to add/remove
- `FormFocusChain` API shape (focus requesters, IME actions, bring-into-view flag)

This is **implementation-level detail**, not architectural design. It violates the Zoom-Out Standard's Anti-Weeds principle: "High-level sections must focus on architecture, interfaces, and data flows, not code-level implementation details."

**Alternative:** Keep the Detailed Implementation Plan at the component/interface level:
- "Add `ReplaceMedicationCommand` with fields X, Y, Z"
- "Modify `RecordsRepository` to handle replace transaction"
- "Extract `MedicationForm` from `MedicationEditor`"
- "Update `MedsTab` to host sheet and picker"
Move line numbers, exact method signatures, and test case enumerations to a separate implementation task list or engineering notebook.

---

### 7. [Major] Missing Reuse Investigation: `ModalBottomSheet` Pattern Already Exists

**Affected Scope:** Technical Plan §2 (line 111: "ModalBottomSheet (the app already uses one in `PatientListScreen.kt`)"), `MedicationSheet.kt` (line 314)

**Rationale:** The design correctly notes an existing `ModalBottomSheet` in `PatientListScreen.kt` but does not investigate whether that sheet's **infrastructure** (state management, dismissal handling, keyboard padding, save/error UI) can be reused or generalized. Creating a second `MedicationSheet` with its own `MedicationForm`, cancel/save buttons, error display, and `imePadding` handling duplicates the pattern.

**Code Search Needed:** Check if `PatientListScreen.kt`'s sheet uses a shared `BaseBottomSheet` or `EditorBottomSheet` component that could be parameterized for medications. If not, consider extracting one *as part of this work* rather than building a one-off `MedicationSheet`.

---

### 8. [Nit] Premature Optimization: "Quick Actions Run One at a Time" Serialization

**Affected Scope:** Goals (line 37), Technical Plan §5 (line 135: "While the save phase is not idle, tiles are disabled")

**Rationale:** The design adds explicit UI disabling during saves to prevent "two fast taps [failing] the revision check" (line 37). This assumes a contention problem that may not exist in practice (doctor workflow is sequential). The serialization logic adds state (`savePhase` propagation to picker, disabled tiles) and test cases.

**Alternative:** Ship without proactive disabling. If rapid double-taps cause observable revision conflicts in testing, add the guard then. The existing `ClinicalCommandFailure` mapping (line 156) already handles the error case gracefully.

---

### 9. [Nit] Undo Command Duplication: `UndoMedicationChangeCommand` vs Existing Undo Pattern

**Affected Scope:** Technical Plan §8 (lines 148-153), `UndoCoordinator.kt` (line 274)

**Rationale:** The design adds a new `UndoMedicationChangeCommand` (a `RecordUndoCommand`) with custom logic for restoring two rows. The existing undo system already handles `DeleteRecordCommand` and create-undo via `offerSaveUndo`. Adding a third command type increases the undo coordinator's complexity.

**Alternative:** Model the stop/change as a **single logical "replace" record** at the command level, so the existing create-undo (delete the replace record) or update-undo (revert the replace record) patterns apply. This requires the replace command to be structured as a single entity, not two separate DAO operations.

---

### 10. [Nit] Context Insufficiency: No Baseline Metrics for "Review-First" Claim

**Affected Scope:** Goals (line 29), Problem statement (line 22: "In pregnancy follow-ups the doctor mostly reviews the existing list")

**Rationale:** The design claims the screen is "built around adding" and should become "review-first," but provides no telemetry, user study, or observational data to quantify the review vs. add ratio. Without baseline metrics, "review-first" is a subjective label, not a measurable outcome.

**Alternative:** State the design principle as "optimize for the common case of reading/scanning the active list" without claiming a verified majority use case. Add a non-goal: "No analytics instrumentation in Stage 1."

---

## Positive Observations

| Aspect | Why It Works |
|--------|--------------|
| **Route elimination** | Removing `AncRoute.MedicationEditor` and keeping workspace stable directly solves the scroll/search loss problem (Problem #1). Clean architectural win. |
| **Single `MedicationForm`** | Extracting one shared form (Goal line 39) enforces the Rule of Reusability. Deleting `MedicationEditor.kt` (396 lines) removes duplication. |
| **Draft stays, route doesn't** | Leveraging existing `medicationDraft` state without route change is the minimal state mutation to achieve the goal. |
| **Existing undo infrastructure reuse** | `UndoCoordinator.armUndo`, timer, stale-revision retry are reused (line 152), not rewritten. |
| **Schema v6 frozen respected** | No Room migrations proposed; all changes work within current entities. |
| **Explicit non-goals** | Stage 2 scope clearly bounded (lines 43-50). |

---

## Summary Recommendation

**Do not approve as written.** The design conflates three distinct efforts:
1. **Navigation refactor** (remove full-screen editor, add bottom sheet) — **high value, low risk**
2. **Clinical feature additions** (course length, change tracking, undo for stop/change) — **high risk, separable**
3. **Shared infrastructure** (FormFocusChain, date chip, serialization) — **premature**

**Recommended Stage 1 Scope:**
- Remove `AncRoute.MedicationEditor` and `MedicationEditor.kt`
- Add `MedicationSheet` hosting extracted `MedicationForm`
- Collapse picker, add "Show past" toggle
- Keep start/end date as simple fields (today default, optional end)
- Dose/frequency edits update same row in place (defer stop+create to Stage 2)
- No "Changed" mark, no course length, no custom undo command
- Enter-to-next implemented inline in `MedicationForm`
- Serialization guard only if observed in testing

This reduces the level-3 surface area to `AncState.kt` (draft/route), `EditorHooks.kt` (hook), `MedicationForm/Sheet/Tabs` (UI), and tests — avoiding `ClinicalCommands.kt`, `RecordsRepository.kt`, `UndoCoordinator.kt`, and `ClinicalModels.kt` changes entirely for Stage 1.
[Tokens] upload: 20980 download: 3298 cache_read: 0 cache_write: 0

