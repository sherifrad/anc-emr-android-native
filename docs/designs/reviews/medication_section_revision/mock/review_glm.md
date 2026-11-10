# Adversarial Review — `docs/mocks/medication-section.html` (Stage 1 mock)

**No outright oracle failures found.** ORACLE-7 is the weakest PASS (a real gap in the lockout), and several oracles depend on self-attesting panels that I could only confirm by code-reading.

## Oracle verdicts

**ORACLE-1 — PASS.** Single-page mock; nothing calls `buildApp` on add/stop/edit — those only `refreshLists()` (L531, 670, 692, 713), so `#scroller` scrollTop, `S.query` and `S.showPast` are untouched. `S.snap` (L551) captures scroll/query/past at open and the verdict panel compares after commit (`report`, L815–822). Seeded content (~1086px vs 780px screen) is tall enough to scroll. *Strongest residual risk:* the panel asserts numeric scrollTop equality while adding a lock message above the list shifts content visually; "Meds tab instance" is trivially unchanged because nothing rebuilds — the check is tautological.

**ORACLE-2 — PASS.** Tile → Save: `S.task={taps:1}` at open (L552), each overlay click adds 1 (L754–756), `report` writes the task line (L821). Stop identical. *Risk:* any click on inputs/chips inflates the task count; the headline "Taps since reset" counts *every* click in the phone (L749–752), so only the task line is meaningful.

**ORACLE-3 — PASS.** `.icon-btn` 48×48 (L85), `.row-stop` 60px wide / min-height 60 (L120), `.row-main` 60 (L115), `.tile` 56 (L138), `.picker-head` 56 (L124), `.chip`/`.btn`/inputs 48 (L106, 134, 176), review `.opt`/`.seg` 48 (L60, 64), with a runnable audit (L864–874). *Risk:* it audits only the *current* DOM state — tiles inside a closed picker are never measured; the note at L873 admits this.

**ORACLE-4 — PASS.** I spot-computed: muted-on-surface 7.5:1 (light) / 8.2:1 (dark); alert on alert-bg 5.72:1 / 6.8:1; success-on-surface dark 9.1:1; dark disabled-text on disabled-bg **4.80:1** — passes 4.5 but with almost no margin. Self-generated table L877–894. *Risk:* the 4.80 dark disabled pair is one rounding away from failing; scrim text/`--color-shadow` pairs aren't audited.

**ORACLE-5 — PASS.** `overflow-wrap:anywhere` on name/meta (L90, 117–118), pills wrap via `.pills{flex-wrap:wrap}` (L91), BP/GA use `bdi dir="ltr"` (L471). *Risk:* wrapping is browser-performant but the 200% state never changes pills (`.pill` min-height 32, not font-based), so worst-cases lean on wrap only.

**ORACLE-6 — PASS (structurally, then verified).** The keyboard (`.kb`, 280px, L201) is a **sibling below `#app`**, and `.overlay` is absolutely positioned inside `#app` (L159, L458) — so the sheet and `.sheet-foot` cannot ever overlap the keyboard, irrespective of `reveal()` (L641–645). `checkKb` measures live (L802–812). *Risk:* radio toggle re-check runs on `focusin` only inside `#sheetBody`; focus in the discard dialog or failbox isn't checked.

**ORACLE-7 — PASS, with a genuine gap.** `applyEntryState` disables **all** `[data-entry]` elements (L524–527): Prescribe (L439), rows/stop buttons (L483–485), picker head (L443), search + clear + src chips + tiles (L445–449, 519), empty-state CTA (L498), hatched disabled CSS (L111). *But the strongest reason it might fail:* `#pastToggle` (L454) **lacks `data-entry`** and its click handler doesn't check `locked()` — while a save runs, "Show past" stays clickable and re-renders lists, the only live list control. Also see the note in that spec section: while the picker head is disabled, aria-expanded stays true and the panel stays open, so the "disabled" state communicates oddly.

**ORACLE-8 — PASS.** `failMode` path keeps `S.draft` and values, sets `saveError='failCause'` (the spec's exact wording, L347), renders the failbox, focuses it, no retry timer (L674–679, 581). *Risk:* `S.phase='idle'` after failure means re-locking relies solely on `!!S.draft`; nothing rebuilds — acceptable in a mock.

**ORACLE-9 — PASS.** Cancel (L601) and scrim (L575→`requestClose`) close; dirty check opens the discard alertdialog (L705–709, 605–607); Esc/back (L850–853). *Nuisance:* the Stop form is born dirty (status+stop changed from original, L546), so Cancel on an untouched stop always prompts — more friction than the design implies.

**ORACLE-10 — PASS.** Check marks + "Active" text (L518), stop icon + "Stop" word (L485), warn icons on pill/banner/errors (L471–474, 564), hatching + dashed borders + cursor on disabled (L111), ✓ glyph on segmented toggles (L66). *Risk:* the disabled hatch contrast isn't in the audit table.

**ORACLE-11 — PASS.** `#scopeNote` (L313–321) lists all four exclusions verbatim (course length, history/"Changed", undo scope, favourites/variants).

## Other defects

1. **Past toggle not disabled while saving/loading/locked** (above) — the clearest lockout miss.
2. **A11y keyboard pattern:** route/freq/status are `role="radiogroup"` of buttons but every chip is a tab stop with no arrow-key roving focus (L568–570, 593).
3. **RTL:** progress bar animation slides the same way in both directions (L169–170); `.dir` mirroring covers only back/chevrons (L48, 433, 484). In RTL the Meds tab (5th of 6) may need horizontal scrolling to be seen.
4. **200% state:** `.ic` icons stay fixed at 24/20px (L46–47) — text doubles, icons don't; the KB "Next/Done" key inherits the 32px font while its rows are fixed px — inconsistent.
5. **`#kbAction` has `tabindex="-1"`** (L286) — fine as decoration, but inconsistent: click-only control inside an otherwise keyboard-complete mock.
6. **Enter on the textarea** (Notes) unconditionally saves (Enter is "Done"), swallowing newline intent except Shift+Enter (L778–782) — matches spec but is surprising.
7. **Escape is global** (L853): pressing Esc with no sheet open fires a "Not part of this mock" toast while tabbing/entering dates — noisy.
8. **`checkKb`/audit strings are hardcoded English**, not in `I18N` — acceptable (outside frame), but the deliverable mixes languages trivially.
9. **Loading skeleton height parity** relies on 4 fixed 60px rows (L148–150); with the picker open by default in another state, heights don't match — "keep the list height" holds only for the default layout.
