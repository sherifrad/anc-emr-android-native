package com.sherifradwan.ancnative.ui.patienthub.model

import com.sherifradwan.ancnative.model.DemoUltrasoundScan

data class BiometryValues(
    val bpd: String?,
    val hc: String?,
    val ac: String?,
    val fl: String?,
    val efw: String?,
    val fhr: String?,
    val amnioticFluid: String?,
    val placenta: String?,
    val presentation: String? = null,
) {
    val isEmpty: Boolean
        get() = bpd.isNullOrBlank() &&
            hc.isNullOrBlank() &&
            ac.isNullOrBlank() &&
            fl.isNullOrBlank() &&
            efw.isNullOrBlank()

    val hasAnyData: Boolean
        get() = !isEmpty ||
            !fhr.isNullOrBlank() ||
            !amnioticFluid.isNullOrBlank() ||
            !placenta.isNullOrBlank() ||
            !presentation.isNullOrBlank()
}

fun extractBiometry(scan: DemoUltrasoundScan): BiometryValues {
    return BiometryValues(
        bpd = scan.biometryBpd?.trim()?.takeIf { it.isNotEmpty() },
        hc = scan.biometryHc?.trim()?.takeIf { it.isNotEmpty() },
        ac = scan.biometryAc?.trim()?.takeIf { it.isNotEmpty() },
        fl = scan.biometryFl?.trim()?.takeIf { it.isNotEmpty() },
        efw = scan.estimatedFetalWeight?.trim()?.takeIf { it.isNotEmpty() },
        fhr = scan.fetalHeartRate?.trim()?.takeIf { it.isNotEmpty() },
        amnioticFluid = scan.amnioticFluid?.trim()?.takeIf { it.isNotEmpty() },
        placenta = scan.placentalSite?.trim()?.takeIf { it.isNotEmpty() },
        presentation = scan.presentation?.trim()?.takeIf { it.isNotEmpty() },
    )
}
