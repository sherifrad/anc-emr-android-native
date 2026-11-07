# Design report — approved direction, mocks, and the path from code to mock

Written for the owner and for an independent consultant (agy). It records **what was approved**, **the
rules that are closed**, **which mock is the target for each screen**, **where the code stands**, and
**the exact steps to close the gap** between the built batch screen and the approved mock.

---

## 1. The approved direction (closed; beats any mock or opinion)

Owner overrides, 2026-11-02, in `design/REDESIGN_SPECS.md` §1:

1. **Simplify every round** — fewer fields, fewer rows, fewer taps.
2. **Palette is CLOSED**: navy / ivory / rose / teal, light theme default. No new colours.
3. **Medication is two statuses** (Active / Stopped), one note, no frequency/end date/side effects.
4. **Tab label "Meds".** No legacy-data handling. No regression code for old patients.
5. **English only** (D16). Clinical values come from the owner; no invented ranges or rules.

Precedence when documents disagree: **owner overrides → the mock → the spec file**. If all three are
silent, copy the nearest built screen; if that still does not answer it, stop and ask.

## 2. Tokens and layout rules (from the mock `:root`, mapped to `ui/Theme.kt`)

| Token | Light value | In app |
|---|---|---|
| background | `#FBF8F5` | `ClinicalIvory` |
| surface / variant | `#FFFFFF` / `#F3E9E7` | `colorScheme.surface` / `surfaceVariant` |
| on / on-variant / muted | `#0D1B3D` / `#4C3F43` / `#6B6166` | `ClinicalNavy` / `onSurfaceVariant` |
| primary (navy) | `#0D1B3D` on `#FFFFFF` | `colorScheme.primary` |
| secondary (rose) | `#934F5B`, container `#FFD9DE` | `ActionRose` / `secondaryContainer` |
| tertiary (teal) | `#176B70` | `ClinicalTeal` |
| error | `#BA1A1A` | `colorScheme.error` |
| pills | green `#DCFCE7/#166534`, red `#FEE2E2/#B91C1C`, blue `#E0F2FE/#0369A1`, amber `#FEF3C7/#92400E` | `ClinicalBadges.kt` |

Shape and size: 360 dp design width (Honor 400), **48 dp minimum target**, cards 16 dp radius with 1 dp
outline-variant border and 10 dp padding, page side padding 10 dp. Section header: small caps 0.78em,
letter-spacing 0.08em, rose, bold, 4 dp rose bar. Buttons: pill 24 dp radius, 48 dp tall; primary navy,
secondary rose container, ghost rose text, cancel outlined. **One pinned bottom bar per entry screen**
via `EditorPage(bottomBar = …)`. **Short labels stay on one line** (shrink to 80 % before failing); long
free text may wrap. Text size 90–130 %, five steps.

Colour discipline: **only the risk badge and real alerts use colour**. Active / Stopped / Completed
badges are neutral outlined.

## 3. The approved mocks (source of truth)

| Mock | Covers | Used by |
|---|---|---|
| `design/d4/d1/index.html` | Laboratory entry, Ultrasound entry, Medication entry | §4.1, §4.2, §4.3 |
| `design/d4/d2/index.html` | Obstetric history, Previous pregnancies, Dating, Clinical attention, Delivery plan | §4.7 |
| `design/d4/d3/index.html` | Patient directory, Patient chart, Visit and vitals | §4.4, §4.5, §4.6 |
| `design/d4/shared.js` | text-size steps and the one-line-label rule | all |
| **`design/rev-b/lab-batch.html`** | **Laboratory batch (model B): Add → Enter → Review → one Save, CBC/urine as element panels, choose-subset, save-as-panel** | the batch screen |

Published copy of d4: `https://claude.ai/artifact/LUtQqawvTycThDsPd5652E`.
Mocks are HTML on purpose: a consultant reads the markup and CSS, not a picture.

## 4. Screen status (the honest table)

| Screen | Spec | Status |
|---|---|---|
| Laboratory entry (single test) | §4.1 | Built (`6ef0f9c`); polish list: Review tab, Back button |
| Laboratory **batch** | rev-b mock, D25 | **Built at the logic level; UI is a first pass, not yet matched to the mock** (see §5) |
| Ultrasound entry | §4.2 | Built (`80e6416`); biometry steppers blocked on owner values (d18) |
| Medication editor | §4.3 | Simplified editor built (`e6642ca`); "Today's list" not built |
| Visit and vitals | §4.4 | Not built (needs logic l08, task d28) |
| Patient directory | §4.5 | Not built (d26) |
| Patient chart | §4.6 | Not built (d26) |
| History / dating / attention / delivery | §4.7 | Not built (d27, w09, w10, d14, d15); delivery plan blocked on w04 |

## 5. The batch screen: approved mock vs what is in the code

**Approved flow (mock `rev-b/lab-batch.html`, six slices):**
1. Start on **Add**: batch header (date + lab for all tests), panels with counts, `Choose` beside `Add N`, search, often-added chips.
2. **Choose** sheet: tick a subset of a panel's elements; "Save this selection as a panel…".
3. **Enter** a CBC element: "CBC · Test 2 of 6", name, unit, range *blank → from the report*, big value, auto flag (Low/High), keypad/phone keyboard, Back/Next.
4. **Enter** a qualitative element: Urine Protein as big choices (Normal / Trace / + / ++ / +++), microscopy as small ranges.
5. **Review** grouped under panel headers (CBC, Urine), pending rows visible, "Mark N empty as normal", one Save.
6. **After save**: grouped cards, toast "N results saved · Undo" (one batch undo).

**In the code now (`884be6a`, `ui/LabBatchScreen.kt`):**
- Route `AncRoute.LabBatchEditor`, entry button "New laboratory batch" in the laboratory section, back/cancel return to the section.
- Three `FilterChip` tabs, Add list of panel cards with `Add N`, a search field that adds single tests.
- Enter: plain `OutlinedTextField`s for result and range, a `Switch` for abnormal, a note field, Back/Next.
- Review: flat rows with an Edit action and "Mark N empty as normal".
- Footer: Cancel + Save N (not the shared `EditorPage` bottom bar).
- Catalogue split: CBC = Hb, Hct, MCV, MCH, WBC, PLT (no default range); Urine = protein, glucose, ketones, blood, nitrite, leukocyte esterase, pus cells, RBCs.

**Gap list (each item is a real difference; none is owner-approved as an exception):**
1. No **`Choose`** step — a panel can only be added whole. The mock's per-batch subset and "save as panel" are missing.
2. No **panel group headers** in Review; rows are flat.
3. **Batch header** is two text fields; the mock uses a batch bar with date chips, lab chips and a `Change` affordance.
4. **Qualitative entry** uses a text field, not the big `Normal / Trace / + / ++` buttons.
5. **CBC ranges** are in the model (blank) but the Enter screen shows a raw text field; the mock labels it "from the report".
6. **No keypad** — acceptable for now: d17 says phone keyboard until the owner chooses (do not build a keypad).
7. **Footer** is a bespoke Row, not the shared `EditorPage(bottomBar=)`; the spec forbids a second action bar.
8. **Headers/cards** are plain `Card`; no section-header small-caps/rose bar, no 16 dp/1 dp/padding-10 treatment.
9. **Hardcoded labels** ("Add", "Enter", "Review", "Save …", "pending", "Back", "Next") instead of `strings.xml`.
10. **Never run**: no 360 dp check, no screenshot pair, no UI test, no navigation smoke. See §6.

## 6. Path from the current code to the approved mock

Order matters; each step is verifiable.

1. **Smoke first (no design work).** Start the emulator at 360 dp, open a patient → Laboratory → New laboratory batch, add CBC, enter one result, Review, Save, Undo. Fix crashes and dead ends. Until this passes, nothing else is real.
2. **Spec sheet** (`design/lab-batch-spec.md`): every mock element → Compose component → shared or new. Reuse `EditorPage`, `NoticeCard`, `ClinicalBadges`, `ClinicalPresets`, `DeleteUndoUi`; new components go in `DesignSystem.kt`, never in the screen file.
3. **Layout pass**: move the screen onto `EditorPage(bottomBar=)`, add the section-header pattern, card treatment, batch bar with chips, and the tab row styled as the mock's chips. No behaviour change.
4. **Review grouping**: group rows under panel headers (a pure grouping function, tested); keep the flat list as the fallback when a panel is unknown.
5. **Choose sheet + save-as-panel**: a modal list of a panel's elements with ticks; adds the ticked subset; "save as panel" writes a `LabCategoryGroup` (custom, included in backup). Needs a logic round first (custom group storage) — then the sheet.
6. **Qualitative entry**: big 48 dp choice buttons driven by the catalogue's qualitative vocabulary; numeric stays a field.
7. **Strings**: move every label to `strings.xml`; check the one-line rule at 360 dp and 130 % text.
8. **Screenshot pair** `design/lab-batch-mock.png` vs `design/lab-batch-built.png`, write the gap list, close it, then ask the owner.
9. **Tests**: add → enter → review → save → one-action undo in `DeleteUndoUiTest` style; an 11-row case; a Choose-subset case.
10. **Review**: level-3 rule — an independent review of the diff before the round counts as done. Record "review owed" if skipped.

## 7. Governance (how design work is done here)

- Mocks are HTML in `design/`; each screen gets a 360-wide screenshot next to the built screen.
- A design round changes **UI only**; state/storage changes are a logic round. Never both.
- The owner approves the built screenshots; the agent does not declare a screen done.
- Forbidden: new colours/fonts/card styles, a second action bar or dialog or Undo bar, invented clinical
  values, legacy-data handling, pushing, touching the clinic app.

## 8. Open design decisions (owner input needed)

| ID | Question | Blocks |
|---|---|---|
| d17 | On-screen keypad or phone keyboard for numeric results | Batch Enter polish (default: phone keyboard) |
| d18 | Ultrasound reference values for biometry steppers | §4.2 completion |
| w04 | Delivery-plan field list | §4.7.5 |
| d26 / d27 | Directory + chart, history/dating/attention rounds | §4.5, §4.6, §4.7 |

## 9. What a consultant should not re-litigate

The palette, the six-element CBC, the eight-element urine panel, blank CBC ranges, one-action batch
undo, and the Add → Enter → Review → one Save order are all **owner-approved** (D25 and the approvals in
`AI_HANDOFF_LEDGER.md`). Challenge implementation, not those decisions.
