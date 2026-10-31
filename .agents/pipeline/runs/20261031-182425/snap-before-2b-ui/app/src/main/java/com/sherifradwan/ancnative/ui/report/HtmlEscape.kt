package com.sherifradwan.ancnative.ui.report

internal fun escapeHtml(text: String?): String {
    if (text == null) return ""
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
