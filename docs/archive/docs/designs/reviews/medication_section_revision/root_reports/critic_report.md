# Stage 7 report: Critic Review (round 1)

Target: `docs/designs/patient_workspace_overview_tab_optimization.md`, FeatureRange lines 1..216
Reviewers (all completed, none retried): `egm-design` (feature-spec-standard), `egm-structural` (feature-spec-standard), `egm-style`
Finding ids: `<reviewer>-1-<n>` (design = D, structural = S, style = Y). Line numbers are the reviewers' own.

Round verdict: not clean. Unsettled Blockers and Majors from all three reviewers. No fixes applied yet.

Limits stated by reviewers: the structural reviewer did not open `ClinicalModels.kt`, `OverviewTab.kt` or `strings.xml` in full. The style reviewer did not open `strings.xml` or grep for `Modifier.minimumTouchTarget()`. The design reviewer could not verify `patient.*` fields or `isRhNegative` from the supplied files. Stage 5 (zoom-out) earlier found `isRhNegative` in `ui/ClinicalFormat.kt`, and `DemoPreviousPregnancy` in `DemoModels.kt`.

## 🔴 Blockers

### C1. No Risks & Mitigations section (D-1-1, S-1-1)
The feature-spec standard requires "Risks & Limits". The doc ends at §5 with none. It names no failure modes, privacy implications, scale limits or mitigations. Items it should cover: misparse of clinical values, allergy display regressions, large N of prior pregnancies, font scale and accessibility, rollout and revert of the removed cards, test coverage. Related: no Open Questions section, reviewers or related-doc links in the header (D-1-15, S-1-12).

### C2. Regex extraction of GA and birth weight is unsafe and contradicts its own examples (D-1-2, D-1-3, S-1-5, Y-1-4)
Lines 44, 153-154, 157-175, 212.
- The GA pattern `\d{1,2}(?:\+\d)?\s*(?:w|wks|weeks?)` matches any "N weeks" phrase ("2 weeks postpartum", "12 w", "3 weeks earlier"). `find()` takes the first match over `otherDetails` and `outcomeText` joined together.
- It captures `39w` only, not `39w 2d`. The optional `+d` group sits before the unit, so `39+2` and `39w+2d` fail. The §4.3 example `GA 39w 2d` cannot be produced.
- The weight pattern `[1-5]\d{3}\s*g` can match drug doses and blood-loss notes. The kg branch can match maternal weight. The example `3,450g` has a thousands comma, which the pattern does not match, so it would show `—`.
- No range check (0w, 60w, 99w pass). No rule for multiple matches. No provenance label. The test lists disagree: 6.1 uses `39w`, 6.2 uses `39w 2d` (comprehension C-2 earlier).
- Fixtures show the pattern matching "9 weeks" or "34 weeks" in miscarriage and IUFD notes (`DemoFixtures.kt:809, 829, 849`). That is GA at loss, not "GA at delivery".
- Claims "safe", "non-destructive", "zero-cost", "same visual outcome" (lines 44, 157, 212) are unsupported.
- Reviewer-proposed fixes: range validation (for example GA 4-45w, weight 300-6000g); labelled-context matching; show nothing on multiple matches; mark values as "from notes" or show source text; separate "not found" from "not recorded"; state a false-positive tolerance and a test corpus with adversarial negatives. Inventive and design reviewers also suggest a structured entry path in the pregnancy editor that writes a canonical token into the existing free-text field, read first with the regex last. Raise the schema question separately with the regulatory owner.

### C3. Section count and attachments footer are inconsistent (S-1-3, Y-1-1, D-1-6)
Line 29 says "four primary sections plus a footer" and lists five. Line 80 says "All four sections" use `ClinicalGroupSection`. §4.1, the mermaid diagram and §4.5 show five. Line 34 says "thumbnail gallery", line 201 says "horizontal preview strip". It is not stated whether the footer is wrapped in `ClinicalGroupSection`. Line 201 says it reuses `AttachmentsSectionCard` logic, but line 19 and Rule 3 say old cards go. State one count and one treatment everywhere.

### C4. Clinical display strings assert more than the data supports (S-1-4, Y-1-5, D-1-5, D-1-14 area)
- Line 190: NKDA is shown when `allergyHistory` is null or blank. Blank means "not recorded". Fixtures store `"No known drug allergies (NKDA)"` (`DemoFixtures.kt:21, 428`), so the rule `!isNullOrBlank` would show the red drug-allergy banner for patients with no allergies. Need a defined negative sentinel or structured allergy, and a "not recorded" state (`not_recorded` string exists).
- Lines 185-186: "Primigravida" shown for any empty `previousPregnancies`. A patient with gravida above 1 and no recorded history is not a primigravida. Derive from `gravida`.
- Line 198: `isRhNegative` behaviour for unknown or malformed blood group is not stated.

## 🟡 Majors

### C5. Row height and width claims cannot all hold (D-1-4, S-1-6, Y-1-3, Y-1-7)
Line 43 says 40dp row, line 145 says 48dp. Tier 1 holds year badge, outcome chip, mode label such as "Cesarean Section (CS)" and two 48dp actions. That leaves about 230dp of 328dp. This is the failure the doc rejects in §5 row 2. Tier 2 has no wrap or ellipsis rule. A fixed 40dp height fails at font scale 1.3 or more, with long place names or RTL. "Zero horizontal text clipping" has no check named. Heights are inconsistent: ~120dp per child (line 22) vs ~120dp total (line 31); N=3 rows at 48dp is 144dp or more. "~540dp" implies 4.5 children while the example has 3 (D-1-6). Field order differs between line 31 and §4.3, and "five data points" (line 145) vs six listed (Y-1-2). All `~NNdp` figures lack a measurement source (S-1-15, Y-1-3).

### C6. Symbols used without definition or that may not exist (S-1-7, D-1-4, Y-1-12, S-1-11)
- `Modifier.minimumTouchTarget()` was not found in the codebase. Material 3 has `minimumInteractiveComponentSize()`.
- `parsed.hospital`, `parsed.doctor`, `parsed.deliveryPlan`: no definition or source. Stage 5 found `DeliveryNoteSections` has `hospital`, `fees`, `plan`, `postpartum` only.
- `isRhNegative`, `FactRow`, `calculateEstimatedDueDate()`, `ObstetricParityBadge`: used without declaration. Components in the diagram (`PastDeliveriesMicroTable`, `ObstetricParityBadge`, `AllergyCalloutBanner`, `AttachmentsFooterSection`) have no spec or file. No file is named for the parser.

### C7. Architecture lacks data flow and list rendering is undefined (S-1-8, D-1-8, D-1-7)
- No state ownership, edit and delete callbacks, or navigation to editors. The doc does not say which sections get which edit action, nor `canEdit` and archived-patient behaviour. `OverviewTab` passes `onEditPatient` to `DeliveryPlanningCard` and `AttachmentsSectionCard` without a `canEdit` guard today. The doc does not say whether this is fixed.
- `items(patient.previousPregnancies)` at line 187 is `LazyColumn` syntax. The screen is a `verticalScroll` Column, and `ClinicalGroupSection` takes `ColumnScope` content. A lazy list nested in a scrolling Column crashes on infinite max height. State `forEach` or a whole-tab `LazyColumn`, and the expected maximum N.

### C8. Parsing placement, performance and tests unspecified (D-1-3)
`Regex(...)` is built per call, so it runs per row per recomposition unless hoisted or remembered. The doc does not say where parsing runs (composable, ViewModel, mapper). No test cases in the body. No Arabic digits or Arabic text consideration for an Egypt-based clinic.

### C9. Alternatives are weak (D-1-9, S-1-9)
Every row says "Rejected by author" with no evidence, and line 212 cites a "constitutional rule" and "hash invalidation" with no link. Missing options: flatten existing cards with GA and parity dedupe only; structured entry at input time written as a canonical token in the existing free text; Material 3 `ListItem` rows; schema change cost for old records. The "~96dp" figure has no derivation. The §5 row 2 cell contains unescaped `|` characters at line 210 that split the table row (S-1-2, Y-1-10).

### C10. Header and body duplication applied selectively (D-1-5, Y-1-11)
GA is removed from the body but the allergy banner and Rh alert duplicate header data. §4.5 dating omits GA and does not say whether dating notes or GA at scan remain. The `datingMethod` list on line 181 ("Ultrasound, LMP, IVF") differs from `ClinicalModels.kt:211` (`LMP`, `Ultrasound`, `IVF_TRANSFER`, `Clinical`). The EDD precedence `agreedEdd ?: ultrasoundEdd ?: calculateEstimatedDueDate()` is stated as authoritative with no justification or conflict display (`redating_hint` exists).

### C11. Goals lack measurable success criteria (D-1-7)
No scroll height at N, taps to reach key data, or contrast ratio targets. "Retain legacy test tags" conflicts with "zero legacy baggage" (see Stage 6 comprehension C-8).

### C12. Subjective language, undefined jargon and hyperbole (Y-1-6, Y-1-8, Y-1-9, S-1-10)
- Subjective words without criteria: "heavy", "severe", "bloat", "calm", "unmissable", "clean", "useless", "clunky", "un-scannable wall of text", "cohesive", "card fatigue" (lines 12, 20, 22, 23, 29, 32, 34, 57, 149, 201, 209, 210, 213).
- Undefined terms: "SAMD" (never expanded), "Rule 3: Zero Legacy Baggage", "Rule of Reusability", "constitutional rule", "hash invalidation", "Authoritative Milestones", "dual-tier, monospaced micro-tabular", "domain-tinted icon capsules", "hairline dividers".
- Line 211 "violates SAMD clinical safety principles" is unsupported. The concrete decision is that prior CS and scar history must be visible without interaction.

## 🟢 Nits

- C13. Frozen-schema constraint is stated four times (lines 44, 48, 157, 212). "Rejected by author" repeats in every row (lines 209-213). Filler adverbs ("strictly", "standardized", "zero-cost ... safely"). Title-case headings with ampersands (Y-1-13..16, S-1-9).
- C14. Hard-coded English strings in the spec ("Primigravida · No prior deliveries recorded", NKDA text) where `strings.xml` is the convention. `ga_prefix`, `no_allergy_history` already exist. The em-dash `—` fallback conflicts with a recent commit that removed em-dashes. Confirm (D-1-13, S-1-14).
- C15. Mermaid diagram: `C4` ("48dp Touch Targets") is a requirement, not a component. Component paths are split between `ui/patienthub/components/` and `tabs/`. No file map (S-1-11).
- C16. LaTeX `$\ge 48\,\text{dp}$` at line 45 is inconsistent with the rest of the text and may not render (S-1-13).
- C17. Proposed files (`ClinicalGroupSection.kt`, `PastDeliveryMicroRow.kt`, `OverviewSections.kt`, `DeliveryBiometricsParsingTest.kt`) are not marked "(proposed)" at first mention (Y-1-17).
- C18. Divider at 0.35 alpha on `outlineVariant` and 0.75dp is called "necessary visual scaffolding" with no contrast expectation; either state it or call it decorative (D-1-14).
- C19. Line 216 ends the range at a bare `---` with no decision, risks or test plan inside it (Y-1-18). Dp heights at lines 12-18 and 21 are approximations with no method (S-1-15).

## Cross-reviewer priority
1. C2 and C4: patient-safety risks (wrong clinical values and false negatives shown as fact). All three reviewers raised the regex; four reviewers/stages raised NKDA or Primigravida.
2. C1: missing Risks & Mitigations section.
3. C3, C5, C6: internal inconsistencies and undefined symbols that block implementation.
4. C7 and C8: data flow, lazy-list crash risk, parser placement.
5. C9 to C12: alternatives, duplication principle, measurable goals, wording.

## Settled findings
None. All findings remain open pending the gate decision.
