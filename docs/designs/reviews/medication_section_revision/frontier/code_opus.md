I'll read the design doc and the patch first.
Patch read in full. Next I'll check a few claims against the working tree: the Compose `requestFocus` return type, translated `values-*` files, removed strings, and the card's expanded section.
# Code review: `main.patch` (medication section, Stage 1)

I read the whole patch (1–1761), the design (§1–8 and Detailed Implementation 1–15), and checked some claims against the working tree. No files were edited. There is nothing to carry out, so I skipped the plan artifact. `tests.patch` was not in scope.

**Result: no Blockers, 4 Major and 9 Minor findings. Fix the Majors before accepting the patch.**

## Blocker

None found.

## Major

**M1. A drug that is already active can be prescribed a second time when the stored route differs from the tile's route.**
- Where: `main.patch:1268-1274` (`medicationNameRouteKey`, `activeMedicationKeys`, `isAlreadyActive`).
- The "already active" check now matches on name plus route instead of name only. Rows with a blank route are expected: `MedicationForm` shows `values.route.ifBlank { DEFAULT_MEDICATION_ROUTE }` in `RouteReadOnly`.
- Failing scenario:
  1. The patient has an active "Ferrous sulfate" row with a blank route.
  2. The catalog or custom tile "Ferrous sulfate" has route "Oral".
  3. The keys `ferrous sulfate|` and `ferrous sulfate|oral` differ, so the tile shows no check mark and stays tappable.
  4. Tapping it opens the sheet, and Save creates a second active prescription.
- The same happens with any spelling difference in the route, such as "PO" vs "Oral".
- Design conflict: §5 says the check is by drug. Non-Goals say the "different route is a different drug" rule is "applied in Stage 2". This applies a Stage 2 rule in Stage 1 with a weaker key.

**M2. The first field in error is not scrolled into view on a second rejected Save.**
- Where: `main.patch:664-672` (`revealWhen`, `LaunchedEffect(reveal)`), used at 589, 601, 629, 641 and 649.
- The effect only fires when `reveal` goes from false to true. A field change clears the errors (`MedicationReducer.kt:180`), but a repeat Save without changes sets the same error set again (`AncReducer.kt:106`), so `revealFrequency` stays true and nothing fires.
- Failing scenario:
  1. A tile with no frequency is opened and Save is tapped.
  2. The form scrolls to the frequency error.
  3. The doctor scrolls up to check the name and taps the always-visible Save again.
  4. The form does not scroll, which breaks design §7 ("After a rejected save the sheet scrolls").
- Save failures do re-fire, because `savePhase` passes through PENDING; the field errors do not. Key the effect on a counter that goes up on every rejection.

**M3. Long patient names are still cut off at 360dp.**
- Where: `main.patch:125-135` (`PatientHubHeader`).
- The patch edits this `Text` but keeps `maxLines = 2` and `overflow = TextOverflow.Ellipsis`.
- Failing scenario: a three-part name at font scale 1.3 or higher on a 360dp phone is cut with "…".
- This breaks the AGENTS.md §4 rule that the patient name is never truncated. Freeing the full row width (moving the GA badge below) reduces the problem but does not fix it.

**M4. The rule against a second prescription of an active drug lives only in the UI.**
- Where: `main.patch:987` and `1193` (`clickable(enabled = enabled && !isActive)`).
- Nothing in the reducer or command enforces it. Opening "Prescribe" (`main.patch:1389-1398`) and typing the same name creates a second active row with no warning.
- This conflicts with the AGENTS.md "no business logic in composables" rule. The clinical rule should be checked in state or validation; the tile can still be dimmed.

## Minor

1. **Name focus can fail silently.** `main.patch:555-566`: `onNameFocused()` runs even when both `requestFocus()` attempts return false. After "Save and add another", if the field still isn't attached two frames later, focus never lands and is never retried, so the keyboard drops.
2. **Enter on the last field does not save.** In the current `MedicationForm.kt`, the notes field uses `ImeAction.Default` and nothing uses `Done`. Design §6 says the last field gets `Done`, and CUJ 2 says "presses Enter on the last field" to save; that path does not exist.
3. **The new `imePadding()` on the Meds tab does nothing.** `main.patch:1365-1366`: `AncApp.kt:103` already applies `WindowInsets.safeDrawing`, which includes the keyboard, and uses up those insets. The comment "the keyboard shortens the list" is misleading; remove it or the padding.
4. **The search field can lose focus while typing.** `main.patch:1572` puts the picker inside a `LazyColumn` item. If the doctor scrolls the search field off screen, the item is disposed, focus is lost and the keyboard closes. The picker's inner list scroll position (`main.patch:1020`, `rememberScrollState`) is also not kept.
5. **The closed row shows only one line of notes.** `main.patch:379-386` cuts notes to one line with "…" (it was two). A warning such as "hold if SBP < 100, check pulse first" is cut on the review list at 360dp.
6. **Contrast is claimed, not checked.** `main.patch:477-487` says `colorScheme.error` meets 4.5:1 "in both themes", but nothing tests it, and the dynamic colour in `values-v31` can change it.
7. **Strings are out of date.** `set_dose_in_editor` ("Set dose in editor", `strings.xml:517`) and `already_on_todays_list` ("…today's list", `strings.xml:506`) are still used at `main.patch:1110` and `1184`. The editor and "today's list" no longer exist, so TalkBack reads a concept that is gone.
8. **Two near-duplicate expand rows.** `PickerToggle` (`main.patch:872-909`) and `ShowPastToggle` (`main.patch:1672-1700`) are the same expand/collapse row: state description, click label and decorative chevron. This goes against the one-canonical-component rule; extract one shared row.
9. **A composable modifier factory.** `main.patch:662-673`: `@Composable fun Modifier.revealWhen` holds `remember` and `LaunchedEffect` inside a modifier factory, which Compose lint flags. A `Modifier.Node` or a `LaunchedEffect` at the call site is cleaner.

## Checked and passing

- **48dp hit areas:** the stop control, chips, toggles, tiles and Prescribe all use `minimumTouchTarget` or `heightIn(56.dp)`.
- **Colours:** no hard-coded colours; everything uses `colorScheme`.
- **RTL:** start/end padding and arrangements throughout.
- **Strings:** "Active Prescriptions", "Prescribe" and "No records" are now string resources.
- **State across actions, rotation and swipes:** the picker, "Show past" and the open card use `rememberSaveable(patient.id)`.
- **Pager sync:** the stale-route fix uses `rememberUpdatedState` (`main.patch:211-224`).
- **Frequency rule:** limited to new rows (`main.patch:51`), as the design's Decisions require.
- **File sizes:** all touched UI files are under 360 lines.
