You are the independent test designer (you did not write the production code). Write tests only; change no production file.

Production behaviour under test (already built):
- `editPatientEvent(patient: DemoPatient, editorSessionId: String, newPregnancyId: String): AncEvent?` in package `com.sherifradwan.ancnative.state`: archived gives null; unfinished registration gives `AncEvent.Flow(FlowEvent.Begin(patient.id, <first unfinished step>), editorSessionId, newPregnancyId)`; finished gives `AncEvent.StartPatientEditor(patient.id, patient.id, editorSessionId)`.
- Registration is unfinished when the name or `ageText` is blank (step IDENTITY), or `dating` is null (step DATING), or any of `gravida`, `parity`, `abortion` is null (step OBSTETRIC). First unfinished step in that order wins.
- UI test tags: the header pencil on the patient screen is `editPatientButton`; the Incomplete chip on a patient list card is `incompleteChip`, and its tappable wrapper is `incompleteChipAction`. The chip is shown only for a patient with unfinished registration. Tapping the chip must NOT also send `AncEvent.OpenWorkspace`.
- Routes: `AncRoute.PatientList`, `AncRoute.Workspace(patientId)` (other parameters have defaults), `AncRoute.PatientEditor(patientId, isNew)`, `AncRoute.DatingEditor(patientId)`.
- `DemoFixtures.patients.first()` is a patient with finished registration.

TASK A. NEW FILE `app/src/androidTest/java/com/sherifradwan/ancnative/RegistrationPencilUiTest.kt`, class `RegistrationPencilUiTest`, same harness as the excerpt of RegistrationFlowUiTest below (copy its `compose` rule, `events` list and `show(state)` function exactly, including HOST_TAG as a private const in a companion object or top-level private const named differently, e.g. `PENCIL_HOST_TAG`, to avoid a clash). Build the state as `AncState(patients = listOf(p) , vaultState = VaultState.Unlocked(sessionEpoch = 1L), route = ...)` where `p` is `DemoFixtures.patients.first()` or a `.copy(...)` of it. Tests:
 1. `chipTapStartsTheFlowAtIdentityAndDoesNotOpenTheWorkspace`: p = first.copy(ageText = null), route PatientList; click `incompleteChipAction`; `events.single()` is `AncEvent.Flow` whose `event` equals `FlowEvent.Begin(p.id, RegistrationStep.IDENTITY)`.
 2. `completePatientShowsNoChip`: p = first, route PatientList; `compose.onAllNodesWithTag("incompleteChip").fetchSemanticsNodes().isEmpty()` is true.
 3. `pencilOnAnIncompletePatientStartsTheFlowAtTheFirstUnfinishedStep`: p = first.copy(dating = null), route Workspace(p.id); click `editPatientButton`; single event is Flow with `FlowEvent.Begin(p.id, RegistrationStep.DATING)`.
 4. `pencilOnACompletePatientOpensThePlainEditor`: p = first, route Workspace(p.id); click `editPatientButton`; single event equals an `AncEvent.StartPatientEditor` with `patientId == p.id` and `newPatientId == p.id` (the session id is random: compare the two fields, not the whole object).
 Import only what you use.

TASK B. EDIT `app/src/test/java/com/sherifradwan/ancnative/state/PatientEditEntryTest.kt` (full file shown below): add tests before the final closing brace, reusing the helpers already in the file:
 1. identity-incomplete event reduced from `AncState(patients = listOf(p), route = AncRoute.Workspace("p1"))` gives `registrationFlow?.current == RegistrationStep.IDENTITY` and a route that is `AncRoute.PatientEditor`.
 2. obstetric-incomplete event reduced the same way gives `registrationFlow?.current == RegistrationStep.OBSTETRIC` and a route that is `AncRoute.PatientEditor`.
 3. the attention dialog path: `AncReducer.reduce(state, AncEvent.StartPatientEditor("p1", "p1", "s1"))` for an INCOMPLETE patient (ageText = null) gives a route that is `AncRoute.PatientEditor` and `registrationFlow == null`.
 4. `row` and rule agree: for the complete patient `isRegistrationIncomplete(p)` is false and `editPatientEvent` gives StartPatientEditor; for each incomplete patient it is true and the event is `AncEvent.Flow`.
Use the SEARCH/REPLACE format: for the new file an empty SEARCH; for the edit, SEARCH the last test's final lines plus the class closing brace so it matches exactly once.
