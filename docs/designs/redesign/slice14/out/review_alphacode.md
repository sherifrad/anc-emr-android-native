## Defects found

1) **app/src/main/java/com/sherifradwan/ancnative/ui/VisitEditor.kt (subtitle construction, ~lines shown in diff around subtitle)**  
- **Concrete failure scenario:** With a patient who has a calculable post-conception dating, opening the Visit Editor in the normal (non-saving) state no longer shows gestational age in the editor header/subtitle (now set to `""` when `savePhase == SavePhase.IDLE`). Before this change the subtitle included GA when available (e.g. showed patient name plus GA). This is a real loss of at-a-glance clinical context in the editor chrome (GA still appears later in Step 0/review, but the header no longer presents it), which is a behavior/display regression versus prior behavior.

2) **app/src/main/java/com/sherifradwan/ancnative/ui/VisitEditor.kt (patient context removed in save/error/unresolved states)**  
- **Concrete failure scenario:** When `savePhase != SavePhase.IDLE` (saving, error, unresolved), the editor no longer renders the patient banner/card context that was previously shown in the fallback path (old code showed `ClinicalPatientCard` in all states; new code only shows `VisitPatientBanner` when `savePhase == SavePhase.IDLE`). If a save fails or is pending/erroring and the fallback/recorded-values UI is shown, the user loses patient identity/context at the moment they must interpret/act on the error state. This is a regression in displayed clinical context during error handling.

## Notes (not reported as defects)
- `DesignSystem.kt`: `SummaryRow`/`ValueStepper` new optional params defaulted (`useRecordStyle=false`, etc.); no signature break for existing callers inferred from defaults.
- Accessibility/hygiene: touch targets and record-style semantics look intentional in the new steps; no hardcoded em dashes or obvious dead code introduced in the reviewed diff; localized strings added for new UI text.

If you want, I can also call out the `formatVisitSummary` GA text source change (`displayText` → `shortWeeksText`) as a potential unintended copy regression, but it is borderline “style restyle”; the two items above are the clearest real regressions.
[Tokens] upload: 49378 download: 4744 cache_read: 132 cache_write: 0

