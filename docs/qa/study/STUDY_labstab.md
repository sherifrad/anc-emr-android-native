1 Problem as reported
Owner reports three Labs tab and lab editor issues: (1) no way to attach a file or photo to a specific lab result or lab group; (2) lab groups cannot be collapsed, so the tab is very long; (3) the tab lists each result separately, but the owner wants the printed-report comparison view (rows tests, columns dates, H and L marks) reusing the shared report data layer.

2 Root cause or current behaviour
- LabsTab.kt:32-34 computes reports via groupLabsIntoReports; lines 56-79 render one LazyColumn item per report, with no attachment handling and no report-level collapse state.
- LabReportItem.kt:56-111 wraps each report in RecordBandCard; composite panels use PanelGroup (lines 101-109) with rememberSaveable(group.title) default true (line 120), so panels start expanded and state is keyed only by group title, not patient.
- LabReportGrouping.kt:120-132 groups labs by date plus labName; partitionReportIntoCompositeGroups (line 58) groups only within one report, so there is no cross-date comparison.
- ReportData.kt:159 labReport already builds LabComparisonTable (lines 51-55) with dates, rows, and H/L marks through resolveLabFlags (lines 392-406); groupLabComparisonsByCategory (line 441) merges chunks.
- ReportData.kt:263 pictureCandidates already links attachments via DemoAttachment.linkedRecordId (ClinicalModels.kt:203) and AttachmentsSectionCard.kt:128-135 filters the LAB category.
- AttachmentsSectionCard.kt:58-177 shows patient-level attachments only; no lab-linked attachment display is visible in the provided files.

3 Proposed fix
1. Attachments: reuse linkedRecordId. When adding a lab attachment, set category LAB and linkedRecordId to the lab id or report key. In LabReportItem, filter patient.attachments by that id and show thumbnails using the AttachmentRow pattern. The report picture picker already includes them through pictureCandidates.
2. Collapsible groups: add report-level collapse in LabsTab and LabReportItem using rememberSaveable keyed by patient.id plus report date and labName. Default expanded only for the newest report (reports.first(), since reports are sorted descending in LabReportGrouping.kt:132). Use minimumTouchTarget on the header row for a 48dp expand and collapse target.
3. Comparison view: add a toggle in LabsTab that renders labReport(patient) tables using LabComparisonTable and groupLabComparisonsByCategory. Keep groupLabsIntoReports for detail editing. Do not add a second grouping rule.

4 Tests to add
- Unit: labReport produces expected comparison tables and H/L marks for repeated tests across dates.
- Unit: groupLabComparisonsByCategory merges six-date chunks and notes.
- Compose: collapse state persists per patient and group, with only the newest report expanded by default.
- Compose: attachments with a matching linkedRecordId appear in the correct lab card.
- TestTag checks for expand and collapse headers and the comparison toggle.

5 Risks, protected files, questions
Protected or level 3: ClinicalModels.kt (DemoLabInvestigation, DemoAttachment) is level 3; avoid schema or model changes. Reuse ReportData.kt as the single grouping source. Risk: default collapse could hide data, so keep headers accessible. Questions for owner: should attachments link to an individual result or the whole report, should collapse state persist across app restarts, and are any existing attachments affected?

6 Task entries for the roadmap
- Labs: attach files to lab results. Reuse DemoAttachment.linkedRecordId and LAB category; show matching thumbnails in LabReportItem. Filter attachments by linked id, add thumbnail row.
- Labs: collapsible report cards. State keyed by patient, date, and lab name, with only the newest expanded. 48dp header target, saveable state.
- Labs: comparison view. Render LabReport and LabComparisonTable in LabsTab behind a toggle. Reuse groupLabComparisonsByCategory, no duplicate grouping.

