package com.sherifradwan.ancnative.storage

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CaptureTempFileTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var cameraDir: File
    private val authority = "com.example.app.fileprovider"

    @Before
    fun setUp() {
        cameraDir = tmp.newFolder("camera")
    }

    private fun resolve(
        segments: List<String>,
        scheme: String? = "content",
        auth: String? = authority,
    ): File? = captureTempFile(cameraDir, authority, scheme, auth, segments)

    @Test
    fun validFileIsReturned() {
        val file = File(cameraDir, "x.jpg").apply { writeText("a") }
        assertEquals(file.canonicalFile, resolve(listOf("camera_photos", "x.jpg")))
    }

    @Test
    fun wrongSchemeIsNull() {
        File(cameraDir, "x.jpg").writeText("a")
        assertNull(resolve(listOf("camera_photos", "x.jpg"), scheme = "file"))
        assertNull(resolve(listOf("camera_photos", "x.jpg"), scheme = null))
    }

    @Test
    fun wrongAuthorityIsNull() {
        File(cameraDir, "x.jpg").writeText("a")
        assertNull(resolve(listOf("camera_photos", "x.jpg"), auth = "evil.provider"))
        assertNull(resolve(listOf("camera_photos", "x.jpg"), auth = null))
    }

    @Test
    fun wrongFirstSegmentIsNull() {
        File(cameraDir, "x.jpg").writeText("a")
        assertNull(resolve(listOf("other", "x.jpg")))
    }

    @Test
    fun singleSegmentIsNull() {
        assertNull(resolve(listOf("camera_photos")))
        assertNull(resolve(emptyList()))
    }

    @Test
    fun missingFileIsNull() {
        assertNull(resolve(listOf("camera_photos", "missing.jpg")))
    }

    @Test
    fun directoryInsteadOfFileIsNull() {
        File(cameraDir, "dir.jpg").mkdir()
        assertNull(resolve(listOf("camera_photos", "dir.jpg")))
    }

    @Test
    fun dotDotSegmentIsNull() {
        File(cameraDir.parentFile, "x.jpg").writeText("outside")
        assertNull(resolve(listOf("camera_photos", "..", "x.jpg")))
    }

    @Test
    fun subFolderIsNull() {
        File(cameraDir, "sub").mkdir()
        File(cameraDir, "sub/x.jpg").writeText("a")
        assertNull(resolve(listOf("camera_photos", "sub", "x.jpg")))
    }

    @Test
    fun segmentWithSeparatorIsNull() {
        File(cameraDir, "a").mkdir()
        File(cameraDir, "a/b.jpg").writeText("a")
        assertNull(resolve(listOf("camera_photos", "a/b.jpg")))
    }

    @Test
    fun symlinkPointingOutsideIsNull() {
        val outside = tmp.newFile("outside.jpg")
        val created = try {
            Files.createSymbolicLink(File(cameraDir, "link.jpg").toPath(), outside.toPath())
            true
        } catch (e: Exception) {
            false
        }
        assumeTrue(created)
        assertNull(resolve(listOf("camera_photos", "link.jpg")))
    }

    @Test
    fun ruleIsLocationBasedNotNameBased() {
        val file = File(cameraDir, "NOTES.Txt").apply { writeText("a") }
        assertEquals(file.canonicalFile, resolve(listOf("camera_photos", "NOTES.Txt")))
        // a differently-cased name must never resolve to some other file
        if (!File(cameraDir, "notes.txt").exists()) {
            assertNull(resolve(listOf("camera_photos", "notes.txt")))
        }
    }
}
