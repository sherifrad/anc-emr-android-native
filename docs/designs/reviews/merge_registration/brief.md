# Task: write the safest plan to merge an unmerged feature branch into main (no code, a plan)

Android Kotlin app (Compose, MVI, Room + SQLCipher). Clinical software: wrong merges can lose patient data or break safety rules. Owner rules: never force-push; push only on the owner's approval; schema is frozen except the owner-approved change already made; files under 1,500 lines; one canonical component per clinical entity; no legacy shims; every step must be verified by tests; commit each verified round.

## Situation
- `main` (HEAD bace905) has 42 commits since the common ancestor b4337fc (2026-11-09). Recent main work: Meds bottom sheet, Plan/Delivery bottom sheet (`DeliveryPlanSheet`, `PatientDraft.deliveryPlan` slice, `StartDeliveryPlanEditor`), fetal presentation on Scans tab, delivery outcome (schema v7: columns delivered, delivery_date, delivery_mode, delivery_outcome; `PatientDraftValues` gained 4 fields; `SavePatientProfileCommand.prepare` validates them), AncState split, a branch-hygiene audit script.
- Branch `claude/clever-goldwasser-e086ce` ("registration flow", worktree `.claude/worktrees/clever-goldwasser-e086ce`) has 15 commits on b4337fc: a new patient registration flow (identity step then obstetric step, RegistrationFlow.kt and RegistrationFlowDriver.kt, scaffold/header/footer components, patient age required on create, "Incomplete" chip on the directory, previous-pregnancy form with one outcome and one delivery mode, dating method dropdown, removed dead dating-at-registration code and eight unused dating fields from `PatientDraftValues`, UI copy cleanup, severe BP and risk badges on the directory card), plus a last WIP commit of previously uncommitted files. 173 files, +20429 / -1505, 23 of them test files.
- Goal: the header pencil and the New patient button open the new flow on main (today the old `PatientEditor` form).
- A dry run `git merge-tree --write-tree main branch` reports content conflicts in: .agents/scripts/README.md, state/AncState.kt, storage/PatientCommands.kt, ui/AncApp.kt, ui/AncDatePicker.kt, ui/PatientListScreen.kt, ui/patienthub/PatientHubHeader.kt, res/values/strings.xml; and a modify/delete conflict on concepts_brief.md (deleted on main, modified on the branch). The conflict hunks are pasted below. Auto-merged files (no textual conflict) include DemoModels.kt, AncViewModel.kt, DraftMapping.kt, PatientPrintHtmlGenerator.kt, ClinicalPresets.kt, EncryptedRecordsTest.kt, AncWorkflowTest.kt: textual auto-merge may still be semantically wrong.
- Tools available: git, gradle (`:app:testDebugUnitTest`, lint), an Android emulator test runner script (full suite 456 tests, about 9 minutes), a phone for manual QA, builder lanes that propose edits as SEARCH/REPLACE blocks, and reviewer models.

## Write the plan
1. Strategy: merge main into the branch, rebase the branch onto main, or merge the branch into main on a new integration branch. Compare for this case (history, conflict count, WIP commit, rollback, bisectability) and pick one. Never touch `main` until the integration branch is fully green.
2. Pre-merge safety: what to snapshot or tag, what audit to run, how to keep the unmerged branch intact as a fallback.
3. Resolution plan for EVERY conflicted file and every semantically risky auto-merged file: what main's side must keep (delivery outcome fields, delivery plan sheet, medication sheet), what the branch's side must keep, and the rule for deciding. Be concrete per hunk where the pasted hunks allow it.
4. Semantic traps to check after textual resolution: `PatientDraftValues` (branch removed 8 dating fields, main added 4 delivery fields), the dating rule `intakeDatingUpdate`, `SavePatientProfileCommand.prepare` and its fingerprint, draft recovery (`DraftMapping`), `startsAnotherAction` lists, `clearsFrozenPatient`, route handling for sheets over tabs, the pencil target, strings that two sides both added, test files that reference removed fields.
5. Ordered steps with a verification after each (compile, unit, lint, targeted emulator classes, full suite), and which steps are done by builder lanes, which by reviewer lanes (builder and reviewer from different model families), and what needs the owner.
6. Stop conditions: when to abandon and fall back.
7. Phone QA checklist for the owner after the merge (what to tap, what must still work: Meds sheet, Plan sheet, delivery outcome, dashboard Delivered filter, registration flow happy path and back gesture, draft recovery).
8. Risks ranked, and anything you would need to see that is not pasted (name the file).
Keep it concrete and short. Plain language.

=== CONFLICTS IN app/src/main/java/com/sherifradwan/ancnative/state/AncState.kt: 5 hunk(s) (ours = main, theirs = registration branch) ===
<<<<<<< main
import com.sherifradwan.ancnative.model.composeDeliveryNotes
=======
import com.sherifradwan.ancnative.model.PreviousPregnancyFormRules
import com.sherifradwan.ancnative.model.effectiveMedicationStatus
import com.sherifradwan.ancnative.model.parseBloodPressure
import com.sherifradwan.ancnative.model.parseVisitFindings
import com.sherifradwan.ancnative.storage.ClinicalRecordKind
>>>>>>> claude/clever-goldwasser-e086ce

<<<<<<< main
        medicationStopUndo = null,
        medicationQuickSave = null,
=======
        registrationFlow = null,
        awaitingAdvance = null,
>>>>>>> claude/clever-goldwasser-e086ce

<<<<<<< main
    val delivered: Boolean = false,
    val deliveryDate: String = "",
    val deliveryMode: String = "",
    val deliveryOutcome: String = "",
    val lmpDate: String = "",
    val datingMethod: String = "",
    val transferDate: String = "",
    val embryoAgeDays: String = "",
    val agreedEdd: String = "",
    val scanDate: String = "",
    val scanGaWeeks: String = "",
    val scanGaDays: String = "",
=======
>>>>>>> claude/clever-goldwasser-e086ce

<<<<<<< main
    /** A one-tap stop or its Undo in flight; it stays after a failed save so the Meds tab can show the cause. */
    val medicationQuickSave: MedicationQuickSave? = null,
    val medicationStopUndo: MedicationStopUndo? = null,
=======
    val registrationFlow: RegistrationFlow? = null,
    val awaitingAdvance: AwaitingAdvance? = null,
>>>>>>> claude/clever-goldwasser-e086ce

<<<<<<< main
/** The draft that drives the delivery plan sheet over the Plan tab, only while it is open for this patient. */
fun AncState.deliveryPlanSheetDraft(patientId: String): PatientDraft? =
    patientDraft?.takeIf { it.deliveryPlan != null && it.patientId == patientId }
=======
sealed interface AncEvent {
    data class SearchChanged(val query: String) : AncEvent
    data object ClearSearch : AncEvent
    data object ShowNewPatientInfo : AncEvent
    data object HideNewPatientInfo : AncEvent
    data class OpenWorkspace(val patientId: String) : AncEvent
    data class SelectSection(val section: WorkspaceSection) : AncEvent
    data class StartVisit(val patientId: String, val visitId: String?, val editorSessionId: String) : AncEvent
    data class StartPatientEditor(
        val patientId: String?,
        val newPatientId: String,
        val editorSessionId: String,
    ) : AncEvent
    data class StartHistoriesEditor(val patientId: String, val editorSessionId: String) : AncEvent
    data class Flow(val event: FlowEvent, val editorSessionId: String, val newPregnancyId: String) : AncEvent
    data class RegistrationNext(val editorSessionId: String, val newPregnancyId: String) : AncEvent
    data class StartPregnancyEditor(
        val patientId: String,
        val pregnancyId: String?,
        val newPregnancyId: String,
        val editorSessionId: String,
    ) : AncEvent
    data class StartStatusEditor(
        val patientId: String,
        val targetStatus: String,
        val editorSessionId: String,
    ) : AncEvent
    data class DraftChanged(val field: DraftField, val value: String) : AncEvent
    data class PatientDraftChanged(val field: PatientDraftField, val value: String) : AncEvent
    data class HistoriesDraftChanged(val field: HistoriesDraftField, val value: String) : AncEvent
    data class PregnancyDraftChanged(val field: PregnancyDraftField, val value: String) : AncEvent
    data class StatusReasonChanged(val value: String) : AncEvent
    data object SubmitPatientProfile : AncEvent
    data object SubmitOtherHistories : AncEvent
    data object SubmitPreviousPregnancy : AncEvent
    data object SubmitPatientStatus : AncEvent
    data class StartDatingEditor(val patientId: String, val editorSessionId: String) : AncEvent
    data class StartUltrasoundEditor(
        val patientId: String,
        val scanId: String?,
        val newScanId: String,
        val editorSessionId: String,
    ) : AncEvent
    data class StartLabEditor(
        val patientId: String,
        val labId: String?,
        val newLabId: String,
        val editorSessionId: String,
        val defaultDate: String = "",
        val defaultLabName: String = "",
    ) : AncEvent
    data class StartMedicationEditor(
        val patientId: String,
        val medicationId: String?,
        val newMedicationId: String,
        val editorSessionId: String,
        val defaultStartDate: String = "",
        /** "Save and add another": open the blank editor but keep the just-shown saved notice. */
        val keepSavedNotice: Boolean = false,
    ) : AncEvent
    data class DatingDraftChanged(val field: DatingDraftField, val value: String) : AncEvent
    data class UltrasoundDraftChanged(val field: UltrasoundDraftField, val value: String) : AncEvent
    data class LabDraftChanged(val field: LabDraftField, val value: String) : AncEvent
    data class LabAbnormalChanged(val isAbnormal: Boolean) : AncEvent
    data class MedicationDraftChanged(val 

=== CONFLICTS IN app/src/main/java/com/sherifradwan/ancnative/storage/PatientCommands.kt: 1 hunk(s) (ours = main, theirs = registration branch) ===
<<<<<<< main
import com.sherifradwan.ancnative.model.DeliveryOutcomeFields
import com.sherifradwan.ancnative.model.normalized
import com.sherifradwan.ancnative.model.validateDeliveryOutcome
=======
import com.sherifradwan.ancnative.model.PreviousPregnancyFormRules
>>>>>>> claude/clever-goldwasser-e086ce


=== CONFLICTS IN app/src/main/java/com/sherifradwan/ancnative/ui/AncApp.kt: 1 hunk(s) (ours = main, theirs = registration branch) ===
<<<<<<< main
import com.sherifradwan.ancnative.state.medicationSheetDraft
=======
import com.sherifradwan.ancnative.state.FlowEvent
import com.sherifradwan.ancnative.state.SystemBack
import com.sherifradwan.ancnative.state.systemBackKind
import com.sherifradwan.ancnative.ui.components.RegistrationFlowScaffold
import com.sherifradwan.ancnative.ui.components.flowMoveEvent
>>>>>>> claude/clever-goldwasser-e086ce


=== CONFLICTS IN app/src/main/java/com/sherifradwan/ancnative/ui/AncDatePicker.kt: 1 hunk(s) (ours = main, theirs = registration branch) ===
<<<<<<< main
                    label = { Text("⚡ " + stringResource(R.string.today) + " (" + LocalDate.now().toString() + ")") },
                    modifier = Modifier.minimumTouchTarget().testTag("${tag}TodayButton"),
=======
                    label = { Text(stringResource(R.string.today) + " (" + LocalDate.now().toString() + ")") },
                    modifier = Modifier.testTag("${tag}TodayButton"),
>>>>>>> claude/clever-goldwasser-e086ce


=== CONFLICTS IN app/src/main/java/com/sherifradwan/ancnative/ui/PatientListScreen.kt: 2 hunk(s) (ours = main, theirs = registration branch) ===
<<<<<<< main
    val isDelivered: Boolean = false,
    val deliveryDate: String? = null,
=======
    val isIncomplete: Boolean = false,
>>>>>>> claude/clever-goldwasser-e086ce

<<<<<<< main
        isDelivered = patient.delivered,
        deliveryDate = patient.deliveryDate,
=======
        isIncomplete = isRegistrationIncomplete(patient),
>>>>>>> claude/clever-goldwasser-e086ce


=== CONFLICTS IN app/src/main/java/com/sherifradwan/ancnative/ui/patienthub/PatientHubHeader.kt: 1 hunk(s) (ours = main, theirs = registration branch) ===
<<<<<<< main
                        Box(
                            modifier = Modifier
                                .testTag("hubRiskBadge")
                        ) {
                            ClinicalRiskBadge(isHighRisk = isHighRisk(patient.riskLevel))
                        }
=======
                        ClinicalRiskBadge(
                            isHighRisk = isHighRisk(patient.riskLevel),
                            isModerate = parseRiskTier(patient.riskLevel) == PatientRiskTier.MEDIUM,
                        )
>>>>>>> claude/clever-goldwasser-e086ce


=== CONFLICTS IN app/src/main/res/values/strings.xml: 1 hunk(s) (ours = main, theirs = registration branch) ===
<<<<<<< main
    <string name="medication_route_locked_note">A different route is a new drug.</string>
    <string name="medication_route_locked_label">Route (read-only)</string>
    <string name="medication_stop_date">Stop date</string>
    <string name="medication_frequency_other">Other</string>
    <string name="medication_frequency_other_label">Frequency in words</string>
    <string name="medication_frequency_required">Choose a frequency.</string>
    <string name="medication_frequency_once_daily">Once daily</string>
    <string name="medication_frequency_twice_daily">Twice daily</string>
    <string name="medication_frequency_three_times_daily">Three times daily</string>
    <string name="medication_frequency_at_bedtime">At bedtime</string>
    <string name="medication_frequency_as_needed">As needed (PRN)</string>
    <string name="medication_saving">Saving…</string>
    <string name="medication_save_failed_title">The medication was not saved.</string>
    <string name="medication_stopped_undo_message">Stopped: %1$s</string>
    <!-- Delivery outcome (delivered flag, date, mode, outcome) -->
    <string name="delivery_outcome_section">Delivery outcome</string>
    <string name="delivery_outcome_delivered_switch">Delivered</string>
    <string name="delivery_outcome_date_label">Delivery date (YYYY-MM-DD)</string>
    <string name="delivery_outcome_date_hint">Leave empty if the date is not known.</string>
    <string name="delivery_outcome_mode_label">Mode of delivery</string>
    <string name="delivery_outcome_result_label">Outcome</string>
    <string name="delivered_headline">Delivered %1$s</string>
    <string name="delivered_date_unknown">Delivered, date ???</string>
    <string name="delivered_today">Today</string>
    <string name="delivered_days_ago">%1$d days ago</string>
    <string name="delivered_one_day_ago">1 day ago</string>
    <string name="delivery_error_date_invalid">Enter the date as YYYY-MM-DD, for example 2026-11-05.</string>
    <string name="delivery_error_date_future">The delivery date cannot be after today.</string>
    <string name="delivery_error_date_before_dating">The delivery date is before the last menstrual period.</string>
    <string name="delivery_error_choice_invalid">Choose one of the listed values.</string>
    <string name="patients_filter_delivered">Delivered</string>
    <string name="delivered_patients_empty">No delivered patients.</string>
=======
    <string name="registration_step_identity">Identity</string>
    <string name="registration_step_histories">Histories</string>
    <string name="registration_header">%1$s: %2$s</string>
    <string name="registration_status_skipped">Skipped</string>
    <string name="registration_status_pending">To do</string>
    <string name="registration_current">Current</string>
    <string name="registration_segment_cd">%1$s, %2$s</string>
    <string name="registration_segment_current_cd">%1$s, %2$s, current</string>
    <string name="registration_pregnancy_progress">Pregnancy %1$d of %2$d to add</string>
    <string name="flow_skip">Skip</string>
    <string name="flow_finish">Finish</string>
    <string name="registration_count_title">How many previous pregnancies?</string>
    <string name="registration_count_label">Number of previous pregnancies</string>
    <string name="registration_count_hint">Suggested from the gravida count. Change it if the record differs. 0 to %1$d.</string>
    <plurals name="registration_count_existing">
        <item quanti