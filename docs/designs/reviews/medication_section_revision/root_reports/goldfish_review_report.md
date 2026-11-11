# Goldfish Annotated Review Report

Status: resolved (auto accept 2026-11-09: 4 PROPOSE FIX applied to the design doc; 1 ESCALATE answered by the owner: §8 item 6 deferred as a non-goal). The earlier Stage 5 to 9 report is kept as `goldfish_review_report_stage5-9.md` (status: resolved).

**Target:** `docs/designs/patient_workspace_overview_tab_optimization.md` (full file, §8 items 3 to 7)

Scope: these are the five open questions left in §8 after the Delivery Plan decisions. They were not raised by a Goldfish reviewer. I treated each as a finding and checked it against the code. Items 1 and 2 of §8 are already decided and are not listed.

## Prioritized Findings & AI Suggestions

### [Issue 1] [`owner-open-question`] - Severity: Major (§8.3 Outcome chip colours)
*   **Affected Scope:** §4.3 field-mapping table, Outcome chip row, and §8 item 3
*   **Discrepancy Description:** The doc says the colour per outcome is open. An implementer cannot build `PastDeliveryMicroRow` without it.
*   **Rationale:** The old `PreviousPregnancyCard` used no outcome colour (a neutral `surfaceContainerHigh` badge and plain text). The codebase already uses `errorContainer` for severe states and `tertiaryContainer` for raised-but-not-severe states (`BedsideVitalsGrid.kt:91-92`, `PatientHubHeader.kt:461`). A stillbirth is a severe event, an early loss is serious, and an elective termination or a live birth is not an alarm state.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Reuse the two existing severity colours instead of adding new ones. Colour is never the only signal because the label text is always shown (already in the doc). You can overrule the mapping.
*   ***Proposed Edit:***
    *   Location: §4.3 Outcome chip row, and §8 item 3
    *   Change: Replace "The colour per outcome is open (see §8)" with: "Chip colour: `Stillbirth` uses `errorContainer`; `EarlyPregnancyLoss` uses `tertiaryContainer`; `LiveBirth`, `TerminationOfPregnancy` and free-text outcomes use the neutral `surfaceContainerHigh`. The label is always shown." Mark §8 item 3 decided with the date.

### [Issue 2] [`owner-open-question`] - Severity: Nit (§8.4 Section title)
*   **Affected Scope:** §4.5 item 2, §7.1 Phase A step 2, §8 item 4
*   **Discrepancy Description:** The title for the parity and prior-pregnancy section is listed as open, though the doc already uses the existing string.
*   **Rationale:** `previous_pregnancies` ("Previous pregnancies") exists at `res/values/strings.xml:142`. The data class is `DemoPreviousPregnancy`, which holds losses and terminations as well as deliveries, so the model already calls them pregnancies. A new string would be a second name for the same entity.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Keep the existing string. It matches the model name, adds no string, and the row's outcome chip already says whether the pregnancy ended in birth, loss or termination.
*   ***Proposed Edit:***
    *   Location: §8 item 4
    *   Change: Mark decided: "The section keeps the existing `previous_pregnancies` string. Losses and terminations are shown by the Outcome chip."

### [Issue 3] [`owner-open-question`] - Severity: Major (§8.5 GA in print or share)
*   **Affected Scope:** §3 header de-duplication bullet (line 44) and §8 item 5
*   **Discrepancy Description:** The doc asks whether a printed or shared chart needs GA in the body because the header is not part of it.
*   **Rationale:** The question rests on a wrong assumption. Print and share do not render the Overview tab. Print uses `PatientPrintHtmlGenerator` (current GA at line 1058, per-visit GA at line 105, a GA column at line 1107). Share uses `ClinicalSummaryGenerator.generateTextSummary` ("Gestational Age: ..." at `ClinicalSummaryGenerator.kt:22-23`). Both are opened from `PatientTwoPageReportDialog.kt`. Removing GA from the Overview body cannot change either output.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** A reader can check this in the cited files, so the question can be closed with evidence.
*   ***Proposed Edit:***
    *   Location: §3 line 44 and §8 item 5
    *   Change: Replace "Whether printed or shared charts need GA is open (see §8)" with "Print (`PatientPrintHtmlGenerator`) and share (`ClinicalSummaryGenerator`) are generated separately from the tab and already include GA; this design does not change them." Mark §8 item 5 closed with the same evidence.

### [Issue 4] [`owner-open-question`] - Severity: Major (§8.6 Schema sign-off)
*   **Affected Scope:** §4.4 and §8 item 6
*   **Discrepancy Description:** Structured GA at delivery and birth weight would need schema changes; whether to request them is open.
*   **Rationale:** `AGENTS.md` freezes Room schema v6 and requires clinical sign-off for any change. The decision to ask for sign-off is a project decision, and no file in the repository records it.
*   ***AI Suggestion:*** ESCALATE
*   ***Justification:*** The evidence is outside every file the reviewer can read. It is your call, and the design does not depend on it: line 2 of the row shows the recorded text today and stays that way until a separate, signed-off change.
*   ***Proposed Edit:***
    *   Location: §8 item 6
    *   Change: None until you decide. If you defer, I would rewrite it as a non-goal: "Structured GA and birth weight are a separate change that needs clinical sign-off (schema v6 is frozen). Not part of this design."

### [Issue 5] [`owner-open-question`] - Severity: Nit (§8.7 Documents card restyle)
*   **Affected Scope:** §3 non-goals (line 54) and §8 item 7
*   **Discrepancy Description:** Item 7 is a statement, not a question: it says the restyle is out of scope.
*   **Rationale:** Line 54 already lists "No restyle of the Documents card" under Non-Goals. Keeping it in Open Questions makes the doc look less settled than it is.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Remove the duplicate from §8. The Non-Goals line is the single place for it.
*   ***Proposed Edit:***
    *   Location: §8 item 7
    *   Change: Replace with "Closed: see §3 Non-Goals."

## Summary
| # | §8 item | Suggestion |
|---|---|---|
| 1 | 3. Outcome chip colours | PROPOSE FIX (stillbirth `errorContainer`, early loss `tertiaryContainer`, others neutral) |
| 2 | 4. Section title | PROPOSE FIX (keep `previous_pregnancies`) |
| 3 | 5. GA in print or share | PROPOSE FIX (close; print and share are separate and already show GA) |
| 4 | 6. Schema sign-off | ESCALATE (your decision; design does not depend on it) |
| 5 | 7. Documents restyle | PROPOSE FIX (remove duplicate; already a non-goal) |
