Created the Slice 15 mock and builder brief for the ultrasound editor and photo viewer without editing any production code or running Gradle, the emulator, any device, or git write commands. Verified by code inspection and WCAG contrast calculation (static HTML mock was not rendered in a live browser).

### What the mock shows
[mock_s15_ultrasound_editor.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/mocks/mock_s15_ultrasound_editor.html) presents `360px` BEFORE (faithful to current [UltrasoundEditor.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundEditor.kt), [ZoomableImage.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/components/ZoomableImage.kt), and [UltrasoundPhotoViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundPhotoViewerDialog.kt)) and AFTER frames using the Obstetric family (`FieldObstetricBg` / `FieldObstetricLabel`), `RecordSectionCard`, `FormFieldLabel`, `RecordFilterChip`, `SummaryRow`, `PlainField`, and `SheetCancelSaveRow`:

1. **Live summary, timing, and guided steps** (`Mona Ragab`, `27+2 wks`, `EDD 11/02/2027`):
   - **Before**: Numbered Title Case headers, `27w 2d` inside a bright blue `primaryContainer` box, tight floating-label `GA weeks (0–45)` / `GA days (0–6)` notches, `13 sp` collapsed rows with downward chevrons, and M3 `8 dp` rose chips.
   - **After (initial and after Fill normal scan)**: Calm `FieldObstetricBg` `ultrasoundLiveSummary` card at top with `16 sp` summary sentence, `48 dp` `RecordShapes.Small` `Fill normal scan` button, and quiet `TextButton` `Clear findings`; `scanCalculatedGaCard` displaying `27+2 wks` (`shortWeeksText`) plus semantic amber `redatingHint` when triggered; `ultrasoundFindingsCard` (`Fetus, placenta and amniotic fluid`) showing `"0 of 4 set"` / `"3 of 4 set"`, `48 dp` `RecordFilterChip`s on the open step (`findingOpen-*`), and clean `SummaryRow`s without downward chevrons on closed rows (`findingRow-*`).
2. **Biometry entry and clinical impression** (in-repo `DemoFixtures` scan `...0102`, labelled):
   - **Before**: `"3. Fetal Biometry"` with `11 sp` subtitle, `"▲ Collapse"` triangle toggle, `"Estimated fetal weight (EFW)"` truncated in half-width notch, `"4. Clinical Impression"`, `11 sp` AES-256 Lock footer, and uneven pill `Cancel` / `Save`.
   - **After**: Clean `"Fetal biometry"` header with `14 sp` summary and `"Collapse"` / `"Expand"` `SectionTextAction`; 6 external-label `10 dp` numeric fields (`fetalHeartRate`, `efw`, `biometryBpd`, `biometryHc`, `biometryAc`, `biometryFl`) that stack vertically above `1.3f` font scale; single-label `"Impression"` card with `10 dp` `"From my choices"` button; equal-width `48 dp` `SheetCancelSaveRow` and no Lock footer.
3. **Photo attach, linked visit, and photo viewer** (`Mona Ragab` / `110/70` and `DemoFixtures` scan `...0102`):
   - **Before**: `11 sp` pink `Photo 1` chip, side-by-side `"Take Photo"` / `"Choose Image"` buttons with `softWrap = false`, `"More: photos and visit link"` with downward chevron, `20 dp` elevated viewer dialog with decorative camera icon, `11 sp` `8 dp` `0.9f`-alpha `1x` zoom reset chip, `8 dp` clickable dots, and `11 sp` `FactBadge` pills.
   - **After**: `OutlinedCard` toggle (`56 dp`, no chevron), `16 sp` `attachmentItem-*` row with `48 dp` `removePhoto-*` `Close` icon, stacked full-width `48 dp` `"Take photo"` and `"Choose from gallery"` buttons, clean `linkedVisitCard` (`BP 110/70, weight ...`), solid-surface `14 sp` `10 dp` `1x` zoom reset button (`zoomResetButton`), and scrollable `16 dp` `UltrasoundPhotoViewerDialog` with non-clickable indicator dots and `PlainField` biometry summary.
4. **Dark theme with Arabic patient name** (<bdi lang="ar">منى رجب</bdi>), **computed WCAG 2.1 contrast table** (every text pair >= `5.53:1`, borders >= `3.89:1`), and **Compose mapping**.

### Decisions for the owner to confirm

1. **Card 0 (`ultrasoundLiveSummary`) at the top in `FieldObstetricBg` with demoted Clear button**:
   Keep `ultrasoundLiveSummary` at the top in calm `FieldObstetricBg` (`#E3F0EE` / `#1D3836`) with `16 sp` summary text, `Fill normal scan` as a `48 dp` white-surface outlined button, and `Clear findings` as a `48 dp` `TextButton`.
   *Recommendation*: **Accept** so 1-tap normal scan entry stays immediate while separating Fill from Clear.
2. **Calculated GA format (`27+2 wks` via `shortWeeksText`) and amber `redatingHint`**:
   Switch `scanCalculatedGaCard` and `applyScanGa` from `27w 2d` (`displayText`) to `27+2 wks` (`shortWeeksText`), restyle the GA tile in `FieldObstetricBg`, and show `redatingHint` in semantic amber (`StatusModerate*`) when `redatingAdvice` triggers.
   *Recommendation*: **Accept** to match Owner Decision 9 across the app (zero tests assert `27w 2d` in `UltrasoundEditor`).
3. **Reuse Slice 14's backward-compatible `SummaryRow(..., useRecordStyle = true)` and drop downward chevron on `ultrasoundMoreToggle`**:
   Apply `useRecordStyle = true` to `findingRow-*` (omitting `"Edit"`/`"Choose"` above `1.3f` font scale to prevent label squeeze) and style `ultrasoundMoreToggle` as an `OutlinedCard` with `"Open"` / `"Close"` (`RecordType.Action`). If Slice 14 has not landed yet, add those exact `useRecordStyle = false` default parameters to `SummaryRow` in [DesignSystem.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/DesignSystem.kt) so `LabEditor` does not shift.
   *Recommendation*: **Accept**.
4. **Include `UltrasoundPhotoViewerDialog.kt` with `UltrasoundEditor.kt` and `ZoomableImage.kt`, and stack photo source buttons full-width**:
   Stack `"Take photo"` and `"Choose from gallery"` full-width (`48 dp`), raise `attachmentItem-*` to `16 sp`, update `ZoomableImage`'s `1x` reset chip (`10 dp`, solid `surface`, `outline`, `14 sp`), and restyle `UltrasoundPhotoViewerDialog` (`16 dp` scrollable card, non-clickable indicator dots, `PlainField` biometry grid).
   *Recommendation*: **Accept** to eliminate `360 dp` clipping, `11 sp` text, and `8 dp` clickable dots in one slice.
5. **Drop `11 sp` vault Lock footer, normalize numbered card headings, and use inline `SheetCancelSaveRow`**:
   Remove the AES-256 Lock row, replace `"1. "`–`"4. "` and `"▲"`/`"▼"` strings with sentence-case `strings.xml` entries, and place `SheetCancelSaveRow` at the bottom of the scrolling column (rather than pinned) to keep scroll height available when the soft keyboard opens on `360x640` viewports.
   *Recommendation*: **Accept**.

### Output file paths
- [mock_s15_ultrasound_editor.html](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/mocks/mock_s15_ultrasound_editor.html)
- [slice_15_ultrasound_editor.md](file:///Users/sherifgamalradwan/anc-emr-android-native/docs/designs/redesign/slice_15_ultrasound_editor.md)
