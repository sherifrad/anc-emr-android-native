I made both edits to `AncViewModel.kt`, but I couldn't run the build, the unit tests or `check_r3f1.py`. Each run needed approval and wasn't granted, so nothing is verified yet.

- **`deleteAttachment`:** it now keeps the result of `controller.deleteAttachment(...)`. If that returns false, it dispatches `AttachmentFailed(DELETE_FAILED)` and returns before touching the file. Only when the row delete succeeded does it call `PatientAttachmentVault.deleteAttachment`. The existing exception handling is unchanged.
- **`addAttachment`:** if `controller.saveAttachment(...)` returns false, it calls `PatientAttachmentVault.deleteAttachment` with `savedResult.patientId` and `savedResult.fileName`. It then dispatches `AttachmentFailed(SAVE_FAILED)`. I didn't add a check on the file delete's return value, so its failure is ignored.
- **Unchanged:** the `Log.e` calls and the `CancellationException` rethrow. I added no new helpers and edited no other file.

Please approve these two commands, or run them yourself, and I'll report the results:
- `./gradlew --offline :app:compileDebugKotlin :app:testDebugUnitTest`
- `python3 .agents/checks/check_r3f1.py`
