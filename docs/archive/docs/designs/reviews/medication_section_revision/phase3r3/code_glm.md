# Independent Code Review — Meds Tab Stage 1 (phase3r3)

## Blocker

**B1. Opening a medication card hides the clinical note entirely.**
`MedicationCard.kt` (patch lines 184–195): notes render only under `if (notes.isNotEmpty() && !isExpanded)`, and the expanded section (patch lines 265–273) contains FactRows for dosage, route, frequency, start date and adverse effects — but **no notes row**. The component's own KDoc (patch 79–81) and inline comment (patch 184–185) both claim "the whole note … shows when the row is opened". Failing scenario: a row carries "hold if SBP < 100"; the doctor taps the row to review it before refilling — the note is visible while the card is closed and **vanishes the moment it is expanded**. The old code showed the full note when expanded. Clinical safety information is hidden exactly where the doctor looks for it; the implementation contradicts the documented contract.

## Major

**M1. Double-tap on Stop dispatches two commands; the repeat is not recognized.**
`MedsTab.kt` (patch line 661): `onStop` generates a **fresh `UUID.randomUUID()` operation id per tap**. The design says the operation id exists "so a repeat is recognized" — but two taps are two *different* ids, so dedupe by operation id cannot recognize them. `StopControl` becomes `enabled=false` only after the reducer round-trips the save phase; a fast double-tap fires two `StopMedication` events carrying the same base revision. Outcome: a spurious "changed since opened" failure on the second save (with no sheet to show it in), and two stop entries in the save history where the design promises exactly two (stop + undo). The disabled state must be applied at dispatch (e.g. ignore a start/stop while the previous command is unsettled), not only via recomposed `enabled`.

**M2. Name autofocus on "Save and add another" can silently fail.**
`MedicationForm.kt` (patch lines 348–354): `LaunchedEffect(draftKey, focusNameOnStart)` waits a single `withFrameNanos { }` then calls `nameFocus.requestFocus()`. The new draft composes inside a `ModalBottomSheet` that is still running its enter animation; after one frame the field may not yet be focusable/attached, `requestFocus` returns false, the effect then re-keys to `focusNameOnStart=false` (because `onNameFocused()` already set `nameFocusedFor = draftKey`) and never retries. Failing scenario: doctor taps "Save and add another" — the sheet opens blank with **no keyboard**, defeating the design goal "the sheet opens blank for the next drug" with the keyboard up (CUJ 2). The return value of `requestFocus` must be checked and retried, or focus requested after layout.

**M3. "Already active" tile lock can block a legitimately different preparation.**
`MedicationTapToAddSection.kt` (patch lines 1037, 1074, 1120): the active check is name-keyed (`activeMedicationNameKeys`, `tile.isAlreadyActive(activeNames)`), yet the design itself states "a different administration route … is a separate drug and not a variant". Failing scenario: patient is on oral ferrous sulfate; the doctor needs IM iron — the tile shows the check mark and "Already on today's list" and **cannot be opened at all**. Unless `activeMedicationNameKeys` keys on name+route (not visible here), Stage 1 already violates the route-separateness rule the design "recorded now and applied". Verify or key the check on name+route+form.

## Minor

1. **Disabled states are invisible to TalkBack.** `MedicationTapToAddSection.kt` (patch 947, 1119) conveys disabled with `alpha(0.38f)` only; `clickable(enabled=false)` adds no semantics. Add `semantics { disabled() }`.
2. **Duplicated disabled-alpha constant.** `DISABLED_CONTENT_ALPHA = 0.38f` (MedicationCard.kt, patch 313) and `DISABLED_TILE_ALPHA = 0.38f` (MedicationTapToAddSection.kt, patch 883) — two files, one magic number; extract one shared token alongside the theme.
3. **Chip labels clipped in long locales.** `FilterChip` labels use `maxLines=1, softWrap=false` (patch 1023, 1030); Arabic/"Recent" translations truncate without ellipsis at 360dp.
4. **Button label interpolated into a sentence.** Patch 1049–1056: `medication_picker_no_match` receives `prescribe_medication` (a button label) as an argument — grammatical mismatches in translated strings.
5. **UI state lost on rotation.** `searchFocused` (patch 993) and `focusOtherBox` (MedicationForm, existing) are plain `remember`; after rotation the "Other frequency" box is not refocused and the picker's bring-into-view effect does not re-arm. `confirmDiscard`, `nameFocusedFor`, picker state, `showPast` and `expandedMedId` are correctly saveable.
6. **Redundant empty messaging / unreachable primary action.** `MedsTab.kt`: the header always renders "Active Prescriptions (0)" above `MedsEmptyState` (double empty state), and when only stopped drugs exist the header's Prescribe button is hidden (`patient.medications.isNotEmpty()` guard, patch 559) while the only add entry point is the collapsed picker further down.
7. **Stale comment/magic numbers.** Patch 885–889: "five tiles (56 dp each)" — tiles are two-line (titleMedium + bodySmall + padding), taller than 56dp; `PICKER_LIST_MAX_HEIGHT` shows fewer tiles than claimed. `Rect(0f, 0f, 1f, firstRowPx)` (patch 999) uses a 1px-wide rect — works but fragile.
8. **Repository access inside a composable.** `ClinicalCatalogRepository.getInstance(context)` in `PickerPanel` (patch 985–988) keeps data access in the UI layer; acceptable only if it matches the pre-existing picker pattern, but it is a drip away from the "no business logic in composables" rule.

## Compliant (verified)

Hard-coded "Active Prescriptions (N)" / "Prescribe" / "No records" / "Discontinued / Past Prescriptions (N)" all moved to plurals/strings; all colours are `MaterialTheme` tokens; toggle, stop, picker and tile targets use `minimumTouchTarget()`; start/end padding (no left/right) keeps RTL safe; the `rememberUpdatedState` pager fix in `PatientHubScreen.kt` correctly repairs the lost first-tab report; stopped rows correctly omit the stop control.

**Verdict:** fix B1 and M1–M3 before accept; B1 alone is a clinical-safety regression.
