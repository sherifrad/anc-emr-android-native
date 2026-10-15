package com.sherifradwan.ancnative.ui.report

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.security.SecuritySettingsState

object PatientPrintManager {

    fun printPatientReport(
        context: Context,
        patient: DemoPatient,
        settings: SecuritySettingsState? = null,
        onComplete: (() -> Unit)? = null,
    ) {
        val htmlContent = PatientPrintHtmlGenerator.generateHtml(
            context = context,
            patient = patient,
            settings = settings,
        )

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val jobName = "ملف_المريضة_${patient.displayName}_${patient.caseId}"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("res1", "default", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                printManager.print(jobName, printAdapter, printAttributes)
                onComplete?.invoke()
            }
        }

        webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
    }
}
