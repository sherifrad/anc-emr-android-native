# Medication Meds tab, Stage 1: mock requirements and oracles

Source of truth: `docs/designs/medication_section_revision.md` (read Goals, CUJs, Technical Plan sections 1-7). This file only lists what the mock must show and the checks that decide if it is acceptable.

Output: ONE self-contained file `docs/mocks/medication-section.html` (HTML, CSS, vanilla JS, no network, no external fonts or libraries, no images). Fictional data only, no real patient data.

## What the mock must show (all clickable, at 360px width, inside a phone-width frame)

1. **Patient header strip** (context only): fictional patient name (long, to test truncation), gestational age, blood pressure, and a high-risk alert badge with an icon and text (not colour only). Must stay readable at 360px.
2. **Meds tab, review-first list**
   - Title "Active Prescriptions (N)" with count.
   - Each active row: drug name, dose, route, frequency on a compact row; a stop symbol (48x48px touch target) and an edit action (tapping the row body). No large expanded cards.
   - "Show past (N)" toggle that reveals stopped drugs; its state must survive every other action (add, stop, edit). Stopped rows have no stop symbol.
   - A "Prescribe" button (blank custom drug) in the header row.
3. **Collapsed "Add medication" picker**
   - Collapsed by default as one row. Expanding shows a search box, "Common" and "Recent" chips, and a list with a LIMITED height (about 5 rows) that scrolls inside itself.
   - Each tile: name and "dose - route - frequency". A drug already active shows a check mark and does nothing.
   - Search filters the list. Search text and picker open state must survive add, stop and edit.
4. **Bottom sheet (one shared form) in these modes**, opened with a visible scrim:
   - **Add** from a tile: name, dose, route, frequency pre-filled; "Start date" field defaulting to today. Buttons: Cancel, Save, "Save and add another".
   - **Stop** from the stop symbol: drug set to Stopped, field labelled "Stop date" defaulting to today. Buttons: Cancel, Save.
   - **Edit** from the row: stored values; route is READ-ONLY with a short note ("A different route is a new drug"); changing dose or frequency updates the same row (CUJ 3).
   - **Custom** from "Prescribe": blank form.
   - Fields: name, dose, route, frequency (fixed list of chips: Once daily, Twice daily, Three times daily, Every 8 hours, Weekly, plus "Other" with a text box), Active/Stopped control, notes, and ONE date field with the existing style "Today" shortcut and a native date input as the picker.
   - **Enter key**: pressing Enter in a field moves focus to the next field; the last field uses Done (saves only if valid). The focused field must scroll fully into view above a simulated keyboard.
   - A toggle outside the phone frame, "Show keyboard (simulated)", that puts a keyboard-height block at the bottom of the phone so the keyboard-open layout is visible. The Save button must stay visible above it.
5. **States to demonstrate** (a small control outside the phone frame switches them): Populated (default), Empty list ("No medications yet" plus a clear primary action), Loading (skeleton rows that keep the list height), Saving (sheet buttons disabled with a progress indicator, and EVERY entry point on the list visibly disabled), Save failed (sheet stays open with the entered values and a message naming the cause, for example "This patient's record changed since you opened it. Close and reopen the patient."; no automatic retry), Warning (a high-risk alert in the header), Extreme text (200% font scale with long names).
6. **Direction and theme**: a toggle outside the frame for LTR (English) and RTL (Arabic, with a few Arabic labels), and a toggle for light and dark. Colours only through CSS variables (for example `--color-surface`, `--color-text`, `--color-alert`). Directional icons must mirror in RTL.

## Oracles (the mock is accepted only if all hold)

- ORACLE-1: Add, stop and edit never navigate away from the Meds tab; scroll position, picker search text and the "Show past" toggle are unchanged after each action (demonstrate with a list long enough to scroll).
- ORACLE-2: Adding a catalog drug takes exactly 2 taps (tile, Save); stopping takes exactly 2 taps (stop symbol, Save). Show a small tap counter outside the frame.
- ORACLE-3: Every interactive control has a hit area of at least 48x48px.
- ORACLE-4: Body text and labels meet contrast 4.5:1 and large text 3:1 in both light and dark.
- ORACLE-5: Nothing critical is truncated at 360px: patient name (wraps or ellipsis with full name available), gestational age, blood pressure and the high-risk alert stay visible.
- ORACLE-6: With the simulated keyboard shown, the focused field and the Save button are both visible.
- ORACLE-7: While saving, every entry point (tiles, every stop symbol, edit, Prescribe, Add row) is visibly disabled.
- ORACLE-8: A failed save keeps the sheet open with the entered values, shows a named cause, and has no automatic retry.
- ORACLE-9: Sheet has a clear Cancel and the scrim or back action closes it; if the form was changed, a discard prompt appears before closing.
- ORACLE-10: Information is never given by colour alone (icons or text accompany colour).
- ORACLE-11: The mock has a visible note listing what Stage 1 does NOT include (course length, change history and the "Changed" mark, undo for stop and dose change, favourites and variants).

## Out of scope for the mock

No Kotlin. No real data. No network calls.
