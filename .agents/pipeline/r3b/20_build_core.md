ROLE: CORE LOGIC BUILDER. Make the tests pass without editing them (tests are owned by another agent and are the contract).
ALLOWED files (package storage under app/src/main/java/com/sherifradwan/ancnative/):
- storage/RecordAnomalies.kt (NEW, Q22)   - storage/VaultAutoAction.kt (NEW, Q23)   - storage/VaultCleanupRule.kt (NEW, Q24 hasVaultDatabase)
- storage/RecordsRepository.kt   (Q22 wiring in validateAndLoad and RepositoryOpenResult.Success only; remove isInternallyValid)
- storage/VaultFiles.kt   (Q24: guard in cleanupUnpublishedArtifacts only)
- storage/VaultController.kt   (Q24: guard both deleteOrphanAliases call sites only)
- MainActivity.kt   (Q23: the LaunchedEffect(state.vaultState) block only)
Never delete or weaken any existing safety check other than the ones Q22 moves. Minimal, readable changes in the surrounding style.
Run: ./gradlew --offline :app:testDebugUnitTest --tests '*RecordAnomaliesTest' --tests '*VaultAutoActionTest' --tests '*VaultCleanupRuleTest' --tests '*VaultStateTest', python3 .agents/checks/check_r3b.py, and ./gradlew --offline :app:compileReleaseKotlin. Report real results.
