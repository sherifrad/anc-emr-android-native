# Slice 10: Plan tab and attachments

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s10_plan.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Components as built: `ui/components/RecordTokens.kt`, `RecordSectionCard.kt` (`RecordSectionCard`, `SectionTextAction`), `StatusTag.kt` (`StatusWord`, `StatusTone`), `RecordListHeader.kt`. Read them; do not re-spec them.

**Scope:** the attachments section (shown on the Plan tab and on Overview: one component, both change), the add attachment dialog, the attachment viewer's chrome, and the Plan tab spacing. `DeliveryPlanningCard` is already done (slice 2) and is not touched.

**Owner direction for record lists (2026-11-15):** no box inside a box, no tinted box per field. The attachments section is a section of the record, like the delivery card, so it is a `RecordSectionCard` with plain rows split by hairlines (owner decision pending, see "Decisions" item 2), not one band card per file.

Branch per coordinator; `.agents/scripts/git_audit.sh` before work and before push; `.agents/pipeline/HANDOFF_AGY.md` updated in the same commit.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`. Also not touched: `storage/PatientAttachmentVault.kt`, `storage/BoundedBitmap.kt`, `storage/CameraCaptureHelper.kt`, `storage/PhotoQuality.kt`, `ui/InAppCameraDialog.kt` (slice 19), `ui/components/ZoomableImage.kt` (slice 15), `patienthub/PatientHubScreen.kt` (dialog wiring), `DeliveryPlanningCard.kt`, `OverviewTab.kt`.

**Level 3 (frontier-tier review plus an independent test review):**
- `ui/ClinicalAttachmentViewerDialog.kt`: chrome only. Must not change: the `loadedBitmap` `remember(attachment.id)` block, the `isIntegrityVerified` `remember(attachment.id)` block and its `verifyIntegrity(file, attachment.sha256Hash)` call, `onDelete` and `onDismiss` wiring, `enabled = canDelete` on delete, the `ZoomableImage` call (bitmap, contentDescription, `resetKey = attachment.id`, `fillMaxSize`), the 380 dp image height, the `Dialog` properties, every tag.
- `ui/PatientListScreen.kt`: move only. The private `ListFilterChip` is deleted and its seven call sites call `RecordFilterChip` with the same arguments. No other line changes.
- `ui/ClinicalFormat.kt`: only if the owner says yes to decision 5 (one character pair in `attachmentDisplayTitle`). Otherwise not touched.

**Visual only.** Same attachments, same filter state and counts, same order, same tap to open, same archived gate for Add (`patient.status != "archived"`), same dialog state machine (camera temp cleanup, `sent`, `chosenPreset`, `onSave` arguments, `onDismiss()` after save), same viewer delete rule.

## Decisions for the owner (this slice)

1. **Failed integrity wording.** When `isIntegrityVerified` is false (hash mismatch or missing file) the viewer says "Protected Medical Vault" with a lock, which reads as reassurance. Recommended: `attachment_vault_protected` becomes "Integrity not verified", drawn as `StatusWord(..., StatusTone.Moderate)`. If not approved: the value becomes "Protected medical vault" (sentence case) and is drawn as `StatusWord(..., StatusTone.Neutral)`. The string key and the branch stay.
2. **Rows, not band cards**, for files inside the section. Recommended: rows (this brief).
5. **Category names and untitled titles.** Recommended: `AttachmentCategories.localizedName` values to sentence case ("Ultrasound scan", "Laboratory investigation", "Prescription or report", "Clinical photo", "General medical attachment") in `storage/AttachmentSyncManager.kt`, and `attachmentDisplayTitle` uses `", "` instead of `" · "` (level 3, `ClinicalFormat.kt`), with `AttachmentDisplayTitleTest` updated by the test lane (lines 27, 30, 53 expect " · "). `AttachmentDeleteAdapter` uses `localizedName` for its undo label, so that text changes too. If not approved, both stay as they are and the rows show "Ultrasound Scan" (as in the mock).

Build items 1 and 5 only after the owner answers; everything else does not wait.

## Lessons (apply all)

1. First (outermost) `testTag` wins.
2. Size assertions allow 0.1 dp (`>= 47.9.dp`).
3. One `setContent` per test; `performScrollTo()` before asserting below the fold.
4. `strings.xml` values starting with `?` must be escaped.
5. The dialogs are separate windows: tests find their nodes with `useUnmergedTree = true` and the dialog tag as ancestor.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Action |
|---|---|
| `ui/components/RecordFilterChip.kt` | new: `RecordFilterChip`, moved verbatim from `PatientListScreen.kt` |
| `ui/components/FormFieldLabel.kt` | new: `FormFieldLabel` (also used by slice 11) |
| `ui/PatientListScreen.kt` (level 3) | delete private `ListFilterChip`, call `RecordFilterChip` (7 call sites, same arguments) |
| `ui/patienthub/tabs/PlanTab.kt` (53 lines) | gap 14 -> 12 dp |
| `ui/patienthub/components/AttachmentsSectionCard.kt` (294 lines) | rewrite drawing on `RecordSectionCard` (same signature) |
| `ui/AddAttachmentDialog.kt` (395 lines) | restyle (same signature) |
| `ui/ClinicalAttachmentViewerDialog.kt` (287 lines, level 3) | restyle chrome (same signature) |
| `app/src/main/res/values/strings.xml` | move hard-coded text into strings, change 7 values |
| (decision 5 only) `storage/AttachmentSyncManager.kt`, `ui/ClinicalFormat.kt` | copy only |

## 1. Shared pieces

### 1.1 `RecordFilterChip` (new `ui/components/RecordFilterChip.kt`)

Cut the body of `PatientListScreen.ListFilterChip` and paste it unchanged as:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordFilterChip(selected: Boolean, label: String, onClick: () -> Unit, tag: String)
```
(same `FilterChip`, `RecordType.Action` label SemiBold when selected else Medium, `maxLines = 1`, `softWrap = false`, `RecordShapes.Small`, surface / onSurface, selected primary / onPrimary, border outline / primary, `Modifier.testTag(tag)`). In `PatientListScreen.kt` replace `ListFilterChip(` with `RecordFilterChip(` and add the import. M3 `FilterChip` keeps its 48 dp touch area; do not shrink it.

### 1.2 `FormFieldLabel` (new `ui/components/FormFieldLabel.kt`)

```kotlin
/** Label above a group of choices or a field in a form: 12 sp, the family label colour inside a family, else onSurfaceVariant. */
@Composable
fun FormFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = RecordType.FieldLabel,
        color = LocalFieldFamily.current?.label ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
```

## 2. Strings

Add (all text that is hard-coded today moves here; English default, sentence case):
```xml
<string name="attachments_title">Documents and attachments</string>
<string name="attachment_add_action">Add</string>
<string name="attachment_add_description">Add attachment</string>
<string name="attachments_empty">No saved attachments or clinical images</string>
<string name="attachments_filter_empty">No attachments in this selected category</string>
<plurals name="attachments_summary">
    <item quantity="one">%1$d file, %2$s</item>
    <item quantity="other">%1$d files, %2$s</item>
</plurals>
<string name="attachment_filter_all">All (%1$d)</string>
<string name="attachment_filter_ultrasound">Ultrasound (%1$d)</string>
<string name="attachment_filter_labs">Labs (%1$d)</string>
<string name="attachment_filter_prescriptions">Prescriptions (%1$d)</string>
<string name="attachment_filter_photos">Clinical photos (%1$d)</string>
<string name="attachment_row_detail">%1$s, %2$s</string>
<string name="attachment_saved_on_phone">Saved on this phone, %1$s</string>
<string name="attachment_open">Open attachment</string>
<string name="add_attachment_title">Add attachment</string>
<string name="attachment_type_label">Type</string>
<string name="attachment_type_ultrasound">Ultrasound</string>
<string name="attachment_type_labs">Labs</string>
<string name="attachment_type_prescription">Prescription or report</string>
<string name="attachment_type_photo">Clinical photo</string>
<string name="attachment_type_other">Other</string>
<string name="attachment_title_label">Title or note (optional)</string>
<string name="attachment_title_placeholder">For example: growth scan, CBC report</string>
<string name="attachment_take_photo">Take photo</string>
<string name="attachment_take_photo_hint">Capture a scan or document</string>
<string name="attachment_gallery_hint">An existing file or photo</string>
<string name="attachment_save">Save to record</string>
```
Change values (keys unchanged; grep found no test asserting the old text):
- `attachment_gallery` "Gallery" -> "Choose from gallery" (used for both the first-choice button and the preview action).
- `attachment_delete_action` "Delete Attachment" -> "Delete attachment".
- `attachment_integrity_verified` "Verified Digital Integrity · SHA-256" -> "Integrity verified (SHA-256)".
- `attachment_vault_protected` -> decision 1 ("Integrity not verified", or "Protected medical vault").
- `attachment_size_format` "%1$s · WebP" -> "%1$s, WebP".
- `attachment_saved_at` "Saved At: %1$s" -> "Saved at %1$s".
- `photo_quality_small` / `_standard` / `_detailed` "Small - documents" -> "Small (documents)", "Standard (reports)", "Detailed (scans)".

Reuse: `cancel`, `close`, `attachment_retake`, `attachment_sha256_label`, `image_unavailable`, `photo_quality_label`.

## 3. `PlanTab` (same signature)

Only `verticalArrangement = Arrangement.spacedBy(12.dp)` (was 14). Delete the two `//` comments. Tag `planTabContent` stays on the column.

## 4. `AttachmentsSectionCard(patient, modifier, onAddAttachment, onViewAttachment)` (same signature)

```kotlin
RecordSectionCard(
    title = stringResource(R.string.attachments_title),
    family = FieldFamilies.patient(),
    modifier = modifier.testTag("attachmentsSectionCard"),
    action = if (patient.status != "archived") {
        { SectionTextAction(stringResource(R.string.attachment_add_action), stringResource(R.string.attachment_add_description), onAddAttachment, Modifier.testTag("addAttachmentBtn")) }
    } else null,
) {
    if (totalCount == 0) Text(stringResource(R.string.attachments_empty), style = RecordType.FieldValue, color = onSurfaceVariant)
    else {
        // keep: selectedFilter remember, filteredAttachments remember, totalBytes remember, context
        Text(pluralStringResource(R.plurals.attachments_summary, totalCount, totalCount, AttachmentSyncManager.formatFileSize(totalBytes)), style = RecordType.Secondary, color = onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {   // no vertical gap: each chip already has a 48 dp touch area
            RecordFilterChip(selectedFilter == "ALL", stringResource(R.string.attachment_filter_all, totalCount), { selectedFilter = "ALL" }, "attachmentFilter-ALL")
            RecordFilterChip(... ULTRASOUND, attachment_filter_ultrasound, count, "attachmentFilter-ULTRASOUND")
            RecordFilterChip(... LAB, attachment_filter_labs, count, "attachmentFilter-LAB")
            RecordFilterChip(... PRESCRIPTION, attachment_filter_prescriptions, count, "attachmentFilter-PRESCRIPTION")
            RecordFilterChip(... CLINICAL_PHOTO, attachment_filter_photos, count, "attachmentFilter-CLINICAL_PHOTO")
        }
        if (filteredAttachments.isEmpty()) Text(stringResource(R.string.attachments_filter_empty), style = RecordType.FieldValue, color = onSurfaceVariant)
        else Column { filteredAttachments.forEachIndexed { index, attachment -> AttachmentRow(patient.id, attachment, showDivider = index > 0, onClick = { onViewAttachment(attachment) }) } }
    }
}
```
The counts keep today's expressions (`patient.attachments.count { it.category == ... }`). Same five filters in the same order; `OTHER` still has no chip (as today).

### `AttachmentRow` (private, same file)

```kotlin
Column(Modifier.fillMaxWidth()) {
    if (showDivider) HorizontalDivider(color = colorScheme.outlineVariant)
    Row(
        Modifier.fillMaxWidth()
            .testTag("attachmentCard-${attachment.id}")
            .clickable(onClickLabel = stringResource(R.string.attachment_open), onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // thumbBitmap: today's produceState block, moved here unchanged (key1 = attachment.id, Dispatchers.IO, resolveAttachmentFile, BoundedBitmap.decodeFile(path, 2048))
        Box(Modifier.size(56.dp).clip(RecordShapes.Small).background(colorScheme.surfaceContainerHigh)) {
            thumbBitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(attachmentDisplayTitle(attachment.title, AttachmentCategories.localizedName(attachment.category), attachment.capturedAt), style = RecordType.KeyValue, color = colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.attachment_row_detail, AttachmentCategories.localizedName(attachment.category), AttachmentSyncManager.formatFileSize(attachment.fileSizeBytes)), style = RecordType.Secondary, color = colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.attachment_saved_on_phone, ltrIsolate(attachment.capturedAt.take(10))), style = RecordType.Secondary, color = colorScheme.onSurfaceVariant)
        }
    }
}
```
- The tag `attachmentCard-<id>` stays on the clickable row, so `DeleteUndoUiTest` clicks the same node.
- Removed: the tinted `Card` (`surfaceContainerLow`), the `Info` header icon, the filled "+ Add" `Button` with `Add` icon, the 11 sp teal summary, the `Surface` per row with `tonalElevation`, the `DarkGray` box with "-" (now `surfaceContainerHigh`, no glyph), the `secondaryContainer` category pill, the `VerifiedUser` icon (shown without any check), every `labelSmall` / `labelMedium`, `sp` import.

## 5. `AddAttachmentDialog(patient, initialCategory, onDismiss, onSave)` (same signature)

Keep every state, launcher, `DisposableEffect`, `InAppCameraDialog` block, preview decode and the save lambda exactly. Drawing:

- `Card(Modifier.fillMaxWidth(0.95f).padding(vertical = 16.dp).heightIn(max = (LocalConfiguration.current.screenHeightDp - 32).dp).testTag("addAttachmentDialog"), shape = RecordShapes.Large, colors = surface)`; no elevation argument (0). Inner `Column(verticalScroll, padding(20.dp), spacedBy(14.dp))` as today.
- Header `Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top)`: `Column(Modifier.weight(1f)) { Text(add_attachment_title, RecordType.SectionTitle, onSurface, Modifier.semantics { heading() }); Text(patient.displayName, RecordType.Action, onSurfaceVariant) }`; `IconButton(onDismiss, Modifier.minimumTouchTarget()) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel), tint = onSurfaceVariant) }`.
- Type: `Column(spacedBy(4.dp)) { FormFieldLabel(attachment_type_label); FlowRow(spacedBy(6.dp)) { FilterChip x5 } }`. Labels from the five `attachment_type_*` strings, same category order and selection. `FilterChip` stays the M3 default (forms keep it until the editor slices); only remove `style = labelSmall` from the label `Text` (default 13 sp).
- Photo size: same pattern with `FormFieldLabel(photo_quality_label)`; tags `preset_small`, `preset_standard`, `preset_detailed` stay on the chips; remove `labelSmall`.
- Title field: `OutlinedTextField` as today with `label = { Text(stringResource(R.string.attachment_title_label)) }`, `placeholder = { Text(stringResource(R.string.attachment_title_placeholder)) }`.
- No photo yet: `Column(spacedBy(8.dp))` of two `OutlinedButton`s, each `Modifier.fillMaxWidth().heightIn(min = 64.dp)`, `shape = RecordShapes.Small`, `contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)`, content `Column(Modifier.fillMaxWidth()) { Text(title, RecordType.Action, primary); Text(hint, RecordType.Secondary, onSurfaceVariant) }`:
  - `testTag("captureCameraBtn")`, `onClick = { showCamera = true }`, `attachment_take_photo` / `attachment_take_photo_hint`;
  - `testTag("pickGalleryBtn")`, `onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ImageOnly)) }`, `attachment_gallery` / `attachment_gallery_hint`.
- Photo chosen: the preview `Box(Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp).clip(RecordShapes.Small).border(1.dp, outline, RecordShapes.Small))` with the same `Image` (contentDescription "Preview" stays); the overlay `Row` is removed. Under the box: `Row(spacedBy(4.dp)) { SectionTextAction(attachment_retake, attachment_retake, { showCamera = true }); SectionTextAction(attachment_gallery, attachment_gallery, { photoPickerLauncher.launch(...) }) }`.
- Buttons: replace the `Row` of `OutlinedButton` / `Button` with
```kotlin
SheetCancelSaveRow(
    enabled = true, saving = false, saveEnabled = selectedUri != null,
    savingLabel = stringResource(R.string.attachment_save),
    onCancel = onDismiss,
    onSave = { val uri = selectedUri ?: return@SheetCancelSaveRow; sent = true; onSave(uri, selectedCategory, titleText.trim().ifEmpty { null }, preset); onDismiss() },
    cancelTag = "cancelAttachmentBtn", saveTag = "saveAttachmentConfirmBtn", spinnerTag = "attachmentSaveSpinner",
)
```
  (`SheetCancelSaveRow` is `internal` in `patienthub/components/EditorSheetParts.kt`, same module; import it. Slice 11 restyles it, and this dialog follows.)
- Removed: radius 20 and elevation 8, the 11 sp labels, the colon in "Attachment Type:", the two 120 dp tinted `Surface` tiles and their 36 dp icons, the black overlay pills, `Color` import if unused.

## 6. `ClinicalAttachmentViewerDialog` (level 3, same signature)

- `Card(... .testTag("clinicalAttachmentViewerDialog"), shape = RecordShapes.Large, colors = surface)`; no elevation argument. Inner column as today (`padding(16.dp)`, `spacedBy(12.dp)`).
- Header `Row(Modifier.fillMaxWidth(), verticalAlignment = CenterVertically)`:
  - `Column(Modifier.weight(1f)) { Text(patient.displayName, RecordType.CardName, onSurface); Text(line, RecordType.Secondary, onSurfaceVariant) }` with `line = AttachmentCategories.localizedName(attachment.category) + (attachment.title?.trim()?.takeIf(String::isNotEmpty)?.let { ", $it" } ?: "")` (was `" · $it"`).
  - Delete `IconButton` unchanged (onClick, `enabled = canDelete`, error tint, content description) apart from `Modifier.minimumTouchTarget().testTag("deleteAttachmentBtn")`. Close `IconButton` the same with `dismissAttachmentBtn`.
  - Removed: the 40 dp `CircleShape` `Surface` with `PhotoLibrary` icon, the inner `Row` around the name.
- Image box: `.height(380.dp).clip(RecordShapes.Small).background(Color.Black).border(1.dp, outlineVariant, RecordShapes.Small)`. Unavailable branch: only `Text(stringResource(R.string.image_unavailable), style = RecordType.FieldValue, color = Color.LightGray, modifier = Modifier.testTag("image_unavailable"))`; the grey `PhotoLibrary` icon goes.
- Meta: replace the tinted `Surface` with `Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp))`:
  - `StatusWord(if (isIntegrityVerified) attachment_integrity_verified else attachment_vault_protected, tone = if (isIntegrityVerified) StatusTone.Neutral else <decision 1: Moderate, else Neutral>)`.
  - `Text(stringResource(R.string.attachment_size_format, formatFileSize(...)), RecordType.Secondary, onSurfaceVariant)`.
  - `Text(stringResource(R.string.attachment_saved_at, ltrIsolate(attachment.capturedAt)), RecordType.Secondary, onSurfaceVariant)`.
  - `Text(stringResource(R.string.attachment_sha256_label, attachment.sha256Hash), style = RecordType.Tag.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal), color = onSurfaceVariant)`: no `maxLines`, so the 64 characters wrap (was 9 sp, one clipped line).
  - Removed: `VerifiedUser` / `Lock` icons, the `SpaceBetween` row, every `labelSmall`, `sp` import.

## Acceptance checklist

1. Tags unchanged: `planTabContent`, `attachmentsSectionCard`, `addAttachmentBtn` (not archived), `attachmentCard-<id>`, `addAttachmentDialog`, `captureCameraBtn`, `pickGalleryBtn`, `preset_small`, `preset_standard`, `preset_detailed`, `saveAttachmentConfirmBtn`, `clinicalAttachmentViewerDialog`, `deleteAttachmentBtn`, `dismissAttachmentBtn`, `image_unavailable`, every `riskFilter-*` on the patient list. New: `attachmentFilter-<CATEGORY>`, `cancelAttachmentBtn`.
2. `git diff` of `ClinicalAttachmentViewerDialog.kt` shows no change inside the two `remember(attachment.id)` blocks, the `ZoomableImage` call, `onDelete`, `enabled = canDelete`.
3. `git diff` of `PatientListScreen.kt` only removes `ListFilterChip` and renames its seven calls (plus imports).
4. No text under 12 sp in the four screens (no `labelSmall`, `labelMedium`, no `fontSize = 9.sp`); no "·" or " - " separators in these screens' strings.
5. No `Card`, filled `Surface`, `tonalElevation` or `LabeledValue` inside the attachments body; rows split by `HorizontalDivider`.
6. Radii only 10 and 16 in the four files (M3 `FilterChip` default inside the add dialog is the known exception until the editor slices).
7. Every tappable >= 47.9 dp: `addAttachmentBtn`, each `attachmentCard-<id>` (72 dp), each `attachmentFilter-*`, `captureCameraBtn`, `pickGalleryBtn` (64 dp), Retake, Choose from gallery, Cancel, Save, close, `deleteAttachmentBtn`, `dismissAttachmentBtn`.
8. 360 dp, font scale 1.3: row titles wrap to two lines then ellipsis; chips wrap; the dialog scrolls and its buttons do not clip; the hash wraps inside the viewer.
9. RTL: section title right, Add left; thumbnail right of the text; dates isolated.
10. Dark: section, rows, chips and viewer meta in dark tokens (mock section 4).
11. Archived patient: no Add; viewer delete disabled (as today).

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected. Decision 5 only: `AttachmentDisplayTitleTest` (lines 27, 30, 53) expects " · "; it changes to ", " if the owner says yes.

### Must pass unchanged

`DeleteUndoUiTest` (all `attachment_*` cases), `OverviewTabSectionsTest`, `AncWorkflowTest`, `DeliveryPlanTabsEditGateTest`, `DeliveryPlanningCardEditGateTest`, `DeliveryCardRedesignTest`, `PatientHubNavigation`, `OwnerFixesUiTest` and `PatientListRedesign` tests (chip move), `VaultIntegrationTest`, unit tests.

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/AttachmentsRedesignTest.kt`

Fixtures (exact constructors; values from `DeleteUndoUiTest`):
```kotlin
private fun attachment(n: Int, title: String? = "Scan $n", category: String = "ULTRASOUND") = DemoAttachment(
    id = "ui-att-$n", patientId = "p-att", fileName = "att_ui_$n.webp",
    filePath = "patient_attachments/p-att/att_ui_$n.webp", category = category,
    fileSizeBytes = 1024, sha256Hash = "00", title = title, capturedAt = "2026-03-0$n", revision = 1,
)
private fun patient(vararg atts: DemoAttachment, status: String = "active") = DemoPatient(
    id = "p-att", caseId = "A-1", displayName = "Mona Ragab", dateOfBirth = null, phone = null,
    visits = emptyList(), attachments = atts.toList(), status = status,
)
```
(`DemoPatient` is in `model/DemoModels.kt`: `status: String? = null`, `attachments: List<DemoAttachment> = emptyList()`. `DemoAttachment` is in `model/ClinicalModels.kt`; do not edit either.)

Section, render once: `compose.setContent { AncTheme { AttachmentsSectionCard(patient = p, onAddAttachment = {}, onViewAttachment = { opened = it.id }) } }`.
- `patient(attachment(1), attachment(2))`: "Documents and attachments" exists; "2 files, 2.0 KB" exists; `attachmentFilter-ALL` displayed and selected; "Ultrasound Scan, 1.0 KB" count 2 (read the category through `AttachmentCategories.localizedName("ULTRASOUND")` so decision 5 does not break it); `hasText("Saved on this phone, ", substring = true)` count 2; click `attachmentCard-ui-att-1` sets `opened == "ui-att-1"`.
- filter: click `attachmentFilter-LAB`; "No attachments in this selected category" exists; `attachmentCard-ui-att-1` does not exist; click `attachmentFilter-ALL`; it returns.
- `attachmentCard-ui-att-1` height >= 71.9 dp; `addAttachmentBtn` height and width >= 47.9 dp; each `attachmentFilter-*` height >= 47.9 dp.
- empty patient: "No saved attachments or clinical images"; `addAttachmentBtn` exists.
- archived: `patient(status = "archived")`: `addAttachmentBtn` does not exist.
- no node text contains "·" or "Documents & Attachments".
- font sizes: the summary and row lines are 14 sp (`GetTextLayoutResult`, `layoutInput.style.fontSize`).

Add dialog, render once: `AddAttachmentDialog(patient = patient(), onDismiss = {}, onSave = { _, _, _, _ -> })`.
- "Add attachment", "Mona Ragab", "Type", "Photo size" exist; `preset_detailed` selected by default for ultrasound (today's `defaultFor`); `captureCameraBtn` and `pickGalleryBtn` height >= 63.9 dp; `saveAttachmentConfirmBtn` not enabled; `cancelAttachmentBtn` height >= 47.9 dp; no text node under 12 sp (walk `onAllNodes(hasText(""), substring)` text layouts).

Viewer, render once: `ClinicalAttachmentViewerDialog(patient = patient(attachment(1)), attachment = attachment(1), canDelete = true, onDismiss = {}, onDelete = { deleted = true })`.
- `image_unavailable` displayed (no file on disk); "Mona Ragab" exists; "Ultrasound Scan, Scan 1" exists (or the decision 5 text); the not-verified word from `attachment_vault_protected` exists; "1.0 KB, WebP" exists; `hasText("SHA-256: 00", substring = true)` exists and its font size is 12 sp.
- `deleteAttachmentBtn` and `dismissAttachmentBtn` height and width >= 47.9 dp; click delete sets `deleted`.
- `canDelete = false` (second test): `deleteAttachmentBtn` not enabled.

## Risk

Medium. One level-3 chrome edit (viewer) and one level-3 move (`PatientListScreen.kt`): frontier-tier review and an independent test review. The attachments section also draws on Overview: check it there at 360 dp. Note for the owner, not for this slice: the Plan tab shows the same two cards as the bottom of Overview (content question, no change here).
