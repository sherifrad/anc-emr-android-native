# Test Review: Medication Section Revision (Stage 1)

**Verdict:** The test patches cover the new frequency validation rule well but miss most Stage 1 behavioral requirements. Critical CUJs (stop-with-undo, scroll/toggle persistence, entry-point disabling, draft recovery, 360dp layout) have **no UI tests**. Several tests are weak (asserting implementation details rather than user-visible behavior).

---

## Blocker — Critical Behaviors With No Test

| # | Design Requirement (CUJ/Section) | Missing Test | Why It Matters |
|---|----------------------------------|--------------|----------------|
| 1 | **CUJ 1: One-tap stop + 10s Undo bar** (Lines 79–83, 141) | No UI test for stop symbol tap, Undo bar appearance, 10s timeout, or Undo tap restoring the row. Only reducer logic tested (`aStoredRowWithoutFrequencyCanBeStoppedUndoneAndEditedThroughTheReducer`). | The stop action is the primary review-time workflow. If the Undo bar doesn't appear, times out wrong, or tap is ignored, the doctor loses data with no recovery. |
| 2 | **Scroll position, search text, "Show past" survive every action** (Lines 28, 149, 339) | No test verifies `LazyColumn` scroll offset, picker search query, or toggle state persist across add/stop/edit/save-fail. | The entire "review-first" goal collapses if the tab rebuilds or state resets. |
| 3 | **All entry points disabled during save/draft** (Line 150) | Only `theSheetButtonsAreDisabledWithProgressWhileADraftSaves` tests sheet-internal buttons. No test for: stop symbols on cards, picker tiles, row edit taps, "Prescribe" button, tab swipes (scrim block). | Silent tap drops during save are explicitly called out as a risk (Line 175). |
| 4 | **Draft recovery reopens sheet on Meds tab** (Line 173, Verification Plan) | Design mandates a test: "recovery of add, stop and edit drafts reopens the sheet on the Meds tab." Absent from patches. | If recovery navigates to the deleted editor route, the refresh bug returns. |
| 5 | **360dp layout with keyboard open** (Lines 36, 131, 176, 339) | No test runs at 360dp with IME visible. `MedicationForm` adds `revealWhen`/`revealOnFocus` but no test asserts the Save button stays visible or fields aren't truncated. | The design calls this a mitigation for a known risk (Line 176). Patient name, GA, BP, high-risk alert readability (Line 36) also untested. |
| 6 | **Modal scrim blocks tab swipes** (Line 126) | No test attempts tab swipe while sheet open. | Sheet orphaned by tab change = lost draft + confused doctor. |

---

## Major — Important Behaviors Partially/Weakly Tested

| # | Design Requirement | Test Gap / Weakness |
|---|--------------------|---------------------|
| 7 | **Enter → next field, field stays visible above keyboard** (Line 154) | `keyboardMovesFocusThroughTheFormAndTheFieldStaysVisible` (Line 1137) adds frequency selection then types in name field, but doesn't simulate `IME_ACTION_NEXT`/`Done` presses or verify each field scrolls into view. The new `revealOnFocus`/`revealWhen` modifiers are untested. |
| 8 | **"Save and add another" keeps focus & keyboard** (Line 339) | Tests at Lines 599, 1138 exercise the flow but assert only sheet state + draft values — not that `IME` stays shown or focus lands on name field. |
| 9 | **Failed save shows `SaveUiError` messages (not just validation)** (Line 159) | Tests only cover `REQUIRED_FREQUENCY` validation error. No test for: `CHANGED_SINCE_OPENED`, `ARCHIVED_PATIENT`, `CROSS_PATIENT`, `STORAGE_ERROR`, `RESULT_UNKNOWN`. The `SaveMessage` composable adds `revealWhen(isFailure)` (Line 146) but no test triggers a real save failure. |
| 10 | **Stop symbol on active row (not in sheet)** (Line 323) | No UI test taps the stop icon on a `MedicationCard`. The one-tap stop is a card action, not a sheet action. |
| 11 | **"Show past" toggle** (Line 149) | No test expands/collapses stopped section or verifies toggle state survives save. |
| 12 | **Collapsed picker: search, height-limited self-scroll, Common/Recent chips** (Line 147) | No test opens picker, types in search, verifies list height constraint, or checks Common/Recent sections. |
| 13 | **Active drug tile shows check mark, doesn't open sheet** (Line 148) | No test. |
| 14 | **Date field label: "Start date" vs "Stop date"** (Line 135) | No test verifies label switches by mode (add/edit active vs stop). |
| 15 | **Route read-only on existing row** (Line 128) | No test verifies route chips disabled in edit mode. |
| 16 | **Cancel prompts only after edit (touched flag)** (Line 117) | No test: untouched Cancel closes instantly; touched Cancel shows discard dialog. |
| 17 | **Back press with draft open → closes sheet, stays in workspace** (Line 122) | No test. |
| 18 | **Active/stopped logic unchanged (regression)** (Line 137) | No test asserts `isOngoingMedication`/`medicationStatusLabel` still agree with header count, summary, print. |

---

## Minor — Gaps in Coverage Depth

| # | Area | Note |
|---|------|------|
| 19 | **Custom tile starts with blank frequency** (Line 148) | `otherChosenWithNoTextIsRejected...` tests the error but not the initial blank state. |
| 20 | **Prescribe button opens blank sheet** (Line 120) | No test. |
| 21 | **Frequency fixed list + custom** (Line 129) | `saveMedicationPrepareAcceptsNewMedicationWithEachFixedFrequencyAndOtherText` covers prepare; no UI test renders the chips. |
| 22 | **Sheet height limited at 360dp, Save button visible** (Line 131) | No test. |
| 23 | **Sheet shows save phase + progress on buttons** (Line 130) | Only `theSheetButtonsAreDisabledWithProgressWhileADraftSaves` covers disabled state; no test for progress indicator visibility. |

---

## Weak / Tautological Asserts

| Test | Issue |
|------|-------|
| `aNewMedicationWithoutFrequencyShowsTheErrorAndKeepsTheSheetOpen` (Line 48) | Asserts `commands.isEmpty()` — implementation detail. Should assert sheet stays open, error visible, **and** frequency field scrolled into view (new `revealWhen` behavior). |
| `theSheetButtonsAreDisabledWithProgressWhileADraftSaves` | Only checks `assertIsDisabled()`; doesn't verify progress spinner or that taps are ignored (not dropped silently). |
| Multiple UI tests | Use `commands.isEmpty()` / `state.medicationDraft == null` as primary assertions instead of user-visible outcomes (sheet visible, list updated, error shown). |

---

## Flaky Timing Risks

| Location | Concern |
|----------|---------|
| `MedicationForm.kt:122` `withFrameNanos { }` in `revealWhen` | Single-frame delay may not elapse on slow CI devices; `bringIntoView` could fire before layout. |
| 10-second Undo bar (Line 141) | No test covers it; any test that did would be slow and flaky. Consider a test hook to accelerate the timeout. |
| `compose.waitForIdle()` + `performScrollTo()` chains | Standard but can timeout under load; tests at Lines 1137, 1333 chain multiple scroll/click/ime actions. |

---

## Summary

| Severity | Count |
|----------|-------|
| Blocker  | 6     |
| Major    | 12    |
| Minor    | 5     |

**The patches implement the frequency validation rule thoroughly (unit + UI) but leave the core Stage 1 UX promises — stop-with-undo, state persistence, entry-point disabling, draft recovery, 360dp keyboard layout — untested at the UI layer.** The reducer tests are solid but don't substitute for instrumented tests that exercise the actual Compose hierarchy, focus system, and navigation-less sheet hosting.

**Recommendation:** Before Phase 2 switch-over, add instrumented tests for Blockers 1–6. Treat Major 7–18 as Phase 3 polish requirements.
[Tokens] upload: 29609 download: 4558 cache_read: 0 cache_write: 0

