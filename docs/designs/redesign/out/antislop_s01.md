### Delivery Gate Audit Report: `docs/designs/redesign/out/s01_diff.diff`

**Mode**: DURING  
**Declared Dials**: ENERGY 1 / RHYTHM 1 / MOTION 1  
**Scope**: Slices 0 & 1 (`s01_diff.diff`, 12 files)

---

### Rule Evaluation (C-1..C-5, R-01..R-40)

- **C-1**: PASS: Every visual and copy decision traces to Mock D / VDDS domain-scoped tokens with documented rationale.
- **C-2**: PASS: All interactive elements (`patientMenu`, `incompleteChipAction`, `SectionTextAction`, `RecordRow`, `Card`) bind to active event dispatchers.
- **C-3**: PASS: All sections represent antenatal clinical domains; no filler or landing page template sections.
- **C-4**: FAIL: `app/src/main/java/com/sherifradwan/ancnative/ui/components/LabeledValue.kt:53` (unbounded `.weight(1f)` causes runtime crash inside `verticalScroll`); `app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt:174` (sub-3.0:1 non-text boundary contrast in dark mode).
- **C-5**: PASS: Only real patient clinical fields and explicit clinical missing markers (`--/--`, `Not recorded`) displayed; no fabricated statistics or social proof.
- **R-01**: PASS: No unauthorized gradients, radial orbs, colored glows, or pastel default palettes.
- **R-02**: PASS: No em dash (`—`) characters present across any added text or strings.
- **R-03**: FAIL: `app/src/main/java/com/sherifradwan/ancnative/ui/components/LabeledValue.kt:53` (layout failure on vertically scrollable screens); `app/src/main/java/com/sherifradwan/ancnative/ui/components/RecordSectionCard.kt:80` (hardcoded positive x-offset breaks RTL layout alignment).
- **R-04**: PASS: Standard utility and clinical icons only (`MoreVert`, `Warning`); no generic AI glyphs.
- **R-05**: PASS: Structured clinical micro-grids and domain sections; complies with RHYTHM 1 dial.
- **R-06**: PASS: Standard clinical typography (`RecordType`); sentence case applied (`high_risk` -> `"High risk"`); no decorative monospace or extreme tracking.
- **R-07**: PASS: Solid neutral clinical surfaces; no blueprint, grid, or dot backgrounds.
- **R-08**: PASS: No arrow glyphs (`→`, `↗`) attached to action buttons or chips.
- **R-09**: PASS: Semantic clinical status tags only; no promotional capsule badges.
- **R-10**: PASS: Zero glassmorphism or backdrop-filter blurs.
- **R-11**: PASS: Hierarchical border radii (`RecordShapes.Small` 10.dp for components, `RecordShapes.Large` 16.dp for cards); no uniform pill styling.
- **R-12**: PASS: Flat outlined cards with 1.dp border; no floating ambient drop shadows.
- **R-13**: PASS: Zero glow effects across cards, borders, and chips.
- **R-14**: PASS: Sections vary internal composition based on clinical content hierarchy.
- **R-15**: PASS: Action verbs are context-specific ("Edit", "Add", "Select date"); no generic CTAs.
- **R-16**: PASS: Pure medical terminology; zero marketing buzzwords.
- **R-17**: PASS: Displays only real clinical entities and source-backed vitals; no vanity numbers.
- **R-18**: PASS: No testimonial sections.
- **R-19**: PASS: Complies with MOTION 1 dial; no unsolicited entry or floating animations.
- **R-20**: PASS: Visual Distinction Design System (VDDS) with 4 domain field families establishes distinct identity.
- **R-21**: PASS: Full dark theme token parity implemented across field families and status tags.
- **R-22**: PASS: No generic illustrations or 3D characters.
- **R-23**: PASS: All visual assets use native Material components; no unconfirmed placeholders.
- **R-24**: PASS: No phantom navigation links.
- **R-25**: FAIL: `app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt:164,167,174` (status tag border tokens fail 3.0:1 non-text contrast standard).
- **R-26**: PASS: Every button, row, and tag click executes a functional callback or navigation event.
- **R-27**: PASS: Empty and missing states explicitly handled for previous pregnancies, allergies, medical histories, and vitals.
- **R-28**: PASS: No FAQ section.
- **R-29**: PASS: Restricted to VDDS domain-scoped tokens and Material semantic roles.
- **R-30**: PASS: Implements product owner's custom clinical EMR specification (Mock D).
- **R-31**: FAIL: `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientDirectoryCard.kt:683` (comment restates obvious code parameters, violating antislop-code).
- **R-32**: PASS: Standard focusable Compose nodes; no focus suppression.
- **R-33**: PASS: All modifications implemented directly in Kotlin and XML sources.
- **R-34**: FAIL: `app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt:174` (dark mode `StatusIncompleteDarkBorder` #5B6578 on #2A3140 is 2.22:1 and on dark surface #1A202E is 2.77:1, violating dark mode accessibility).
- **R-35**: PASS: Automated Compose test suites executed and verified.
- **R-36**: PASS: No fabricated compliance, security, or performance claims.
- **R-37**: PASS: Guided by owner-approved Mock D design direction with declared dials.
- **R-38**: PASS: Honest missing labels (`"Not recorded"`, `"--/--"`) used for unrecorded patient data.
- **R-39**: FAIL: `app/src/main/java/com/sherifradwan/ancnative/ui/components/RecordSectionCard.kt:80` (hardcoded positive horizontal offset does not mirror for RTL layout direction).
- **R-40**: PASS: Layout components allocate reserved dimensions; no unmeasured performance metrics.

---

### Contrast Table: `Field*` and `Status*` Tokens (`Theme.kt`)

*Computed via `python3 ~/.claude/skills/antislop-human/contrast-check.py "#fg" "#bg"`*

| Token Pair | Foreground | Background | Theme | Ratio | Normal (4.5:1) | Large / Non-Text (3.0:1) | Verdict |
|---|---|---|---|---|---|---|---|
| **Field Family: Label on Container** | | | | | | | |
| `FieldPatientLabel` / `FieldPatientBg` | `#23407A` | `#E9EEF8` | Light | 8.65:1 | PASS | PASS | PASS |
| `FieldObstetricLabel` / `FieldObstetricBg` | `#1D5149` | `#E3F0EE` | Light | 7.73:1 | PASS | PASS | PASS |
| `FieldBackgroundLabel` / `FieldBackgroundBg` | `#594A2C` | `#F1EDE4` | Light | 7.36:1 | PASS | PASS | PASS |
| `FieldDeliveryLabel` / `FieldDeliveryBg` | `#4E3A78` | `#EEE9F5` | Light | 8.00:1 | PASS | PASS | PASS |
| `FieldPatientDarkLabel` / `FieldPatientDarkBg` | `#AFC6F2` | `#22304A` | Dark | 7.66:1 | PASS | PASS | PASS |
| `FieldObstetricDarkLabel` / `FieldObstetricDarkBg` | `#9FD6CC` | `#1D3836` | Dark | 7.76:1 | PASS | PASS | PASS |
| `FieldBackgroundDarkLabel` / `FieldBackgroundDarkBg` | `#E0CFA6` | `#36301F` | Dark | 8.53:1 | PASS | PASS | PASS |
| `FieldDeliveryDarkLabel` / `FieldDeliveryDarkBg` | `#CDBDF0` | `#2E2842` | Dark | 8.10:1 | PASS | PASS | PASS |
| **Field Family: Label on Card Surface** | | | | | | | |
| `FieldPatientLabel` / `Surface` | `#23407A` | `#FFFFFF` | Light | 10.07:1 | PASS | PASS | PASS |
| `FieldObstetricLabel` / `Surface` | `#1D5149` | `#FFFFFF` | Light | 9.03:1 | PASS | PASS | PASS |
| `FieldBackgroundLabel` / `Surface` | `#594A2C` | `#FFFFFF` | Light | 8.60:1 | PASS | PASS | PASS |
| `FieldDeliveryLabel` / `Surface` | `#4E3A78` | `#FFFFFF` | Light | 9.54:1 | PASS | PASS | PASS |
| `FieldPatientDarkLabel` / `Surface` | `#AFC6F2` | `#1A202E` | Dark | 9.44:1 | PASS | PASS | PASS |
| `FieldObstetricDarkLabel` / `Surface` | `#9FD6CC` | `#1A202E` | Dark | 10.05:1 | PASS | PASS | PASS |
| `FieldBackgroundDarkLabel` / `Surface` | `#E0CFA6` | `#1A202E` | Dark | 10.58:1 | PASS | PASS | PASS |
| `FieldDeliveryDarkLabel` / `Surface` | `#CDBDF0` | `#1A202E` | Dark | 9.41:1 | PASS | PASS | PASS |
| **Status Tag: Text on Background** | | | | | | | |
| `StatusIncompleteText` / `StatusIncompleteBg` | `#333B4A` | `#EEF0F4` | Light | 9.87:1 | PASS | PASS | PASS |
| `StatusModerateText` / `StatusModerateBg` | `#6E4708` | `#FCEFD6` | Light | 7.19:1 | PASS | PASS | PASS |
| `StatusHighText` / `StatusHighBg` | `#8E1B1B` | `#FBE3E3` | Light | 7.40:1 | PASS | PASS | PASS |
| `StatusIncompleteDarkText` / `StatusIncompleteDarkBg` | `#D5DAE3` | `#2A3140` | Dark | 9.29:1 | PASS | PASS | PASS |
| `StatusModerateDarkText` / `StatusModerateDarkBg` | `#F3D18E` | `#3A2A0E` | Dark | 9.44:1 | PASS | PASS | PASS |
| `StatusHighDarkText` / `StatusHighDarkBg` | `#FFC9C9` | `#4A1616` | Dark | 10.19:1 | PASS | PASS | PASS |
| **Status Tag: Border on Background (WCAG 1.4.11 >= 3.0:1)** | | | | | | | |
| `StatusIncompleteBorder` / `StatusIncompleteBg` | `#8C95A5` | `#EEF0F4` | Light | 2.65:1 | FAIL | FAIL | **FAIL** |
| `StatusModerateBorder` / `StatusModerateBg` | `#B9862C` | `#FCEFD6` | Light | 2.84:1 | FAIL | FAIL | **FAIL** |
| `StatusHighBorder` / `StatusHighBg` | `#C26666` | `#FBE3E3` | Light | 3.19:1 | FAIL | PASS | PASS |
| `StatusIncompleteDarkBorder` / `StatusIncompleteDarkBg` | `#5B6578` | `#2A3140` | Dark | 2.22:1 | FAIL | FAIL | **FAIL** |
| `StatusModerateDarkBorder` / `StatusModerateDarkBg` | `#A87A2A` | `#3A2A0E` | Dark | 3.61:1 | FAIL | PASS | PASS |
| `StatusHighDarkBorder` / `StatusHighDarkBg` | `#B45A5A` | `#4A1616` | Dark | 3.21:1 | FAIL | PASS | PASS |
| **Status Tag: Border on Card Surface (WCAG 1.4.11 >= 3.0:1)** | | | | | | | |
| `StatusIncompleteBorder` / `Surface` | `#8C95A5` | `#FFFFFF` | Light | 3.02:1 | FAIL | PASS | PASS |
| `StatusModerateBorder` / `Surface` | `#B9862C` | `#FFFFFF` | Light | 3.23:1 | FAIL | PASS | PASS |
| `StatusHighBorder` / `Surface` | `#C26666` | `#FFFFFF` | Light | 3.90:1 | FAIL | PASS | PASS |
| `StatusIncompleteDarkBorder` / `Surface` | `#5B6578` | `#1A202E` | Dark | 2.77:1 | FAIL | FAIL | **FAIL** |
| `StatusModerateDarkBorder` / `Surface` | `#A87A2A` | `#1A202E` | Dark | 4.25:1 | FAIL | PASS | PASS |
| `StatusHighDarkBorder` / `Surface` | `#B45A5A` | `#1A202E` | Dark | 3.53:1 | FAIL | PASS | PASS |

---

### Numbered FAIL List (Most Serious First)

1. [file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/components/LabeledValue.kt#L53](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/components/LabeledValue.kt#L53)  
   **Defect**: `Surface` inside `LabeledValue` applies `.weight(1f, fill = true)` inside a `Column` that receives unbounded height constraints when embedded in `OverviewTab`'s `verticalScroll()`, triggering an `IllegalStateException` layout crash at runtime.  
   **Fix**: Remove `.weight(1f, fill = true)` from `Surface` in `LabeledValue.kt` and allow row height equalization to occur via `LabeledValueRow`'s `IntrinsicSize.Min`.

2. [file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt#L164](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt#L164) & [file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt#L167](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt#L167) & [file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt#L174](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt#L174)  
   **Defect**: Status tag border tokens fail WCAG 2.1 SC 1.4.11 non-text contrast (< 3.0:1) against their backgrounds and surfaces (`StatusIncompleteBorder` is 2.65:1, `StatusModerateBorder` is 2.84:1, `StatusIncompleteDarkBorder` is 2.22:1 on bg and 2.77:1 on surface).  
   **Fix**: Darken light border tokens (`StatusIncompleteBorder` to `#757E8E` [3.56:1], `StatusModerateBorder` to `#9E6F1E` [3.68:1]) and lighten dark border token (`StatusIncompleteDarkBorder` to `#7D889E` [3.82:1 on bg, 4.49:1 on surface]).

3. [file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/components/RecordSectionCard.kt#L80](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/components/RecordSectionCard.kt#L80)  
   **Defect**: `SectionTextAction` specifies a hardcoded horizontal offset `.offset(x = 12.dp)`, which does not adapt to RTL layout direction and shifts the action inward toward the title in Arabic.  
   **Fix**: Remove `.offset(x = 12.dp)` and manage edge alignment using standard layout padding and end alignment.

4. [file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientDirectoryCard.kt#L683](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientDirectoryCard.kt#L683)  
   **Defect**: Comment `// Name row: 48dp min touch target height, vertically centered` merely restates the explicit parameters of the following `Row` declaration, violating comment hygiene rules.  
   **Fix**: Delete the redundant comment line.
