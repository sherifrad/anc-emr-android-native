I made the edits, but I couldn't run any of the Gradle steps, so nothing is verified by a build, the unit suite or lint.

**Files changed:** `LabEditor.kt`, `UltrasoundEditor.kt`, `DatingEditor.kt`, `PreviousPregnancyEditor.kt`, `OtherHistoriesEditor.kt`, `PatientEditor.kt`.

**What I did:**
- **Call sites:** All five editors now call `GenericEditorErrorCard(draft.errors.isNotEmpty(), state)`. Each old call passed `draft.errors`, so the condition is the same.
- **Wrappers:** I deleted `EditorErrorCard` and `ClinicalEditorErrorCard` from `PatientEditor.kt`, along with the `ClinicalValidationError` import, which nothing else in that file used. A grep finds no remaining references to either wrapper.
- **My mistake, fixed:** My first delete took the closing `}` of the function above the wrappers. I added it back.
- **Texts, tags and look:** I didn't touch any of them.

**Checks:**
- `python3 .agents/checks/check_r3z.py`: S101 and S102 both pass.
- The `compileDebugKotlin`, `compileReleaseKotlin` and `compileDebugAndroidTestKotlin` tasks, the whole unit suite and `lintDebug`: not run. The Gradle wrapper failed at startup with `FileNotFoundException: ~/.gradle/wrapper/dists/gradle-8.13-bin/.../gradle-8.13-bin.zip.lck (Operation not permitted)`. The sandbox doesn't allow writes to `~/.gradle`, and I can't change that.

To finish, either allow writes to `~/.gradle` for this session or run `./gradlew --offline :app:compileDebugKotlin :app:compileReleaseKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:lintDebug` yourself.

`PatientEditor.kt` already had uncommitted changes before I started, so its `git diff` shows more deletions than my edit.