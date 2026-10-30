package com.sherifradwan.ancnative.storage

import java.io.File

internal fun hasVaultDatabase(namespaceRoot: File): Boolean =
    namespaceRoot.listFiles().orEmpty().any { child ->
        child.isDirectory && child.name.startsWith("vault-") && File(child, "records.db").isFile
    }

internal fun cleanUpAbandonedInitialization(
    namespaceRoot: File,
    activePointerFile: File,
    cleanupUnpublished: () -> Unit,
    deleteOrphanAliases: () -> Unit,
) {
    if (activePointerFile.exists()) return
    cleanupUnpublished()
    if (!hasVaultDatabase(namespaceRoot)) {
        deleteOrphanAliases()
    }
}
