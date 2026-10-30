ROLE: UI BUILDER for ROUND 3g. Implement Q66 and Q67.
ALLOWED files: app/src/main/java/com/sherifradwan/ancnative/ui/UltrasoundEditor.kt, ui/UltrasoundPhotoViewerDialog.kt, ui/WorkspaceScreen.kt, ui/ProfileScreen.kt (only the UltrasoundPhotoViewerDialog( call sites). Use the Edit tool directly.
Reuse: scanPhotoFileName, PatientAttachmentVault.resolveAttachmentFile, BoundedBitmap.decodeFile (all exist or are written by the core lane); no new decode routine. Keep every testTag, including image_unavailable.
Run: python3 .agents/checks/check_r3g.py, python3 .agents/checks/check_r3e.py, python3 .agents/checks/check_r3f2.py, ./gradlew --offline :app:compileDebugKotlin :app:compileReleaseKotlin. Report real results.
