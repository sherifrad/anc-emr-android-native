package com.sherifradwan.ancnative.ui.report

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.security.SecuritySettingsState

object PatientPrintManager {

    private const val MAX_JOB_NAME_LENGTH = 60
    private const val FALLBACK_JOB_NAME = "Patient_Report"
    private const val LOAD_TIMEOUT_MS = 20_000L

    // Keeps WebViews alive until onPageFinished, main-frame error, or timeout releases them (main thread only).
    private val activeWebViews = mutableSetOf<WebView>()

    fun printPatientReport(
        context: Context,
        patient: DemoPatient,
        settings: SecuritySettingsState? = null,
        onComplete: (() -> Unit)? = null,
        onUnavailable: (() -> Unit)? = null,
        selectedPictureKeys: Set<String> = emptySet(),
        pictureDataUris: Map<String, String> = emptyMap(),
    ) {
        val activity = context.findActivity()
        if (activity == null) {
            onUnavailable?.invoke()
            return
        }

        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            onUnavailable?.invoke()
            return
        }

        val htmlContent = PatientPrintHtmlGenerator.generateHtml(
            context = activity,
            patient = patient,
            settings = settings,
            selectedPictureKeys = selectedPictureKeys,
            pictureDataUri = { pictureDataUris[it.key] },
        )

        loadHtmlInActivityWebView(
            activity = activity,
            htmlContent = htmlContent,
            onPageLoaded = { webView ->
                val jobName = printJobName(patient.displayName, patient.caseId)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("res1", "default", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                val printResult = runCatching {
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(jobName, printAdapter, printAttributes)
                }
                printResult
                    .onSuccess { onComplete?.invoke() }
                    .onFailure {
                        webView.destroy()
                        onUnavailable?.invoke()
                    }
            },
            onFailed = { onUnavailable?.invoke() },
        )
    }

    /**
     * Loads [htmlContent] in a hidden [WebView] hosted by [activity] with a 20-second timeout,
     * keeping the instance retained until page load succeeds or fails and releasing/destroying it
     * on failure, timeout, or cancellation.
     */
    internal fun loadHtmlInActivityWebView(
        activity: Activity,
        htmlContent: String,
        onPageLoaded: (WebView) -> Unit,
        onFailed: () -> Unit,
    ): () -> Unit {
        val webView = WebView(activity)
        activeWebViews.add(webView)
        var settled = false
        // Detached WebViews never attach to a window, so post the timeout on the main Looper directly.
        val mainHandler = Handler(Looper.getMainLooper())
        lateinit var timeoutRunnable: Runnable

        fun releaseAbandonedWebView(notifyFailure: Boolean) {
            mainHandler.removeCallbacks(timeoutRunnable)
            activeWebViews.remove(webView)
            webView.destroy()
            if (notifyFailure) {
                onFailed()
            }
        }

        timeoutRunnable = Runnable {
            if (settled) return@Runnable
            settled = true
            releaseAbandonedWebView(notifyFailure = true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (settled) return
                settled = true
                mainHandler.removeCallbacks(timeoutRunnable)
                activeWebViews.remove(webView)
                onPageLoaded(webView)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame != true || settled) return
                settled = true
                releaseAbandonedWebView(notifyFailure = true)
            }
        }

        mainHandler.postDelayed(timeoutRunnable, LOAD_TIMEOUT_MS)
        webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)

        return {
            val cancelOnMain = {
                if (!settled) {
                    settled = true
                    releaseAbandonedWebView(notifyFailure = false)
                }
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                cancelOnMain()
            } else {
                mainHandler.post(cancelOnMain)
            }
        }
    }

    internal fun printJobName(displayName: String, caseId: String): String {
        val raw = "$displayName $caseId"
        val sanitized = buildString(raw.length) {
            var previousWasUnderscore = false
            for (ch in raw) {
                val isAllowed = Character.isLetterOrDigit(ch) || ch == '-' || ch == '_'
                if (isAllowed) {
                    if (ch == '_') {
                        if (!previousWasUnderscore && isNotEmpty()) {
                            append('_')
                            previousWasUnderscore = true
                        }
                    } else {
                        append(ch)
                        previousWasUnderscore = false
                    }
                } else if (!previousWasUnderscore && isNotEmpty()) {
                    append('_')
                    previousWasUnderscore = true
                }
            }
        }.trimEnd('_').take(MAX_JOB_NAME_LENGTH).trimEnd('_')
        return sanitized.ifEmpty { FALLBACK_JOB_NAME }
    }
}
