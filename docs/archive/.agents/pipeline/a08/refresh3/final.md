**1. `reviewedVisitEditorLayoutsKeepStatusDraftAndControlsAccessible`** — **PRODUCT-BUG-SUSPECT**
- **Evidence**: [`AncApp.kt:134`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt#L134) renders the top bar exclusively on `PatientList`; [`VisitEditor.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/VisitEditor.kt) has no `lockStorage` control for clinicians.
- **Change**: None (left unchanged; clinicians require a terminal storage lock control during open visit edits).

**2. `recordsWindowKeepsSecureFlagAndGateExplainsDraftPolicy`** — **PRODUCT-BUG-SUSPECT**
- **Evidence**: [`VaultGateScreen.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/VaultGateScreen.kt) only renders vault title/body; `R.string.unsaved_drafts_policy` (`strings.xml:62`) is unreferenced anywhere in UI.
- **Change**: None (left unchanged; gate must communicate the clinical draft discard policy).

**3. `launchIsFailClosedAndNeverRendersSyntheticPatientsBeforeRealUnlock`** — **STALE-TEST**
- **Evidence**: [`AncApp.kt:104-124`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt#L104-L124) renders logo + spinner (`vaultGate`) without clinic text during opening; synthetic banner was removed in D8.
- **Change**: Removed stale `assertDemoBannerDisplayed()` on line 92 in [`AncWorkflowTest.kt`](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/androidTest/java/com/sherifradwan/ancnative/AncWorkflowTest.kt#L85-L97); preserved fail-closed gate & patient non-leak assertions.
