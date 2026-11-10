# Design: S5b (remove the dead intake-dating path) and S6 (registration UI), revision 2

* **Author:** planner, 2026-11-10. Revision 2 after three reviews (`.agents/fleet-runs/s6_plan_opus.txt`, `s6_plan_argon.txt`, `s6_plan_alphacode.txt`) and the owner's answers. Read-only planning; nothing here is built.
* **Inputs:** `.agents/SPEC_REGISTRATION_FLOW.md` (owner changes at the bottom win), `.agents/DESIGN_S5_REGISTRATION_DRIVER.md` (commit 7f16290), approved mock `.agents/mocks/registration_flow.html` (the .png was not opened).
* **Owner decisions (2026-11-10):** Back means previous step, for both the footer and the system gesture. On step 1 the footer Back is disabled; the gesture on step 1 leaves through the discard dialog. The dead draft dating fields are removed as a separate commit (S5b-2). Segment status words are hidden above font scale 1.3 (glyph and screen-reader text stay). No "Reason:" line now. No extra exit control.
* **Convention:** `file:line` means I read that line. Paths are under `app/src/main/java/com/sherifradwan/ancnative/` unless they start with `.agents`, `app/src/test` or `app/src/androidTest`. UNVERIFIED means not checked.
* **Risk levels (AGENTS.md: `app/src` is the highest-risk area; I did not load `PROJECT_MAP.json`):** treat every edit to `state/`, `storage/` and `AncApp.kt` as level 3. Level 3 needs a frontier-tier review and an independent test review inside the gate of the sub-slice that makes the edit (section 11).

## Part 1. S5b: remove the dead intake-dating path

### Why it is dead
The patient form has no dating fields (`ui/PatientEditor.kt:139-190`: name, phone, age, address; ObstetricFields has no LMP). The only caller of `intakeDatingUpdate` is `AncViewModel.kt:506`. For a new patient the draft `lmpDate` is blank, so the rule returns null (`IntakeDatingRule.kt:54`). For an edit the draft is seeded with the stored LMP (`AncState.kt:1821`), so it returns null on "same LMP" (`IntakeDatingRule.kt:32`). The second `controller.execute` for dating (`AncViewModel.kt:508-527`) therefore cannot run from the UI. One correction to "dead": draft recovery restores `lmpDate` and the other dating keys (`DraftMapping.kt:378-385`), so an old recovered profile draft could still drive the rule and overwrite newer dating. Removing the path closes that hole, so the deletion is a fix as well as a cleanup. Dating is now written only by the Dating editor (flow step 2); the spec clause "an empty lmpDate saves no dating" holds by construction.

### S5b-1: one commit that compiles alone
It must delete the ViewModel block and `IntakeDatingRule.kt` together (the file's `profileSavedNotice` is called at `AncViewModel.kt:526`).
1. Delete `state/IntakeDatingRule.kt` (146 lines): `IntakeDating`, `intakeDatingUpdate`, `IntakeDatingPreview`, `intakeDatingPreview` (no caller outside the file and its test), `profileSavedNotice`.
2. `AncViewModel.kt:503-528`: delete the `if (command is SavePatientProfileCommand) { ... }` block. Keep `val patients = result.snapshot.patients` and `val notice = command.savedNotice()` (`:501-502`, `var` to `val`). `SaveDatingCommand` and `ClinicalCommandOutcome` imports stay (used at `:561`, `:756`).
3. Delete `SavedNotice.PATIENT_DATING_NOT_UPDATED` (`AncState.kt:223`, mapping `:252`) and `<string name="patient_saved_dating_not_updated">` (`strings.xml:143`). Reason: its only producer was `profileSavedNotice`; unreachable values with user text are legacy baggage (AGENTS.md section 3).
4. Tests: delete `IntakeDatingRuleTest.kt` and `ProfileSavedNoticeTest.kt`; edit `SavedNoticeTextTest.kt:14` (drop the map entry; `everyNoticeMapsToItsOwnString` compares the enum to the map keys, so a stale side fails). The engine rules those tests touched are covered elsewhere: `GestationalAgeEngineTest.kt` exists and `DueDatesByMethodTest.kt:50-85` covers the IVF, LMP, Ultrasound and fallback branches of `dueDateForMethod` (per Opus review; I did not open them). Nothing to move.
5. `model/IsoDates.kt` is NOT touched. `eddFromLmp` (`IsoDates.kt:9`) and `eddFromScan` (`:13`) live there; `GestationalAgeEngine.kt:69` still calls `eddFromLmp`. Checks r3i S53 and r4f S160 (which require those names and one engine call) stay unchanged and green.
6. No ViewModel JVM harness exists (`AncViewModel(` is constructed nowhere in `app/src/test`; per Opus review, UNVERIFIED by me). So the oracle for "a profile save issues one command" is static check S1a (below) plus the existing `PatientCommands` tests. I will not claim a JVM test.

### Static checks (each still able to fail)
* `check_r3.py` S1a (`:46`, asserted the VM calls `intakeDatingUpdate(`) now asserts `AncViewModel.kt` contains neither `intakeDatingUpdate` nor `profileSavedNotice` and `state/IntakeDatingRule.kt` does not exist. S1b (no `plusDays(280)`) unchanged. Docstring line 6 updated.
* `check_r3i.py` S54 (`:80-87`) looked at `IntakeDatingRule.kt`. Repoint the loop to `state/DatingDraftRule.kt`, which calls `dueDateForMethod(` (`DatingDraftRule.kt:30`) and so passes the existing "any of eddFromLmp(, FULL_TERM_DAYS, dueDateForMethod(, intakeDatingPreview(" test; drop `intakeDatingPreview(` from that list so the check cannot pass on a name that no longer exists. Adds: `IntakeDatingRule.kt` must not exist. S51-S53 unchanged.
* `check_r4g.py`: S172 becomes "`intakeDatingUpdate` and `intakeDatingPreview` are defined nowhere in main" (uses the existing `defined_in`). S173 (no hard-coded LMP in the VM, `:101-108`) can no longer fail on its current patterns, so repoint it to "`AncViewModel.kt` contains no `datingCmd`", which fails if the block comes back. S171 and S174 stay in S5b-1 because the fields still exist; S5b-2 replaces them.
* `check_r3l.py` S66 (`:168-201`) now asserts: no `profileSavedNotice` and no `PATIENT_DATING_NOT_UPDATED` in main, no `patient_saved_dating_not_updated` in strings.xml, and the fire-and-forget `execute(datingCmd, ...) {}` stays absent. S64, S65 unchanged. Docstring lines 17-22 updated.
* `.agents/TEST_MANIFEST.json`: remove entries `UNIT-intake-dating` (`:130-141`) and `UNIT-profile-saved-notice` (`:861-872`); remove `"app/src/main/**/state/IntakeDatingRule.kt"` from `STATIC-r3i.deps` (`:769`); in `UNIT-r4g` (`:1657-1663`) remove the `--tests '...IntakeDatingRuleTest'` argument from `cmd` (Gradle fails with "No tests found" otherwise) and the `IntakeDatingRuleTest.kt` dep; the same entry also loses `PatientDatingDraftTest` in S5b-2. Add manifest entries for every new test in Part 2 (ids `UNIT-registration-step-text`, `UNIT-registration-flow-ui-logic`, `ANDROID-registration-flow-ui`). `PROJECT_MAP.json` is regenerated by `scripts/project_map.py`; `EVIDENCE.json` is history, leave it.
* Gate: `run_static_checks.sh` (prints FAIL for some "no logic file changed" checks until committed, per its header: judge by which scripts fail, not by exit code alone), then `run_build_gate.sh`.

### S5b-2: eight dead draft fields (separate commit, owner yes)
Eight fields, not seven (`lmpDate` plus the seven in r4g's `FIELDS`): `PatientDraftValues.lmpDate..scanGaDays` (`AncState.kt:468-475`), `PatientDraftField.LMP_DATE..SCAN_GA_DAYS` (`:101-108`), reducer branches (`:1583-1590`), the seed (`:1821`), and in `DraftMapping.kt` the save keys (`:107` and the new-patient-only block `:110-119`) and restore lines (`:378-385`). Not the `DatingDraftValues` lines (`DraftMapping.kt:202,448`, `AncState.kt:548,1839`), which stay.
* Tests that stop compiling and must change in this commit: `RegistrationFlowStateTest.kt:760-763` (`obstetricValues.copy(lmpDate = "")` becomes `assertEquals(obstetricValues, draft.values)`), `DraftMappingTest.kt:62` (constructor arg), `:137` (drop the `lmpDate` assertion), `:139` (field count 14 becomes 13; the new-patient count drops by 8), `PatientDatingDraftTest.kt` (delete; remove from `UNIT-r4g`).
* Checks: r4g `FIELDS`/`ENUMS` (`:25-26`) and S171/S174 become "`PatientDraftValues` and `PatientDraftField` contain none of the eight names and `DraftMapping.kt` does not mention them".
* `eddFromScan` and `EddFromScanTest` are removed (dead, no callers); Q270 is retired; Q271 and Q272 are retired with the fields.
* Old unsaved drafts still load: restore reads known keys by name (`DraftMapping.kt:360-385`, `record.fields[key] ?: default`), and `DraftCodec` stores a generic string map (`DraftCodec.kt:8`, `RecordsRepository.kt:1668`); extra keys are therefore ignored. Strict rejection in `decodeDraftFields` is UNVERIFIED. New test in `DraftMappingTest`: a PATIENT_PROFILE record whose `fields` still contain `lmpDate`, `datingMethod`, `scanGaDays` etc. restores with every kept field intact and no error. Add the same case through `encodeDraftFields`/`decodeDraftFields` if that function rejects unknown keys.

## Part 2. S6: registration UI

### What the driver gives the UI (read)
* Events `AncEvent.Flow(event, editorSessionId, newPregnancyId)` and `AncEvent.RegistrationNext(editorSessionId, newPregnancyId)` (`AncState.kt:726-727`), dispatched through `AncViewModel.dispatch` (`:192`, default branch `:259-264`). The UI generates UUIDs in composables already (`OverviewTab.kt:38-52`), so AncApp keeps its signature.
* `AncViewModel.registrationNext()`, `flowMove()` and `flowIds()` (`:351-357`) have no caller. Delete them (one id generator, in the UI).
* Next: `reduceRegistrationNext` stores `awaitingAdvance`; `AncViewModel.registrationNext` (`:363-374`) then runs the editor's own save. Skip, Back, Begin, count: `reduceFlowEvent` (`RegistrationFlowDriver.kt:120-142`); Back on step 1 goes to `requestBack` (`:133`).

### Driver fix: Next on an untouched, not-done Identity step (BLOCKER from review, level 3)
Problem, checked in the code: `reduceRegistrationNext` stores `awaitingAdvance` only when `state.hasDirtyDraft || flow.current == PREGNANCIES` (`RegistrationFlowDriver.kt:155`). A new patient's draft is untouched (`original == values`), and so is a legacy patient resumed from the hub with no age. Next then calls `applyFlowMove(Next(hasData = false))`; `reduce` returns the flow unchanged for Identity (`RegistrationFlow.kt:84`), `isNoOp` is true (`:96`), nothing happens and no error shows. With Back disabled and Skip hidden on step 1 the user is stuck. The S5-4 test pins this behaviour (`RegistrationFlowStateTest.kt:337-344`).
Smallest fix, in `RegistrationFlowDriver.kt:155`: `if (state.hasDirtyDraft || flow.current == PREGNANCIES || (flow.current == IDENTITY && !hasStepData(state, flow)))`. The reducer then records `awaitingAdvance`, the ViewModel runs `savePatientProfile()` (`AncViewModel.kt:367-369`), `prepare` rejects (blank name and/or age, with `forceAgeRequired` from `AncState.forcesAgeRequired()`, `RegistrationFlowDriver.kt:42`), `PatientValidationFailed` shows the errors and the post-pass clears `awaitingAdvance` (`:180-187`). A valid draft with data still takes the clean path (`hasStepData` true).
Oracles (JVM), replace and add:
* S5-4 edited: Next on an untouched new-patient draft, and on a stored patient with no age, now sets `awaitingAdvance` for the active session and leaves flow and route unchanged (was: state equals input).
* S6-1: with that state, `SavePatientProfileCommand.prepare(state, ..., forceAgeRequired = state.forcesAgeRequired())` (`PatientCommands.kt:54-85`) returns `Rejected` with `FULL_NAME_REQUIRED` and `AGE_REQUIRED` for the new patient and `AGE_REQUIRED` for the legacy one; then `reduce(state, PatientValidationFailed(PROFILE, errors))` gives `patientDraft.errors` containing them and `awaitingAdvance == null`.
* S6-2: a stored patient with name and age and an untouched draft still advances without a save (`hasData` true), unchanged from S5-4.
Carried by sub-slice S6a-0 (section 11). Other steps are not affected: Next on a clean non-Identity step with no data is "skipped" and advances (`RegistrationFlow.kt:85`).

### 1. New files
Package `com.sherifradwan.ancnative.ui.components` (new directory `ui/components/`, named in AGENTS.md). All composables are stateless.

| File | Size | Contents |
|---|---|---|
| `ui/components/RegistrationStepHeader.kt` | about 140 lines | `RegistrationStepHeader(flow: RegistrationFlow, compact: Boolean, modifier: Modifier = Modifier)` |
| `ui/components/RegistrationStepFooter.kt` | about 110 lines | `RegistrationStepFooter(flow: RegistrationFlow, backEnabled: Boolean, skipEnabled: Boolean, nextEnabled: Boolean, nextIsFinish: Boolean, onBack: () -> Unit, onSkip: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier)` |
| `ui/components/RegistrationFlowScaffold.kt` | about 100 lines | `RegistrationFlowScaffold(state: AncState, onEvent: (AncEvent) -> Unit, nextEnabled: Boolean = true, nextIsFinish: Boolean? = null, onNext: (() -> Unit)? = null, content: @Composable () -> Unit)`; `internal fun flowMoveEvent(move: FlowEvent): AncEvent.Flow`; `internal fun registrationNextEvent(): AncEvent.RegistrationNext` (the only id helper) |
| `ui/components/RegistrationStepText.kt` | about 50 lines | resource mapping (section 5) |
| `ui/PregnancyCountPrompt.kt` | about 120 lines | `PregnancyCountPrompt(state: AncState, route: AncRoute.PregnancyCountPrompt, onEvent: (AncEvent) -> Unit)` |
| `ui/patienthub/components/ContinueRegistrationCard.kt` | about 50 lines | `ContinueRegistrationCard(patient: DemoPatient, onContinue: () -> Unit)` |

Header (input: `RegistrationFlow`; output: none):
* Title line `Text(headerText)` with no `maxLines`, so it wraps (RF11). `heading()` and `liveRegion = Polite` go on this `Text` only, not the container.
* Segment row (hidden when `compact` is true, or when `LocalDensity.current.fontScale > 1.3f`, see section 6): five segments, `weight(1f)`, each a bar (solid for Done, dashed for Skipped, hollow for To do, taller for current), a glyph, the status word, and under the current one the word "Current".
* Each segment: `semantics(mergeDescendants = true) { contentDescription = "<Title>, <status>" or "<Title>, <status>, current"; selected = isCurrent }`, tag `registrationSegment-<1..5>`. The glyph `Text` alone uses `clearAndSetSemantics {}`, so it is not read as "check mark"; the segment's own tag is not on that node.
* In the pregnancy walk it adds the line "Pregnancy X of Y to add" (`pregnancyIndex + 1`, `pregnancyTarget` is the remaining count, `RegistrationFlow.kt:100-102`).
* Container tag `registrationStepHeader`.
* Reuse check (reviewer point): `SegmentedProgressBar` (`DesignSystem.kt:233`, used by the visit wizard `VisitEditor.kt:341-355`) is a clickable two-state tab bar (done-or-current, `onStepClick`), `BatchSegmentedProgress` (`DesignSystem.kt:453`) is for batches. The registration header is read-only (steps move only by Back, Skip, Next), has three statuses with a glyph and a word, and must not be tappable, so it is a separate component. It reuses the string `step_n_of_m` (`strings.xml:517`) for "Step 2 of 5" and joins the title with a new `registration_header` format "%1$s: %2$s".

Footer:
* `FlowRow` from the start (not a `Row`), so Next wraps instead of being squeezed at large font; order Back, then (weight spacer), Skip, Next; each button `minimumTouchTarget()` (`Components.kt:395`). Tags `flowBack`, `flowSkip`, `flowNext`. `FlowRow` item weights need the BOM in `app/build.gradle.kts:73` (2025.12.01) to support them (UNVERIFIED); if not, drop the spacer and use `Arrangement.End` with Back first on its own line when wrapping.
* Separate flags (BLOCKER from review): `backEnabled = !blocksEditing && flow.current != IDENTITY`, `skipEnabled = !blocksEditing` (shown only when `showsSkip(flow)`), `nextEnabled = !blocksEditing && callerNextEnabled`. An invalid count disables only Next.
* Label: "Finish" when `nextIsFinish`, else "Next" (existing `next_step`, `strings.xml:542`). Pure rule in `state/RegistrationFlow.kt`: `forwardIsFinish(flow, remainingOnPrompt: Int? = null)`: true when `current == PREGNANCIES && pregnancyTarget != null && pregnancyIndex + 1 >= pregnancyTarget`; on the count prompt (`pregnancyTarget == null`) true only when `remainingOnPrompt == 0` (the typed count gives `remainingPregnancies(n, existing) == 0`, which exits at `RegistrationFlow.kt:101`). Mock D shows Finish on "pregnancy 2 of 3"; that contradicts `PregnancyCommitted` (`:104-113`), so the code rule wins and the mock needs a small correction.

Scaffold:
```
val flow = state.activeFlowForRoute() ?: return content()
Column(fillMaxSize) {
  RegistrationStepHeader(flow, compact = WindowInsets.isImeVisible)
  Box(Modifier.weight(1f)) { key(flow.activeEditorSessionId, flow.current) { content() } }
  RegistrationStepFooter(...)   // pinned, outside the scroll
}
```
* One helper, defined once in `state/RegistrationFlowDriver.kt` beside `forcesAgeRequired` (`:42`): `fun AncState.activeFlowForRoute(): RegistrationFlow? = registrationFlow?.takeIf { route.matchesStep(it.current, it.patientId) }`. The scaffold, the four editors and the system Back rule all call it, so they cannot disagree (review point: zero buttons when route and flow mismatch).
* `key(activeEditorSessionId, current)` resets remembered state and the `rememberScrollState()` in `EditorPage` (`PatientEditor.kt:331,355`) between Identity and Obstetric (same composable) and between pregnancies. `activeEditorSessionId` changes on every move and on draft recovery (`withFlowSessionFollowing`, `RegistrationFlowDriver.kt:174-178`).
* `check_r4d.py` S141 (`:84-96`) pins that only Lab and Medication pass `bottomBar` to `EditorPage`. The footer is outside `EditorPage`, so that check is not touched.

### 2. AncApp wiring and editors
AncApp (`AncApp.kt:215-248`; the placeholder branch `:225-232` is deleted):
```
is AncRoute.PatientEditor -> RegistrationFlowScaffold(state, onEvent) { PatientEditor(state, route, onEvent) }
is AncRoute.DatingEditor -> ... same
is AncRoute.OtherHistoriesEditor -> ... same
is AncRoute.PreviousPregnancyEditor -> ... same
is AncRoute.PregnancyCountPrompt -> PregnancyCountPrompt(state, route, onEvent)   // builds its own scaffold
```
Outside a flow the scaffold renders `content()` alone, so hub-started edits are unchanged. In S6a the `PregnancyCountPrompt` branch keeps the hub placeholder until S6b.

System Back (`AncApp.kt:89-91`, today `if (state.showDiscardDialog) KeepEditing else Back`). Exact order, as one pure function in `RegistrationFlowDriver.kt`: `fun systemBackKind(state): SystemBack` returning `KEEP_EDITING` if `state.showDiscardDialog`; else `FLOW_BACK` if `state.activeFlowForRoute() != null`; else `BACK`. AncApp maps: `KEEP_EDITING -> AncEvent.KeepEditing`, `FLOW_BACK -> flowMoveEvent(FlowEvent.Back)`, `BACK -> AncEvent.Back`. The discard dialog must win (otherwise Back while the dialog is open would re-open it instead of dismissing it). On step 1 `Flow(Back)` is `requestBack` (`RegistrationFlowDriver.kt:133`): clean draft leaves to the list or hub, dirty draft shows the discard dialog (owner's answer). The `BackHandler` `enabled` expression (`:89`) is unchanged.
Tests: JVM `systemBackKind` cases (dialog open with a flow gives KEEP_EDITING; flow editor with no dialog gives FLOW_BACK; hub with a stale `registrationFlow` and a non-matching route gives BACK; no flow gives BACK). androidTest (event capture): dialog open then back gesture emits `KeepEditing`; flow editor then gesture emits `Flow(Back)`; hub then gesture emits `Back`.

Editors (each keeps its own save path; Next reaches it through `AncViewModel.registrationNext`):
* `PatientEditor.kt:70-113`: `val flow = state.activeFlowForRoute()`. Groups to show come from one pure function `patientEditorGroups(step: RegistrationStep?, errors: Set<PatientValidationError>): Set<Group>` (ui package, JVM-testable): outside a flow both groups; in IDENTITY the Identity group, in OBSTETRIC the Obstetric group, plus any hidden group that owns an error in `errors` (Identity owns `FULL_NAME_REQUIRED`, `AGE_REQUIRED`; Obstetric owns `GPA_ALL_OR_NONE`, `INVALID_GPA`, `INVALID_BLOOD_GROUP`, `INVALID_RISK_LEVEL`). What the user sees if a hidden field fails validation (review point; `prepare` validates the whole profile): the hidden group appears below the current one with its red error, exactly as outside the flow, so the error is never invisible. Errors that belong to no field (`CASE_ID_*`, `STALE_REVISION`, `SAVE_FAILED`) already show in the generic card (`Components.kt:430-455`). The photo notice (`:87-104`) shows only when the Identity group is shown. `EditorActions` (`:106-111`) is hidden when `flow != null`. `PatientDraft` and `SubmitPatientProfile` are untouched.
* `DatingEditor.kt:316-320`, `OtherHistoriesEditor.kt:242-246`, `PreviousPregnancyEditor.kt:229-233`: hide `EditorActions` when `state.activeFlowForRoute() != null`.
* `EditorPage` (`PatientEditor.kt:311`) gets `showHeader: Boolean = true`; the four editors pass `showHeader = (flow == null)` so the big headline and subtitle (about 80dp, "Edit patient" on step 3 of a new registration because `isNew` is false after the first save, `PatientEditor.kt:79`) disappear in the flow and only the step header names the step. `check_r4d.py` S141 regex still finds `bottomBar ... = null` in the signature. In flow the tags `patientEditor`, `datingEditor`, `otherHistoriesEditor`, `previousPregnancyEditor` (carried by the page header) are absent; the only test using one is `AncWorkflowTest.kt:520` (`datingEditor`, hub-started) and is unaffected.
* Tags kept outside the flow: `savePatient`, `saveDating`, `saveHistories`, `savePregnancy`.

### 3. Hub "Continue registration"
* Where: first child of `OverviewTab`'s column (`OverviewTab.kt:65-72`), as `ContinueRegistrationCard`. Props already there: `patient`, `canEdit`, `onEvent` (`:33-40`).
* Shown when `canEdit && isRegistrationIncomplete(patient)` (`RegistrationFlow.kt:154`). `canEdit` is the hub's `canModifyData` (`PatientHubScreen.kt:142`: not archived, no dirty draft, no save running, no delete running). Cases (RF15): incomplete and editable shows; complete hides; archived hides (the function returns false); incomplete with a dirty draft or a save running hides.
* Event: `onEvent(flowMoveEvent(FlowEvent.Begin(patient.id, step)))` with `step = firstNotDoneStep(patient) ?: return`. `firstNotDoneStep` is a new pure function in `RegistrationFlow.kt`: first step in enum order whose `deriveStepStatuses` value is not DONE, null if all done. The driver refuses a Begin when all are done (`RegistrationFlowDriver.kt:113`). Because the card shows only when IDENTITY, DATING or OBSTETRIC is not DONE, the hub can never produce PREGNANCIES (a reducer-only case).
* A step skipped earlier is "not done" again on resume (derived statuses are DONE or PENDING only, `RegistrationFlow.kt:149`): intended.
* Text: tag `continueRegistration` (button), `registrationResumeCard` (card); body "Registration is not finished. Resume at: %1$s." (no per-reason text, owner Q5).

### 4. Count prompt
* Route `AncRoute.PregnancyCountPrompt` (`AncState.kt:51`). Screen = own `RegistrationFlowScaffold`; the body is `Column(fillMaxSize, imePadding, verticalScroll)` so it scrolls with the keyboard at 360dp and at font scale 1.4.
* Field: `EditorField` with `singleLine = true` and `KeyboardType.Number` (`Components.kt:398-420`; without `singleLine` it forces `minLines = 3`), tag `pregnancyCount`. State: `rememberSaveable(route.patientId, patient.gravida, patient.parity, patient.abortion)`, default `suggestedPregnancyCount(g, p, a).toString()` (`RegistrationFlow.kt:127-130`), so going Back to step 3, changing G/P/A and returning recomputes the default.
* `parsePregnancyCount(text): Int?` (pure, `RegistrationFlow.kt`): trim, `toIntOrNull`, null outside 0..20. Invalid: `nextEnabled = false` and the field error "Enter a number from 0 to 20."; Back and Skip stay enabled.
* Next dispatches `flowMoveEvent(PregnancyCountChosen(n))` via the scaffold's `onNext` (the driver fills the existing count, `RegistrationFlowDriver.kt:126-130`). `RegistrationNext` is not used here: it is ignored without a draft session (`:152`). The label is Finish when `forwardIsFinish(flow, remainingPregnancies(n, existing)) ` is true. Skip exits the flow (last step); Back returns to Histories (`RegistrationFlow.kt:94`).
* Tags `pregnancyCountPrompt`, `pregnancyCount`.

### 5. Strings
Only `res/values/strings.xml` exists (plus `values-v31`); no locale file. New strings: English only, in that file. Reuse, no new key: `step_n_of_m` (`:517`), `next_step` (`:542`), `back_step` (`:543`), `done` (`:545`, the Done status word), `obstetric_risk_section_title` (`:120`, step 3 title), `previous_pregnancies` (`:162`, step 5 title), `registration_incomplete_chip` (`:390`). New: `registration_step_identity`, `registration_step_dating`, `registration_step_histories`, `registration_header` ("%1$s: %2$s"), `registration_status_skipped`, `registration_status_pending` ("To do"), `registration_current`, `registration_segment_cd` ("%1$s, %2$s"), `registration_segment_current_cd` ("%1$s, %2$s, current"), `registration_pregnancy_progress`, `flow_skip`, `flow_finish`, `registration_count_title`, `registration_count_label`, `registration_count_hint`, plural `registration_count_existing`, `registration_count_error`, `registration_resume_body`, `registration_resume_button`.
Moving the hard-coded names out of `RegistrationFlow.kt` (`:6-18`, `:56-59`): delete the enum `title`/`label` properties and `registrationHeaderText`/`stepStatusText`; add `val registrationStepCount = RegistrationStep.entries.size` (no such constant exists today; `STEPS` is private, `:52,57`). Mapping lives in `ui/components/RegistrationStepText.kt`: `@StringRes RegistrationStep.titleRes()`, `@StringRes StepStatus.labelRes()`, `StepStatus.glyph()` (`✓`, `−`, `○`), `@Composable registrationHeaderText(flow)`. The pure file stays free of `R`; JVM tests read `R.string.*` the way `SavedNoticeTextTest` does. "Pending" becomes "To do" (the mock). Tests that call the removed functions (`RegistrationFlowTest.kt:25,32`) change in the same commit.

### 6. 360dp rules
* Header title wraps (no `maxLines`). Segments are about 58dp wide ((360 - 32 - 24 - 16) / 5): "Skipped" at 11sp fits at font scale 1.0. Above `fontScale 1.3` the segment row's words are hidden (owner Q4); the 1.3 cutoff is a guess ("Skipped" at 11sp x 1.3 is about 50dp in 58dp), so RF13 also runs at exactly 1.3 and at 1.4.
* Keyboard (review point): while the IME is visible the segment row is hidden (`compact`) and only the title line stays, using `WindowInsets.isImeVisible` (needs `@OptIn(ExperimentalLayoutApi::class)`; whether it sees the IME after AncApp's `safeDrawing` padding, `AncApp.kt:99-102`, is UNVERIFIED and is what the RF11 keyboard variant proves). Header title (about 24-48dp) + footer (about 64dp) leave the field room on 640dp.
* Tap targets 48dp for Back, Skip, Next and the resume button. Next is reachable with no scrolling because the footer is pinned outside the scroll area.
* Footer is a `FlowRow` from the start; RF12 runs at font scale 1.0, 1.4 and 2.0.
* RTL: start and end arrangements only; the existing `arabicRtl` fixture (`AncWorkflowTest.kt:995-1000`) mirrors them.

### 7. Accessibility
Done, Skipped and To do differ by glyph, by word and by bar style; colour is decoration. The current step is spoken in the segment description and `selected`, and the title line is a polite live region. Above 1.3 font scale the words are hidden but glyph and the description stay. Dark-theme contrast of the new elements is UNVERIFIED (only theme tokens are used; the `darkTheme` fixture runs RF11 and RF13).

### 8. Reuse check
Reused as is: `IdentityFields`/`ObstetricFields`, `EditorField`, `minimumTouchTarget`, `ClinicalTagBadge` (chip), `SectionCard`-style card from `DesignSystem.kt:49`, `isRegistrationIncomplete`, `deriveStepStatuses`, `suggestedPregnancyCount`, `remainingPregnancies`, `FlowIds`, `blocksEditing`, strings listed in section 5, `ComposeTextAsserts.kt` helpers. Not reused, with the one-line reason: `SegmentedProgressBar` (clickable, two states; see Header). One header, one footer, one scaffold, one id helper, one "flow active for this route" helper, one step-to-text mapping, one count parser, one Finish rule.

### 9. Tests
JVM:
* `RegistrationFlowTest.kt`: replace `:25,:32` with `registrationStepNumber` and the title resource; add `firstNotDoneStep` (name and age but no dating gives DATING; no age gives IDENTITY; complete with no histories gives HISTORIES; all done gives null), `forwardIsFinish` (false on steps 1-4; false on the prompt with `remainingOnPrompt` null or 2; true on the prompt with 0; false on pregnancy 1 of 3; true on 3 of 3), `showsSkip`, `parsePregnancyCount` ("2", " 7 ", "0", "20" valid; "21", "-1", "", "x" null).
* `ui/components/RegistrationStepTextTest.kt` (new): five distinct step resources (steps 3 and 5 equal the two reused resources); three distinct status resources; three distinct glyphs. The glyph is asserted here only, not in androidTest.
* `ui/PatientEditorGroupsTest.kt` (new): outside a flow both groups; IDENTITY shows Identity only; OBSTETRIC shows Obstetric only; IDENTITY with `INVALID_GPA` also shows Obstetric; OBSTETRIC with `AGE_REQUIRED` also shows Identity; `STALE_REVISION` adds nothing.
* `RegistrationFlowStateTest.kt`: S5-4 edited and S6-1/S6-2 (driver fix, above); `activeFlowForRoute` (null when route and step mismatch); `systemBackKind` cases; "Begin with a hub patient whose first not-done step is IDENTITY, DATING or OBSTETRIC opens that editor", and, labelled reducer-only, "Begin at PREGNANCIES opens `PregnancyCountPrompt`" (the hub cannot produce it; `G=2` with no stored pregnancies is the fixture).
* S5b/S5b-2 tests as in Part 1.

androidTest. New `app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationFlowUiTest.kt` (about 250 lines) hosts content in `Box(Modifier.requiredSize(360.dp, 640.dp))` with `AncTheme` and sets `registrationFlow` on `AncState` directly. Event capture uses `onEvent = { events += it }`.
* RF11: step 3 header: `assertTextNotClipped` (`ComposeTextAsserts.kt:18`) on the title; full text "Obstetric history and risk" present; `flowNext` displayed with no `performScrollTo()`. Variants: font scale 1.4, dark theme, and keyboard shown (focus a field, show the IME; then `registrationSegment-1` does not exist, the title and `flowNext` are displayed). The keyboard variant may need the emulator rather than an instrumented IME call (UNVERIFIED).
* RF12: at steps 2 and 5, and at font scale 1.0, 1.4, 2.0: `flowBack`, `flowSkip`, `flowNext` displayed, enabled, fully inside the host, width and height >= 48dp (move the 48dp bound check from `AncWorkflowTest.assertFullyWithinHost`, `:829`, into a shared helper in `ComposeTextAsserts.kt`; do not copy it). Step 1: `flowBack` displayed and not enabled, `flowSkip` absent, `flowNext` enabled.
* RF13: statuses DONE, SKIPPED, PENDING with step 2 current: at font scale 1.0 and 1.3, `registrationSegment-1` has text "Done", `-2` "Skipped" (current also "Current"), `-3` "To do"; `contentDescription` of `-1` is "Identity, Done" and of the current one ends with ", current". At 1.4 the words are absent and only the descriptions are asserted. Glyph is not asserted here.
* RF14: directory with one incomplete and one complete seed; `incompleteChip` (tag at `PatientDirectoryCard.kt:170`, under `ui/patientdirectory/`) exists once, on the incomplete card. No androidTest covers this today.
* RF15: hub cases listed in section 3; click `continueRegistration` and the captured event's `.event` equals `FlowEvent.Begin(id, DATING)` for a patient with name and age but no dating.
* RF16 (count prompt): default "2" for G3 P1 A1; typing "25" disables `flowNext` and shows the error while `flowBack` and `flowSkip` stay enabled; blank does the same; "2" then Next emits `Flow(PregnancyCountChosen(2))`; with two stored pregnancies and "2" the button reads Finish; layout at 360dp with font scale 1.4 and the keyboard shown keeps the field visible.
* Wiring (review point): `flowNext` on each editor step emits `RegistrationNext` and never `Submit*`; `flowSkip` emits `Flow(Skip)` and no save; `savePatient`, `saveDating`, `saveHistories`, `savePregnancy` are absent in the flow and present for hub-started editors; the three system Back cases from section 2.
Existing androidTest files that change (corrects "none"): the three tests Argon names, `patientProfileValidationAndPersistenceRoundTrip`, `bloodGroupChoicePersistsAndInvalidGpaFailsClosed` and `discardDialogProtectsUnsavedPatientAndClinicalEdits`, do not exist in this worktree (`git grep` over `app/src/androidTest` finds none; the only patient-editor uses are the reducer-level `AncWorkflowTest.kt:304` and `:359`). If they exist on another branch, each starts a new patient (flow at Identity), so: the blood group and G/P/A assertions must first press `flowNext` to step 3 (after typing name and age), the Save/Cancel taps become `flowNext`/`flowBack` (Back is disabled on step 1; the discard test uses the system Back event instead), and `onNodeWithText("Back")` must target `flowBack`. The two reducer-level tests are unaffected (no UI, route and flow assertions only; UNVERIFIED that none asserts the post-commit route as the hub after a flow start). `AncWorkflowTest.kt:740` AncApp calls keep compiling.

### 10. Risks and what blocks the UI
1. Blocker found and fixed above: Next on an untouched Identity step (`RegistrationFlowDriver.kt:155`).
2. Pregnancy walk and mock: mock D's Finish on a non-final pregnancy is wrong against `RegistrationFlow.kt:104-113`; fixed in the footer rule.
3. No "registration saved" notice between steps or after Finish: starts reset `savedNotice` (`AncState.kt:1354`, `exitFlow` `RegistrationFlowDriver.kt:82-88`). The header turning to Done is the only feedback. Smallest fix if wanted later: set `SavedNotice.PATIENT` in `exitFlow` for Finish only. Not in S6.
4. Skip with unsaved edits uses the generic discard dialog (`AncApp.kt:288-311`); wording acceptable.
5. `WindowInsets.isImeVisible` behaviour under the `safeDrawing` padding: UNVERIFIED (RF11 keyboard variant).
6. `FlowRow` weights in the BOM: UNVERIFIED.
7. Mock gap: the directory card shows "Reason: age missing" in the mock; code shows the chip only (owner: not now).

### 11. Build order (each sub-slice compiles and passes `run_build_gate.sh` alone)
**S5b-1** (level 3; frontier review + independent test review in its gate): items 1-6 of Part 1 in one commit, with the check and manifest edits.
**S5b-2** (level 3; own review): the eight fields, tests, checks, backward-load test.
**S6a-0** (level 3; the review for all state edits happens here): `state/RegistrationFlow.kt` (drop `title`/`label` and the two text functions; add `registrationStepCount`, `firstNotDoneStep`, `forwardIsFinish`, `showsSkip`, `parsePregnancyCount`); `state/RegistrationFlowDriver.kt` (Identity-Next fix, `activeFlowForRoute`, `systemBackKind`); delete `AncViewModel.kt:351-357`; strings and `RegistrationStepText.kt`; update `RegistrationFlowTest`, `RegistrationFlowStateTest`; add `RegistrationStepTextTest`.
**S6a-1** (UI; reviewed with the AncApp change): `RegistrationStepHeader`, `RegistrationStepFooter`, `RegistrationFlowScaffold`; editors (groups function, hidden actions, `showHeader`); `AncApp` wraps the four routes and applies `systemBackKind`; `PatientEditorGroupsTest`; `RegistrationFlowUiTest` RF11-RF13 and the wiring cases. Emulator walk at 360dp covers steps 1-4 only and does not press Next or Skip on Histories (that lands on the hub placeholder with the flow still alive until S6b).
**S6b**: `ContinueRegistrationCard` and the OverviewTab hook; `PregnancyCountPrompt` and the AncApp branch; remaining strings; RF14-RF16; full-flow emulator walk including resume from the hub, at 360dp, font 1.4 and 2.0, dark, RTL and keyboard shown.

## Open questions for the owner (max 2, new)
1. Do you want a "Registration saved" notice on the hub after Finish (and none between steps)? Default: no notice in S6; revisit after the emulator walk.
2. The mock shows Finish on every pregnancy editor; the code only finishes after the last one. Default: Next on all but the last, Finish on the last, and I correct mock D.

## Changes from revision 1
* Added the Next-on-untouched-Identity driver fix with oracles S5-4 (edited), S6-1, S6-2, carried by new sub-slice S6a-0 (level 3).
* Footer now has separate back, skip and next flags (an invalid count disables only Next); RF16 asserts it. Footer is a `FlowRow` from the start; RF12 runs at 1.0, 1.4, 2.0.
* System Back has an exact order (dialog, then flow Back, then old path) as `systemBackKind`, with JVM and androidTest cases. Owner decisions applied (Back = previous step, step 1 Back disabled).
* Segment row hidden while the keyboard shows (RF11 variant); editor title and subtitle hidden in flow (`showHeader`); step title only in the header.
* Level-3 review moved into S5b-1, S5b-2 and S6a-0 gates; ViewModel helper deletion and `RegistrationFlow.kt` edits sit in S6a-0.
* Reuse: `SegmentedProgressBar` considered and rejected in one line; `step_n_of_m`, `next_step`, `back_step`, `done` reused.
* RF13 no longer asserts glyphs in androidTest; segment description includes "current" and `selected`; heading and live region on the title only; `key(activeEditorSessionId, current)`; count field `singleLine` + number keyboard + scrolling body; `rememberSaveable` keyed on patient and G/P/A; one `activeFlowForRoute` helper.
* Finish on the count prompt when `remainingPregnancies == 0`; mock D noted as wrong.
* S5b corrections: `IsoDates.kt` untouched (r3i S53, r4f S160 stay); eight draft fields; S5b-1 deletes the VM block and the rule file together; manifest entries listed with the `--tests` argument; r4g S173 repointed so it can fail; r3i S54 repointed to `DatingDraftRule.kt`; line references fixed (`SavedNoticeTextTest.kt:14`, `DraftMapping.kt:107,110-119,378-385`, `AncState.kt:101-108,468-475`); old-draft loading test specified; "dead" reworded (recovery could still trigger it).
* Hidden-field validation: `patientEditorGroups` reveals the group that owns an error.
* Argon's three AncWorkflowTest names are not in this worktree; update rules written for them anyway. PREGNANCIES Begin test labelled reducer-only.
