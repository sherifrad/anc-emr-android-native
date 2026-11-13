Mission: the header pencil and the Incomplete chip open the registration flow for a patient whose registration is not finished.
Android Kotlin, Jetpack Compose, MVI. Package root `com.sherifradwan.ancnative`. Source root `app/src/main/java/com/sherifradwan/ancnative/`.

Facts (all verified in the code shown below; do not invent other names):
- `isRegistrationIncomplete(patient: DemoPatient): Boolean` and `firstNotDoneStep(patient: DemoPatient): RegistrationStep?` exist in package `state` (file RegistrationFlow.kt).
- `FlowEvent.Begin(patientId: String, startStep: RegistrationStep = IDENTITY, patient: DemoPatient? = null)`.
- `AncEvent.Flow(event: FlowEvent, editorSessionId: String, newPregnancyId: String)`.
- `AncEvent.StartPatientEditor(patientId: String?, newPatientId: String, editorSessionId: String)`.
- `DemoPatient.status` is a nullable String; the value "archived" means archived.
- `Modifier.minimumTouchTarget()` exists in package `ui` (48 dp minimum).

Produce exactly these edits, nothing else:

1. NEW FILE `app/src/main/java/com/sherifradwan/ancnative/state/PatientEditEntry.kt` with one public function:
   `fun editPatientEvent(patient: DemoPatient, editorSessionId: String, newPregnancyId: String): AncEvent?`
   Rule: archived patient returns null. If `isRegistrationIncomplete(patient)` is true, return
   `AncEvent.Flow(FlowEvent.Begin(patient.id, firstNotDoneStep(patient) ?: RegistrationStep.IDENTITY), editorSessionId, newPregnancyId)`
   (leave the `patient` argument of Begin at its default; the reducer looks the stored patient up itself).
   Otherwise return `AncEvent.StartPatientEditor(patient.id, patient.id, editorSessionId)`.
   One short KDoc line saying this is the single rule every "edit patient" control uses.

2. NEW FILE `app/src/test/java/com/sherifradwan/ancnative/state/PatientEditEntryTest.kt` (JUnit4, `org.junit.Assert.*`, same style as RegistrationFlowDriverTest shown below; build `DemoPatient` exactly as that test does, with named arguments). Tests:
   - complete patient gives `AncEvent.StartPatientEditor("p1", "p1", "s1")`;
   - patient with `ageText = null` gives `AncEvent.Flow(FlowEvent.Begin("p1", RegistrationStep.IDENTITY), "s1", "g1")`;
   - patient with `dating = null` (identity done) gives Begin at `RegistrationStep.DATING`;
   - patient with `gravida = null` (identity and dating done) gives Begin at `RegistrationStep.OBSTETRIC`;
   - archived patient (status = "archived", ageText = null) gives null;
   - reducing the incomplete event: `AncReducer.reduce(AncState(patients = listOf(p)), event)` gives a state whose `registrationFlow?.patientId` is "p1", `registrationFlow?.current` is DATING for the dating case, and `route` is `AncRoute.DatingEditor`. Use `AncReducer.reduce(state, event)`.
   - reducing the complete event leaves `registrationFlow` null and `route` an `AncRoute.PatientEditor`.

3. EDIT `ui/patienthub/PatientHubScreen.kt`: in `onEditPatient = { ... }` (the header pencil) replace the hand-built `AncEvent.StartPatientEditor(...)` with
   `editPatientEvent(patient, UUID.randomUUID().toString(), UUID.randomUUID().toString())?.let(onEvent)`.
   DO NOT touch the `ClinicalAttentionDialog` block lower in the same file: it must keep `AncEvent.StartPatientEditor`.

4. EDIT `ui/patienthub/tabs/OverviewTab.kt`: the local `fun startPatientEditor()` body becomes the same `editPatientEvent(...)?.let(onEvent)` call. Keep the function name and its `Unit` result (write it with a block body).

5. EDIT `ui/patientdirectory/PatientActionsModalSheet.kt`: inside the `.clickable { ... }` of "Edit Demographics" keep `onDismissRequest()` and replace the hand-built event with the same `editPatientEvent(...)?.let(onEvent)` call.

6. EDIT `ui/patientdirectory/PatientDirectoryCard.kt`: make the Incomplete chip tappable. Wrap the existing `ClinicalTagBadge(...)` in
   `Box(modifier = Modifier.minimumTouchTarget().clickable(onClickLabel = stringResource(R.string.registration_incomplete_chip_action)) { editPatientEvent(patient, UUID.randomUUID().toString(), UUID.randomUUID().toString())?.let(onEvent) }.testTag("incompleteChipAction"), contentAlignment = Alignment.CenterStart) { ... }`.
   Keep the badge and its testTag "incompleteChip" unchanged inside the Box.

7. EDIT `app/src/main/res/values/strings.xml`: add after the line of `registration_incomplete_chip`:
   `<string name="registration_incomplete_chip_action">Continue registration</string>`

For every edited Kotlin file add the imports that are missing, as a separate edit block on that file's import lines (imports are shown in the excerpts): `com.sherifradwan.ancnative.state.editPatientEvent`, and in PatientDirectoryCard.kt also whichever of `java.util.UUID`, `androidx.compose.foundation.layout.Box`, `androidx.compose.ui.Alignment`, `androidx.compose.ui.platform.testTag`, `com.sherifradwan.ancnative.ui.minimumTouchTarget` are not already imported. Do not add an import that is already there. Do not remove any import.
Close every block with the line `>>>>>>> REPLACE`. For a new file use an empty SEARCH section.
