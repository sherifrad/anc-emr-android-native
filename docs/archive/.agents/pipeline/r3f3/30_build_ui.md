ROLE: UI BUILDER for ROUND 3f-3. Implement Q62 in ui/AddAttachmentDialog.kt and the Q61 cleanup in ui/InAppCameraDialog.kt (remove its decodeUpright/EXIF code; decode the still through the existing BoundedBitmap).
ALLOWED files: app/src/main/java/com/sherifradwan/ancnative/ui/AddAttachmentDialog.kt, ui/InAppCameraDialog.kt. Keep every testTag and behaviour. Buttons stay >= 48dp.
Run: python3 .agents/checks/check_r3f3.py, python3 .agents/checks/check_r3f2.py, ./gradlew --offline :app:compileDebugKotlin :app:compileReleaseKotlin. Report real results.
