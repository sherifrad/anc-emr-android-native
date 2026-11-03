# Frontier Review Brief: Design Round d28 - Visit and Vitals Entry UI Redesign

## Task Context
- Repository: `/Users/sherifgamalradwan/anc-emr-android-native`
- Branch: `maca-round2`
- Redesign specifications: `.agents/pipeline/design/REDESIGN_SPECS.md` Section 4.4 and `.agents/pipeline/SPEC_DESIGN_D28_VISIT_ENTRY.md`
- Approved mock: `.agents/pipeline/design/d4/d3/index.html` Section 3

## Modified Files to Review
- `app/src/main/java/com/sherifradwan/ancnative/ui/DesignSystem.kt` (`SegmentedProgressBar`, `ValueStepper`)
- `app/src/main/java/com/sherifradwan/ancnative/ui/VisitEditor.kt` (4-step guided wizard implementation)
- `app/src/main/res/values/strings.xml` (Visit entry localized strings)
- `app/src/androidTest/java/com/sherifradwan/ancnative/VisitEntryDesignTest.kt` (New UI design verification test)

## Review Objectives
1. Check S70 compliance: does `VisitEditor.kt` use `R.string.visit_date_in_future`, `R.string.blood_pressure_invalid`, `R.string.weight_invalid` and call `bpLevel(` or `bpAlertResource(`?
2. Clinical safety:
   - Does "Fill normal values" ONLY fill blank oedema, movements, urine, and NEVER BP or weight?
   - Does BP >= 140/90 show the warning and flag, but NEVER block saving?
   - Does "+ Add other" properly validate inputs through `ClinicalCodexRepository.addCustomChoice`?
3. Design System & Accessibility:
   - Minimum 48 dp touch target on all interactive elements?
   - 360 dp width compliance without horizontal overflow or clipping?
   - Closed palette adherence (navy #0D1B3D, rose #934F5B, rose container #FFD9DE, teal #176B70, surface #FFFFFF)?
   - Pinned bottom bar using `EditorPage` slot?
4. Independent Test Review:
   - Do all 5 tests in `VisitEntryDesignTest` verify real clinical behaviors?
   - Does `AncWorkflowTest` continue to pass without regressions?

Run all unit tests, examine the code diff, and report any blockers, defects, or should-fixes.
