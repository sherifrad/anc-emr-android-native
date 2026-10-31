Now I have a thorough understanding of the codebase. Let me compile the audit findings.

## Team Nemotron 3 Ultra - lens CONCURRENCY, LIFECYCLE and COMPOSE STATE

### Bugs

| # | Sev | file:line | quote | failure scenario | fix | confidence |
|---|---|---|---|---|---|---|
| 1 | Blocker | MainActivity.kt:43 | `val securitySettings = securityRepo.settings.collectAsStateWithLifecycle().value` | `collectAsStateWithLifecycle()` returns `State<T>`; accessing `.value` immediately reads the **initial** value (empty list) before first emission, causing UI to show stale/uninitialized security settings on first composition. Triggers wrong theme, lock behavior on cold start. | Use `collectAsStateWithLifecycle()` directly in composition: `val securitySettings by securityRepo.settings.collectAsStateWithLifecycle()` | H |
| 2 | Blocker | MainActivity.kt:65 | `val state = viewModel.state.collectAsStateWithLifecycle().value` | Same as #1 — reads initial `AncState` (empty patients, `vaultState = Sealed`) before vault controller emits, causing AncApp to render with empty state briefly; also breaks `LaunchedEffect(authenticating?.request?.id)` which may fire on wrong state. | `val state by viewModel.state.collectAsStateWithLifecycle()` | H |
| 3 | Should-fix | AncViewModel.kt:67-80 | `viewModelScope.launch { controller.view.collectLatest { ... _state.update { ... } } }` | `collectLatest` cancels previous emission handling mid-flight. If `sessionView.state` transitions `Unlocked -> Locked -> Unlocked` rapidly, the inner `_state.update` for the first Unlocked may run *after* the second, causing patient list to show stale snapshot or wrong `vaultState`. | Use `collect` (not `collectLatest`) since each emission fully replaces state; or add epoch guard comparing `sessionView.epoch` before updating. | M |
| 4 | Should-fix | AncViewModel.kt:166-180 | `val frozen = frozenVisitCommand?.takeIf { it.editorSessionId == current.draft?.editorSessionId }` | Race: `saveVisit()` reads `current.draft` but between preparation and `submitVisit()`, user rotates device — `draft` survives (ViewModel) but `editorSessionId` in new Activity's recomposition may differ if `AncEvent.StartVisit` re-fires with new UUID. Causes double-submit or lost save. | Bind `editorSessionId` to `PendingSaveToken` at `SaveStarted` and validate token in `submitVisit` instead of comparing draft. | M |
| 5 | Should-fix | LabEditor.kt:179-180 | `val codexLabItems by codexRepo.labCodexItems.collectAsState()`<br>`val codexGroups by codexRepo.labCategoryGroups.collectAsState()` | `ClinicalCodexRepository` is a singleton loaded via `remember { ClinicalCodexRepository.getInstance(context) }` — `collectAsState()` (no lifecycle) keeps observing after `LabEditor` leaves composition, leaking observer and triggering recomposition on dead UI. Same pattern in `MedicationEditor.kt`, `CodexSettingsSections.kt`. | Use `collectAsStateWithLifecycle()` or hoist repository to ViewModel scope and expose `StateFlow`. | H |
| 6 | Should-fix | ClinicalCodexRepository.kt:20-28 | `private val _labCodexItems = MutableStateFlow(emptyList())`<br>`override val labCodexItems: StateFlow = _labCodexItems.asStateFlow()` | `loadCodexData()` runs in `init {}` on **main thread** (singleton instantiated from Compose `remember`), doing `SharedPreferences` I/O + JSON parsing synchronously. Blocks UI thread on first access. | Move loading to `Dispatchers.IO` via `viewModelScope.launch` or `lazy` init with `withContext(Dispatchers.IO)`. | H |
| 7 | Should-fix | AncViewModel.kt:113-138 | `viewModelScope.launch(Dispatchers.IO) { ... controller.saveAttachment(...) }` | `addAttachment` launches coroutine but **does not track** it — rapid "add attachment" clicks fire multiple unbounded coroutines. No dedup, no queue, no cancellation on `AncEvent.CancelEditor`. Attachments can save out-of-order or after editor closed. | Store `Job` in `frozenAttachmentCommand`; cancel on `CancelEditor`/`Back`; use `Mutex` for sequential saves. | M |
| 8 | Nit | MainActivity.kt:68-72 | `LaunchedEffect(authenticating?.request?.id) { authenticating?.request?.let { viewModel.completeAuthentication(request) } }` | Key is `request.id`; if same `id` re-emitted (e.g., biometric prompt dismissed and re-shown with same request), effect **won't re-run**, leaving auth incomplete. | Include `vaultState` hash in key: `LaunchedEffect(authenticating?.request?.id to state.vaultState)` | L |
| 9 | Nit | AncViewModel.kt:278-330 | `viewModelScope.launch { val result = controller.execute(...) { _state.update { ... } } ... }` | Inner `_state.update` (SaveReconciling) runs on **IO dispatcher** (from `controller.execute` → `withContext(dispatcher)`). `MutableStateFlow.update` must be called on main thread for Compose snapshot consistency; works by accident but violates contract. | Wrap `_state.update` in `withContext(Dispatchers.Main)` or ensure `controller.execute` calls reconciler on Main. | M |
| 10 | Nit | WorkspaceScreen.kt:126 | `LaunchedEffect(route.section) { pagerState.animateScrollToPage(section.ordinal) }` | On process death/restore, `route.section` restored but `pagerState` not (no `rememberSaveable`). `animateScrollToPage` jumps to wrong page or crashes if `pagerState.pageCount=0`. | Use `rememberSaveable` for `route` or `pagerState.currentPage`; guard `LaunchedEffect` with `pagerState.pageCount > section.ordinal`. | M |

### Style groups

| rule | deviation | count | examples file:line | introduced by |
|---|---|---|---|---|
| **Compose state collection must use `collectAsStateWithLifecycle()` for ViewModel/Lifecycle-owned flows** | Using `.collectAsState()` (no lifecycle) or `.collectAsStateWithLifecycle().value` (immediate read) | 6 | MainActivity.kt:43, 65; LabEditor.kt:179, 180; MedicationEditor.kt:162, 163; CodexSettingsSections.kt:66, 67, 714 | Multiple authors; pattern suggests no code review gate for state collection |
| **No `rememberSaveable` used anywhere** — process-death state loss guaranteed | Zero usages of `rememberSaveable`; all draft/editor state in `mutableStateOf`/`remember` | 0 (total absence) | — | Project-wide |
| **Singleton repositories instantiated in Compose via `remember { Repo.getInstance(context) }`** | `ClinicalCodexRepository`, `SecuritySettingsRepository` created in composition, not ViewModel/DI | 4 | LabEditor.kt:178; MedicationEditor.kt:161; CodexSettingsSections.kt:64; ProfileScreen.kt:89 | Introduced in codex/settings refactor (likely GPT-6 Luna) |
| **Coroutines launched in ViewModel without structured tracking** | `viewModelScope.launch` fire-and-forget for attachment save, backup export, clinical saves | 8 | AncViewModel.kt:113, 143, 475, 557, 706, 717; SaveVisitCommand.kt:148; RecordsRepository.kt:200+ | Predominant pattern from initial ViewModel design |
| **Hard-coded strings in UI (no string resources)** | "Locally Encrypted & Secured", "Pending Cloud Sync", "Synthetic coordinator test", error messages | 20+ | AttachmentSyncManager.kt:29-31; SaveCoordinatorTest.kt:23; AncViewModel.kt:136, 156; UiErrorMessages.kt (if exists) | Mixed; no lint rule enforcing `@string/` |

### Not covered / not verified
- **Room transaction isolation**: `RecordsDao` uses `@Transaction` but `RecordsRepository` calls `database.withTransaction { }` — nested transactions not verified for deadlock safety.
- **SQLCipher native library loading**: `RecordsDatabase.kt:32-45` uses `@Volatile` + lock; correctness under concurrent first-access from multiple threads not verified by running.
- **VaultController `Mutex` usage**: Lines 16, 353, 383, 419 — protects provisioning but not all `execute` paths; race on `sessionEpoch` not verified.
- **Backup export/restore**: `VaultController.exportEncryptedBackup`/`inspectEncryptedBackup` run on `dispatcher` (IO) but touch `SharedPreferences` and file I/O — no test coverage for concurrent backup + unlock.
- **BiometricPrompt lifecycle**: `AppLockScreen.kt` uses `LaunchedEffect(Unit)` for auth — cancellation on configuration change not verified.

### Verdict: **YES-after-fixes**

**Top 3 fixes required before continuing:**
1. **Fix state collection in MainActivity** (Bugs #1, #2) — causes wrong initial UI, potential lock-screen bypass, theme flash.
2. **Replace all `collectAsState()` with `collectAsStateWithLifecycle()`** in editors (Bug #5) — leaks observers, crashes on recomposition after navigation.
3. **Move `ClinicalCodexRepository` loading off main thread** (Bug #6) — blocks UI on first editor open; trivial to fix with `viewModelScope.launch(Dispatchers.IO)`.

---

> 🦅 **MACA Active Fleet Dispatch**  
> Tier: 1 (Single-Model Multi-Session) | Phase: 1 (Recon) → 2 (Analysis) → 3 (Audit)  
> Lane Assignments:  
> - Primary: Concurrency/Lifecycle/Compose State (this team)  
> - Deferred: Security/Intent/Manifest audit (separate lane)  
> - Deferred: Room/SQLCipher transaction stress (separate lane)
