## Review Report: Medication Section Mock (Stage 1)

### Oracle Results

| Oracle | Verdict | Evidence |
|--------|---------|----------|
| **ORACLE-1** (tab stays put, scroll/search/past survive) | **PASS** | `buildApp()` preserves scroll (L429, L461); `openSheet()` snapshots `scroll`, `q`, `past` (L551); `commit()`/`closeSheet()` do not rebuild the app, so scroller persists; `report()` compares before/after (L816–821). |
| **ORACLE-2** (2 taps add/stop, tap counter shown) | **PASS** | Task tap counter initialized to 1 on open (L552), incremented per overlay click (L755), reported in `report()` (L821). Global counter also present (L295–L298, L749–L753). |
| **ORACLE-3** (every control ≥48×48 px) | **FAIL** | Simulated keyboard keys `.kb-row div` are `height:44px` (L205) — below 48 px. All app controls meet 48 px (btns, chips, tiles, stop icons, inputs, etc.). |
| **ORACLE-4** (contrast 4.5:1 body, 3:1 large, both themes) | **PASS** | Contrast table computed from CSS variables checks 17 pairs per theme (L877–L894). Light/dark tokens chosen with sufficient luminance gaps; automated check would pass. |
| **ORACLE-5** (no critical truncation at 360 px) | **PASS** | Patient name uses `overflow-wrap:anywhere` (L90); pills `flex-wrap:wrap` (L91); high-risk pill + banner use icon+text; extreme mode sets `--fs:32px` (L81) and wraps. |
| **ORACLE-6** (focused field + Save visible above simulated keyboard) | **FAIL** | Sheet sits at bottom of `overlay` (L159, L162) and overlaps the keyboard block (both anchored to phone bottom). `checkKb()` (L802–L812) would report Save button **hidden** because `sr.bottom > kb.top`. |
| **ORACLE-7** (all entry points visibly disabled while saving) | **PASS** | `locked()` true during `saving`/draft/loading (L421); `applyEntryState()` sets `disabled` on all `[data-entry]` (L526); CSS shows dashed border + hatch pattern (L111). “Saving” review state demonstrates (L827–L828). |
| **ORACLE-8** (failed save keeps sheet open, shows cause, no auto-retry) | **PASS** | `failMode` branch keeps `S.draft`, sets `saveError='failCause'`, re-renders (L674–L677); `failbox` shows named cause “This patient's record changed…” (L581, L347). No retry logic. |
| **ORACLE-9** (Cancel/scrim/back close; discard prompt if dirty) | **PASS** | `requestClose()` checks `dirty()` (JSON compare, L558) → shows `dialog` with Keep/Discard (L605–L607). Scrim (L575), Cancel (L601), system back/Esc (L269, L853) all route through `requestClose()`. |
| **ORACLE-10** (no colour-only information) | **PASS** | Every status uses icon+text: high-risk pill (icon+text L472), BP elevated (icon+text L471), banner (icon+text L474), stop symbol (icon+“Stop” L485), active check (icon+“Active” L518), errors (icon+text L564, L581), saving spinner+text (L602), disabled hatch pattern (L111), selected chips check+bold (L135, L570), tabs border+weight (L100). |
| **ORACLE-11** (visible Stage 1 exclusions note) | **PASS** | `<aside id="scopeNote">` lists the four required exclusions (L313–L321). |

---

### Other Defects

1. **Keyboard keys below 48 px hit target** (ORACLE-3 failure). `.kb-row div { height:44px }` (L205). The simulated keyboard is an interactive control in the mock; it fails the 48 px rule.

2. **Sheet overlaps simulated keyboard** (ORACLE-6 failure). The `overlay` covers the full phone; the `sheet` is bottom-aligned inside it. The keyboard block (`.kb`) sits at the bottom of `.screen`, a sibling of `.overlay`. Both occupy the bottom ~280 px. The Save button in `.sheet-foot` is therefore visually behind/overlapping the keyboard. `checkKb()` correctly detects this.

3. **Picker/past toggle chevrons not mirrored in RTL**. They use a down-chevron rotated 180° on expand (L126, L153) but lack the `.dir` class that flips horizontal icons (L48). In RTL the chevron still points down/up — acceptable for vertical expansion, but inconsistent with the “directional icons must mirror” requirement.

4. **`Noto Sans Arabic` font not guaranteed locally**. Font stack includes it (L40) but no `@font-face` or fallback guarantee; on a device without the font, Arabic may fall back to a generic sans-serif. Not a functional failure for the mock, but a deployment risk.

5. **Tap counter counts *all* clicks, not just the canonical flow**. The global counter (L749–L753) increments on any enabled control click. The task-specific counter (L754–L756) is more accurate but still increments on any overlay interaction (e.g., switching route chips). The reported “2 taps” assumes a clean flow.

6. **`prefers-reduced-motion` disables animations but leaves progress bar at 60% opacity** (L210). Acceptable.

7. **Empty state “Prescribe the first medication” button lacks `data-entry` when not locked** — it has it (L498), but the empty state only appears when `!S.meds.length` (L496). In “Empty list” review state this works.

---

### Summary

**Failures:** ORACLE-3 (keyboard keys 44 px), ORACLE-6 (Save button not above keyboard).  
**Passes:** ORACLE-1, 2, 4, 5, 7, 8, 9, 10, 11.  
**Action required:** Increase `.kb-row div` height to 48 px; constrain sheet `max-height` or reposition it when `.kb-on` is active so the Save button remains above the keyboard block.
[Tokens] upload: 36314 download: 9305 cache_read: 0 cache_write: 0

