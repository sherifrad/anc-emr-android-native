ROLE: CORE BUILDER for ROUND 3g. Implement Q63, Q64, Q65. Make the tests and checks pass without editing them.
ALLOWED files (app/src/main/java/com/sherifradwan/ancnative/): storage/ScanPhotoRef.kt (NEW), storage/CameraCaptureHelper.kt, state/AncState.kt, state/AncViewModel.kt.
Reuse: PatientAttachmentVault.saveAttachment, PhotoQualityPreset, CameraCaptureHelper.deleteIfCaptureTemp, the existing AttachmentFailed banner. No second copy of any rule. Replace (do not keep) the old 2-argument clearStaleCaptures.
Run: ./gradlew --offline :app:testDebugUnitTest (whole suite), python3 .agents/checks/check_r3g.py, every older .agents/checks/check_r3*.py, ./gradlew --offline :app:compileDebugKotlin :app:compileReleaseKotlin. Use the Edit tool directly. Report real results and assumptions.
