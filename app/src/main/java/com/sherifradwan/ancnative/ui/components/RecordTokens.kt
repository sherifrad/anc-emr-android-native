package com.sherifradwan.ancnative.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherifradwan.ancnative.ui.FieldBackgroundBg
import com.sherifradwan.ancnative.ui.FieldBackgroundDarkBg
import com.sherifradwan.ancnative.ui.FieldBackgroundDarkLabel
import com.sherifradwan.ancnative.ui.FieldBackgroundLabel
import com.sherifradwan.ancnative.ui.FieldDeliveryBg
import com.sherifradwan.ancnative.ui.FieldDeliveryDarkBg
import com.sherifradwan.ancnative.ui.FieldDeliveryDarkLabel
import com.sherifradwan.ancnative.ui.FieldDeliveryLabel
import com.sherifradwan.ancnative.ui.FieldObstetricBg
import com.sherifradwan.ancnative.ui.FieldObstetricDarkBg
import com.sherifradwan.ancnative.ui.FieldObstetricDarkLabel
import com.sherifradwan.ancnative.ui.FieldObstetricLabel
import com.sherifradwan.ancnative.ui.FieldPatientBg
import com.sherifradwan.ancnative.ui.FieldPatientDarkBg
import com.sherifradwan.ancnative.ui.FieldPatientDarkLabel
import com.sherifradwan.ancnative.ui.FieldPatientLabel
import com.sherifradwan.ancnative.ui.isDarkTheme

@Immutable
data class FieldFamily(val container: Color, val label: Color)

object FieldFamilies {
    @Composable fun patient() = if (isDarkTheme()) FieldFamily(FieldPatientDarkBg, FieldPatientDarkLabel) else FieldFamily(FieldPatientBg, FieldPatientLabel)
    @Composable fun obstetric() = if (isDarkTheme()) FieldFamily(FieldObstetricDarkBg, FieldObstetricDarkLabel) else FieldFamily(FieldObstetricBg, FieldObstetricLabel)
    @Composable fun background() = if (isDarkTheme()) FieldFamily(FieldBackgroundDarkBg, FieldBackgroundDarkLabel) else FieldFamily(FieldBackgroundBg, FieldBackgroundLabel)
    @Composable fun delivery() = if (isDarkTheme()) FieldFamily(FieldDeliveryDarkBg, FieldDeliveryDarkLabel) else FieldFamily(FieldDeliveryBg, FieldDeliveryLabel)
}

/** Family for fields inside a RecordSectionCard. Default: patient. */
val LocalFieldFamily = compositionLocalOf<FieldFamily?> { null }

@Composable
fun currentFieldFamily(): FieldFamily = LocalFieldFamily.current ?: FieldFamilies.patient()

object RecordShapes {
    val Small = RoundedCornerShape(10.dp)
    val Large = RoundedCornerShape(16.dp)
    val Sheet = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
}

object RecordSpacing {
    val XS = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
}

object RecordType {
    val HeaderName = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
    val SectionTitle = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
    val FieldLabel = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val FieldValue = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val KeyValue = FieldValue.copy(fontWeight = FontWeight.SemiBold)
    val Missing = FieldValue.copy(fontStyle = FontStyle.Italic)
    val Secondary = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val Action = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val Tag = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val CardName = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
    val MetricLabel = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
    val MetricValue = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
}

/** Keeps codes and numbers (A+, 110/70, G4 P1 A2, dates) in left-to-right order inside RTL text. */
fun ltrIsolate(text: String): String = "\u2066$text\u2069"
