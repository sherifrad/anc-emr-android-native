# Overview tab optimization, Phases B and C spec

Source of truth: `docs/designs/patient_workspace_overview_tab_optimization.md` (§4.2 to §4.6, §7.1 Phases B and C). Approved mock: `.agents/mocks/overview_tab_sections.html`.
Rules: AGENTS.md (schema v6 frozen; one canonical implementation; no legacy shims; 1,500-line ceiling; 360dp first). Phase A is committed (`classifyAllergy`, `canEdit` on `DeliveryPlanningCard`). No patient data in briefs.

## Deliverables
1. `ui/patienthub/components/ClinicalGroupSection.kt` per §4.2 (header row with 24dp icon capsule, title weight(1f) + heading semantics, optional action slot with `minimumTouchTarget()`, 0.75dp divider, `ColumnScope` content, test tag via `modifier`). No elevated card; the three sections are separated by hairline dividers inside one surface.
2. Pure row model (JVM-testable) `ui/patienthub/model/PastDeliveryRowModel.kt`:
   ```kotlin
   enum class OutcomeTone { Neutral, Caution, Severe }
   enum class RowExtraKind { PregnancyType, LossManagement, TerminationMethod }
   data class PastDeliveryRow(
       val year: String?,            // see mapping; null means "date not recorded"
       val dateTextFallback: String?,// dateText when no 4-digit year found but text non-blank
       val outcomeLabel: String?,    // enum label text, else outcomeText, else null
       val outcomeTone: OutcomeTone,
       val mode: String?,            // birthMethod else deliveryBy, blank -> null
       val placeAndDoctor: String?, val notes: String?,
       val extras: List<Pair<RowExtraKind, String>>)
   fun pastDeliveryRow(p: DemoPreviousPregnancy): PastDeliveryRow
   ```
   Mapping per §4.3: year = first 4-digit year (19xx or 20xx) in `dateText`; if none and `dateText` non-blank -> `dateTextFallback = dateText`, year null; blank -> both null. Tone: Stillbirth -> Severe; EarlyPregnancyLoss -> Caution; all else (LiveBirth, TerminationOfPregnancy, free text) -> Neutral. Blank strings become null. Check real field names/types in `model/DemoModels.kt` and the existing `PreviousPregnancyCard.kt` labels; the outcome label for enum values must come from the existing strings/labels used today.
3. `ui/patienthub/components/PastDeliveryMicroRow.kt`: two-line row per §4.3 (min height 48dp, whole row is the edit target, trailing existing `DeleteIconButton`, both hidden when `!canEdit`; line 1 wraps; line 2 `maxLines = 2` ellipsis). Tags: outer container `pregnancy-<id>`; inner clickable node `editPregnancy-<id>` (a different node); delete keeps `delete-previous_pregnancy-<id>`. Chip colours: Severe `errorContainer`, Caution `tertiaryContainer`, Neutral `surfaceContainerHigh`; label always shown.
4. `ui/patienthub/components/OverviewSections.kt`: Dating section (tag `datingCard`, edit action tag `editDating`), Parity and previous pregnancies section (tag `obstetricParityCard`, add action tag `addPregnancyButton`, `ObstetricParityBadge` kept with its `obstetricParityBadge` tag, `forEach` rows, empty list shows `previousDeliverySummary(patient)`; the word primigravida only when gravida <= 1; `obstetricNotes` via `FactRow`), Medical histories section (tag `medicalHistoriesCard`; allergy via `classifyAllergy`: Recorded -> errorContainer banner, ExplicitlyNone -> calm `no_allergy_history`, NotRecorded -> `not_recorded`; then medical, surgical, family via `FactRow`). Reuse existing `FactRow`, `ObstetricParityBadge`, `DeleteIconButton`, `minimumTouchTarget`. Section title for previous pregnancies reuses `R.string.previous_pregnancies`. Add only the new strings actually used (exact English values; also add Arabic if `values-ar` exists and others are there). No hard-coded strings, no hex colours.
5. Refactor `tabs/OverviewTab.kt` to: header sections (3) then existing `DeliveryPlanningCard`, then `AttachmentsSectionCard`. At most 180 lines; keep `AncEvent` construction (`StartDatingEditor`, `StartPregnancyEditor`, `StartPatientEditor`, `RequestDelete`) and `canEdit` gating (callbacks null/absent when `!canEdit`). Keep tag `overviewTabContent`.
6. Delete `GestationalDatingCard.kt`, `ObstetricParityCard.kt`, `MedicalHistoriesCard.kt`, `PreviousPregnancyCard.kt` (and any now-unused imports/strings/helpers; grep first that nothing else calls them).

## Outcome oracles
- ORACLE-B1: `pastDeliveryRow` year extraction: "2023" -> "2023"; "Mar 2019" -> "2019"; "last year" -> year null, dateTextFallback "last year"; blank/null -> both null.
- ORACLE-B2: tones: Stillbirth Severe, EarlyPregnancyLoss Caution, LiveBirth/Termination/free text Neutral; label never null when `outcome` or `outcomeText` exists.
- ORACLE-B3: mode prefers birthMethod over deliveryBy; blank -> null; extras only include non-blank pregnancyType/lossManagement/terminationMethod, in that order; blanks never appear as segments.
- ORACLE-C1: all legacy tags in the §7.1 Phase C table still resolve on the Overview tab for a seeded multiparous patient; `pregnancy-<id>` and `editPregnancy-<id>` are different nodes.
- ORACLE-C2: with `canEdit=false` there is no `editDating`, `addPregnancyButton`, `editPregnancy-*`, `delete-previous_pregnancy-*`, `editDeliveryPlan` node.
- ORACLE-C3: row at 360dp width and fontScale 1.3 shows Outcome label and Mode text not truncated (bounds within parent, no ellipsis on line 1) for the longest fixture mode "Cesarean Section (CS)".
- ORACLE-C4: empty previous list on a G1 patient shows primigravida text; empty list on a G3 patient does NOT say primigravida.
- ORACLE-C5: legacy cards deleted; `./gradlew assembleDebug` and `testDebugUnitTest` pass; existing `AncWorkflowTest` (`editDating`, `deliveryPlanningCard`) and `DeleteUndoUiTest` (`pregnancy-` prefix) compile and keep their tags.

## Gates
Tier 0-1: `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`, `./gradlew compileDebugAndroidTestKotlin`. Tier 2 (androidTest): run only if an emulator or device is attached (`adb devices`), else report "not run". Level-3 rule: independent review (DeepSeek + GLM read-only) required.
