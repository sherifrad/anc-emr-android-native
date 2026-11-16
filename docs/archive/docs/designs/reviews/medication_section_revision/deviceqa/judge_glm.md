## Defects first (with dump + node)

1. **Low-risk filter clipped off-screen** — `02_back_to_list`: `riskFilter-low | [1263,858][1264,1026]` is 1 px wide. The filter row overflows the 1264 px screen; the control is unreachable.
2. **Show-past toggle buried under the keyboard** — `07_picker_search`: `medShowPastToggle [56,1707][1208,1792]` vs keyboard top at y=1708 → 85 px (≈24 dp) visible, almost fully obscured, and below the 168 px (48 dp) minimum.
3. **Picker tile under the nav bar** — `06_picker_open`: `medTile-med_calcium [56,2641][1208,2736]` runs past the nav-bar line (y=2658); the tile list does not respect the bottom inset (also weakens D04's "scrolls inside itself" claim).
4. **Stopped-row actions exceed their row and the screen** — `13_past_open`, second row `medication-c2b9…[56,2395][1208,2658]`: its children `delete…[858,2563][1026,2731]`, `editMedication…[1026,2563][1194,2731]`, badge `[196,2608][419,2658]` overflow the parent into the nav-bar area — Edit/Delete partially unreachable.
5. **Collapsed status labels** — `08_sheet_open`, `14_custom_sheet`, `15_rejected`: `Active [112,2207][276,2210]` and `Stopped [702,2207][916,2210]` are 3 px tall. Either clipped text or a dump anomaly; needs eyes on the PNG.
6. **Inconsistent title text** — `09_after_add`/`11_undone`: "Active **Prescription** (1)" vs "Active **Prescriptions** (N)" everywhere else.
7. **Sub-48dp target** — all workspace dumps: `View clinical alert details [1117,865][1180,928]` is 63×63 px (≈18 dp).
8. **Fixture mismatch / launch-state contradiction** — plan P-A = 4 active + 2 stopped; every dump shows "Active Prescriptions (0)", Show past (8)→(9). And `01_launch.xml` already shows the picker expanded, search "fer" typed, "Hide past (8)" — contradicting "collapsed by default, Show past off" and making the dump naming unreliable.
9. **No BP in the workspace header** (01/03/04/05/17) though plan D02 requires name+GA+BP+risk there.
10. **Stale search state**: "fer" persists in `medSearchField` through undo, expiry, discard (`09`–`13`, `17`) and even appears in `01_launch`.

## Unsupported / weak PASS lines

- **D01**: dump `05_meds_tab` shows "No active medications" — the plan's D01 (active drugs first, stop symbols) was never satisfiable with this fixture; only the weaker scripted claim holds.
- **D04**: search/chips/tiles present, but internal scrolling and "list not pushed away" are not provable from one dump; calcium tile clipped (defect 3).
- **D06**: prefill (Ferrous Sulfate / 200 mg / Once daily) shown in `08_sheet_open`, but "start date today" appears nowhere.
- **D10** (expiry): see (b) — no bar-present baseline for the second stop.
- **D14**: `16_discard` proves the prompt; the "Keep editing returns with text intact" branch was never dumped.
- **D05 SKIP is over-cautious**: `07_picker_search` shows `keyboard_holder [0,1708][1264,2575]` and `medTile-med_iron [56,1484][1208,1658]` — the tile bottom (1658) is above the keyboard (1708). The dump supports PASS.

## (a) D07 and D09 against surrounding dumps

- **D07** ("adds one row 0→1, closes sheet"): `09_after_add` shows "Active Prescription (1)", exactly one `medication-c2b9…` row with Active badge + Stop/Delete/Edit, tab badge "Meds 1", `medShowPastToggle (8)` unchanged, no sheet. Supports the scripted claim. But the **plan's** D07 (second add in 2 taps, Folic acid) never ran — the persistent "fer" query filtered Folic acid out, so the picker-stays-open shortcut was never exercised.
- **D09** ("undo removes bar, restores row"): `11_undone` shows the same row id restored with identical dose/frequency text, count back to (1), past back to (8), no `medStopUndoBar`. Supported.

## (b) Undo bar after D09

The bar (`medStopUndoBar`) appears in exactly one dump: `10_stopped`. After `11_undone` (D09), dumps `12_expired`–`17_discarded` contain **no bar ever**. `12_expired` shows count (0), past (9) — a second stop happened between `11` and `12`, but no dump captured that stop's bar. So the expiry PASS rests on a single absent-bar snapshot: it cannot distinguish "expired at ~10 s" from "never shown" or "expired early", and both dumps read clock 9:06, so timing is unverifiable.

## Plan checks never performed at all

D02 (header text incl. BP — BP absent), plan-D07 (2-tap second add), D08 (scroll position survives add), D12 (Cancel on *untouched* form), D14 (system Back), D18 (Show past survives add/stop/edit sequence), D19 (scrim blocks entry points), D20's "Stop date" field, D21 (Enter navigation), and D22–D26 (emulator-only; not even recorded as SKIP). Also note the script renumbered checks (script D08 = plan D15, script D12 ≈ plan D09/D10/D11), which breaks the plan's traceability for sign-off.
