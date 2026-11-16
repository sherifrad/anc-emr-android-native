# Audit 2026-11-13: registration flow merge plus pencil and chip (testing mode)

Requirement (owner): deep testing of the app at HEAD before the push. Scope: what the registration merge and the pencil change could have altered.

ORACLE-A1: a delivered patient (delivered, date, mode, outcome all set) edited through the registration flow keeps all four delivery values in the save command, and `prepare` raises no delivery error.
ORACLE-A2: in the flow's Identity step `prepare(..., forceAgeRequired = true)` rejects a blank age and accepts a filled one.
ORACLE-A3: two `prepare` calls on the same draft give the same fingerprint; changing one delivery field changes it.
ORACLE-A4: flow events are in both "another action started" lists, so a failed save followed by a flow move does not resend the old command (review of `startsAnotherAction` and `clearsFrozenPatient`).
ORACLE-A5: every edit control uses `editPatientEvent`; the Clinical attention dialog never starts a flow (covered by PatientEditEntryTest and RegistrationPencilUiTest).
ORACLE-A6: no schema or protected storage file changed by the pencil change; full unit and emulator suites pass on HEAD.
Negative case: an archived patient never enters an editor through Begin.

Roles: test designer DeepSeek (opencode); reviewers GLM (opencode), big-pickle (opencode), alphacode. The merge port was written by Claude, so every reviewer is a different family from the author. Orchestrator and runner: Claude.
