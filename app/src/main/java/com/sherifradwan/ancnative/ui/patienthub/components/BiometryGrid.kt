package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.ui.components.PlainField
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.patienthub.model.BiometryValues

private class BiometryCell(
    val label: String,
    val text: String,
    val isMeasurement: Boolean,
    val tag: String? = null,
)

@Composable
fun BiometryGrid(
    values: BiometryValues,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty && !values.hasAnyData) return

    val bpdLabel = stringResource(R.string.biometry_bpd)
    val hcLabel = stringResource(R.string.biometry_hc)
    val acLabel = stringResource(R.string.biometry_ac)
    val flLabel = stringResource(R.string.biometry_fl)
    val efwLabel = stringResource(R.string.biometry_efw)
    val fhrLabel = stringResource(R.string.biometry_fhr)
    val presentationLabel = stringResource(R.string.scan_presentation_label)
    val placentaLabel = stringResource(R.string.scan_placenta)
    val fluidLabel = stringResource(R.string.amniotic_fluid)
    val bpd = values.bpd?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.unit_mm, it) }
    val hc = values.hc?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.unit_mm, it) }
    val ac = values.ac?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.unit_mm, it) }
    val fl = values.fl?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.unit_mm, it) }
    val fhr = values.fhr?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.unit_bpm, it) }

    val measurements = listOfNotNull(
        bpd?.let { BiometryCell(bpdLabel, it, isMeasurement = true) },
        hc?.let { BiometryCell(hcLabel, it, isMeasurement = true) },
        ac?.let { BiometryCell(acLabel, it, isMeasurement = true) },
        fl?.let { BiometryCell(flLabel, it, isMeasurement = true) },
        values.efw?.takeIf { it.isNotBlank() }?.let { BiometryCell(efwLabel, it, isMeasurement = true) },
        fhr?.let { BiometryCell(fhrLabel, it, isMeasurement = true) },
    )
    val words = listOfNotNull(
        values.presentation?.takeIf { it.isNotBlank() }
            ?.let { BiometryCell(presentationLabel, it, isMeasurement = false, tag = "scanPresentation") },
        values.placenta?.takeIf { it.isNotBlank() }?.let { BiometryCell(placentaLabel, it, isMeasurement = false) },
        values.amnioticFluid?.takeIf { it.isNotBlank() }?.let { BiometryCell(fluidLabel, it, isMeasurement = false) },
    )

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        measurements.chunked(MEASUREMENT_COLUMNS).forEach { BiometryRow(it, MEASUREMENT_COLUMNS) }
        val wideWord = words.any { it.text.length > LONG_WORD }
        words.chunked(if (wideWord) 1 else WORD_COLUMNS).forEach { BiometryRow(it, if (wideWord) 1 else WORD_COLUMNS) }
    }
}

@Composable
private fun BiometryRow(cells: List<BiometryCell>, columns: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        cells.forEach { cell ->
            val fieldModifier = Modifier.weight(1f).let { if (cell.tag != null) it.testTag(cell.tag) else it }
            PlainField(
                label = cell.label,
                value = if (cell.isMeasurement) ltrIsolate(cell.text) else cell.text,
                modifier = fieldModifier,
                emphasis = cell.isMeasurement,
            )
        }
        repeat(columns - cells.size) { Spacer(Modifier.weight(1f)) }
    }
}

private const val MEASUREMENT_COLUMNS = 3
private const val WORD_COLUMNS = 2
private const val LONG_WORD = 10
