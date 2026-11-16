## Review: added lines vs. antislop rules

No em dashes (`—`) appear in any added string or comment, so R-02 is clean. The diff also does well on R-26/R-27: empty, saving, and error states exist, and disabled controls are visibly dimmed. Findings below.

### Major

**1. The medication sheet can become an inescapable dead end in `SavePhase.UNRESOLVED`** — `MedicationSheet.kt`
```kotlin
val enabled = savePhase == SavePhase.IDLE || savePhase == SavePhase.ERROR
```
```kotlin
!dismissAllowed -> false
```
Rule: R-26 (every interactive element must have a real behavior or be removed; no dead ends), C-4 (Resilience).

In `UNRESOLVED`, `enabled` is false, so Cancel, Save, and "Save and add another" are all disabled, and `confirmValueChange` refuses every hide attempt (back gesture, scrim tap, swipe). `SaveMessage` shows `save_unresolved` text but offers no retry or resolve control in the diff. Unless the view model resolves `UNRESOLVED` on its own (not shown here), the doctor is locked inside the sheet with nothing tappable. Fix: allow dismissing/closing the sheet in `UNRESOLVED` (treat it like `ERROR` for dismissal), or add a visible "Resolve"/"Retry" control inside the sheet for that phase.

### Minor

**2. Pointless plurals with identical `one` and `other`** — `strings.xml`
```xml
<item quantity="one">Show past (%1$d)</item>
<item quantity="other">Show past (%1$d)</item>
```
Same for `medication_hide_past`. Rule: C-1 (Intentionality — no decision without a reason).

The plural forms are byte-identical, so the plural adds nothing and will mislead translators into thinking a distinction is needed. Fix: use a plain `string name="medication_show_past">Show past (%1$d)</string>` with `stringResource`, or write distinct forms if one is intended.

**3. Unverifiable contrast claim stated as fact in a comment** — `MedicationCard.kt` (`StopControl` KDoc)
```kotlin
* The one-tap stop: the stop icon over a visible "Stop" label, in the error colour (at least 4.5:1 on the card in
* both themes), at least 48 dp.
```
Rule: C-5 / R-36 spirit (claims must be verifiable, not asserted).

The diff contains no measurement of `colorScheme.error` against the card surface in both themes; if a future theme pairs a lighter error color with a light surface, this comment becomes false and hides the defect. Fix: either verify the ratio for the shipped themes and keep the number with its source, or soften to "error colour chosen for error semantics; verify 4.5:1 per theme."

**4. Empty-state text references a button by a label that can differ on screen** — `MedicationTapToAddSection.kt` with `strings.xml`
```kotlin
R.string.medication_picker_no_match   // "No match. Use %1$s for a custom drug."
```
formatted with `stringResource(R.string.prescribe_medication)` → "No match. Use Prescribe for a custom drug."

When the patient has no medications at all, the visible button reads "Prescribe the first medication" (`medications_empty_action`), not "Prescribe". Rule: C-2 / R-38 (content must match reality; no ghost references).

Fix: interpolate the control's actual label per context, or reword to a fixed instruction that does not quote a button, e.g. "No match. Prescribe it as a custom drug."

**5. Telegraphic note may not parse for clinicians** — `strings.xml`
```xml
<string name="medication_route_locked_note">A different route is a new drug.</string>
```
Rule: C-1 (Intentionality) / copy clarity (R-02's "text must feel natural and human").

As a standalone caption under "Route (read-only)" the sentence is cryptic; it does not say what happens or why. Fix: "Changing the route makes it a new prescription." (same meaning, natural sentence).

### Not reported (no rule tie)
The `·` separators, deleted strings, `rememberUpdatedState` route fix, touch-target minimums, and the picker's non-clickable "Active" tiles all look correct and rule-compliant from the diff alone. One caveat I could not check without file access: the diff deletes several string resources (`medication`, `tap_to_add`, `todays_list`, etc.); if any file outside this diff still references them, the build breaks — worth a compile check.
