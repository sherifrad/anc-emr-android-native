# rd40 Build Report

Branch: `redesign-2026-11-15`

## Files changed

1. `app/src/main/java/com/sherifradwan/ancnative/security/SecuritySettingsRepository.kt`
   - Extended `SecuritySettingsState` with `doctorNameEn`, `doctorNameAr`, `doctorSpecialtyEn`, `doctorSpecialtyAr`, `clinicPhone` and companion default constants (`DEFAULT_DOCTOR_NAME_EN`, `DEFAULT_DOCTOR_NAME_AR`, `DEFAULT_DOCTOR_SPECIALTY_EN`, `DEFAULT_DOCTOR_SPECIALTY_AR`, `DEFAULT_CLINIC_PHONE`).
   - Loaded the five keys in `loadSettings()` with sanitization and default fallback.
   - Added `updateClinicAttestation(nameEn, specialtyEn, nameAr, specialtyAr, phone)` with trimming, length caps (80 for names/titles, 24 for phone), phone character filtering (`'0'..'9'`, `+`, space, `-`, `(`, `)`), blank fallback to defaults, SharedPreferences persistence, and `StateFlow` update.
   - Kept all PIN, hashing, lockout, biometric, keystore, backup, and `updateClinicBranding` code unchanged; Arabic text remains permitted in the Arabic attestation fields.
2. `app/src/main/java/com/sherifradwan/ancnative/ui/SecuritySettingsDialog.kt`
   - Added five `SecurityLabeledField` inputs in the existing Clinic branding `RecordSectionCard` using `FormFieldLabel` and string resources (`settingsDoctorNameEn`, `settingsDoctorSpecialtyEn`, `settingsDoctorNameAr`, `settingsDoctorSpecialtyAr`, `settingsClinicPhone`).
   - Added `rtlAwareTextStyle` using `isRtlText`, `TextDirection.Rtl`/`TextDirection.Ltr`, and `TextAlign.Right`/`TextAlign.Left` for `settingsDoctorNameAr` and `settingsDoctorSpecialtyAr`.
   - Configured `KeyboardType.Phone` and `TextDirection.Ltr` on `settingsClinicPhone`.
   - Updated the `saveBrandingButton` click handler to also call `securityRepo.updateClinicAttestation(...)` and sync local edited attestation state from `securityRepo.settings.value`.
   - Preserved every existing field, `testTag`, and behavior.
3. `app/src/main/java/com/sherifradwan/ancnative/ui/report/ReportSections.kt`
   - Removed private constant `CLINIC_PHONE`.
   - Added shared `ReportAttestationValues` and `SecuritySettingsState?.resolvedAttestation()` helper falling back to `SecuritySettingsState.DEFAULT_*`.
   - Updated `ReportAttestationCard(securitySettings)` to render `doctorNameEn`, `doctorSpecialtyEn`, `doctorNameAr` (via `PatientNameText`), `doctorSpecialtyAr` (via `PatientNameText`), and `ltrIsolate(attestation.clinicPhone)` from `securitySettings.resolvedAttestation()`.
4. `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientPrintHtmlGenerator.kt`
   - Removed private constant `CLINIC_PHONE`.
   - Updated `generateHtml(context, patient, settings, asOfDate)` to read `settings.resolvedAttestation()` and pass all five values through `escapeHtml(...)`.
   - Updated page 1 header (`doctorOfficialTitle`), page 1 footer (`doctorOfficialTitle`, `clinicPhone`), page 2 signature box (`doctorNameEn`, `doctorNameAr`, `doctorSpecialtyAr`), and page 2 footer (`doctorOfficialTitle`, `clinicPhone`).
5. `app/src/main/res/values/strings.xml`
   - Added 5 sentence-case field labels: `security_doctor_name_en_label`, `security_doctor_specialty_en_label`, `security_doctor_name_ar_label`, `security_doctor_specialty_ar_label`, `security_clinic_phone_label`.

Checked without edits:
- `app/src/main/java/com/sherifradwan/ancnative/ui/report/PatientShareText.kt`: inspected lines 1-128; carries patient details only (no doctor name, doctor specialty, or clinic phone).

## New keys and defaults

Single canonical default source in `SecuritySettingsState.Companion`:

| State property | SharedPreferences key | Default value | Max length |
|---|---|---|---|
| `doctorNameEn` | `key_doctor_name_en` (`KEY_DOCTOR_NAME_EN`) | `Dr. Sherif Radwan` | 80 |
| `doctorSpecialtyEn` | `key_doctor_specialty_en` (`KEY_DOCTOR_SPECIALTY_EN`) | `Specialist of Obstetrics, Gynecology & Laparoscopy` | 80 |
| `doctorNameAr` | `key_doctor_name_ar` (`KEY_DOCTOR_NAME_AR`) | `شريف جمال رضوان` | 80 |
| `doctorSpecialtyAr` | `key_doctor_specialty_ar` (`KEY_DOCTOR_SPECIALTY_AR`) | `أخصائي التوليد وأمراض النساء وتأخر الحمل` | 80 |
| `clinicPhone` | `key_clinic_phone` (`KEY_CLINIC_PHONE`) | `01201720808` | 24 (`0-9 + space - ( )`) |

## Strings and constants removed

- Removed `private const val CLINIC_PHONE = "01201720808"` from `ui/report/ReportSections.kt` and `ui/report/PatientPrintHtmlGenerator.kt`.
- Removed all references to `R.string.report_doctor_name_en`, `R.string.report_doctor_specialty_en`, `R.string.report_doctor_name_ar`, and `R.string.report_doctor_specialty_ar` from `app/src/main/` (`ReportSections.kt`).
- Retained `report_doctor_name_en`, `report_doctor_specialty_en`, `report_doctor_name_ar`, and `report_doctor_specialty_ar` in `app/src/main/res/values/strings.xml` because `app/src/androidTest/java/com/sherifradwan/ancnative/ReportDialogRedesignTest.kt` still references them at lines 500, 502, 504, 506, and 933, and test files could not be edited under task safety constraints while `:app:compileDebugAndroidTestKotlin` must pass. Once `ReportDialogRedesignTest.kt` is updated, those four strings can be deleted cleanly.

## Callers changed

- `SecuritySettingsDialog.kt`: Save button (`saveBrandingButton`) calls `securityRepo.updateClinicAttestation(...)` alongside `securityRepo.updateClinicBranding(...)`.
- `ReportSections.kt`: `ReportAttestationCard(securitySettings)` consumes `securitySettings.resolvedAttestation()` instead of `R.string.report_doctor_*` and `CLINIC_PHONE`. Existing upstream callers (`PatientHubScreen.kt`, `PatientTwoPageReportDialog.kt`, `ReportPageTwoSections`) already pass `securitySettings`.
- `PatientPrintHtmlGenerator.kt`: `generateHtml(context, patient, settings, asOfDate)` consumes `settings.resolvedAttestation()`. Existing callers (`PatientPrintManager.printPatientReport`, `PatientTwoPageReportDialog.kt`) already pass `settings = securitySettings`.
- `PatientShareText.kt`: unchanged (contains neither doctor info nor clinic phone).

## Verification and parity evidence

- `./gradlew --offline :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin -q`: passed (exit code 0).
- `./gradlew --offline :app:testDebugUnitTest -q`: passed (exit code 0).
  - Total unit tests across `app/build/test-results/testDebugUnitTest/TEST-*.xml`: 1616 tests, 0 failures, 0 errors, 0 skipped.
  - Report and security suites unchanged and passing:
    - `PatientPrintHtmlGeneratorTest`: 59 passed
    - `ReportSectionsTest`: 51 passed
    - `PatientPrintHtmlEscapeTest`: 22 passed
    - `ExportFixRoundTwoTest`: 15 passed
    - `PrintLayoutTest`: 12 passed
    - `PrintObstetricSummaryTest`: 6 passed
    - `PrintSectionTitlesTest`: 3 passed
    - `PrintBpAlertTest`: 1 passed
    - `PrintSampleDumpTest`: 1 passed
    - `SecuritySettingsRepositoryTest`: 17 passed
    - `AppLockPolicyTest`: 8 passed
    - `PinPolicyTest`: 14 passed
    - `IsRtlTextTest`: 9 passed
- Default-settings parity:
  - Native `ReportAttestationCard` with `null` or default `SecuritySettingsState()` resolves to the exact previous English and Arabic doctor name/specialty strings and `"01201720808"` inside `ltrIsolate`.
  - `PatientShareText.kt` was not modified; share text is byte-identical.
  - `app/build/print-sample/report.html` generated by `PrintSampleDumpTest` (`settings = null`) shows the expected attestation block (`Dr. Sherif Radwan`, `شريف جمال رضوان`, `أخصائي التوليد وأمراض النساء وتأخر الحمل`, `Tel 01201720808`) and `Specialist of Obstetrics, Gynecology &amp; Laparoscopy` (escaped via `escapeHtml`, identical HTML rendering to unescaped `&` and identical across `settings = null` and default `SecuritySettingsState()`).
