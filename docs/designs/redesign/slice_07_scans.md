# Slice 7: Scans tab (band cards)

Builder brief, self-contained. Look: `docs/designs/redesign/mocks/mock_s07_scans.html`. Rules: `docs/designs/redesign/PLAN.md` sections 0, 2, 6, 7 and 8. Depends on slice 6: `RecordBandCard` (`ui/components/RecordBandCard.kt`), `BandChip` and `StatusWord` (`StatusTag.kt`), `PlainField` (`MetricCell.kt`), the `no_records` and `unit_bpm` strings. Read those as built.

**Owner direction for record lists (2026-11-15):** one container per record (`RecordBandCard`): a family-colour header band holding filled status-word chips (`BandChip`), the title and one-tap actions; a quiet white body with plain label-over-value text (`PlainField`). No tinted box per field, no box inside a box.

Branch per coordinator; `.agents/scripts/git_audit.sh` before work and before push; `.agents/pipeline/HANDOFF_AGY.md` updated in the same commit.

Never touch: `app/schemas/**`, `storage/RecordsDatabase.kt`, `storage/RecordEntities.kt`, `storage/RecordsDao.kt`, `storage/RecordsRepository.kt`, `state/UndoCoordinator.kt`, `model/ClinicalModels.kt`, `ui/report/PatientPrintHtmlGenerator.kt`. Also not touched: `ui/patienthub/model/BiometryUi.kt`, `UltrasoundEditor.kt` (slice 15), `UltrasoundPhotoViewerDialog.kt` (opened unchanged).

**Level 3:** none.

**Visual only.** Same values and show or hide rules, same events (`StartUltrasoundEditor`, `RequestDelete`, `onViewPhotos(scan, idx)`), same gates.

## Owner decisions that apply

- 9: GA reads "20+0 wks" (`shortWeeksText`).

## Lessons (apply all)

1. First (outermost) `testTag` wins.
2. Size assertions allow 0.1 dp (`>= 47.9.dp`).
3. `ltrIsolate` values: match with `substring = true`.
4. One `setContent` per test; `performScrollTo()` before asserting below the fold.

## Files (base `app/src/main/java/com/sherifradwan/ancnative/`)

| File | Action |
|---|---|
| `ui/patienthub/tabs/ScansTab.kt` (233 lines) | restyle to band cards |
| `ui/patienthub/components/BiometryGrid.kt` (144 lines) | rewrite drawing on `PlainField` (same signature) |
| `app/src/main/res/values/strings.xml` | add strings |

## Strings (add)

```xml
<string name="scans_tab_title">Ultrasound scans (%1$d)</string>
<string name="record_scan_action">Record scan</string>
<string name="biometry_bpd">BPD</string>
<string name="biometry_hc">HC</string>
<string name="biometry_ac">AC</string>
<string name="biometry_fl">FL</string>
<string name="biometry_efw">EFW</string>
<string name="biometry_fhr">FHR</string>
<string name="scan_presentation_label">Presentation</string>
<string name="scan_placenta">Placenta</string>
<string name="scan_amniotic_fluid">Amniotic fluid</string>
<string name="scan_photo_n">Photo %1$d</string>
<string name="unit_mm">%1$s mm</string>
```
Reuse `no_records`, `unit_bpm`, `action_edit`, `edit_ultrasound`, `scan_impression`, `scan_attachments`, `R.plurals.photos_count`. Keep `scan_presentation` ("Presentation: %1$s").

## 1. `ScansTab` (same signature)

- `LazyColumn(modifier.fillMaxSize().testTag("scansTabContent"), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(12.dp))`.
- Header and empty state exactly as slice 6, with `scans_tab_title`, `record_scan_action`, tag `recordNewScanBtn`, the unchanged `StartUltrasoundEditor(scanId = null, ...)` event.
- Items:
```kotlin
RecordBandCard(
    title = scan.date,
    family = FieldFamilies.obstetric(),
    modifier = Modifier.testTag("ultrasound-${scan.id}"),
    chips = if (gaText != null || scan.imageUris.isNotEmpty()) {
        {
            gaText?.let { BandChip(ltrIsolate(it), tone = null, modifier = Modifier.testTag("gestationalAgeBadge")) }
            if (scan.imageUris.isNotEmpty()) BandChip(pluralStringResource(R.plurals.photos_count, scan.imageUris.size, scan.imageUris.size), tone = null)
        }
    } else null,
    actions = if (canEdit) {
        {
            DeleteIconButton(type = DeletableType.ULTRASOUND, record = scan, onClick = { onEvent(AncEvent.RequestDelete(patient.id, DeletableType.ULTRASOUND, scan.id)) })
            SectionTextAction(stringResource(R.string.action_edit), stringResource(R.string.edit_ultrasound), { /* unchanged StartUltrasoundEditor(scanId = scan.id, newScanId = scan.id, ...) */ }, Modifier.testTag("editUltrasound-${scan.id}"))
        }
    } else null,
) {
    BiometryGrid(values = biometry)
    if (!scan.impression.isNullOrBlank()) PlainField(stringResource(R.string.scan_impression), scan.impression)
    if (scan.imageUris.isNotEmpty()) ScanPhotos(scan, onViewPhotos)
}
```
  `gaText = gaAtScan?.shortWeeksText`. The `gestationalAgeBadge` tag keeps `DatingGaMarkerSurfacesTest.scanGaBadgesNeverShowTheMarkerEvenForADiscordantPatient` meaningful.
- `ScanPhotos` (private): `Text(stringResource(R.string.scan_attachments) + ", " + pluralStringResource(R.plurals.photos_count, n, n), RecordType.Secondary, onSurfaceVariant)`, then `FlowRow(spacedBy(8.dp), verticalArrangement = spacedBy(4.dp))` of `OutlinedButton(onClick = { onViewPhotos(scan, idx) }, shape = RecordShapes.Small, border = BorderStroke(1.dp, outline), modifier = Modifier.heightIn(min = 48.dp).testTag("wsScanPhotoChip-${scan.id}-$idx")) { Text(stringResource(R.string.scan_photo_n, idx + 1), RecordType.Action, primary) }`. Photos are actions, so they keep an outline.
- Removed: the tinted `Card`, the bottom action row, the secondary-container photo chips, `FactRow`, the `Add` icon.

## 2. `BiometryGrid(values, modifier)` (same signature)

- Keep the early return (`isEmpty && !hasAnyData`).
- One list of `(label, text, emphasis)` from the non-blank values, in today's order: BPD `unit_mm`, HC `unit_mm`, AC `unit_mm`, FL `unit_mm`, EFW as stored, FHR `unit_bpm` (emphasis true); then Presentation, Placenta, Amniotic fluid (emphasis false).
- `chunked(3)`; each chunk a `Row(Modifier.fillMaxWidth(), horizontalArrangement = spacedBy(12.dp))` of `PlainField(label, ltrIsolate(text) for measurements / text for words, Modifier.weight(1f), emphasis)`, padded with `Spacer(Modifier.weight(1f))`. Rows in a `Column(spacedBy(10.dp))`.
- The Presentation field carries `Modifier.testTag("scanPresentation")` (used by `PatientHubDesignTest`).
- Removed: the bordered surface, the "Ultrasound Biometry" caption, the highlighted EFW pill, every pill.

## Acceptance checklist

1. Tags: `scansTabContent`, `recordNewScanBtn`, `ultrasound-<id>`, `editUltrasound-<id>`, `delete-ultrasound-<id>`, `scanPresentation`, `wsScanPhotoChip-<id>-<n>`, `gestationalAgeBadge` when GA known.
2. One `RecordBandCard` per scan (teal band); no `LabeledValue`, filled `Surface` or card inside its body (grep). GA and photo count are chips in the band.
3. "49 mm" style for BPD, HC, AC, FL; "148 bpm" for FHR; EFW as stored. Blank values hidden.
4. GA text is `shortWeeksText`; no marker in scans.
5. Photo buttons >= 47.9 dp; tapping one calls `onViewPhotos(scan, idx)`.
6. 360 dp, font scale 1.3: three columns do not overlap; "Oligohydramnios" wraps, not cut.
7. RTL: heading right, actions left; values isolated.
8. Dark theme renders.

## Tests (written by a different lane from this spec)

### Existing tests that need edits

None expected. `PatientHubDesignTest.patientHub_scansTabShowsTheFetalPresentation` scrolls to `scanPresentation`: kept. `PrintLayoutTest` and `PrintSectionTitlesTest` test print HTML.

### Must pass unchanged

`PatientHubDesignTest`, `DatingGaMarkerSurfacesTest`, `DeleteUndoUiTest` (ultrasound kind), `AncWorkflowTest` (`recordNewScanBtn`), `UltrasoundGuidedFlowTest`, `PatientHubNavigation`, unit tests.

### New test: `app/src/androidTest/java/com/sherifradwan/ancnative/ScansTabRedesignTest.kt`

Fixtures (exact constructors; values from `DemoFixtures` scan `...0102`):
```kotlin
private fun scan(id: String = "s1", images: List<String> = emptyList(), presentation: String? = "Cephalic", fluid: String? = "Normal") = DemoUltrasoundScan(
    id = id, patientId = "p-scans", date = "2026-05-30",
    gestationalAgeWeeks = 20, gestationalAgeDays = 0, fetalNumber = "Singleton",
    presentation = presentation, placentalSite = "Anterior", amnioticFluid = fluid, fetalHeartRate = "148",
    biometryBpd = "49", biometryHc = "178", biometryAc = "155", biometryFl = "34", estimatedFetalWeight = "380",
    impression = "Normal fetal anatomy and morphology survey; no anomalies detected",
    imageUris = images,
)
private fun patient(vararg scans: DemoUltrasoundScan) = DemoPatient(
    id = "p-scans", caseId = "S-1", displayName = "Mona Ragab", dateOfBirth = null, phone = null, visits = emptyList(),
    dating = DemoPatientDating(patientId = "p-scans", lmpDate = "2026-01-10", ultrasoundEdd = null, agreedEdd = null, datingMethod = "LMP", notes = null),
    ultrasoundScans = scans.toList(),
)
```
Render once: `compose.setContent { AncTheme { ScansTab(patient = p, canEdit = true, onEvent = {}, onViewPhotos = { s, i -> opened = s.id to i }) } }`.

Cases:
- labels "BPD", "HC", "AC", "FL", "EFW", "FHR", "Presentation", "Placenta", "Amniotic fluid", "Impression" exist; `hasText("49 mm", substring = true)`; "Ultrasound Biometry" does not exist;
- `scanPresentation` displayed after `performScrollTo()`, contains "Cephalic";
- `scan(fluid = "Oligohydramnios")` at 360 dp, font scale 1.3: `assertNotEllipsized` (from `ComposeTextAsserts.kt`);
- `scan(images = listOf("a", "b"))`: two photo buttons, each height >= 47.9 dp; clicking `wsScanPhotoChip-s1-1` sets `opened == "s1" to 1`;
- `gestationalAgeBadge` text contains "20+0 wks"; with two images a band chip shows the photo count;
- `canEdit = false`: no `editUltrasound-s1`;
- empty patient: "No records".

## Risk

Low-medium. No level-3 file, no logic change.
