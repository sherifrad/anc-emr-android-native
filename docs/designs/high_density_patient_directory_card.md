# High-Density Patient Directory Card & Actions Sheet Redesign

*   **Author:** sherifrad
*   **Status:** Approved (Remediated via EGM Review Swarm)
*   **Date:** 2026-11-09

--------------------------------------------------------------------------------

## Overview

This design document specifies the visual, architectural, and interaction overhaul of the primary patient directory (`PatientListScreen`). It replaces oversized, variable-height cards (~180–220dp) with a standardized, high-density clinical card (`Modifier.heightIn(min = 104.dp)`) that renders a structured 3-column metric grid (Gestational Age & EDD, Obstetric Parity & History, Bedside Vitals/BP with color-coded safety indicators). It eliminates triple-redundant click affordances in favor of single-tap workspace navigation, replaces the bloated footer with a consolidated 4-action clinical bottom sheet, and enforces strict start-alignment across all patients regardless of language script.

## The Problem

1. **Low Information Density & Excessive Scrolling**: Each card currently consumes 180–220dp of vertical space, showing only 2 to 3 patients on standard mobile viewports. High-volume obstetric rounds require clinicians to scan 30–50 patients efficiently.
2. **Ragged Badging & Visual Disorientation**: Up to 5 clinical badges (Risk level, Severe BP, Parity GPA, Age, Blood Group) wrap unpredictably inside an unconstrained `FlowRow`, causing adjacent cards to vary in height and forcing the clinician's eye to zig-zag across rows.
3. **Triple-Redundant Click Affordances & Footer Bloat**: The card container, the bottom sheet menu, and two floating icon buttons (`EditNote` and `AccountCircle`) all perform the identical action (`AncEvent.OpenWorkspace`). The floating buttons waste ~48dp of height for zero functional benefit.
4. **BiDi Alignment Inconsistency**: Arabic patient names align to the far right while Latin clinical metrics (`GA 38+3 wks · EDD 2026-11-20`) align to the far left, creating disjointed, ragged scanning patterns across mixed patient rosters.
5. **Intrusive Alert Boxes**: Every patient card renders an oversized salmon-tinted callout box for minor notes, creating visual noise and masking critical safety alerts like severe pre-eclampsia.

## Goals

*   **Standardize High-Density Card Envelope**: Implement `Modifier.heightIn(min = 104.dp)` (~108–112dp standard height) displaying 5–6 patients simultaneously on a 360dp–412dp screen without text truncation.
*   **Structured 3-Column Clinical Micro-Grid**: Allocate predictable column positions on 360dp viewports (~100dp / ~104dp / ~100dp):
    *   Column 1: Gestational Age (`34+2 wks`) and EDD (`Due 14 Nov 2026`). Highlights late/post-term if $\ge 41$ weeks.
    *   Column 2: Obstetric Parity (`G3 P2 A0`) and prior delivery tags (`Prev: 2x C/S` or `Nulliparous`).
    *   Column 3: Bedside Vitals (`120/80 mmHg`) with color-coded alerts, blood group (`Rh+ / Rh-`), and a standardized placeholder (`BP: --/--`) for new patients.
*   **Ambient 3.5dp Severity Accent Strip**: Replace the bloated callout box with a leading-edge color bar (Red for Severe BP / High Risk, Amber for Medium Risk, Neutral for Low Risk).
*   **Consolidated Clinical Actions Bottom Sheet**: Streamline the "⋮" menu into 4 primary actions: New Visit (canonical `AncEvent.StartVisit`), Open Chart (`AncEvent.OpenWorkspace`), Edit Demographics (`AncEvent.StartPatientEditor`), and Archive/Restore (`AncEvent.StartStatusEditor`).
*   **Strict Start-Alignment & BiDi Protection**: Anchor the card container and 3-column microgrid to LTR (`LayoutDirection.Ltr`) while preserving content-aware script rendering for Arabic patient names.
*   **Anti-Spaghetti Modularity (Rule 2)**: Extract decomposed components into `ui/patientdirectory/` to keep `PatientListScreen.kt` under 500 lines.
*   **Maintain SAMD Safety & Schema v6 Invariant**: Zero SQLite Room database migrations; zero regression in gestational age calculations or blood pressure thresholds.

## Non-Goals

*   **No Multi-Column Desktop Spreadsheet on Mobile**: No horizontally scrolling desktop data grid on 360dp mobile phones.
*   **No Database Schema Changes**: No Room entity or database schema modifications; all new projections are computed in-memory.
*   **No Alterations to Clinical Algorithms**: Core gestational age engines, blood pressure staging rules, and codex validation logic remain untouched.

## Open Questions

*   None. (All core design trade-offs regarding accessibility font scaling, missing vitals placeholders, and leading accent lines were resolved during brainstorming and audit).

## Critical User Journeys (CUJs)

### CUJ 1: High-Volume Roster Scanning at Morning Rounds
*   **Given**: An obstetrician opens `PatientListScreen` on a 360dp phone with 35 active patients.
*   **When**: The clinician scrolls through the patient directory.
*   **Then**: 5–6 patients are visible per screen; Column 1 clearly displays GA/EDD, Column 2 shows parity history, Column 3 displays latest BP, and patients with severe pre-eclampsia are immediately identifiable by a prominent 3.5dp red leading border and red BP badge.

### CUJ 2: Fast-Track Direct Visit Documentation
*   **Given**: A clinician identifies an active walk-in patient in the directory.
*   **When**: The clinician taps the "⋮" menu button on the patient's card and selects "New Visit".
*   **Then**: The app dispatches canonical `AncEvent.StartVisit` directly into the Visit Editor pre-populated with the patient's context, bypassing the intermediate chart overview. (Disabled for archived records).

### CUJ 3: Seamless Chart Navigation
*   **Given**: A clinician wants to inspect ultrasound scans or laboratory reports for a patient.
*   **When**: The clinician taps anywhere on the patient card body.
*   **Then**: The app emits `AncEvent.OpenWorkspace` and navigates cleanly into the Patient Workspace.

--------------------------------------------------------------------------------

## Technical Plan (Architecture)

### 1. Architectural Overview & Component Decomposition

To comply with the Core Architectural Constitution's Anti-Spaghetti Invariant (Rule 2: file size ceiling $<1,500$ lines), the monolithic `PatientDirectoryCard` in `PatientListScreen.kt` is extracted into modular composables under `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/`:

1.  **`PatientDirectoryCard.kt` (Parent Container)**:
    *   Enforces `Modifier.fillMaxWidth().heightIn(min = 104.dp)`.
    *   Attaches `Modifier.clickable { onEvent(AncEvent.OpenWorkspace(patient.id)) }` to the entire card body.
    *   Renders an ambient 3.5dp leading-edge indicator line inside the card border mapping directly to clinical risk and severe blood pressure status.
2.  **`PatientDirectoryCardHeader.kt`**:
    *   Renders the patient's display name (`weight(1f)`, `maxLines = 2`, `overflow = TextOverflow.Ellipsis`) to prevent truncation on 360dp viewports.
    *   Renders the canonical `ClinicalRiskBadge` (or `BpAlertLabel` if severe BP is detected).
    *   Renders the isolated `MoreVert` ("⋮") icon button with `Modifier.minimumTouchTarget()` ($\ge 48\,\text{dp}$) and `testTag("patientMenu-${patient.caseId}")` to trigger the actions sheet.
3.  **`PatientDirectoryMetricsRow.kt` (3-Column Micro-Grid)**:
    *   Wrapped in `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)` to prevent column order mirroring on Arabic system locales.
    *   Allocates a horizontal `Row` containing three equal `Modifier.weight(1f)` columns (~100dp / ~104dp / ~100dp on a 360dp device):
        *   **Column 1 (`GestationalAgeColumn`)**: Primary: bold GA text (e.g., `34+2 wks` or `Pending`). Secondary: muted EDD text (e.g., `Due 14 Nov`). Highlights with post-term badge if $\ge 41$ weeks (`isLateOrPostTerm`).
        *   **Column 2 (`ObstetricHistoryColumn`)**: Primary: bold parity summary (`G3 P2 A0`). Secondary: prior delivery tags (e.g., `Prev: 2x C/S` or `Nulliparous`).
        *   **Column 3 (`BedsideVitalsColumn`)**: Primary: bold latest blood pressure (e.g., `120/80 mmHg`) with color threshold flags (`ActionRose` for severe BP $\ge 160/110$, `TagHighRiskText` for stage 1/2 HTN, green check for normal). Secondary: maternal blood group (`Blood: O+`). Displays `BP: --/--` placeholder when no visits exist. Uses `fontFeatureSettings = "tnum"`.
4.  **`PatientActionsModalSheet.kt`**:
    *   Replaces the legacy 4-item list with a clean clinical modal bottom sheet containing 4 non-redundant actions:
        1.  **Record Visit**: Dispatches `onEvent(AncEvent.StartVisit(p.id, null, UUID.randomUUID().toString()))` (disabled if `row.isArchived`).
        2.  **Open Chart / Workspace**: Emits `AncEvent.OpenWorkspace(patient.id)`.
        3.  **Edit Demographics**: Emits `AncEvent.StartPatientEditor(patient.id, ...)`.
        4.  **Archive / Restore Patient**: Emits `AncEvent.StartStatusEditor(...)`.

---

### 2. Visual Blueprint & Layout Structure

```text
+-----------------------------------------------------------------------------------+
|[Accent]  [Patient Full Name (up to 2 lines)]               [HIGH RISK]        [⋮] | <- Header (~32dp)
|  3.5dp  |                                                                         |
|  Line   |  [ Col 1: GA & EDD ]      [ Col 2: Parity ]       [ Col 3: Vitals ]     | <- Micro-Grid
| (Rose / |  34+2 wks                 G3 P2 A0                140/90 mmHg           |    (~56dp)
|  Amber) |  Due 14 Nov 2026          Prev: 2x C-Section      Blood: O+ (Rh+)       |
+-----------------------------------------------------------------------------------+
Min Height: 104dp | Natural Rest Height: ~108-112dp | Zero Horizontal Truncation at 360dp
```

---

### 3. In-Memory Data Projection Enhancements (`PatientDashboardRowProjection`)

To feed the 3-column micro-grid without executing runtime database queries or altering Room schema v6, `projectDashboardRow(patient)` in `PatientListScreen.kt` retains all existing fields and appends high-density vitals projections:

```kotlin
data class PatientDashboardRowProjection(
    val id: String,
    val caseId: String,
    val displayName: String,
    val ageText: String?,
    val isArchived: Boolean,
    val obstetricSummary: String,
    val gestationalAgeText: String,
    val eddText: String?,
    val datingMethodText: String?,
    val bloodGroup: String?,
    val isRhNegative: Boolean,
    val isRhPositive: Boolean,
    val isHighRisk: Boolean,
    val riskTier: PatientRiskTier,
    val clinicalAttention: String?,
    // High-density projections:
    val latestVitalsBp: String?,
    val isSevereBp: Boolean,
    val isLateOrPostTerm: Boolean,
)
```

*   `latestVitalsBp`: Extracted from `patient.visits.maxByOrNull { it.date }?.bloodPressure`. If no visits or BP recorded, returns `null`.
*   `isSevereBp`: Evaluated via `severeBpAttention(patient.visits) != null`.
*   `isLateOrPostTerm`: Evaluated via `patient.dating?.calculateGestationalAgeResult()?.let { it.weeks >= 41 } == true`.

---

### 4. Data Flow & Event Routing (Unidirectional MVI)

```mermaid
sequenceDiagram
    autonumber
    actor Clinician
    participant Directory as PatientListScreen
    participant Card as PatientDirectoryCard
    participant Sheet as PatientActionsModalSheet
    participant ViewModel as AncViewModel
    participant Hub as PatientHubScreen
    participant Editor as VisitEditor

    Note over Directory,Card: Clinician scrolls directory (5-6 patients visible)
    
    alt Direct Workspace Tap
        Clinician->>Card: Tap Card Body
        Card->>ViewModel: AncEvent.OpenWorkspace(patientId)
        ViewModel->>Hub: Navigate to Patient Hub
    else Fast-Track Visit Documentation
        Clinician->>Card: Tap MoreVert ("⋮")
        Card->>Sheet: Open Actions Bottom Sheet
        Clinician->>Sheet: Tap "Record Visit"
        Sheet->>ViewModel: AncEvent.StartVisit(patientId, null, uuid)
        ViewModel->>Editor: Navigate directly to Visit Editor
    else Secondary Management
        Clinician->>Sheet: Tap "Edit Demographics" or "Archive"
        Sheet->>ViewModel: AncEvent.StartPatientEditor / StartStatusEditor
    end
```

---

### 5. Viewport & Safety Invariants

*   **360dp Viewport Math**: On a 360dp phone, outer horizontal padding is 16dp on each side ($360 - 32 = 328\,\text{dp}$). Card inner padding is 10dp on each side ($328 - 20 = 308\,\text{dp}$). Three equal columns have ~102dp width each. With 12sp bold typography, `34+2 wks` occupies ~52dp, `G3 P2 A0` occupies ~58dp, and `140/90 mmHg` occupies ~74dp—guaranteeing zero text truncation.
*   **Touch Bounds**: Card body covers the full $328 \times 108\,\text{dp}$ touch area. The "⋮" menu button is padded to $48 \times 48\,\text{dp}$.
*   **Database Invariant**: Schema v6 is frozen; all projections operate on immutable in-memory state slices.

--------------------------------------------------------------------------------

## Alternatives Considered

### Alternative 1: Full Tabular Data Grid (Spreadsheet-Style Columns)
*   *Rejected by author*: Unacceptable clinical safety hazard on handheld mobile devices due to off-screen alert hiding and high probability of mis-reading truncated clinical metrics.

### Alternative 2: Compact Master Row with Tap-to-Expand Accordion
*   *Rejected by author*: Increases cognitive friction and interaction overhead during high-volume clinical rounds where instant visibility of vitals and gestational age is mandatory.

### Alternative 3: Retaining Floating Footer Icon Buttons
*   *Rejected by author*: Redundant click affordances that waste critical vertical space (~36dp) and introduce accidental navigation mis-taps.

### Alternative 4: Full-Width Prominent Attention Callout Box on Every Card
*   *Rejected by author*: Clutters the directory and causes alert desensitization. The ambient 3.5dp leading-edge severity line signals critical alerts immediately without consuming vertical lines of text.

--------------------------------------------------------------------------------

## Detailed Implementation Plan

### 1. Enumeration of Touched Files

| File Path | Type | Rationale |
| :--- | :--- | :--- |
| `app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt` | Modified | Core patient directory UI. Update `PatientDashboardRowProjection` with `latestVitalsBp`, `isSevereBp`, and `isLateOrPostTerm`. Replace bloated card rendering with modular calls to `PatientDirectoryCard`. File length reduced from 859 to ~480 lines. |
| `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientDirectoryCard.kt` | Created | Modular high-density card container with 3.5dp leading severity accent strip and single-tap workspace navigation. |
| `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientDirectoryMetricsRow.kt` | Created | Structured 3-column micro-grid with LTR lock, tabular figures, and safety thresholds. |
| `app/src/main/java/com/sherifradwan/ancnative/ui/patientdirectory/PatientActionsModalSheet.kt` | Created | Consolidated 4-action clinical bottom sheet dispatching canonical MVI events. |
| `app/src/main/res/values/strings.xml` | Modified | Add localized strings for the new actions bottom sheet (`quick_new_visit`, `bp_placeholder`, `parity_nulliparous`). |
| `app/src/test/java/com/sherifradwan/ancnative/ui/PatientListProjectionTest.kt` | Created | Unit test verifying in-memory extraction of latest visit vitals, severe BP detection, late-term calculation, and fallback `--/--` placeholders under diverse clinical fixture conditions. |

---

### 2. Phase-by-Phase Execution Steps

#### Phase 1: In-Memory Projection Layer Updates
1.  Extend `PatientDashboardRowProjection` in `PatientListScreen.kt`:
    *   Add `latestVitalsBp: String?`
    *   Add `isSevereBp: Boolean`
    *   Add `isLateOrPostTerm: Boolean`
2.  Update `projectDashboardRow(patient: DemoPatient)`:
    *   Inspect `patient.visits.maxByOrNull { it.date }?.bloodPressure` for `latestVitalsBp`.
    *   Compute `isSevereBp = severeBpAttention(patient.visits) != null`.
    *   Compute `isLateOrPostTerm = patient.dating?.calculateGestationalAgeResult()?.let { it.weeks >= 41 } == true`.

#### Phase 2: Modular Component Creation in `ui/patientdirectory/`
1.  **Create `PatientDirectoryCard.kt`**:
    *   Container with `Modifier.fillMaxWidth().heightIn(min = 104.dp).clickable { onEvent(AncEvent.OpenWorkspace(patient.id)) }`.
    *   3.5dp leading edge vertical severity strip (`ActionRose` for high risk / severe BP, amber for medium, neutral outline for normal).
    *   Header: `Text(row.displayName, maxLines = 2)` + `ClinicalRiskBadge` + `IconButton(MoreVert)`.
2.  **Create `PatientDirectoryMetricsRow.kt`**:
    *   `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)`.
    *   Col 1: GA (`34+2 wks`) + EDD (`Due 14 Nov 2026`). Late-term tag if `isLateOrPostTerm`.
    *   Col 2: Parity (`G3 P2 A0`) + Prior delivery tag (`Prev: 2x C/S` or `Nulliparous`).
    *   Col 3: BP (`120/80 mmHg` or `BP: --/--`) with threshold color coding + `Blood: O+`.
3.  **Create `PatientActionsModalSheet.kt`**:
    *   Clean bottom sheet with 4 canonical MVI actions:
        *   Action 1 (New Visit): Dispatches `AncEvent.StartVisit` (disabled if `row.isArchived`).
        *   Action 2 (Open Chart): Emits `AncEvent.OpenWorkspace`.
        *   Action 3 (Edit Demographics): Emits `AncEvent.StartPatientEditor`.
        *   Action 4 (Archive / Restore): Emits `AncEvent.StartStatusEditor`.

#### Phase 3: Automated Verification & Viewport Testing
1.  **Unit Tests**: Run `./gradlew testDebugUnitTest` to verify all domain, projection, and UI unit tests pass cleanly.
2.  **Physical Device Verification**:
    *   Deploy debug APK to physical device (**Honor DNY-NX9**, 360dp viewport baseline).
    *   Verify 5–6 cards render per viewport without vertical bloat.
    *   Verify zero horizontal truncation for standard GA, Parity, and BP readings.
    *   Verify touch targets on card body and "⋮" menu button respect $\ge 48\,\text{dp}$.
3.  **Git Commit**: Commit with message: `feat(ui): redesign patient directory with high-density 3-column cards and unified actions sheet`.
