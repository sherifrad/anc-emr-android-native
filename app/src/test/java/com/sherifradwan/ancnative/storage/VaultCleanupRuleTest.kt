package com.sherifradwan.ancnative.storage

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VaultCleanupRuleTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun dir(root: File, name: String) = File(root, name).also { it.mkdirs() }

    @Test fun missingRootIsFalse() {
        assertFalse(hasVaultDatabase(File(tmp.root, "does-not-exist")))
    }

    @Test fun emptyRootIsFalse() {
        assertFalse(hasVaultDatabase(tmp.newFolder("ns")))
    }

    @Test fun vaultDirWithDatabaseIsTrue() {
        val root = tmp.newFolder("ns")
        File(dir(root, "vault-a"), "records.db").writeText("data")
        assertTrue(hasVaultDatabase(root))
    }

    @Test fun zeroByteDatabaseIsTrue() {
        val root = tmp.newFolder("ns")
        File(dir(root, "vault-a"), "records.db").createNewFile()
        assertTrue(hasVaultDatabase(root))
    }

    @Test fun anyOfSeveralVaultsCounts() {
        val root = tmp.newFolder("ns")
        dir(root, "vault-a")
        File(dir(root, "vault-b"), "records.db").writeText("x")
        assertTrue(hasVaultDatabase(root))
    }

    @Test fun vaultDirWithOnlyDescriptorFilesIsFalse() {
        val root = tmp.newFolder("ns")
        val v = dir(root, "vault-x")
        File(v, "descriptor.json").writeText("{}")
        File(v, "envelope.bin").writeText("e")
        assertFalse(hasVaultDatabase(root))
    }

    @Test fun databaseOutsideVaultDirIsFalse() {
        val root = tmp.newFolder("ns")
        File(root, "records.db").writeText("x")
        File(dir(root, "other"), "records.db").writeText("x")
        assertFalse(hasVaultDatabase(root))
    }

    @Test fun nestedDeeperThanDirectChildIsFalse() {
        val root = tmp.newFolder("ns")
        File(dir(File(root, "vault-a"), "sub"), "records.db").writeText("x")
        assertFalse(hasVaultDatabase(root))
    }

    @Test fun vaultPrefixedFileIsNotADirectory() {
        val root = tmp.newFolder("ns")
        File(root, "vault-file").writeText("x")
        assertFalse(hasVaultDatabase(root))
    }

    private fun runCleanup(root: File, pointer: File): List<String> {
        val calls = mutableListOf<String>()
        cleanUpAbandonedInitialization(
            namespaceRoot = root,
            activePointerFile = pointer,
            cleanupUnpublished = { calls += "cleanup" },
            deleteOrphanAliases = { calls += "aliases" },
        )
        return calls
    }

    @Test fun pointerExistsDoesNothing() {
        val root = tmp.newFolder("ns")
        val pointer = File(root, "active.json").also { it.writeText("{}") }
        assertEquals(emptyList<String>(), runCleanup(root, pointer))
    }

    @Test fun pointerExistsDoesNothingEvenWithoutVaultDatabase() {
        val root = tmp.newFolder("ns")
        dir(root, "vault-a")
        val pointer = File(root, "active.json").also { it.writeText("{}") }
        assertEquals(emptyList<String>(), runCleanup(root, pointer))
    }

    @Test fun noPointerButVaultDatabasePresentCleansUpAndKeepsAliases() {
        val root = tmp.newFolder("ns")
        File(dir(root, "vault-a"), "records.db").writeText("data")
        assertEquals(listOf("cleanup"), runCleanup(root, File(root, "active.json")))
    }

    @Test fun noPointerAndNoVaultDatabaseCleansUpThenDeletesAliasesInThatOrder() {
        val root = tmp.newFolder("ns")
        dir(root, "vault-a")
        assertEquals(listOf("cleanup", "aliases"), runCleanup(root, File(root, "active.json")))
    }

    @Test fun noPointerAndMissingNamespaceRootStillCleansUpThenDeletesAliases() {
        val root = File(tmp.root, "does-not-exist")
        assertEquals(listOf("cleanup", "aliases"), runCleanup(root, File(root, "active.json")))
    }

    @Test fun aliasGuardIsEvaluatedAfterCleanup() {
        // cleanupUnpublished may remove an unpublished vault dir; the guard must see the post-cleanup state.
        val root = tmp.newFolder("ns")
        val unpublished = dir(root, "vault-u")
        File(unpublished, "records.db").writeText("x")
        val calls = mutableListOf<String>()
        cleanUpAbandonedInitialization(
            namespaceRoot = root,
            activePointerFile = File(root, "active.json"),
            cleanupUnpublished = {
                calls += "cleanup"
                unpublished.deleteRecursively()
            },
            deleteOrphanAliases = { calls += "aliases" },
        )
        assertEquals(listOf("cleanup", "aliases"), calls)
    }
}
