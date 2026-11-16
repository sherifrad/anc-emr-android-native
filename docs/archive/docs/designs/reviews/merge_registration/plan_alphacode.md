# Merge Plan: `claude/clever-goldwasser-e086ce` → `main`

---

## 1. Strategy: Rebase the feature branch onto `main` (integration branch `merge/registration-flow`)

**Decision rationale:**

| Factor | Rebase onto main | Merge main into branch | Merge branch into main (integration branch) |
|--------|------------------|------------------------|---------------------------------------------|
| History clarity | Linear, bisectable | Pollutes branch with main's 42 commits | Merge commit only, but main stays clean until green |
| Conflict resolution | One pass, sequential | One pass, but on feature branch | One pass on integration branch |
| WIP commit handling | Can squash/fix before landing | WIP stays on feature branch | Can clean on integration branch |
| Rollback | `git reset --hard` to pre-rebase tag | Branch untouched | Delete integration branch |
| Owner approval gate | Before push to main | Before push to main | Before merge to main |
| **Verdict** | **✅ Best** | Acceptable | Fallback if rebase fails catastrophically |

**Chosen path:** Create `merge/registration-flow` from `main`, rebase the feature branch onto it, resolve conflicts there, verify fully, then fast-forward `main` (owner approval required). Never force-push `main`.

---

## 2. Pre-merge Safety

| Action | Command | Purpose |
|--------|---------|---------|
| Tag pre-merge main | `git tag -a pre-merge-registration-$(date +%Y%m%d) -m "main before registration merge"` | Instant rollback target |
| Tag feature branch tip | `git tag -a feature-registration-tip-$(date +%Y%m%d) claude/clever-goldwasser-e086ce` | Fallback reference |
| Create integration branch | `git checkout -b merge/registration-flow main` | Isolated workspace |
| Run branch audit | `.agents/scripts/git_audit.sh` | Confirm no stray worktrees, unmerged branches, stashes |
| Snapshot worktree | `cp -r .claude/worktrees/clever-goldwasser-e086ce .claude/worktrees/clever-goldwasser-e086ce.backup` | Preserve original branch state |
| Record commit hashes | `git rev-parse main > .merge/main-sha.txt && git rev-parse claude/clever-goldwasser-e086ce > .merge/feature-sha.txt` | Audit trail |

---

## 3. Conflict Resolution Plan (Per File)

### 3.1 `AncState.kt` — 5 hunks (highest risk)

| Hunk | Main keeps | Branch keeps | Rule |
|------|------------|--------------|------|
| Imports | `composeDeliveryNotes` | `PreviousPregnancyFormRules`, `effectiveMedicationStatus`, `parseBloodPressure`, `parseVisitFindings`, `ClinicalRecordKind` | **Union** — both sides add new imports; keep all |
| State fields (first) | `medicationStopUndo`, `medicationQuickSave` | `registrationFlow`, `awaitingAdvance` | **Union** — disjoint field sets; keep all 4 |
| `PatientDraft` fields | 12 delivery/dating fields (`delivered`, `deliveryDate`, `deliveryMode`, `deliveryOutcome`, `lmpDate`, `datingMethod`, `transferDate`, `embryoAgeDays`, `agreedEdd`, `scanDate`, `scanGaWeeks`, `scanGaDays`) | **None** (branch removed 8 dating fields) | **Main wins** — schema v7 delivery fields are frozen; branch's removal of 8 dating fields must be preserved *but* the 4 new delivery fields must be added back. **Result**: `PatientDraft` keeps main's 12 fields minus the 8 the branch intentionally removed (`lmpDate`, `datingMethod`, `transferDate`, `embryoAgeDays`, `agreedEdd`, `scanDate`, `scanGaWeeks`, `scanGaDays`) → net 4 delivery fields only. |
| State fields (second) | `medicationQuickSave`, `medicationStopUndo` (with doc comment) | `registrationFlow`, `awaitingAdvance` | **Union** — same as hunk 2; keep all 4 with main's doc comment on `medicationQuickSave` |
| `AncEvent` sealed interface | `deliveryPlanSheetDraft` extension + main's event list | **Entire replacement** of `AncEvent` with branch's expanded list (adds `RegistrationNext`, `StartPregnancyEditor`, `StartStatusEditor`, `Flow`, `DatingDraftChanged`, etc.) | **Branch wins for event list** — branch's `AncEvent` is a superset (adds registration flow events). **But** main's `deliveryPlanSheetDraft` extension must be retained. **Action**: take branch's `AncEvent` definition, add main's `deliveryPlanSheetDraft` extension below it. |

### 3.2 `PatientCommands.kt` — 1 hunk (imports)

| Main keeps | Branch keeps | Rule |
|------------|--------------|------|
| `DeliveryOutcomeFields`, `normalized`, `validateDeliveryOutcome` | `PreviousPregnancyFormRules` | **Union** — keep all 4 imports |

### 3.3 `AncApp.kt` — 1 hunk (imports + navigation)

| Main keeps | Branch keeps | Rule |
|------------|--------------|------|
| `medicationSheetDraft` | `FlowEvent`, `SystemBack`, `systemBackKind`, `RegistrationFlowScaffold`, `flowMoveEvent` | **Union** — keep all. **Critical**: navigation graph must register both the medication sheet route *and* the registration flow route. Verify `NavHost` composable includes both. |

### 3.4 `AncDatePicker.kt` — 1 hunk (UI detail)

| Main keeps | Branch keeps | Rule |
|------------|--------------|------|
| `⚡` prefix + `Modifier.minimumTouchTarget()` | Neither prefix nor `minimumTouchTarget()` | **Main wins** — accessibility touch target is a safety requirement (360dp viewport). Keep `⚡` prefix if it's intentional UX; otherwise drop prefix but **keep `minimumTouchTarget()`**. |

### 3.5 `PatientListScreen.kt` — 2 hunks (data model for list items)

| Hunk | Main keeps | Branch keeps | Rule |
|------|------------|--------------|------|
| Data class fields | `isDelivered: Boolean`, `deliveryDate: String?` | `isIncomplete: Boolean` | **Union** — list items need both delivered status (for Delivered filter) and incomplete status (for "Incomplete" chip). Keep all 3. |
| Field assignment | `isDelivered = patient.delivered`, `deliveryDate = patient.deliveryDate` | `isIncomplete = isRegistrationIncomplete(patient)` | **Union** — assign all 3. Add `isIncomplete` computation. |

### 3.6 `PatientHubHeader.kt` — 1 hunk (risk badge)

| Main keeps | Branch keeps | Rule |
|------------|--------------|------|
| `ClinicalRiskBadge(isHighRisk = ...)` | `ClinicalRiskBadge(isHighRisk = ..., isModerate = ...)` | **Branch wins** — `isModerate` adds medium-risk tier display. Keep branch signature; ensure `parseRiskTier` import exists. |

### 3.7 `strings.xml` — 1 hunk (disjoint string sets)

| Main keeps | Branch keeps | Rule |
|------------|--------------|------|
| 28 medication/delivery strings | 30+ registration flow strings | **Union** — keep all. No key collisions visible. Verify no duplicate `<string name="...">` keys. |

### 3.8 `concepts_brief.md` — modify/delete

| Main | Branch | Rule |
|------|--------|------|
| Deleted | Modified | **Delete** — main intentionally removed it. Branch's modification is stale. Drop the file. |

---

## 4. Semantic Traps (Auto-merged Files + Cross-cutting)

| Area | Trap | Check |
|------|------|-------|
| `PatientDraftValues` | Branch removed 8 dating fields; main added 4 delivery fields | **Result must have**: 4 delivery fields (`delivered`, `deliveryDate`, `deliveryMode`, `deliveryOutcome`) + remaining dating fields that branch kept (`edd`, `gaWeeks`, `gaDays`, `lmp` if any). **Zero** of the 8 removed fields. |
| `intakeDatingUpdate` rule | Branch may have altered dating logic | Verify `intakeDatingUpdate` still computes EDD/GA correctly with remaining fields. Run `DatingCalculatorTest`. |
| `SavePatientProfileCommand.prepare` | Main added delivery validation; branch may have changed fingerprint | Confirm `prepare` validates 4 delivery fields **and** registration flow fields. Fingerprint must include all required fields. |
| `DraftMapping.kt` | Auto-merged; maps DB ↔ draft | Spot-check: delivery fields map to `patients` table v7 columns; removed dating fields no longer mapped. |
| `startsAnotherAction` lists | Both sides may have added actions | Union of both lists. Verify no duplicate action keys. |
| `clearsFrozenPatient` | Both sides may have added triggers | Union. Verify registration flow start clears frozen patient; delivery plan sheet open does too. |
| Route handling (sheets over tabs) | Main: medication sheet, delivery plan sheet. Branch: registration flow (full-screen?) | Confirm `NavHost` handles: (a) bottom sheets over tabs via `navigation-compose` dialog destinations, (b) registration flow as full-screen destination replacing workspace. Pencil button → registration flow for new patient; pencil on existing patient → `PatientEditor` (old) or new pregnancy editor? **Clarify with owner**. |
| Pencil target | Goal: header pencil + New Patient button → new registration flow | Ensure `PatientHubHeader` pencil click emits `ShowNewPatientInfo` / `StartPatientEditor` with new flow. `PatientListScreen` FAB → same. |
| Strings | Both sides added keys | Grep for duplicate `name=` attributes. |
| Test files referencing removed fields | `EncryptedRecordsTest`, `AncWorkflowTest`, plus 23 branch test files | Search for `lmpDate`, `datingMethod`, `transferDate`, `embryoAgeDays`, `agreedEdd`, `scanDate`, `scanGaWeeks`, `scanGaDays` in test expectations. Update or delete. |

---

## 5. Ordered Steps with Verification Gates

| Step | Action | Verification | Executor |
|------|--------|--------------|----------|
| 0 | `git checkout -b merge/registration-flow main` | Branch exists at `main` HEAD | You |
| 1 | `git rebase claude/clever-goldwasser-e086ce` | Rebase starts; pauses at conflicts | You |
| 2 | **Resolve each conflicted file** per §3 | `git diff --check` clean; `git add <file>` | Builder lane (Model A) |
| 3 | **Fix semantic traps** in auto-merged files (§4) | Grep for removed fields → 0 hits; delivery fields present | Builder lane (Model A) |
| 4 | **Compile** | `./gradlew :app:compileDebugKotlin` → success | Builder lane |
| 5 | **Unit tests** (fast subset) | `./gradlew :app:testDebugUnitTest --tests "*Dating*" --tests "*DraftMapping*" --tests "*PatientCommands*"` → all pass | Builder lane |
| 6 | **Lint** | `./gradlew :app:lintDebug` → no new errors/warnings | Builder lane |
| 7 | **Targeted emulator tests** | Run: `AncWorkflowTest`, `EncryptedRecordsTest`, `RegistrationFlowTest` (new), `PatientListScreenTest`, `PatientHubHeaderTest` → all pass | Builder lane |
| 8 | **Reviewer audit** (Model B, different family) | Review diff: conflict resolutions, semantic traps, test coverage. Sign off or request changes. | Reviewer lane (Model B) |
| 9 | **Address reviewer feedback** | Repeat steps 2–7 as needed | Builder lane |
| 10 | **Full emulator suite** (456 tests, ~9 min) | `./run_emulator_tests.sh` → 100% pass | Builder lane |
| 11 | **Owner manual QA** (phone) | Checklist §7 → all ✅ | Owner |
| 12 | **Owner approval to merge** | Explicit "merge approved" | Owner |
| 13 | **Fast-forward main** | `git checkout main && git merge --ff-only merge/registration-flow && git push origin main` | You (only on owner approval) |
| 14 | **Post-merge verification** | `./gradlew :app:testDebugUnitTest` on `main` → green | You |
| 15 | **Cleanup** | Delete `merge/registration-flow`, keep tags | You |

**Builder vs Reviewer separation:** Builder (Model A) writes fixes. Reviewer (Model B) reads diff only, never writes. They must be from different model families (e.g., Builder = Claude, Reviewer = GPT-4o / Gemini / Opus).

---

## 6. Stop Conditions (Abandon & Fall Back)

| Condition | Action |
|-----------|--------|
| Rebase produces >50 conflict hunks (indicates divergence too deep) | Abort rebase (`git rebase --abort`), fall back to **merge strategy** on integration branch (`git merge --no-ff claude/clever-goldwasser-e086ce`) |
| Any unit test fails after resolution and cannot be fixed in 2 iterations | Tag `merge/registration-flow-broken`, revert to `pre-merge-registration-*` tag, escalate to owner |
| Lint introduces new errors in `AncState.kt`, `PatientCommands.kt`, or navigation | Stop; do not proceed to emulator |
| Full suite has flaky failures unrelated to merge (pre-existing) | Document, but do not block if merge-specific tests pass |
| Owner rejects manual QA | Do not merge; keep integration branch for further work |

---

## 7. Phone QA Checklist (Owner)

| # | Action | Expected |
|---|--------|----------|
| 1 | Tap **New Patient FAB** on directory | Opens **Registration Flow** (Identity step → Histories step), not old `PatientEditor` |
| 2 | Tap **header pencil** on existing patient workspace | Opens **Registration Flow** for new pregnancy? Or `PatientEditor`? **Clarify with owner** — document decision |
| 3 | Complete registration flow: Identity → Histories → Finish | Patient appears in directory with **no "Incomplete" chip**; all fields persisted |
| 4 | Back gesture during registration flow (Identity step) | Returns to directory, **no draft saved** |
| 5 | Back gesture during registration flow (Histories step) | Returns to Identity step (not directory) |
| 6 | Open **Meds tab** → tap add medication → fill → save | Medication sheet opens, saves, appears in list |
| 7 | Open **Plan tab** → tap delivery plan → fill delivered/date/mode/outcome → save | Delivery plan sheet opens, saves, patient shows **Delivered** badge |
| 8 | Directory filter → **Delivered** | Shows only delivered patients |
| 9 | Directory filter → **Incomplete** (if exists) | Shows patients with incomplete registration |
| 10 | Kill app mid-edit (Meds sheet, Plan sheet, Registration flow) → reopen | **Draft recovery** restores in-progress data |
| 11 | 360dp narrow viewport (folded/emulator) | No critical truncation: Patient Name, GA, BP, High Risk badge, Delivered headline |
| 12 | High-risk patient → directory card shows **red risk badge**; medium-risk → **amber** | `ClinicalRiskBadge` renders
[connection interrupted, retrying response from the top]
# Merge Plan: `claude/clever-goldwasser-e086ce` → `main`

## 1. Strategy: Rebase the feature branch onto `main` (on an integration branch)

**Decision:** Create `integration/registration-flow`, rebase the feature branch onto current `main`, resolve conflicts there, verify fully, then fast-forward `main` only on owner approval.

**Why not the others:**
- **Merge main→branch**: Leaves a messy history with a merge commit on the feature branch; bisectability degrades because the merge commit hides which side introduced a regression.
- **Merge branch→main directly**: Violates "never touch main until integration branch is fully green" and makes rollback harder (would need revert commit on main).
- **Rebase onto main (chosen)**: Linear history, each commit testable, easy bisect, the WIP commit can be squashed or dropped cleanly, and the owner can review the exact diff that will land on main.

**Rollback:** If anything goes wrong, `integration/registration-flow` is deleted and the original branch `claude/clever-goldwasser-e086ce` (with its worktree) remains untouched.

---

## 2. Pre-merge Safety

| Action | Command | Purpose |
|--------|---------|---------|
| Tag main | `git tag pre-merge-main-$(date +%Y%m%d-%H%M) bace905` | Immutable rollback point |
| Tag feature branch | `git tag pre-merge-feature-$(date +%Y%m%d-%H%M) claude/clever-goldwasser-e086ce` | Fallback reference |
| Run audit script | `.agents/scripts/git_audit.sh` | Confirm no stray branches, dirty worktrees, stashes |
| Snapshot worktree | `rsync -a .claude/worktrees/clever-goldwasser-e086ce/ .claude/worktrees/clever-goldwasser-e086ce.backup/` | Full file-level backup |
| Create integration branch | `git checkout -b integration/registration-flow main` | Clean base |

**Do not push `integration/registration-flow`** until the full suite passes and owner approves.

---

## 3. Conflict Resolution Plan (per file)

### `state/AncState.kt` (5 hunks) — **Highest risk**
| Hunk | Main keeps (delivery outcome) | Branch keeps (registration flow) | Rule |
|------|-------------------------------|----------------------------------|------|
| Imports | `composeDeliveryNotes` | `PreviousPregnancyFormRules`, `effectiveMedicationStatus`, `parseBloodPressure`, `parseVisitFindings`, `ClinicalRecordKind` | **Union** — both sets needed |
| State fields (1) | `medicationStopUndo`, `medicationQuickSave` | `registrationFlow`, `awaitingAdvance` | **Union** — both feature sets coexist |
| PatientDraft fields | 4 delivery fields + 8 dating fields | (branch removed 8 dating fields) | **Main's delivery fields + Branch's removal of 8 dating fields** — the 8 dating fields were dead code per branch; delivery fields are new schema v7. Keep `lmpDate`, `datingMethod`, `transferDate`, `embryoAgeDays`, `agreedEdd`, `scanDate`, `scanGaWeeks`, `scanGaDays` **only if** `intakeDatingUpdate` still uses them (check `DraftMapping.kt`). If not, delete them. |
| State fields (2) | `medicationQuickSave`, `medicationStopUndo` docs | `registrationFlow`, `awaitingAdvance` | **Union** — add docs for new fields |
| `AncEvent` sealed interface | (main has no `AncEvent` here) | Full `AncEvent` hierarchy from branch | **Branch wins** — main moved `AncEvent` elsewhere? Verify `AncViewModel.kt` auto-merge. If main already has `AncEvent` in another file, **delete the duplicate here** and keep only the branch's new events (`RegistrationNext`, `Flow`, etc.) added to the canonical location. |

**Critical:** `PatientDraftValues` must end up with: **4 delivery fields + only the dating fields still used by `intakeDatingUpdate`**. Run `grep -r "intakeDatingUpdate" --include="*.kt"` after merge to confirm.

### `storage/PatientCommands.kt` (1 hunk)
| Main keeps | Branch keeps | Rule |
|------------|--------------|------|
| `DeliveryOutcomeFields`, `normalized`, `validateDeliveryOutcome` | `PreviousPregnancyFormRules` | **Union** — both validation rule sets needed. `SavePatientProfileCommand.prepare` must validate **both** delivery outcome AND previous pregnancy. |

### `ui/AncApp.kt` (1 hunk)
| Main keeps | Branch keeps | Rule |
|------------|--------------|------|
| `medicationSheetDraft` import | `FlowEvent`, `SystemBack`, `systemBackKind`, `RegistrationFlowScaffold`, `flowMoveEvent` | **Union** — both navigation systems coexist. Verify `systemBackKind` handles sheets over tabs correctly (see semantic trap #6). |

### `ui/AncDatePicker.kt` (1 hunk)
| Main keeps | Branch keeps | Rule |
|------------|--------------|------|
| `Modifier.minimumTouchTarget()` on Today button | (branch removed it) | **Main wins** — accessibility/touch target requirement. Keep the `minimumTouchTarget()` wrapper. |

### `ui/PatientListScreen.kt` (2 hunks)
| Hunk | Main keeps | Branch keeps | Rule |
|------|------------|--------------|------|
| Data class params | `isDelivered`, `deliveryDate` | `isIncomplete` | **Union** — list item needs both delivered flag AND incomplete chip. |
| Data construction | `isDelivered = patient.delivered`, `deliveryDate = patient.deliveryDate` | `isIncomplete = isRegistrationIncomplete(patient)` | **Union** — compute both. `isRegistrationIncomplete` must be added (branch likely has it in `PatientListScreen.kt` or a util). |

### `ui/patienthub/PatientHubHeader.kt` (1 hunk)
| Main keeps | Branch keeps | Rule |
|------------|--------------|------|
| `ClinicalRiskBadge(isHighRisk = ...)` | `ClinicalRiskBadge(isHighRisk = ..., isModerate = ...)` | **Branch wins** — richer badge (adds moderate tier). Update `ClinicalRiskBadge` component to accept `isModerate` (check it doesn't break existing callers). |

### `res/values/strings.xml` (1 hunk)
| Main keeps | Branch keeps | Rule |
|------------|--------------|------|
| All delivery outcome strings (`delivery_outcome_*`, `delivered_*`, `delivery_error_*`, `patients_filter_delivered`, `delivered_patients_empty`) | All registration flow strings (`registration_step_*`, `registration_*`, `flow_*`, `registration_count_*`) | **Union** — concatenate both blocks. No key overlap. Verify no duplicate `<string name="...">`. |

### `concepts_brief.md` (modify/delete)
- **Main deleted it**, branch modified it.
- **Rule:** If the file is obsolete (replaced by `docs/designs/` or similar), **keep deleted**. If branch's changes are still relevant, resurrect it under `docs/designs/` with a new name. Check `docs/designs/` for current design docs.

---

## 4. Semantic Traps (Auto-merged files + cross-cutting)

| Trap | File(s) to Check | What to Verify |
|------|------------------|----------------|
| `PatientDraftValues` field set | `model/PatientDraftValues.kt`, `storage/DraftMapping.kt` | Final fields = **4 delivery + only dating fields still read by `intakeDatingUpdate`**. No orphan fields. |
| `intakeDatingUpdate` rule | `model/DatingRules.kt` (or wherever) | Still compiles and references only existing fields. |
| `SavePatientProfileCommand.prepare` + fingerprint | `storage/PatientCommands.kt`, `storage/CommandFingerprint.kt` | Validates **both** delivery outcome AND previous pregnancy. Fingerprint includes new fields. |
| `DraftMapping` (draft recovery) | `storage/DraftMapping.kt` | Maps all current `PatientDraftValues` fields to/from `PatientDraft`. No NPE on removed fields. |
| `startsAnotherAction` lists | `state/AncState.kt`, `ui/AncApp.kt` | Includes `RegistrationNext`, `Flow`, `StartMedicationEditor`, `StartUltrasoundEditor`, etc. Sheets over tabs work. |
| `clearsFrozenPatient` | `state/AncState.kt` | Registration flow clears frozen patient correctly on start. |
| Route handling for sheets over tabs | `ui/AncApp.kt`, `ui/patienthub/PatientWorkspaceScreen.kt` | `systemBackKind` returns correct `SystemBack` for `DeliveryPlanSheet`, `MedicationSheet`, `RegistrationFlowScaffold`. |
| Pencil target (header + New button) | `ui/patienthub/PatientHubHeader.kt`, `ui/PatientListScreen.kt` | Both emit `StartPatientEditor` with `newPatientId` → triggers registration flow (not old `PatientEditor`). |
| Strings duplicates | `res/values/strings.xml` | No duplicate keys. Run `grep -c 'name="' res/values/strings.xml | sort -u` — should be 1 per key. |
| Test files referencing removed dating fields | `*Test.kt` (23 from branch + auto-merged) | Update or delete tests that assert on the 8 removed dating fields. |

---

## 5. Ordered Steps with Verification

| Step | Action | Verification | Who |
|------|--------|--------------|-----|
| 0 | `git checkout -b integration/registration-flow main` | `git status` clean | You |
| 1 | `git rebase claude/clever-goldwasser-e086ce` | Pause at each conflict | You |
| 2 | Resolve **AncState.kt** (5 hunks) per §3 | `./gradlew :app:compileDebugKotlin` | Builder lane |
| 3 | Resolve **PatientCommands.kt** | Compile | Builder lane |
| 4 | Resolve **AncApp.kt** | Compile | Builder lane |
| 5 | Resolve **AncDatePicker.kt** | Compile | Builder lane |
| 6 | Resolve **PatientListScreen.kt** (2 hunks) | Compile | Builder lane |
| 7 | Resolve **PatientHubHeader.kt** | Compile | Builder lane |
| 8 | Resolve **strings.xml** | `aapt2 dump resources` shows no dup keys | Builder lane |
| 9 | Resolve **concepts_brief.md** | File either gone or moved to `docs/designs/` | You |
| 10 | **Semantic audit** of auto-merged files (DemoModels, AncViewModel, DraftMapping, PatientPrintHtmlGenerator, ClinicalPresets, EncryptedRecordsTest, AncWorkflowTest) | Compile + spot-check diffs | Reviewer lane (different model) |
| 11 | **Full compile** | `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` | Builder lane |
| 12 | **Unit tests** | `./gradlew :app:testDebugUnitTest` (must be 0 failures) | Builder lane |
| 13 | **Lint** | `./gradlew :app:lintDebug` (no new errors) | Builder lane |
| 14 | **Targeted emulator tests** (registration flow, delivery outcome, Meds/Plan sheets) | Run specific test classes: `RegistrationFlowTest`, `DeliveryOutcomeTest`, `MedicationSheetTest`, `PlanSheetTest`, `PatientListScreenTest` | Builder lane |
| 15 | **Full emulator suite** (456 tests, ~9 min) | `./run_emulator_tests.sh` (or your script) — **0 failures** | Builder lane |
| 16 | **Owner phone QA** (checklist §7) | Owner taps through on device | Owner |
| 17 | **Squash WIP commit** (last commit on branch) | `git rebase -i HEAD~2` → squash WIP into previous | You |
| 18 | **Final diff review** | `git diff main...integration/registration-flow` | Reviewer lane (different model) |
| 19 | **Owner approval to push** | Owner says "push" | Owner |
| 20 | **Fast-forward main** | `git checkout main && git merge --ff-only integration/registration-flow && git push` | You (only on owner go) |

**Builder vs Reviewer lanes:** Builder = model that writes code (proposes SEARCH/REPLACE). Reviewer = different model family that reads the diff and signs off. They must not be the same model.

---

## 6. Stop Conditions (Abandon & Fall Back)

| Condition | Action |
|-----------|--------|
| Any unit test fails after conflict resolution and cannot be fixed in <30 min | `git reset --hard pre-merge-main-<tag>`, delete `integration/registration-flow`, keep feature branch intact |
| Lint introduces new errors in `AncState.kt`, `PatientCommands.kt`, or `DraftMapping.kt` | Same rollback |
| Full emulator suite has >0 failures unrelated to flakiness (re-run once to confirm) | Same rollback |
| Owner phone QA reveals data loss, crash, or safety rule violation (e.g., delivery outcome not saved, registration flow loses draft) | Same rollback |
| `PatientDraftValues` fingerprint mismatch on cold start (draft recovery broken) | Same rollback |
| Reviewer lane refuses sign-off on semantic audit (§5 step 10) | Pause, do not push; resolve or rollback |

**Fallback:** The original branch `claude/clever-goldwasser-e086ce` and its worktree `.claude/worktrees/clever-goldwasser-e086ce` are never touched. The tagged `pre-merge-main-<tag>` is the clean main.

---

## 7. Owner Phone QA Checklist (Post-merge, Pre-push)

| Flow | Steps | Must Work |
|------|-------|-----------|
| **Registration — Happy path** | Tap **New patient** → Identity step (name, age, gravida) → Next → Histories step (prev pregnancy count, outcome, delivery mode, dating method) → Finish | Patient appears in directory with **Incomplete** chip until finished; after finish, chip gone; age calculated correctly; back gesture at each step returns to previous step, not to directory |
| **Registration — Back gesture** | At Identity step: back → directory<br>At Histories step: back → Identity step | No crash; draft preserved if navigated away and returned |
| **Pencil (edit) on existing patient** | Tap header pencil on a patient with delivery outcome filled | Opens **registration flow** (not old `PatientEditor`); delivery outcome fields visible and editable |
| **Delivery outcome** | Open patient → Plan tab → pencil → fill Delivered, date, mode, outcome → Save | Patient moves to **Delivered** filter on directory; headline shows "Delivered <date>"; dashboard Delivered filter shows only delivered patients |
| **Meds sheet** | Open patient → Meds tab → add medication → stop medication → undo stop | Stop/undo notice persists after failed save; route locked after first save |
| **Plan sheet** | Open patient → Plan tab → open delivery plan sheet → fill → save | Sheet overlays tabs correctly; back gesture closes sheet, returns to Plan tab |
| **Draft recovery** | Kill app mid-registration → reopen → continue | All entered fields restored |
| **High-risk badge** | Patient with severe BP / high risk tier | Badge shows on directory card and header (moderate tier also shown per branch) |

---

## 8. Risks Ranked + Missing Info Needed

| Rank | Risk | Mitigation |
|------|------|------------|
| 1 | **PatientDraftValues field mismatch** — 8 dating fields removed on branch, 4 delivery fields added on main; `DraftMapping` and `intakeDatingUpdate` may reference stale fields | **Need to see:** `model/PatientDraftValues.kt`, `storage/DraftMapping.kt`, `model/DatingRules.kt` (or wherever `intakeDatingUpdate` lives) |
| 2 | **AncEvent duplication** — branch defines full `AncEvent` in `AncState.kt`; main may have moved it to `AncViewModel.kt` (auto-merged) | **Need to see:** `state/AncEvent.kt` (if exists) or `ui/AncViewModel.kt` post-auto-merge |
| 3 | **SystemBack for sheets over tabs** — `systemBackKind` must handle `RegistrationFlowScaffold` + `DeliveryPlanSheet` + `MedicationSheet` simultaneously | **Need to see:** `state/AncState.kt` `systemBackKind` implementation, `ui/AncApp.kt` navigation graph |
| 4 | **Test coverage gap** — 23 test files on branch; auto-merged `EncryptedRecordsTest.kt`, `AncWorkflowTest.kt` may test removed dating fields | **Need to see:** All `*Test.kt` diffs in the merge result |
| 5 | **ClinicalRiskBadge API change** — branch adds `isModerate` param; existing callers (if any outside header) may break | **Need to see:** `ui/components/ClinicalRiskBadge.kt` and all call sites |
| 6 | **Schema v7 migration** — delivery outcome columns added; branch doesn't touch schema. Ensure Room DAO queries include new columns. | **Need to see:** `storage/PatientDao.kt`, `storage/AppDatabase.kt` (schema version) |
| 7 | **String resource collision** — low probability but verify | Already covered in step 8 |

**Files not pasted that I need to review before merge:**
1. `app/src/main/java/com/sherifradwan/ancnative/model/PatientDraftValues.kt`
2. `app/src/main/java/com/sherifradwan/ancnative/storage/DraftMapping.kt`
3. `app/src/main/java/com/sherifradwan/ancnative/model/DatingRules.kt` (or wherever `intakeDatingUpdate` is)
4. `app/src/main/java/com/sherifradwan/ancnative/state/AncEvent.kt` (if separate file) OR `ui/AncViewModel.kt`
5. `app/src/main/java/com/sherifradwan/ancnative/ui/components/ClinicalRiskBadge.kt`
6. `app/src/main/java/com/sherifradwan/ancnative/storage/PatientDao.kt`
7. `app/src/main/java/com/sherifradwan/ancnative/storage/AppDatabase.kt`
8. All `*Test.kt` files changed in the merge (especially `AncWorkflowTest.kt`, `EncryptedRecordsTest.kt`)

---

**End of plan.** The integration branch `integration/registration-flow` is the single gate; nothing lands on `main` until steps 0–18 are green and owner approves at step 19.
[Tokens] upload: 12379 download: 4875 cache_read: 0 cache_write: 0

