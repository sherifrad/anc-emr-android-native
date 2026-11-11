# Spec: one-outcome, one-mode previous-pregnancy form (owner decisions 2026-11-10)

Files: ui/PreviousPregnancyEditor.kt, state/AncState.kt (PregnancyDraftField, reducer ~1611), storage/PatientCommands.kt (~184-215), model/DemoModels.kt (PreviousPregnancyClinicalInput, PatientValidationError), model/ObstetricHistory.kt (reader, must keep working). No schema change, no column dropped.

Problem: the form asks outcome twice (free text `outcomeText` + structured `outcome`) and mode twice (`deliveryBy` CS/Vaginal/Hysterotomy + `birthMethod` Vaginal/Cesarean), and asks for a clinician id.

Required behaviour (new and edited saves through this form):
1. Remove the free-text "entered outcome" input. The structured outcome choice (LiveBirth, Stillbirth, EarlyPregnancyLoss, TerminationOfPregnancy) is the only outcome input. Anything else goes in the existing "Other details" field.
2. One "Delivery mode" choice, shown only when outcome is LiveBirth or Stillbirth, with the options Vaginal and CS. On save the single choice is written to BOTH columns so existing readers keep working: Vaginal -> deliveryBy=Vaginal, birthMethod=Vaginal; CS -> deliveryBy=CS, birthMethod=Cesarean. For other outcomes deliveryBy and birthMethod are null. Loss management (EarlyPregnancyLoss) and termination method (TerminationOfPregnancy) stay as they are; Hysterotomy is covered by the termination method only.
3. Clinician id: no input in the form. New pregnancies save clinicianId = the default clinician "Dr. Sherif Radwan" from ONE constant (look for an existing definition of the doctor name, e.g. in the print report or clinic profile settings, and reuse it; do not add a second copy). Editing an existing pregnancy keeps its stored clinicianId. Provenance stays CLINICIAN_ENTRY. CLINICIAN_ID_REQUIRED must never block a save made through the form.
4. Place and doctor is required for a NEW pregnancy. Add a validation error (e.g. PLACE_AND_DOCTOR_REQUIRED) shown inline on that field. For an EXISTING row being corrected, it is required only if the row already had a value (old rows with null stay editable).
5. Old rows still display correctly: readers that use outcomeText, deliveryBy, birthMethod (model/ObstetricHistory.kt, ui/patienthub/model/PastDeliveryRowModel.kt, print report) are not changed. When an old row has outcomeText but no structured outcome, the form shows it read-only as a note and the user may pick the structured outcome; it is not silently deleted unless the user picks an outcome (then outcomeText is cleared to null).
6. Card layout at 360dp: reduce to two cards (core facts: date, outcome, mode/method; then place and doctor, pregnancy type, other details). No new fixed widths, no truncated labels, keep existing testTags where the field remains, add none that are dead.

ORACLES (each needs a JVM unit test; pure functions preferably, put the mode mapping and the default/required rules in one small testable function in model/ or state/, called by the command builder, not copied):
- ORACLE-PF1: outcome LiveBirth + mode CS -> deliveryBy=CS, birthMethod=Cesarean; mode Vaginal -> Vaginal/Vaginal; EarlyPregnancyLoss + any stray mode -> both null.
- ORACLE-PF2: a new pregnancy with blank place and doctor fails with the new error; filled passes; an old row with null place stays valid on edit.
- ORACLE-PF3: saving never produces CLINICIAN_ID_REQUIRED; new row clinicianId equals the default constant; edit keeps the old id.
- ORACLE-PF4: existing validation still holds (OUTCOME_METHOD_MISMATCH for loss + birth method, INVALID_* for unknown values).
- ORACLE-PF5: an old row with outcomeText only reads the same through ObstetricHistory as before (existing tests stay green).
Negative/boundary: blank vs whitespace-only place and doctor; Hysterotomy only valid as termination method.
