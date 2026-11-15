Launched `./gradlew --offline :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin -q` and waiting for the build to finish.
Applied the requested fixes for the 4 slice-12 review findings and wrote the report to [fix_s12_report.md](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/out/fix_s12_report.md):

1. **External label accessibility**:
   - In [Components.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt#L464-L484) (`EditorField`, `externalLabel` branch): removed `.semantics(mergeDescendants = true) {}` from the wrapper `Column`, wrapped the `FormFieldLabel` / error `Text` in `Box(modifier = Modifier.clearAndSetSemantics {})`, and passed `Modifier.semantics { contentDescription = label }` to `field(...)` so `OutlinedTextField` carries `contentDescription = label` alongside `testTag(tag)` only when `externalLabel` is true.
   - In [PatientEditor.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt#L553-L599) (`ClinicalDropdownField`, `externalLabel` branch): removed `.semantics(mergeDescendants = true) {}` from the wrapper `Column`, wrapped `FormFieldLabel(label)` in `Box(modifier = Modifier.clearAndSetSemantics {})`, and added `.then(if (externalLabel) Modifier.semantics { contentDescription = label } else Modifier)` on `OutlinedTextField` before `.testTag(tag)`.
   - Note: [RegistrationRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationRedesignTest.kt#L467) was left untouched per file scope rules; its `compose.onNodeWithText(labelText)` call uses the default `useUnmergedTree = false`, whereas `Box(Modifier.clearAndSetSemantics {})` removes the visual label from the merged tree while keeping it accessible under `useUnmergedTree = true`.

2. **Dropdown typography leak**:
   - In [PatientEditor.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientEditor.kt#L559-L584) (`ClinicalDropdownField`): removed `itemStyle` and updated both the `R.string.not_specified_empty` and `options` `DropdownMenuItem` `text` lambdas to use `Text(..., style = RecordType.FieldValue)` when `externalLabel` is true and plain `Text(...)` with no `style` argument when `externalLabel` is false. Verified [DatingEditor.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/DatingEditor.kt#L101-L113) omits `externalLabel` and remains unchanged (no other callers exist in `app/src`).

3. **Consolidated `"Saving…"` string**:
   - Kept `saving_label` and removed `delivery_plan_saving` and `medication_saving` from [strings.xml](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/res/values/strings.xml#L783).
   - Replaced all usages with `R.string.saving_label` in [DeliveryPlanSheet.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/DeliveryPlanSheet.kt#L175), [MedicationSheet.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt#L183), [MedicationStopBar.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationStopBar.kt#L62), and [MedicationFormTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/MedicationFormTest.kt#L347).

4. **"Archived" tag wording**:
   - Left untouched as instructed.

## Verification
- `grep -rn "delivery_plan_saving\|medication_saving\|saving_label" app/src/`: zero remaining occurrences of `delivery_plan_saving` or `medication_saving`.
- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin -q`: exited with code 0 and no output. (No instrumentation tests were executed on a device/emulator.)
