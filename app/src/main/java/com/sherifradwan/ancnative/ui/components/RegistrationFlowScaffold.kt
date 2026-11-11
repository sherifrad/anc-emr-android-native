package com.sherifradwan.ancnative.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.sherifradwan.ancnative.state.AncEvent
import com.sherifradwan.ancnative.state.AncState
import com.sherifradwan.ancnative.state.FlowEvent
import com.sherifradwan.ancnative.state.RegistrationStep
import com.sherifradwan.ancnative.state.activeFlowForRoute
import com.sherifradwan.ancnative.state.blocksEditing
import com.sherifradwan.ancnative.state.forwardIsFinish
import java.util.UUID

/** The one place the UI makes the ids a flow move needs. */
internal fun flowMoveEvent(move: FlowEvent): AncEvent.Flow =
    AncEvent.Flow(move, UUID.randomUUID().toString(), UUID.randomUUID().toString())

internal fun registrationNextEvent(): AncEvent.RegistrationNext =
    AncEvent.RegistrationNext(UUID.randomUUID().toString(), UUID.randomUUID().toString())

/**
 * Wraps an editor screen with the step header and the pinned footer while the registration flow shows that screen;
 * outside a flow it renders [content] alone. [onNext] defaults to the editor's own save through RegistrationNext.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistrationFlowScaffold(
    state: AncState,
    onEvent: (AncEvent) -> Unit,
    nextEnabled: Boolean = true,
    nextIsFinish: Boolean? = null,
    onNext: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val flow = state.activeFlowForRoute() ?: return content()
    val blocked = state.savePhase.blocksEditing()
    Column(Modifier.fillMaxSize()) {
        RegistrationStepHeader(flow, compact = WindowInsets.isImeVisible)
        Box(Modifier.weight(1f)) {
            key(flow.activeEditorSessionId, flow.current) { content() }
        }
        RegistrationStepFooter(
            flow = flow,
            backEnabled = !blocked && flow.current != RegistrationStep.IDENTITY,
            skipEnabled = !blocked,
            nextEnabled = !blocked && nextEnabled,
            nextIsFinish = nextIsFinish ?: forwardIsFinish(flow),
            onBack = { onEvent(flowMoveEvent(FlowEvent.Back)) },
            onSkip = { onEvent(flowMoveEvent(FlowEvent.Skip)) },
            onNext = onNext ?: { onEvent(registrationNextEvent()) },
        )
    }
}
