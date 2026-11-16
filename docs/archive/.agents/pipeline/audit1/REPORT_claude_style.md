## Team Sonnet 5.5 - lens CODE CONSISTENCY / BEST PRACTICES

Everything below comes from reading and grep, with no build and no test run. The unused-import counts come from my own scripted grep, so they are "not verified by compiler".

**Authorship caveat:** `git log` shows one author (`sherifrad`) on all 11 commits, so `git blame` cannot separate the models. I can only attribute a deviation to the commit that added the file. The initial commit `36c303e` added 74 .kt files (every older main and test file). Later commits added few files: `f0e1cb9` 2, `c0e4917` 8, `2ff5051` 3, `64bb935` 6 and `5fd691d` 4.

### Bugs

| # | Sev | file:line | quote | failure scenario | fix | conf |
|---|---|---|---|---|---|---|
| 1 | Should-fix | `state/AncViewModel.kt:135-136` | `} catch (e: Exception) { android.util.Log.e("AncViewModel", "Failed to save attachment", e)` | Disk full, a bad URI or a vault error while saving an attachment: the exception goes only to logcat. No state change reaches the UI, so the clinician believes the scan or report was filed. | Add `AttachmentFailed` to the state and show a snackbar or dialog. | H |
| 2 | Should-fix | `state/AncViewModel.kt:146-156` | `PatientAttachmentVault.deleteAttachment(` is called before `controller.deleteAttachment(` | The file is deleted first, then the DB row. If the DB step throws (swallowed at line 155), the row stays and points at a missing file: a broken attachment with no error shown. Reversing the order would be safer. | Delete the DB row first, then the file, and report failures. | M |
| 3 | Should-fix | `ui/AddAttachmentDialog.kt:293-294` | `openInputStream(selectedUri!!)?.use { ...BitmapFactory.decodeStream(it)` | The preview decodes the full-size user image with no `inSampleSize`. A 12 MP or larger photo (about 48 MB bitmap) can cause an OutOfMemoryError. `runCatching` does not catch `Error`, so the app crashes on the add-attachment screen. | Decode with bounds first, then use `inSampleSize` (the vault code does this). | M |
| 4 | Should-fix | `ui/WorkspaceScreen.kt:1925`, `ClinicalAttachmentViewerDialog.kt:82`, `UltrasoundPhotoViewerDialog.kt:75` | `BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()` | Full-resolution decode on the composition path, with no downsampling. Large or old files cause jank or OOM. Whether this runs off the main thread depends on the call site; I did not verify that. | Add a shared bounded-decode helper. | L-M |
| 5 | Nit | `ui/PatientEditor.kt:178-186` | `java.time.LocalDate.parse(trimmedLmp).plusDays(280).toString()` | The EDD and GA are computed inline in the composable on every recomposition. This is a second copy of the EDD rule that also exists in `IntakeDatingRule.kt:25` and `GestationalAgeEngine.kt:122`. The `days in 0..300` cutoff exists only here. The three places can drift apart. | Use one `eddFromLmp()` in `model/`. | H |
| 6 | Nit | `report/PatientPrintHtmlGeneratorTest.kt:1` | `package com.sherifradwan.ancnative.report` | This is a stale, near-empty test (1 `@Test`, 53 lines). The real one is `ui/report/PatientPrintHtmlGeneratorTest.kt` (545 lines). Same class name, wrong package: it tests `ui.report.PatientPrintHtmlGenerator` but sits in a non-matching package. It gives false coverage and confuses runs. | Delete it or move it into `ui/report`. | H |
| 7 | Nit | `ui/ClinicalBadges.kt:120` | `text = if (hasValue) value!! else placeholder` | This is safe today only if `hasValue` is derived from `value != null`, which I did not confirm. `value!!` is fragile (similar sites: `WorkspaceScreen.kt:232,241`, `ProfileScreen.kt:275`). | Use `value ?: placeholder` or a local `val`. | M |
| 8 | Nit | `ui/CodexSettingsSections.kt:473` | `var testName by remember { mutableStateOf("") }` | No `rememberSaveable` anywhere in the project (0 hits). The manifest has no `configChanges` and no orientation lock (grep shows only `allowBackup` and `exported`). Rotating the device while adding a lab test or medication wipes the typed dialog fields, the app-lock PIN entry (`AppLockScreen.kt:77`) and others. Draft forms live in the ViewModel, so only the local dialogs are affected. Not verified by running. | Use `rememberSaveable` for dialog fields. | M |

### Style groups

**Dominant pattern (inferred from the majority of files):**
- Package `com.sherifradwan.ancnative.{model,state,storage,security,ui}`, with composables in `ui/` and report code in `ui/report`.
- Camel-case test names with `@Test fun x()`, one test class per production file.
- Explicit imports, no wildcards.
- `internal` for module-private helpers, and `catch (_: Exception)` returning null at parse sites.

| Rule | Deviation | Count | Examples (file:line) | Introduced by |
|---|---|---|---|---|
| No unused imports | Unused imports (my script, delegate imports excluded) | 55 across 20 files | `ui/CodexSettingsSections.kt:3,4,6,12,27,31` (6); `ui/PatientListScreen.kt:23,33,41,60,69`; `ui/ClinicalAttachmentViewerDialog.kt:7,12,18,19,22`; `ui/VisitEditor.kt:5,9,13,39,40`; `ui/UltrasoundPhotoViewerDialog.kt:14,20,44,51` | Mostly `36c303e` (initial commit) |
| User-visible text from `strings.xml` | Hard-coded `Text("…")` literals. `strings.xml` has 324 strings, but 124 `Text(` calls use a literal | 124 (+34 hard-coded `contentDescription = "…"`) | `ui/CodexSettingsSections.kt:125,134,148,154` (54 literals, **0** `stringResource`); `ui/SecuritySettingsDialog.kt` (25 literals vs 3 `stringResource`); `ui/WorkspaceScreen.kt:1782,1837`; `ui/UltrasoundPhotoViewerDialog.kt:265` | `36c303e` (Codex and security settings written in a different style from WorkspaceScreen, which has 58 `stringResource`) |
| Imports, not fully-qualified names | FQN inline although imports exist | about 12 | `state/AncViewModel.kt:113,143` (`kotlinx.coroutines.Dispatchers.IO`) while `kotlinx.coroutines.launch` is imported; `AncViewModel.kt:109,120` (`com.sherifradwan.ancnative.storage.PatientAttachmentVault`); `ui/PatientEditor.kt:180,183,184` (`java.time.*`, 5 uses); `ui/WorkspaceScreen.kt:1925` | `36c303e` (attachment feature) |
| No dev-process references in code | Comments cite `Q1…Q16`, `D1/D3`, "round 1", `.agents/pipeline/SPEC_FIX.md` | 22 lines | `ui/DeliveryNotesParser.kt:24,64,125,141,192,199`; `ui/ClinicalFormat.kt:21`; test header `ui/ParseDeliveryNotesSpecTest.kt:7-20` | `c0e4917` (round 2/2b-e: spec-driven parser and tests) |
| Test names | The initial tests (and `ClinicalCommandsTest`, `AncStateTest`, instrumented tests) use camelCase `fun x()`. New tests are snake_case (70 functions), compressed one-liners, or use `= assertEquals(...)` | snake_case 70 functions in 5 files. `@Test fun x() {` or `@Test fun x() = …` one-liners: 5 files (`storage/VaultCleanupRuleTest.kt:15`, `security/PinPolicyTest.kt:21`, `security/AppLockPolicyTest.kt`, `storage/VaultAutoActionTest.kt`, `state/IntakeDatingRuleTest.kt`) | `ui/report/PatientPrintHtmlGeneratorTest.kt` (40), `NegativeControlAndClinicalValidationTest.kt` (11), `ui/ClinicalFormatR2Test.kt` (8), `ClinicalFormatTest.kt` (6), `ThemeContrastTest.kt` (5) | snake_case: `f0e1cb9`, `c0e4917`, `2ff5051`; one-liners: `64bb935`, `5fd691d` |
| Test file naming | "R2", "Spec" and "AndZeroDataLoss" are process labels, not subjects. `ClinicalFormatR2Test` splits one subject across two files | 4 files | `ui/ClinicalFormatR2Test.kt`, `ui/ParseDeliveryNotesSpecTest.kt` (757 lines, bigger than its subject), `NegativeControlAndClinicalValidationTest.kt`, `storage/BackupVaultIntegrationAndZeroDataLossTest.kt` | `f0e1cb9`, `c0e4917` |
| One duplicate helper per rule | EDD = LMP+280 written three times | 3 | `ui/PatientEditor.kt:180`, `state/IntakeDatingRule.kt:25`, `model/GestationalAgeEngine.kt:122,129` (bare literal `280`) | `36c303e` + `2ff5051` (IntakeDatingRule copied the rule instead of calling the engine) |
| One duplicate helper per rule | ISO date parse | `parseDateOrNull` twice, plus about 28 inline `LocalDate.parse` blocks | `model/LongitudinalTimelineProjection.kt:117`, `model/GestationalAgeEngine.kt:159`; inline at `ui/AncDatePicker.kt:50`, `storage/SaveVisitCommand.kt:70`, `storage/RecordsRepository.kt:609`, `state/IntakeDatingRule.kt:11` (strict `toString()==` check) and `model/ClinicalModels.kt:116` (lenient `trim()`). The strictness is inconsistent, so the same date can be valid in one layer and rejected in another | `36c303e` |
| One duplicate helper per rule | SHA-256 to hex | 5 copies of `joinToString("") { "%02x".format(it) }` | `storage/ClinicalCommands.kt:483-485`, `storage/SaveVisitCommand.kt:103`, `storage/PatientAttachmentVault.kt:168,238`, `security/SecuritySettingsRepository.kt:140` | `36c303e` |
| `catch` style | `Throwable` vs `Exception` vs `_` | 44 `catch (x: Throwable/Exception)`; 8 `runCatching`; 3 `Log.e` sites | `storage/AndroidVaultKeys.kt:86,159,178` (`_: Throwable`), `storage/RecordsRepository.kt:248,265,301` (`failure: Throwable`), `state/AncViewModel.kt:135` (`e: Exception`) | Mixed across `36c303e` and `64bb935` |
| File size ≤600 lines, one feature per file | Oversized files | 17 files (14 main) | `ui/WorkspaceScreen.kt` 2159, `ui/ProfileScreen.kt` 2129, `storage/RecordsRepository.kt` 1385, `model/DemoFixtures.kt` 1269, `ui/report/PatientPrintHtmlGenerator.kt` 1245, `state/AncState.kt` 1211, `ui/CodexSettingsSections.kt` 1097, `SecuritySettingsDialog.kt` 979, `PatientListScreen.kt` 968, `storage/VaultController.kt` 813; also `AncViewModel.kt` 723, `EncryptedBackupManager.kt` 710, `PatientEditor.kt` 688, `UltrasoundEditor.kt` 645; tests: `ParseDeliveryNotesSpecTest.kt` 757, androidTest `AncWorkflowTest.kt` 1058, `EncryptedRecordsTest.kt` 1039 | `36c303e` (grew in later rounds) |
| Names must match meaning | `Demo*` prefix on production types (`DemoPatient`, `DemoVisit`, `DemoAttachment`, `model/DemoModels.kt:3`) and `DemoFixtures.kt` (1269 lines) live in `main`. Real patient data is typed `DemoPatient`. "Codex" names (`ui/CodexSettingsSections.kt`, `LabCodexSettingsSection`) mean a lab and medication catalogue. `ui/DemoModels` and the `ui` package also hold logic (`DeliveryNotesParser.kt`, `ClinicalFormat.kt`) | 61 `DemoPatient` uses | `model/DemoModels.kt:3`, `ui/CodexSettingsSections.kt:62,710`, `storage/AttachmentSyncManager.kt` | `36c303e` |
| Compose conventions | 0 `@Preview`; 0 `rememberSaveable`. `modifier: Modifier = Modifier` appears in only 16 of 82 composables, so most have no modifier parameter. 129 local `remember`s in 7 big screens, with state kept in screens instead of hoisted. Business logic in composables (`PatientEditor.kt:177-186`) | project-wide | `ui/WorkspaceScreen.kt:407`, `ui/Components.kt:32`, `ui/TimelineSpine.kt:20` (the few that follow the rule) | `36c303e` |

### Proposed single standard
- Strings go in `strings.xml`, with no literals in `Text(` or `contentDescription`.
- Explicit imports only, and every import used.
- Use `rememberSaveable` for dialog fields.
- Every public composable takes `modifier: Modifier = Modifier` as its first optional parameter.
- Test names are camelCase `fun subjectBehaviour()`, with one test class per production class and no process labels.
- Comments describe the behaviour and never cite Q/D/round IDs.
- Dates, EDD, SHA-hex and bounded bitmap decode each live in one shared helper.
- Production types do not use a `Demo` prefix. Split files larger than 600 lines.

### Ranked fix list (safest first)
1. Delete the 55 unused imports and the stale `report/PatientPrintHtmlGeneratorTest.kt`. Zero risk, compiler-checked.
2. Replace FQNs with imports. Rewrite the Q/D/round comments into plain descriptions. Rename the R2/Spec/AndZeroDataLoss tests.
3. Extract shared helpers (`eddFromLmp`, `parseIsoDateOrNull`, `sha256Hex`), with one test each. Make strict and lenient date parsing consistent.
4. Move `CodexSettingsSections` and `SecuritySettingsDialog` text into `strings.xml`.
5. Add `rememberSaveable` to the dialog fields. Add the `modifier` parameter to composables.
6. Surface attachment errors (bugs 1 and 2) and bound the bitmap decode (bugs 3 and 4).
7. Rename `Demo*` types and split the 14 oversized main files. Do this last; it is a large mechanical change.

### Not covered / not verified
- I did not run a build, lint, detekt or ktlint, so the unused-import count is from a text script, not the compiler.
- I did not read all of the 36k lines. I scanned structure by grep and sampled files. I did not check the correctness of clinical rules (GA, risk), the encryption code, `RecordsMigration`, the DAO queries, or the `res/` and `.agents/checks` contents.
- Bugs 2 to 4 and 8 are not verified by running. Bug 4's thread usage was not checked.
- I could not separate models by `git blame` because there is a single author. Attribution is by the commit that added each file.

### Verdict: YES-after-fixes
Style problems are widespread, but I found nothing in my lens that endangers data on its own. The three things to fix first:
1. Attachment save and delete errors are swallowed, and the delete order can leave dangling rows (bugs 1 and 2, `AncViewModel.kt:135,155`).
2. Duplicated clinical and date helpers (EDD ×3, date parsing ×30 with inconsistent strictness, SHA ×5) plus the stale duplicate-named test (bugs 5 and 6). Consolidate them before more rounds add copies.
3. Remove the 55 unused imports and move the 124 hard-coded strings and 34 hard-coded content descriptions into `strings.xml` (starting with `CodexSettingsSections.kt` and `SecuritySettingsDialog.kt`). Also add `rememberSaveable` and bounded bitmap decoding (bugs 3, 4 and 8).
