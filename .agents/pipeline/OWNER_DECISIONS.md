# Owner decisions (Dr. Sherif, 2026-10-29) - apply in round 2
D1 Rh-negative: add a visible text marker (e.g. "Rh−") in addition to colour emphasis.
D2 Hospital: appears in ONE box only. Hospital box shows the extracted name; the full source line stays in its home section (Plan/Fees) so no text is lost. Supersedes SPEC_FIX P5 "may appear twice".
D3 Emoji: remove emoji from displayed values (e.g. the blood-drop on blood group).
D4 Edit buttons: pencil icon everywhere, EXCEPT "Correct previous pregnancy", which keeps a text button (a correction changes a past record).
D5 Touch targets: approved for automatic measurement via USB on the Honor 90, but only in the later device-testing phase (not during build rounds).
D6 (2026-10-30) App lock: PIN on every cold start and after 10 minutes or more in the background.
D7 (2026-10-30) First launch forces the owner to set a 6-digit PIN (no default 123456); after 5 wrong tries a growing wait
   (30 s, 1 min, 5 min ...); PIN stored with a strong hash and random salt.
D8 (2026-10-30) Demo patients are seeded only in debug/QA builds, never in the release (clinic) app.
D9 (2026-10-31) Draft recovery, supersedes the old "drafts are memory-only" policy: on every screen that has entry fields the unsaved draft is kept (encrypted, inside the vault) so it can be brought back later; only the most recent draft per patient (per entry screen) is kept; opening a patient or screen that has an unsaved draft asks "Continue" or "Discard and start fresh"; a draft is removed when it is saved or discarded.
D10 (2026-10-31) Cancelling vault setup midway = clean rollback (back to "not set up"), not a blocked state.
D11 (2026-10-31) No Lock button inside entry screens (visit editor etc.); locking stays on the patient list top bar.
D12 (2026-10-31) Visit blood-pressure alerts use two ACOG levels (raised 140/90, severe 160/110); visit inputs use strict plausible ranges that block the save (BP 60-260 / 30-160, weight 30-250 kg, no future dates).
D13 (2026-10-31) IVF embryo days 3, 5, 6 accepted; redating against the scan is a SUGGESTION ONLY (ACOG table); LMP cycle-length adjustment is postponed to its own round.
D14 (2026-10-31) Verified rounds are committed locally without asking each time; never pushed (owner merges and pushes).
D15 (2026-10-31) Screen-capture block (FLAG_SECURE) is removed in release builds too, permanently (it is already off in debug). Screenshots and the recent-apps preview may show app content. The owner confirmed this explicitly after being told it lowers protection.
D16 (2026-10-31) English only: no Arabic resources, no Arabic UI; Arabic patient NAMES and notes still display and print with correct direction.
D17 (2026-10-31) Hysterotomy deliveries count as cesarean deliveries in the previous-delivery totals, and the difference is shown in the history text (for example G5 P4+0 with "4 previous CS (3 CS, 1 hysterotomy)") on the list, the profile and the printed report; also mentioned where the surgical history is shown. To be built as its own round (reuse previousDeliverySummary).
D18 (2026-10-31) Scan, lab and medication dates may be in the future (kept as is); only visit dates refuse the future (D12).
D19 (2026-10-31) agy writing lanes may use --dangerously-skip-permissions (the upstream relay no longer sets accept-edits). Done in run_pipeline.py for writing lanes only; read-only lanes use the relay sandbox. Never for the phone: phone lanes still obey the QA-copy rules.
D20 (2026-11-01) Entry redesign: owner chose design B (from the mock-ups) for laboratory, ultrasound and medication; the same style is to be applied later to the other entry sections (obstetric history, previous pregnancies, delivery, etc.) with their own mock-ups first.
D21 (2026-11-01) Laboratory B: the Next button stays in place (pinned, always reachable) whether or not the screen scrolls; also wire laboratory settings to the new design, with a way to edit test names, reference ranges and units (no "Codex" wording).
D22 (2026-11-01) Ultrasound B: impression is built only from the doctor's choices (remove the other impression options) plus the editable text box. Medication B: remove the Prophylaxis button; statuses are exactly Active, Completed, Discontinued, Patient non-compliant. Cards shown on the patient profile stay as designed. All related functions, colours and laboratory/medication settings must be wired to the new designs.
D23 (2026-11-01) The emulator test securitySettingsDialogDisplaysFixedAutoLockTimeoutAndAllowsManualLock is parked as a known emulator-only failure until the full device testing. Findings: on the API 26 emulator the soft keyboard stays up after the PIN entry and its target is the dialog window, which pans the window up about 330 px so the title "Clinic & System Settings" sits above the screen. Tried and reverted: card max height, hiding the keyboard in the test, clearing focus and hiding the keyboard in the dialog. Not shown to happen on a real phone. Expect 55 of 56 emulator tests to pass meanwhile.
D24 (2026-11-02) Medication simplification (supersedes D22 status list):
- Statuses: Active and Stopped only in the UI (Stopped is stored as Discontinued; existing DB schema and enum unchanged).
- Medication editor fields: name, dose, route, Active/Stopped, one Note. No frequency, end date, side effects, start date in the UI.
- Card: bold name, status badge, lighter 'dose · route · note' line. Same on print. A note is not required.
- Presets and previous prescriptions do not copy hidden fields (frequency, end date, adverse notes cleared).
- Tab label is 'Meds'.
- Aim: simplify each section (fewer fields, fewer taps). No legacy or old-patient handling.

D25 (2026-11-05) Laboratory works in batches (supersedes the one-test-at-a-time editor):
- Flow: choose tests (Add) -> enter -> Review -> ONE save for the whole batch.
- A panel is a container; every element is its own row with its own unit, range and flag.
- CBC panel = Haemoglobin, Haematocrit, MCV, MCH, WBC, Platelets (six elements). The old single
  "Complete Blood Count (CBC)" value (unit g/dL) is wrong clinically; existing records stay as history,
  new entries use the six elements.
- Urine Routine & Microscopic = protein, glucose, ketones, blood, nitrite, leukocyte esterase,
  pus cells, RBCs. Microscopy is included: the owner considers pus cells / RBCs important.
- CBC reference ranges are left blank and typed from each lab report; no invented obstetric ranges.
- Panels are customisable at three levels: per batch (tick a subset before adding), permanently in
  Settings (name + elements), and "save this selection as a panel" from the Add screen.
- OPEN: whether Undo after a batch save removes all records of the batch as one action or only the
  last record. Mock slices assume one batch undo.
