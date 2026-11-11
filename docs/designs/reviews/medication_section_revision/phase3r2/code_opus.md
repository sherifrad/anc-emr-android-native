I'll read the design and patch first.
# Independent code review: medication section, phase 3 round 2 (`main.patch`)

I found no Blockers, 3 Majors and 9 Minors. The patch compiles in principle: `hideFromAccessibility` is in BOM 2025.12.01, and the tag/string references I grepped are gone. The file-size rule holds: the largest touched file is `PatientHubScreen.kt` at 365 lines. I found no hard-coded colours. I did not edit anything and I did not build or run the tests.

## Blocker

None.

## Major

**M1. Rotation pulls focus back to the name field in a "Save and add another" draft.** Patch lines 420–425 (`MedicationForm.kt:99-104`) and 468.
- `focusNameOnStart = draft.chainedAdd` is true for the whole life of the draft. `chainedAdd` is never cleared (patch line 13, `AncState.kt:646`).
- `LaunchedEffect(draftKey, focusNameOnStart)` runs again whenever the composition is rebuilt, for example after rotation or a dark-mode switch.
- **Scenario:** the doctor taps Save and add another, types a name, moves to Notes and rotates the phone. Focus and the keyboard jump back to Name. This breaks the KDoc promise "focus once for each new draftKey" (`MedicationForm.kt:68`).
- **Fix:** keep a `rememberSaveable(draftKey)` "already focused" flag, or have the form send an event that clears `chainedAdd`.

**M2. The sheet may close and reopen between the two drafts of "Save and add another", and the keyboard may not come back.** `PatientHubScreen.kt:274` shows the sheet only while `if (medicationDraft != null)`.
- `AncViewModel.kt:827-835` sends `StartMedicationEditor` in a second `dispatch`, after the success state has already cleared the draft. If Compose draws a frame between the two, the `ModalBottomSheet` leaves composition and opens again in a new dialog window.
- The focus effect waits only one frame (patch line 422). That is often before the new window has focus, so `requestFocus()` succeeds but the keyboard stays down.
- The patch comment (lines 466–467) admits the sheet may be "shown again". That is exactly the flicker the design's mitigation was meant to remove. The design says to derive the sheet's visibility from the draft "so it stays composed" (design line 171). That fix was not applied.

**M3. Notes and adverse effects are hidden while a row is closed.** Patch lines 164–166 and 176–179 remove the two-line notes preview.
- Notes now appear only in the expanded `FactRow` (`MedicationCard.kt:167`).
- **Scenario:** a row whose note says "hold if SBP < 100" shows only "dose · route · frequency" when the doctor reviews the list.
- The design (item 14) only requires that name, dose and frequency are never truncated. Removing the notes is a change to what clinicians see that nobody has signed off.
- **Fix:** keep a one-line notes preview, or a notes marker, or get the author to accept the change.

## Minor

1. **The "Active" mark on picker tiles is a second status badge** (patch lines 905–918). The tile builds its own check icon plus `medication_status_active` text instead of using the existing `MedicationStatusBadge` (line 282). This breaks the one-canonical-component rule.

2. **Three hand-written expand/collapse rows.** The card toggle (209–216), `PickerToggle` (585–621) and `ShowPastToggle` (1329–1356) repeat the same semantics code (`stateDescription`, `Role.Button`, `onClickLabel`, chevron). Extract one shared disclosure-row component.

3. **The disabled-transparency constant is duplicated.** `DISABLED_CONTENT_ALPHA` (387) has the same value as `DISABLED_TILE_ALPHA` (529) in another file. The disabled styling is also inconsistent:
   - Stop uses `onSurface` at 0.38 (363).
   - Edit uses `LocalContentColor.current` (328).

4. **Route and non-standard frequencies are not translated.** Patch lines 72–73 and 80–86 show the stored `route` raw, for example "Oral". A frequency stored with different case or spacing ("once daily ") gets no label and is shown in English. Translating only the five fixed frequencies gives mixed-language rows in RTL.

5. **A format argument is passed to a string with no placeholder** (patch lines 697–704). `no_recent_medications` (`strings.xml:516`) has no `%1$s`, yet it receives `prescribe_medication`. The empty-Recent message therefore does not point to "Prescribe", unlike the no-match message. Lint may also flag it.

6. **When the actions wrap, they land on the start side** (patch lines 273–281). `FlowRow` with `SpaceBetween` puts a wrapped item on the start side. With large fonts or a long translation of "Stop", the Stop, Delete and Edit buttons move under the badge on the start side. The comment says "actions on the end side". The same happens with "Prescribe" in the header (1103–1108).

7. **Picker results can sit under the keyboard at 360dp.** The open picker stacks a 56dp toggle, the search field, the chips and a list of up to 304dp (patch lines 532, 713). It is inside the outer `LazyColumn`, and nothing brings the results into view.
   - **Scenario:** on a 360×640 screen with the keyboard open, the doctor types in `medSearchField` (637–644). The filtered tiles are hidden under the keyboard, so she has to close it to pick one.
   - The search field also has no `ImeAction.Search` or Done handling.

8. **Every catalogue tile is composed at once** (patch lines 710–717, 741). The list is a `Column` with `verticalScroll` and `forEach`, so a large catalogue builds all its tiles on every query change. It worked the same way before; a bounded `LazyColumn` or a fixed result limit would avoid this.

9. **The tab-sync effect only partly fixes stale values** (patch lines 108–119). `rememberUpdatedState(route)` fixes the stale route. `onEvent` inside `LaunchedEffect(pagerState)` is still captured once. That is harmless only while the callback stays the same object, so wrap it in `rememberUpdatedState` as well.

## Checked and found compliant

- **Rotation and tab swipes:** the picker state, "Show past" and the open card are saved per patient with `rememberSaveable(patient.id)`, and the state sits above the `LazyColumn` (1055–1057).
- **List keys:** the `LazyColumn` items have stable keys (1101, 1163, 1196, 1229, 1266).
- **Touch targets:** the Stop control is at least 48dp, has a content description, and its "Stop" label is hidden from screen readers (358–384). The "Prescribe" button keeps `minimumTouchTarget` (`MedsTab.kt:179`).
- **RTL:** the layout uses `start`/`end` padding and an "Expanded"/"Collapsed" state description.
- **Strings:** the strings that were hard-coded before ("Prescribe", "Active Prescriptions", "No records") are now string resources and plurals (1112, 1142, 1198).
- **One name rule:** "the same drug name" is decided by one helper, `medicationNameKey` (950), used by both the Recent filter and the already-active check.
- **No database change:** the reducer adds only `chainedAdd`, and it is not part of `isDirty` (patch line 35 and line 15).
