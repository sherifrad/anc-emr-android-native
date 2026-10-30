I couldn't run any of the verification, and the check scripts and Gradle were never run. Every `python3` and `./gradlew` call was blocked as needing approval, and this run is non-interactive. All seven fixes are in the code, but none of it has been compiled or tested.

Changes by item:
1. **Stale sweep removed.** `InAppCameraDialog` no longer calls `clearStaleCaptures`. The function stays in `CameraCaptureHelper.kt` with a KDoc line saying it is intentionally not called until scan photos move into the vault.
2. **Dismiss during capture.** A remembered `CameraSession` holder tracks the in-flight file. `onDispose` deletes it, and the `onImageSaved` callback deletes it too if the dialog is already gone. The error callback does the same.
3. **Double tap.** A `capturing` flag stops a second `takePicture`, and the capture button is disabled and greyed out while it's set.
4. **Retake and cancel.**
   - `AddAttachmentDialog` deletes the old camera `selectedUri` when a new photo replaces it.
   - It also deletes an unsent camera `selectedUri` when the dialog is disposed (Cancel, back, app lock). A `sent` flag set on Save prevents that delete.
   - `AncViewModel.addAttachment` calls `deleteIfCaptureTemp` on `event.sourceUri` before returning when the vault is locked.
5. **Decode off the main thread.** The still is decoded with `produceState` on `Dispatchers.Default`, with a progress spinner until it's ready. I did not recycle the displayed bitmap when it's replaced, because recycling a bitmap Compose may still be drawing could crash. I only recycle the intermediate bitmap inside `decodeUpright` after rotation.
6. **Bind failure.** The new string `camera_bind_failed` is shown instead of the capture-failed message, and the capture button is hidden when binding fails.
7. **PreviewView.** It is now created inside the `AndroidView` factory and stored in the session holder, so each Retake gets a fresh one.

To check it, run these yourself:
- `python3 .agents/checks/check_r3f2.py`
- each `python3 .agents/checks/check_r3*.py`
- `./gradlew --offline :app:testDebugUnitTest :app:compileDebugKotlin :app:compileReleaseKotlin`
