package com.sherifradwan.ancnative.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ClinicalNavy = Color(0xFF0D1B3D)
val ClinicalIvory = Color(0xFFFBF8F5)
val ClinicalRose = Color(0xFFD49AA0)
val ActionRose = Color(0xFF934F5B)
val ClinicalTeal = Color(0xFF176B70)

private val LightColors = lightColorScheme(
    primary = ClinicalNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6DFFF),
    onPrimaryContainer = Color(0xFF041334),
    secondary = ActionRose,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DE),
    onSecondaryContainer = Color(0xFF3B0815),
    tertiary = ClinicalTeal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFAFECEE),
    onTertiaryContainer = Color(0xFF002021),
    background = ClinicalIvory,
    onBackground = ClinicalNavy,
    surface = Color.White,
    onSurface = ClinicalNavy,
    surfaceVariant = Color(0xFFF3E9E7),
    onSurfaceVariant = Color(0xFF4C3F43),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9F5F3),
    surfaceContainer = Color(0xFFF4EFEA),
    surfaceContainerHigh = Color(0xFFEFE9E4),
    surfaceContainerHighest = Color(0xFFE9E2DD),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF002A78),
    primaryContainer = Color(0xFF183F91),
    onPrimaryContainer = Color(0xFFDCE1FF),
    secondary = Color(0xFFFFB2BC),
    onSecondary = Color(0xFF5A1826),
    secondaryContainer = Color(0xFF762E3B),
    onSecondaryContainer = Color(0xFFFFD9DE),
    tertiary = Color(0xFF70D7D9),
    onTertiary = Color(0xFF003738),
    tertiaryContainer = Color(0xFF004F50),
    onTertiaryContainer = Color(0xFF8CF3F5),
    background = Color(0xFF10141D),
    onBackground = Color(0xFFE2E2EA),
    surface = Color(0xFF10141D),
    onSurface = Color(0xFFE2E2EA),
    surfaceVariant = Color(0xFF453A40),
    onSurfaceVariant = Color(0xFFCDC3C6),
    surfaceContainerLowest = Color(0xFF0B0E17),
    surfaceContainerLow = Color(0xFF181C26),
    surfaceContainer = Color(0xFF1C202B),
    surfaceContainerHigh = Color(0xFF262B36),
    surfaceContainerHighest = Color(0xFF313642),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

val ClinicalTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun AncTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ClinicalTypography,
        content = content,
    )
}
