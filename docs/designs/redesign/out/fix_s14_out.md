Fixed both review findings:

1. **Always show `VisitPatientBanner` and clear `EditorPage` subtitle** in [VisitEditor.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/VisitEditor.kt#L168-L175):
   - Removed the `subtitle` construction block and passed `subtitle = ""` to `EditorPage`.
   - Removed the `state.savePhase == SavePhase.IDLE` guard around `VisitPatientBanner(patient = patient, gaResult = calculatedGa)` so patient identity and risk context render in every `SavePhase`.

2. **Accessible `stepSegment-0..3` tab semantics across font scales** in [VisitEditorSteps.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/VisitEditorSteps.kt#L198-L266) and [strings.xml](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/res/values/strings.xml#L794):
   - Added `visit_step_segment_cd` (`"%1$s, step %2$d of %3$d"`) in `strings.xml`.
   - In `VisitStepHeader`, removed the duplicate `onClickLabel` from `.clickable(enabled = editingEnabled, role = Role.Tab)`, set `contentDescription = stepDescription` alongside `selected = isCurrent` in `.semantics { ... }`, preserved `testTag("stepSegment-$i")` and `onStepClick(i)`, and added `Modifier.clearAndSetSemantics {}` to the visible short label `Text` so it is not read twice.

3. **Verification & report**:
   - Compiled with `./gradlew --offline :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin -q` (exited with code 0; no runtime tests executed or edited).
   - Wrote [fix_s14_report.md](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/out/fix_s14_report.md).
