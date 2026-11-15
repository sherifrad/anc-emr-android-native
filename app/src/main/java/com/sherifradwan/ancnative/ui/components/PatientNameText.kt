package com.sherifradwan.ancnative.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow

/**
 * True when the first strong directional character of [text] is right-to-left
 * (Hebrew/Arabic letters, or an explicit RLE/RLO/RLI mark). Digits, punctuation and
 * spaces are neutral and skipped. Empty or neutral-only text is not RTL.
 */
internal fun isRtlText(text: String): Boolean {
    var i = 0
    while (i < text.length) {
        val cp = text.codePointAt(i)
        when (Character.getDirectionality(cp)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE,
            -> return true
            Character.DIRECTIONALITY_LEFT_TO_RIGHT,
            Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING,
            Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE,
            -> return false
        }
        i += Character.charCount(cp)
    }
    return false
}

/**
 * The one place a patient name is drawn. A name whose first strong character is RTL
 * (Arabic) is right-aligned with an RTL paragraph direction; any other name stays
 * left-aligned LTR. Absolute Left/Right are used on purpose so the result does not
 * depend on the app layout direction.
 */
@Composable
fun PatientNameText(
    name: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    val rtl = isRtlText(name)
    Text(
        text = name,
        style = style.copy(textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = if (rtl) TextAlign.Right else TextAlign.Left,
        modifier = modifier.fillMaxWidth(),
    )
}
