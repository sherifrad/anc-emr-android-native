# Code Review: Medication Section Revision (Stage 1)

## Blocker

**1. Business logic in composable — `MedicationStopBar.kt:1636–1639`**  
The 10-second undo auto-dismiss is driven by a `LaunchedEffect` timer inside the UI. This violates unidirectional MVI (reducer owns state transitions), loses the remaining time on rotation/recomposition (fresh 10 s after config change), and makes the timeout untestable without UI. The design states “the bar is dropped when the doctor starts any other action or leaves the patient” — action-driven dismissal is in `withMedicationStopUndoKept`, but time-driven dismissal must also live in the reducer (e.g., a `MedicationStopUndoTick` event from a ViewModel timer) or be derived from a timestamp in state.

**2. Top-level `DEFAULT_MEDICATION_ROUTE` depends on `ClinicalVocabulary` init order — `AncState.kt:88`**  
`val DEFAULT_MEDICATION_ROUTE = ClinicalVocabulary.medicationRoutes.first()` executes at class-load time. If `ClinicalVocabulary` is not an `object` or is lazily initialised, this throws. Convert to a `get()` property or a function.

**3. Conditional composition drops `UndoBarHost` state — `PatientHubScreen.kt:1077`**  
`if (state.medicationSheetDraft() == null) UndoBarHost(...)` removes the host from composition when the sheet opens. Any internal state (animations, timers, scroll) in `UndoBarHost` is lost and recreated when the sheet closes. The design says “the sheet also hosts the app’s Undo bar so the bar is never hidden under the modal sheet” — keep `UndoBarHost` always composed and control its *visibility* (e.g., `AnimatedVisibility`) instead of its presence in the tree.

**4. Undo timer resets on rotation — `MedicationStopBar.kt:1636–1639`**  
`LaunchedEffect(undo.token) { delay(10_000); send(Expired) }` restarts the full 10 s after every configuration change because the effect re-runs in a new composition. The remaining time must be stored in state (e.g., `undoExpiresAt: Long`) and the reducer must emit the expiry event when that time passes.

---

## Major

**5. Hard-coded disabled alpha — `MedicationTapToAddSection.kt:1261`**  
`private const val DISABLED_TILE_ALPHA = 0.38f` — Material 3 uses `ContentAlpha.disabled` (0.38) but it should be referenced via `LocalContentAlpha` or `MaterialTheme.colorScheme` tokens, not a raw literal.

**6. Incorrect disabled tint colour — `DeleteUndoUi.kt:613`, `MedicationCard.kt:1112,1131`**  
`tint = if (enabled) ... else LocalContentColor.current` uses the *current* content colour (which may be primary/secondary/onSurface) instead of the spec’d disabled content colour (`onSurfaceVariant` or `ContentAlpha.disabled` applied to the enabled colour). Use `MaterialTheme.colorScheme.onSurfaceVariant` or `IconButtonDefaults.iconColor(enabled)`.

**7. Missing `stateDescription` for disabled tiles — `MedicationTapToAddSection.kt:1316`**  
`semantics { if (isActive) stateDescription = alreadyOnList }` — a disabled but *inactive* tile announces no state. Add `stateDescription = stringResource(R.string.disabled)` (or similar) when `!tileEnabled`.

**8. `MedicationSheet` 360 dp / keyboard constraints not verifiable**  
The patch only shows the `footer` addition. The design requires: “At 360dp the sheet is limited to the screen height minus the keyboard. The form scrolls inside the sheet and the Save button stays visible.” No `imePadding`, `heightIn`, or `bringIntoViewOnFocus` logic is visible in the diff.

**9. `MedicationForm` Enter-to-next / focus handling not verifiable**  
The patch changes only a route constant. The design mandates “each field [has] Next keyboard action, the last one Done, and moves focus with standard Compose focus requesters … the focused field is brought into view.” The full `MedicationForm.kt` must be inspected for `KeyboardActions(onNext = …)`, `FocusRequester`, and `BringIntoViewRequester`.

**10. “Show past” toggle implementation absent**  
Design: “Past drugs sit behind a ‘Show past’ toggle. The tab is no longer rebuilt, so the toggle keeps its state.” `MedsTab.kt` is truncated in the diff; the toggle and its `rememberSaveable` state are not shown.

**11. Collapsed picker state not shown**  
Design: “One collapsed ‘Add medication’ picker with search replaces the always-open tile list.” `MedicationTapToAddSection.kt` receives `enabled` but no `expanded`/`collapsed` state or animation is visible in the diff.

**12. Two Undo bars coexist**  
`PatientHubScreen.kt:1073` places `UndoBarHost` in the sheet’s `footer`; `MedsTab.kt:1425` renders `MedicationStopBar` at the bottom of the tab. The design says “the sheet also hosts the app’s Undo bar” (singular). Clarify whether the general undo bar should move into the sheet *only* while the sheet is open, or whether `MedicationStopBar` replaces it for stop-specific undo.

---

## Minor

**13. `LinearProgressIndicator` lacks accessibility label — `MedicationStopBar.kt:1607`**  
Add `contentDescription = stringResource(R.string.medication_saving)` or wrap in a labelled container.

**14. `today()` called multiple times per reducer step — `AncState.kt:257,300,332,432`**  
Each call evaluates `LocalDate.now()`. In a single synchronous reducer step this is benign, but if the step ever becomes async or is split, dates could drift. Capture `val now = today()` once at the top of each private function.

**15. Alpha on disabled tiles may fail contrast — `MedicationTapToAddSection.kt:1317`**  
`.alpha(if (enabled || isActive) 1f else 0.38f)` reduces text/icon contrast. Material 3 recommends keeping text at full opacity and only reducing container/background opacity for disabled state, or using `ContentAlpha.disabled` via `LocalContentAlpha`.

**16. Removed strings may have lingering references — `strings.xml`**  
`medication_editor`, `common_medications`, `patient_previous_meds`, `medication`, `medication_dose_and_schedule` are deleted. Grep the codebase for `@string/` or `stringResource(R.string.…)` references before merge.

**17. `MedsTab.kt` shows disabled UI for archived patients**  
Lines 1456/1470 changed `if (canEdit && patient.status != "archived")` → `if (canEdit)`. `canEdit` is `false` for archived patients (set in `PatientHubScreen`), so the picker and “Prescribe” button render disabled rather than hidden. Confirm this is intentional (design does not specify).

---

## Summary of Required Fixes (Priority Order)

| # | File | Fix |
|---|------|-----|
| 1 | `MedicationStopBar.kt` | Move 10 s timer to reducer/ViewModel; store `undoExpiresAt` in state; emit `MedicationStopUndoExpired` from reducer when time passes. |
| 2 | `AncState.kt:88` | Make `DEFAULT_MEDICATION_ROUTE` a `get()` property or function. |
| 3 | `PatientHubScreen.kt:1077` | Replace conditional composition with `AnimatedVisibility(visible = medicationSheetDraft() == null)` around `UndoBarHost`. |
| 4 | `MedicationStopBar.kt` | Read `undoExpiresAt` from state; show remaining time or let reducer drive expiry. |
| 5 | `MedicationTapToAddSection.kt:1261` | Replace `0.38f` with `ContentAlpha.disabled` via `LocalContentAlpha` or `MaterialTheme`. |
| 6 | `DeleteUndoUi.kt`, `MedicationCard.kt` | Use `IconButtonDefaults.iconColor(enabled)` or `onSurfaceVariant` for disabled tint. |
| 7 | `MedicationTapToAddSection.kt:1316` | Add `stateDescription` for disabled inactive tiles. |
| 8 | `MedicationSheet.kt` | Add `imePadding`, height constraint (`heightIn(max = …)`), `bringIntoViewOnFocus` for 360 dp + keyboard. |
| 9 | `MedicationForm.kt` | Verify/implement `KeyboardActions(onNext/onDone)`, `FocusRequester` per field, `BringIntoViewRequester`. |
|10| `MedsTab.kt` | Implement “Show past” toggle with `rememberSaveable`; implement collapsed picker state. |
|11| `MedicationSheet.kt` / `MedsTab.kt` | Decide on single Undo bar location; remove duplicate. |
|12| `MedicationStopBar.kt:1607` | Add `contentDescription` to progress indicator. |
|13| `strings.xml` | Verify no deleted string keys are still referenced. |

---

## Files Exceeding 1 500 Lines (AGENTS.md §2)

`AncState.kt` grows past 2 100 lines in this patch. While the rule cites “UI files,” `AncState.kt` is a core state/reducer file (level-3) and should be split — e.g., extract medication draft logic, undo logic, and route logic into separate files — to stay maintainable and reviewable.
[Tokens] upload: 49126 download: 10478 cache_read: 0 cache_write: 0

