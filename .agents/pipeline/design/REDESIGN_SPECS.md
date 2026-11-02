# Redesign specs (fixed rules; do NOT design from your head)

Source of truth for every visual decision: the owner-approved mocks, served as plain HTML files you can open and read:
- `.agents/pipeline/design/d4/d1/index.html`  Laboratory, Ultrasound, Medication entry
- `.agents/pipeline/design/d4/d2/index.html`  Obstetric history, Previous pregnancies, Dating, Clinical attention card, Delivery plan
- `.agents/pipeline/design/d4/d3/index.html`  Patient directory, Patient chart, Visit and vitals entry
- `.agents/pipeline/design/d4/shared.js`      text-size and one-line-label rule
- Published copy: https://claude.ai/artifact/LUtQqawvTycThDsPd5652E

If a spec line below and the mock disagree, the OWNER OVERRIDES in section 1 win, then the mock, then this file. If the mock is silent, copy the nearest already-built screen (lab or ultrasound). Never invent a new pattern. If you still need one, STOP and ask the owner.

## 1. Owner overrides of the mock (decided 2026-11-02; they beat the mock)
- Medication: only two statuses, Active and Stopped (stored as Active / Discontinued; the stored enum stays). The mock's four statuses are dropped.
- Medication editor fields: name (presaved list), dose, route, Active/Stopped, ONE "Note / instructions". NO frequency, end date, side effects, start date, prophylaxis, non-compliance in the UI. Stored fields stay in the model; a new entry starts today.
- Medication card: bold name, status badge, one lighter line "dose · route · note". Same on the print. A note is not required.
- Tab label is "Meds".
- No handling of old records ("app not used"): do not write legacy or regression code for old patients.
- Palette is CLOSED: navy / ivory / rose / teal, light theme default.
- Aim of every round: simplify each section (fewer fields, fewer rows, fewer taps).

## 2. Design tokens (copy from the mock `:root`; map to the app theme, never new colours)
Light: bg #FBF8F5, surface #FFFFFF, surface-variant #F3E9E7, on #0D1B3D, on-variant #4C3F43, muted #6B6166, outline #79747E, outline-variant #CAC4D0, primary #0D1B3D (on #FFFFFF), primary-container #D6DFFF, secondary (rose) #934F5B, secondary-container #FFD9DE, tertiary (teal) #176B70, error #BA1A1A.
Status pills (only these): green bg #DCFCE7 / text #166534, red #FEE2E2 / #B91C1C, blue #E0F2FE / #0369A1, amber #FEF3C7 / #92400E.
Rule: only the RISK badge and real alerts use colour. Active / Stopped / Completed badges are neutral outlined (already done in `ClinicalBadges.kt`).

## 3. Shape and size
- Design width 360 dp (Honor 400). Check every screen at 360 dp (`wm size 1080x2400`, density 480; reset afterwards).
- Touch target 48 dp minimum on every tappable thing. Cards radius 16 dp, border 1 dp outline-variant, padding 10 dp, soft shadow. Page side padding 10 dp.
- Section header: small caps, 0.78em, letter spacing 0.08em, rose, bold, 4 dp rose left bar, 32 dp min height; right-aligned muted summary text allowed.
- Field label: 0.76em, bold, uppercase, muted, 10 dp above, 4 dp below. Value: stronger, normal case.
- Buttons: pill (24 dp radius), 48 dp tall, bold. Primary = navy fill white text. Secondary = rose container. Ghost = rose text. Cancel = outlined.
- Pinned bottom action bar on every entry screen: surface background, top divider, Cancel left, Save primary. Use the shared `EditorPage(bottomBar=)` slot (already used by lab and medication). Never a second implementation.
- Undo bar and delete confirm: the shared `UndoBarHost` / `DeleteConfirmDialog` / `NoticeCard`. Do not build another.
- Short labels (pills, chips, buttons, row names, tab labels up to about 30 characters) stay on ONE line, never cut, never wrapped; shrink the font to at most 80 percent before failing. Long free text may wrap.
- Text size preference: 90 to 130 percent, five steps, hard limits; light theme default.

## 4. Per screen specs (read the matching mock section, then follow)

### 4.1 Laboratory entry (mock d1 section 1)  STATUS: built (commit 6ef0f9c); polish only
One test card at a time with "Test X of Y"; edit-details (unit, range) behind a light edit button; big result buttons for qualitative tests; Flag and Note; Add tests; panel adds all its tests. Keeps: test, result, unit, reference range, flag (auto, can be overridden), note, lab name, date. Open owner decision d17: on-screen keypad vs the phone keyboard (default now: phone keyboard; do not build the keypad until the owner says).
Polish list: notice "Close" button alignment (done in NoticeCard), Review tab, Back button, "mark empty yes/no tests normal" are LOGIC items, not design.

### 4.2 Ultrasound entry (mock d1 section 2)  STATUS: built (commit 80e6416); polish only
Guided findings: one finding open at a time, the next blank opens after each answer; "Fill normal scan" (never fills placenta); live "Report so far" card; "From my choices" button; "Clear findings"; photos and visit link under More. Biometry steppers and numbered collapsible sections are NOT built: they need the owner's reference values (d18). Do not invent reference values.

### 4.3 Medication entry (mock d1 section 3 + overrides in section 1)  STATUS: simplified editor built (e6642ca); BUILD NEXT: "Today's list"
Mock behaviour to copy: one tap on a drug in a grid adds it to "Today's list" with its usual dose, route and today's date; the new line has chips to change dose / route; a Stop control on each line; every add, change and stop shows the shared Undo bar. Differences you MUST apply: two statuses only, no frequency, no end date, one note.
Layout of the list line: bold name (left), Active/Stopped badge, lighter "dose · route · note" line, Stop and Edit icons 48 dp, delete icon (already shared).
Needs first (logic round, separate from design): custom choices store for "+ Add other" (task l06). Do the logic round, review it, then the design round.

### 4.4 Visit and vitals entry (mock d3 section 3)  STATUS: not built (task d28, needs logic l08 first)
Four steps; closed rows, one opens at a time, answering opens the next blank row. Date defaults to Today. BP: two big cells with "Same as last" and "Normal" chips. Weight and fundal height start from the last visit (one tap repeats). "Fill normal values" fills ONLY blank oedema none, movements good, urine negative; NEVER BP or weight. BP 140/90 or higher turns rose with a flag and never blocks saving. Last step: one-line summary, then pinned "Save visit". "+ Add other" ends the oedema, movement, urine, BP, weight, fundal height choices.

### 4.5 Patient directory (mock d3 section 1)  STATUS: not built (d26)
One colour: only the risk badge. No coloured left edges, no icon rows. Clinical attention at a glance: the doctor's own short note (up to 140 characters) on at most two lines. A 48 dp "⋯" opens a sheet with words (Edit, Archive), not tiny icons. Filter chips with counts under the search box. One pinned "New patient" button replaces the toolbar icon.

### 4.6 Patient chart (mock d3 section 2)  STATUS: not built (d26)
One sticky header: name, age, G/P, blood group, GA pill, risk pill, bell with count. Tabs Visits | Scans | Labs+Meds | Notes, nothing longer than a screen and a half. Visit timeline: date, GA, "BP 118/76 · Wt 74.5 kg" on one line, then a short summary. One "Add" button opens a sheet: Visit, Ultrasound, Lab, Medication, Note. Alert preview two lines with "+ N more" (needs logic w03 first).

### 4.7 History, dating, attention, delivery (mock d2)  STATUS: not built (d27, w09, w10, d14, d15)
1 Obstetric history (less crowded, tap-to-add G/P/A chips); 2 Previous pregnancies (outcome and year first, details on tap, label small and light, value stronger); 3 Dating (three due dates side by side with "Use this one"; uses the existing due-date rule, never a new one); 4 Clinical attention card (two-line field, counter, quick phrases, preview); 5 Delivery plan (BLOCKED: needs the owner's field list w04 and the wrong-form fix `WorkspaceScreen.kt` about lines 806-818, tag `editDeliveryPlan`).

## 5. How to apply a spec (the loop, for every screen)
1. Open the mock section in a browser (or read the HTML) and screenshot it at 360 wide into `.agents/pipeline/design/<screen>-mock.png`.
2. Write a one-page "spec sheet" for the screen: list every element in the mock, each as `mock element -> Compose component -> existing shared component or new`. Reuse the shared components first (`NoticeCard`, `EditorPage`, `ClinicalBadges`, `ClinicalPresets` chips, `DeleteUndoUi`). A new component only if no shared one fits, and then it goes in `DesignSystem.kt`, never in the screen file.
3. Build with a Sonnet subagent. Design rounds change UI only; logic rounds change state or storage only. Never both in one round.
4. Run `.agents/scripts/run_build_gate.sh`, `run_static_checks.sh` (after commit), and the emulator classes for the screen (`run_emulator_test_class.sh`).
5. Screenshot the app at 360 dp (`take_device_screenshot.sh`) into `.agents/pipeline/design/<screen>-built.png`. Put mock and built side by side and write a gap list. A gap is any difference in: element order, spacing, sizes, label text, case, colour, icon, number of lines. Fix gaps; repeat until the gap list is empty or each remaining gap has an owner-approved reason.
6. Check: one-line labels at 360 dp, 48 dp targets, text size 90 and 130 percent, long note wrap, Stopped state, empty state, error state.
7. Level-3 rule: any `app/src` edit needs a frontier-tier review (Opus via `launch_opus_review.sh`) and an independent test review before the round counts as done. If skipped, write "review owed" in PROGRESS_LOG and the tracker.
8. Commit locally. Never push. Update `PROGRESS_LOG.md`, `HANDOFF_AGY.md` and the tracker.
9. Send the owner the built screenshots next to the mock. The owner approves; you do not.

## 6. Forbidden
- New colours, fonts, icon styles, card styles, or a second action bar / dialog / Undo bar.
- Reference values, defaults or clinical rules not written in the mock or by the owner.
- Handling for old data. Pushing. Deleting `.agents/backup-*`. Touching other apps on the phone.
- Combining logic and design in one round. Declaring done without the side-by-side gap list.
