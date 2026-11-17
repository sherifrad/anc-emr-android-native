# Slice 21 Cleanup: Batch 3a Implementation Report

Branch: `redesign-2026-11-15`
Scope: Batch 3a dead composables, replaced patient card and badges, and their dead tests (`app/src/main/java/com/sherifradwan/ancnative/ui/`, `app/src/androidTest/`, and `docs/cleanup/s21/`).

## Items Done (Verified Removed / Updated)

### Step 1: Dead Composables With Zero Callers
1. `app/src/main/java/com/sherifradwan/ancnative/ui/TimelineSpine.kt`:
   - Deleted entire file (`TimelineSpineItem`).
   - Verified via `rg -n "\b(TimelineSpineItem|TimelineSpine)\b" app/src` that only the declaration at `TimelineSpine.kt:17` existed prior to deletion, and 0 hits exist post-edit.
2. `app/src/main/java/com/sherifradwan/ancnative/ui/DesignSystem.kt`:
   - Deleted unreferenced composables `SectionCard`, `NoticeCard` (and its KDoc comment), `SegmentedProgressBar`, and `SectionHeader`.
   - Removed the 4 imports used solely by those deleted composables: `ColumnScope`, `widthIn`, `TextButton`, `Color`.
   - Kept `SummaryRow`, `BigChoiceButton`, `ValueStepper`, `StepperButton`, `BatchInfoBar`, `BatchSegmentedProgress`, and `QualitativeChoiceButton` untouched.
   - Verified 0 hits in `app/src` for `SectionCard`, `NoticeCard`, `SegmentedProgressBar`, and `SectionHeader` post-edit.
3. `app/src/androidTest/java/com/sherifradwan/ancnative/VisitEntryDesignTest.kt:111`:
   - Updated stale comment `// Jump back to step 1 via SegmentedProgressBar` to `// Jump back to step 1 via VisitStepHeader` (live `VisitStepHeader` renders `stepSegment-$i` at `ui/VisitEditorSteps.kt:221`).

### Step 2: Old Patient Card & Its Dead Tests
1. `app/src/main/java/com/sherifradwan/ancnative/ui/Components.kt`:
   - Verified via `rg -n "\b(ClinicalPatientCard|PatientCardVariant)\b" app/src` that the only references were inside `Components.kt` itself and the two dead test surfaces (`ClinicalPatientCardTest.kt` and `DatingGaMarkerSurfacesTest.kt:115-120`).
   - Deleted `enum class PatientCardVariant` and `fun ClinicalPatientCard` (plus their KDoc comments).
   - Removed the 18 imports used solely by `ClinicalPatientCard` / `PatientCardVariant`: `LocalDate`, `ExperimentalLayoutApi`, `FlowRow`, `size`, `RoundedCornerShape`, `Icons`, `ArrowBack`, `Print`, `Warning`, `HorizontalDivider`, `Icon`, `IconButton`, `Surface`, `heading`, `TextOverflow`, `DemoPatient`, `datingSummaryOf`, `formatDmy`.
   - Kept `FactRow` (both overloads), `minimumTouchTarget`, `editorFieldColors`, `EditorField`, and `GenericEditorErrorCard` untouched.
2. `app/src/androidTest/java/com/sherifradwan/ancnative/ClinicalPatientCardTest.kt`:
   - Deleted entire file (74 lines testing `ClinicalPatientCard` variants exclusively).
3. `app/src/androidTest/java/com/sherifradwan/ancnative/DatingGaMarkerSurfacesTest.kt`:
   - Deleted `headerCardShowsMarkerOnlyWhenDiscordant` (exercise loop over `PatientCardVariant.BEDSIDE_TOP_BAR` and `PatientCardVariant.EDITOR_SUMMARY`) and the now-unused imports `ClinicalPatientCard` and `PatientCardVariant`.
   - Kept all other tests (`visitGaBadgesNeverShowTheMarkerEvenForADiscordantPatient`, `scanGaBadgesNeverShowTheMarkerEvenForADiscordantPatient`, `listRow*`, `expandedPatientHubHeaderShowsMarkerOnlyWhenDiscordant`, `discordantPatientWithGaOutsideTheWindow*`) untouched.
4. `app/src/androidTest/java/com/sherifradwan/ancnative/OverviewTabSectionsTest.kt`:
   - Because `GestationalAgeBadge` had zero callers in `app/src/main` outside deleted `ClinicalPatientCard` and was removed in Step 3, deleted the 3 direct `GestationalAgeBadge` test methods (`gaBadgeShowsMarkerWithContentDescriptionWhenDiscordant`, `gaBadgeKeepsTheNumberAndHasNoMarkerWhenNotDiscordant`, `gaBadgeDefaultIsNotDiscordant`) and `import com.sherifradwan.ancnative.ui.GestationalAgeBadge`.
   - Kept `markerTag`, `showContent`, `markerCount`, and all remaining tests untouched.

### Step 3: Replaced Old Badges in `ui/ClinicalBadges.kt` (Level 3 Pure Deletion)
1. Re-grepped all 6 badge symbols in `app/src` after Step 2 (`PatientAgeBadge`, `ClinicalTagBadge`, `ClinicalRiskBadge`, `BloodGroupBadge`, `GestationalAgeBadge`, `ObstetricParityBadge`):
   - `PatientAgeBadge`: 0 callers in `app/src/main` outside declaration; 0 code references in tests (the sole grep hit outside `ClinicalBadges.kt` is a KDoc prose mention in `app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt:75`, not a code/test reference). Deleted.
   - `ClinicalTagBadge`: 0 callers outside dead `ClinicalRiskBadge` in `ClinicalBadges.kt`. Deleted.
   - `ClinicalRiskBadge`: 0 callers in `app/src` outside declaration. Deleted.
   - `BloodGroupBadge`: 0 callers in `app/src` outside declaration. Deleted.
   - `GestationalAgeBadge`: 0 callers in `app/src/main` outside declaration; its only test references (`OverviewTabSectionsTest.kt`) were removed in Step 2. Deleted.
   - `ObstetricParityBadge` (both 3-arg and 1-arg overloads): 3-arg overload called only 1-arg overload; 0 external callers in `app/src`. Both overloads deleted.
2. Removed the 12 imports used solely by the deleted badges (`BorderStroke`, `background`, `Arrangement`, `size`, `Row`, `height`, `IntrinsicSize`, `Icons`, `Warning`, `Icon`, `Alignment`, `clearAndSetSemantics`). Note: `MedicationStatusBadge` uses fully qualified `androidx.compose.foundation.BorderStroke`, which was left untouched.
3. No other lines or logic in `ui/ClinicalBadges.kt` were modified.

## Badges Kept in `ui/ClinicalBadges.kt` (With Reasons)

1. `BpAlertLabel` (`ui/ClinicalBadges.kt:27`):
   - Referenced in `app/src/main/java/com/sherifradwan/ancnative/ui/VisitEditor.kt:243` and `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt:60,284`.
2. `BpAlertChip` (`ui/ClinicalBadges.kt:35`):
   - Referenced in `ui/ClinicalBadges.kt:30` (via `BpAlertLabel`) and `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientDirectoryCard.kt:34,156`.
3. `MedicationStatusBadge` (`ui/ClinicalBadges.kt:68`):
   - Referenced in `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationCard.kt:47,147`.
4. `GaDiscordanceMarker` (`ui/ClinicalBadges.kt:92`):
   - Referenced in `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt:64,338,459` and `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientDirectoryMetricsRow.kt:23,117`.

## Items Skipped / Deferred (With Evidence)

1. `app/src/main/java/com/sherifradwan/ancnative/ui/Theme.kt` tokens (`Tag*`, `ClinicalCardTokens.*`) and `app/src/test/java/com/sherifradwan/ancnative/ui/ThemeContrastTest.kt` (including the stale KDoc mention `PatientAgeBadge` at `:75`):
   - Skipped per batch instruction ("Do NOT touch Theme.kt tokens in this batch (next batch)"). `ThemeContrastTest` continues to compile and pass against current `Theme.kt`.
2. Pre-existing unused imports in `VisitEntryDesignTest.kt` (`getValue`, `setValue`, `assertIsNotEnabled`, `assertTextContains`, `performTextInput`, `assertTrue`) and `OverviewTabSectionsTest.kt:33` (`LocalDate`):
   - Skipped because they were already unused prior to Batch 3a edits (tracked under Batch 5 `C6-06`).
3. Protected files (`schemas`, `RecordsDatabase`, `RecordEntities`, `RecordsDao`, `RecordsRepository`, `UndoCoordinator`, `ClinicalModels`, `*Generator*`, `state/`, `ui/DatingFieldVisibility.kt`, `storage/`, `model/`, `security/`):
   - Zero protected files modified.

## Commands and Results

1. Symbol reference scans (`rg -n`) across `app/src`:
   - Verified zero remaining references for `TimelineSpineItem`, `SectionCard`, `NoticeCard`, `SegmentedProgressBar`, `SectionHeader`, `ClinicalPatientCard`, `PatientCardVariant`, `ClinicalTagBadge`, `ClinicalRiskBadge`, `BloodGroupBadge`, `GestationalAgeBadge`, `ObstetricParityBadge`, and only `ThemeContrastTest.kt:75` KDoc mention for `PatientAgeBadge`.
   - Verified active callers for `BpAlertLabel`, `BpAlertChip`, `MedicationStatusBadge`, and `GaDiscordanceMarker`.
   - Verified zero newly introduced unused imports across all 6 edited files (`DesignSystem.kt`, `Components.kt`, `ClinicalBadges.kt`, `VisitEntryDesignTest.kt`, `DatingGaMarkerSurfacesTest.kt`, `OverviewTabSectionsTest.kt`).
2. Step 1 compile verification:
   - `./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin -q` -> exit 0.
3. Step 2 compile verification:
   - `./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin -q` -> exit 0.
4. Step 3 compile verification:
   - `./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin -q` -> exit 0.
5. Unit test verification:
   - `./gradlew --offline :app:testDebugUnitTest --console=plain` -> exit 0 (`BUILD SUCCESSFUL in 25s`, 28 actionable tasks: 5 executed, 23 up-to-date). Verified `ThemeContrastTest` (`app/build/test-results/testDebugUnitTest/TEST-com.sherifradwan.ancnative.ui.ThemeContrastTest.xml`): 6 tests, 0 skipped, 0 failures, 0 errors.
   - Note: `connectedDebugAndroidTest` was not executed (no adb/emulator rule); `:app:compileDebugAndroidTestKotlin` compiled cleanly after every step.
