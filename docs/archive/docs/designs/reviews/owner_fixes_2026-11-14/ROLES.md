# Roles (MACA, independence rule: test designer != builder; reviewer != author)
- Orchestrator and verifier: Claude (briefs, applies nothing without checking, runs gates).
- Test designers (spec only, never see the build): DeepSeek (logic tests F2, F4 helpers, F5), GLM (F1, F3 and the emulator UI tests), alphacode as second test designer for F5 screen test.
- Builder A: agy Gemini 3.8 Flash, edit rights only on: ui/DashboardSort.kt (new), ui/PatientListScreen.kt, ui/patienthub/components/OverviewSections.kt, ui/components/DueDateBox.kt (new), strings.xml (add keys only).
- Builder B: agy Gemini 3.8 Flash, edit rights only on: ui/patienthub/PatientHubHeader.kt, ui/patienthub/components/ClinicalAttentionBanner.kt, ui/components/ZoomableImage.kt (new), ui/UltrasoundPhotoViewerDialog.kt, ui/ClinicalAttachmentViewerDialog.kt, strings.xml (add keys only; Builder A and B add DIFFERENT keys).
- Reviewer (read-only, frontier): agy Gemini 4 Argon. Antislop gate (read-only): agy Gemini 3.8 Flash high.
