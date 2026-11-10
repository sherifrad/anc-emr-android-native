package com.sherifradwan.ancnative.ui

import com.sherifradwan.ancnative.model.canonicalDatingMethod

internal data class DatingFieldVisibility(
    val selectedMethod: String?,
    val showLmp: Boolean,
    val showUltrasoundEdd: Boolean,
    val showIvf: Boolean,
    val showAgreedEdd: Boolean = true,
)

internal val DatingEditorMethods = listOf("LMP", "Ultrasound", "IVF_TRANSFER", "Clinical")

/** A blank or unrecognised method has no selection and shows both LMP and ultrasound EDD, like Clinical. */
internal fun datingFieldVisibility(storedMethod: String?): DatingFieldVisibility {
    val method = canonicalDatingMethod(storedMethod)?.takeIf { it in DatingEditorMethods }
    return DatingFieldVisibility(
        selectedMethod = method,
        showLmp = method == null || method == "LMP" || method == "Clinical",
        showUltrasoundEdd = method == null || method == "Ultrasound" || method == "Clinical",
        showIvf = method == "IVF_TRANSFER",
    )
}
