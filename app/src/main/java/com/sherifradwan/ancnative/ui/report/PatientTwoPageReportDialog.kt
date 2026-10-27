package com.sherifradwan.ancnative.ui.report

import android.annotation.SuppressLint
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sherifradwan.ancnative.model.ClinicalSummaryGenerator
import com.sherifradwan.ancnative.model.DemoPatient
import com.sherifradwan.ancnative.security.SecuritySettingsState
import com.sherifradwan.ancnative.ui.ClinicalNavy
import com.sherifradwan.ancnative.ui.minimumTouchTarget

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PatientTwoPageReportDialog(
    patient: DemoPatient,
    securitySettings: SecuritySettingsState? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val htmlContent = remember(patient.id) {
        PatientPrintHtmlGenerator.generateHtml(
            context = context,
            patient = patient,
            settings = securitySettings,
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("patientTwoPageReportDialog"),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Surface(
                    color = ClinicalNavy,
                    contentColor = Color.White,
                    shadowElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.minimumTouchTarget().testTag("closeReportDialog"),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                            Column {
                                Text(
                                    text = "ملف المريضة المعتمد",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = patient.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Share text/summary button
                            IconButton(
                                onClick = {
                                    val summary = ClinicalSummaryGenerator.generateTextSummary(patient)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "الملف الطبي - ${patient.displayName}")
                                        putExtra(Intent.EXTRA_TEXT, summary)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الملف الطبي"))
                                },
                                modifier = Modifier.minimumTouchTarget().testTag("shareReportButton"),
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color.White)
                            }

                            // Print / Export PDF button
                            Button(
                                onClick = {
                                    PatientPrintManager.printPatientReport(
                                        context = context,
                                        patient = patient,
                                        settings = securitySettings,
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = Color.White,
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 8.dp,
                                ),
                                modifier = Modifier.minimumTouchTarget().testTag("printReportButton"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "طباعة",
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "طباعة PDF",
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Interactive WebView Paper Canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                this.settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    useWideViewPort = false
                                    loadWithOverviewMode = true
                                    defaultTextEncodingName = "UTF-8"
                                }
                                webViewClient = WebViewClient()
                                loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize().testTag("reportWebView"),
                    )
                }
            }
        }
    }
}
