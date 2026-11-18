PatientPrintManager.kt:13,70–72 — `activeWebView` is shared mutable state on the singleton object; a second `printPatientReport` call before the first `onPageFinished` overwrites it, so the first WebView’s `onPageFinished` skips release (`activeWebView !== webView`) and that WebView leaks, while the wrong WebView may be released instead. Concrete failure: rapid double-tap of Print leaks the first WebView and desynchronizes release tracking.

PatientPrintManager.kt:46,70–72 — the WebView is released only inside `onPageFinished`; if loading never completes (no `onPageFinished`, no error client), the WebView is retained forever and neither `onComplete` nor `onUnavailable` is invoked. Concrete failure: a failed asset/load leaves the print call hanging with a permanently leaked WebView.

findActivity unwraps `ContextWrapper` chains and terminates cycles via the visited set (ActivityContext.kt:156–164); Activity is used for both `PrintManager` and `WebView`; double `onPageFinished` is guarded (line 54); job names sanitize Arabic, spaces, and slashes, cap at 60 chars, and fall back to `Patient_Report` (lines 82–105); the unavailable path calls `onUnavailable` (lines 23–26, 29–32, 75) and the dialog shows a Toast (PatientTwoPageReportDialog.kt:123–129). Tests are specific and non-tautological.
[Tokens] upload: 12990 download: 4742 cache_read: 0 cache_write: 0

