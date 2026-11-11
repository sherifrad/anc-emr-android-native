# Read-only audit brief (do NOT edit any file)
Project: Android EMR for antenatal care (Kotlin, Jetpack Compose). Safety-critical clinical UI, must work on a 360dp wide phone. Repo root is the current directory.
Report format: a markdown list. Each finding = `path:line | severity (BLOCKER/SHOULD/NIT) | what is wrong | suggested fix in one line`. Only report things you verified by reading the file at that line. No praise, no summary of what is fine, max 40 findings, most severe first. Do not invent files or line numbers. Never write patient data. If you find nothing in a category, say "none found".

## Your slice: copy and code comments
Scope: app/src/main/res/values*/strings.xml, all user-visible string literals in app/src/main/java/**/ui/**, and comments in app/src/main/java/**.
Check for:
1. Em dashes (U+2014) and " -- " in user-visible strings.
2. Buzzword/AI copy: unlock, seamless, robust, empower, elevate, "powered by AI", filler like "Let's get started", chatbot closers, exclamation-mark cheerfulness in clinical UI.
3. Fabricated placeholder data shown as real: John Doe, example.com addresses, invented phone numbers or sample metrics outside DemoFixtures/test code.
4. Vague errors ("Something went wrong", "No data") with no cause or next action; Title Case On Every Word in list items or labels; ALL CAPS emphasis; decorative emoji.
5. Code comments: decorative separators (=====, -----), step-by-step narration, restating the next line, end-of-block markers, vague TODOs, multi-line comments that should be one line. Do NOT flag comments explaining clinical rules, safety, workarounds or protocols.
6. Strings hardcoded in Kotlin that belong in strings.xml, and strings.xml entries that no code references.
