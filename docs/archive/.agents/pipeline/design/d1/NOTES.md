# ANC EMR · UI/UX Design Mock-ups (d1 Lane)
**Author:** UI/UX Designer Lane  
**Client / Owner:** Dr. Sherif Radwan, Consultant Obstetrician & Gynecologist  
**Target Platform:** Jetpack Compose (Android Native, Phone, English UI)  
**Deliverable:** `.agents/pipeline/design/d1/index.html` (interactive HTML mock-up) & `NOTES.md`

---

## 1. Executive Summary & Design Principles

Dr. Sherif Radwan's brief identified acute cognitive friction and physical fatigue in the current Android Compose entry editors:
> *"The editors are too crowded and tiring; every field is a big text box that takes the whole width; labels and values look alike; sections are not visually distinct from the boxes around them; entry should be EASY and RAPID with one consistent type of entry across fields (tap choices, chips, steppers, smart defaults, recent/common pick lists, only type when there is no better way); the doctor must not be exhausted."*

### Core Architectural & UI Principles Applied Across Both Options:
1. **Zero Clinical Fields Dropped:** Every field currently recorded in `LabDraftValues`, `UltrasoundDraftValues`, `MedicationDraftValues`, `ClinicalVocabulary`, and the room database is strictly preserved and reachable (directly or through intuitive inline expansions).
2. **Elimination of Developer Jargon:** All occurrences of developer-internal terminology (such as "Codex") are completely eliminated from visible UI. For example, *"Save to Lab Codex"* becomes *"Save to my test list"*.
3. **Consolidated Batch Metadata:** Date and Laboratory Name are set once for the entire batch rather than repeated per investigation row.
4. **Light Sub-text for Units & Reference Ranges:** Units and reference intervals no longer take separate wide input boxes; they sit as crisp, low-contrast secondary sub-text beneath test names or values.
5. **Real-time Automated Flagging:** Numerical tests are compared against laboratory reference thresholds automatically (`Normal ✓`, `High ▲`, `Low ▼`). Qualitative serology/urinalysis are matched against normative baselines (`Negative`, `Non-reactive`, `Nil albumin`). Separate "Normal/Abnormal" toggle switches on every test are removed; overrides are 1-tap popovers only when clinically needed.
6. **48 dp Minimum Touch Targets & Anti-Wrapping:** All buttons, chips, steppers, and pill controls have a minimum touch boundary of 48×48 dp with `white-space: nowrap` and `min-width: max-content` to prevent vertical text splitting.
7. **Native Android M3 & Clinical Palette Parity:** Strict color mapping matching `app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt`:
   - Primary: Clinical Navy (`#0D1B3D` light / `#B4C5FF` dark)
   - Secondary / Action: Clinical Rose / Action Rose (`#934F5B`, `#FFD9DE`, `#D49AA0` light / `#FFB2BC`, `#762E3B` dark)
   - Tertiary: Clinical Teal (`#176B70` light / `#70D7D9` dark)
   - Background / Surfaces: Warm Clinical Ivory (`#FBF8F5` light / `#0E121B` dark)
   - Clinical Alert Badges: Tag Normal (`#DCFCE7`/`#166534`), Tag High Risk (`#FEE2E2`/`#B91C1C`), Tag Attention (`#FEF3C7`/`#92400E`), Tag GA/Sky (`#E0F2FE`/`#0369A1`).
8. **Scalability:** Full responsiveness across font scaling up to 160% and seamless dark mode support via native CSS variables.

---

## 2. Design Comparison Matrix

| Screen | Option | Fields Kept & Destination | New Behaviour & Speedups | Open Questions for Dr. Sherif Radwan |
| :--- | :--- | :--- | :--- | :--- |
| **1. Laboratory Entry** (`LabEditor.kt`) | **Option A: One list, type in place** | • Date & Lab Name: Pinned batch top bar<br>• Test Name: Row title<br>• Category: Row sub-badge & drawer<br>• Result: Direct in-row decimal input or 1-tap chip<br>• Unit & Reference: Light sub-text under test name<br>• Abnormal flag: Auto-flag pill (`Normal ✓`, `High ▲`, `Low ▼`), 1-tap override<br>• Notes & Custom test save: Expandable row drawer | • 1-tap selection of clinical panels (First Trimester Booking, Preeclampsia Workup, GDM, Anemia, Infection) adds all constituent tests simultaneously.<br>• Doctor scrolls through a single unified sheet and inputs numerical results directly into compact right-aligned boxes.<br>• Qualitative tests (Blood Group, Serology) expose inline tap-chips.<br>• Search bar with autocomplete to quickly add isolated tests.<br>• Eliminates iterative "Save & add another" loop. | 1. When ordering panels with qualitative tests (e.g., ABO/Rh, HBsAg), do you prefer default "Negative/Normal" pre-selection or blank state until tapped?<br>2. For urinalysis, is the standard choice chip set ("Nil albumin, nil sugar", "Albumin +", "Sugar +", "Pus cells ↑") sufficient for 95% of routine visits? |
| **1. Laboratory Entry** (`LabEditor.kt`) | **Option B: One test at a time, number keypad** | • Same fields as Option A.<br>• Flag override and clinical notes relocated under secondary "Flag / note" toggle tab.<br>• Batch review table available under "Review" tab. | • Dedicated, high-contrast on-screen numerical keypad (0-9, dot, delete, next) remains fixed in the lower viewport.<br>• Zero mobile keyboard jumping/resizing between rows.<br>• Entering a value advances automatically to the next test in the panel.<br>• Qualitative tests display large, full-width touch cards that auto-advance upon selection.<br>• Top progress tracker shows "Test X of Y" with a completion bar. | 1. Does having a custom on-screen keypad feel faster on the phone than the native Android Gboard/keyboard?<br>2. When a value is out of range, should the screen emit a haptic buzz before automatically stepping to the next test? |
| **2. Ultrasound Entry** (`UltrasoundEditor.kt`) | **Option A: Picture cards and steppers** | • Date & GA: Section 1 card with steppers and live Dating Engine discrepancy banner<br>• Fetus Number, Presentation, Placenta, Liquor: Sections 2 & 3 with graphical cards<br>• FHR, BPD, HC, AC, FL, EFW: Section 4 with smart steppers<br>• Impression: Section 5 with preset chips + "✨ From my choices" generator<br>• Photos: Section 6 photo gallery<br>• Linked Visit: Collapsible "More" card | • Replaces text boxes with anatomical SVG graphical choice cards (Cephalic, Breech, Transverse; Anterior, Posterior, Fundal, Lateral, Low-lying, Previa; Normal, Poly, Oligo).<br>• Biometry steppers initialize at GA-typical median values for the current week (e.g., 28w BPD 7.3 cm, EFW 1210 g) so doctor only needs 1-2 taps to adjust.<br>• "✨ From my choices" button automatically synthesizes selected findings into a grammatically sound clinical impression.<br>• Direct tap on stepper value allows manual numerical typing if preferred. | 1. Are the visual anatomical diagrams for placental site and presentation intuitive at a glance?<br>2. Would you prefer biometry steppers to auto-calculate EFW (Hadlock formula) based on BPD/HC/AC/FL inputs? |
| **2. Ultrasound Entry** (`UltrasoundEditor.kt`) | **Option B: Start from a normal scan, one finding at a time** | • Same fields as Option A.<br>• Date, GA, and Redating Alert collapsed into top sticky summary banner.<br>• Biometry controlled via combined slider + stepper controls.<br>• Photos and Linked Visit nested under bottom "More" expander. | • Single-tap "✓ Fill normal scan" populates singleton, cephalic, anterior, normal liquor, allowing doctor to edit only exceptions.<br>• Sticky top banner maintains a live running sentence report (e.g., *"Singleton · Cephalic · Anterior placenta · Normal liquor · FHR 145 bpm"*).<br>• "Auto-next" mode automatically folds the answered finding and expands the next item in sequence.<br>• Biometry items utilize coarse horizontal scrubbers with fine stepper adjustment buttons. | 1. Is pre-filling a "Normal Scan" clinically acceptable in your practice, or does it risk confirmation bias during busy clinic sessions?<br>2. Do you find the live-updating summary bar useful for reading back to the patient or ultrasound assistant? |
| **3. Medication Entry** (`MedicationEditor.kt`) | **Option A: Build one prescription card** | • Drug Name: Search bar or 1-tap quick tiles<br>• Dose, Route, Frequency: Smart chip rows pre-selected to drug default<br>• Start Date: Preset chips ("Today", "Yesterday") + picker<br>• Prophylaxis: High-visibility badge toggle<br>• End Date, Status, Adherence, Adverse Notes: "More options" accordion<br>• Prescription Notes: Expandable "Add note"<br>• Current Medications: Active list below with quick Stop & Edit | • Pick a drug from Common/Recent tiles; the prescription card immediately populates standard dosage (e.g., Aspirin -> 150 mg, Oral, At bedtime, Prophylaxis = Yes).<br>• Doctor modifies only atypical parameters before tapping "Add to prescription".<br>• Clear visual separation between the composer card and the active medications ledger.<br>• Stopping a drug opens a 1-tap reason prompt ("Completed" vs "Discontinued") and immediately timestamps today's end date.<br>• "Undo" snackbar prevents accidental additions or terminations. | 1. Do you prefer having the composer card always docked at the top, or should it collapse once the prescription is added?<br>2. Would you like default stop dates automatically calculated for acute courses (e.g., antibiotics 7 days)? |
| **3. Medication Entry** (`MedicationEditor.kt`) | **Option B: One tap to add, fix it in the list** | • Same fields as Option A.<br>• Route, Frequency, Start Date, Prophylaxis exposed as interactive inline chips directly on the medication row.<br>• End Date, Adverse Notes, Non-compliance accessible via "⋯ More" dropdown per medication card. | • Extreme velocity: Tapping any drug in the catalog immediately inserts it as an active row in today's medication list with default parameters.<br>• The newly inserted drug is highlighted in Teal with pulsing chips.<br>• Doctor modifies dosage or frequency directly on the medication tile without opening a separate editing modal.<br>• 1-tap inline Stop button with instant undo toast.<br>• Search bar allows immediate on-the-fly custom drug creation. | 1. Is the instant-add interaction safe against accidental palm touches while navigating on a phone?<br>2. Do you prefer seeing all active medications as expanded cards or as compact single-line items? |

---

## 3. Detailed Screen Breakdown & Clinical Rationale

### Screen 1: Laboratory Investigations & Diagnostics
*Current Codebase Reference:* `ui/LabEditor.kt`, `model/codex/ClinicalCodexRepository.kt`

#### The Problem Solved:
Previously, the doctor had to create a lab record, fill out 7-9 separate text fields (each occupying a full card row), manually toggle an abnormal switch, hit "Save and add another test", and repeat this entire process 8 times just to book first-trimester routine labs.
Furthermore, the word "Codex" was scattered across labels, buttons, and dialogs.

#### Clinical Field Audit & Relocation:
- **Date (`date`):** Moved to the unified Batch Header. Set once for all investigations in the session. Supports 1-tap shortcuts ("Today", "Yesterday") and custom date picker. Automatically computes gestational age at the date of testing.
- **Laboratory Name (`labName`):** Moved to Batch Header. Presets provided: *Al Borg Labs*, *El Mokhtabar*, *Hospital Lab*, *Alfa Labs*, plus *Other…*.
- **Diagnostic Panels (Codex groups):** 1-tap multi-test activation:
  - *First Trimester Routine Booking* (8 tests: CBC, Blood Grouping & Rh, FBS, Urine Routine, HBsAg, HIV 1 & 2, Rubella IgG, TSH)
  - *Preeclampsia Workup* (6 tests: CBC, Serum Creatinine, Serum Uric Acid, ALT, AST, Urine Routine)
  - *GDM Screening 24-28 W* (3 tests: FBS, OGTT 75g, Urine Routine)
  - *Anemia Workup & Iron Stores* (2 tests: CBC, Serum Ferritin)
  - *Infection & Serology Screening* (4 tests: HBsAg, HIV, VDRL, Rubella)
- **Test Name (`testName`):** Left side of row, bold header with category badge.
- **Result (`result`):** Right side of row. Compact numeric input or discrete qualitative chips.
- **Unit (`unit`) & Reference Range (`referenceRange`):** Rendered as muted, light sub-text under the test name (e.g., `g/dL · ref 11.0 – 15.0`).
- **Abnormal Flag (`isAbnormal`):** Automatically evaluated. Green pill for normal, Red pill with alert chevron for high/abnormal, Sky blue pill for low. Tapping the pill allows a 1-tap manual override.
- **Clinical Notes (`notes`):** Tapping any row opens the inline detail drawer where notes, custom unit, custom range, and category can be updated.
- **Save to Clinic Catalog:** When a custom test is typed in search, a checkbox *"Save to my test list"* allows persistent catalog inclusion without developer jargon.

---

### Screen 2: Ultrasound Examination & Fetal Biometry
*Current Codebase Reference:* `ui/UltrasoundEditor.kt`, `model/ClinicalVocabulary.kt`, `model/GestationalAgeEngine.kt`

#### The Problem Solved:
Previously, biometry and fetal anatomy entry required typing into 6 separate floating text boxes while toggling tiny chip filters. The GA calculation was buried, and writing an impression required typing repetitive clinical prose repeatedly.

#### Clinical Field Audit & Relocation:
- **Scan Date & GA (`date`, `gaWeeks`, `gaDays`):** Displayed with quick-steppers. The screen calculates discrepancy against baseline dating and triggers the redating guideline alert if difference exceeds clinical threshold (e.g., >7 days in 1st trimester / >10 days in 2nd trimester).
- **Fetal Number (`fetalNumber`):** Singleton, Twins, Higher order. Presented as visual cards with fetal silhouette counts.
- **Fetal Presentation (`presentation`):** Cephalic, Breech, Transverse, Variable. Visual anatomical cards showing fetal orientation relative to the pelvic inlet.
- **Placental Site (`placentalSite`):** Anterior, Posterior, Fundal, Lateral, Low-lying, Previa. Rendered with uterus section diagrams indicating implantation position.
- **Amniotic Fluid (`amnioticFluid`):** Normal, Oligohydramnios, Polyhydramnios. Visual pool indicators.
- **Fetal Heart Rate (`fetalHeartRate`):** Stepper pre-set to 145 bpm (typical range 110–160 bpm) with rapid ±5 bpm stepping or direct numeric entry.
- **Fetal Biometry (`biometryBpd`, `biometryHc`, `biometryAc`, `biometryFl`, `estimatedFetalWeight`):**
  - Option A uses smart steppers calibrated with 28-week normal ranges (BPD 7.3 cm, HC 26.4 cm, AC 23.7 cm, FL 5.3 cm, EFW 1210 g).
  - Option B uses smooth range scrubbers with fine increment step buttons.
- **Scan Impression (`impression`):** Includes standard preset chips (*"Single live fetus, cephalic..."*, *"Normal fetal growth & biometry..."*, etc.) plus an intelligent **"✨ From my choices"** synthesizer that formats selected findings into a professional report.
- **Photos & Attachments (`imageUris`):** Dedicated camera capture button, photo picker, and removable photo thumbnails.
- **Linked Visit (`linkedVisitId`):** Collapsible visit picker displaying visit date, blood pressure, and maternal weight.

---

### Screen 3: Medication Management & Prescription
*Current Codebase Reference:* `ui/MedicationEditor.kt`, `model/codex/ClinicalCodexRepository.kt`, `model/ClinicalVocabulary.kt`

#### The Problem Solved:
Adding routine pregnancy supplements (Folic acid, Iron, Calcium, Aspirin) required filling multiple full-width text boxes for name, dosage, frequency, and dates. There was no easy way to see what the patient was actively taking today or stop a completed course in one tap.

#### Clinical Field Audit & Relocation:
- **Medication Selection (`name`):** Rapid selection via Common Antenatal Drugs grid (Folic Acid, Ferrous Sulfate, Calcium + D3, Multivitamins, Low-Dose Aspirin, Paracetamol, Micronized Progesterone, Enoxaparin LMWH, Labetalol, Methyldopa) or Patient's Recent Drugs chips. Search bar supports any generic/trade name.
- **Dosage (`dosage`):** Instant chips based on standard clinical dosages for the selected molecule (e.g., Folic acid: `400 mcg`, `5 mg`; Aspirin: `75 mg`, `150 mg`; Ferrous Sulfate: `100 mg`, `200 mg`). Custom dose input available via "Other…".
- **Administration Route (`route`):** Defaults to `Oral`. Fast chips for `IM`, `IV`, `Topical`, `Subcutaneous`.
- **Dosing Frequency (`frequency`):** Direct chip selection: `Once daily`, `Twice daily`, `Three times daily`, `At bedtime`, `As needed (PRN)`.
- **Start Date (`startDate`):** Defaults automatically to `Today` (`LocalDate.now()`). Quick chips for `Today`, `Yesterday`, or standard date picker.
- **Prophylaxis Indicator (`isProphylaxis`):** Prominent toggle switch/chip with green active styling.
- **Adherence & Side Effects (`isNonCompliant`, `adverseNotes`):** Kept reachable under "More options" accordion to keep routine prescribing streamlined.
- **Prescription End Date & Status (`endDate`, `status`):** Displayed on active medications list. 1-tap "Stop" prompts for reason (`Completed` or `Discontinued`) and automatically populates today's date into `endDate`.
- **Active Prescription List:** Pinned summary of all medications the patient is currently taking with quick edit and stop buttons, plus an immediate "Undo" notification to guard against mis-taps.

---

## 4. Recommendations for Dr. Sherif Radwan

1. **Laboratory Entry: RECOMMEND OPTION A ("One list, type in place")**
   - *Clinical Reason:* In antenatal practice, laboratory investigations arrive as a paper or PDF batch from Al Borg or El Mokhtabar. Seeing all 6-8 tests on one screen mirrors the laboratory report sheet. Entering numbers directly down the column is substantially faster than stepping through 8 successive screens on a keypad, while qualitative serologies take just one tap.
2. **Ultrasound Entry: RECOMMEND OPTION A ("Picture cards and steppers")**
   - *Clinical Reason:* The visual anatomical cards for placenta, presentation, and fluid eliminate reading fatigue during an active scan. Biometry steppers that start at GA-typical median values allow the doctor to complete an entire routine biometry scan in fewer than 10 total screen taps. The "✨ From my choices" impression generator alone saves significant typing time per patient.
3. **Medication Entry: RECOMMEND OPTION A ("Build one prescription card")**
   - *Clinical Reason:* While Option B's instant-add is rapid, Option A provides a crucial clinical safety pause: selecting a drug populates its standard obstetric dose, route, and frequency in a clean card where the doctor confirms the regimen before tapping "Add to prescription". This prevents accidental dosage errors while still taking under 3 seconds per prescription.
