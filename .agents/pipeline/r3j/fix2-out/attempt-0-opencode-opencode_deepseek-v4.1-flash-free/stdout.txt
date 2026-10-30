I'll start by locating the exact code and the plural resource.
Import already exists. Making the edit.
- Edited `WorkspaceScreen.kt:2050`: only the count segment is now `${pluralStringResource(R.plurals.photos_count, scan.imageUris.size, scan.imageUris.size)}`; prefix/format unchanged, no comment added.
- `./gradlew --offline :app:compileDebugKotlin` → BUILD SUCCESSFUL (only pre-existing LocalClipboardManager deprecation warning at line 1276).
- `check_r3j.py` → all PASS (S55, S56, S57, S58).
- No other files touched; no git actions.
