# Specification: Laboratory Batch Screen (`LabBatchScreen.kt`)
**Source Mock**: `.agents/pipeline/design/rev-b/lab-batch.html`
**Design Tokens**: Navy (`#0D1B3D`), Ivory (`#FBF8F5`), Rose (`#934F5B` / `#FFD9DE`), Teal (`#176B70`), Muted (`#6B6166`), Border (`#CAC4D0`)
**Target Width**: 360 dp (Honor 400). Minimum touch target: 48 dp. All labels in `strings.xml`.

---

## 1. Mock Element → Compose Component Mapping

| Mock Element (`rev-b/lab-batch.html`) | Compose Target | Source / Location | Design Tokens & Rules |
|---|---|---|---|
| **Page Scaffold** (`.inner`, `.appbar`, `.scroll`, `.foot`) | `EditorPage(bottomBar = ...)` | `ui/PatientEditor.kt` (shared) | Single scroll container, pinned bottom bar with `.imePadding()`, horizontal padding 10-16 dp. |
| **Top App Bar** (`.appbar`) | Handled by `EditorPage` header + top back arrow | `ui/PatientEditor.kt` | Title "Laboratory results", Subtitle "Patient Name · GA Xw Yd", Tint `ClinicalNavy`. |
| **Batch Info Bar** (`.batch`) | `BatchInfoBar` | `ui/DesignSystem.kt` (new shared) | 14 dp rounded surface, 1 dp border, shows "Date · GA", "Lab Name", and "Change ▾" affordance to edit batch metadata. |
| **Tab Bar** (`.tabs`, `.chip`) | `BatchTabRow` with 3 `FilterChip`s | `ui/LabBatchScreen.kt` | "＋ Add", "Enter (n/total)", "Review (n)". Active: rose container `ActionRose.copy(0.15f)`, bold text; min 48 dp target. |
| **Section Header** (`.sh`) | `SectionHeader(title, subtitle)` | `ui/DesignSystem.kt` (shared) | Small-caps 11 sp, letter-spacing 0.08em, 4 dp rose vertical bar on the left (`ActionRose`), subtitle muted. |
| **Cards** (`.card`) | `SectionCard` / `Card` | `ui/DesignSystem.kt` (shared) | 16 dp radius, 1 dp outline-variant border, 10 dp padding, surface color. |
| **Panel Row** (`.pcard`) | `PanelAddCardRow` | `ui/LabBatchScreen.kt` | Panel name bold, element summary text, `Choose` (outlined button) + `＋ Add N` (navy button). |
| **Search Input** (`.search`) | `OutlinedTextField` | Compose M3 | 12 dp rounded corners, placeholder "Search a test, e.g. ferritin". |
| **Often Added Chips** (`.chips`, `.chip`) | `SuggestionChip` / `AssistChip` | Compose M3 | "Ferritin", "TSH", "Vitamin D", "＋ Add other". 40 dp height, 48 dp touch target. |
| **Choose Subset Sheet** (`slice 2`) | `ModalBottomSheet` | Compose M3 | Displays element checklist with toggle pills, helper text, "Save selection as panel…", Cancel + "Add N selected". |
| **Segmented Progress** (`.segs`) | `BatchSegmentedProgress` | `ui/DesignSystem.kt` (new shared) | 4 dp height, 2 dp radius; done = navy, current = rose, remaining = outlineVariant. |
| **Numeric Value Display** (`.big`) | `Text` 34 sp bold + `OutlinedTextField` | Compose M3 | Centered large value display; phone decimal keyboard (per D17). Blank range styled "from the report". |
| **Result Status Pill** (`.pill`, `.pillrow`) | `ClinicalBadges` / `ResultBadge` | `ui/ClinicalBadges.kt` (shared) | `Low ▼` (red `#FEE2E2/#B91C1C`), `Normal ✓` (green `#DCFCE7/#166534`), `High ▲` (red), `pending` (amber `#FEF3C7/#92400E`). |
| **Qualitative Choices** (`.opt-big`) | `QualitativeChoiceButton` | `ui/DesignSystem.kt` (new shared) | 52 dp tall choice buttons, 12 dp radius. Selected: rose container + checkmark. Drives urinalysis/dipstick. |
| **Entry Nav Row** (`.navr`) | `Row` with `OutlinedButton` + `Button` | Compose M3 | `◀ Back` (outlined), `Next ▶` (navy pill), 48 dp min height. |
| **Review Grouped Rows** (`.rv`) | `ReviewResultRow` grouped by panel | `ui/LabBatchScreen.kt` | Pure grouping function (`groupLabBatchItems`); shows test name, unit, value, pill badge, tap to edit. |
| **"Mark empty as normal"** | `TextButton` | Compose M3 | Only affects qualitative empty elements (per Spec §5), sets them to "Normal" / "Negative". |
| **Bottom Bar** (`.foot`) | `EditorPage(bottomBar = ...)` | `ui/PatientEditor.kt` | `Cancel` (outlined 48 dp) + `Save N results` (navy 48 dp, disabled when N=0). |
| **Undo Toast** (`.toast`) | `UndoBarHost` | `ui/DeleteUndoUi.kt` (shared) | 8-second toast: "N results saved · Patient Name" with "Undo" button. |

---

## 2. Shared vs New Component Policy
- **Shared components reused**:
  - `EditorPage` (from `ui/PatientEditor.kt`)
  - `SectionHeader` (from `ui/DesignSystem.kt`)
  - `SectionCard` (from `ui/DesignSystem.kt`)
  - `ClinicalBadges` (from `ui/ClinicalBadges.kt`)
  - `UndoBarHost` (from `ui/DeleteUndoUi.kt`)
- **New shared components (added to `ui/DesignSystem.kt`)**:
  - `BatchInfoBar(date, gaText, labName, onChangeClick)`
  - `BatchSegmentedProgress(total, current, filledIndices)`
  - `QualitativeChoiceButton(label, isSelected, onClick)`
- **Screen-private components (in `ui/LabBatchScreen.kt`)**:
  - `ChoosePanelSheet`
  - `PanelAddCardRow`
  - `ReviewResultRow`
  - Pure grouping function: `groupLabBatchItems(items, groups)`
