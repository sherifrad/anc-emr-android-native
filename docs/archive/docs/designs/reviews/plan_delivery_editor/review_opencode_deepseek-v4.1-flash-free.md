Independent review. I checked the four pasted files. Many design claims about `AncState.kt`, `AncReducer.kt`, `AncViewModel.kt`, `PlanTab.kt`, `PatientHubScreen.kt`, persistence and `UpdatePatientCommand` cannot be verified from what was supplied, and the design itself flags most of them as unverified. Those are not findings by themselves, but the blockers below must be resolved before implementation.

BLOCKERS

1. Section 5.2 / D2, write-back safety is unproven and likely wrong. `toPatientDraftValues()` (ClinicalDraftReducers.kt:394-409) populates `caseId`, `displayName`, dates, `obstetricNotes`, `clinicalAttention`, `lmpDate` and a few others, but omits `ultrasoundEdd`, `agreedEdd`, `datingMethod`, `transferDate`, `embryoAgeDays`, `scanDate`, `scanGaWeeks`, `scanGaDays`. Those fields exist in `PatientDraftValues`, as `updatePatientDraft` shows (ClinicalDraftReducers.kt:261-268). If `UpdatePatientCommand` writes every field, then a fees edit through the sheet can clear the agreed EDD and dating method. The design claims "the risk on the other columns is no greater than today"; that claim is false or at least unverified, and the sheet makes the path far more reachable. Verify the command mapping and the merge rules before anything else.

2. Section D4 / 5.3 / risk table, silent parse change before any save. The strict branch `continue`s before `hospitals += hospitalNames(segment.text)` (DeliveryNotesParser.kt:38). An older note containing the exact-case line `Delivery Plan: follow up at X Hospital` already shows X in the hospital box today; after this change the card drops X the moment it is rendered, with no sheet open and no notice. Similarly `Delivery Plan: SVD` changes from "Delivery Plan: SVD" to "SVD". The notice only appears inside the sheet, so the doctor sees a changed card without being told. The design calls this "unlikely"; `Delivery Plan` is the card's own title, so it is not unlikely. Add an explicit pre-save warning or defer the strict rule to composed-only text, and test the visible card before and after.

3. Section 5.2 / D10, stale sheet draft on patient change or restore. Visibility is only `patientDraft.deliveryPlan != null && patientId == hub id` (5.1). A draft for patient A while the hub shows patient B stays in state, `hasDirtyDraft` stays true, and every editor on B silently no-ops. There is no reducer path that clears it. "A dirty draft cannot normally exist" is not a guarantee. Add a clear-on-patient-change rule and a restore test.

MAJOR

4. Section 5.5 / step 9, `SheetSaveMessage(savePhase, saveError, hasFieldErrors, failedTitle, tag)` cannot show the new `delivery_plan_profile_invalid` text. The signature only carries a boolean. Either add a message parameter or drop the custom string.

5. Section 5.5 / D6, unreachable validation errors. If `SubmitPatientProfile` fails on a profile field the sheet does not show, the message says "Open the full profile", but there is no action, and `startPatientEditor` is guarded by `hasDirtyDraft`, so the doctor can be trapped in a failing Save. Give the banner a button or a documented escape.

6. Section D6 / Corollary C, "the rewrite happens at most once per patient" is false. A later profile-form edit (which 5.4 explicitly permits) makes `isComposedDeliveryNotes` false, and the next sheet save rewrites again. The notice appears again, but the claim is wrong. Test this sequence.

7. Section 5.4, "No content text is dropped" contradicts the same list: `Fee package: 20000` loses "package". That word can change the meaning of a price. Disclose it in the notice or preserve it.

8. Section 5.2 start, `startDeliveryPlanEditor` does not reset `savePhase` or `saveError`. Opening the sheet after a failed save elsewhere can show a stale error banner with editable fields. Also, `clearDrafts()` clearing every field is inferred, not verified.

9. Section D2 / Rule 1, the full profile form remains a second editor of the same delivery entity (5.4 says the doctor can still edit the notes there). AGENTS.md Rule 1 requires exactly one canonical component. State the temporary exception explicitly, or route the profile notes field to the sheet.

10. Section 5.5, the Hospital field is prefilled with heuristic-derived names but is not labelled as derived. Editing the plan to remove a hospital leaves the derived name in the hospital box, and the save records it as "Agreed Delivery Hospital". That is a clinical assertion the doctor did not make.

11. Section 5.3, typing a character and undoing it sets `touched` and triggers the first rewrite even though the visible fields are unchanged. Compare normalized sections, not just `touched`.

12. Section 5.5 footer and `revealWhen` are asserted to be reusable, but the host wiring is unverified and no test covers the Undo bar while the sheet is open.

MINOR

13. Section 1, the card does not send `AncEvent.StartPatientEditor`; it calls `onEditPatient` (DeliveryPlanningCard.kt:51, 94). The parent sends the event.

14. Section D4(b), the counterexample writes `Fees:`; compose uses `Agreed Delivery Fees:`. The point stands for the old string, but the wording is loose.

15. Section 5.4 Lemma 1 says hospital names end with "Hospital"; `labelledHospital` can return "Cleopatra" from `Hospital: Cleopatra` (DeliveryNotesParser.kt:254-260). Not load-bearing, but wrong.

16. Section 9, "no ellipsis" is a weak test because Compose `Text` defaults to clip. Assert the full text via semantics. Add tests for lone `\r`, U+2028, U+0085, NBSP at value edges, a label inside every field, 5,000-character values, duplicate hospital lines, and strict lines for all four labels with `;` and sentence ends.

17. Section 5.2, `rewritesNotes` stays true after the doctor clears every field until Save. Harmless, but the notice is stale.

18. Section 5.2 crash recovery cites `RECONCILING`/`UNRESOLVED` without proof. Add a restore test, including a draft whose route is not `PatientEditor`.

19. Step 3 says a new field with a default keeps existing constructors compiling. Only if it is appended last or all call sites use named arguments. State which.

The compose/parse proof itself is mostly sound for the stated invariants, including `;`, blank lines, `\r`, U+2028/U+0085 and labels inside values, provided Kotlin `lines()` splits only on `\r`, `\n` and `\r\n`. The failures above are in state, write-back and disclosure, not in Theorem A.
