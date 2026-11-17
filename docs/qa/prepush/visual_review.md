# Visual QA Review: Android EMR Post-Redesign Build

Evaluated on real device screenshots (1264x2736 px @ 560 dpi, 1 dp = 3.5 px) against design rules in `PLAN.md` (sections 0 & 2).

## Per-Screenshot Findings

- **01_launch.png** (Patient List): **OK with minor defect**. Cards and chips are clean; Arabic names ("أميرة وليد", "منى رجب المتولي") are properly right-aligned. Defect: Subtitle reads "9 Patients" with capital 'P' instead of sentence-case "9 patients". Filter chip row clips "Moderate risk (2)" on right edge until scrolled.
- **07_tab_overview.png**: **Defect**. (1) Hub header Arabic name "أميرة وليد" is left-aligned instead of right-aligned, breaking RTL convention. (2) Attention card reads "prev Gest hypertension" with lowercase "prev" and capital "Gest", violating sentence-case.
- **08_tab_visits.png**: **OK in tab body; Header defect**. Patient-family band card (blue) with GA chip, date, and quiet body (BP, Weight, FHR) adheres cleanly to slice 6 design. Defect: Header Arabic name remains left-aligned.
- **09_tab_scans.png**: **Defect**. Obstetric band card (teal) is clean, but tab bar scroll truncates outer labels mid-glyph ("...iew" on left, "Meds..." on right). Header Arabic name is left-aligned.
- **10_tab_labs.png**: **Defect**. Background band card (sand) renders well, but: (1) Tab bar clips "1" on left and "Plan" on right. (2) "Add test" is redundantly duplicated in both the tab top header and the record band header. (3) Header Arabic name is left-aligned.
- **11_tab_meds.png**: **Defect**. (1) Header Arabic name left-aligned. (2) Section header uses Title Case: "Active Prescriptions (3)". (3) Button "+ Prescribe" contains a decorative "+" symbol. (4) Inconsistent drug name casing ("Folic Acid", "Low-Dose Aspirin", "Panadol extra T").
- **12_tab_plan.png**: **OK in tab body; Header defect**. Delivery planning card uses Delivery family tint (purple) with clean *Not recorded* placeholders and sentence-case labels. Defect: Header Arabic name is left-aligned.
- **15_visit_editor.png**: **OK**. Follows all editor rules: canonical patient banner with right-aligned Arabic name and GA/parity/blood group chips; 4 labelled step tabs ("Date & BP", "Wt & FHR", etc.); calendar date trigger; sentence-case throughout.
- **16_visit_date_picker.png**: **OK**. Date field triggers full graphical calendar modal; no manual date typing required; touch targets >= 48 dp.
- **20_scan_editor.png**: **CRITICAL DEFECT**. Major redesign regressions: (1) Missing patient banner container entirely (shows raw unboxed "أميرة وليد"). (2) Patient name is left-aligned. (3) Completely lacks labelled step tabs / guided flow. High risk of clinical misattribution.
- **23_lab_batch_editor.png**: **Defect**. Patient banner is present, but: (1) Back arrow `<-` is misplaced next to the middle date card instead of the top app bar. (2) Step tab "+ Add" and buttons "+ Add 6", "+ Add 8" use decorative "+" characters. (3) "Urine Routine & Microscopic" is Title Case.
- **25_lab_editor.png**: **Defect**. Patient banner is present and Arabic name is right-aligned, but the "Abnormal result" toggle switch is visually clipped at the bottom border of the card container, and action buttons are cramped.
- **27_report_page1.png**: **OK**. Excellent clinical hierarchy. Arabic name is properly right-aligned in header and identity card; real values shown without horizontal scrolling; status tags use word + colour ("Low risk", "Rh negative").
- **28_report_page2.png**: **Defect**. (1) An unexplained empty hyphen pill `[ - ]` renders next to the biometry date. (2) EFW displays raw `-` instead of *Not recorded*. (3) Header promises "medications and delivery plan", but neither section is visible.
- **31_settings_security.png**: **Defect**. (1) Header "Clinic & System Settings" uses Title Case. (2) Category tabs wrap awkwardly into two uneven rows (2 on top, 1 stranded on bottom row).
- **32_settings_lab_tests.png**: **Defect**. Same 2-row tab wrap and Title Case dialog header as 31.
- **33_settings_medications.png**: **Defect**. Same 2-row tab wrap and Title Case dialog header as 31. Additionally, the bottom medication indication is truncated mid-sentence ("Bone mineralization and") without ellipsis.

## Top Issues (Ranked by Clinical & Visual Impact)

1. **Missing Patient Banner & Step Tabs in Scan Editor (`20_scan_editor.png`)**: Unlike visit and lab editors, the ultrasound editor has no patient banner card (just loose left-aligned text) and no step tabs. Clinicians lack immediate visual confirmation of patient identity/parity during scan entry.
2. **Arabic Name Left-Aligned Across Patient Hub Header (`07`–`12`) and Scan Editor (`20`)**: Patient name "أميرة وليد" is left-aligned instead of right-aligned in the main hub header and scan editor, directly violating RTL standards and conflicting with the patient list and report.
3. **Tab Bar Edge Label Truncation (`09`, `10`)**: Horizontal scroll positioning cuts off outer tab titles mid-word ("...iew", "Meds...", "P..."), degrading tab discoverability.
4. **Visual Clipping & Broken Tokens in Dialogs and Reports**:
   - `25_lab_editor.png`: "Abnormal result" switch is sliced by the card container boundary.
   - `28_report_page2.png`: Empty dash badge `[ - ]` indicates a broken/unhandled status token.
   - `23_lab_batch_editor.png`: Back navigation arrow awkwardly floats beside the date card.
   - `33_settings_medications.png`: Medication indication text clipped mid-sentence at screen bottom.
5. **Settings Tabs 2-Row Wrap & Casing Inconsistencies (`31`–`33`, `11`)**: 3 tabs split onto 2 rows; Title Case persists in dialog titles ("Clinic & System Settings") and tab titles ("Active Prescriptions").

## What Could Not Be Judged

- **12 sp minimum font size compliance**: Precise typography sizing cannot be definitively verified from UIAutomator dumps without inspecting Compose source tokens (though visual glyph boxes appear >= 42 px).
- **Dark mode theme fidelity**: Screenshots only cover light mode; dark mode contrast and family token mapping remain unverified.
- **Vertical scroll completeness**: Below-the-fold content (e.g. delivery plan & meds on report page 2, full medication catalog) was not scroll-tested.
- **Dynamic interaction & gesture responsiveness**: Tap accuracy for elements near the Android navigation bar and touch targets across screen densities.
