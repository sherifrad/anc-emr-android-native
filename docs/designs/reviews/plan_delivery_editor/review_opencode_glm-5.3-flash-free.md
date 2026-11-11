BLOCKER

B1. Section D2 (shared save path). The design claims a sheet save "writes the same non-notes fields that a save from today's form would write" and "the risk on the other columns is no greater than today," but never verified what UpdatePatientCommand does with values that toPatientDraftValues never seeds. Checked: ClinicalDraftReducers.kt lines 394-409 seeds only 14 columns, while updatePatientDraft (lines 247-268) handles 21 PatientDraftField values including DATING_METHOD, TRANSFER_DATE, EMBRYO_AGE_DAYS, AGREED_EDD, SCAN_DATE, SCAN_GA_WEEKS, SCAN_GA_DAYS. Those seven come from PatientDraftValues defaults (empty) in a sheet-created draft. If the save command writes them, one delivery-plan save silently wipes dating and scan data the doctor never saw in the sheet. Verify the command's write set before build, or scope it.

MAJOR

M1. D6 / 5.4: "the rewrite happens at most once per patient" is false. Corollary C only proves compose(parse(compose(U))) == compose(U). After the first save, if the doctor adds any free line in the patient form (for example "Call if bleeding"), compose(parse(notes)) != notes, so isComposedDeliveryNotes (5.3) is false again, the notice returns, and the next sheet save rewrites the whole document. The idempotence claim overreaches.

M2. 5.4 "What the user sees": whether print and the clinical summary read obstetricNotes raw or parsed is deferred to implementation step 12. That is a clinical-document change resolved after code exists, against your item (6). It belongs in the design gate.

M3. D10: "one rule set for every editor" is wrong. startVisit (ClinicalDraftReducers.kt lines 19-24) answers a dirty draft with showDiscardDialog plus PendingAction, not a silent no-op; startPatientEditor no-ops. Also unaddressed: if route is persisted, a restored patientDraft could show the sheet over the wrong tab, since the visibility condition (5.1) checks only the patient id, not the route.

M4. 5.2 draft persistence: the restore fallback rule is self-contradictory. It drops "a patientDraft whose route is not PatientEditor and has no deliveryPlan," leaving unspecified what happens to a draft with route PatientEditor and deliveryPlan != null, and to a null route. If a restored PatientEditor draft ever carried the slice, SubmitPatientProfile from the registration form would take the untouched-close branch and silently discard a registration edit.

MINOR (wrong or unverified code claims)

m1. D4(b) wording: "Fees typed as `5000 EGP; includes 2 nights` compose to `Fees: 5000 EGP; includes 2 nights`" is not what the boxes show. Lines 40-53 of DeliveryNotesParser.kt rejoin only same-home segments, so fees shows "5000 EGP" and plan shows "includes 2 nights". The stated conclusion (second piece lands in plan) is right; the quoted composite string is wrong.

m2. Lemma 1: "the trimmed phrase group... starts with a capital letter and ends with Hospital" is false for labelled values: `Hospital: al nour clinic` yields hospital "al nour clinic" (acceptedName, lines 272-276). Not load-bearing for invariant I, but the stated lemma is wrong as written.

m3. Unverified and flagged only vaguely by the design: "638-line profile form"; "the Meds tab already solved it the same way"; whether CancelEditor keeps the route for the medication sheet; where SubmitPatientProfile's success branch lives. The design does admit most of these; steps 6-8 must not proceed without checking.

m4. 5.5 footer: the design admits it does not know how the host passes the Undo bar. A delivery-plan save may have no undoable action, so copying the MedicationSheet footer slot may carry wrong content or nothing.

m5. 5.4 notice text understates the transformation: it says text "keeps the box it is shown in" but does not say label words are deleted ("Fees agreed: 5000" becomes "Agreed Delivery Fees: 5000") and lines are regrouped. A doctor reading the notice is not fully warned.

PARSER AND PROOF

I traced Theorem A and B against the pasted parser for the adversarial inputs you listed: semicolons, sentence ends, blank lines, lone '\r' (Kotlin lines() splits on CR, so Lemma 1's no-CR claim holds), U+2028 and U+0085 (DOT_MATCHES_ALL is genuinely required, since Java '.' excludes them), labels inside values, and very long values. No counterexample found; the rejoin logic (lines 40-53), pieceOf trimming (102-103), LinkedHashSet distinctness, and strict-branch skip of hospitalNames hold together. Two cautions: the regex '\s' is Java ASCII while trim() is Unicode; the direction used is safe ('\s' is a subset of isWhitespace) but should be documented; and strict values are unbounded, so no max-length guard exists anywhere.

MISSING TESTS

No back-press test (Dismiss claims back is blocked while saving). No process-death restore test for the slice. No host-visibility test that a draft for another patient id shows nothing. No test for the delivery_plan_profile_invalid path. No test that a touched save with identical values writes byte-identical text. No check that the new string resources equal the LABEL_ constants (D11 divergence would desync display and stored labels silently). No instrumented archived-patient case (manual step only).

SCHEMA AND CLINICAL TEXT

Schema v6 untouched, verified. Silent-text risks: B1 (other columns), M2 (print), m5 (notice wording). Open question 1 is acceptable with tests; open question 2 should be resolved as "writes nothing," as designed.
