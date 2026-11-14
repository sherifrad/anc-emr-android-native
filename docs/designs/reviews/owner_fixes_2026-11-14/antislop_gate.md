antislop active: after (session override).

Reading this as: clinical EMR native Android application for antenatal care providers and midwives, in a calm Material 3 clinical language, dial ENERGY 1 / RHYTHM 1 / MOTION 1.

---

### Delivery Gate Report

#### Block 1: Hard Gate (Absolute)
- **R-02 PASS**: No em dashes (`—`) or en dashes (`–`) exist in any new or modified copy (`res/values/strings.xml#L665-L679`).
- **R-03 FAIL**: `PatientListScreen.kt#L426-L427` sort button sets `maxLines = 1` and `softWrap = false` without `overflow = TextOverflow.Ellipsis`, creating potential truncation/overflow on 360dp narrow viewports. (All interactive touch targets pass $\ge 48\text{dp}$, e.g. `DueDateBox.kt#L43`, `ClinicalAttentionCard.kt#L61`, `PatientHubHeader.kt#L528`).
- **R-17 PASS**: All numbers displayed (e.g., patient counts in `PatientListScreen.kt#L351-L401`, remaining attention count `PatientHubHeader.kt#L553`) originate from real database query aggregates; no fabricated statistics.
- **R-18 N/A**: No testimonials or social proof components exist in this clinical EMR app.
- **R-23 PASS**: No AI-generated logos, avatars, or fictional imagery were created. Photo viewers (`ClinicalAttachmentViewerDialog.kt#L192`, `UltrasoundPhotoViewerDialog.kt#L178`) render real captured ultrasound and lab bitmaps.
- **R-24 PASS**: All navigation and dialog triggers connect to real, implemented destinations (`PatientHubScreen.kt#L215`, `ClinicalAttentionCard.kt#L63`).
- **R-25 FAIL**: `ClinicalAttentionLevel.NOTE` accent text (`MaterialTheme.colorScheme.outline`, defined in `ClinicalAttentionBanner.kt#L128`) placed on container (`surfaceContainerHigh`) achieves only **3.78:1** in Light theme and **4.26:1** in Dark theme, failing WCAG AA 4.5:1 for normal text in `ClinicalAttentionCard.kt#L181`, `ClinicalAttentionBanner.kt#L189`, and `PatientHubHeader.kt#L556`.
- **R-26 PASS**: Every interactive control has executable handlers (`ClinicalAttentionBanner.kt#L278`, `ClinicalAttentionBanner.kt#L330`, `PatientListScreen.kt#L416`); no dead buttons or stubbed controls.
- **R-27 PASS**: All data views provide full state coverage, including empty state for attention entries (`ClinicalAttentionCard.kt#L75-L103`) and filtered patient directory list (`PatientListScreen.kt#L488-L498`).
- **R-28 N/A**: No FAQ section exists in the clinical workspace.
- **R-32 PASS**: Interactive elements utilize standard Material 3 components (`Surface`, `Button`, `IconButton`, `FilterChip`, `DropdownMenuItem`) that automatically support Compose focus trees, hardware keyboard navigation (Tab/Shift+Tab, Enter/Space), and TalkBack d-pad navigation with default visible focus rings.
- **R-33 PASS**: Zero external shell or CSS patch scripts; all UI and logic are implemented directly in native Kotlin Jetpack Compose source.
- **R-34 FAIL**: Dark theme contrast for Note attention accent text fails WCAG AA (4.26:1) on `surfaceContainerHigh` (`ClinicalAttentionBanner.kt#L128`), breaking the requirement that all shipped themes must pass contrast standards.
- **R-35 PASS**: Code verified via static inspection of all 14 changed files, contrast formula calculations, and validation against existing UI test tags (`testTag("clinicalAttentionCard")`, `testTag("dueDateBox")`, `testTag("zoomResetButton")`).
- **R-36 PASS**: No fabricated security badges, compliance certifications, or marketing claims.
- **R-37 PASS**: Design direction strictly adheres to owner instructions: calm clinical app, dials ENERGY 1 / RHYTHM 1 / MOTION 1, Material 3 theme colors, 360dp narrow viewport first.
- **R-38 PASS**: All displayed data reflects real patient domain models (`DatingSummary`, `SevereBpAttention`, `DemoPatient`).
- **R-39 FAIL**: `CompactClinicalAttentionStrip` (`PatientHubHeader.kt#L534-L543`) and `ClinicalAttentionBanner` (`ClinicalAttentionBanner.kt#L180-L186`) convey the distinction between Critical and Alert levels by color alone (both render identical `Icons.Default.Warning` with no level indicator text).
- **R-40 N/A**: Android native Jetpack Compose application; web-specific HTML `<head>`, `<img>`, and CWV script tags do not apply.

#### Block 2: Purpose-Gate (Technique Allowed, Reason Required)
- **R-01 PASS**: Flat Material 3 theme containers used throughout; zero gratuitous gradients or glowing orbs (`Theme.kt#L221-L248`).
- **R-04 PASS**: Icons are strictly domain-relevant standard clinical glyphs: `Icons.Default.Warning` for alerts/vitals, `Icons.Default.Info` for notes, `Icons.Default.Edit` for edits, `Icons.Default.Delete` for removal (`ClinicalAttentionCard.kt#L14-L15`, `ClinicalAttentionBanner.kt#L15-L19`).
- **R-06 FAIL**: `ClinicalAttentionBanner.kt#L186-L191` applies `.uppercase()` and wide letter-spacing (`0.8.sp`) to `"CLINICAL ATTENTION"`, introducing AI-typical all-caps label styling without clinical purpose.
- **R-07 PASS**: Clean neutral backgrounds (`surface`, `surfaceContainerHigh`); no decorative dot grids or blueprint lines (`Theme.kt#L233-L241`).
- **R-08 PASS**: Chevron icon (`Icons.AutoMirrored.Filled.KeyboardArrowRight` in `PatientHubHeader.kt#L560`) has a direct functional purpose (indicating drill-in to dialog); no decorative arrows on standard action buttons.
- **R-09 PASS**: Badges strictly represent clinical triage states ("Critical", "Alert", "Note", "+N"); no "AI Powered" or decorative capsule badges (`ClinicalAttentionCard.kt#L178`).
- **R-10 PASS**: Glassmorphism/alpha blur is limited strictly to a single HUD overlay control (`ZoomableImage.kt#L182` reset button with `alpha = 0.9f`); well within the 1-2 element cap.
- **R-12 PASS**: Zero floating elevation shadows; components sit flat with 1dp structural borders (`ClinicalAttentionCard.kt#L114`, `DueDateBox.kt#L38`).
- **R-13 PASS**: Zero glow effects applied anywhere.
- **R-14 PASS**: Cards represent distinct clinical entities (`DueDateBox` vs `ClinicalAttentionCard` vs `PatientDirectoryCard`); no cookie-cutter feature grids.
- **R-19 PASS**: Motion dial is strictly MOTION 1; animations are confined to direct-manipulation gestures (pinch-to-zoom, pan, double-tap in `ZoomableImage.kt#L91-L158`).
- **R-22 PASS**: Zero decorative 3D blob characters or stock illustrations.

#### Block 3: Liveliness
- **Dials declared**: ENERGY 1 / RHYTHM 1 / MOTION 1.
- **Consistency**: PASS; calm clinical tone, flat structural hierarchy, predictable information architecture.
- **Focal point per screen**: PASS; `DueDateBox` anchors dating in Overview tab (`OverviewSections.kt#L88`), `ClinicalAttentionCard` anchors triage alerts (`OverviewTab.kt#L70`).
- **Whitespace as structure**: PASS; 8dp/12dp spacing scale consistently applied (`ClinicalAttentionCard.kt#L107`, `ClinicalAttentionBanner.kt#L256`).
- **One deliberate accent**: PASS; crimson (`error`) reserved strictly for critical clinical safety alerts; amber for clinical attention.
- **Identity motif**: PASS; consistent 1dp outline borders and tonal containers matching clinical risk tiers (`ClinicalCardTokens`).
- **Design Read declared**: Declared at the beginning of the audit report.

#### Block 4: Craftsmanship & Quality Locks
- **C-1 FAIL**: `ClinicalAttentionCard.kt#L52` hardcodes `title = "Clinical attention"` instead of `stringResource(R.string.clinical_attention)`. `PatientHubHeader.kt#L508` hardcodes `"Severe BP: ..."` instead of reusing `R.string.bp_attention_severe`.
- **C-2 PASS**: Every button, chip, and dropdown item is wired to real state changes.
- **C-3 PASS**: All sections derive directly from ANC clinical protocols (dating summary, severe hypertension alerts, patient archive/delivery triage).
- **C-4 FAIL**: Contrast failure in Note level breaks UI resilience across both light and dark themes (R-25 / R-34).
- **C-5 PASS**: All clinical alerts map directly to actual recorded vitals (`SevereBpAttention`) or parsed provider attention entries (`parseAttentionEntries`).
- **R-05 PASS**: Layout structure reflects real clinical workflow without template landing page sections.
- **R-11 PASS**: Border radii are systematic: 8dp for inner elements/buttons, 12dp for cards, 24dp for search.
- **R-15 PASS**: CTAs are explicit and domain-specific: "Add attention" (`R.string.add_clinical_attention`), "Save" (`R.string.save`), "Cancel" (`R.string.cancel`).
- **R-16 PASS**: Zero buzzwords; plain clinical terminology used exclusively.
- **R-20 PASS**: Unmistakable clinical EMR identity built on Medical M3 tokens.
- **R-21 PASS**: Light and Dark themes both fully wired via `Theme.kt` and `isDarkTheme()`.
- **R-29 PASS**: Palette is constrained to Clinical Navy, Action Rose, Clinical Teal, and semantic risk tokens.
- **R-30 PASS**: Custom medical EMR layout; not a clone of generic SaaS products.
- **R-31 PASS**: Every component addition addresses documented clinical workflow requirements.
- **antislop-code FAIL**: `PatientListScreen.kt#L411` contains a redundant category label comment (`// Sort Control`) immediately preceding self-explanatory code.

---

### Computed Contrast Ratios

All contrast ratios computed using WCAG 2.x relative luminance formula via `contrast-check.py`:

| Component & Color Pair | Foreground Hex | Background Hex | Contrast Ratio | Light / Dark Theme | WCAG AA Normal Text (4.5:1) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **DueDateBox**: Normal Text on `primaryContainer` | `#041334` | `#D6DFFF` | **13.82:1** | Light | **PASS** |
| **DueDateBox**: Normal Text on `primaryContainer` | `#DCE1FF` | `#183F91` | **7.53:1** | Dark | **PASS** |
| **DueDateBox**: Missing Text (70% $\alpha$) on `primaryContainer` | `#435071` | `#D6DFFF` | **6.04:1** | Light | **PASS** |
| **DueDateBox**: Missing Text (70% $\alpha$) on `primaryContainer` | `#A1B0DE` | `#183F91` | **4.53:1** | Dark | **PASS** |
| **Critical Entry**: Content (`onErrorContainer`) on `errorContainer` | `#410002` | `#FFDAD6` | **13.26:1** | Light | **PASS** |
| **Critical Entry**: Content (`onErrorContainer`) on `errorContainer` | `#FFDAD6` | `#5C141D` | **10.33:1** | Dark | **PASS** |
| **Critical Entry**: Accent Label ("Critical") on `errorContainer` | `#BA1A1A` | `#FFDAD6` | **5.00:1** | Light | **PASS** |
| **Critical Entry**: Accent Label ("Critical") on `errorContainer` | `#FFB4AB` | `#5C141D` | **7.86:1** | Dark | **PASS** |
| **Alert Entry**: Content (`WarningText`) on `WarningBg` | `#B45309` | `#FFFBEB` | **4.84:1** | Light | **PASS** |
| **Alert Entry**: Content (`WarningDarkText`) on `WarningDarkBg` | `#FDE68A` | `#451A03` | **12.03:1** | Dark | **PASS** |
| **Alert Entry**: Accent Label ("Alert") on `WarningBg` | `#B45309` | `#FFFBEB` | **4.84:1** | Light | **PASS** |
| **Alert Entry**: Accent Label ("Alert") on `WarningDarkBg` | `#F59E0B` | `#451A03` | **6.97:1** | Dark | **PASS** |
| **Note Entry**: Content (`onSurface`) on `surfaceContainerHigh` | `#1A202E` | `#EFE9E4` | **13.52:1** | Light | **PASS** |
| **Note Entry**: Content (`onSurface`) on `surfaceContainerHigh` | `#E2E2EA` | `#283248` | **9.94:1** | Dark | **PASS** |
| **Note Entry**: Accent Label ("Note" / "+N") on `surfaceContainerHigh` | `#79747E` | `#EFE9E4` | **3.78:1** | Light | **FAIL** |
| **Note Entry**: Accent Label ("Note" / "+N") on `surfaceContainerHigh` | `#8E95A5` | `#283248` | **4.26:1** | Dark | **FAIL** |
| **ZoomableImage HUD**: "1x" Text on `surfaceContainerHigh` | `#1A202E` | `#EFE9E4` | **13.52:1** | Light | **PASS** |
| **ZoomableImage HUD**: "1x" Text on `surfaceContainerHigh` | `#E2E2EA` | `#283248` | **9.94:1** | Dark | **PASS** |
| **PatientList**: Archived Chip Selected on `secondaryContainer` | `#3B0815` | `#FFD9DE` | **13.18:1** | Light | **PASS** |
| **PatientList**: Archived Chip Selected on `secondaryContainer` | `#FFD9DE` | `#762E3B` | **7.30:1** | Dark | **PASS** |
| **Attention Dialog**: Button Text on `surfaceContainerHighest` | `#1A202E` | `#E9E2DD` | **12.70:1** | Light | **PASS** |
| **Attention Dialog**: Button Text on `surfaceContainerHighest` | `#E2E2EA` | `#323D56` | **8.41:1** | Dark | **PASS** |

---

### Numbered List of FAIL Findings (Most Serious First)

1. **[R-25, R-34] Note Level Accent Text Fails WCAG AA Contrast in Both Themes**
   - **File & Line**: [ClinicalAttentionBanner.kt#L128](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt#L128) (and consumed in [ClinicalAttentionCard.kt#L181](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionCard.kt#L181), [ClinicalAttentionBanner.kt#L189](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt#L189), [PatientHubHeader.kt#L556](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt#L556)).
   - **Problem**: `accent = MaterialTheme.colorScheme.outline` on `surfaceContainerHigh` produces a contrast ratio of only 3.78:1 (Light) and 4.26:1 (Dark), failing the 4.5:1 WCAG AA threshold for normal text.
   - **One-line fix**: In `ClinicalAttentionBanner.kt:128`, set `accent = MaterialTheme.colorScheme.onSurfaceVariant` (or bind text components to `colors.content` / `onSurface`).

2. **[R-39] Attention Level Conveyed by Colour Alone Between Critical and Alert**
   - **File & Line**: [PatientHubHeader.kt#L534-L543](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt#L534-L543) and [ClinicalAttentionBanner.kt#L178-L186](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt#L178-L186).
   - **Problem**: In both the compact header strip and the legacy banner, Critical and Alert entries render the exact same warning icon (`Icons.Default.Warning`) and omit level text, relying strictly on container background color to distinguish severity.
   - **One-line fix**: Prepend the level label text (e.g. `"${levelLabel}: ${baseText}"`) or render an explicit level badge alongside the icon.

3. **[R-06] Uppercase Header Text with Expanded Letter-Spacing in Banner**
   - **File & Line**: [ClinicalAttentionBanner.kt#L186-L191](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt#L186-L191).
   - **Problem**: Rendering `stringResource(R.string.clinical_attention).uppercase()` with `letterSpacing = 0.8.sp` introduces generic AI all-caps emphasis and diverges from `ClinicalAttentionCard.kt:52`'s sentence case.
   - **One-line fix**: Remove `.uppercase()` and `letterSpacing = 0.8.sp`, using standard sentence case `stringResource(R.string.clinical_attention)`.

4. **[C-1, R-02] Hardcoded String Literals Bypassing Localization**
   - **File & Line**: [ClinicalAttentionCard.kt#L52](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionCard.kt#L52) and [PatientHubHeader.kt#L508](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt#L508).
   - **Problem**: `title = "Clinical attention"` is hardcoded instead of localized; `val bpText = "Severe BP: ..."` uses a raw interpolated string instead of canonical `R.string.bp_attention_severe`.
   - **One-line fix**: Replace with `stringResource(R.string.clinical_attention)` and `stringResource(R.string.bp_attention_severe, severeBp.bloodPressure, severeBp.date)`.

5. **[R-03] Missing Ellipsis on Dashboard Sort Button Text on Narrow Viewports**
   - **File & Line**: [PatientListScreen.kt#L426-L427](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt#L426-L427).
   - **Problem**: Sort button text sets `maxLines = 1` and `softWrap = false` without `overflow = TextOverflow.Ellipsis`, which can push the trailing dropdown icon or clip abruptly on 360dp screens.
   - **One-line fix**: Add `overflow = TextOverflow.Ellipsis` to the `Text` composable.

6. **[antislop-code] Generic Category Label Comment Restating Obvious Code**
   - **File & Line**: [PatientListScreen.kt#L411](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt#L411).
   - **Problem**: `// Sort Control` adds no non-obvious context or technical constraint above the immediately adjacent `sortMenuExpanded` item block.
   - **One-line fix**: Delete line 411 `// Sort Control`.
