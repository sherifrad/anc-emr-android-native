package com.sherifradwan.ancnative.storage

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CameraCaptureSweepTest {
    @get:Rule val tmp = TemporaryFolder()

    private val now = 10_000_000L
    private val limit = 3_600_000L

    private fun file(name: String, ageMs: Long): File =
        tmp.newFile(name).also { it.setLastModified(now - ageMs) }

    private fun sweep(referenced: Set<String> = emptySet()) =
        staleCaptureFiles(tmp.root, limit, now, referenced).map { it.name }.sorted()

    @Test fun oldUnreferencedIsListed() {
        file("old.jpg", limit + 60_000)
        assertEquals(listOf("old.jpg"), sweep())
    }

    @Test fun oldReferencedIsKept() {
        file("old.jpg", limit + 60_000)
        file("gone.jpg", limit + 60_000)
        assertEquals(listOf("gone.jpg"), sweep(setOf("old.jpg")))
    }

    @Test fun newFileIsKept() {
        file("new.jpg", 60_000)
        assertEquals(emptyList<String>(), sweep())
    }

    @Test fun subfoldersAreIgnored() {
        tmp.newFolder("sub").setLastModified(now - limit - 60_000)
        assertEquals(emptyList<String>(), sweep())
    }

    @Test fun missingFolderGivesEmptyList() {
        assertEquals(emptyList<File>(), staleCaptureFiles(File(tmp.root, "nope"), limit, now, emptySet()))
    }
}
