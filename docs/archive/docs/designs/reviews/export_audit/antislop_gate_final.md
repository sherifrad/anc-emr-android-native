antislop active: after (session override).

### Delivery Gate Report

#### Block 1: Hard Gate (Absolute)
- R-02 PASS: Zero em dashes (`—`) or en dashes (`–`) in `report.html`, `PatientPrintHtmlGenerator.kt`, `ReportSections.kt`, `PatientShareText.kt`, or `PatientTwoPageReportDialog.kt` (`PatientPrintHtmlGenerator.kt:38-39`: `"Raised BP - recheck"`, `"Severe-range BP - urgent assessment"`, `line 76`: `"Specialist of Obstetrics, Gynecology & Laparoscopy"`).
- R-03 PASS: `@media screen and (max-width: 540px)` collapses multi-column grids (`report.html:100-122`), wraps wide tables in `.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }` (`report.html:123-126`), wraps long unbroken strings via `overflow-wrap: anywhere;` (`report.html:553,561`, verified visually in `print_page1.png` and `print_page2.png`), and enforces 48dp (>= 44px) touch targets on all preview dialog controls (`PatientTwoPageReportDialog.kt:98,136,158`: `Modifier.minimumTouchTarget()`).
- R-17 PASS: Zero fabricated statistics or numbers; all values derive from patient domain records (`PatientPrintHtmlGenerator.kt:78-100,938-1010`, `ReportSections.kt:48-68`).
- R-18 PASS: Zero testimonials, avatars, or fictional reviews in the document or source files.
- R-23 PASS: Clinic logo uses explicit resource `R.drawable.clinic_logo_mark` (`PatientPrintHtmlGenerator.kt:52`, rendered in `print_page1.png` and `print_page2.png`), Arabic signature uses `fonts/ArslanWessamB.ttf` (`report.html:8`, rendered in `print_page3.png`), and the stamp box uses an honest placeholder (`report.html:1187`: `<div class="stamp-placeholder">Stamp</div>`).
- R-24 N/A: Printed clinical document and in-app preview dialog have no navigation bar.
- R-25 PASS: Every text and background color pair in `report.html` and `PatientTwoPageReportDialog.kt` meets WCAG AA (>= 4.5:1 normal text, >= 3.0:1 large text); `--danger-text: #B91C1C` (`report.html:21`, `PatientPrintHtmlGenerator.kt:296`) achieves 5.30:1 on `--danger-bg: #FEE2E2` (`.badge-danger`) and 5.91:1 on `#FEF2F2` (`.val-abnormal`).
- R-26 PASS: All preview dialog controls invoke real actions (`PatientTwoPageReportDialog.kt:97`: `onClick = onDismiss`, `line 127`: `context.startActivity(Intent.createChooser(shareIntent, "Share Clinical Record"))`, `line 144`: `PatientPrintManager.printPatientReport(...)`).
- R-27 PASS: Every clinical section defines an explicit empty state (`PatientPrintHtmlGenerator.kt:109`: `"No clinical visits recorded yet"`, `line 137`: `"No ultrasound scans recorded yet"`, `line 167`: `"No laboratory test results recorded yet"`, `line 204`: `"No current medications recorded"`, `line 209`: `"No previous pregnancies or deliveries recorded"`).
- R-28 N/A: Printed clinical record contains no FAQ section.
- R-32 PASS: Preview dialog supports Back/Escape dismissal (`PatientTwoPageReportDialog.kt:68`: `dismissOnBackPress = true`), and Compose `IconButton`/`Button` controls (`lines 96, 126, 142`) provide native keyboard focus and activation.
- R-33 PASS: All HTML, CSS, and Kotlin are written directly in source (`PatientPrintHtmlGenerator.kt`, `ReportSections.kt`, `PatientShareText.kt`, `PatientTwoPageReportDialog.kt`); zero external patch scripts exist.
- R-34 PASS: Preview dialog `"Print PDF"` button pairs `containerColor = MaterialTheme.colorScheme.secondary` with `contentColor = MaterialTheme.colorScheme.onSecondary` (`PatientTwoPageReportDialog.kt:150-153`), achieving 5.98:1 in Light Theme (`#FFFFFF` on `#934F5B`) and 7.74:1 in Dark Theme (`#5A1826` on `#FFB2BC`).
- R-35 PASS: Verified by static inspection of `report.html`, `PatientPrintHtmlGenerator.kt`, `ReportSections.kt`, `PatientShareText.kt`, and `PatientTwoPageReportDialog.kt`, and visual inspection of rendered outputs `print_page1.png`, `print_page2.png`, and `print_page3.png` (Close -> `onDismiss`, Share -> `ACTION_SEND` chooser with `patientShareText(patient)`, Print PDF -> `PatientPrintManager.printPatientReport`).
- R-36 PASS: Zero fabricated compliance, security, or performance claims; header badge reads `"CLINICAL RECORD"` (`report.html:655`) and footer reads `"Confidential medical record"` (`report.html:892`).
- R-37 PASS: Honors explicit owner direction: obstetric clinic medical record with dials ENERGY 1 / RHYTHM 1 / MOTION 1, clinic logo palette (`#0D1B3D`, `#B34A60`, `#176B70`), calligraphic Arabic signature (`report.html:8,385,1185-1186`), clinic phone `01201720808` (`PatientPrintHtmlGenerator.kt:35`, `report.html:892,1196`), and `"Specialist"` title (`PatientPrintHtmlGenerator.kt:76`, `report.html:651`).
- R-38 PASS: Missing clinical fields use honest placeholders (`ReportSections.kt:20`: `NOT_RECORDED = "Not recorded"`, `line 33`: `"Risk not recorded"`, `PatientPrintHtmlGenerator.kt:940`: `"Unspecified"`, `report.html:1187`: `"Stamp"`).
- R-39 PASS: Document sets `<html lang="en" dir="ltr">` (`report.html:2`), unique `<title>` (`report.html:6`), zoomable viewport (`report.html:5`), `<header>`/`<section>`/`<footer>` landmarks (`report.html:645,663,891`), and `<img alt="Clinic Logo">` (`PatientPrintHtmlGenerator.kt:262`); preview dialog sets `contentDescription = "Close"` (`PatientTwoPageReportDialog.kt:100`), `contentDescription = "Share"` (`line 138`), and `contentDescription = null` on the decorative icon inside the `"Print PDF"` button (`line 162`).
- R-40 PASS: Clinic logo `<img>` declares explicit `width="46" height="64"` HTML attributes (`PatientPrintHtmlGenerator.kt:262`) matching `.clinic-logo { width: 46px; height: 64px; }` (`report.html:215-219`), `<head>` contains zero render-blocking scripts (`report.html:3-638`), and no unmeasured performance metrics are claimed.

#### Block 2: Purpose-Gate (Technique Allowed, Reason Required)
- R-01 PASS: Zero decorative gradients or glows (`report.html:9-24`); solid brand hexes `--navy-primary: #0D1B3D`, `--rose-accent: #B34A60`, and `--teal-clinical: #176B70` derive from the clinic logo to separate primary headings, clinical alerts, and gestational age values.
- R-04 PASS: Zero generic AI icons (sparkle, orb, robot) or decorative emoji; preview dialog uses only functional Material icons (`PatientTwoPageReportDialog.kt:20-22`: `Icons.Default.Close`, `Print`, `Share`).
- R-06 PASS: Document uses system sans-serif for dense clinical legibility (`report.html:33`), `.date-cell` uses `font-variant-numeric: tabular-nums; font-size: 7.8pt; white-space: nowrap;` (`report.html:540`) for tabular date alignment without monospace fonts, and `.doc-sign` uses owner-specified `'Arslan Wessam B'` (`report.html:8,385`) for the Arabic signature.
- R-07 PASS: Zero decorative background grids or dot patterns (`report.html:43-66`); white paper surface uses only a 4.5% opacity clinic logo watermark (`report.html:59`: `opacity: 0.045;`).
- R-08 PASS: Zero decorative button arrows (`→`, `↗`) in the document or preview dialog.
- R-09 PASS: Badges are strictly functional clinical status indicators (`High risk`, `Medium risk`, `Low risk`, `Rh negative` at `report.html:344-377`, `.photo-badge` at `line 569`, and `CLINICAL RECORD` / `CONTINUED` at `lines 655,909`) with no glow or pulsing dot.
- R-10 PASS: Zero glassmorphism or `backdrop-filter` in `report.html:7-637`.
- R-12 PASS: `@media print` strips shadows (`report.html:147`: `box-shadow: none;`), while `@media screen` applies a single elevation shadow to `.page-sheet` (`report.html:93`: `box-shadow: 0 4px 14px rgba(13, 27, 61, 0.12);`) to separate paper sheets from the `#E2E8F0` canvas.
- R-13 PASS: Zero glow effects on cards, buttons, badges, or borders (`report.html:7-637`).
- R-14 PASS: Section layouts vary by clinical domain structure: 4-column `.patient-card` (`report.html:293`), 3-column `.dating-matrix` (`line 422`), key-value `.history-list` (`line 458`), and fixed-column `.clinical-table` logs (`line 486`).
- R-19 PASS: Matches declared MOTION 1 dial with zero animations, transitions, or keyframes (`report.html:7-637`).
- R-22 PASS: Zero generic Undraw, Storyset, or 3D blob illustrations.

#### Block 3: Liveliness (Required to Be Alive, Not Just Clean)
- L-01 PASS: Dials explicitly set to ENERGY 1 / RHYTHM 1 / MOTION 1 per owner direction.
- L-02 PASS: Output is consistent with ENERGY 1 / RHYTHM 1 / MOTION 1: calm, dense, predictable tabular and key-value clinical hierarchy with zero motion (`print_page1.png`, `print_page2.png`, `print_page3.png`).
- L-03 PASS: Clear focal point per sheet: Page 1 anchors on `.patient-card` with risk badge (`report.html:663`, `print_page1.png`), and the closing sheet anchors on `.attestation-grid` with the Delivery Plan and calligraphic Arabic signature (`report.html:1168`, `print_page3.png`).
- L-04 PASS: Structural whitespace (`report.html:73`: `gap: 9px;`, `line 407`: `padding: 8px 10px;`) separates clinical domains without wasted vertical space.
- L-05 PASS: Single deliberate accent (`--rose-accent: #B34A60`) highlights key clinical items only: EDD (`report.html:696`), `.ga-highlight` (`line 455`), recorded drug allergies (`line 714`), and the Page 2 `CONTINUED` badge (`line 909`).
- L-06 PASS: Distinct identity motif pairing the clinic logo mark (`print_page1.png`, `print_page2.png`) with the calligraphic Arabic signature (`Arslan Wessam B`, `report.html:8,385,1185-1186`, `print_page3.png`).
- L-07 PASS: Design Read declared: A4 printed obstetric medical record and 360dp in-app preview for clinicians and patients, in a dense clinical typography style, dial ENERGY 1 / RHYTHM 1 / MOTION 1.

#### Block 4: Craftsmanship & Quality Locks
- C-1 PASS: Every visual and wording decision serves clinical record density, safety, or explicit owner direction.
- C-2 PASS: All interactive controls in `PatientTwoPageReportDialog.kt` (`Close`, `Share`, `Print PDF`) execute real actions (`lines 97, 127, 144`).
- C-3 PASS: Every section maps to a required obstetric record domain; zero template filler sections exist.
- C-4 PASS: Holds up across print (`print_page1.png`, `print_page2.png`, `print_page3.png`), 360dp phone preview (`report.html:99-130`), missing-logo text fallback (`.logo-fallback` at `report.html:221-227`), long unbroken text (`overflow-wrap: anywhere` at `report.html:553,561`), and Light/Dark dialog themes (`PatientTwoPageReportDialog.kt:150-153`).
- C-5 PASS: All displayed values come from real patient records (`ReportSections.kt`, `PatientShareText.kt`); zero fabricated claims.
- R-05 PASS: Matches declared RHYTHM 1 dial with domain-driven clinical tables and key-value summaries; zero bento grids, fake terminals, or 3-tier pricing layouts.
- R-11 PASS: Consistent 3px–6px border radii (`report.html:296`: `6px`, `line 405`: `5px`, `lines 268,347`: `4px`, `line 393`: `3px`); zero pill-shaped cards or tables.
- R-15 PASS: Preview dialog primary CTA is action-specific (`PatientTwoPageReportDialog.kt:167`: `"Print PDF"`).
- R-16 PASS: Printed and shared text uses concise medical terminology (`ReportSections.kt`, `PatientShareText.kt`); zero marketing buzzwords.
- R-20 PASS: Distinct clinic identity from the Navy/Rose/Teal logo palette, obstetric domain structure, and calligraphic Arabic signature block.
- R-21 PASS: Fixed white paper surface (`--surface-white: #FFFFFF`) is justified for an A4 printed medical record, while the dialog shell supports Light and Dark themes.
- R-29 PASS: Active palette is capped at 3 brand colors (`#0D1B3D`, `#B34A60`, `#176B70`) plus neutral slate/white surfaces and semantic alert tints (`report.html:9-24`).
- R-30 PASS: Custom obstetric clinical record layout; does not clone Linear, Vercel, Stripe, or Notion.
- R-31 PASS: Every visual decision has a one-line clinical or brand purpose, all boastful CSS banner comments were removed from `PatientPrintHtmlGenerator.kt`, and the 4 KDoc comments in `ReportSections.kt:41,78,134,170` concisely document non-obvious domain rules.

---

### Verification of Earlier FAIL Items (`docs/designs/reviews/export_audit/antislop_gate_gemini.md`)

1. **R-34 & C-4 (Finding #1 — Dark Theme `"Print PDF"` button contrast was 1.70:1)**: **FIXED**. [PatientTwoPageReportDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientTwoPageReportDialog.kt#L150-L153) now sets `containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary`, producing 7.74:1 in Dark Theme (`#5A1826` on `#FFB2BC`) and 5.98:1 in Light Theme (`#FFFFFF` on `#934F5B`).
2. **R-25 (Finding #2 — `.badge-danger` at 3.95:1 and `.val-abnormal` at 4.41:1 with `#DC2626`)**: **FIXED**. [PatientPrintHtmlGenerator.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt#L296) and [report.html](file:///Users/sherifgamalradwan/anc-emr-android-native/app/build/print-sample/report.html#L21) now set `--danger-text: #B91C1C;`, raising `.badge-danger` (`#B91C1C` on `#FEE2E2`) to 5.30:1 and `.val-abnormal` (`#B91C1C` on `#FEF2F2`) to 5.91:1 (verified visually on `"High risk"` in `print_page1.png` and `"11.2 g/dL (Abnormal)"` in `print_page2.png`).
3. **R-39 (Findings #3 & #7 — Redundant `contentDescription = "Print PDF"` on button icon and document landmarks)**: **FIXED**. [PatientTwoPageReportDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientTwoPageReportDialog.kt#L160-L164) now sets `contentDescription = null` on `Icon(imageVector = Icons.Default.Print, ...)`, while each printed sheet in [report.html](file:///Users/sherifgamalradwan/anc-emr-android-native/app/build/print-sample/report.html#L645-L893) uses `<header>`, `<section>`, and `<footer>` landmarks with `<html lang="en" dir="ltr">` and `<title>`.
4. **C-4 (Finding #4 — Unstyled `.logo-fallback` when `logoBase64` is empty)**: **FIXED**. [PatientPrintHtmlGenerator.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt#L496-L502) and [report.html](file:///Users/sherifgamalradwan/anc-emr-android-native/app/build/print-sample/report.html#L221-L227) now define `.logo-fallback { color: var(--navy-primary); font-size: 14pt; font-weight: bold; width: 46px; text-align: center; }`.
5. **R-06 (Finding #5 — Unjustified `font-family: monospace` on `.date-cell`)**: **FIXED**. [PatientPrintHtmlGenerator.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt#L815) and [report.html](file:///Users/sherifgamalradwan/anc-emr-android-native/app/build/print-sample/report.html#L540) now define `.date-cell { font-variant-numeric: tabular-nums; font-size: 7.8pt; white-space: nowrap; }` (verified visually in the Date columns of `print_page1.png` and `print_page2.png`).
6. **R-40 (Finding #6 — Missing `width` and `height` HTML attributes on `<img class="clinic-logo">`)**: **FIXED**. [PatientPrintHtmlGenerator.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt#L262) now outputs `"""<img src="data:image/png;base64,$logoBase64" alt="Clinic Logo" class="clinic-logo" width="46" height="64" />"""`.
7. **R-31 (Finding #8 — Boastful CSS banner comments in `PatientPrintHtmlGenerator.kt`)**: **FIXED**. All CSS banner and restatement comments (`/* Pixel-Perfect ISO A4 Print / PDF Layout */`, `/* Header Meta: Perfectly uniform alignment */`, `/* History lines with Arabic on right, English/values on left */`, `/* Clinical Tables: Perfectly Centered Contents, Zero-Overflow Fixed Layout */`) were removed from [PatientPrintHtmlGenerator.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt#L282-L910) and [report.html](file:///Users/sherifgamalradwan/anc-emr-android-native/app/build/print-sample/report.html#L7-L637).

---

### Color Contrast Analysis

Every text and background color pairing in `report.html`, `PatientPrintHtmlGenerator.kt`, and `PatientTwoPageReportDialog.kt`, computed with the WCAG 2.x relative luminance formula:

| Foreground | Background | Context | Ratio | Normal Text (4.5:1) | Large Text (3.0:1) |
|---|---|---|---|---|---|
| Text main `#0F172A` | Surface white `#FFFFFF` | Body text & odd table cells (`report.html:18,512`) | 17.85:1 | PASS | PASS |
| Text main `#0F172A` | Even row `#FAFAFA` | Alternating table row text (`report.html:512,522`) | 17.10:1 | PASS | PASS |
| Text main `#0F172A` | Patient card `#F8FAFC` | Unrecorded risk fallback badge (`report.html:294,344`) | 17.00:1 | PASS | PASS |
| Navy primary `#0D1B3D` | Surface white `#FFFFFF` | Clinic `h1`, `.logo-fallback`, `.section-title`, `.doc-name`, odd row `.text-navy` (`report.html:10,222,230,411,609`) | 16.92:1 | PASS | PASS |
| White `#FFFFFF` | Navy primary `#0D1B3D` | Page 1 `.doc-badge` & dialog top bar (`report.html:263-264`, `PatientTwoPageReportDialog.kt:80-81`) | 16.92:1 | PASS | PASS |
| Navy primary `#0D1B3D` | Even row `#FAFAFA` | Even table row `.text-navy` (`report.html:522,535`) | 16.21:1 | PASS | PASS |
| Navy primary `#0D1B3D` | Patient card / matrix / plan `#F8FAFC` | `.pt-name-box h3`, `.data-value`, `.matrix-item .val`, `.plan-box .v` (`report.html:306,340,444,588`) | 16.17:1 | PASS | PASS |
| Navy primary `#0D1B3D` | Badge navy `#EFF6FF` | `.badge-navy` (`report.html:371-375`) | 15.55:1 | PASS | PASS |
| Navy primary `#0D1B3D` | Table header `#F1F5F9` | `.clinical-table th` (`report.html:495-497`) | 15.44:1 | PASS | PASS |
| White `#FFFFFF` | Status banner `#1E293B` | `.status-banner` (`report.html:379`) | 14.63:1 | PASS | PASS |
| Signature `#1E293B` | Surface white `#FFFFFF` | `.doc-sign` calligraphic Arabic signature (`report.html:385,599`) | 14.63:1 | PASS | PASS |
| White 85% (`#DADCE2`) | Clinical Navy `#0D1B3D` | Dialog patient subtitle (`PatientTwoPageReportDialog.kt:114`) | 12.23:1 | PASS | PASS |
| Attention text `#7C2D12` | Attention bg `#FFF7ED` | `.attention-box` (`report.html:381`) | 8.83:1 | PASS | PASS |
| Dark `onSecondary` `#5A1826` | Dark `secondary` `#FFB2BC` | Dialog `"Print PDF"` button in Dark Theme (`PatientTwoPageReportDialog.kt:150-153`, `Theme.kt:255-256`) | 7.74:1 | PASS | PASS |
| Text muted `#475569` | Surface white `#FFFFFF` | `.meta-row`, `.history-row .k`, `.stamp-placeholder`, `.page-footer`, odd row `.text-muted` (`report.html:19,280,474,621,632`) | 7.58:1 | PASS | PASS |
| Text muted `#475569` | Even row `#FAFAFA` | Even table row `.text-muted` / `.empty-cell` (`report.html:522,538-539`) | 7.26:1 | PASS | PASS |
| Text muted `#475569` | Card / matrix / plan `#F8FAFC` | `.pt-code`, `.data-label`, `.matrix-item .label`, `.plan-box .k` (`report.html:313,332,438,588`) | 7.24:1 | PASS | PASS |
| Loss badge text `#991B1B` | Loss badge bg `#FEE2E2` | Previous pregnancy loss badge (`PatientPrintHtmlGenerator.kt:236`) | 6.80:1 | PASS | PASS |
| Success text `#166534` | Success bg `#DCFCE7` | `.badge-success` (`report.html:22-23,359-363`) | 6.49:1 | PASS | PASS |
| Warning text `#92400E` | Warning bg `#FEF3C7` | `.badge-warning` (`report.html:377`) | 6.37:1 | PASS | PASS |
| Teal clinical `#176B70` | Surface white `#FFFFFF` | `.clinic-title-box p` & odd row `.text-teal` (`report.html:13,246,537`) | 6.23:1 | PASS | PASS |
| Light `onSecondary` `#FFFFFF` | Light `secondary` `#934F5B` | Dialog `"Print PDF"` button in Light Theme (`PatientTwoPageReportDialog.kt:150-153`, `Theme.kt:26,223-224`) | 5.98:1 | PASS | PASS |
| Teal clinical `#176B70` | Even row `#FAFAFA` | Even table row `.text-teal` (`report.html:522,537`) | 5.96:1 | PASS | PASS |
| Teal clinical `#176B70` | Patient card `#F8FAFC` | `.pt-phone` (`report.html:294,320`) | 5.95:1 | PASS | PASS |
| Teal clinical `#176B70` | Badge teal `#E6FFFA` | `.badge-teal` (`report.html:365-369`) | 5.94:1 | PASS | PASS |
| Danger text `#B91C1C` | Abnormal bg `#FEF2F2` | `.val-abnormal` (`report.html:21,388-395`) | 5.91:1 | PASS | PASS |
| Outcome text `#0F766E` | Surface white `#FFFFFF` | Previous pregnancy Live Birth odd row (`report.html:750`) | 5.47:1 | PASS | PASS |
| Danger text `#B91C1C` | Danger bg `#FEE2E2` | `.badge-danger` (`report.html:20-21,353-357`) | 5.30:1 | PASS | PASS |
| Outcome text `#0F766E` | Even row `#FAFAFA` | Previous pregnancy Live Birth even row (`report.html:522,756`) | 5.24:1 | PASS | PASS |
| Rose accent `#B34A60` | Surface white `#FFFFFF` | `.clinic-title-box h2`, `.doc-title`, `.history-row .v.text-rose`, odd row `.text-rose` (`report.html:12,238,536,614,714`) | 5.17:1 | PASS | PASS |
| White `#FFFFFF` | Rose accent `#B34A60` | Page 2 `CONTINUED` `.doc-badge` (`report.html:909`) | 5.17:1 | PASS | PASS |
| Photo badge text `#0369A1` | Photo badge bg `#E0F2FE` | `.photo-badge` (`report.html:569-578`) | 5.17:1 | PASS | PASS |
| BP alert `#B45309` | Surface white `#FFFFFF` | `.bp-alert` odd row (`report.html:556`) | 5.02:1 | PASS | PASS |
| Rose accent `#B34A60` | Even row `#FAFAFA` | Even table row `.text-rose` (`report.html:522,536`) | 4.95:1 | PASS | PASS |
| Rose accent `#B34A60` | Card / matrix `#F8FAFC` | `.matrix-item .val.text-rose` & Rh-negative `.data-value.text-rose` (`report.html:430,696`, `PatientPrintHtmlGenerator.kt:950`) | 4.94:1 | PASS | PASS |
| BP alert severe `#DC2626` | Surface white `#FFFFFF` | `.bp-alert-severe` odd row (`report.html:557`) | 4.83:1 | PASS | PASS |
| BP alert `#B45309` | Even row `#FAFAFA` | `.bp-alert` even row (`report.html:522,556`) | 4.81:1 | PASS | PASS |
| Rose accent `#B34A60` | GA highlight `#FDF2F4` | `.ga-highlight` (`report.html:447-456`) | 4.72:1 | PASS | PASS |
| BP alert severe `#DC2626` | Even row `#FAFAFA` | `.bp-alert-severe` even row (`report.html:522,557`, `line 824`) | 4.63:1 | PASS | PASS |

---

### Numbered FAIL Findings

None (0 FAIL findings).
