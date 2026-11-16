# Stage 9 report: Implementation Readiness Test (round 1)

Target: `docs/designs/patient_workspace_overview_tab_optimization.md`, DetailedRange lines 217..248 (§6 Detailed Implementation Plan)
Reviewers (all completed, none retried): `egm-implementation-readiness` (R), `egm-codebase-details` (K), `egm-style` (Y). Standard: `implementation-readiness-standard.md` for R and K.
The design doc has not been edited at any stage, so Stage 5 to 8 findings still stand.

Verdict: FAIL. 6 Blockers (after merging duplicates), 9 Majors, 8 Nits. Plan is not implementable without the implementer making decisions.

## Facts verified in code by the codebase reviewer
- `Modifier.minimumTouchTarget()` exists at `ui/Components.kt:473` (`sizeIn(minWidth = 48.dp, minHeight = 48.dp)`). It enlarges an element to at least 48dp, so a "40dp row" becomes at least 48dp.
- `isRhNegative(bloodGroup: String?)` is `internal` at `ui/ClinicalFormat.kt:8`. The patient field is `bloodGroupText`, not `bloodGroup`, so the doc's `isRhNegative(patient.bloodGroup)` (line 198) will not compile.
- `previousDeliverySummary` is at `model/ObstetricHistory.kt:73`. For G2P0 it says "P0 · No prior deliveries".
- `DeliveryNoteSections` has only `hospital`, `fees`, `plan`, `postpartum` (`ui/DeliveryNotesParser.kt:3-8`). The type and parser are `internal` but reachable from `ui.patienthub.components`.
- All six tags named in the plan exist: `datingCard`, `obstetricParityCard`, `medicalHistoriesCard`, `deliveryPlanningCard`, `editDating`, `addPregnancyButton`. `editDating` (`GestationalDatingCard.kt:103`) and `addPregnancyButton` (`ObstetricParityCard.kt:96`) are on buttons.
- `FactRow` (`Components.kt:423, 438`), `ObstetricParityBadge(gravida, parity, abortion)`, `AttachmentsSectionCard`, `DeleteIconButton`, `HorizontalDivider` exist.
- `OverviewTab.kt` is the only caller of `GestationalDatingCard`, `ObstetricParityCard`, `MedicalHistoriesCard`, `PreviousPregnancyCard`. `DeliveryPlanningCard` (`PlanTab.kt:38`) and `AttachmentsSectionCard` (`PlanTab.kt:52`) are also used by `PlanTab.kt`, which §6.2 omits.
- Stored outcome values are `LiveBirth`, `Stillbirth`, `EarlyPregnancyLoss`, `TerminationOfPregnancy`. The doc shows "Live Birth" and "Early Loss" and gives no colours for stillbirth or termination.
- Fixtures store `"No known drug allergies (NKDA)"` as `allergyHistory` (also `DemoFixturesVerificationTest.kt:32`, `AncStateTest.kt:458`). The banner rule `!isNullOrBlank` would show the red allergy banner for them.
- Proposed files (`ClinicalGroupSection.kt`, `PastDeliveryMicroRow.kt`, `OverviewSections.kt`, `DeliveryBiometricsParsingTest.kt`) do not exist and are correctly marked CREATED. No finding for their absence.

## 🔴 Blockers

### R1. No phases or checkpoints (R-1, K-10)
§6.1 is one flat "Single Unified Execution List". Step 3 deletes the old stack and rewires `OverviewTab.kt` and depends on steps 2 and 4, so the build is broken between steps. Test and build checkpoints exist only in step 5. Suggested phases, each with an exit command:
- A: model helper plus its unit test, run unit tests.
- B: new composables and strings, not wired.
- C: `OverviewTab` swap and deletion of old cards.
- D: device verification.
Also state whether the physical-device step (Honor DNY-NX9) is gating or advisory, and where "Maya" and "Elena" fixtures live (not confirmed to exist by those names; R-9, K-10, Y-10).

### R2. File boundary incomplete, no write allowlist possible (R-2, K-3, K-8, K-1, Y-5)
- Composables named in §4.1 appear in no step or file row: `ObstetricParityBadge` use, `PastDeliveriesMicroTable`, `AllergyCalloutBanner`, `AttachmentsFooterSection`, Rh alert, structured history `FactRow`s.
- Step 2 lists four sections for `OverviewSections.kt`. §4.1 and §4.5 item 5 have a fifth, Attachments, with no file or step. The existing `AttachmentsSectionCard` is a bordered Card with a filter state and a Button, not a "horizontal preview strip". Either it stays (contradicting the no-floating-cards goal) or an unlisted redesign is needed.
- `ui/DeliveryNotesParser.kt` is not listed, yet §4.5 uses `parsed.doctor` and `parsed.deliveryPlan`, which do not exist. "Attending Clinician" and "Intended Mode" have no data source. The plan must say to use `plan` and drop the doctor, or add fields and list the file.
- `PlanTab.kt` is not listed. It keeps a full duplicate of the Delivery Plan and Attachments content.
- `OverviewSections.kt` hosts four large sections with no size cap, while `OverviewTab.kt` has a 180-line cap. The two caps for `OverviewTab.kt` also disagree: "under 180 lines" (line 228) vs "~150 lines, down from bloated card stack" (line 247). The file is 152 lines now (K-9, Y-1).

### R3. No DELETE rows for replaced composables, contradicting "no dead composables" (R-3, K-3, K-13)
`GestationalDatingCard`, `ObstetricParityCard`, `MedicalHistoriesCard`, `PreviousPregnancyCard` will have no callers after the refactor, and §6.2 has no Delete action. The files that carry the current test tags (`DeliveryPlanningCard.kt`, `ObstetricParityCard.kt`, `GestationalDatingCard.kt`, `MedicalHistoriesCard.kt`) would vanish. `DeliveryPlanningCard` and `AttachmentsSectionCard` stay alive via `PlanTab.kt` and need "UNCHANGED" or a modification entry. Add Delete rows with caller lists, or drop the Rule 3 claim.

### R4. Test-tag list is incomplete and mis-scoped; "100% test compatibility" is false (K-2, R-8, Y-2)
- Missing tags that existing tests depend on: `pregnancy-<id>` (prefix used in `DeleteUndoUiTest.kt:84`), `delete-previous_pregnancy-<id>` (from `DeleteIconButton`), `editPregnancy-<id>`, `editDeliveryPlan`, `deliveryHospital`, `deliveryFees`, `deliveryPlan`, `deliveryPostpartum`, `obstetricParityBadge`, `attachmentsSectionCard`, `addAttachmentBtn`.
- `editDating` and `addPregnancyButton` are on buttons, so "directly on section containers" is wrong for them.
- `PastDeliveryMicroRow` must carry `pregnancy-${id}` because delete and undo tests find the row by that tag and then its delete button. `ClinicalGroupSection` has no `testTag` parameter; a modifier carrying one is unspecified.
- `PatientHubDesignTest.kt` and `PatientHubPureModelTest.kt` are not enumerated. `testDebugUnitTest` does not run instrumented tests, so `connectedAndroidTest` must be named. "Legacy" tags sits under the "no legacy shims" non-goal (Y-2).

### R5. Parsing spec contradicts itself and the display (R-4, K-6, Y-3, Y-4)
- Test inputs conflict: line 222 `39w`, `3.4kg`, `3400g`; line 243 `39w 2d`, `3.5kg`, `3500g`, empty/null.
- The regex returns only the matched unit text (`39w`), never days or the `39+2` form. The row spec shows `GA 39w 2d` and `3,450g`; neither can be produced. The weight regex does not match comma-grouped `3,450 g`. A raw string like `3.25 kg` is shown as-is, with no formatter or normalisation specified.
- False matches ("2 weeks postpartum", "treated for 6 w") would be shown as GA at delivery. "Uncomplicated spontaneous vaginal delivery at term" gives `—` though "term" is informative. "Safely" and "various clinical input notations" are unsupported claims (Y-4).
- Decisions the plan must make: exact output format; `NNw Nd` and `NN+N` handling; precedence of `outcomeText` vs `otherDetails`; precedence for multiple matches; an "approximate, from notes" label; where `—` is produced (no function or string resource exists for it); range limits.
- Test plan lacks cases for `39+2`, comma weights, false positives, `null`, blank, both fields present, out-of-range values (such as `99w`, `5kg`).
- Placement in `model/ClinicalModels.kt`, alongside medication, ultrasound and dating code, is not justified; `ObstetricHistory.kt` already holds outcome-parsing logic (K-6, R-13). The `Regex` is built per call (R-13).

### R6. Prior-pregnancy row has no field mapping, and fields the old card showed are silently dropped (R-5, K-4)
`DemoPreviousPregnancy` (`DemoModels.kt:73-92`) already has `outcome`, `birthMethod`, `deliveryBy`, `placeAndDoctor`, `pregnancyType`, `dateText`, `lossManagement`, `terminationMethod`. The plan does not say:
- how Year is derived from free-text `dateText`;
- how Outcome maps to the green and rose chips (stored values, colours for stillbirth and termination, theme token source);
- what Mode renders (`CS`, `Vaginal`, `Hysterotomy` vs "Cesarean Section (CS)" and "Spontaneous Vaginal (SVD)") and the fallback when `birthMethod` is null;
- precedence of `outcomeText` vs `outcome`;
- how `lossManagement`, `terminationMethod`, `pregnancyType`, `clinicianId` display, since the old card showed them. Dropping them is a silent clinical-data regression given the plan's own safety framing (line 211).
A field-to-display mapping table is needed.

## 🟡 Majors

### R7. Unresolved data references in §4.5 (R-6, K-14)
`parsed.hospital`, `parsed.doctor`, `parsed.deliveryPlan` undefined. `patient.bloodGroup` should be `bloodGroupText`. `patient.gravida`, `parity`, `abortion` unverified. "Postpartum & Contraception notes" names no field. Dating fallback `agreedEdd ?: ultrasoundEdd ?: calculateEstimatedDueDate()` confirms only the last accessor, and the null display is unspecified. Give exact field names, types, and a null and empty-state rule for each.

### R8. Strings plan incomplete and partly redundant (R-7, K-7, Y-6, Y-8)
Four new keys, none present yet. Missing keys: section titles, NKDA text, allergy banner label, Rh warning, GA label, `—` fallback, edit and delete content descriptions, "Prior Pregnancies (N)" (hardcoded today). No values or placeholder formats. Overlaps: `delivery_mode_label` vs `historical_delivery_mode`; `past_deliveries_title` vs `previous_pregnancies`; `primigravida_no_prior_deliveries` vs `no_previous_pregnancies`; NKDA text vs `no_allergy_history`. Line 248 promises "section headings" but lists only one heading key. State which strings are resources and which stay hardcoded. "Strings & Localization" overstates a one-file edit (Y-8).

### R9. Primigravida guard and allergy banner are wrong (K-7, K-11)
"Primigravida" for every empty list is wrong for G3P0 with only miscarriages; reuse `previousDeliverySummary` or derive from `gravida`. The allergy banner rule shows the red banner for seeded "NKDA" values. Neither has a plan step or test.

### R10. Composable signatures and event wiring unspecified (R-11, K-4)
No signatures for `PastDeliveryMicroRow`, `PregnancyDatingSection`, `ParityAndDeliveriesSection`, `MedicalHistoriesSection`, `DeliveryPlanSection`. `StartDatingEditor`, `StartPregnancyEditor`, `StartPatientEditor` (used twice), `RequestDelete` not mapped to sections. `canEdit` gating undefined: `DeliveryPlanningCard` shows edit ungated today, the others gate. Icons and tint values per domain unspecified.

### R11. Row dimensions and touch targets conflict (R-10, K-5, K-15)
40dp content height with 48dp targets: `minimumTouchTarget` only sets a minimum, so rows end up at least 48dp. Not stated whether the row or each button gets it. Edit was a `TextButton` (`correct_previous_pregnancy`); the plan does not say whether it becomes an icon or its accessibility label. Two lines plus chip plus two actions at 360dp has no layout. No font family or `tnum` for the "monospaced Year badge", no `maxLines` or `overflow`, no RTL or font-scale rule. `ClinicalGroupSection` action slot has no 48dp rule and the title Row is not weight-constrained, so long titles can clip the action.

### R12. Test plan is thin (R-9, K-11)
Only parsing tests, and without expected values. No tests for the nulliparous guard, allergy versus NKDA, Rh alert, date fallback, 48dp targets, 360dp no-truncation, tag compatibility. "Maya" and "Elena" fixture references unverified. Decide on Compose UI tests and add `connectedAndroidTest`.

### R13. Attachments footer has no plan entry (K-8, Y-5)
See R2. Tag list also omits attachments.

### R14. Step order and subjective wording (Y-9, Y-10)
Steps 2 and 3 consume string keys added in step 4. "Clean visual rhythm" is not verifiable; keep "zero horizontal truncation" with viewport width and screenshot criteria. Rule 2 and Rule 3 are cited by name only.

### R15. Accessibility regression not called out (K-16)
`ClinicalGroupSection` has no `semantics { heading() }`. Existing cards (`DeliveryPlanningCard`) mark titles as headings.

## 🟢 Nits
- R16. Test path spelled `test/java/...` (line 222) vs `app/src/test/java/...` (line 243) (R-14, K-12, Y).
- R17. Action column has no DELETE value (K-13).
- R18. "Single Unified", "modular", "canonical" are filler words (Y-7). LaTeX `$\ge 48\,\text{dp}$` vs plain "48dp" (Y-12).
- R19. Step text and §6.2 table row duplicate each other for the section container (Y-13).
- R20. Section count: "four" vs five (R-12).
- R21. Imports and test framework not named (R-14).
- R22. Physical-device step has no serial or command (R-15).
- R23. `ClinicalModels.kt` placement note (R-13, K-6): state why, and hoist the regexes to a top-level `private val` if kept.

## Cross-stage view
Findings repeated by independent reviewers, across all stages:
1. The regex parser (Stages 5, 6, 7, 8, 9).
2. Missing field mapping and dropped prior-pregnancy data (Stages 5, 8, 9).
3. Section count and attachments footer (Stages 6, 7, 9).
4. Test tags and old-card disposition (Stages 5, 6, 7, 9).
5. NKDA and Primigravida clinical strings (Stages 7, 8, 9).
6. Missing Risks section and phases (Stages 7, 9).
Corrections to earlier reports: `minimumTouchTarget` exists (Stage 8 verified). `patient.bloodGroup` should be `bloodGroupText`; this was not caught earlier.

## Settled findings
None. All findings remain open.
