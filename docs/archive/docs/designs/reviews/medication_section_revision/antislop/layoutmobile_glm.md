# Review: antislop-layoutmobile (added lines only)

## Major

**1. Frequency chips have zero vertical spacing between wrapped rows** — `MedicationForm.kt` (new file), in the frequency `FlowRow`:

> `verticalArrangement = Arrangement.spacedBy(0.dp),`

**Rule:** Targets Too Close ("44px targets packed together with no gap, so a thumb tap hits the wrong one").
**Why it's real:** five frequency chips on a 360 dp screen will wrap to two or three rows; `minimumTouchTarget()` extends each 32 dp chip's hit area to 48 dp, so with `0.dp` between rows the expanded touch areas of vertically adjacent chips overlap. A thumb tap on a wrapped second row can land on the chip above it — changing the medication frequency silently.
**Fix:** set `verticalArrangement = Arrangement.spacedBy(8.dp)` (matching the horizontal gap) so wrapped chip rows keep a clear gap between hit areas.

## Minor

**2. Picker source chips use `softWrap = false`, which can clip or overflow** — `MedicationTapToAddSection.kt`, both added chips:

> `label = { Text(stringResource(R.string.tab_common), maxLines = 1, softWrap = false) },`
> `label = { Text(stringResource(R.string.tab_recent), maxLines = 1, softWrap = false) },`

**Rule:** Horizontal Scroll Leak / Overflow Hidden Clipping — text sized so it can exceed its container at a narrow width does not reflow.
**Why:** with `softWrap = false` the label never wraps; at large font scale (or a longer translation) the two chips in a fixed 8 dp-spaced `Row` can push past the viewport edge and get clipped, making the source toggle unreadable. (This is carried over from the old code, but the lines are added here.)
**Fix:** drop `softWrap = false` (keep `maxLines = 1` with ellipsis, or let the label wrap) so the chips shrink/wrap instead of spilling.

## Checked and clean

- Tap targets: all new interactive controls (`medFreqChoice-*`, `medTab-*`, `stopMedication-*`, `medStopUndo`, dialog buttons, `medPickerToggle`, `medShowPastToggle`, `medEmptyPrescribe`, `addCustomMedicationBtn`) carry `minimumTouchTarget()` or are full-width buttons — no under-sized targets found.
- Reflow: the Meds tab is a `Column` with `imePadding()` and the stop bar placed in-flow below the list (not a fixed overlay), so nothing is covered; the picker list uses a deliberate, internally scrolling `heightIn(max = 304.dp)` rather than a rigid grid.
- Fixed-width children: the card and header use `weight(1f)` / `FlowRow` so status and actions wrap instead of colliding at 360 dp; the header now stacks name over GA badge with `fillMaxWidth()` — no fixed-px widths on content.
- Overflow: removed `maxLines`/ellipsis on the patient name lets it wrap instead of clip; allergy pill keeps a deliberate one-line ellipsis.
- Strings and comments: no em dashes in user-visible text; no misleading strings spotted (the "at least 4.5:1 in both themes" contrast claim in the `StopControl` KDoc and the "labels wrap rather than clip" note on `MedicationSheetButtons` match their code as written); comments are specific, not filler.
