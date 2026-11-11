package com.sherifradwan.ancnative.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sherifradwan.ancnative.R
import com.sherifradwan.ancnative.state.RegistrationFlow
import com.sherifradwan.ancnative.state.RegistrationStep
import com.sherifradwan.ancnative.state.StepStatus
import com.sherifradwan.ancnative.state.registrationStepCount
import com.sherifradwan.ancnative.state.registrationStepNumber

@StringRes
fun RegistrationStep.titleRes(): Int = when (this) {
    RegistrationStep.IDENTITY -> R.string.registration_step_identity
    RegistrationStep.DATING -> R.string.dating
    RegistrationStep.OBSTETRIC -> R.string.obstetric_risk_section_title
    RegistrationStep.HISTORIES -> R.string.registration_step_histories
    RegistrationStep.PREGNANCIES -> R.string.previous_pregnancies
}

@StringRes
fun StepStatus.labelRes(): Int = when (this) {
    StepStatus.DONE -> R.string.done
    StepStatus.SKIPPED -> R.string.registration_status_skipped
    StepStatus.PENDING -> R.string.registration_status_pending
}

/** Shape that tells the three statuses apart without colour or words. */
fun StepStatus.glyph(): String = when (this) {
    StepStatus.DONE -> "✓"
    StepStatus.SKIPPED -> "−"
    StepStatus.PENDING -> "○"
}

/** "Step 3 of 5: Obstetric history and risk". */
@Composable
fun registrationHeaderText(flow: RegistrationFlow): String = stringResource(
    R.string.registration_header,
    stringResource(R.string.step_n_of_m, registrationStepNumber(flow.current), registrationStepCount),
    stringResource(flow.current.titleRes()),
)
