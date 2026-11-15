package com.sherifradwan.ancnative.ui.patienthub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.model.BpLevel
import com.sherifradwan.ancnative.model.FhrLevel
import com.sherifradwan.ancnative.model.SfhConcordance
import com.sherifradwan.ancnative.ui.components.PlainField
import com.sherifradwan.ancnative.ui.components.RecordType
import com.sherifradwan.ancnative.ui.components.StatusTone
import com.sherifradwan.ancnative.ui.components.StatusWord
import com.sherifradwan.ancnative.ui.components.ltrIsolate
import com.sherifradwan.ancnative.ui.formatWeightKg
import com.sherifradwan.ancnative.ui.patienthub.model.VitalsSnapshot

@Composable
fun BedsideVitalsGrid(
    vitals: VitalsSnapshot,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val hasFhr = !vitals.fhrText.isNullOrBlank()
    val hasLegacySfh = !vitals.sfhCm.isNullOrBlank()
    val rowGap = if (compact) 6.dp else 10.dp

    val bpWord: (@Composable () -> Unit)? = when (vitals.bpLevel) {
        BpLevel.SEVERE -> { { StatusWord(stringResource(R.string.vital_bp_severe), StatusTone.High) } }
        BpLevel.RAISED -> { { StatusWord(stringResource(R.string.vital_bp_raised), StatusTone.Moderate) } }
        BpLevel.NORMAL -> null
    }
    val fhrWord: (@Composable () -> Unit)? = when (vitals.fhrLevel) {
        FhrLevel.BRADYCARDIA -> { { StatusWord(stringResource(R.string.vital_fhr_brady), StatusTone.High) } }
        FhrLevel.TACHYCARDIA -> { { StatusWord(stringResource(R.string.vital_fhr_tachy), StatusTone.High) } }
        FhrLevel.NORMAL, null -> null
    }
    val sfhWord: (@Composable () -> Unit)? = when (vitals.sfhConcordance) {
        SfhConcordance.DISCREPANT_LARGE -> { { StatusWord(stringResource(R.string.vital_sfh_large), StatusTone.Moderate) } }
        SfhConcordance.DISCREPANT_SMALL -> { { StatusWord(stringResource(R.string.vital_sfh_small), StatusTone.High) } }
        SfhConcordance.CONCORDANT, null -> null
    }
    val weightDelta: (@Composable () -> Unit)? = vitals.weightDeltaText?.let { delta ->
        {
            Text(delta, style = RecordType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(rowGap)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlainField(
                label = stringResource(R.string.vital_blood_pressure),
                value = vitals.bpText?.takeIf { it.isNotBlank() }?.let { ltrIsolate(stringResource(R.string.unit_mmhg, it)) },
                modifier = Modifier.weight(1f),
                emphasis = true,
                extra = bpWord,
            )
            PlainField(
                label = stringResource(R.string.vital_weight),
                value = formatWeightKg(vitals.weightText)?.let(::ltrIsolate),
                modifier = Modifier.weight(1f),
                emphasis = true,
                extra = weightDelta,
            )
        }
        if (hasFhr || hasLegacySfh) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasFhr) {
                    PlainField(
                        label = stringResource(R.string.vital_fetal_heart_rate),
                        value = ltrIsolate(stringResource(R.string.unit_bpm, vitals.fhrText.orEmpty())),
                        modifier = Modifier.weight(1f),
                        emphasis = true,
                        extra = fhrWord,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (hasLegacySfh) {
                    PlainField(
                        label = stringResource(R.string.vital_fundal_height),
                        value = ltrIsolate(stringResource(R.string.unit_cm, vitals.sfhCm.orEmpty())),
                        modifier = Modifier.weight(1f),
                        emphasis = true,
                        extra = sfhWord,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
