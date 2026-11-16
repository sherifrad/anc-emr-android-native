antislop active: after (session override).

### Delivery Gate Report

#### Block 1: Hard Gate (Absolute)
- R-02 PASS: Zero em dashes (`—`) or en dashes (`–`) in document text or source; quotes comma in title `PatientPrintHtmlGenerator.kt:70`: `"Specialist of Obstetrics, Gynecology & Laparoscopy"`.
- R-03 PASS: Responsive layout wraps tables in `.table-scroll` (`report.html:126-129`: `.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }`) and enforces minimum touch targets on preview dialog controls (`PatientTwoPageReportDialog.kt:98,136,158`: `Modifier.minimumTouchTarget()`, 48.dp >= 44px).
- R-17 PASS: Zero fabricated numbers or statistics; values come strictly from patient record entities (`PatientPrintHtmlGenerator.kt:647-660`: demographic, age, gestational age, and blood pressure values).
- R-18 PASS: Zero testimonials or reviews present in document or source code.
- R-23 PASS: Clinic logo uses explicit resource `R.drawable.clinic_logo_mark` (`PatientPrintHtmlGenerator.kt:48`); signature stamp placeholder explicitly labeled (`report.html:591,1166`: `<div class="stamp-placeholder">Stamp</div>`).
- R-24 N/A: Printed clinical document and preview modal have no website navigation bar.
- R-25 FAIL: `.badge-danger` uses `--danger-text: #DC2626` on `--danger-bg: #FEE2E2` (`report.html:20-21,331-335`) at 3.95:1 (< 4.5:1), and `.val-abnormal` uses `#DC2626` on `#FEF2F2` (`report.html:367-374`) at 4.41:1 (< 4.5:1) for 8pt text.
- R-26 PASS: All preview dialog controls have active handlers (`PatientTwoPageReportDialog.kt:97`: `onClick = onDismiss`, `line 127`: `startActivity(...)`, `line 144`: `PatientPrintManager.printPatientReport(...)`).
- R-27 PASS: Every clinical section defines an explicit empty state (`PatientPrintHtmlGenerator.kt:103`: `"No clinical visits recorded yet"`, `line 131`: `"No ultrasound scans recorded yet"`, `line 161`: `"No laboratory test results recorded yet"`, `line 198`: `"No current medications recorded"`).
- R-28 N/A: Printed clinical document contains no FAQ section.
- R-32 PASS: Dialog supports keyboard/back dismissal (`PatientTwoPageReportDialog.kt:68`: `dismissOnBackPress = true`), and Compose `IconButton`/`Button` provide native keyboard activation. Printed document contains no interactive DOM controls.
- R-33 PASS: All markup and CSS are authored directly in source (`PatientPrintHtmlGenerator.kt:270-610`); zero external patch scripts exist.
- R-34 FAIL: In Dark Theme, `PatientTwoPageReportDialog.kt:150-153` renders `contentColor = Color.White` on `containerColor = MaterialTheme.colorScheme.secondary` (`Theme.kt:255`: `Color(0xFFFFB2BC)`), producing an illegible 1.70:1 contrast ratio.
- R-35 PASS: Code inspection of static preview HTML (`report.html:80-133`) and Compose dialog source verified touch sizes, responsive media queries, and click handlers without runtime modification.
- R-36 PASS: Zero fabricated security compliance badges, uptime stats, or marketing claims.
- R-37 PASS: Explicit owner direction honored: obstetric clinic medical record with dials ENERGY 1 / RHYTHM 1 / MOTION 1, clinic logo palette, Arabic calligraphic doctor signature, clinic phone 01201720808, and title "Specialist".
- R-38 PASS: Missing clinical fields use honest labeled placeholders (`ReportSections.kt:18`: `"Not recorded"`, `PatientPrintHtmlGenerator.kt:903`: `"Unspecified"`, `line 1144`: `"Not recorded"`).
- R-39 FAIL: Document lacks a `<main>` landmark tag (`report.html:617`: uses `<div class="page-content">`), duplicates `<h1>` (`report.html:623` on Page 1 and `line 880` on Page 2), section titles use non-heading `<div><span>` tags, and `PatientTwoPageReportDialog.kt:162` provides redundant `contentDescription = "Print PDF"` on an icon inside a labeled button.
- R-40 FAIL: `PatientPrintHtmlGenerator.kt:250` emits `<img src="data:image/png;base64,... alt="Clinic Logo" class="clinic-logo" />` without explicit HTML `width` and `height` attributes.

#### Block 2: Purpose-Gate (Technique Allowed, Reason Required)
- R-01 PASS: No default gradients or radial glows (`report.html:9-24`); solid brand hexes `--navy-primary: #0D1B3D`, `--rose-accent: #B34A60`, and `--teal-clinical: #176B70` derive directly from clinic logo for clinical hierarchy.
- R-04 PASS: Zero generic feature glyphs (sparkle, orb, robot); preview dialog uses only standard functional Material icons (`PatientTwoPageReportDialog.kt:100,138,161`: `Icons.Default.Close`, `Share`, `Print`).
- R-06 FAIL: `.date-cell` applies `font-family: monospace; font-size: 7.8pt;` (`report.html:522`, `PatientPrintHtmlGenerator.kt:785`) to table dates without written justification instead of using `font-variant-numeric: tabular-nums`.
- R-07 PASS: No decorative background grids or blueprint lines (`report.html:34,45`); clean white paper surface with faint 4.5% authentic watermark (`report.html:61`: `opacity: 0.045`).
- R-08 PASS: Zero decorative button arrows (`→`, `↗`) in document or dialog buttons.
- R-09 PASS: Badges are strictly functional clinical risk tiers (`report.html:331-356`: `badge-danger`, `badge-warning`, `badge-success`, `Rh negative`) and pagination markers without capsule glow or pulsating dots.
- R-10 PASS: Zero glassmorphism or `backdrop-filter` in stylesheet (`report.html:7-611`).
- R-12 PASS: Print styles enforce `box-shadow: none` (`report.html:149`); on-screen preview applies single elevation shadow to sheet container (`report.html:96`: `box-shadow: 0 4px 14px rgba(13, 27, 61, 0.12);`). Flat internal components.
- R-13 PASS: Zero CSS glow or `box-shadow` lighting effects.
- R-14 PASS: Layout varies by clinical data entity (patient card grid, dating matrix, history rows, tabular logs) rather than uniform copy-paste cards (`report.html:270,402,446,468`).
- R-19 PASS: Strictly adheres to declared MOTION 1 dial; zero animations, transitions, or looping keyframes.
- R-22 PASS: Zero generic Undraw, Storyset, or 3D blob illustrations.

#### Block 3: Liveliness (Required to Be Alive, Not Just Clean)
- L-01 PASS: Dials explicitly declared: ENERGY 1 / RHYTHM 1 / MOTION 1.
- L-02 PASS: Output matches declared dials: calm, uniform, predictable clinical hierarchy without animation.
- L-03 PASS: Clear focal point per page: Page 1 centers on patient identity banner (`report.html:638`), Page 2 centers on attestation & signature block (`report.html:1144`).
- L-04 PASS: Structural whitespace (`report.html:75`: `gap: 9px;`, `line 387`: `padding: 8px 10px;`) separates clinical domains without empty voids.
- L-05 PASS: Single deliberate accent color: Rose accent (`--rose-accent: #B34A60`) used selectively for gestational age highlight (`report.html:435`), drug allergies (`report.html:690`), and Page 2 badge (`report.html:885`).
- L-06 PASS: Distinct identity motif: clinic brand lockup paired with calligraphic Arabic signature block (`report.html:8,363,1164`).
- L-07 PASS: Design Read declared: Obstetric clinical record for maternal-fetal medicine practice, dials ENERGY 1 / RHYTHM 1 / MOTION 1.

#### Block 4: Craftsmanship & Quality Locks
- C-1 PASS: Visual and copy choices serve clinical record density and clinic identity rather than AI defaults.
- C-2 PASS: Dialog buttons execute real system actions (`onDismiss`, `startActivity`, `printPatientReport`).
- C-3 PASS: Every section corresponds to an essential clinical ANC domain; zero template filler.
- C-4 FAIL: When `logoBase64` is empty, `PatientPrintHtmlGenerator.kt:252` emits `<div class="logo-fallback">DR</div>`, but `.logo-fallback` has no CSS definition in `report.html:7-611`, rendering unstyled text. Additionally, Dark Theme button contrast breaks at 1.70:1 (`PatientTwoPageReportDialog.kt:150`).
- C-5 PASS: All clinical entries represent real patient records; zero fabricated claims.
- R-05 PASS: Section rhythm follows declared RHYTHM 1 dial (predictable tabular and key-value obstetric record); no bento grids, 3-tier pricing, or generic hero blocks.
- R-11 PASS: Subtle, consistent corner radii (3px to 6px; `report.html:273`: 6px, `line 385`: 5px, `line 325`: 4px, `line 372`: 3px); zero pill-shaped cards or inputs.
- R-15 PASS: Dialog CTA is specific and contextual (`PatientTwoPageReportDialog.kt:167`: `"Print PDF"`).
- R-16 PASS: Zero marketing buzzwords or empty AI vocabulary; language is strictly medical.
- R-20 PASS: Strong domain identity via Navy/Rose/Teal palette and calligraphic Arabic signature block.
- R-21 PASS: Fixed paper theme justified for an A4 clinical document print layout.
- R-29 PASS: Palette strictly limited to 3 brand colors (Navy `#0D1B3D`, Rose `#B34A60`, Teal `#176B70`) plus neutral background and standard alert tints (`report.html:9-24`).
- R-30 PASS: Bespoke clinical record design; does not clone popular tech web products.
- R-31 FAIL: Code comments contain AI fluff and obvious restatements (`PatientPrintHtmlGenerator.kt:398`: `/* Pixel-Perfect ISO A4 Print / PDF Layout */`, `line 489`: `/* Header Meta: Perfectly uniform alignment */`, `line 730`: `/* Clinical Tables: Perfectly Centered Contents, Zero-Overflow Fixed Layout */`, `line 701`: inaccurate comment `/* History lines with Arabic on right, English/values on left */`, `PatientTwoPageReportDialog.kt:78,125,141,178`: `// Top Action Bar`, `// Share text/summary button`, `// Print / Export PDF button`, `// Interactive WebView Paper Canvas`).

---

### Color Contrast Analysis

Every text and background color pairing in the CSS and preview dialog, calculated using the WCAG 2.x relative luminance formula:

| Foreground | Background | Context | Ratio | Normal Text (4.5:1) | Large Text (3.0:1) |
|---|---|---|---|---|---|
| Text main `#0F172A` | Surface white `#FFFFFF` | Body text & table cells | 17.85:1 | PASS | PASS |
| Navy primary `#0D1B3D` | Surface white `#FFFFFF` | Headings, labels, doc name | 16.92:1 | PASS | PASS |
| White `#FFFFFF` | Navy primary `#0D1B3D` | Doc badge (Page 1) & Dialog top bar | 16.92:1 | PASS | PASS |
| Navy primary `#0D1B3D` | Patient card `#F8FAFC` | Patient name & card values | 16.17:1 | PASS | PASS |
| Navy primary `#0D1B3D` | Badge navy `#EFF6FF` | Navy badge | 15.55:1 | PASS | PASS |
| Navy primary `#0D1B3D` | Header bg `#F1F5F9` | Table headers (`th`) | 15.44:1 | PASS | PASS |
| White `#FFFFFF` | Status banner `#1E293B` | Patient status banner | 14.63:1 | PASS | PASS |
| Signature text `#1E293B` | Surface white `#FFFFFF` | Calligraphic Arabic signature | 14.63:1 | PASS | PASS |
| White 85% alpha (`#DADCE1`) | Clinical Navy `#0D1B3D` | Dialog patient subtitle | 12.23:1 | PASS | PASS |
| Attention text `#7C2D12` | Attention bg `#FFF7ED` | Clinical attention box | 8.83:1 | PASS | PASS |
| Text muted `#475569` | Surface white `#FFFFFF` | Meta keys, stamp placeholder, footer | 7.58:1 | PASS | PASS |
| Text muted `#475569` | Even row `#FAFAFA` | Table cell muted notes | 7.26:1 | PASS | PASS |
| Text muted `#475569` | Patient card `#F8FAFC` | Patient card labels & code | 7.24:1 | PASS | PASS |
| Text main `#0F172A` | Even row `#FAFAFA` | Alternating table row text | 17.10:1 | PASS | PASS |
| Loss text alt `#991B1B` | Danger bg `#FEE2E2` | Previous pregnancy loss badge | 6.80:1 | PASS | PASS |
| Success text `#166534` | Success bg `#DCFCE7` | Low risk badge | 6.49:1 | PASS | PASS |
| Warning text `#92400E` | Warning bg `#FEF3C7` | Medium risk badge | 6.37:1 | PASS | PASS |
| Teal clinical `#176B70` | Surface white `#FFFFFF` | Clinic title box subtitle | 6.23:1 | PASS | PASS |
| Teal clinical `#176B70` | Even row `#FAFAFA` | Table GA cells (even row) | 5.96:1 | PASS | PASS |
| Teal clinical `#176B70` | Patient card `#F8FAFC` | Patient phone number | 5.95:1 | PASS | PASS |
| Teal clinical `#176B70` | Teal badge bg `#E6FFFA` | Teal badge | 5.94:1 | PASS | PASS |
| Outcome text `#0F766E` | Surface white `#FFFFFF` | Previous outcome (odd row) | 5.47:1 | PASS | PASS |
| Outcome text `#0F766E` | Even row `#FAFAFA` | Previous outcome (even row) | 5.24:1 | PASS | PASS |
| Rose accent `#B34A60` | Surface white `#FFFFFF` | Clinic subheadline & table cells | 5.17:1 | PASS | PASS |
| White `#FFFFFF` | Rose accent `#B34A60` | Doc badge (Page 2) & Light button | 5.17:1 | PASS | PASS |
| Photo badge text `#0369A1`| Photo badge bg `#E0F2FE` | Photo count badge | 5.17:1 | PASS | PASS |
| BP alert text `#B45309` | Surface white `#FFFFFF` | Raised BP alert (odd row) | 5.02:1 | PASS | PASS |
| Rose accent `#B34A60` | Even row `#FAFAFA` | Table cell rose text (even row) | 4.95:1 | PASS | PASS |
| Rose accent `#B34A60` | Patient card `#F8FAFC` | Patient card EDD / allergy text | 4.94:1 | PASS | PASS |
| BP alert severe `#DC2626` | Surface white `#FFFFFF` | Severe BP alert (odd row) | 4.83:1 | PASS | PASS |
| BP alert text `#B45309` | Even row `#FAFAFA` | Raised BP alert (even row) | 4.81:1 | PASS | PASS |
| Rose accent `#B34A60` | GA highlight bg `#FDF2F4`| Gestational age highlight box | 4.72:1 | PASS | PASS |
| BP alert severe `#DC2626` | Even row `#FAFAFA` | Severe BP alert (even row) | 4.63:1 | PASS | PASS |
| **Danger text `#DC2626`** | **Abnormal bg `#FEF2F2`** | **Abnormal lab result tag (`.val-abnormal`)** | **4.41:1** | **FAIL** | PASS |
| **Danger text `#DC2626`** | **Danger bg `#FEE2E2`** | **High risk badge (`.badge-danger`)** | **3.95:1** | **FAIL** | PASS |
| **White `#FFFFFF`** | **Dark secondary `#FFB2BC`** | **Print PDF button in Dark Theme** | **1.70:1** | **FAIL** | **FAIL** |

---

### Numbered FAIL Findings

1. `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientTwoPageReportDialog.kt:151`: Set `containerColor = ActionRose` (or use `contentColor = MaterialTheme.colorScheme.onSecondary`) so the "Print PDF" button maintains >= 4.5:1 contrast in dark mode instead of collapsing to 1.70:1.
2. `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt:284,597` (and `app/build/print-sample/report.html:21,333`): Change `--danger-text: #DC2626` to `#991B1B` (or `#B91C1C`) so `.badge-danger` (3.95:1) and `.val-abnormal` (4.41:1) meet the 4.5:1 WCAG AA normal text threshold.
3. `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt:334,1017,1023` (and `app/build/print-sample/report.html:617,623,880`): Replace `<div class="page-content">` with `<main class="page-content">`, change Page 2's header from `<h1>` to `<h2>` to prevent duplicate `<h1>` tags, and convert `.section-title span` to `<h2>` or `<h3>`.
4. `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt:252` (and `app/build/print-sample/report.html:621,878`): Add `.logo-fallback { width: 46px; height: 64px; display: flex; align-items: center; justify-content: center; font-weight: 800; color: var(--navy-primary); border: 1.5px solid var(--navy-primary); border-radius: 4px; }` to CSS so text fallback does not render unstyled when logo Base64 is missing.
5. `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt:785` (and `app/build/print-sample/report.html:522`): Replace `font-family: monospace;` with `font-variant-numeric: tabular-nums;` in `.date-cell` to align date numerals without injecting unstyled terminal fonts into a sans-serif medical document.
6. `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt:250`: Add explicit `width="46" height="64"` attributes directly to `<img class="clinic-logo" ... />` to satisfy R-40 HTML media dimensions.
7. `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientTwoPageReportDialog.kt:162`: Set `contentDescription = null` on `Icon(Icons.Default.Print)` to eliminate duplicate screen reader announcements inside a button that already displays visible text `"Print PDF"`.
8. `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt:398,489,701,730` and `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientTwoPageReportDialog.kt:78,125,141,178`: Remove boastful AI adjectives (`"Pixel-Perfect"`, `"Perfectlys uniform"`), delete obvious UI restatements (`// Top Action Bar`, `// Print / Export PDF button`), and correct the inaccurate history line comment.
