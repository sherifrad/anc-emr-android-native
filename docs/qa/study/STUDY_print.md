1 Problem as reported
On a phone, tapping Print in the report dialog shows "Printing is not available right now. Close this window and try again." even though Share PDF uses the same HTML and shared WebView loader and produces a valid PDF.

2 Root cause or current behaviour
- onUnavailable is invoked in PatientPrintManager.kt:35-37 when findActivity() returns null.
- onUnavailable is invoked in PatientPrintManager.kt:41-43 when PrintManager is null.
- onUnavailable is invoked in PatientPrintManager.kt:76 when loadHtmlInActivityWebView calls onFailed.
- onUnavailable is invoked in PatientPrintManager.kt:71-74 when runCatching around createPrintDocumentAdapter or printManager.print fails.
- loadHtmlInActivityWebView can reach onFailed through timeout at lines 107-111 or main-frame onReceivedError at lines 129-132; its cancellation path does not call onFailed.
- Share PDF uses the same loader and succeeds, so timeout or onReceivedError is less likely unless the print path is slower or uses a different context; this is not visible in the files given.
- activeWebViews.remove(webView) happens at PatientPrintManager.kt:119 during onPageFinished, before printManager.print at line 67, so the WebView is not retained for later system print adapter callbacks.
- printManager.print returns immediately and onComplete is called at line 70 before any PrintJob completion is known; no PrintJob state is tracked.
- No failure path logs the exception, so the exact cause cannot be distinguished from the reported toast.

3 Proposed fix
1. Add a TAG and Log.w in every onUnavailable path, including the throwable from runCatching; log no patient data.
2. Retain the loaded WebView in a dedicated set until printManager.print returns and the print job is no longer active; only then remove and destroy it.
3. Keep createPrintDocumentAdapter and printManager.print inside runCatching, but do not destroy the WebView immediately when print() returns success.
4. If retention alone does not fix it, reuse exportReportPdf to create the PDF and print from a file-backed PrintDocumentAdapter instead of relying on the live WebView.
5. Leave the Share PDF flow unchanged.

4 Tests to add
- Unit tests for printJobName edge cases, including empty input and maximum length.
- Android or Robolectric tests for null activity and null PrintManager asserting onUnavailable is called once.
- Instrumented test with a fake or recording PrintManager verifying the print adapter is created and print is called.
- Instrumented test verifying the WebView is not destroyed before printManager.print returns.

5 Risks, protected files, questions
- No protected files from the listed set appear touched; PatientPrintManager.kt and ReportPdfExporter.kt are UI/report helpers, not listed protected paths.
- Risk: retaining WebViews can leak memory if print jobs are cancelled or abandoned; use a bounded cleanup timeout.
- Risk: system print callbacks may arrive after the dialog is dismissed; ensure the Activity is still valid.
- Question for owner: does the failure reproduce with a minimal report, and can logcat be captured for the new tag?
- Question for owner: is a printer/print service installed and enabled on the affected phone?

6 Task entries for the roadmap
- Add print failure logging
  Add Log.w with the throwable at each onUnavailable path in PatientPrintManager; redact patient data.
- Retain WebView during system print
  Keep the loaded WebView alive until printManager.print returns and job activity ends; avoid early destruction.

