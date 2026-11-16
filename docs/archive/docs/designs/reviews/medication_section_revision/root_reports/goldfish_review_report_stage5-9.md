# Goldfish Annotated Review Report

Status: resolved (auto accept: 17 PROPOSE FIX issues applied to the design doc; 1 contested; 1 declined; E1-E5 recorded as open questions in section 8)

**Numbering convention:** Issue 2 inserts a new §6 (Risks) and renumbers the plan to §7. All proposed edits below that mention plan sections (§6.1, §6.2, "step 3") mean the plan after renumbering (§7.1, §7.2).

**Target:** `docs/designs/patient_workspace_overview_tab_optimization.md` (`full file`, lines 1..248; no edits have been made at any stage)
**Source reports:** `zoom_out_report.md`, `comprehension_report.md`, `critic_report.md`, `intermediate_zoom_out_report.md`, `implementation_readiness_report.md`
**Orchestrator:** Claude (Elephant). The ~100 raw findings are merged below into 20 issues. Source ids are in brackets: Z = Stage 5 zoom-out, C = Stage 6 comprehension, K = Stage 7 critic (K-n = critic report items), I = Stage 8, R = Stage 9.

## Decisions only the owner can make (ESCALATE, read first)

These need facts or choices that are not in any file a reviewer could read.

- **E1. Can the Room schema freeze be lifted for structured GA and birth weight?** Every stage says the regex parser is the weakest part. The doc says a schema change needs "clinical regulatory sign-off". `AGENTS.md` confirms "Schema v6 Frozen ... without clinical sign-off", but nothing says whether sign-off was asked for or is obtainable. Recommended default below: drop the parser and show verbatim text; request structured biometrics as a separate signed-off change.
- **E2. New section composables, or flatten the existing cards?** `AGENTS.md` Rule 1 says each clinical entity has exactly one canonical component. The design builds four new section composables while `DeliveryPlanningCard` and `AttachmentsSectionCard` must stay (the Plan tab uses them, `PlanTab.kt:38, 52`), so Delivery Plan would exist twice. The zoom-out reviewers suggested a lighter route: add a flat variant to the existing cards and drop the duplicated fields. Choose A (current design, with shared composables used by both tabs) or B (flat variants of the existing cards). Issue 9 below is written for A; B would shrink issues 3, 9, 10 and 15.
- **E3. Attachments footer.** Keep `AttachmentsSectionCard` unchanged this round (recommended; matches Rule 1 and PlanTab), or redesign it as a preview strip now.
- **E5. Clinical labelling choices.** (a) Outcome chip colours: stillbirth, early loss and elective termination should not all share an "error" colour. Owner picks the mapping. (b) Section is titled "Past Deliveries" but lists losses and terminations; keep "Previous pregnancies" (existing string). (c) Does a printed or shared chart need GA in the body, since the header is not part of it?
- **E4. `canEdit` for Delivery Plan edit.** Today `DeliveryPlanningCard.onEditPatient` is not gated by `canEdit` while the other cards are. Gate it (behaviour change, consistent with archived-patient write blocking) or preserve it.

## Prioritized Findings & AI Suggestions

### [Issue 1] [`zoom-out`, `design`, `inventive`, `comprehension`, `implementation-readiness`, `codebase-details`, `style`] - Severity: Blocker
*   **Affected Scope:** [Lines 44, 153-175, 212, 220-222, 242-243](docs/designs/patient_workspace_overview_tab_optimization.md#L44) (§3.1, §4.3, §4.4, §5 row 4, §6)
*   **Discrepancy Description:** Z-B1, C-2, K-C2, I, R-5. GA and birth weight are regex-scraped from free text. The patterns match "2 weeks postpartum" and drug doses, cannot produce the example `GA 39w 2d` or `3,450g`, take the first match over two fields joined together, show no provenance, and have conflicting test inputs (`39w` vs `39w 2d`).
*   **Rationale:** Wrong clinical values would appear as authoritative biometrics on a safety-relevant screen. Fixtures show it matching gestational age at loss ("34 weeks", "9 weeks") as if it were GA at delivery.
*   ***AI Suggestion:*** PROPOSE FIX (see E1; the owner picks the direction)
*   ***Justification:*** All five review stages independently rank this first. The root problem is unstructured data. Heuristics over free text cannot meet the doc's own safety standard.
*   ***Proposed Edit:***
    *   §3.1 bullet "Non-Destructive Biometric Extraction": replace with "**Verbatim Past-Delivery Context**: Tier 2 of each past-delivery row shows `otherDetails` and `placeAndDoctor` as recorded, without inferred values. No schema change."
    *   Delete §4.4 (code block and regex). Replace with: "### 4.4 Biometrics Are Not Parsed. GA at delivery and birth weight have no structured fields. This design does not infer them. Tier 2 shows `otherDetails` (max 2 lines) and `placeAndDoctor` as written. Structured biometrics need a schema change and are a separate proposal that requires clinical sign-off."
    *   §4.3 Line 2: replace the GA, weight and Place/Doctor bullets with `Place / Doctor: placeAndDoctor verbatim` and `Notes: otherDetails verbatim (max 2 lines, ellipsized; full text in the editor)`.
    *   §5 row 4: rewrite the rationale: "Needs a Room version bump and migration; schema v6 is frozen without clinical sign-off (`AGENTS.md`, Core Architectural Constitution §4). Parsing free text was rejected because it can show wrong values as fact; showing the recorded text avoids that."
    *   §6.1 step 1 and §6.2 rows 1-2: delete (no model helper, no parsing test file).

### [Issue 2] [`structural`, `design`] - Severity: Blocker
*   **Affected Scope:** [Lines 1-216](docs/designs/patient_workspace_overview_tab_optimization.md#L216) (whole doc; K-C1, D-1-1, S-1-1, D-1-15, I-N3)
*   **Discrepancy Description:** No Risks & Mitigations section, no Open Questions, no rollout or revert plan, no reviewers or related documents.
*   **Rationale:** The feature-spec standard requires it. A six-file rewrite of the primary landing screen needs a revert story.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Required by the standard; the missing risks are real (see Issues 1, 5, 6, 8).
*   ***Proposed Edit:***
    *   Insert a new `## 6. Risks & Mitigations` before the plan and renumber the plan to §7 (6.1 to 7.1, 6.2 to 7.2). Rows: (1) removed fields hide history, mitigated by the field mapping (Issue 4); (2) allergy state shown wrongly, mitigated by three states (Issue 5); (3) row clipping at 360dp or large fonts, mitigated by a Compose test (Issue 6); (4) existing test tags break, mitigated by the tag table (Issue 8); (5) large N of prior pregnancies, mitigated by plain `Column`/`forEach` and a stated maximum (Issue 7); (6) revert: single PR, `git revert` restores the six cards; no data changes.
    *   Add `* **Reviewers:**` and `* **Related:** docs/designs for the patient directory card RFC` to the header.
    *   Add `## 8. Open Questions` listing E1 to E4 as decided or open.

### [Issue 3] [`comprehension`, `structural`, `style`, `inventive`, `implementation-readiness`] - Severity: Blocker
*   **Affected Scope:** [Lines 29-34, 65, 80, 200-201, 226](docs/designs/patient_workspace_overview_tab_optimization.md#L29) (C-1, K-C3, R-2, R-13)
*   **Discrepancy Description:** "Four sections plus a footer" is followed by five numbered items; §4.2 says "All four sections" use the container; the footer is called a "thumbnail gallery" and a "horizontal preview strip" and has no file or step.
*   **Rationale:** A reader cannot tell what is built. Stage 8 rated this a Major (a consistency fix); Stage 6 rated it a Blocker. Severity is kept as reported.
*   ***AI Suggestion:*** PROPOSE FIX (depends on E3; the text assumes "unchanged")
*   ***Justification:*** Pick one count and one treatment.
*   ***Proposed Edit:***
    *   §2 line 29: "...organized into four primary clinical domain sections separated by hairline dividers, followed by the existing Documents & Attachments card:". Item 5: "**Documents & Attachments**: unchanged `AttachmentsSectionCard`, placed below the four sections."
    *   §4.1 diagram: remove `F["Section 5: AttachmentsFooterSection"]`; show `A --> F["AttachmentsSectionCard (unchanged)"]`.
    *   §4.2: "All four sections".
    *   §4.5 item 5: "`AttachmentsSectionCard` is reused unchanged. Restyling it is out of scope."
    *   §6.1 and §6.2: add `AttachmentsSectionCard.kt` as **UNCHANGED** (still used by `PlanTab.kt`).

### [Issue 4] [`zoom-out`, `implementation-readiness`, `codebase-details`] - Severity: Blocker
*   **Affected Scope:** [Lines 142-155, 184-188](docs/designs/patient_workspace_overview_tab_optimization.md#L142) (§4.3, §4.5 item 2; Z-B2, R-5, K-4)
*   **Discrepancy Description:** The micro-row shows Year, Outcome, Mode, GA, Weight, Place/Doctor only. The old `PreviousPregnancyCard` also showed `pregnancyType`, `lossManagement`, `terminationMethod`, `birthMethod`, `deliveryBy`, `clinicianId`. No mapping from `DemoPreviousPregnancy` (`DemoModels.kt:73-92`) to the row exists. Year source, Outcome values and colours, Mode text and fallbacks are unspecified.
*   **Rationale:** Silent loss of clinical history (prior Cesarean details, loss management) contradicts the doc's own safety argument against accordions.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** The fields exist and are verified. The mapping only needs to be written and checked against `PreviousPregnancyCard.kt`.
*   ***Proposed Edit:*** Add to §4.3 a table (the implementer verifies against `PreviousPregnancyCard.kt` before coding):

    | Row element | Source | Rule |
    | :--- | :--- | :--- |
    | Year | `dateText` | First 4-digit year in the text; if none, show `dateText` as recorded; if blank, "Date not recorded" |
    | Outcome chip | `outcome` (`LiveBirth`, `Stillbirth`, `EarlyPregnancyLoss`, `TerminationOfPregnancy`), else `outcomeText` | Label text always shown; colour from theme tokens (live: tertiary container, other outcomes: error container); colour is never the only signal |
    | Mode | `birthMethod`, else `deliveryBy` | Display recorded text; classification for counts reuses `previousDeliveryCounts`; blank: omitted |
    | Tier 2 | `placeAndDoctor`, `otherDetails` | Verbatim (Issue 1) |
    | Tier 2 extras | `pregnancyType`, `lossManagement`, `terminationMethod` | Each shown as a labelled segment when non-blank (existing `PreviousPregnancyCard` strings) |
    | Provenance (`clinicianId`, `recordedAt`) | existing | Not shown in the row; unchanged in the editor (state this explicitly) |

### [Issue 5] [`inventive`, `critic-design`, `style`, `comprehension`, `codebase-details`, `zoom-out`] - Severity: Blocker
*   **Affected Scope:** [Lines 185-186, 190, 198](docs/designs/patient_workspace_overview_tab_optimization.md#L185) (C-9, K-C4, I-1/2, R-9)
*   **Discrepancy Description:** (a) The red allergy banner shows whenever `allergyHistory` is non-blank, so seeded "No known drug allergies (NKDA)" patients get an alarm; a blank value is shown as NKDA, asserting a negative nobody entered. (b) "Primigravida" is derived from an empty list, so a G2 P0 patient is mislabelled. (c) `isRhNegative(patient.bloodGroup)` will not compile (field is `bloodGroupText`) and unknown blood group is unspecified.
*   **Rationale:** Clinical safety. Verified: `DemoFixtures.kt:21, 428`; `Components.kt:244-254` already has this NKDA detection inline, treating blank as NKDA (the directory card). `PatientHubHeader.kt:289` shows the allergy for any non-blank text. Three copies of one rule would drift.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Rule 1 (single implementation) plus the three-state requirement.
*   ***Proposed Edit:***
    *   §4.5 item 3: "Allergy state comes from one shared function (extract the existing NKDA test from `ui/Components.kt:244-254`; the directory card, header and this section all call it) returning `NotRecorded`, `ExplicitlyNone`, or `Recorded(text)`. `NotRecorded` (blank) shows the existing `not_recorded` string. `ExplicitlyNone` shows the calm no-allergy line only when the whole entry is a negation (for example exactly "NKDA", "None", "No known drug allergies"); any entry that continues with other text ("No known allergies except penicillin") is `Recorded`. The existing inline test uses `contains`, which would hide such an allergy, so the shared function must be stricter (needs a unit test with both cases). `Recorded` shows the error-container banner. Changing the directory card's blank-as-NKDA behaviour is in scope and listed in §7.2."
    *   §4.5 item 2 nulliparous guard: "If `previousPregnancies` is empty, show `previousDeliverySummary(patient)` (existing, `model/ObstetricHistory.kt:73`). 'Primigravida' appears only when `gravida` is 1 or less. If G/P/A and the list disagree, show the existing summary, not a derived label."
    *   §4.5 item 4: `isRhNegative(patient.bloodGroupText)` (existing, `ui/ClinicalFormat.kt:8`). Unknown or blank blood group shows no Rh alert and "Not recorded" for the blood group.

### [Issue 6] [`critic-design`, `zoom-out`, `style`, `implementation-readiness`, `comprehension`] - Severity: Major
*   **Affected Scope:** [Lines 31, 43, 145, 225](docs/designs/patient_workspace_overview_tab_optimization.md#L43) (C-7, K-C5, R-11)
*   **Discrepancy Description:** A 40dp row, 48dp touch targets, two text tiers, a chip and two actions cannot fit as written (about 230dp of 328dp remain for tier 1 after two 48dp actions). "Zero clipping" has no check. `~120dp` per child vs total, `~540dp`, `~96dp` and other dp figures have no measurement.
*   **Rationale:** `Modifier.minimumTouchTarget()` (`ui/Components.kt:473`) only sets a minimum, so the row ends up at least 48dp.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Spec the layout the code can deliver. The reviewers' "single overflow control" idea adds a tap to delete; instead keep edit on row tap (existing `editPregnancy-<id>` tag) and one trailing delete (existing `DeleteIconButton`).
*   ***Proposed Edit:***
    *   §4.3 Dimensions: "Minimum height 48dp (grows with content and font scale). The whole row is the edit target (`Modifier.minimumTouchTarget()`); a trailing `DeleteIconButton` is the only other control. Both are hidden when `!canEdit`. Tier 1 may wrap to two lines; tier 2 is `maxLines = 2` with ellipsis."
    *   §3.1: replace "zero horizontal text clipping" with "no truncation of Outcome and Mode, checked by a Compose UI test at 360dp width and 1.3 font scale".
    *   Label every dp figure in §1.1, §2, §5 as "estimate (measured on `<device>` / not measured)". Remove "~540dp to ~120dp" or replace it with a measured figure.

### [Issue 7] [`critic-design`, `comprehension`, `inventive`, `zoom-out`] - Severity: Major
*   **Affected Scope:** [Lines 22, 187](docs/designs/patient_workspace_overview_tab_optimization.md#L187) (C-10, K-C7, I-3)
*   **Discrepancy Description:** `items(patient.previousPregnancies)` is `LazyColumn` syntax. `OverviewTab` is a `verticalScroll` Column and `ClinicalGroupSection` takes `ColumnScope` content, so a lazy list crashes on unbounded height. No expected maximum N.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Crash risk, easily fixed.
*   ***Proposed Edit:*** §4.5 item 2 timeline: "`patient.previousPregnancies.forEach { PastDeliveryMicroRow(it, ...) }` inside the section `Column` (no lazy list; the tab stays one `verticalScroll`). Expected N is at most about 10; behaviour above that is unchanged scrolling." Add to Risks (Issue 2).

### [Issue 8] [`implementation-readiness`, `codebase-details`, `comprehension`, `style`] - Severity: Blocker
*   **Affected Scope:** [Line 229](docs/designs/patient_workspace_overview_tab_optimization.md#L229) (R-4, K-8, C-8)
*   **Discrepancy Description:** "Retain legacy test tags on section containers to preserve 100% compatibility" is wrong and incomplete. `editDating` and `addPregnancyButton` are on buttons. Tags tests depend on but the plan omits: `pregnancy-<id>` (`DeleteUndoUiTest.kt:84`), `delete-previous_pregnancy-<id>`, `editPregnancy-<id>`, `editDeliveryPlan`, `deliveryHospital`, `deliveryFees`, `deliveryPlan`, `deliveryPostpartum`, `obstetricParityBadge`, `attachmentsSectionCard`, `addAttachmentBtn`. "Legacy" conflicts with the "no legacy shims" non-goal.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Verified by the codebase reviewer; the claim as written is false.
*   ***Proposed Edit:*** Replace step 3 bullet 2 with a table "Existing test tags: tag, element that carries it after the change" covering every tag above, plus: "`PastDeliveryMicroRow` carries `pregnancy-<id>` on its outer container node and `editPregnancy-<id>` on the inner clickable content node (two different nodes, so the tags do not collide; the delete button keeps its own tag). `ClinicalGroupSection` accepts a `Modifier`; callers pass the section tag." List the instrumented tests that must keep passing (`AncWorkflowTest`, `DeleteUndoUiTest`, `PatientHubDesignTest`, `PatientHubPureModelTest`) and add `./gradlew connectedDebugAndroidTest` (or the project script in `.agents/scripts/run_emulator_test_class.sh`) to verification. Say "existing tags", not "legacy".

### [Issue 9] [`implementation-readiness`, `codebase-details`, `zoom-out`] - Severity: Blocker
*   **Affected Scope:** [Lines 220-248](docs/designs/patient_workspace_overview_tab_optimization.md#L238) (§6.2; R-2, R-3, K-1, K-3, Z-M2)
*   **Discrepancy Description:** The file list is incomplete. No DELETE rows for `GestationalDatingCard`, `ObstetricParityCard`, `MedicalHistoriesCard`, `PreviousPregnancyCard` (the only caller is `OverviewTab.kt`), despite "no dead composables". `PlanTab.kt` and `DeliveryNotesParser.kt` are missing. `DeliveryPlanningCard` and `AttachmentsSectionCard` stay alive via `PlanTab.kt`. `parsed.doctor` and `parsed.deliveryPlan` do not exist (`DeliveryNoteSections` has `hospital`, `fees`, `plan`, `postpartum`).
*   **Rationale:** A write allowlist cannot be enforced; the implementer must invent unlisted changes. Depends on E2.
*   ***AI Suggestion:*** PROPOSE FIX (written for E2 option A)
*   ***Justification:*** Needed under either option; B changes which rows are Delete.
*   ***Proposed Edit:***
    *   §7.2: add **DELETED** rows for the four cards, each with "no remaining callers after `OverviewTab.kt` changes (grep checked)". Add `ui/patienthub/components/DeliveryPlanningCard.kt` as MODIFIED or UNCHANGED per E2, and `PlanTab.kt` as MODIFIED if the shared composable replaces its card content (Rule 1: one Delivery Plan implementation used by both tabs).
    *   §4.5 item 4: Planned Facility = `parsed.hospital`; Intended Mode / Plan = `parsed.plan`; Postpartum = `parsed.postpartum`; Fees = `parsed.fees` (as today). Remove "Attending Clinician (`parsed.doctor`)". If a clinician field is wanted, add it to `parseDeliveryNotes` and list `ui/DeliveryNotesParser.kt`.
    *   Add rows for `ui/Components.kt` (shared allergy-state function, Issue 5) and the directory card file it touches.

### [Issue 10] [`implementation-readiness`, `codebase-details`] - Severity: Blocker
*   **Affected Scope:** [Lines 219-237](docs/designs/patient_workspace_overview_tab_optimization.md#L219) (R-1, K-10, Y-9)
*   **Discrepancy Description:** One flat "Single Unified Execution List" with no buildable intermediate states. Step 3 breaks the build until steps 2 and 4 are done; strings (step 4) are needed by steps 2 and 3. Physical-device step is not marked gating or advisory.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** The standard requires phases that each leave the build green.
*   ***Proposed Edit:*** Replace §7.1 with phases: **A** shared allergy-state function + its unit test + strings; exit `./gradlew testDebugUnitTest`. **B** `ClinicalGroupSection`, `PastDeliveryMicroRow`, `OverviewSections.kt` added but not wired; exit `./gradlew assembleDebug`. **C** `OverviewTab` swap, delete four cards, tag table, `connectedDebugAndroidTest`; exit: unit and instrumented tests pass. **D** advisory: screenshots on device at 360dp with the seeded multiparous and nulliparous patients (name the fixture ids from `DemoFixtures.kt`; "Maya" and "Elena" are not defined in the doc).

### [Issue 11] [`implementation-readiness`, `codebase-details`, `style`] - Severity: Major
*   **Affected Scope:** [Lines 227, 230-231, 248](docs/designs/patient_workspace_overview_tab_optimization.md#L230) (R-6/7/11, K-7)
*   **Discrepancy Description:** Strings plan lists four keys that overlap existing keys (`delivery_mode_label` vs `historical_delivery_mode`, `past_deliveries_title` vs `previous_pregnancies`, `primigravida_no_prior_deliveries` vs `no_previous_pregnancies`, NKDA vs `no_allergy_history`) and omits section titles, allergy and Rh texts, GA label, fallbacks, content descriptions. No signatures for the new composables, no event mapping, no `canEdit` rules.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Proposed Edit:*** Replace the string step with "Reuse: `previous_pregnancies`, `no_previous_pregnancies`, `no_allergy_history`, `not_recorded`, `historical_delivery_mode`, `place_and_doctor`, `other_details`, `ga_prefix`. Add (with exact English values): four section titles, Rh alert text, allergy banner label, edit/delete content descriptions." Add §4.6 "Signatures and Events": each section takes plain callbacks or `null` (null when `!canEdit`); `OverviewTab` keeps constructing `AncEvent`s (`StartDatingEditor`, `StartPregnancyEditor`, `StartPatientEditor`, `RequestDelete`) as today; Delivery Plan edit gating follows E4. `ClinicalGroupSection` title row marks the title `semantics { heading() }` and its action slot applies `minimumTouchTarget()`; the title `Text` gets `Modifier.weight(1f)`.

### [Issue 12] [`design`, `inventive`, `comprehension`, `zoom-out`] - Severity: Major
*   **Affected Scope:** [Lines 21, 30, 42, 180-181](docs/designs/patient_workspace_overview_tab_optimization.md#L21) (K-C10, C-3, Z-N1)
*   **Discrepancy Description:** Header duplication principle is applied selectively; header behaviour on scroll is unstated; the `datingMethod` list differs from `ClinicalModels.kt:211`; EDD source is not shown.
*   **Rationale:** Verified in `PatientHubHeader.kt`: the expanded header shows GA (line 265) and the collapsed header keeps a compact GA (`gaCompact`, line 321-354). The Inventive reviewer's concern that GA disappears on scroll is therefore not true for GA; the reviewer did not read the header file.
*   ***AI Suggestion:*** PROPOSE FIX (CONTEST the "header may hide GA" part with evidence)
*   ***Justification:*** State what the header shows in both states and what the body keeps.
*   ***Proposed Edit:*** §3.1 "Header De-duplication": "The header shows GA in both expanded and collapsed states (`PatientHubHeader.kt`); the body therefore omits GA. The body keeps EDD and its source. Whether printed or shared charts need GA is an open question (E5)." §4.5 item 1 method tag: `datingMethod` values `LMP`, `Ultrasound`, `IVF_TRANSFER`, `Clinical`; label the EDD source (agreed / ultrasound / calculated).

### [Issue 13] [`comprehension`, `structural`, `style`, `design`] - Severity: Major
*   **Affected Scope:** [Lines 44-45, 50, 57, 211-212](docs/designs/patient_workspace_overview_tab_optimization.md#L44) (C-5, K-C12, I-5)
*   **Discrepancy Description:** "SAMD", "Rule 2", "Rule 3", "Rule of Reusability", "constitutional rule", "hash invalidation" are undefined or unsourced. Line 211 claims accordions "violate SAMD principles" without the concrete reason.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** The rules are written in `AGENTS.md` ("Core Architectural Constitution"), a file a human reader of the repo can open; the doc must cite it. "Hash invalidation" should say what actually happens.
*   ***Proposed Edit:*** Expand "SAMD" once ("Software as a Medical Device"). Replace rule names with section links: "`AGENTS.md` Core Architectural Constitution §1 (Reusability), §2 (file size), §3 (Zero Legacy Baggage), §4 (schema v6 frozen, 360dp)". §5 row 4: "Room validates a schema identity hash; any entity change needs a version bump and migration, which the constitution forbids without clinical sign-off." Row 3: "Prior Cesarean and complication history must be visible without a tap."

### [Issue 14] [`zoom-out`, `codebase-details`] - Severity: Major
*   **Affected Scope:** [Lines 79-140, 145, 185-198](docs/designs/patient_workspace_overview_tab_optimization.md#L79) (Z-M1, Z-M3, K-C6, R-6)
*   **Discrepancy Description:** The design reinvents logic that exists (`isStillbirth`, `isEarlyLoss`, `previousDeliveryCounts`, `previousDeliverySummary`, `isRhNegative`, `FactRow`, `ObstetricParityBadge`, `minimumTouchTarget`). It also pastes a full composable body and regex into a design doc (altitude).
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Rule 1; zoom-out standard (no code-level detail in the feature range).
*   ***Proposed Edit:*** Add a "Reuses" list at the top of §4 naming each existing helper with its file. Replace the §4.2 code block with the signature only (`title`, `icon`, `iconTint`, `iconBgColor`, `actionButton`, `modifier`, `content: ColumnScope.() -> Unit`) and the behaviour bullets (24dp icon capsule, bold title, optional action slot, 0.75dp divider). State that `minimumTouchTarget` is the existing helper at `ui/Components.kt:473`.

### [Issue 15] [`zoom-out`, `critic-design`, `inventive`] - Severity: Major
*   **Affected Scope:** [Lines 205-213](docs/designs/patient_workspace_overview_tab_optimization.md#L205) (K-C9, Z-N3, I)
*   **Discrepancy Description:** Every alternative is "Rejected by author" with no evidence. Missing options: flatten the existing cards; structured entry at input time; Material 3 `ListItem` rows. Line 210 has unescaped `|` characters that split the table row.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Proposed Edit:*** Change the "Evaluation" column to "Decision and evidence". Add rows: "Flatten the existing cards (no border, hairline divider) and remove duplicated fields" (outcome per E2), and "Structured GA / weight entry in the pregnancy editor" (deferred: needs sign-off, see E1). Escape the pipes in row 2: use `Year / Mode / Outcome / GA / Birth Weight`.

### [Issue 16] [`style`, `implementation-readiness`] - Severity: Major
*   **Affected Scope:** [Lines 228, 247](docs/designs/patient_workspace_overview_tab_optimization.md#L228) (Y-1, Z-M4)
*   **Discrepancy Description:** Two line budgets for `OverviewTab.kt` ("under 180" vs "~150, down from bloated"). The file is 152 lines now.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Proposed Edit:*** One budget: "At most 180 lines (152 today). `OverviewTab.kt` keeps the `AncEvent` construction." Remove "down from bloated card stack". Add a cap for `OverviewSections.kt` (1,500-line ceiling in `AGENTS.md`; aim for one file per section if it passes 400 lines).

### [Issue 17] [`style`, `comprehension`] - Severity: Major
*   **Affected Scope:** Lines 12, 20, 22, 23, 29, 32, 34, 57, 149, 201, 209-213 (Y-6, Y-8, C-15)
*   **Discrepancy Description:** Subjective words with no criteria ("heavy", "severe", "bloat", "calm", "unmissable", "clean", "useless", "clunky", "un-scannable", "cohesive", "card fatigue"). Colours named instead of tokens. Hard-coded English strings (`Primigravida ...`, NKDA) where `strings.xml` is the convention.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Proposed Edit:*** Wording pass: replace each with the measurable fact (for example "six bordered cards, about N dp before the first novel history, estimate") or delete it. Name colour tokens (`errorContainer`, `tertiaryContainer`). Move literal UI text to string-resource names.

### [Issue 18] [`style`, `structural`, `design`] - Severity: Nit
*   **Affected Scope:** Lines 5, 45, 57, 59-77, 245; §6.2 test path (S-9..S-13, Y-13..Y-18, R-14)
*   **Discrepancy Description:** LaTeX `$\ge 48\,\text{dp}$` vs plain text; repeated frozen-schema statements; "Rejected by author" repeated; title-case ampersand headings; proposed files not marked "(proposed)"; diagram node `C4` is a requirement; test path spelled two ways; em-dash fallback conflicts with the recent em-dash removal commit (`00f275f`).
*   ***AI Suggestion:*** PROPOSE FIX (cheap, batch with Issue 17)
*   ***Proposed Edit:*** Use "at least 48dp"; state the schema freeze once (in §3.2) and reference it; mark the four created files "(proposed)"; remove `C4` from the diagram; use `app/src/test/java/...` throughout; replace the `—` fallback with the existing `not_recorded` string.

### [Issue 19] [`critic` and `comprehension`, claim that `Modifier.minimumTouchTarget()` does not exist] - Severity: Major (as reported)
*   **Affected Scope:** Line 145 (K-C6, C-6)
*   **Discrepancy Description:** Reported as "not a standard Compose API; not found in the codebase".
*   ***AI Suggestion:*** CONTEST
*   ***Justification:*** It exists: `Modifier.minimumTouchTarget()` at `app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt:473` (`sizeIn(minWidth = 48.dp, minHeight = 48.dp)`), imported in `PatientDirectoryCard.kt:46` and used in the editors. The real gap (not saying it is a project helper) is fixed in Issue 14. Verified by the Stage 8 reviewer and the Stage 9 codebase reviewer.

### [Issue 20] [`inventive`, `critic-design`] - Severity: Major (as reported)
*   **Affected Scope:** §4.4 (suggestions in K-C2, F1)
*   **Discrepancy Description:** Add a versioned parser returning value, source span and confidence; add canonical tokens (`GA:39+2 BW:3450g`) written by the editor into `otherDetails`; add a shared `PatientSummary` state object; add a font-scale 1.0/1.3/2.0 and RTL test matrix.
*   ***AI Suggestion:*** DECLINE
*   ***Justification:*** These grow the design instead of removing the root problem (Stage 8 zoom-out note). The parser goes away under Issue 1, a canonical token adds a second write path and a mixed-format corpus, and a `PatientSummary` object is not needed (the header already carries the data, Issue 12). One font-scale check at 1.3 is kept (Issue 6). Recorded as accepted trade-offs, revisit if E1 allows structured data.

## Summary for the owner
- PROPOSE FIX: 17 issues (1-18 except 19, 20). CONTEST: 1 (19). DECLINE: 1 (20). ESCALATE: 4 (E1-E4, listed first).
- Issues 3, 9 and 10 change if E2 is "flatten existing cards".
- Applying all proposed edits changes sections 2, 3, 4, 5 and 6 of the doc and adds sections 6 (Risks) and 8 (Open Questions), renumbering the plan to 7.

## Second opinion (opencode DeepSeek, read-only, folded in above)

Checked by a different vendor (opencode `deepseek-v4.1-flash-free`, no file writes, run from an empty scratch folder). It confirmed Issue 1 as the safest option, provided the source text stays reachable in print and share. It found six problems in this report, now fixed: plan numbering was inconsistent (see convention at top); `pregnancy-<id>` and `editPregnancy-<id>` could collide on one node (Issue 8); the print/share argument in Issue 12 contradicted itself (now E5c); the NKDA test could hide "No known allergies except penicillin" (Issue 5); one error colour for every non-live outcome is a clinical choice (E5a); the directory card behaviour change in Issue 5 needs its own test and file row.

Points the report still does not resolve (add as open items, or ignore):
- §4.5 item 4 promises "Postpartum & Contraception notes", but `DeliveryNoteSections` has no contraception field. Say postpartum only, or add a field.
- §4.5 item 1 lists "LMP date, IVF transfer date / embryo age" with no display rule.
- No empty-state text when `hospital`, `plan` or `postpartum` is missing; use `not_recorded`.
- "Monospaced tabular figures" is not checked against an existing font or token.
- §6.2 file links are absolute `file:///Users/...` paths; use repo-relative links.

## Lane notes (MACA)
- Mode: Light path, doc-only change; the reviewer lane is a different vendor from the author.
- opencode DeepSeek: used (read-only second opinion). zcode: not used. `zcode -p` returns "Select a model before sending a message" and no model can be selected from the command line; it needs a sign-in or model choice in the zcode app, which this session cannot do.
- Nothing was sent that contains patient data (the doc and report name only fixture patients).
