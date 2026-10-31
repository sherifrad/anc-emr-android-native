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
