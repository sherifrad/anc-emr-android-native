### Scorecard (0–10 per criterion)

| Criterion | [mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html) | [mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html) | [mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html) |
| :--- | :---: | :---: | :---: |
| **(1) Distinction: heading / field label / value** | 6 | 6 | 8 |
| **(2) Calm clinical tone, one colour per family** | 3 | 8 | 9 |
| **(3) Compact dashboard card, tag on name row, RTL** | 5 | 5 | 8 |
| **(4) Contrast >= 4.5:1 (computed)** | 8 | 9 | 10 |
| **(5) Antislop rules** | 6 | 8 | 9 |
| **(6) Compose M3 feasibility (360dp, 48dp targets)** | 6 | 6 | 8 |
| **Total (out of 60)** | **34** | **42** | **52** |

---

### Ranking

1. **[mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html)** (52 / 60)
2. **[mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html)** (42 / 60)
3. **[mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html)** (34 / 60)

---

### Criterion-by-Criterion Evaluation & Contrast Computations

#### (1) Heading / Field Label / Value Distinction
- **mock_A (6/10)**:
  - Section heading `.section-title` is `16px / 700` (`#1F2F5C`), field label `.field-label` is `12px / 700` (`#22375D`), and field value `.field-value` is `16px / 500` (`#151C26`).
  - Heading and field label share almost identical navy hues (`#1F2F5C` vs `#22375D`) and identical `700` weight.
  - On the dashboard card ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L618-L664)), `EDD 11/02/2027` and `Blood A+` lose their label/value pairing and are demoted to `11px` muted sub-captions (`.metric-sub`) beneath `GA` and `Parity`.
- **mock_B (6/10)**:
  - Section heading `.sec-title` is `15px / 700` (`#1F2F5C`), which is 1px smaller than the `16px` field values (`.fld`) inside the section.
  - "Previous pregnancies" ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L187-L192)) abandons the label-above-tinted-box system, rendering plain white bordered rows (`.pp-row`) with `15px` text (below the brief's `>= 16sp` value requirement) and no field label.
  - "Obstetric history" ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L178-L183)) renders a summary chip (`Gravida 4, Para 1, Abortions 2`) immediately above 3 field boxes repeating `Gravida: G4`, `Para: P1`, `Abortions: A2`.
- **mock_C (8/10)**:
  - Section heading `.shead h3` is `15px / 700` (`#171B26`), field label `.flabel` is `12.5px / 600` (`--*-lb`), and field value `.fbox` is `16px / 600` (`#16203A`).
  - Section heading (`15px`) is 1px smaller than the field value (`16px`) and uses `--ink` (`#171B26`) rather than `--navy` (`#1F2F5C`).
  - Both `.flabel` and `.fbox` use `font-weight: 600`, reducing weight contrast between label and value on multi-word prose fields.

#### (2) Calm Clinical Tone, One Colour Per Family (Not a Rainbow)
- **mock_A (3/10)**:
  - Fails the brief requirement to mock all 6 Overview sections ([brief_mock.md](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/brief_mock.md#L2)): only renders 1 section ("Medical, surgical & family") using a single navy tint (`#F0F4FA`).
  - Defines `--dating-surface`, `--dating-border`, and `--dating-label` in CSS ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L16-L18)), which are unused dead code in the HTML.
  - Injects an unrequested red allergy alert banner (`#FEECEB` / `#8A1C1A`) inside the single Overview card.
- **mock_B (8/10)**:
  - Assigns 5 families across the 6 sections: slate (Identity), green (Pregnancy dating), purple (Obstetric history & Previous pregnancies), teal (Medical/surgical/family history), amber (Current medications).
  - Although "Previous pregnancies" carries `.fam-purple` on its parent `<div class="sec fam-purple">`, its child `.pp-row` elements do not reference `--purple-box` or `--purple-lab`, so the family color is absent from that section.
- **mock_C (9/10)**:
  - Assigns 5 desaturated families across all 6 sections with uniform box luminance ($L \in [0.837, 0.870]$): slate (`fam-id`), blue (`fam-dt`), teal (`fam-ob` shared by Obstetric history and Previous pregnancies), sand (`fam-md`), and plum (`fam-rx`).
  - Because sections use flat top hairlines (`border-top: 1px solid var(--line)`) rather than distinct card containers, the two adjacent `fam-ob` sections (`Obstetric history` and `Previous pregnancies`) run together visually.

#### (3) Compact Dashboard Card, Tag on Name Row, No Dead Space, Arabic RTL
- **mock_A (5/10)**:
  - Places name, status chip, and overflow button on one row, but collapses the 5 required metrics into 3 columns by burying `EDD 11/02/2027` and `Blood A+` as `11px` sub-labels and inventing `Normal BP` as a 3rd sub-label.
  - **RTL flex/text alignment mismatch**: `.metric-item.align-end` ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L431-L434)) sets `align-items: flex-end; text-align: right;`. Inside `.rtl-card` (`direction: rtl`), `flex-end` is the **left** edge while `text-align: right` is the **right** edge.
  - **RTL BiDi corruption**: `<span class="metric-sub">فصيلة A+</span>` ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L712)) has no `dir="ltr"` or `<bdi>` around `A+` inside an RTL container, causing the trailing neutral `+` to resolve to RTL embedding direction and render visually to the left of `A` (`+A`).
- **mock_B (5/10)**:
  - **Missing Arabic Before card**: Renders Before only in English ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L223-L251)).
  - **Missing fields in Incomplete variant**: The `Salma Hassan` card ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L280-L294)) drops `Blood group` and `Due date` entirely.
  - **RTL strip pinned to physical left**: `.strip` ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L84-L88)) uses `left: 0`, `border-radius: 0 10px 10px 0`, and `.pcard-inner { padding-left: 10px }`. In the Arabic RTL card ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L304-L320)), the severity bar remains stuck on the physical left edge behind the kebab icon instead of mirroring to the right (start) edge.
  - **RTL grid expansion**: Switches from a 2-row (`3+2`) metric grid in English to a 2-column, 3-row (`2+2+1`) grid (`.metric-row-ar`, [mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L108)) in Arabic, making the Arabic card taller with a single orphan metric on row 3.
  - **RTL BiDi corruption**: `<div class="metric ar"><span class="lab">فصيلة الدم</span><span class="val">A+</span></div>` ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L316)) applies `direction: rtl` directly to `.metric.ar` without `dir="ltr"` on `.val`, rendering `A+` visually as `+A`.
- **mock_C (8/10)**:
  - Includes both English and Arabic cards in Before and After, keeps all 5 required metrics in a 2-row grid (`3` columns on row 1, `1 + span 2` on row 2), and sets `dir="rtl" lang="ar"` on the Arabic `<article>` so flex and grid flow mirror natively.
  - Wraps Arabic card metric values in `dir="ltr"` so `A+`, `110/70`, and `11/02/2027` do not suffer BiDi reordering.
  - **RTL block alignment bug**: In [mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L444-L457), `<div class="mval" dir="ltr">` is a block-level `<div>` with `dir="ltr"` and no explicit `text-align`. Per the HTML UA stylesheet rule for `[dir="ltr"]`, `text-align: start` resolves to **left** alignment, while `.mlabel` (`dir="rtl"`) resolves to **right** alignment. Inside every Arabic grid cell (most visibly on `.metric--span2` spanning 2 columns), the Arabic label aligns to the right edge while the LTR value aligns to the left edge.
  - **Untranslated strings in Arabic card**: Leaves `<span class="tag tag--info">Incomplete</span>` and `27+2 wks` in English inside the `lang="ar"` card ([mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L447-L451)).

#### (4) Contrast of Every Label and Value >= 4.5:1 (WCAG 2.1 Relative Luminance)

| Mock | Element / Pair | Foreground | Background | Computed Ratio | Status |
| :--- | :--- | :--- | :--- | :---: | :---: |
| **mock_A** | Section title (`.section-title`) | `#1F2F5C` ($L=0.0310$) | `#FFFFFF` ($L=1.0000$) | **12.96 : 1** | Pass |
| **mock_A** | Overview field label (`.field-label` on card) | `#22375D` ($L=0.0386$) | `#FFFFFF` ($L=1.0000$) | **11.85 : 1** | Pass (Note claims 7.8:1) |
| **mock_A** | Overview field label (vs `.field-box` tint) | `#22375D` ($L=0.0386$) | `#F0F4FA` ($L=0.9013$) | **10.74 : 1** | Pass |
| **mock_A** | Overview field value (`.field-value`) | `#151C26` ($L=0.0113$) | `#F0F4FA` ($L=0.9013$) | **15.52 : 1** | Pass |
| **mock_A** | Card metric label (`.metric-label`) | `#48566A` ($L=0.0910$) | `#F8FAFC` ($L=0.9536$) | **7.12 : 1** | Pass |
| **mock_A** | Card metric value (`.metric-val`) | `#151C26` ($L=0.0113$) | `#F8FAFC` ($L=0.9536$) | **16.37 : 1** | Pass |
| **mock_A** | Card metric sub (`.metric-sub`: EDD, Blood) | `#59677B` ($L=0.1325$) | `#F8FAFC` ($L=0.9536$) | **5.50 : 1** | Pass |
| **mock_A** | Moderate chip (`.status-chip.moderate`) | `#704300` ($L=0.0746$) | `#FEF3D6` ($L=0.9002$) | **7.63 : 1** | Pass |
| **mock_A** | Incomplete chip (`.status-chip.incomplete`) | `#8C4700` ($L=0.1013$) | `#FFF2E3` ($L=0.9031$) | **6.30 : 1** | Pass |
| **mock_A** | Before label (`.before-label`) | `#6C7A8C` ($L=0.1900$) | `#FFFFFF` ($L=1.0000$) | **4.38 : 1** | **Fail (< 4.5:1)** |
| **mock_B** | Section heading (`.sec-title`) | `#1F2F5C` ($L=0.0310$) | `#FFFFFF` ($L=1.0000$) | **12.96 : 1** | Pass |
| **mock_B** | Slate label (`--slate-lab` on white / on box) | `#3D4A66` ($L=0.0692$) | `#FFFFFF` / `#EEF1F6` ($L=0.8774$) | **8.81 : 1** / **7.78 : 1** | Pass |
| **mock_B** | Green label (`--green-lab` on white / on box) | `#1E6B3C` ($L=0.1112$) | `#FFFFFF` / `#E9F3EB` ($L=0.8742$) | **6.51 : 1** / **5.73 : 1** | Pass |
| **mock_B** | Purple label (`--purple-lab` on white / on box) | `#5B3A8E` ($L=0.0720$) | `#FFFFFF` / `#F2EEF7` ($L=0.8675$) | **8.61 : 1** / **7.52 : 1** | Pass |
| **mock_B** | Teal label (`--teal-lab` on white / on box) | `#0E5F5F` ($L=0.0910$) | `#FFFFFF` / `#E8F3F3` ($L=0.8773$) | **7.45 : 1** / **6.58 : 1** | Pass |
| **mock_B** | Amber label (`--amber-lab` on white / on box) | `#7A5210` ($L=0.1021$) | `#FFFFFF` / `#F8F1E3` ($L=0.8842$) | **6.90 : 1** / **6.14 : 1** | Pass |
| **mock_B** | Field values (`--ink` on family boxes) | `#1B2333` ($L=0.0167$) | `#F2EEF7`..`#F8F1E3` ($L=0.868..0.884$) | **13.76 : 1 – 14.01 : 1** | Pass |
| **mock_B** | Card metric label (`--muted` on white) | `#5A6172` ($L=0.1194$) | `#FFFFFF` ($L=1.0000$) | **6.20 : 1** | Pass (Comment says 5.6:1) |
| **mock_B** | Severe tag (`.tag-severe`) | `#8A2E24` ($L=0.0748$) | `#F6E4E1` ($L=0.8052$) | **6.85 : 1** | Pass |
| **mock_C** | Section heading / Card value (`--ink`) | `#171B26` ($L=0.0110$) | `#FFFFFF` ($L=1.0000$) | **17.21 : 1** | Pass |
| **mock_C** | Identity label (`--id-lb` on white / on box) | `#33405C` ($L=0.0515$) | `#FFFFFF` / `#EDF0F6` ($L=0.8698$) | **10.34 : 1** / **9.06 : 1** | Pass |
| **mock_C** | Dating label (`--dt-lb` on white / on box) | `#1F3A66` ($L=0.0428$) | `#FFFFFF` / `#E7EEF9` ($L=0.8498$) | **11.31 : 1** / **9.70 : 1** | Pass |
| **mock_C** | Obstetric label (`--ob-lb` on white / on box) | `#1C4A45` ($L=0.0564$) | `#FFFFFF` / `#E4F0EE` ($L=0.8498$) | **9.87 : 1** / **8.46 : 1** | Pass |
| **mock_C** | Medical label (`--md-lb` on white / on box) | `#4A3A1E` ($L=0.0459$) | `#FFFFFF` / `#F4EEE1` ($L=0.8582$) | **10.95 : 1** / **9.47 : 1** | Pass |
| **mock_C** | Meds label (`--rx-lb` on white / on box) | `#4A2C4C` ($L=0.0381$) | `#FFFFFF` / `#F0EAF1` ($L=0.8373$) | **11.92 : 1** / **10.07 : 1** | Pass |
| **mock_C** | Field values (`--val` on family boxes) | `#16203A` ($L=0.0151$) | `#F0EAF1`..`#EDF0F6` ($L=0.837..0.870$) | **13.63 : 1 – 14.13 : 1** | Pass |
| **mock_C** | Card metric label (`--ink-3` on white) | `#5C6475` ($L=0.1267$) | `#FFFFFF` ($L=1.0000$) | **5.94 : 1** | Pass |
| **mock_C** | Warn tag (`.tag--warn`) | `#6B4A12` ($L=0.0813$) | `#FBEFD9` ($L=0.8726$) | **7.03 : 1** | Pass |
| **mock_C** | Info tag (`.tag--info`) | `#2C3D63` ($L=0.0478$) | `#E9EDF7` ($L=0.8461$) | **9.16 : 1** | Pass |

- **mock_A (8/10)**: `.before-label` (`#6C7A8C` on `#FFFFFF`) is 4.38:1, and the note on line 583 claims `"Contrast 7.8:1 on labels"`, which does not match `#22375D` on `#FFFFFF` (11.85:1) or `#F0F4FA` (10.74:1).
- **mock_B (9/10)**: All pairs pass; CSS comment on line 12 states `5.6:1 on white` for `#5A6172` when actual contrast is 6.20:1.
- **mock_C (10/10)**: Every label and value pair clears 5.94:1, and all 5 Overview family labels clear AAA (7:1) against both `#FFFFFF` and their respective box tints.

#### (5) Antislop Rules
- **mock_A (6/10)**:
  - Uses `box-shadow: 0 2px 8px rgba(20, 30, 50, 0.08)` on `.phone-frame` ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L126)).
  - Invents clinical pathology not in the brief (`Allergy: Penicillin (severe rash)`, `Chronic hypertension on Methyldopa`, `Cesarean section (2021)`, `Normal BP`) and injects artificial `.dead-space-indicator` callout boxes inside the Before patient cards.
- **mock_B (8/10)**:
  - Introduces a 3rd border-radius value (`border-radius: 3px` on `.kebab i`, [mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L102)) outside its declared two-radius system (`10px` and `20px`).
  - Invents a third patient (`Salma Hassan`) and alters the Arabic card's clinical data (`150/100`, `ضغط شديد الخطورة`).
- **mock_C (9/10)**:
  - Zero gradients, zero shadows/glow, zero em dashes, zero tracked all-caps labels, zero decorative icons, strict two-radius token system (`10px` and `16px`).
  - Uses `"16 dp"` instead of `"16 sp"` for font size in the masthead copy ([mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L208)).

#### (6) Feasibility in Jetpack Compose Material 3 (360dp width, 48dp touch targets)
- **mock_A (6/10)**:
  - Stacks every Overview field as a full-width (`1fr`) row with `min-height: 48px`; across all 6 Overview sections (~18 fields), this would require >1,200dp of vertical scrolling for read-only fields.
  - Arabic status chip `"قيد استكمال التسجيل"` (20 chars) + 4px severity bar + 48dp menu button leaves insufficient width for 3- or 4-part Arabic patient names at 360dp without early truncation.
- **mock_B (6/10)**:
  - `.phone` width is `376px` ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L38)), violating the `360px` frame constraint.
  - `.kebab` touch target in the After patient card ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L101)) is `32px × 19px` (`width: 32px; padding: 8px 0` inside a `21.25px`-high `.pc-head`), failing the 48dp minimum touch target requirement.
- **mock_C (8/10)**:
  - `.edit` action ([mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L86-L91)) sets `min-height: 48px; padding: 0 2px` with **no `min-width: 48px`**, resulting in a ~`28px × 48px` hit target that fails Android M3's `48dp × 48dp` minimum touch target width.
  - Renders "Previous pregnancies" as static 2-column `.fbox` cells without tappable row affordances, whereas `.fbox` sets `min-height: 48px` on every static non-tappable field.
  - Half-width (`~161dp`) 2-column cells in `Medical, surgical and family history` and `Medications` force multi-line text wrapping when clinical strings exceed ~20 characters (unless `.field--wide` is applied).

---

### Ideas From Each Mock Worth Merging Into the Winner (`mock_C.html`)

1. **From [mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html) (Base Winner)**:
   - The 5-family desaturated token palette (`--id-*`, `--dt-*`, `--ob-*`, `--md-*`, `--rx-*`) where every label clears 8.4:1 on its tint and 9.8:1 on white.
   - The 2-column adaptive grid with `.field--wide` (`grid-column: 1 / -1`) spans for longer values.
   - The 3-column, 2-row patient card metric grid (`GA`, `Parity`, `BP` on row 1; `Blood group` + 2-column `Due date` on row 2).
   - Native container-level `dir="rtl" lang="ar"` mirroring paired with explicit `dir="ltr"` isolation on numeric/blood-group tokens (`A+`, `110/70`, `11/02/2027`).
2. **From [mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html)**:
   - **Tappable 48dp rows for Previous Pregnancies** (`.pp-row`, [mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L72-L75)): Previous pregnancies are discrete historical records that clinicians tap to edit individually; rendering each pregnancy as a full-width tappable `min-height: 48dp` row with an explicit trailing `Edit` action (styled in the `fam-ob` tint and `16sp` value text) fits the brief's *"rows touch >= 48dp where tappable"* rule better than `mock_C`'s static 2-column boxes, while allowing read-only non-tappable `.fbox` cells to use compact vertical padding (`min-height: 38dp–40dp`).
   - **Card-level section containment (`.sec`)**: Wrapping each Overview section in an outlined card (`background: #FFFFFF; border: 1px solid var(--line); border-radius: 16dp`) on a `#F2F4F8` scaffold background prevents adjacent same-family sections (`Obstetric history` and `Previous pregnancies`) from merging into each other.
   - **48dp × 48dp `.edit-btn` sizing**: `min-height: 48px; min-width: 48px` ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L67)) fixes `mock_C`'s narrow `28px`-wide `.edit` target.
3. **From [mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html)**:
   - **Negative-margin 48dp overflow `IconButton` in compact card header** (`.menu-btn`, [mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L379-L393)): Using a `48dp × 48dp` `<button type="button" aria-label="...">` with negative vertical bleed (`margin: -10px -8px -10px 0` in LTR / `margin-inline-end: -8px` in logical properties) preserves a full 48dp×48dp touch target without inflating the visual height of the patient card's top row.
   - **Leading 4dp severity bar paired with the text chip** (`.severity-bar`, [mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L310-L321)): Because it sits as a flex child (`flex-shrink: 0`) rather than `position: absolute; left: 0`, it automatically mirrors to the right edge in RTL (`direction: rtl`) while the text status chip satisfies the "never by colour alone" rule.

---

### Concrete Defects Catalogue (Every File)

#### Defects in [mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html)
1. **Missing 5 of 6 Overview sections**: Only mocks "Medical, surgical & family" ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L547-L580)); omits Identity, Pregnancy dating, Obstetric history, Previous pregnancies, and Medications.
2. **Dead CSS and broken sibling selector** ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L16-L18), [L260-L267](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L260-L267)): `--dating-*` variables and `.field-box.dating-family` are never used in the HTML, and `.field-box.dating-family + .field-label` targets a `.field-label` *after* `.field-box`, whereas `.field-label` precedes `.field-box` in the DOM.
3. **RTL BiDi `+A` reversal** ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L712), [L743](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L743)): `<span class="metric-sub">فصيلة A+</span>` inside `.rtl-card` (`direction: rtl`) lacks `dir="ltr"` or `<bdi>`, rendering `A+` visually as `+A`.
4. **RTL flex-end vs text-align conflict** ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L431-L434)): `.metric-item.align-end` hardcodes `align-items: flex-end; text-align: right;`. In RTL, `flex-end` aligns items to the left edge while `text-align: right` aligns text to the right edge.
5. **Demoted `Blood group` and `Due date` metrics** ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L648-L664)): `EDD 11/02/2027` and `Blood A+` are rendered as `11px` muted subtext (`.metric-sub`) without dedicated labels, while column 3 invents `Normal BP`.
6. **Box-shadow violation** ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L126)): `.phone-frame` uses `box-shadow: 0 2px 8px rgba(20, 30, 50, 0.08)`.
7. **Inaccurate contrast annotation & sub-4.5:1 Before label** ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L208), [L583](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L583)): `.before-label` (`#6C7A8C` on `#FFFFFF`) is 4.38:1, and the After note claims `"Contrast 7.8:1 on labels"` when `#22375D` is 11.85:1 on `#FFFFFF` and 10.74:1 on `#F0F4FA`.
8. **Missing `aria-label` on Before menu buttons** ([mock_A.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L609), [L695](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_A.html#L695)): Present on After cards, omitted on Before cards.

#### Defects in [mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html)
1. **Phone frame width is `376px`, not `360px`** ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L38)): `.phone { width: 376px }`.
2. **Kebab overflow fails 48dp touch target** ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L101-L102)): `.kebab` is a non-button `<span>` measuring `32px × 19px` inside a `21.25px`-high `.pc-head`.
3. **RTL severity strip pinned to physical left edge** ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L84-L88), [L305](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L305)): `.strip` uses `left: 0` and `.pcard-inner` uses `padding-left: 10px`, placing the severity bar on the trailing (left) side of the Arabic RTL card behind the kebab menu.
4. **RTL BiDi `+A` reversal** ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L316)): `<span class="val">A+</span>` inside `.metric.ar` (`direction: rtl`) has no `dir="ltr"`, rendering visually as `+A`.
5. **Arabic card expands from 2 metric rows to 3 rows** ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L108), [L312](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L312)): `.metric-row-ar` uses a 2-column grid (`2+2+1`) instead of the 2-row (`3+2`) layout used in English.
6. **Section heading smaller than field value** ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L56), [L66](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L66)): `.sec-title` is `15px` while `.fld` is `16px`.
7. **Previous pregnancies section drops family tint and 16sp value size** ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L72-L75), [L187-L192](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L187-L192)): `.pp-row .txt` is `15px` (below 16sp) on a white background with no `.lab` or `--purple-box` fill.
8. **Duplicate Obstetric history data** ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L178-L183)): Renders `Gravida 4, Para 1, Abortions 2` in `.chip` and immediately repeats `G4`, `P1`, `A2` in `.grid3`.
9. **Incomplete Before comparisons and dropped card fields**: Overview Before ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L127-L143)) shows only 2 of 6 sections; Dashboard Before omits Arabic RTL; `Salma Hassan` card ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L280-L294)) omits `Blood group` and `Due date`.
10. **Third radius token** ([mock_B.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_B.html#L102)): `.kebab i` uses `border-radius: 3px` alongside `--r-sm: 10px` and `--r-lg: 20px`.

#### Defects in [mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html)
1. **RTL metric value left-alignment bug** ([mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L404-L408), [L451-L455](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L451-L455)): `<div class="mval" dir="ltr">` sets `dir="ltr"` on a block `<div>` without `text-align: right`. In HTML/CSS, `[dir="ltr"]` resets default `text-align: start` to **left**, so every metric value in the Arabic card aligns to the left of its column while its Arabic `.mlabel` above it aligns to the right (most glaring on the 2-column `.metric--span2` due date cell).
2. **`.edit` touch target width is ~28px (< 48dp)** ([mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L86-L91)): `.edit` specifies `min-height: 48px; padding: 0 2px` without `min-width: 48px`, making the tap target ~`28px × 48px`. Both `.edit` and `.pmore` use non-interactive `<span>` elements instead of `<button>`.
3. **Untranslated English text inside Arabic RTL card** ([mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L402), [L404](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L404), [L447](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L447), [L451](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L451)): Status tag `Incomplete` and unit `wks` remain in English inside `<article class="pcard" dir="rtl" lang="ar">`.
4. **Section heading smaller than field value** ([mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L83), [L125](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L125)): `.shead h3` is `15px` (`#171B26`), whereas `.fbox` value text is `16px` (`#16203A`).
5. **Same font-weight (`600`) on both `.flabel` and `.fbox`** ([mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L118), [L125](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L125)): Both field labels and field values use `font-weight: 600`, making multi-word prose values overly bold.
6. **Non-tappable static fields forced to `min-height: 48px` while Previous pregnancies lack tap affordances** ([mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L123), [L330-L337](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L330-L337)): Every static read-only `.fbox` has `min-height: 48px`, while "Previous pregnancies" items are rendered as static `.fbox` cells (`2022, loss` / `2021, loss` side-by-side) with no per-row tap indicator.
7. **Half-width (`161px`) columns on free-text Medical/Medication fields** ([mock_C.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/reviews/owner_fixes_2026-11-15/mock_C.html#L339-L355)): `Medical`, `Surgical`, `Family`, `Allergy`, and `Medications` use 2-column cells without `.field--wide`, causing `Ferrous sulphate` (`200 mg, alternate days`) and `Family` (`Mother, type 2 diabetes`) to wrap across multiple lines in uneven row heights.
