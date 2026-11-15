# Meds defects (owner reports) - spec from the Opus read-only investigation, 2026-11-15
Owner: (1) "the medication section refreshes with every click"; (2) "adding a new medication: a long list of medications takes a lot of space". Keep today's Meds layout and look. Room schema frozen; protected files (schemas, RecordsDatabase, RecordEntities, RecordsDao, RecordsRepository, UndoCoordinator, ClinicalModels, *Generator*) must NOT be edited. Do NOT edit MedicationTiles.kt, MedicationSheet.kt, MedicationForm.kt.
Files to edit: ui/patienthub/tabs/MedsTab.kt, ui/patienthub/components/MedicationTapToAddSection.kt, ui/patienthub/components/MedicationCard.kt, new ui/patienthub/model/MedicationListOrder.kt, res/values/strings.xml (append only).

## Bug 1: what the doctor sees as "refresh" (no real remount)
Starting the editor makes the draft dirty, canModifyData false, so entryEnabled false, so every control in the tab dims to 0.38 alpha under the sheet; on save the sheet vanishes and everything un-dims in one frame (the flash); the store sorts newest first (RecordsRepository sort) so a new drug is inserted at LazyColumn index 1 under the active header and everything below, including the open picker, jumps down about 100 dp; the undo bar appearing/disappearing resizes the viewport.
Fix (a) no visual dimming while the sheet covers the tab (keep `enabled` semantics exactly, only the alpha changes):
- MedsTab.kt: after `val entryEnabled = canEdit && actionsAllowed && medicationDraft == null && !savePhase.blocksEditing()` add `val entryDimmed = !entryEnabled && medicationDraft == null`; pass `entryDimmed` to MedsList and from there to MedicationTapToAddSection(dimmed = entryDimmed) and to the cards (StopControl etc. colour by `actionsDimmed: Boolean = !actionsEnabled`).
- MedicationTapToAddSection: new parameter `dimmed: Boolean = !enabled` passed to PickerToggle, PickerPanel and MedicationPickerTile; alpha uses `if (dimmed) MEDICATION_DISABLED_ALPHA else 1f` (tile: `if (dimmed && !isActive)`). `enabled` still disables clicks.
- MedicationCard StopControl: `actionsDimmed` parameter defaulting to `!actionsEnabled`, colour decided by it.
- Same file: stop the collector restart: `val customChoicesFlow = remember(catalogRepo) { catalogRepo.customChoices(CustomChoiceList.MEDICATION_NAME) }; val customChoices by customChoicesFlow.collectAsState()` (check the real return type; keep initial emptyList if collectAsState needs it).
Fix (b) stable order: new pure file ui/patienthub/model/MedicationListOrder.kt:
```kotlin
package com.sherifradwan.ancnative.ui.patienthub.model
import com.sherifradwan.ancnative.model.DemoMedicationRecord
/** Rows already shown keep their order; rows new since [shownIds] join the end; removed rows drop out. */
fun stableMedicationOrder(shownIds: List<String>, meds: List<DemoMedicationRecord>): List<DemoMedicationRecord> {
    val byId = meds.associateBy { it.id }
    val kept = shownIds.mapNotNull { byId[it] }
    val keptIds = kept.mapTo(HashSet()) { it.id }
    return kept + meds.filter { it.id !in keptIds }
}
```
In MedsList replace activeMeds/pastMeds with: `var shownOrder by rememberSaveable(patient.id) { mutableStateOf(emptyList<String>()) }`, `val ordered = remember(patient.medications, shownOrder) { stableMedicationOrder(shownOrder, patient.medications) }`, `LaunchedEffect(ordered) { shownOrder = ordered.map { it.id } }`, then active/past filtered from `ordered`. Plus `val listState = rememberLazyListState()` passed to the LazyColumn (`state = listState`) and a LaunchedEffect(activeMeds) that, when rows were added and `listState.firstVisibleItemIndex == 0`, awaits a frame and `listState.scrollBy(totalHeightOfAddedRows incl. spacing)` so the open picker does not move (use layoutInfo.visibleItemsInfo sizes; `mainAxisItemSpacing` exists in Compose foundation 1.5+: check the project's version and fall back to the item spacing constant if not). Rows new since the last frame effectively join the end of the active list; if the existing code keeps its own list state, reuse it.

## Bug 2: long picker list
Today PickerPanel shows search + chips + a 304 dp inner-scrolling Column of two-line tiles (whole catalog). Replace with: empty search: a FlowRow of at most 8 compact AssistChips (name only, one tap opens the prefilled sheet exactly as a tile does now) plus a "Show all (N)" TextButton when more than 8; search typed: at most 5 full tile rows (existing MedicationPickerTile, names starting with the query first), plus "Show all (N)" when more; no nested scroll; showAll is kept in saved state and reset when the query or source chip changes. Keep tags `medTile-<id>`, `medTileCheck-<id>`, `medTilesGrid`; new tag `medPickerShowAll`; `medTileMeta-<id>` exists only in search rows. An already-active drug chip is disabled and shows a check (as tiles do: "already on today's list" semantics). Code:
```kotlin
internal const val PICKER_CHIP_LIMIT = 8
internal const val PICKER_ROW_LIMIT = 5
internal fun pickerVisibleTiles(filtered: List<MedicationTile>, query: String, showAll: Boolean, limit: Int): List<MedicationTile> {
    val q = medicationNameKey(query)
    val ranked = if (q.isEmpty()) filtered else filtered.sortedBy { if (medicationNameKey(it.name).startsWith(q)) 0 else 1 }
    return if (showAll) ranked else ranked.take(limit)
}
```
(check `medicationNameKey` and MedicationTile names in the repo and import them; remove PICKER_LIST_MAX_HEIGHT and the unused heightIn/verticalScroll/rememberScrollState imports.) MedicationPickerState gains `showAll: Boolean = false` with the saver storing 4 values. New string: `<plurals name="medication_picker_show_all"><item quantity="one">Show all (%d)</item><item quantity="other">Show all (%d)</item></plurals>` and use an existing string for "Show less" if one exists (else add `action_collapse`="Show less").
MedicationQuickChip: AssistChip(onClick, enabled = enabled && !isActive, label = Text(tile.name, maxLines=1, ellipsis), leadingIcon check when active (tag medTileCheck-<id>), modifier minimumTouchTarget + testTag("medTile-<id>") + semantics stateDescription alreadyOnList when active).
Chip colours/shape must follow the existing tune rules (10 dp small radius via RecordShapes.Small if easy).
