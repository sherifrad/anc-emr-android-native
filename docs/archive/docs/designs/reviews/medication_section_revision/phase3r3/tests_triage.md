# Triage of the alphacode test review (phase3r3)

Reviewer: alphacode lane, 2026-11-11. It saw only `MedsTabSheetTest.kt` and `MedicationFormTest.kt`.
Triaged by zcode (GLM-5.3) against the files it did not receive. A finding is acted on only with new evidence.

## Rejected (covered elsewhere)

1. "Route never changes at every step" (its Blocker 1): the reducer sets the route, and
   `MedicationSheetStateTest.startingADraftNeverChangesTheRoute` covers add, tile-add, edit and a start
   from another tab; `cancelCloses...` and `backOnAnUntouchedDraft...` assert the route after close.
   A per-step recorder at UI level re-tests a reducer invariant.
2. "Draft recovery reopens the sheet" (its Blocker 2): `MedicationSheetStateTest.recoveringAnAddOrEditDraftReopensTheSheetOnTheTab`
   (reducer) and `MedsTabRestorationTest.thePickerAndShowPastSurviveRecreation` (recomposition) cover it.
   The reviewer did not receive these files.
3. "Search text and Show past survive stop and edit" (its Blocker 3): false.
   `thePickerStaysOpenWithItsSearchThroughAddStopEditAndTabSwipes` and `showPastStaysOpenThroughStopAddEditAndTabSwipes`
   assert exactly that through add, stop, edit and tab swipes.
4. "Stale revision causes the failure" (its Blocker 5): the conflict path is tested
   (`aFailedStopLeavesTheRowActiveAndShowsTheCause`, STALE_OR_CONFLICT); the revision check itself is in
   protected storage code outside Stage 1 scope.

## Noted, no Stage 1 action (recorded as small follow-ups)

- Entry points disabled during an *edit* save (its Blocker 4): stop and add saves exercise the same shared
  disabled flag through the same composition; an edit-save variant adds little. Kept as a small follow-up.
- Date picker dialog integration, "Save and add another" after a failed first save, picker height with
  keyboard, SavePhase transition coverage: state-level tests already cover undo expiry
  (`theUndoBarCountsFromTheCommitTimeTheEventCarries`), undo drop on other actions, and the UNRESOLVED rule
  (`unresolvedSaveKeepsMedicationEditingBlocked`). Tab swipe under an open sheet is ModalBottomSheet platform
  behavior (modal scrim). Each is a reasonable later addition; none hides a Stage 1 defect.
