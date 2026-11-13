You are the independent test designer. Write ONE new JVM unit test file; change no other file.
NEW FILE `app/src/test/java/com/sherifradwan/ancnative/state/DeliveryThroughRegistrationFlowTest.kt`, package `com.sherifradwan.ancnative.state`, class `DeliveryThroughRegistrationFlowTest`. JUnit4. Copy the imports and the helpers `patient(...)`, `base(...)`, `AncState.reduce(...)`, `lmpDate`, `submittedAt`, `freeNotes`, `planTab` from the excerpt of DeliveryOutcomeStateTest below (they are private there, so you must copy them into your class). Plain function names (no backticks needed).

Facts about production code (do not invent other API):
- `AncEvent.Flow(FlowEvent.Begin("p1"), "s-flow", "preg-1")` reduced on a state whose patient "p1" has unfinished registration opens the patient editor at the Identity step: afterwards `state.registrationFlow?.current == RegistrationStep.IDENTITY`, `state.patientDraft` is not null, `state.forcesAgeRequired()` is true.
- The helper `patient(...)` below builds a patient with NO age and NO gravida, so its registration is unfinished.
- A draft field is typed with `AncEvent.PatientDraftChanged(PatientDraftField.AGE_TEXT, "30")`.
- `SavePatientProfileCommand.prepare(state, operationUuid, submittedAt, forceAgeRequired = false)` returns `PatientCommandPreparation.Ready(command)` or `PatientCommandPreparation.Rejected(errors)`. `Ready.command` cast to `SavePatientProfileCommand` has `delivered: Boolean`, `deliveryDate`, `deliveryMode`, `deliveryOutcome`, `fingerprint: String`.
- Delivery fields in the delivery sheet draft are changed with `AncEvent.StartDeliveryPlanEditor("p1", "s-plan")`, `AncEvent.DeliveryOutcomeToggled(true)` and `AncEvent.DeliveryOutcomeFieldChanged(DeliveryOutcomeField.DATE, "2026-10-09")`.

Tests to write (the delivered patient is `patient(delivered = true, deliveryDate = "2026-10-08", deliveryMode = "vaginal", deliveryOutcome = "live_birth")`):
1. flow_begin_on_a_delivered_incomplete_patient_opens_identity: assert the three facts above after reducing Begin on `base(deliveredPatient)`.
2. identity_step_rejects_a_blank_age: after Begin, `prepare(state, "op-1", submittedAt, forceAgeRequired = true)` is `Rejected`.
3. identity_step_save_keeps_all_four_delivery_values: after Begin and typing age "30", `prepare(..., forceAgeRequired = true)` is `Ready`; the command has delivered true, date "2026-10-08", mode "vaginal", outcome "live_birth".
4. same_draft_gives_the_same_fingerprint: two prepares of the state in test 3 (same operationUuid and submittedAt) have equal `fingerprint`, and it is not blank.
5. a_changed_delivery_date_changes_the_fingerprint: from `base(deliveredPatient)` (age is not required outside the flow) open the delivery sheet, prepare once (fingerprint f1), then change DATE to "2026-10-09" and prepare again (f2); assert f1 != f2 and that the second command's deliveryDate is "2026-10-09".
6. begin_on_an_archived_patient_changes_nothing: `base(patient(status = "archived"))` reduced with Begin equals the state before (same route, `registrationFlow == null`, `patientDraft == null`).
Output the file as one SEARCH/REPLACE block with an empty SEARCH.
