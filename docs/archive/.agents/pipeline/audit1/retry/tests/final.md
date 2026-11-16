## Team GPT-6 Luna — lens TEST QUALITY and CHECK SCRIPTS

**Dominant pattern:** tests mostly use descriptive camelCase method names, one behavior per test, and JUnit assertions; manifest entries pair a command with explicit source/test dependencies.

### Bugs

| # | Sev | file:line | quote (≤1 line) | failure scenario (input → wrong result) | fix (1 line) | confidence |
|---|---|---|---|---|---|---|
| 1 | Should-fix | `.agents/TEST_MANIFEST.json:177-186` | `"deps": [ "...BiometricAuthManager.kt", "...MainActivity.kt", ".agents/checks/check_r3c.py" ]` | An `AppLockScreen.kt` or `AncApp.kt` change can alter the behaviors checked by S15/S16, but won’t trigger `STATIC-r3c` in a dependency-based runner. The checker explicitly reads those files at `.agents/checks/check_r3c.py:165-176`. | Add both files to `STATIC-r3c` dependencies. | H |
| 2 | Should-fix | `.agents/checks/check_r3d.py:98-105` | `ifs = list(re.finditer(r"\bif\s*\(", code[:pos]))` | If `FLAG_SECURE` is unconditional but any earlier unrelated `if (!BuildConfig.DEBUG)` exists, the checker can accept the source: it checks the nearest prior `if`, not whether that `if` encloses the flag. | Parse the enclosing block or test the actual runtime behavior. | H |
| 3 | Nit | `app/src/test/java/com/sherifradwan/ancnative/ui/ParseDeliveryNotesSpecTest.kt:44-51` | `secLine.contains(it)` | A parser output containing a required phrase plus extra or altered text can count as preserving the original line. This weakens the losslessness oracle wherever `countOfAllowingLabelStrip` is used. | Compare normalized whole output segments, not substring containment. | H |
| 4 | Nit | `app/src/test/java/com/sherifradwan/ancnative/storage/EncryptedBackupTest.kt:286-298` | `assertTrue(text.contains("PREGNANCY DATING"))` | A summary with headings but missing or corrupted clinical values can pass this “comprehensive” test. | Assert representative patient values and child-record details in the generated summary. | H |

### Style groups

| rule | deviation | count | examples file:line | introduced by (commit/model if known) |
|---|---|---:|---|---|
| Keep tests consistently structured; prefer separate `@Test` and function lines when using block-bodied tests. | Compact one-line test declarations are mixed with block-bodied declarations. | 1 file | `app/src/test/java/com/sherifradwan/ancnative/security/AppLockPolicyTest.kt:10` | `5fd691da` (author shown as `sherifrad`; model unknown) |

### Not covered / not verified

- **Riskiest functions with no dedicated named test found in `app/src/test`:** `RecordsMigration.kt:12` `migrate` (1→2); `RecordsMigration.kt:111` `migrate` (2→3); `RecordsMigration.kt:259` `migrate` (3→4); `RecordsMigration.kt:291` `migrate` (4→5); `RecordsMigration.kt:517` `failureReason`; `AndroidVaultKeys.kt:91` `wrap`; `AndroidVaultKeys.kt:108` `unwrap`; `AndroidVaultKeys.kt:148` `deleteOrphanAliases`; `VaultFiles.kt:198` `writeAtomic`; `VaultController.kt:505` `restoreEncryptedBackup`. These are test-coverage gaps, not confirmed runtime defects.
- **Mutation examples that may still pass high-risk tests:**
  1. `STATIC-r3d`: move `FLAG_SECURE` outside the release-only `if`, leaving an earlier `if (!BuildConfig.DEBUG)` in the file; the current checker may still pass.
  2. Parser losslessness helper: append extra text to a parsed line while keeping the required source phrase as a substring; `countOfAllowingLabelStrip` may still pass.
  3. Clinical summary: keep the tested headings/name/case ID but remove actual visit/lab/medication values; the summary test can still pass.
- The project’s static checks state they verify textual contracts, not runtime behavior (for example, `.agents/checks/check_r3d.py:4-5`). No tests, scripts, or builds were run; behavior is **not verified by running**.
- `.agents/checks` and `.agents/TEST_MANIFEST.json` were inspected. The requested `OWNER_DECISIONS.md` and excluded audit materials were not read.

### Verdict: YES-after-fixes

Fix first: **(1)** correct `STATIC-r3c` dependencies, **(2)** make `check_r3d.py` validate actual containment, and **(3)** strengthen losslessness and clinical-summary assertions.
