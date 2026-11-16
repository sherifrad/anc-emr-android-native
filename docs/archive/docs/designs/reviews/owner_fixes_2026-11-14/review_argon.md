VERDICT: FAIL

### Findings

1. **Severity: serious** — [UltrasoundPhotoViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundPhotoViewerDialog.kt#L172-L184), [ClinicalAttachmentViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt#L186-L202)
   - **Concrete input**: Open `UltrasoundPhotoViewerDialog` on any scan with a valid decodable image (and 2+ photos or biometric badges), or open `ClinicalAttachmentViewerDialog` on any attachment with a valid decodable image.
   - **Wrong output**: Changing the viewport `Box` from `.height(280.dp)` / `.height(380.dp)` to `.heightIn(min = 280.dp)` / `.heightIn(min = 380.dp)` gives it `maxHeight = Dp.Infinity` inside a non-scrolling `Column`, while its child `ZoomableImage` has `Modifier.fillMaxSize()`. `fillMaxSize()` forces the `Box` to take 100% of the remaining dialog height, pushing the pager buttons (`prevPhotoButton` / `nextPhotoButton`), biometric pills, and `dismissPhotoViewer` button in `UltrasoundPhotoViewerDialog`, and the SHA-256 medico-legal verification strip in `ClinicalAttachmentViewerDialog`, to `0.dp` / off the bottom of the card.
   - **One-line fix**: Bound the viewport height with `.heightIn(min = 280.dp, max = 360.dp)` (or `.weight(1f, fill = false)` on the `Box` with `Modifier.fillMaxWidth().height(280.dp)` / `380.dp`) instead of unbounded `heightIn` + `fillMaxSize()`.

2. **Severity: serious** — [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L20-L35)
   - **Concrete input**: Stored `clinicalAttention` string `"NOTE: Prefers female doctor\n[CRITICAL]\n• Placenta previa"` (or `"[CRITICAL]\n• Placenta previa"`).
   - **Wrong output**: On the standalone `"[CRITICAL]"` header line, `match` is `CRITICAL` and `cleanText` is `""`, so `if (cleanText.isNotBlank())` is skipped and `currentLevel` is never updated. The following bullet `"• Placenta previa"` inherits `currentLevel = NOTE` (rendering neutral grey with an `Info` icon in `ClinicalAttentionCard` and `CompactClinicalAttentionStrip`) or `ALERT` (amber instead of `CRITICAL` red).
   - **One-line fix**: Assign `currentLevel = level` unconditionally when `match != null` (at line 25, before `if (cleanText.isNotBlank())`).

3. **Severity: serious** — [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L26-L35), [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L107-L112)
   - **Concrete input**: In `ClinicalAttentionDialog`, add an entry with text `"-ve Rh, needs anti-D"` or `"-2 station"` (or `"---"`) and tap `Save`.
   - **Wrong output**: `composeAttentionEntries` writes `"CRITICAL: -ve Rh, needs anti-D"`, and on read-back `parseAttentionEntries` passes `match.remainder` through `stripBullet`, which strips leading `'-'`, `'•'`, and `'*'` characters even without following whitespace—turning `"-ve Rh, needs anti-D"` into `"ve Rh, needs anti-D"` (inverting Rh-negative to read as positive and breaking round-trip idempotence). An entry of `"---"` saves as `"ALERT: ---"` in storage/print but parses to `emptyList()`, hiding the alert in the UI.
   - **One-line fix**: Use `match.remainder.trim()` instead of `stripBullet(match.remainder)` after a matched prefix, and in `stripBullet` only strip bullet characters when followed by whitespace (`text.trim().replaceFirst(Regex("^[•\\-*]+\\s+"), "")`).

4. **Severity: serious** — [ClinicalAttentionBanner.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt#L294-L381), [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L43-L52)
   - **Concrete input**:
     1. Open `ClinicalAttentionDialog` on a patient with 10 entries (or 10 entries where the clinician clears one row's `OutlinedTextField` to `""`), type `"Placenta accreta"` into the still-enabled `attentionNewText` field, and tap `Save`.
     2. Open `ClinicalAttentionDialog` on a legacy patient with a single 180-character clinical attention note (or 11 legacy bullet lines) and tap `Save`.
   - **Wrong output**:
     1. `if (newText.isNotBlank() && all.size < ATTENTION_ENTRIES_MAX)` at line 377 checks `all.size` before blank entries are removed by `composeAttentionEntries`, so `"Placenta accreta"` in `attentionNewText` is silently discarded on Save.
     2. `parseAttentionEntries` preserves >140-char and >10-line legacy notes on display, but tapping `Save` runs `composeAttentionEntries`, which silently truncates each entry to 140 characters and drops entries past 10.
   - **One-line fix**: Filter blank rows from `all` before checking `all.size < ATTENTION_ENTRIES_MAX` in `confirmButton`, disable `attentionNewText` when `entries.count { it.text.isNotBlank() } >= ATTENTION_ENTRIES_MAX`, and enforce the 140-char limit in the dialog input fields rather than truncating pre-existing longer entries inside `composeAttentionEntries`.

5. **Severity: moderate** — [PatientHubScreen.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubScreen.kt#L198), [PatientHubHeader.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt#L223-L399)
   - **Concrete input**: Open the workspace of an archived patient (`status = "archived"`) or a patient while `!canModifyData` (e.g., `state.hasDirtyDraft == true` or `savePhase == PENDING`) who has a clinical attention note or severe BP; tap `clinicalAttentionStrip`, `workspaceAttentionButton`, or `stickyAttentionButton`, add/edit an entry, and tap `Save`.
   - **Wrong output**: While `OverviewTab` disables editing when `!canModifyData`, `PatientHubHeader` always opens the editable `ClinicalAttentionDialog`. Tapping `Save` dispatches `StartPatientEditor` + `PatientDraftChanged` + `SubmitPatientProfile`, which `startPatientEditor` ignores for archived patients or when a draft is dirty, silently losing the clinician's changes.
   - **One-line fix**: Gate `onOpenAttentionDialog` (or pass `canEdit = canModifyData` to `ClinicalAttentionDialog`) so editing controls and `Save` are disabled when `!canModifyData`.

6. **Severity: moderate** — [ZoomableImage.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/components/ZoomableImage.kt#L81-L157)
   - **Concrete input**: Place two fingers on `ZoomableImage` at `1.0x` scale and pinch outward continuously toward `3.0x` (or pinch inward from `2.0x` back to `1.0x`).
   - **Wrong output**: `isZoomed` (`scale > 1f`) is passed as a key to `.pointerInput(resetKey, isZoomed)` at line 106. On the first frame where `scale` crosses `1.0f`, `isZoomed` flips and Compose immediately cancels and restarts the `PointerInputScope` coroutine mid-gesture, killing the active pinch until the user lifts and places both fingers again. Additionally, at `1.0x` (`isZoomed == false`), a 2-finger drag with `zoom == 1f` and `pan != Offset.Zero` enters `if (zoom != 1f || pan != Offset.Zero)` at line 134 and consumes pointer changes at lines 149–151 without zooming.
   - **One-line fix**: Remove `isZoomed` from `.pointerInput(resetKey)` and handle both `scale == 1f` (`downCount >= 2 && zoom > 1f`) and `scale > 1f` inside a single `awaitEachGesture` block.

7. **Severity: moderate** — [PatientHubHeader.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt#L509-L558)
   - **Concrete input**:
     1. Patient has `"CRITICAL: Placenta previa"` (no severe BP) vs `"ALERT: Gestational diabetes"`.
     2. On a 360 dp screen, patient has both a visit with BP `165/105` on `2026-11-10` and `"CRITICAL: Complete placenta previa"`.
   - **Wrong output**:
     1. `CompactClinicalAttentionStrip` displays `Icons.Default.Warning` and `baseText = "Placenta previa"` without the localized level word (`"Critical"` / `"Alert"` / `"Note"`), distinguishing `CRITICAL` from `ALERT` by colour alone in violation of `SPEC_attention.md`.
     2. When `severeBp != null` and 1 entry exists, `baseText` is `"Severe BP: 165/105 (2026-11-10) • Complete placenta previa"` with `maxLines = 1` and `remainingCount = 0` (`entries.size - 1`), so on a 360 dp screen `"Severe BP: 165/105 (2026-11-10) • "` consumes nearly the full row width and ellipses the critical entry with no `+1` badge; line 509 also hardcodes `"Severe BP: ..."` instead of `stringResource(R.string.bp_attention_severe, ...)`.
   - **One-line fix**: Include the localized level word in `firstEntryText`, use `stringResource(R.string.bp_attention_severe, severeBp.bloodPressure, severeBp.date)`, and allow `maxLines = 2` (or count `firstEntry` in `remainingCount` when `severeBp != null`).

8. **Severity: moderate** — [PatientListScreen.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt#L183-L188), [PatientListScreen.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt#L221-L227)
   - **Concrete input**: Database has 1 delivered patient `"Fatma"` (and/or 1 archived patient `"Mona"`) and 0 active patients; enter `"Fatma"` (or `"Mona"`) in `patientSearch` with the `All` filter chip selected.
   - **Wrong output**: `dashboardPatients(visibleList, "all", searching = true)` returns `visibleList` (showing `"Fatma"` and even archived `"Mona"` in the `All` list), while `allCount` on the `All` chip is `activePatients.size` (`0`), so the selected chip displays `"All (0)"` above non-empty search results, and archived patients leak into `All` during search despite `SPEC.md` stating
VERDICT: FAIL

### Findings

1. **Severity: serious** — [UltrasoundPhotoViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundPhotoViewerDialog.kt#L172-L184), [ClinicalAttachmentViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt#L186-L202)
   - **Concrete input**: Open `UltrasoundPhotoViewerDialog` on a scan with a decodable image (and 2+ photos or biometric badges), or open `ClinicalAttachmentViewerDialog` on an attachment with a decodable image.
   - **Wrong output**: Changing the viewport `Box` from `.height(280.dp)` / `.height(380.dp)` to `.heightIn(min = 280.dp)` / `.heightIn(min = 380.dp)` leaves `maxHeight = Dp.Infinity` inside a non-scrolling `Column`, while its child `ZoomableImage` uses `Modifier.fillMaxSize()`. `fillMaxSize()` forces the `Box` to consume 100% of the remaining dialog height, pushing the pager controls (`prevPhotoButton` / `nextPhotoButton`), biometric badges, and `dismissPhotoViewer` button in `UltrasoundPhotoViewerDialog`, and the SHA-256 medico-legal verification strip in `ClinicalAttachmentViewerDialog`, to zero height / off the bottom of the card.
   - **One-line fix**: Bound the viewport height with `.height(280.dp)` / `.height(380.dp)` (or `.heightIn(min = 280.dp, max = 360.dp)`) instead of unbounded `heightIn` with a `fillMaxSize()` child.

2. **Severity: serious** — [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L20-L35)
   - **Concrete input**: Stored `clinicalAttention` string `"NOTE: Prefers female doctor\n[CRITICAL]\n• Placenta previa"` (or `"[CRITICAL]\n• Placenta previa"`).
   - **Wrong output**: On the standalone `"[CRITICAL]"` header line, `match` is `CRITICAL` and `cleanText` is `""`, so `if (cleanText.isNotBlank())` is skipped and `currentLevel` is never updated. The following bullet `"• Placenta previa"` inherits `currentLevel = NOTE` (rendering neutral grey with an `Info` icon in `ClinicalAttentionCard` and `CompactClinicalAttentionStrip`) or `ALERT` (amber instead of `CRITICAL` red).
   - **One-line fix**: Assign `currentLevel = level` unconditionally when `match != null` (at line 25, before `if (cleanText.isNotBlank())`).

3. **Severity: serious** — [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L26-L35), [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L107-L112)
   - **Concrete input**: In `ClinicalAttentionDialog`, add an entry with text `"-ve Rh, needs anti-D"` or `"-2 station"` (or `"---"`) and tap `Save`.
   - **Wrong output**: `composeAttentionEntries` writes `"CRITICAL: -ve Rh, needs anti-D"`, and on read-back `parseAttentionEntries` passes `match.remainder` through `stripBullet`, which strips leading `'-'`, `'•'`, and `'*'` characters even without following whitespace—turning `"-ve Rh, needs anti-D"` into `"ve Rh, needs anti-D"` (inverting Rh-negative to read as positive and breaking round-trip idempotence). An entry of `"---"` saves as `"ALERT: ---"` in storage/print but parses to `emptyList()`, hiding the alert in the UI.
   - **One-line fix**: Use `match.remainder.trim()` instead of `stripBullet(match.remainder)` after a matched prefix, and in `stripBullet` only strip bullet characters when followed by whitespace (`text.trim().replaceFirst(Regex("^[•\\-*]+\\s+"), "")`).

4. **Severity: serious** — [ClinicalAttentionBanner.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt#L294-L381), [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L43-L52)
   - **Concrete input**:
     1. Open `ClinicalAttentionDialog` on a patient with 10 entries (or 10 entries where the clinician clears one row's `OutlinedTextField` to `""`), type `"Placenta accreta"` into the still-enabled `attentionNewText` field, and tap `Save`.
     2. Open `ClinicalAttentionDialog` on a legacy patient with a single 180-character clinical attention note (or 11 legacy bullet lines) and tap `Save`.
   - **Wrong output**:
     1. `if (newText.isNotBlank() && all.size < ATTENTION_ENTRIES_MAX)` at line 377 checks `all.size` before blank entries are filtered out by `composeAttentionEntries`, so `"Placenta accreta"` waiting in `attentionNewText` is silently discarded on Save.
     2. `parseAttentionEntries` preserves >140-char and >10-line legacy notes on display, but tapping `Save` runs `composeAttentionEntries`, which silently truncates each entry to 140 characters and drops entries past 10.
   - **One-line fix**: Filter blank rows from `all` before checking `all.size < ATTENTION_ENTRIES_MAX` in `confirmButton`, disable `attentionNewText` when `entries.count { it.text.isNotBlank() } >= ATTENTION_ENTRIES_MAX`, and do
VERDICT: FAIL

### Findings

1. **Severity: serious** — [UltrasoundPhotoViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundPhotoViewerDialog.kt#L172-L184), [ClinicalAttachmentViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt#L186-L202)
   - **Concrete input**: Open `UltrasoundPhotoViewerDialog` on a scan with a valid image (and 2+ photos or biometric badges), or open `ClinicalAttachmentViewerDialog` on an attachment with a valid image.
   - **Wrong output**: Changing the viewport `Box` from `.height(280.dp)` / `.height(380.dp)` to `.heightIn(min = 280.dp)` / `.heightIn(min = 380.dp)` gives it `maxHeight = Dp.Infinity` inside a non-scrolling `Column`, while its child `ZoomableImage` has `Modifier.fillMaxSize()`. `fillMaxSize()` forces the `Box` to take 100% of the remaining dialog height, pushing the pager buttons (`prevPhotoButton` / `nextPhotoButton`), biometric badges, and `dismissPhotoViewer` button in `UltrasoundPhotoViewerDialog`, and the SHA-256 metadata strip in `ClinicalAttachmentViewerDialog`, to zero height / off the bottom of the card.
   - **One-line fix**: Use `.height(280.dp)` / `.height(380.dp)` (or `.heightIn(min = 280.dp, max = 360.dp)`) on the viewport `Box` instead of unbounded `heightIn` with a `fillMaxSize()` child.

2. **Severity: serious** — [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L20-L35)
   - **Concrete input**: `clinicalAttention = "NOTE: Prefers female doctor\n[CRITICAL]\n• Placenta previa"` (or `"[CRITICAL]\n• Placenta previa"`).
   - **Wrong output**: On the standalone `"[CRITICAL]"` line, `match` is `CRITICAL` and `cleanText` is `""`, so `if (cleanText.isNotBlank())` is skipped and `currentLevel` is never updated. The following line `"• Placenta previa"` inherits `NOTE` (rendering neutral grey with an `Info` icon) or `ALERT` (amber instead of `CRITICAL` red).
   - **One-line fix**: Set `currentLevel = level` whenever `match != null` (at line 25, before `if (cleanText.isNotBlank())`).

3. **Severity: serious** — [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L26-L35), [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L107-L112)
   - **Concrete input**: Add an entry with text `"-ve Rh, needs anti-D"` or `"-2 station"` (or `"---"`) in `ClinicalAttentionDialog` and tap `Save`.
   - **Wrong output**: `composeAttentionEntries` saves `"CRITICAL: -ve Rh, needs anti-D"`, and `parseAttentionEntries` runs `stripBullet(match.remainder)`, which strips leading `'-'` without requiring following whitespace—turning `"-ve Rh, needs anti-D"` into `"ve Rh, needs anti-D"` (inverting Rh-negative to read as positive and breaking round-trip idempotence). An entry of `"---"` saves as `"ALERT: ---"` but parses to `emptyList()`, hiding it in UI while keeping it in print/share reports.
   - **One-line fix**: Use `match.remainder.trim()` after a matched prefix and change `stripBullet` to require whitespace after bullet characters (`text.trim().replaceFirst(Regex("^[•\\-*]+\\s+"), "")`).

4. **Severity: serious** — [ClinicalAttentionBanner.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionBanner.kt#L294-L381), [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L43-L52)
   - **Concrete input**:
     1. Open `ClinicalAttentionDialog` on a patient with 10 entries (or 10 entries where one row's text field was cleared to `""`), type `"Placenta accreta"` into the enabled `attentionNewText` field, and tap `Save`.
     2. Open `ClinicalAttentionDialog` on a legacy patient with a 180-character note (or 11 lines) and tap `Save`.
   - **Wrong output**:
     1. `if (newText.isNotBlank() && all.size < ATTENTION_ENTRIES_MAX)` at line 377 checks `all.size` before blank rows are filtered out, silently discarding `"Placenta accreta"` on Save.
     2. `composeAttentionEntries` silently truncates pre-existing entries to 140 chars and drops entries beyond 10 on Save.
   - **One-line fix**: Filter blanks from `all` before checking `all.size < ATTENTION_ENTRIES_MAX` on Save, disable `attentionNewText` when `entries.count { it.text.isNotBlank() } >= ATTENTION_ENTRIES_MAX`, and only cap newly edited text in the dialog rather than truncating inside `composeAttentionEntries`.

5. **Severity: moderate** — [PatientHubScreen.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubScreen.kt#L198), [PatientHubHeader.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt#L223-L399)
   - **Concrete input**: Open the workspace of an archived patient (`status = "archived"`) or when `!canModifyData` (`state.hasDirtyDraft == true` or `savePhase == PENDING`), tap `clinicalAttentionStrip`, `workspaceAttentionButton`, or `stickyAttentionButton`, edit an entry, and tap `Save`.
   - **Wrong output**: Unlike `OverviewTab` (which passes `onEditAttention = null` when `!canModifyData`), `PatientHubHeader` always opens the editable `ClinicalAttentionDialog`. Tapping `Save` dispatches `StartPatientEditor`, which is ignored by the reducer when archived or when a draft is dirty, silently losing the edits.
   - **One-line fix**: Pass `canEdit = canModifyData` to `ClinicalAttentionDialog` (or hide/disable dialog editing when `!canModifyData`).

6. **Severity: moderate** — [ZoomableImage.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/components/ZoomableImage.kt#L81-L157)
   - **Concrete input**: Place two fingers on `ZoomableImage` at `1.0x` and pinch outward continuously toward `3.0x` (or pinch inward from `2.
VERDICT: FAIL

### Findings

1. **Severity: serious** — [UltrasoundPhotoViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundPhotoViewerDialog.kt#L172-L184), [ClinicalAttachmentViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt#L186-L202)
   - **Concrete input**: Open `UltrasoundPhotoViewerDialog` on a scan with a valid image (and 2+ photos or biometric badges), or open `ClinicalAttachmentViewerDialog` on an attachment with a
VERDICT: FAIL

### Findings

1. **Severity: serious** — [UltrasoundPhotoViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundPhotoViewerDialog.kt#L172-L184), [ClinicalAttachmentViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt#L186-L202)
   - **Concrete input**: Open `UltrasoundPhotoViewerDialog` or `ClinicalAttachmentViewerDialog` with a valid decodable image.
   - **Wrong output**: The viewport `Box` uses `.heightIn(min = 280.dp)` / `.heightIn(min = 380.dp)` (`maxHeight = Dp.Infinity`) inside a non-scrolling `Column`, while child `ZoomableImage` has `Modifier.fillMaxSize()`. `fillMaxSize()` forces the `Box` to take 100% of remaining dialog height, pushing the pager buttons (`prevPhotoButton`/`nextPhotoButton`), biometric badges, and `dismissPhotoViewer
VERDICT: FAIL

### Findings

1. **Severity: serious** — [UltrasoundPhotoViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundPhotoViewerDialog.kt#L172-L184), [ClinicalAttachmentViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt#L186-L202)
   - **Concrete input**: Open `UltrasoundPhotoViewerDialog` or `ClinicalAttachmentViewerDialog` with a valid decodable image.
   - **Wrong output**: Viewport `Box` changed from `.height(280.dp)` / `.height(380.dp)` to `.heightIn(min = 280.dp)` / `.heightIn(min = 380.dp)` (`maxHeight = Dp.Infinity`) inside a non-scrolling `Column`, while child `ZoomableImage` has `Modifier.fillMaxSize()`. `fillMaxSize()` forces the `Box` to consume 100% of remaining dialog height, pushing the pager buttons (`prevPhotoButton`/`nextPhotoButton`), biometric badges, and `dismissPhotoViewer` button in `UltrasoundPhotoViewerDialog`, and the SHA-256 verification strip in `ClinicalAttachmentViewerDialog`, to `0.dp` / off-screen.
   - **One-line fix**: Use `.height(280.dp)` / `.height(380.dp)` (or `.heightIn(min = 280.dp, max = 360.dp)`) on the viewport `Box` instead of unbounded `heightIn` with `fillMaxSize()`.

2. **Severity: serious** — [ClinicalAttentionEntries.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/components/ClinicalAttentionEntries.kt#L20-L35)
   - **Concrete input**: `clinicalAttention = "NOTE: Prefers female doctor\n[CRITICAL]\n• Placenta previa"` (or `"[CRITICAL]\n• Placenta previa"`).
   - **Wrong output**: On the standalone `"[CRITICAL]"` header line, `cleanText` is blank so `currentLevel = level` at line 33 is skipped. The following `"• Placenta previa"` line inherits `NOTE` (rendering neutral grey with `Info` icon) or `ALERT` (amber instead of `CRITICAL` red).
   - **One-line fix**: Set `currentLevel = level` unconditionally inside `if (match != null)` at line 25 before checking `if (cleanText.isNotBlank())`.

3. **Severity: serious** — [ClinicalAttentionEntries.kt](file:///Users/sher
VERDICT: FAIL

### Findings

1. **Severity: serious** — [UltrasoundPhotoViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundPhotoViewerDialog.kt#L172-L184), [ClinicalAttachmentViewerDialog.kt](file:///Users/sherifgamalradwan/anc-emr-android-native/app/src/main/java/com/sherifradwan/ancnative/ui/ClinicalAttachmentViewerDialog.kt#L186-L202)
   - **Concrete input**: Open
