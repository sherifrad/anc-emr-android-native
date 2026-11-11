## Blocker

**B1. The patch cannot compile as pasted — the state/parser half is absent.**
`DeliveryPlanSheet.kt:53` reads `draft.deliveryPlan`, `:110` uses `AncEvent.DeliveryPlanFieldChanged`/`DeliveryPlanField`, `PlanTab.kt:768` and `OverviewTab.kt:746` send `AncEvent.StartDeliveryPlanEditor`, `PatientHubScreen.kt:153` calls `deliveryPlanSheetDraft(patient.id)`. Nothing in this diff declares any of them, and nothing moves `parseDeliveryNotes` out of `ui/`. `./gradlew :app:compileDebugKotlin` fails with unresolved references on all four files. Related: `DeliveryPlanningCard.kt:273` still resolves `parseDeliveryNotes` from the old `ui` package; when the move in design step 1 lands, that import breaks too. If these live in wp1/wp2, the artifact must say so — as an artifact it is red.

**B2. A validation failure on a hidden profile field strands the doctor.**
`DeliveryPlanSheet.kt:91` shows *"…Fix it from Edit patient on the Overview tab"*, but the live sheet draft sets `hasDirtyDraft`, and `PatientHubScreen.kt:151` computes `canModifyData = … && !state.hasDirtyDraft`, while `startPatientEditor` (design D10) no-ops on `hasDirtyDraft`. Scenario: notes open, doctor edits Fees, Save, the repository rejects because `phone` is malformed → sheet shows "go fix it on Overview" → tapping Edit patient does nothing → the only exit is Cancel, which asks to discard and throws away the four boxes. The message is unactionable exactly when it appears.

**B3. `OverviewTab` was rewired to the new sheet with no design cover.**
`OverviewTab.kt:745-747` replaces `startPatientEditor()` with `StartDeliveryPlanEditor`. No design step authorises this, no test covers it (design §9 only exercises `PlanTab`), and the Overview pencil no longer reaches the full patient form — the one place a doctor edits obstetric history, phone, risk level. `startPatientEditor` in `OverviewTab` is now a dead parameter (AGENTS "zero legacy baggage"). The hunk also assumes `AncEvent`, `UUID` and an `onEvent` parameter already exist in `OverviewTab.kt`; no import was added. If any is missing, the build fails here too.

## Major

**M1. The rewrite notice advertises an action Save forbids.**
`rewritesNotes` (design A1) is true whenever stored notes drift from the composed format, but A16 disables Save until `touched` (`DeliveryPlanSheet.kt:138`), and A6 says typing-then-deleting does not set `touched`. Scenario: doctor adds one free line in the patient form, reopens the sheet → notice says "Saving will rewrite this patient's obstetric notes…" → every box is unchanged → Save is greyed out and no reachable gesture converts the notes. The notice describes a rewrite that cannot be triggered.

**M2. Accessibility: `contentDescription` on the editable field duplicates and masks it.**
`DeliveryPlanSheet.kt:257` sets `contentDescription = label` on the `OutlinedTextField`, while `:245` renders the same label as a sibling `Text` node. TalkBack focus reads the label twice, and a content description on an editable node replaces the value/hint announcement, so a doctor cannot tell whether the box holds text or `no_record`. Correct form: no `contentDescription`; put `semantics { }` on the `Column` with `mergeDescendants`, or use the field's own `label`. Also `:139` `LinearProgressIndicator` carries no accessible label, and `PlanField` never exposes whether the field is required/disabled.

**M3. Focus and IME: no traversal, wrong keyboard for Fees.**
`PlanField` sets only `KeyboardCapitalization.Sentences` (`:253`): no `imeAction`, no `KeyboardActions`, no `FocusRequester` chain. On the multi-line Hospital field the IME action is `UNSPECIFIED`, so Enter inserts a newline (correct) but the keyboard never advances or closes; at 360 dp with the keyboard up the field below cannot be reached without dismissing it. `DeliveryPlanField.FEES` gets a sentence-case *text* keyboard, not a number/decimal one, for a field the design says holds amounts. `minLines` is set but `maxLines` is not, so a long hospital or plan value grows without bound inside the scroll and pushes the remaining boxes off screen.

**M4. The visible labels are translatable; the stored labels are not.**
`strings.xml:251-254` adds `delivery_hospital_title` … `delivery_postpartum_title` with no `translatable="false"`, yet D11 stores those English words in `obstetricNotes` and the strict parser is case-sensitive English (`D4`). After an Arabic translation the card heading reads Arabic while the patient form, print and summary show `Delivery Plan:`; the design's own A8 test ("label constants equal the card titles in strings.xml") fails. Mark the four titles `translatable="false"` or they will drift.

## Minor

- `MedicationSheet.kt:528` (`PaddingValues`) and `:564` (`TextAlign`) are now unused after the shared-row extraction — lint/`-Werror` risk.
- `AncApp.kt:12` is >120 chars (ktlint) and hides the global Undo bar on `patientDraft?.deliveryPlan != null` alone, ignoring patient id and route, so a stale draft suppresses Undo app-wide.
- `DeliveryPlanSheet.kt:135` puts `testTag("deliveryPlanSheet")` on `ModalBottomSheet` itself, where the node also wraps the scrim; `assertIsDisplayed` can match the scrim.
- `rememberGuardedSheetState` (`EditorSheetParts.kt:491-505`) uses `rememberUpdatedState` for two params but captures `onNeedsConfirm` directly — asymmetric, and the stale capture silently swallows the discard prompt if the sheet state survives the callback's identity changing.
- `revealOnFocus`/`revealWhen` stay in `MedicationForm.kt` (`:517`) although they are now shared — a medication-named home for a generic modifier (Rule 1/2 smell).
- `DeliveryPlanSheet.kt:251` uses `R.string.no_record` as a placeholder; it reads as an asserted value to TalkBack rather than an example.
- `imePadding()` on the sheet content Column (`:137`) stacks on the insets `ModalBottomSheet` already applies; at 360 dp the footer Undo bar can float above the nav bar.
- Design §9's 360 dp test asserts "no ellipsis on the label `Text`", but M2 means the interesting assertion — the semantics text of each field — is not made.
