## Review: added lines vs. antislop-ui

**No critical findings. No major findings.**

The diff checks out cleanly against the skill's rules: no default gradients, glows, or glass surfaces; no decorative dots, emoji, or capsule badges; radius values are varied and purposeful (10/12/14 dp); the only motion is progress indicators tied to a real save; every control added is tappable or visibly disabled (`enabled = false`, dimmed at 0.38 alpha, 48 dp targets via `minimumTouchTarget()`); no filler data (`"No records"` and `"Active Prescriptions (N)"` hardcoded English were *removed*, replaced by resources); and the new empty/loading/error states name the cause and the next action ("No recent medications", picker no-match, save-failed title + message, discard dialog), which is exactly what R-27 asks for. No em dash character appears in any user-visible string — the `·` separator correctly lives in a translatable resource so a translation can reorder it.

### Minor

1. **Minor — `MedicationCard.kt`, header doc comment.** Quoted: *"Closed, it shows the name, one 'dose · route · frequency' line and, when there are notes, their first line; the whole note and the other facts show when the row is opened."* The status badge (and the actions row) render **outside** the expanded-only block, so they are visible while closed — the comment's claim that the remaining facts show on open is not accurate for the badge, and the closed-state enumeration omits it. Rule: comments must state intent truthfully. Fix: reword to *"Closed, it shows the name, the dose · route · frequency line, the status badge and actions, and the notes' first line when there are notes; the full note and the remaining facts (frequency, dates) show when the row is opened."*

2. **Minor — `MedicationCard.kt`, `StopControl` doc comment.** Quoted: *"in the error colour (at least 4.5:1 on the card in both themes)"*. This asserts a specific measured contrast ratio, but nothing in the diff shows it was measured against the actual card surface color in both themes (R-36: never report a number that was not measured). If the ratio was checked against `surfaceContainerLow/Highest` variants, keep the claim but say what it was measured against; otherwise soften to "error colour for a disabled-vs-enabled cue" or measure and state the real values.

3. **Minor — `MedicationSheet.kt`, class doc.** Quoted: *"While a save runs the form and every button are disabled."* The `footer` slot (the host's `UndoBarHost`) is part of the sheet's button column and its buttons are not gated by `enabled` anywhere in this diff, so "every button" overstates the guarantee. Fix: scope the comment to what the code enforces — *"the form, the Cancel/Save buttons and 'Save and add another' are disabled"* — or gate the footer's actions during a save if that is the intent.

### Not reported (checked and passed)

- The chevrons on the card, picker toggle, and "Show past" rows are state-changing expand/collapse indicators, not the decorative `→`-on-every-button pattern.
- The active-tile check + "Active" label and the `already_on_todays_list` state description replace the old Toast with a state that is announced and visible — an improvement, not a "Most Popular"-style badge.
- `show_details` / `hide_details`, `state_expanded` / `state_collapsed`, and per-drug stop/edit content descriptions are real, relevant semantics.
- The full-width "Save and add another" button and the Cancel/Save row serve distinct actions with content hierarchy; not a template shape.
- The `revealWhen`/`revealOnFocus` scroll-into-view behavior and the comments explaining the frame-retry logic are specific, intent-bearing comments — the opposite of filler.
