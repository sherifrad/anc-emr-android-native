## Slice 15 read-only review (UltrasoundEditor, UltrasoundPhotoViewerDialog, ZoomableImage)

**Verdict: 1 real defect found (behavior parity regression).**

### Defect
**`app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundPhotoViewerDialog.kt:1424-1433` — Photo pager dots are no longer tappable to jump to a specific image (behavior parity regression)**

- **Evidence (diff):** The previous implementation made each dot indicator interactive:
  - Removed: `.clickable { currentIndex = idx }` on the per-photo dot `Box` (old dot block, around removed hunk ending near prior line ~1229 in the replaced section).
  - Added/current: dots are rendered as non-interactive `Box` elements with only `size`/`clip`/`background` modifiers, and the row is wrapped in `Modifier.clearAndSetSemantics {}` (new `1422-1433`). Navigation is now only possible via the previous/next `IconButton`s (`1395-1451`).
- **Concrete failure scenario:** A scan with multiple attached photos (e.g. 4 images) is opened in the photo viewer on photo 1. The user taps the 4th dot to jump directly to photo 4 (previously supported). Nothing happens because the dot has no click handler/focusable semantics; the user must press "Next photo" three times to reach it. On narrow/one-handed use this is a measurable interaction regression compared to the pre-restyle viewer.
- **Note:** Prev/next buttons and index state updates remain correct, and the dots still convey position visually; only the direct-jump affordance is lost.

### Checked (no defects evidenced in diff)
- **Clinical safety / behavior parity (`UltrasoundEditor.kt`):** Save/cancel dispatch preserved (`AncEvent.CancelEditor` @1106-1116 in new flow, `AncEvent.SubmitUltrasound` @1116). All draft bindings remain wired (date, GA weeks/days, fetal number/presentation/placenta/liquor, FHR/EFW/BPD/HC/AC/FL, impression, image URIs, linked visit). Validation/error surfacing preserved (`GenericEditorErrorCard` with `draft.errors`, date/GA field error flags @246-251, 359-395). GA calculation/redating/apply logic preserved (`GestationalAgeEngine.calculateGestationalAgeAt`, `redatingAdvice`, `applyScanGa` @258-355). Impression generation/merge preserved (`ultrasoundImpressionFromChoices`, "From my choices" merge @101-106, 783-805). Conditional rules preserved (biometry auto-expand when data present @525-530, "More" toggle for photos/linked visit @810-875). No fields dropped or made uneditable (enabled gating consistent with `savePhase`).
- **Shared-component/caller regressions:** `ZoomableImage` public signature unchanged (`bitmap`, `contentDescription`, `resetKey`, `modifier` @1592-1614 context); only reset-chip styling changed, zoom/reset behavior and 48dp min touch target retained (`sizeIn(minWidth = 48.dp, minHeight = 48.dp)` preserved). `UltrasoundPhotoViewerDialog` signature/contract unchanged.
- **Accessibility:** Named controls present (close/previous/next photo contentDescriptions localized @1355-1361, 1410-1413, 1445-1448; impression field given `contentDescription` @771-772; dots cleared from semantics to avoid duplicate/noisy reading @1419-1422). 48dp targets maintained on key actions (`minimumTouchTarget()` / `heightIn(min = 48.dp)` on toggles, buttons, icon buttons, save/cancel row). LTR isolation applied to GA/dates/measurements/units in viewer and editor where appropriate.
- **Hygiene:** New/changed user-facing strings moved to resources (`strings.xml` +807 onward); no em dashes or dead code introduced in the changed hunks.

**Conclusion:** Aside from the dot-tap navigation regression above, the slice reads as presentation-only with clinical/state/validation/save behavior preserved per the provided diff.
[Tokens] upload: 31073 download: 2855 cache_read: 0 cache_write: 0

