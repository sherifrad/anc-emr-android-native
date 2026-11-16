I have completed the test authoring for redesign slice 17 (Catalog Settings).

### Summary of Work
- **Created**: [CatalogSettingsRedesignTest.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/CatalogSettingsRedesignTest.kt) (1,158 lines, 29 independent test cases).
- **Existing Tests**: Zero changes needed (no existing test asserted the old hardcoded strings or Title Case text).
- **Report**: Written to [tests_s17_report.md](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/out/tests_s17_report.md).

### Coverage Highlights
1. **Typography & Clean Hierarchy**:
   - Walks semantics trees of `LabCatalogSettingsSection` (individual tests & panels), `MedicationCatalogSettingsSection`, and all three Add/Edit dialogs (`AddLabTestDialog`, `AddLabCategoryGroupDialog`, `AddMedicationDialog`).
   - Asserts zero text layout results below `12.sp` (verifying frequency chips are `>= 12.sp`).
   - Asserts complete absence of `"·"` middle dots and legacy Title Case strings (`"Create New Diagnostic Panel"` normalized to `"Create diagnostic panel"`).
2. **Touch Targets & 360dp Ergonomics**:
   - Asserts `>= 47.99.dp` width & height across all 25 preserved tags and every new interactive target from the build report (`addNew*`, `edit*`, `delete*`, `reset*`, `clear*`, `selectTestChip-*`, `labCodexCategoryChoice-*`, `medCodexRouteChoice-*`, `medCodexFreqChoice-*`, confirm/cancel buttons).
   - Validates horizontal containment within the 360dp viewport host and inside `SecuritySettingsDialog`'s ~298dp inner column.
3. **Behavioral Parity & Delegate Operations**:
   - Search filtering and empty/recovery actions on both lab tests and medications.
   - Inline validation: blank name on all three dialogs and zero selected tests on panel creation.
   - Entity creation, edit updates, and deletion confirmations.
   - Dual reset on lab tests resetting both `resetLabDefaults()` and `resetLabCategoryGroupDefaults()`.
4. **Responsive Layout (1.0f vs 2.0f Font Scale)**:
   - Verifies `addMedNameField` + `addMedDosageField` and `addLabUnitField` + `addLabRefRangeField` sit side-by-side (`<= 1.3f`) and stack vertically (`> 1.3f`).
5. **Dark Theme & RTL**:
   - Validates error-free, unclipped rendering under `AncTheme(darkTheme = true)` and `LayoutDirection.Rtl`.

### Verification
- `./gradlew --offline :app:compileDebugAndroidTestKotlin -q`: Exit code `0`.
- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest -q`: Exit code `0`.
- No production files modified, no git operations performed, and no emulator/adb invoked.
