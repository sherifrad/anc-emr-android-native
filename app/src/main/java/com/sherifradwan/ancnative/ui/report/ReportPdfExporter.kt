package com.sherifradwan.ancnative.ui.report

import android.app.Activity
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PdfPrint
import android.print.PrintAttributes
import android.webkit.WebView
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.security.SecuritySettingsState
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val REPORTS_DIR_NAME = "reports"
private val STALE_REPORT_MAX_AGE_MS = TimeUnit.HOURS.toMillis(1)
private const val PDF_WRITE_TIMEOUT_MS = 20_000L

private fun deleteStaleReportsIn(reportsDir: File) {
    val cutoffMs = System.currentTimeMillis() - STALE_REPORT_MAX_AGE_MS
    reportsDir.listFiles()?.forEach { existing ->
        if (existing.isFile && existing.lastModified() < cutoffMs) {
            existing.delete()
        }
    }
}

/** Deletes exported PDFs older than one hour inside `cacheDir/reports` off the main thread. */
internal suspend fun purgeStaleReportExports(cacheDir: File) = withContext(Dispatchers.IO) {
    val reportsDir = File(cacheDir, REPORTS_DIR_NAME)
    if (!reportsDir.isDirectory) return@withContext
    deleteStaleReportsIn(reportsDir)
}

/**
 * Generates the report HTML, renders it through the shared [WebView] helper on the main thread,
 * writes an A4 PDF into `cacheDir/reports/<printJobName>_<timestamp>.pdf` off the main thread,
 * and returns the non-empty [File] or `null` on any failure.
 */
suspend fun exportReportPdf(
    activity: Activity,
    patient: DemoPatient,
    settings: SecuritySettingsState? = null,
    selectedPictureKeys: Set<String> = emptySet(),
    pictureDataUris: Map<String, String> = emptyMap(),
): File? {
    if (activity.isFinishing || activity.isDestroyed) return null
    var loadedWebView: WebView? = null
    var pfd: ParcelFileDescriptor? = null
    var targetFile: File? = null
    var success = false

    return try {
        val htmlContent = withContext(Dispatchers.Default) {
            PatientPrintHtmlGenerator.generateHtml(
                context = activity,
                patient = patient,
                settings = settings,
                selectedPictureKeys = selectedPictureKeys,
                pictureDataUri = { pictureDataUris[it.key] },
            )
        }

        loadedWebView = loadReportWebView(activity, htmlContent) ?: return null

        val jobName = PatientPrintManager.printJobName(patient.displayName, patient.caseId)
        val prepared = withContext(Dispatchers.IO) {
            val reportsDir = File(activity.cacheDir, REPORTS_DIR_NAME)
            if (!reportsDir.exists() && !reportsDir.mkdirs() && !reportsDir.isDirectory) {
                return@withContext null
            }
            deleteStaleReportsIn(reportsDir)
            val file = File(reportsDir, "${jobName}_${System.currentTimeMillis()}.pdf")
            val descriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE or
                    ParcelFileDescriptor.MODE_READ_WRITE,
            )
            file to descriptor
        } ?: return null

        targetFile = prepared.first
        pfd = prepared.second

        val webView = loadedWebView
        val printWritten = writeWebViewToPdf(activity, webView, jobName, pfd)

        withContext(NonCancellable + Dispatchers.IO) {
            runCatching { pfd?.close() }
            pfd = null
            if (printWritten && targetFile.isFile && targetFile.length() > 0L) {
                success = true
            }
        }

        if (success) targetFile else null
    } catch (ce: CancellationException) {
        throw ce
    } catch (_: Throwable) {
        null
    } finally {
        loadedWebView?.let { webView ->
            withContext(NonCancellable + Dispatchers.Main) {
                runCatching { webView.destroy() }
            }
        }
        withContext(NonCancellable + Dispatchers.IO) {
            runCatching { pfd?.close() }
            if (!success) {
                runCatching { targetFile?.delete() }
            }
        }
    }
}

private suspend fun loadReportWebView(
    activity: Activity,
    htmlContent: String,
): WebView? = withContext(Dispatchers.Main) {
    if (activity.isFinishing || activity.isDestroyed) return@withContext null
    suspendCancellableCoroutine { cont ->
        val cancelLoad = PatientPrintManager.loadHtmlInActivityWebView(
            activity = activity,
            htmlContent = htmlContent,
            onPageLoaded = { webView ->
                if (cont.isActive) {
                    cont.resume(webView) {
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            runCatching { webView.destroy() }
                        } else {
                            Handler(Looper.getMainLooper()).post { runCatching { webView.destroy() } }
                        }
                    }
                } else {
                    webView.destroy()
                }
            },
            onFailed = {
                if (cont.isActive) {
                    cont.resume(null)
                }
            },
        )
        cont.invokeOnCancellation {
            cancelLoad()
        }
    }
}

private suspend fun writeWebViewToPdf(
    activity: Activity,
    webView: WebView,
    jobName: String,
    output: ParcelFileDescriptor,
): Boolean = withContext(Dispatchers.Main) {
    if (activity.isFinishing || activity.isDestroyed) return@withContext false
    withTimeoutOrNull(PDF_WRITE_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            val cancellationSignal = CancellationSignal()
            cont.invokeOnCancellation { cancellationSignal.cancel() }
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("res1", "default", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            PdfPrint(printAttributes).print(
                printAdapter = printAdapter,
                output = output,
                cancellationSignal = cancellationSignal,
                onSuccess = {
                    if (cont.isActive) cont.resume(true)
                },
                onError = {
                    if (cont.isActive) cont.resume(false)
                },
            )
        }
    } ?: false
}
