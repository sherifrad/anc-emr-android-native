# Stage 5 report: Initial Zoom Out Review

Target: `docs/designs/patient_workspace_overview_tab_optimization.md` (lines 1..248)
Reviewers: `egm-zoom-out`, `egm-inventive-catalyst`
Context read: `ClinicalModels.kt`, `OverviewTab.kt`, `strings.xml`. The zoom-out reviewer also read `DeliveryNotesParser.kt`, `ObstetricHistory.kt`, `PreviousPregnancyCard.kt` and `DemoModels.kt`. The inventive reviewer did not read `PatientHubHeader`, `DemoPreviousPregnancy` or the old card composables, so its claims about those are inferred and need checking.
Not-yet-existing files linked by the doc (treated as proposed): `ClinicalGroupSection.kt`, `PastDeliveryMicroRow.kt`, `OverviewSections.kt`, `DeliveryBiometricsParsingTest.kt`.

Verdict: not passing. 2 Blockers, 7 Majors, several Nits.

## Part A: Zoom-out findings

### 🔴 B1. Regex scraping of GA and birth weight from free text (Sections 3.1, 4.4, 5 row 4, 6.1 step 1)
- The root problem is that GA and weight are not structured. The doc builds a heuristic around the frozen schema instead.
- `\d{1,2}\s*w` matches "12 w" in "12 weeks since", "2w post-op", "6w postpartum". The weight pattern `[1-5]\d{3}\s*g` matches "2000 g" in any context, and the kg branch can match maternal weight.
- `find()` runs over `otherDetails` and `outcomeText` joined together, so a wrong value shows as authoritative with no provenance.
- The GA regex cannot capture `39w 2d`, the form used in the tests and the Section 4.3 mockup.
- The `—` fallback mixes "not recorded" with "recorded but not recognised".
- Simpler options: show `otherDetails` and `placeAndDoctor` verbatim on tier 2 (as `PreviousPregnancyCard` does with `FactRow`), or propose structured biometrics as a separate signed-off change. The schema freeze is asserted, not shown to be unavoidable.

### 🔴 B2. Existing prior-pregnancy information silently dropped or demoted (Sections 4.3, 4.5 item 2)
- `PreviousPregnancyCard` shows `dateText`, `outcomeText`, `pregnancyType`, `outcome`, `deliveryBy`, `otherDetails`, `placeAndDoctor`, `birthMethod`, `lossManagement`, `terminationMethod`, `clinicianId`. The micro-row shows Year, Outcome, Mode, GA, Weight, Place/Doctor.
- `lossManagement`, `terminationMethod`, `pregnancyType` (twins), raw `otherDetails` and raw `dateText` have no place in the new layout. The doc does not say if they are dropped or moved.
- "Year" has no stated source (`dateText` is free text). "Mode" and "Outcome" derivation from `outcome`, `birthMethod`, `deliveryBy` is not stated.
- The doc rejects accordions because they hide scar history, but dropping fields hides history the same way.
- Needed: a field-by-field mapping and a tap-to-expand or detail path for the remaining fields.

### 🟡 M1. Reinvents existing logic (Sections 4.4, 4.5)
- `model/ObstetricHistory.kt` already has `previousDeliveryCounts`, `isStillbirth`, `isEarlyLoss`, `previousDeliverySummary`. Reuse them for Mode, Outcome and the "Primigravida / No prior deliveries" text.
- `ui/DeliveryNotesParser.kt` returns `hospital`, `plan`, `postpartum`, `fees`. Section 4.5 item 4 uses `parsed.doctor` and `parsed.deliveryPlan`, which do not exist, and 6.2 does not list the parser change. The parser is `internal` in `ui`, while the new helper would sit in `model/`.

### 🟡 M2. Large new component layer for a layout change (Sections 4.1, 4.2, 6)
- The existing cards (`GestationalDatingCard`, `ObstetricParityCard`, `MedicalHistoriesCard`, `DeliveryPlanningCard`, `PreviousPregnancyCard`) already carry test tags and `canEdit` handling. A flat variant or one shared section scaffold, plus dropping the redundant fields, was not weighed in Section 5.
- `ClinicalGroupSection` has one consumer set and is labelled "canonical".
- The mermaid diagram names sub-components with no files in 6.2 (`ObstetricParityBadge`, `PastDeliveriesMicroTable`, `AllergyCalloutBanner`, `AttachmentsFooterSection`, Rh alert). Sections 2, 3 and the title disagree on four, five, or four plus a footer.
- 4.5 item 5 reuses `AttachmentsSectionCard` while item 2 "replaces" six cards.

### 🟡 M3. Altitude drops (Sections 4.2, 4.4)
- Full composable bodies (padding, spacing, 15dp icon, alpha 0.35) and the full regex source are pasted into the doc.
- Pixel claims (~180dp, ~540dp to ~120dp, ~96dp padding) are unmeasured estimates.

### 🟡 M4. Contradictions in constraints and plan (Sections 3, 6.1)
- "Under 180 lines" vs "about 150 lines" in 6.2, but `OverviewTab.kt` is already 152 lines, mostly event wiring that has to live somewhere.
- Keeping legacy test tags "on section containers" conflicts with "zero legacy baggage". Tags such as `editDating` and `addPregnancyButton` sit on buttons, so moving them breaks `performClick` tests. Affected androidTests (`AncWorkflowTest`, `DeleteUndoUiTest`) are not listed.
- "40dp content height expanded to 48dp" is unclear. Five elements plus two 48dp actions will not fit 328dp, which is the width the doc used to reject the 5-column row.
- Edit and delete are hidden when `!canEdit` in the old card. The doc does not say the same applies.
- Existing strings overlap new ones (`no_previous_pregnancies`, `previous_pregnancies`, `no_allergy_history`, `delivery_planning`, `place_and_doctor`, `obstetric_history`). `OverviewTab.kt` hardcodes "Prior Pregnancies (n)" and the doc does not say it will fix that.

### 🟢 Nits
- N1. The doc does not verify the header shows EDD, G/P, blood group, allergies in all states. `ObstetricParityBadge` re-adds G/P in the body, the duplication the doc set out to remove.
- N2. `isRhNegative` already exists in `ui/ClinicalFormat.kt`. Say it is reused. The Rh alert may belong in the header, since blood group is there.
- N3. Section 5 lists only "Rejected by author" rows and omits the flatten-existing-cards option. Device and chart names (DNY-NX9, Maya, Elena) belong in a test plan. There is no rollout or revert story for a six-file rewrite of the landing screen.

Recommended direction from the reviewer: drop the regex parser and show verbatim text; reuse `ObstetricHistory.kt` helpers; add a flat variant or shared scaffold to existing cards; add the field mapping and `canEdit` rules.

## Part B: Inventive Catalyst findings

### 🟡 F1. False compromise: schema migration vs regex over free text (Sections 3.1, 4.4, rejected "dedicated columns")
- The regex does not give the same clinical outcome as structured data. Examples that mislead: "induced at 41w, baby 3 weeks in NICU", "epidural 2 weeks postpartum", "2000 g of saline", "3 kg" of maternal weight. The `+d` group is non-capturing, so "39+2" with no unit is never matched.
- The freeze applies to Room v6, but the codebase already stores text fields. A non-Room route was not examined. Parsing also runs per recomposition per row.
- Proposed: a pure versioned parser returning value, source span and confidence; show parsed values visibly as "from notes"; show nothing when several candidates match; memoize with `remember(pregnancy.id, revision)`; let the editor write a canonical token (for example `GA:39+2 BW:3450g`) into `otherDetails`, read first, regex last; raise the schema question with the regulatory owner separately; add adversarial negative tests.
- Liabilities: a second write path users can edit away; mixed-format corpus forever; extra visual noise; reduced coverage on legacy data.

### 🟡 F2. False compromise: density needs removing GA, parity and allergy from the body (Sections 3.1, 4.5)
- The header's scroll behaviour is not stated. If it collapses, the body becomes the only copy and removing GA is a loss. Print/PDF and screenshot cases are not covered.
- The principle is applied selectively: allergy and Rh banners duplicate header data while GA is removed. EDD stays but the source that produced it is not shown.
- Proposed: one shared `PatientSummary` consumed by header and body, or state that the header is pinned and add a test; show the EDD source label (Agreed, US, Calculated).
- Liabilities: shared state dependency; scroll-driven visibility is hard to test.

### 🟡 F3. "40dp, 48dp, zero clipping" cannot all hold (Section 4.3)
- Tier 1 with Year, chip, a long Mode label and two 48dp actions leaves about 232dp, which a label like "Spontaneous Vaginal (SVD)" will not fit. Tier 2 has no wrap or ellipsis rule. A fixed 40dp row fails at large font scale, with RTL, or with long place names.
- Proposed: min-height not fixed height; tier 2 may wrap; one 48dp overflow control (the "More actions" sheet already exists); abbreviation table for Mode with long form in `contentDescription`; test matrix at font scale 1.0, 1.3, 2.0 with widest-string fixtures.
- Liabilities: one extra tap for edit/delete, which needs delete confirmation; row height becomes a range.

### 🟡 F4. Test-tag compatibility and file-size rule not shown to be satisfiable (6.1 step 3, 4.1, 6.2)
- A tag on a plain Column does not reproduce the old cards' merged semantics and click targets. 4.1 and 6.2 disagree on sections. The old card files are neither deleted nor kept by 6.2, against Rule 3. The doc does not choose whether `OverviewTab` builds events or the sections do. `canEdit` handling differs: the old code passes `null` callbacks, and `DeliveryPlanningCard` receives `onEditPatient` ungated.
- Proposed: an `OverviewActions` holder that builds events once; one file per section; list old cards as DELETED (after grepping other uses) or justify keeping them; add an old-tag to new-node mapping table; run existing UI tests before and after.

### 🟢 F5. Negatives shown as facts (Section 4.5 items 3, 4)
- Blank `allergyHistory` means "not recorded", not "none". Showing NKDA asserts a negative nobody entered. Rh warning behaviour for unknown blood group is not stated. "Primigravida" is a G=1 fact, not "no list entries".
- Proposed: three states (recorded, explicitly negative, not recorded) using the existing `not_recorded` string; derive "Primigravida" only from `gravida == 1`; show a hint when G/P/A and the list disagree.

### 🟢 F6. Consistency and verification gaps (Sections 2, 5, 6)
- Count mismatches (four vs five sections). No component specified for the "horizontal preview strip". `Modifier.minimumTouchTarget()` may not exist. Hard-coded literals in 4.3 where `ga_prefix` exists. No heading semantics for TalkBack, no dark-mode contrast check, no automated check beyond two device charts. Color-only status chips need a redundancy rule. Height figures are estimates.

## Cross-reviewer overlap (priority order for the author)
1. Free-text regex for GA and weight (B1, F1): both reviewers rate it the top problem.
2. Row layout and prior-pregnancy field loss (B2, F3, M4).
3. Component layer vs flattening existing cards, test tags, file plan (M2, M4, F4).
4. Reuse of `ObstetricHistory.kt`, `DeliveryNotesParser`, existing strings (M1, M4).
5. Header duplication and negative-history display (N1, F2, F5).
6. Altitude and unmeasured estimates (M3, F6).
