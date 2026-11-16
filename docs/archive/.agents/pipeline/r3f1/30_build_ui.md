ROLE: UI BUILDER for ROUND 3f-1. Implement Q48 (UI call sites) and Q52.
ALLOWED files: app/src/main/java/com/sherifradwan/ancnative/ui/WorkspaceScreen.kt, ui/ClinicalAttachmentViewerDialog.kt, ui/UltrasoundPhotoViewerDialog.kt, ui/AddAttachmentDialog.kt; app/src/main/res/values/strings.xml.
Call the existing BoundedBitmap helper (storage/BoundedBitmap.kt, written by the core lane) - do not write another decode routine. Keep every existing testTag and behaviour (including "Image unavailable" and image_unavailable). The banner reuses the existing saved-notice card layout.
Run: python3 .agents/checks/check_r3f1.py (S30, S34 are yours), python3 .agents/checks/check_r3e.py, ./gradlew --offline :app:compileDebugKotlin. Report real results.
