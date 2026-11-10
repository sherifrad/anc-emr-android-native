## Code Review: Medication Section Revision — Phase 2 (r2)

### Blocker

1. **Save-failure context can be destroyed before it is seen — `AncState.kt:397-400` (`withMedicationStopUiKept`).**
   The failed one-tap stop lives in `medicationQuickSave` and is wiped by *any* `startsAnotherAction()` event — including `SelectSection`. Scenario: the doctor taps Stop; the save fails; before she reads the bar she swipes to another tab (does not leave the patient). The failure bar, `savePhase` and `saveError` are silently cleared; the drug stays Active with no indication anywhere that the stop did not happen. Design §4 and CUJ 1 require the cause to be shown; there is no persisted failure, no notice on return. For a clinical action whose recommended dismissal path is "open the row and fix it", silently vanishing failures are unacceptable at this severity.

### Major

2. **Cancel never prompts, contradicting the approved mock decision — `AncState.kt:482-493` (`requestBack`).**
   The approved design says "Cancel prompts only after the doctor edits." Here `event == AncEvent.CancelEditor` closes at once regardless of `touched`. This is only safe if `MedicationSheet` pre-prompts before sending `CancelEditor` — that dialog is not in this patch, and the reducer *forbids* the existing generic discard path from ever firing for Cancel. If the sheet omits its own prompt, a doctor's edits are destroyed with one tap. Also note `PendingAction.Navigate(state.route)` on the Back-discard path navigates to the *same* route — verify that does not rebuild the pager and reset scroll state.

3. **App-level delete-Undo bar hangs on the sheet's compositing — `AncApp.kt:637-639`, `PatientHubScreen.kt:1127-1137`, `MedicationSheet.kt:1298-1303`.**
   When a medication draft is open, `UndoBarHost` is removed from the window and re-hosted as the sheet footer, inside the scrolling/dAnimating sheet. Scenario: a delete-Undo is counting its 8s window from the Meds list, then the doctor opens the edit sheet (allowed for a *different* medication, `AncState.kt:268`). The bar teleports into the sheet, can be scrolled or clipped away, and goes fully uncomposed during the sheet's dismissal animation while the countdown keeps running — undo can expire invisibly and the deletion becomes permanent with no chance to act.

4. **"Save new medication to my list" and form presets silently deleted — `MedicationEditor.kt` (deleted), `strings.xml:1649, 1657`.**
   With the full-screen editor go: the "add to my medication list" codex banner (the only way to grow the personal formulary from the med flow), and the in-form "Common medications"/"Patient's previous prescriptions" preset chips. Neither removal is in the design's field list, Non-Goals, or "What is deleted" (§8) — only `dispatchMedicationDraft`, the route and the *screen* were slated for removal. This is a silent feature regression outside the approved scope.

5. **One-tap stop reports the wrong cause — `AncState.kt:204-210`.**
   A validation-caused rejection of a quick save yields the generic `SaveUiError.CONFIRMED_NOT_SAVED` ("could not be confirmed") rather than the validation cause other editors show next to fields. Scenario: stop a stored row whose frequency no longer passes the save rules → bar says "not confirmed / could not be verified"; the doctor retries the identical save forever with no hint that opening the row to fix a field is the resolution. Design §7 explicitly promises the existing cause messages.

6. **Verify `hasDirtyDraft` excludes `medicationQuickSave.draft` — `AncState.kt:313-314`, `346-385`.**
   `canQuickSaveMedication()` requires `!hasDirtyDraft && medicationDraft == null` while `stopMedication` seeds the dirty quick-save draft into `medicationQuickSave`. If `hasDirtyDraft` counts it, one-tap stop can *never* fire a second time; if it doesn't, the failed quick-save is an invisible dirty state that `startMedicationEditor`'s `hasDirtyDraft` guard also passes over. The design and unit test list assume the first reading; this must be pinned by a test (`MedicationSheetStateTest` covers "start is ignored while a save runs", not this).

### Minor

7. **Archived-patient UI guard weakened — `MedsTab.kt:1583-1584, 1597-1598`.** `patient.status != "archived"` was replaced by bare `canEdit`; safe only if PatientHubScreen's `canEdit` is already false for archived patients. The reducer re-checks, but the picker/archive row can still be offered.

8. **Disabled visual inconsistency — `MedicationTapToAddSection.kt:1432`.** `alpha(if (enabled || isActive) 1f else 0.38f)`: an *active* tile during a save or with a draft open shows full opacity yet is unclickable — no disabled affordance; TalkBack reads it neither as disabled nor with a reason (stateDescription only covers "already on list").

9. **A11y: `MedicationStopBar.kt:1741-1742`** — the `LinearProgressIndicator` has no contentDescription or live region; TalkBack users get no signal that a stop save is running or finished. The failure/undo bars do this correctly (`liveRegion`), the progress line does not.

10. **Suspected dead/mis-deleted strings — `strings.xml:1665-1666, 1642`.** `medication` and `medication_dose_and_schedule` were deleted with the editor, but the AGENTS.md cross-component rule (clinical summary/print use shared helpers) makes it plausible other call sites use `R.string.medication`; if so this breaks compilation. `medication_editor_notice` is now probably unreferenced. Verify both.

11. **`MedicationTapToAddSection.kt:1322, 1413`** — `LocalContext` import survives the Toast removal (unused), and `String.format(alreadyOnListFormat, …)` uses the default locale instead of formatting via `stringResource`/`String.format(Locale, …)`.

12. **Sheet status toggling loses a stored stop date — `AncState.kt:466-473`.** Editing a stopped row, toggling Active then Stopped again stamps *today* over the original stop date; with in-place edits and no history (documented Stage-2 gap) that original date is unrecoverable. A touched-aware warning would be appropriate.

**Not defects, for the record:** the collapsed "Add medication" picker, "Show past" toggle and Enter-to-next/focus work are absent from this patch — they are Phase 3 scope per the plan. No hard-coded colours found; touch targets use `minimumTouchTarget()`; the monotonic-clock stamping for the 10s undo window (`AncViewModel.kt:591` + `MedicationStopBar` clock injection) is consistent.
