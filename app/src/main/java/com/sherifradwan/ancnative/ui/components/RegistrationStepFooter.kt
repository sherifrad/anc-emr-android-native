package com.sherifradwan.ancnative.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.RegistrationFlow
import com.sherifradwan.ancnative.state.showsSkip
import com.sherifradwan.ancnative.ui.minimumTouchTarget

/**
 * Pinned Back, Skip and Next/Finish. Each button has its own enabled flag so an invalid count disables only Next.
 * Skip is shown only on steps that can be skipped. A [FlowRow] lets Next wrap under the others at large font.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistrationStepFooter(
    flow: RegistrationFlow,
    backEnabled: Boolean,
    skipEnabled: Boolean,
    nextEnabled: Boolean,
    nextIsFinish: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = backEnabled,
                    modifier = Modifier.minimumTouchTarget().testTag("flowBack"),
                ) { Text(stringResource(R.string.back_step)) }
                Spacer(Modifier.weight(1f))
                if (showsSkip(flow)) {
                    TextButton(
                        onClick = onSkip,
                        enabled = skipEnabled,
                        modifier = Modifier.minimumTouchTarget().testTag("flowSkip"),
                    ) { Text(stringResource(R.string.flow_skip)) }
                }
                Button(
                    onClick = onNext,
                    enabled = nextEnabled,
                    modifier = Modifier.minimumTouchTarget().testTag("flowNext"),
                ) {
                    Text(stringResource(if (nextIsFinish) R.string.flow_finish else R.string.next_step))
                }
            }
        }
    }
}
