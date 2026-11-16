# Independent Code Review: Medication Section Revision Phase 3 Round 3 (`main.patch`)

Evaluated against `/docs/designs/medication_section_revision.md`, AGENTS.md rules, and Android Jetpack Compose best practices.

---

## Blocker

*None.* The patch builds cleanly, preserves frozen Room schema v6, contains no database mutations, and respects the 1,500-line ceiling (largest touched file is `PatientHubScreen.kt` at 366 lines).

---

## Major

### 1. Sheet destruction and flicker on "Save and add another" drops composition
* **File & Line**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubScreen.kt:275` (read alongside `MedicationSheet.kt:398–401`).
* **Failing Scenario**: When a clinician taps **"Save and add another"**, `AncViewModel` completes the save and dispatches `SaveCompleted`, which resets `medicationDraft = null` before the chained `StartMedicationEditor` event is dispatched. Because `PatientHubScreen.kt:275` gates the sheet strictly with `if (medicationDraft != null)`, `MedicationSheet` is completely disposed from the composition for a frame. When the second draft arrives, a brand-new `ModalBottomSheet` is instantiated and animates in from the bottom. 
* **Design Violation**: Technical Plan §Risks (L170–171) specifically mandates that the sheet's presence must be derived to prevent flicker: *"if it flickers, the sheet's visibility is derived from the draft's existence so it stays composed."* Instead, `MedicationSheet.kt:398` merely adds a comment acknowledging that the sheet might be *"shown again"*, leaving the dismissal/re-opening flicker unmitigated.

### 2. Enter-to-next cannot submit or complete the form on the last field
* **File & Line**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationForm.kt:265–274`.
* **Failing Scenario**: A clinician types the name, moves through dosage and date via the keyboard, and lands in `medNotes`. Because `medNotes` sets `singleLine = false` and `imeAction = ImeAction.Default` (L273), pressing Enter merely inserts a newline character (`\n`). 
* **Design Violation**: Section 6 (L153) explicitly states: *"MedicationForm gives each field the Next keyboard action, and the last one Done"*, and CUJ 2 (L87) requires: *"the doctor taps Save (or presses Enter on the last field)"*. There is no `ImeAction.Done` or submit action wired to the soft keyboard, making keyboard submission impossible.

---

## Minor

### 1. Violation of AGENTS.md Rule 1: Divergent "Active" indicator in picker tile
* **File & Line**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt:1139–1154`.
* **Failing Scenario**: AGENTS.md Rule 1 (*"Build Once, Use Everywhere"*) mandates: *"Every clinical domain entity ... must have exactly one canonical UI component implementation."* While `MedicationCard.kt:208` uses the canonical `MedicationStatusBadge` (`ClinicalBadges.kt:283`), `MedicationPickerTile` constructs a duplicate badge with `Icon(Icons.Default.Check)` and `Text(stringResource(R.string.medication_status_active))`. Any styling or token update to the canonical status badge will not reflect in picker tiles.

### 2. Actions and "Prescribe" button flip from end to start when wrapping at 360dp
* **File & Line**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationCard.kt:200–207` and `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/tabs/MedsTab.kt:542–548`.
* **Failing Scenario**: Both `medicationActions` and `medsActiveHeader` use `FlowRow(horizontalArrangement = Arrangement.SpaceBetween)`. On a 360dp viewport with large accessibility fonts or long translations, the actions row (Stop, Delete, Edit ~152dp) and the "Prescribe" button wrap to a second line. Because `SpaceBetween` places a single wrapped item at the start edge, the buttons jump to the start edge underneath the badge/title, violating the KDoc intent (*"actions on the end side"*).

### 3. Click ripple bleeds outside rounded corners on custom surfaces
* **File & Line**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt:944–954` and `1114–1124`.
* **Failing Scenario**: Both `PickerToggle` and `MedicationPickerTile` attach `.clickable(...)` on `Modifier` before `Surface(shape = RoundedCornerShape(14.dp))`. Because `.clickable` precedes the clipping shape, tap ripples render as rectangular boxes extending outside the 14dp rounded corners. Use `Surface(onClick = ...)` or apply `.clip(RoundedCornerShape(14.dp))` before `.clickable()`.

### 4. Duplicated disabled alpha constant
* **File & Line**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationCard.kt:313` (`DISABLED_CONTENT_ALPHA = 0.38f`) and `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt:883` (`DISABLED_TILE_ALPHA = 0.38f`).
* **Failing Scenario**: The same `0.38f` opacity value is redefined across two files instead of referencing a shared theme token or constant.

### 5. Potential double IME insets inside `ModalBottomSheet`
* **File & Line**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationSheet.kt:104`.
* **Failing Scenario**: `Column(modifier = Modifier.fillMaxWidth().imePadding())` is nested inside `ModalBottomSheet`. Material 3's `ModalBottomSheet` already consumes window insets by default via `BottomSheetDefaults.windowInsets`, causing excessive bottom clearance on certain Android versions.

### 6. Physical coordinate used in RTL-unaware `BringIntoViewRequester`
* **File & Line**: `app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/MedicationTapToAddSection.kt:999`.
* **Failing Scenario**: `resultsRequester.bringIntoView(Rect(0f, 0f, 1f, firstRowPx))` hardcodes `left = 0f`, which corresponds to the physical left edge rather than the start edge in RTL layouts.

---

## Checked and Compliant

* **Rotation & Pager Restoration**: `rememberSaveable(patient.id)` properly preserves `expandedMedId`, `showPast`, and `pickerState` in `MedsList` (L495–497). `nameFocusedFor` in `MedicationSheet.kt:77` prevents re-focusing name on rotation.
* **Touch Targets**: All clickable controls (`StopControl`, `DeleteIconButton`, `Edit`, `PickerToggle`, `FilterChip`, `MedicationPickerTile`) enforce `>= 48dp` hit targets via `minimumTouchTarget()`.
* **Colors & Hard-coded Strings**: Colors strictly consume `MaterialTheme.colorScheme` tokens; strings and plurals are properly extracted to `strings.xml`.
