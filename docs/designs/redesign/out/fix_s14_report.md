# Fix S14 Report

## Changes

1. **Patient identity banner in all save phases (`VisitEditor.kt`)**
   - Removed the `state.savePhase == SavePhase.IDLE` guard around `VisitPatientBanner(patient = patient, gaResult = calculatedGa)` in `VisitEditor` so patient identity and risk context stay visible in `IDLE`, `PENDING`, `RECONCILING`, `UNRESOLVED`, and `ERROR` save phases.
   - Removed the unused `subtitle` construction and passed `subtitle = ""` to `EditorPage`, eliminating the duplicated patient name in non-idle phases.

2. **Step segment accessibility semantics (`VisitEditorSteps.kt`, `strings.xml`)**
   - Added `<string name="visit_step_segment_cd">%1$s, step %2$d of %3$d</string>` to `app/src/main/res/values/strings.xml`.
   - In `VisitStepHeader`, constructed `stepDescription` from `R.string.visit_step_segment_cd` with `stringResource(stepTitles[i])`, `i + 1`, and `totalSteps`, and set `contentDescription = stepDescription` alongside `selected = isCurrent` on each `stepSegment-$i` node.
   - Removed the redundant `onClickLabel` from `.clickable(enabled = editingEnabled, role = Role.Tab)` while preserving `Role.Tab`, `selected`, click handling (`onStepClick(i)`), and `testTag("stepSegment-$i")`.
   - Added `Modifier.clearAndSetSemantics {}` to the visible short label `Text` shown at `fontScale <= 1.3f` so the short label is not read alongside `contentDescription`.

## Verification

- Ran `./gradlew --offline :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin -q` — exited with code 0.
- Note: Existing instrumented tests in `VisitEditorRedesignTest.kt` and `AncWorkflowTest.kt` were not edited or executed (per task constraints) and still contain assertions against the previous non-idle subtitle and un-cleared short step label text.
