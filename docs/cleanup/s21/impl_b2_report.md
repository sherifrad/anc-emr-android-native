# Slice 21 Cleanup: Batch 2 Implementation Report

Branch: `redesign-2026-11-15`
Scope: Batch 2 unused resources (`app/src/main/res/values/strings.xml` and `app/src/main/res/drawable/ic_launcher_background.xml`).

## Items Done (Verified Removed)

1. `app/src/main/res/drawable/ic_launcher_background.xml`:
   - Deleted obsolete vector drawable.
   - Pre-removal check `git grep -n "\bic_launcher_background\b" app/src` and `git grep -n "drawable/ic_launcher_background" app/src` confirmed zero references to `@drawable/ic_launcher_background` or `R.drawable.ic_launcher_background` (adaptive launcher icons reference `@color/ic_launcher_background` in `values/ic_launcher_background.xml`).

2. `app/src/main/res/values/strings.xml` (113 string/plurals entries removed):
   - Prior to deletion, verified every single symbol with `git grep -n "\b${sym}\b" app/src`: all 113 symbols had exactly 0 references across `app/src/main`, `app/src/test`, `app/src/androidTest`, XML layouts/drawables, and `AndroidManifest.xml`.
   - Removed exactly the 113 approved entries:
     - Prototype & shell (17): `demo_banner`, `no_visits`, `recorded_visits`, `read_only`, `read_only_profile`, `prototype_placeholder`, `placeholder_body`, `obstetric_history`, `other_histories`, `episode_overview`, `medications_tab`, `synthetic_edit`, `visit_editor`, `secure_demo_storage`, `lock_demo_storage`, `credential_prompt_title`, `credential_prompt_subtitle`.
     - Patient & dating editor notices / fields (12): `synthetic_patient_editor_notice`, `full_name`, `case_id_required`, `gpa_all_or_none`, `archived_read_only_notice`, `no_previous_pregnancies`, `no_date_inference_notice`, `dating_editor_notice`, `add_ultrasound`, `ga_at_scan`, `no_ultrasound_recorded`, `ultrasound_editor_notice`.
     - Trimester / visit / lab / med overview (12): `active_pregnancy_episode`, `milestones`, `first_trimester`, `second_trimester`, `third_trimester`, `latest_scan`, `latest_visit`, `total_visits`, `no_lab_recorded`, `no_medications_recorded`, `invalid_ga`, `add_lab`.
     - Lab / med editor notices & badges (15): `edit_lab`, `lab_editor_notice`, `abnormal_badge`, `normal_badge`, `medication_editor_notice`, `clinical_summary`, `copy_clinical_summary`, `summary_copied`, `quick_actions`, `delivery_planning_notice`, `parity_format`, `normal_risk`, `create_visit_for_scan`, `add_scan_photo`, `detailed_biometry_toggle`.
     - Badges / scan links / prefixes (16): `add_another_medication`, `critical_badge`, `alert_badge`, `note_badge`, `standalone_scan`, `view_linked_ultrasound`, `view_linked_visit`, `calculated_ga_label`, `simulated_photo`, `advanced_med_options`, `patient_age_years`, `ga_prefix`, `edd_prefix`, `unrecorded_risk`, `edd_pending`, `expand_details`.
     - Hub summaries / presets / batch (24): `collapse_details`, `recent_visits`, `recent_scans`, `recent_labs`, `active_medications`, `stopped_medications`, `parity_summary`, `no_record`, `edit_history`, `attachment_save_failed`, `flag_abnormal`, `note_filled`, `lab_preset_negative`, `lab_preset_non_reactive`, `lab_preset_normal`, `lab_preset_positive`, `collapse_all`, `expand_all`, `stopped_in_this_session`, `restart_medication`, `custom_drug_title`, `custom_drug_label`, `active_count_summary`, `other_date`.
     - Visit quick findings & registration (17): `same_as_last_fh`, `matches_ga_fh`, `fundal_height`, `flag_badge`, `lab_batch_review_n_results`, `lab_batch_review_one_result`, `delivered_patients_empty`, `registration_count_title`, `registration_count_label`, `registration_count_hint`, `<plurals name="registration_count_existing">`, `registration_count_error`, `registration_resume_body`, `registration_resume_button`, `delete_attention_entry`, `attention_new_label`, `attention_add_button`.
   - Removed obsolete changelog comment: `<!-- Slice 20: Two-page clinical report dialog -->` at line 1032.
   - Cleaned up blank lines so no double blank lines exist.

## Items Skipped (With Evidence / Reason)

1. `app/src/main/res/values/ic_launcher_background.xml` (`@color/ic_launcher_background`):
   - **Retained (active resource)**. Referenced by `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml:3` and `ic_launcher_round.xml:3`.
2. `app/src/main/res/mipmap-*/ic_launcher_foreground.png` (5 files, Team A C4-10):
   - **Skipped (not in Batch 2)**. Prompt explicitly specified not to delete mipmap foreground PNGs in this batch.
3. `app/src/main/res/drawable/clinic_logo_foreground.png` vs `drawable-nodpi/` (Team A C4-11):
   - **Skipped (not in Batch 2)**. Flagged as uncertain AAPT2 density fallback behavior in Team A review; out of Batch 2 scope.
4. Other strings mentioned in raw candidate lists (`workspace`, `profile`, `case_id`, `visits`, `laboratory`, `medications`, `attachments`, `identity`, `archived`, `end_date`, `flag`, `note`):
   - **Skipped (not in approved Batch 2 list)**. Retained untouched per strict batch scope boundaries.

## Commands and Results

1. Symbol grep verification:
   - Evaluated `git grep -n "\b${sym}\b" app/src` for all 113 symbols and `ic_launcher_background`: 0 external usages found outside `strings.xml` / `ic_launcher_background.xml`.
   - `git grep -n "getIdentifier" app/src`: exit 1 (0 dynamic reflection calls).
2. Resource merge & compilation verification:
   - `./gradlew --offline :app:mergeDebugResources :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin -q`: exit 0 (clean execution).
   - `./gradlew --offline :app:mergeDebugResources :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin --console=plain`: exit 0, `BUILD SUCCESSFUL in 4s` (35 actionable tasks: 3 executed, 32 up-to-date; `:app:mergeDebugResources`, `:app:compileDebugKotlin`, `:app:compileDebugUnitTestKotlin`, `:app:compileDebugAndroidTestKotlin` all passed).
3. Diff verification:
   - Exactly 113 string/plurals entries + 1 changelog comment line removed from `strings.xml` (117 lines removed, 0 lines added).
   - `app/src/main/res/drawable/ic_launcher_background.xml` deleted.
   - Zero protected files or other resources touched.
