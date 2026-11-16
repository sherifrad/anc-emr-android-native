## Review: antislop-human vs. added diff lines

No critical findings.

### Major

**1. Unverified contrast claim asserted in a comment (Grey-on-Grey Hallucination)**
- File/line: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationCard.kt`, KDoc of `StopControl`.
- Quoted: `"""The one-tap stop: the stop icon over a visible "Stop" label, in the error colour (at least 4.5:1 on the card in both themes), at least 48 dp."""`
- Rule: The skill's "Grey-on-Grey Hallucination" — *never assert a pairing passes without a computation*; run the contrast checker or the formula. This comment states a numeric AA pass ("at least 4.5:1") for `colorScheme.error` against "the card" without showing or performing one, and "the card" doesn't name a concrete container color (the card's `Surface` color isn't in this file's added lines).
- Fix: Run `contrast-check.py` on `MaterialTheme.colorScheme.error` against the actual card container color in light and dark themes. Either replace the claim with the verified numbers ("#B3261E on <color>: 6.5:1 light / dark: X:1") or delete the numeric claim and keep only "in the error colour, verified against AA". Also note the label is `labelMedium` (~12sp), so 4.5:1 (not 3:1) applies — say so explicitly if the claim stays.

**2. Interactive boundaries under 3:1 (`outlineVariant` 1 dp borders)**
Three added controls are distinguished from their background only by a `BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)`:
- `MedicationTapToAddSection.kt`, `PickerToggle`: `border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)`
- `MedicationTapToAddSection.kt`, `MedicationPickerTile`: same border, on a `Surface` whose color (`surface`) can be near-identical to the surrounding list background
- `MedicationStopBar.kt`, `StopUndo`: same border on `surfaceContainerHighest`

Rule: Non-Text Contrast — every component boundary needs 3:1 against adjacent colors (WCAG 1.4.11). By the WCAG formula, light-theme `outlineVariant` (#CAC4D0) against near-white surface computes ≈1.6:1; in dark theme it is similarly low. The tile background itself (`surface`) may not differ ≥3:1 from the parent container either, so the border is the only edge — and it fails. Low-vision users cannot find the tap target edge.
- Fix: Use `MaterialTheme.colorScheme.outline` for the border (≈3:1+ against M3 surface colors), or compute the actual pairs (border vs. tile color, tile color vs. parent background) with the checker in both themes and switch to a combination that passes. Same fix for all three sites; the Undo bar can alternatively rely on elevation only if the shadow is guaranteed visible, which is not verifiable in code — prefer a passing border.

### Minor

**3. Disabled-dimming is alpha-only, value undocumented as verified**
- File/line: `MedicationCard.kt`, `internal const val MEDICATION_DISABLED_ALPHA = 0.38f` (applied via `.alpha(...)` to `PickerToggle`, `MedicationPickerTile`, and disabled `StopControl`/edit/delete tints).
- Rule: "No Hover, Active, or Disabled State" — disabled must be distinct and non-interactive (it is: `clickable(enabled = ...)` blocks action, so this passes). The residual minor point: 0.38 alpha on a near-white surface can drop the tile below perceptibility entirely rather than reading as "disabled but findable", and the constant carries no contrast verification.
- Fix: Compute the dimmed pair against the parent background in both themes; if the dimmed state is indistinguishable from the surface, raise the alpha or dim only foreground content (text/icon/border) instead of the whole `Surface` including its background.

### Passed cleanly (no action)
- Touch targets: `minimumTouchTarget()` added on every added control (`datePickerConfirm/Dismiss`, `${tag}TodayButton`, filter chips, buttons, `ShowPastToggle`, `StopControl`) — good.
- Icon-only controls all carry names (`stop_medication_description`, `edit_medication_description` with the drug name) or are marked decorative with adjacent text — good.
- Keyboard coverage: `imePadding`, `bringIntoViewRequester`/`revealOnFocus` on fields, and the picker's "shortens the list instead of covering its end" design satisfy the mobile-keyboard rule.
- States: empty (`MedsEmptyState`, `medPickerEmpty`, `no_active_medications`), loading (`LinearProgressIndicator` + "Saving" label text), and error (text-first `SaveMessage`/`StopFailure` with live regions) are all present and not color-only — good.
- No em dash characters appear in any added user-visible or comment text.
