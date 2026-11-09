# Consolidated Design Document Review Report: High-Density Patient Directory Card & Actions Sheet Redesign

**Target Document:** `docs/designs/high_density_patient_directory_card.md`  
**Semantic Split:** FeatureRange: lines 1..236 | DetailedRange: lines 237..295  
**Contextual Anchor:** `concepts_brief.md`  
**Harness / Synthesizer:** EGM Synthesizer (Multi-Agent Swarm Orchestrator)  

---

## 1. Reviewer & Stage Execution Status

| Stage | Stage Name | Reviewer Lenses | Target Scope | Status | Finding Count |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Stage 5** | Initial Zoom Out | `egm-zoom-out`, `egm-inventive-catalyst` | Full File (1..293) | **Completed** | 4 findings |
| **Stage 6** | Comprehension Test | `egm-comprehension` | Feature (1..236) & Detailed (237..293) | **Completed** | 3 findings |
| **Stage 7** | Critic Review | `egm-design`, `egm-structural`, `egm-style` | FeatureRange (1..236) | **Completed** | 5 findings |
| **Stage 8** | Intermediate Zoom Out | `egm-zoom-out` | Full File (1..293) | **Completed** | 3 findings |
| **Stage 9** | Implementation Readiness | `egm-implementation-readiness`, `egm-codebase-details`, `egm-style` | DetailedRange (237..293) | **Completed** | 7 findings |

**Consolidated Finding Severity Breakdown:**
* 🔴 **Blockers:** 5
* 🟡 **Majors:** 5
* 🟢 **Nits:** 3
* **Total Actionable Items:** 13

---

## 2. Stage-by-Stage Synthesis Reports

### Stage 5: Initial Zoom Out (`zoom_out_report.md`)
*   **Strategic & Clinical Purpose Alignment (`egm-zoom-out`)**: The transition from 180–220dp cards to a 104–112dp standardized envelope directly resolves the primary clinical bottleneck during high-volume ward rounds (30–50 patients). Displaying 5–6 patients per viewport restores rapid scanning without continuous vertical scroll churn.
*   **Inventive Contradictions & Pattern Resolutions (`egm-inventive-catalyst`)**:
    *   *Density vs. Information Loss*: Segmenting the card into a 3-column micro-grid (~100dp / ~104dp / ~100dp) resolves the mobile spreadsheet contradiction without resorting to horizontal scrolling.
    *   *Alert Desensitization vs. Prominence*: Replacing the bloated salmon callout banner with an ambient 3.5dp leading-edge severity bar resolves visual alarm fatigue.
*   **Lifecycle & Ecosystem Gaps (`egm-zoom-out`)**: The bottom sheet "Record Visit" bypasses the patient chart. While beneficial for speed, it lacks clinical safeguards for archived records and bypasses the state machine's established navigation orchestrator.

### Stage 6: Comprehension Test (`comprehension_report.md`)
*   **Specification Ambiguities (`egm-comprehension`)**:
    *   *Projection Data Class Drift*: Section 3 displays a revised `PatientDashboardRowProjection` signature that omits `ageText`, `isArchived`, and `datingMethodText`, while Phase 1 instructs to "extend" the class.
    *   *Event Dispatch Pattern Conflict*: Section 1.4 lists `onStartVisit(patient.id, null)` as a direct method call, whereas the surrounding architecture uses `onEvent(AncEvent.StartVisit(...))`.
    *   *Empty Vitals Fallback Inconsistency*: Line 27 specifies a `BP: --/--` placeholder for new patients, but line 132 describes extracting `latestVitalsBp` via `maxByOrNull { it.date }` without defining how null blood pressure within an existing visit is formatted.

### Stage 7: Critic Review (`critic_report.md`)
*   **Ergonomics & Viewport Safety (`egm-design`)**:
    *   *Header Row Crowding on 360dp*: Packing `displayName` (`maxLines = 1`), `ClinicalRiskBadge` (~85dp), and `MoreVert` (48dp touch target) into a single row on a 360dp phone leaves only ~150dp for patient names, causing severe truncation of compound names.
    *   *Nested Touch Area Conflicts*: The card container attaches `Modifier.clickable { onEvent(AncEvent.OpenWorkspace) }` while nesting a 48dp `IconButton`. Touch target padding must ensure zero mis-taps when opening the bottom sheet menu.
*   **Architecture & Modularity (`egm-structural`)**:
    *   *Anti-Spaghetti Violation (Rule 2)*: Keeping all decomposed composables inside `PatientListScreen.kt` violates the <700 line file size ceiling. The file is already 859 lines; adding these components will expand it beyond 1,000 lines unless extracted to modular files.
*   **Visual Style & Consistency (`egm-style`)**:
    *   *Typography & Spacing Tokens*: Font weights and colors must align with canonical tokens (`MaterialTheme.typography.labelSmall`, `ActionRose`, `TagHighRiskText`).

### Stage 8: Intermediate Zoom Out (`intermediate_zoom_out_report.md`)
*   **End-to-End Flow & Regression Audit (`egm-zoom-out`)**:
    *   *CUJ 1 (Directory Scanning)*: Fully validated; density increases by ~150%.
    *   *CUJ 2 (Fast-Track Visit)*: Architecturally broken at the seam between `AncApp.kt` and `PatientListScreen.kt` due to undeclared parameter routing. Remediated via canonical MVI dispatch.
    *   *CUJ 3 (Workspace Navigation)*: Fully validated; card-level single tap eliminates redundant buttons.
    *   *Rule of Reusability (Rule 1)*: Bedside vitals in Column 3 should consume shared clinical tokens.

### Stage 9: Implementation Readiness Test (`implementation_readiness_report.md`)
*   **Ground Truth Codebase Audit (`egm-codebase-details`)**:
    *   *Unresolved Reference: `.formatted`*: `DemoVisit.bloodPressure` is `String?`, not an object. Calling `visit.bloodPressure?.formatted` will fail compilation.
    *   *Unresolved Reference: `pulseBpm`*: `DemoVisit` and Room database schema v6 have no `pulseBpm` field. Referencing it will fail compilation, and adding it violates the frozen schema v6 invariant.
    *   *Type Mismatch: `severeBpAttention`*: `severeBpAttention()` expects `List<DemoVisit>` (`patient.visits`), not `DemoPatient`. Calling `severeBpAttention(patient)` causes a compilation error.
    *   *Missing Touched File*: `AncApp.kt` is omitted from touched files despite the design relying on `onStartVisit`.
    *   *Late-Term vs. Post-Term Mismatch*: `GestationalAgeResult.isPostTerm` triggers at $\ge 42$ weeks, but the spec mandates a badge at $\ge 41$ weeks (`isLateTerm`).

---

## 3. Prioritized Actionable Findings

### 🔴 Blockers (Must Fix Prior to Implementation)

#### Finding B1: Compilation Error — Invalid `.formatted` Property Access on `DemoVisit.bloodPressure`
*   **Affected Scope:** Lines 132, 256
*   **Discrepancy:** The design specifies `patient.visits.maxByOrNull { it.date }?.bloodPressure?.formatted`. `DemoVisit.bloodPressure` is already `String?`.
*   **Remediation:** Use `patient.visits.maxByOrNull { it.date }?.bloodPressure` directly.

#### Finding B2: Compilation Error & Schema v6 Violation — Non-Existent `pulseBpm` Property
*   **Affected Scope:** Lines 126, 133, 251, 256
*   **Discrepancy:** `DemoVisit` has no `pulseBpm` field, and Room schema v6 is frozen.
*   **Remediation:** Drop `latestVitalsPulse`. Render maternal blood group (`row.bloodGroup ?: "--"`) in the secondary vitals line.

#### Finding B3: Compilation Error — Type Mismatch on `severeBpAttention()` Invocation
*   **Affected Scope:** Lines 134, 258
*   **Discrepancy:** `severeBpAttention` takes `List<DemoVisit>`, not `DemoPatient`.
*   **Remediation:** Pass `patient.visits`: `severeBpAttention(patient.visits) != null`.

#### Finding B4: Unidirectional MVI Violation & Missing `AncApp.kt` Wire-Up for `onStartVisit`
*   **Affected Scope:** Lines 29, 84, 162, 240, 280
*   **Discrepancy:** `PatientListScreen` takes `(state: AncState, onEvent: (AncEvent) -> Unit)`. Calling `onStartVisit` as a direct callback violates MVI.
*   **Remediation:** Dispatch `onEvent(AncEvent.StartVisit(patientId = p.id, visitId = null, editorSessionId = UUID.randomUUID().toString()))`.

#### Finding B5: Accidental Field Stripping in `PatientDashboardRowProjection`
*   **Affected Scope:** Lines 111–130 vs. Lines 249–253
*   **Discrepancy:** Code snippet omits `ageText`, `isArchived`, and `datingMethodText`.
*   **Remediation:** Explicitly retain all existing fields in `PatientDashboardRowProjection`.

---

### 🟡 Majors (Must Fix to Prevent Safety, UX, or Architectural Regressions)

#### Finding M1: Clinical Threshold Mismatch — Post-Term vs. Late-Term GA Logic
*   **Remediation:** Compute `isLateOrPostTerm = gaResult != null && gaResult.weeks >= 41`.

#### Finding M2: Clinical Safety Hole — Missing Invalidation for Archived Patients in Actions Sheet
*   **Remediation:** Disable active clinical actions (`Record Visit`, `Edit Demographics`) when `patient.status == "archived"`.

#### Finding M3: Viewport Safety Hazard — Header Text Truncation on 360dp Viewports
*   **Remediation:** Allow `maxLines = 2` on patient display name, or move the clinical risk badge to Column 3 to guarantee full name legibility.

#### Finding M4: Architectural Constitution Violation — Monolithic File Size Expansion (Rule 2)
*   **Remediation:** Extract modular components into `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/` (`PatientDirectoryCard.kt`, `PatientDirectoryMetricsRow.kt`, `PatientActionsModalSheet.kt`), reducing `PatientListScreen.kt` to $<500$ lines.

#### Finding M5: BiDi Scanning Disorientation — Missing LayoutDirection Lock on Card Container
*   **Remediation:** Wrap `PatientDirectoryMetricsRow` in `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)` while keeping `displayName` script-aware.

---

### 🟢 Nits (Quality of Life & Code Hygiene)

#### Finding N1: Col 3 Horizontal Width Pressure Under Font Accessibility Scaling
*   **Remediation:** Apply `fontFeatureSettings = "tnum"` and omit `"mmHg"` suffix on $\le 360\,\text{dp}$ screens if font scale $> 1.15x$.

#### Finding N2: String Resource Hardcoding in Specification
*   **Remediation:** Use parameter-formatted strings in `strings.xml`.

#### Finding N3: Test Fixture Coverage for Projection Edge Cases
*   **Remediation:** Enumerate specific test cases for null dating, visit without BP, late-term threshold, and isolated severe systolic BP in `PatientListProjectionTest.kt`.

---

## 4. Traceability Matrix

| Finding ID | Severity | Category | Target Doc Scope | Codebase File Affected | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **B1** | 🔴 Blocker | Compile Error | Lines 132, 256 | `PatientListScreen.kt` | Remediated |
| **B2** | 🔴 Blocker | Schema v6 / Compile | Lines 126, 133, 256 | `PatientListScreen.kt`, `DemoVisit.kt` | Remediated |
| **B3** | 🔴 Blocker | Compile Error | Lines 134, 258 | `PatientListScreen.kt` | Remediated |
| **B4** | 🔴 Blocker | MVI Architecture | Lines 29, 84, 162, 280 | `PatientListScreen.kt`, `AncApp.kt` | Remediated |
| **B5** | 🔴 Blocker | Data Integrity | Lines 111–130, 249 | `PatientListScreen.kt` | Remediated |
| **M1** | 🟡 Major | Clinical Logic | Lines 79, 135, 259 | `PatientListScreen.kt`, `GestationalAgeEngine.kt` | Remediated |
| **M2** | 🟡 Major | Clinical Safety | Lines 83–88, 278 | `PatientListScreen.kt` | Remediated |
| **M3** | 🟡 Major | Viewport Ergonomics | Lines 74–76, 95 | `PatientListScreen.kt` | Remediated |
| **M4** | 🟡 Major | Anti-Spaghetti (Rule 2) | Lines 66–67, 240 | `PatientListScreen.kt` | Remediated |
| **M5** | 🟡 Major | BiDi Consistency | Lines 30, 72, 268 | `PatientListScreen.kt` | Remediated |
| **N1** | 🟢 Nit | A11y Font Scaling | Lines 81, 174 | `PatientListScreen.kt` | Remediated |
| **N2** | 🟢 Nit | Localization | Lines 269–271 | `strings.xml`, `PatientListScreen.kt` | Remediated |
| **N3** | 🟢 Nit | Test Robustness | Line 242 | `PatientListProjectionTest.kt` | Remediated |
