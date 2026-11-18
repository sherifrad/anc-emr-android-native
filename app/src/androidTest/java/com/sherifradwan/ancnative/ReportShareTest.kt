package com.sherifradwan.ancnative

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sherifradwan.ancnative.model.DemoFixtures
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.ui.AncTheme
import com.sherifradwan.ancnative.ui.report.PatientTwoPageReportDialog
import com.sherifradwan.ancnative.ui.report.exportReportPdf
import com.sherifradwan.ancnative.ui.report.purgeStaleReportExports
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportShareTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val patient: DemoPatient = DemoFixtures.patients.first()

    @After
    fun tearDown() {
        runCatching {
            UiDevice.pressBack()
        }
    }

    @Test
    fun tappingShareIcon_showsBothMenuItems_andShareShortTextDoesNotCrash() {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                AncTheme {
                    PatientTwoPageReportDialog(
                        patient = patient,
                        securitySettings = null,
                        onDismiss = {},
                    )
                }
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag("shareReportButton").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("shareReportPdfItem").assertIsDisplayed()
        compose.onNodeWithTag("shareReportTextItem").assertIsDisplayed()

        compose.onNodeWithTag("shareReportTextItem").performClick()
        compose.waitForIdle()
    }

    @Test
    fun fileProvider_resolvesCacheReportsFile_andRejectsCacheRootDir() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val authority = "${context.packageName}.fileprovider"

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val reportsFile = File(reportsDir, "test_share_export.pdf").apply {
            writeText("dummy content")
        }

        try {
            val uri = FileProvider.getUriForFile(context, authority, reportsFile)
            assertNotNull("URI for file in cacheDir/reports must resolve", uri)
            assertEquals("content", uri.scheme)
            assertEquals(authority, uri.authority)
        } finally {
            reportsFile.delete()
        }

        val cacheRootFile = File(context.cacheDir, "test_root_file.pdf").apply {
            writeText("dummy content")
        }
        try {
            var threwExpected = false
            try {
                FileProvider.getUriForFile(context, authority, cacheRootFile)
            } catch (_: IllegalArgumentException) {
                threwExpected = true
            }
            assertTrue(
                "File written directly to cacheDir root must throw IllegalArgumentException because only cache/reports is exposed in file_paths.xml",
                threwExpected,
            )
        } finally {
            cacheRootFile.delete()
        }
    }

    @Test
    fun exportReportPdf_withActivity_returnsPdfStartingWithPdfHeader_andDeletesStaleFiles() {
        Assume.assumeTrue("WebView cannot render in this environment", webViewCanLoadPages())
        val activity = compose.activity
        val reportsDir = File(activity.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val staleFile = File(reportsDir, "stale_report_to_delete.pdf").apply {
            writeText("stale pdf content")
            setLastModified(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2))
        }
        assertTrue("Stale file should exist before export", staleFile.exists())

        val pdfFile = runBlocking {
            exportReportPdf(
                activity = activity,
                patient = patient,
            )
        }

        try {
            assertNotNull("exportReportPdf must return non-null File", pdfFile)
            assertTrue("Exported PDF file must exist on disk", pdfFile!!.exists())
            assertTrue("Exported PDF file must be non-empty", pdfFile.length() > 0L)

            val header = ByteArray(4)
            pdfFile.inputStream().use { it.read(header) }
            assertEquals("%PDF", String(header))

            assertFalse("Stale file older than 1 hour must be deleted", staleFile.exists())
        } finally {
            pdfFile?.delete()
            staleFile.delete()
        }
    }

    @Test
    fun exportReportPdf_twoExportsOfSamePatient_produceUniqueFileNames() {
        Assume.assumeTrue("WebView cannot render in this environment", webViewCanLoadPages())
        val activity = compose.activity
        var firstExport: File? = null
        var secondExport: File? = null

        try {
            firstExport = runBlocking {
                exportReportPdf(
                    activity = activity,
                    patient = patient,
                )
            }
            Thread.sleep(20)
            secondExport = runBlocking {
                exportReportPdf(
                    activity = activity,
                    patient = patient,
                )
            }

            assertNotNull("First export should succeed", firstExport)
            assertNotNull("Second export should succeed", secondExport)
            assertTrue("First export file should exist on disk", firstExport!!.exists())
            assertTrue("Second export file should exist on disk", secondExport!!.exists())
            assertTrue("First export file should end with .pdf", firstExport.name.endsWith(".pdf"))
            assertTrue("Second export file should end with .pdf", secondExport.name.endsWith(".pdf"))
            assertNotEquals(
                "Two exports of the same patient must produce different unique file names",
                firstExport.name,
                secondExport.name,
            )
        } finally {
            firstExport?.delete()
            secondExport?.delete()
        }
    }

    @Test
    fun purgeStaleReportExports_deletesTwoHourOldFile_keepsFreshFile_andNeverTouchesFileOutsideReports() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val staleFile = File(reportsDir, "stale_report_to_purge.pdf").apply {
            writeText("stale report 2 hours old")
            setLastModified(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2))
        }
        val freshFile = File(reportsDir, "fresh_report_to_keep.pdf").apply {
            writeText("fresh report newly created")
            setLastModified(System.currentTimeMillis())
        }
        val outsideFile = File(context.cacheDir, "outside_cache_file.pdf").apply {
            writeText("file in cache root older than 1 hour")
            setLastModified(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2))
        }

        try {
            assertTrue("Stale file in reports must exist before purge", staleFile.exists())
            assertTrue("Fresh file in reports must exist before purge", freshFile.exists())
            assertTrue("File outside cacheDir/reports must exist before purge", outsideFile.exists())

            runBlocking {
                purgeStaleReportExports(context.cacheDir)
            }

            assertFalse("Stale file 2 hours old in cacheDir/reports must be deleted", staleFile.exists())
            assertTrue("Fresh file in cacheDir/reports must be kept", freshFile.exists())
            assertTrue(
                "File outside cacheDir/reports must never be touched even if 2 hours old",
                outsideFile.exists(),
            )
        } finally {
            staleFile.delete()
            freshFile.delete()
            outsideFile.delete()
        }
    }

    private fun webViewCanLoadPages(): Boolean {
        val pageLatch = CountDownLatch(1)
        val jsLatch = CountDownLatch(1)
        val jsSuccess = AtomicBoolean(false)
        var webView: WebView? = null
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                webView = WebView(compose.activity).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            pageLatch.countDown()
                        }
                    }
                    loadDataWithBaseURL(null, "<html><body>test</body></html>", "text/html", "UTF-8", null)
                }
            }
            if (!pageLatch.await(10, TimeUnit.SECONDS)) {
                return false
            }
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                webView?.evaluateJavascript("1+1") { result ->
                    if (result == "2") {
                        jsSuccess.set(true)
                    }
                    jsLatch.countDown()
                }
            }
            return jsLatch.await(10, TimeUnit.SECONDS) && jsSuccess.get()
        } catch (_: Throwable) {
            return false
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                runCatching {
                    webView?.destroy()
                }
            }
        }
    }
}
