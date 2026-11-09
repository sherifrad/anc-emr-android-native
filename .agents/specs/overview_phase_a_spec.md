# Overview tab optimization, Phase A spec (shared allergy state + Delivery Plan canEdit gate)

Source of truth: `docs/designs/patient_workspace_overview_tab_optimization.md` §4.5 item 3, §4.6, §7.1 Phase A.
Rules: AGENTS.md (Room schema v6 frozen, no new migrations; Rule 1 one implementation per rule; no legacy shims).
Orchestrator: Claude. Author (builder): Claude subagent. Test designer: separate Claude subagent (spec only). Reviewers: opencode DeepSeek + GLM, read-only.
Privacy: no patient data in any brief; fixtures are synthetic strings only.

## Scope (Phase A only)
1. New pure function in `app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalFormat.kt` (next to `isRhNegative`):
   ```kotlin
   sealed interface AllergyState {
       data object NotRecorded : AllergyState
       data object ExplicitlyNone : AllergyState
       data class Recorded(val text: String) : AllergyState
   }
   internal fun classifyAllergy(raw: String?): AllergyState
   ```
2. Callers use it instead of their inline tests: the patient directory card allergy chip (`ui/Components.kt`, block "7. Allergy Status"), and `ui/patienthub/PatientHubHeader.kt` (allergy chip around line 289).
   `MedicalHistoriesCard.kt` is deleted in Phase C; do not touch it now.
3. `DeliveryPlanningCard` gets `canEdit: Boolean` and shows the `editDeliveryPlan` IconButton only when true. `PlanTab.kt` and `OverviewTab.kt` pass their existing `canEdit`.
No new strings (existing `no_allergy_history` and `not_recorded` suffice). No new composables, no schema, no new dependencies.

## Rules for classifyAllergy (trim, case-insensitive, ignore trailing "." and surrounding whitespace)
- null, empty or blank -> `NotRecorded`
- the WHOLE entry is exactly one of: `NKDA`, `NKA`, `None`, `No`, `Nil`, `No known allergies`, `No known drug allergies`, `No known drug allergies (NKDA)`, `No known allergy`, `No allergies` -> `ExplicitlyNone`
- anything else, including entries that merely start with or contain a negation ("No known allergies except penicillin", "NKDA but sulfa rash") -> `Recorded(trimmedText)` (original casing, trimmed)
Never classify an entry as ExplicitlyNone when it contains other clinical content.

## Display rules
- Directory card chip: `Recorded` -> existing errorContainer chip with the text; `ExplicitlyNone` -> existing neutral chip, text from `R.string.no_allergy_history`; `NotRecorded` -> neutral chip, text from `R.string.not_recorded` (this replaces the old blank-means-NKDA behaviour).
- Header chip: `Recorded` -> existing red chip with "Allergy: <text>" exactly as today; `ExplicitlyNone` -> neutral (surfaceContainerHigh / onSurfaceVariant) chip, text `R.string.no_allergy_history`, same height; `NotRecorded` -> no chip (unchanged).
- Delivery plan: with `canEdit=false` the node tagged `editDeliveryPlan` does not exist; with `true` it exists and calls `onEditPatient`.

## Outcome oracles
- ORACLE-A1: `classifyAllergy(null)`, `""` and `"   "` return `NotRecorded`.
- ORACLE-A2: each of "NKDA", "nkda", " None ", "No known drug allergies", "No known drug allergies (NKDA)", "No known allergies." returns `ExplicitlyNone`.
- ORACLE-A3: "Penicillin" returns `Recorded("Penicillin")`.
- ORACLE-A4 (negative/boundary): "No known allergies except penicillin" and "NKDA but sulfa rash" return `Recorded` with the full trimmed text (an allergy must never be hidden).
- ORACLE-A5: the synthetic fixture string "No known drug allergies (NKDA)" (DemoFixtures.kt:21,428) does not produce the red alert chip in the header or the directory card.
- ORACLE-A6: `DeliveryPlanningCard(canEdit=false)` shows no `editDeliveryPlan` node; `canEdit=true` shows it (Compose UI test).
- ORACLE-A7: `PlanTab` and `OverviewTab` pass their `canEdit` through; read-only/archived patients cannot start the patient editor from the Delivery Plan card.

## Call-chain map
- `classifyAllergy` <- `Components.kt` directory card (<- `PatientListScreen`), `PatientHubHeader` (<- `PatientWorkspaceScreen`).
- `DeliveryPlanningCard` <- `OverviewTab.kt:132`, `PlanTab.kt:38`; `onEditPatient` -> `AncEvent.StartPatientEditor`.
- Tests already touching these: `AncWorkflowTest` (`deliveryPlanningCard`, `editDating`), `PatientHubDesignTest`, `ClinicalFormatTest`.

## Gates
Tier 0-1: `./gradlew testDebugUnitTest` (JVM). Tier 2: Compose UI test for ORACLE-A6 (androidTest; compile with `./gradlew compileDebugAndroidTestKotlin`, run only if an emulator is available, else report "not run").
Level-3 edit rule: frontier-tier review plus independent test review required.
