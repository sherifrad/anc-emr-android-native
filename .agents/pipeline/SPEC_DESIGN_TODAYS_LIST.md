# SPEC: Design Round - Medication "Today's List" (Mock d1 §3 & REDESIGN_SPECS §4.3)

## 1. Context and Purpose
Implements the approved "Today's list" design from mock `.agents/pipeline/design/d4/d1/index.html` section 3 and `REDESIGN_SPECS.md` Section 4.3, respecting owner decisions D20, D22, D24.
This is a DESIGN round: UI changes only in `ui/` and `res/values/strings.xml`. No files under `model/`, `state/`, or `storage/` are changed.

## 2. Requirements & UI Elements

### 2.1 "Tap to add" Block (M420, tag `medTapToAddCard`)
In `WorkspaceScreen.kt` when `section == WorkspaceSection.MEDICATIONS`:
1. Section header: "Tap to add" with subtitle "usual dose, today".
2. Search bar (`medSearchField`): filter tiles or search drug name.
3. Category chips (`medTab-common`, `medTab-recent`): "Common" (predefined clinical presets) and "Recent" (recently prescribed or patient's past drugs).
4. Quick drug tiles grid (`medTilesGrid`):
   - Predefined common pregnancy medications:
     - Folic Acid (5 mg · Oral)
     - Ferrous Sulfate (200 mg · Oral)
     - Calcium + D3 (500 mg / 400 IU · Oral)
     - Prenatal vitamins (1 tablet · Oral)
     - Low-Dose Aspirin (150 mg · Oral)
     - Paracetamol (500 mg · Oral)
     - Progesterone (200 mg · Oral)
     - Enoxaparin (LMWH) (40 mg · Subcutaneous)
     - Labetalol (100 mg · Oral)
     - Methyldopa (250 mg · Oral)
     - Cefuroxime (500 mg · Oral)
   - Plus custom drugs added via `CustomChoiceList.MEDICATION_NAME`.
   - Each tile (`medTile-<id>`):
     - Bold drug name
     - Usual dose · route
     - Plus icon `+` (or `✓` and active tint if already on patient's active list)
     - 48 dp minimum touch target
     - Tap behavior:
       - If already active on patient: shows toast/feedback ("Already on today's list").
       - If not active: one tap adds the medication with default dose, route "Oral", status Active, start date today, and displays the shared Undo bar!
5. "+ Add other" chip (`medAddOtherChip`):
   - End of the tiles/chips list.
   - Uses `CustomChoicesDelegate` on `CustomChoiceList.MEDICATION_NAME`.
   - Shows dialog or inline input to type a new drug name.
   - Adds the new name to `CustomChoiceList.MEDICATION_NAME` for this and future sessions, and adds it to the list.
6. "+ Add medication" button (`addMedication`):
   - Outlined or ghost button to open full `MedicationEditor` for manual entry.

### 2.2 "Today's list" Block (M421, tag `medTodayList`)
1. Header: "Today's list" with subtitle "${activeCount} active".
2. Active medication cards (`medication-${med.id}`):
   - Bold drug name
   - Active badge (neutral outlined)
   - Instructions line ("dose · route · note", max 2 lines with ellipsis, full on expand)
   - 48 dp Action buttons:
     - Stop button (`stopMedication-${med.id}`): 1-tap toggles status to Stopped (stored as Discontinued), shows shared Undo bar.
     - Edit button (`editMedication-${med.id}`): opens `MedicationEditor` for details.
     - Delete button (`delete-medication-${med.id}`): shared `DeleteIconButton`.
   - In-line chips for dose & route to quickly pick common doses or routes.
3. Collapsible list support (`medicationToggle-${med.id}`, `medicationsToggleAll`):
   - Preserves all tags from round 5d.
4. "Stopped in this session" Section (M422, tag `medStoppedSection`):
   - Displayed when the patient has stopped/discontinued medications.
   - Shows stopped medications with muted styling.
   - 48 dp "↺ Undo" button (`restartMedication-${med.id}`) to restore status back to Active.

## 3. Outcome Oracles
- O1: Tapping a non-active drug tile in `medTilesGrid` immediately adds it to the active medication list and triggers the shared Undo bar.
- O2: Tapping `stopMedication-${med.id}` on an active medication marks it Stopped (Discontinued) and triggers the Undo bar.
- O3: Tapping `restartMedication-${med.id}` on a stopped medication restores it to Active.
- O4: `medAddOtherChip` allows adding a custom drug name via `CustomChoiceList.MEDICATION_NAME`.
- O5: All previous test tags (`medication-${id}`, `medicationToggle-${id}`, `medicationsToggleAll`, `editMedication-${id}`, `addMedication`) remain functional.
- O6: Strict 48 dp touch target and 360 dp width compliance with light theme default.
