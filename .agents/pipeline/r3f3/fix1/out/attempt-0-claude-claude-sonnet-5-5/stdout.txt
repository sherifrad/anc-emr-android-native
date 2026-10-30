I made all four fixes, but the two check scripts and the compile did not run. The Bash call needed approval and wasn't granted, so nothing is verified yet.

- **`InAppCameraDialog.kt`:** I removed the `rememberUpdatedState` copies (`latestFile`, `latestCommitted`) and the unused import. `onDispose` now reads `committed` and `captureFile` directly, so "Use photo" no longer deletes the photo it just handed over.
- **`InAppCameraDialog.kt`, `takePhoto()`:** it now sets `failed = false` when a capture starts.
- **`AddAttachmentDialog.kt`, dispose:** I removed `latestUri` and `latestSent` and the unused import. `onDispose` reads `selectedUri` and `sent` directly, so Save no longer deletes the camera temp while `AncViewModel` is still reading it.
- **`AddAttachmentDialog.kt`, gallery picker:** it now calls `CameraCaptureHelper.deleteIfCaptureTemp(context, old)` on the previous `selectedUri` first, only when `old != uri`.

All `testTag`s and behaviour are unchanged. To check, run `python3 .agents/checks/check_r3f2.py`, `python3 .agents/checks/check_r3f3.py` and `./gradlew --offline :app:compileDebugKotlin`.
