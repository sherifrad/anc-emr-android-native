# SPEC: Design Round d28 - Visit and Vitals Entry (Mock d4/d3 §3 & REDESIGN_SPECS §4.4)

## 1. Context and Purpose
Implements the approved Visit and vitals entry UI from `.agents/pipeline/design/d4/d3/index.html` section 3 and `REDESIGN_SPECS.md` Section 4.4.
This is a DESIGN round: changes are strictly confined to UI (`ui/` and `res/values/strings.xml`). Logic, state, and storage were implemented and verified in round `l08`.

## 2. Mock Element -> Compose Component Mapping

| Mock Element (`d4/d3/index.html` §3) | Compose Component | Shared Component or New |
| :--- | :--- | :--- |
| **Top Bar** ("New visit", Patient Name · GA Xw Yd, Close '✕') | `EditorPage` header / title & subtitle | Shared `EditorPage` (`PatientEditor.kt`) |
| **Step Line** ("Step X of 4", Step Title) | `VisitStepHeader(currentStep, totalSteps, stepTitle)` | New in `DesignSystem.kt` |
| **Segmented Progress** (4 clickable segments: done, cur, pending) | `SegmentedProgressBar(steps, currentStep, onStepClick)` | New in `DesignSystem.kt` |
| **Step 1: Date & Blood Pressure** | | |
| - Date Row (closed/open, label "Date", value "Today" / "Yesterday" / formatted date) | `CollapsibleRowCard` | Reuses `SectionCard` / `SummaryRow` pattern |
| - Date choices ("Today", "Yesterday", "Other date" picker) | `FilterChip`, DatePickerDialog / OutlinedTextField | Standard Compose / `FilterChip` |
| - BP Row (closed/open, label "Blood pressure", value "120/80" or Flag pill) | `CollapsibleRowCard` | Reuses `SectionCard` / `SummaryRow` |
| - BP Quick Chips ("Same as last (sys/dia)", "Normal (110/70)", Custom presets, "+ Add other") | `FilterChip`, `AddOtherChoiceChip` | Existing presets / `CustomChoicesDelegate` |
| - BP Dual Inputs (Systolic `___` / Diastolic `___` mmHg) | `OutlinedTextField` with numeric inputmode | Standard Compose |
| - High BP Banner ("High BP is flagged on the visit. You can still save.") | Warning `Card` (rose container `#FFD9DE`, on-rose text `#934F5B`) | Alert card pattern |
| - "Done" Button (enabled when sys >= 60 && dia >= 30) | `Button` (primary fill, 48 dp) | Standard Compose |
| **Step 2: Weight & Fundal Height** | | |
| - Weight Row (closed/open, label "Weight", value "X.X kg") | `CollapsibleRowCard` | Reuses `SectionCard` / `SummaryRow` |
| - Weight Quick Chips ("Same as last (X kg)", Custom presets, "+ Add other") | `FilterChip`, `AddOtherChoiceChip` | Standard Compose / `CustomChoicesDelegate` |
| - Weight Stepper / Input (- / + 0.1 kg buttons, numeric field) | `Row` with 48 dp `IconButton` / `OutlinedTextField` | New `ValueStepper` in `DesignSystem.kt` |
| - Fundal Height Row (closed/open, label "Fundal height", value "X cm") | `CollapsibleRowCard` | Reuses `SectionCard` / `SummaryRow` |
| - FH Quick Chips ("Same as last (X cm)", "Matches GA (X cm)", Custom presets, "+ Add other") | `FilterChip`, `AddOtherChoiceChip` | Standard Compose / `CustomChoicesDelegate` |
| - FH Stepper / Input (- / + 1 cm buttons, numeric field) | `Row` with 48 dp `IconButton` / `OutlinedTextField` | `ValueStepper` |
| **Step 3: Findings** | | |
| - "Fill normal values" Button (fills blank oedema, movements, urine only; never BP/wt) | `Button` (secondary rose container `#FFD9DE`, 48+ dp tall) | Shared button pattern |
| - Helper note ("Fills blank oedema, movements and urine only. Never BP or weight.") | `Text` (muted, caption) | Standard Compose |
| - Oedema Row (options: "None", "Mild", "Marked" [flagged/red], Custom, "+ Add other") | `BigChoiceButton` list | Shared `BigChoiceButton` (`DesignSystem.kt`) |
| - Fetal Movements Row (options: "Good", "Reduced" [flagged], "Absent" [flagged], Custom, "+ Add other") | `BigChoiceButton` list | Shared `BigChoiceButton` (`DesignSystem.kt`) |
| - Urine Protein Row (options: "Negative", "Trace" [flagged], "+" [flagged], "++" [flagged], Custom, "+ Add other") | `BigChoiceButton` list | Shared `BigChoiceButton` (`DesignSystem.kt`) |
| **Step 4: Review and Save** | | |
| - Summary Card ("Summary", one-line clinical summary, flagged pills / "Nothing flagged") | `Card` with `detectFlaggedFindings` pills | Shared `ClinicalBadges` / `SectionCard` |
| - 7 Review Rows (Date, BP, Weight, FH, Oedema, Movements, Urine) with values & `›` chevron | `SummaryRow` (tapping switches to step with row open) | Shared `SummaryRow` (`DesignSystem.kt`) |
| - Notes & Additional Findings field | `OutlinedTextField` for optional narrative notes | Standard Compose |
| **Pinned Bottom Bar** | | |
| - Step 0 (first step): "Cancel" (outlined, left), "Next" (primary navy, right) | `EditorPage(bottomBar = ...)` | Shared `EditorPage` slot |
| - Steps 1-2 (intermediate): "Back" (outlined, left), "Next" (primary navy, right) | `EditorPage(bottomBar = ...)` | Shared `EditorPage` slot |
| - Step 3 (final): "Back" (outlined, left), "Save visit" (primary navy, right) | `EditorPage(bottomBar = ...)` | Shared `EditorPage` slot |

## 3. Behavioral Oracles
- O1: Step navigation advances upon answering a row or tapping "Next"; tapping segments jumps to the target step.
- O2: "Fill normal values" in Step 3 sets only empty oedema (`None`), movements (`Good`), urine (`Negative`), without touching BP, weight, or non-blank findings.
- O3: BP $\ge 140/90$ displays the rose warning banner and flags the review row, but never blocks proceeding or saving.
- O4: "+ Add other" chip on BP, Weight, Fundal Height, Oedema, Movements, and Urine opens custom choice addition and binds to the respective `CustomChoiceList`.
- O5: All 48 dp minimum touch target rules and 360 dp width constraints are met.
- O6: Backwards compatibility with test harness and static check S70 (`bpLevel`, strings `visit_date_in_future`, `blood_pressure_invalid`, `weight_invalid`).
