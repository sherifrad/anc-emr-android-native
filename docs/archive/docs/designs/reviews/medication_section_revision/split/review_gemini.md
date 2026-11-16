# Code Review: `split.patch` (State Refactoring)

Review of `docs/designs/reviews/medication_section_revision/split/split.patch` against `docs/designs/medication_section_revision.md` and `AGENTS.md`.

*Note: `split.patch` contains zero UI composables; viewport (360dp), touch target (48dp), colors, focus/Enter-to-next, and hardcoded UI string checks apply to UI files and are absent from this patch.*

---

### Blocker

#### 1. Circumvention of Level-3 Safety Risk Mapping
- **Files & Lines Cited**:
  - `split.patch:35-38` (deletes reducers from `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt`)
  - `split.patch:1420, 1638, 2117, 2362` (creates `AncEvent.kt`, `AncReducer.kt`, `MedicationReducer.kt`, `ClinicalDraftReducers.kt`)
  - `AGENTS.md:3-13` (Project risk map rules)
  - `.agents/PROJECT_MAP.json:66-72` (`AncState.kt` mapped as `crit: 3`, `clinical_safety`)
- **Failing Scenario**: `AGENTS.md` and `PROJECT_MAP.json` mandate that `AncState.kt` is a Level-3 safety-critical path (`crit: 3`, `clinical_safety`) requiring frontier-tier and independent test reviews. `AGENTS.md:12` explicitly states: *"A path missing from the map counts as level 2."*
Splitting `AncState.kt` extracts 1,410 lines of clinical safety reduction logic (visits, dating, ultrasound, lab batch, delete, and undo lifecycles) into four new files that do not exist in `PROJECT_MAP.json`. This silently downgrades the entire clinical state machine from Level 3 to Level 2, bypassing automated gating for future modifications.

---

### Major

#### 2. Unauthorized Scope Creep and Violation of Stage 1 Architecture Plan
- **Files & Lines Cited**:
  - `docs/designs/medication_section_revision.md:15-16, 41-42, 167-168, 266-270`
  - `split.patch:1410-2881`
- **Failing Scenario**: The Technical Plan and Detailed Implementation Plan strictly circumscribe Stage 1 scope. Section 4 (`medication_section_revision.md:266-270`) specifies:
  > `app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt` (Modify)
  > Proposed Changes: Delete `AncRoute.MedicationEditor` and its uses. Add `prefill` to `StartMedicationEditor`... `startMedicationEditor` no longer sets the route.
  
  Lines 167-168 emphasize: *"Level-3 files are touched (`AncState.kt`, `EditorHooks.kt`)... Mitigation: the change stays small; a frontier-tier review and an independent test review gate each phase."*
  Splitting the entire application's MVI reducer layer across all domains (obstetric histories, dating, ultrasound, labs) is nowhere in the design. It destabilizes safety-critical code outside the medication feature.

#### 3. Misapplication of AGENTS.md File Size Ceiling Invariant
- **Files & Lines Cited**:
  - `AGENTS.md:21-24` (Rule 2: Modularity & Clean Architecture)
  - `split.patch:1-38`
- **Failing Scenario**: `AGENTS.md:22` establishes:
  > **File Size Ceiling**: No single **UI file** shall exceed 1,500 lines. Monolithic screens must be decomposed into clean, domain-scoped modular tab composables and reusable component files.
  
  The rule explicitly restricts the 1,500-line ceiling to **UI files** (screens and composables). `AncState.kt` is a pure state model and reducer file containing zero Compose code. Splitting it under the guise of an architectural ceiling violation misinterprets the constitution and creates unnecessary churn.

#### 4. Encapsulation Degradation via Package-Private Modifier Leakage
- **Files & Lines Cited**:
  - `split.patch:2176` (`MedicationReducer.kt:60`)
  - `split.patch:2234` (`MedicationReducer.kt:118`)
  - `split.patch:2302` (`MedicationReducer.kt:186`)
  - `split.patch:2346` (`MedicationReducer.kt:230`)
  - `split.patch:2735` (`ClinicalDraftReducers.kt:374`)
- **Failing Scenario**: In `AncState.kt`, all reducer helper functions and domain entity converters (`DemoMedicationRecord.toDraftValues()`, `quickSaveDraft()`, `canQuickSaveMedication()`, `clearDrafts()`, etc.) were strictly `private` to `AncReducer`.
Moving them into separate top-level files forced 35 internal functions to change from `private` to `internal`. Consequently:
  - `AncState.clearDrafts()` is exposed as a top-level extension in `ClinicalDraftReducers.kt:374`.
  - `quickSaveDraft()` is exposed as a top-level function in `MedicationReducer.kt:118`.
  - Model draft converters (e.g. `toDraftValues()`) are exposed to the entire package.
  This allows arbitrary code in `com.sherifradwan.ancnative.state` to bypass `AncReducer.reduce()` and directly mutate or construct drafts, compromising unidirectional MVI integrity.

---

### Minor

#### 5. Domain Incoherence and Arbitrary Placement of Cross-Cutting Concerns
- **Files & Lines Cited**:
  - `split.patch:2277-2300` (`MedicationReducer.kt:161-184`, `startsAnotherAction`)
  - `split.patch:2302` (`MedicationReducer.kt:186`, `today`)
  - `split.patch:2735-2755` (`ClinicalDraftReducers.kt:374-394`, `clearDrafts`)
- **Failing Scenario**:
  - `MedicationReducer.kt:161` hosts `AncEvent.startsAnotherAction()`, which intercepts events across all clinical domains (`OpenWorkspace`, `StartVisit`, `StartUltrasoundEditor`, `StartLabBatch`, `RequestDelete`). Storing app-wide event predicates in a medication-specific reducer violates separation of concerns.
  - `MedicationReducer.kt:186` defines a bare `internal fun today(): String = java.time.LocalDate.now().toString()`, polluting the package namespace instead of using an established date utility.
  - `ClinicalDraftReducers.kt:374` hosts `AncState.clearDrafts()`, which clears `medicationDraft`, `labBatchDraft`, and resets `savePhase` across the entire app, yet is hidden at the bottom of the clinical draft reducers file.
