package com.sherifradwan.ancnative.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val ClinicalNavy = Color(0xFF0D1B3D)
val ClinicalIvory = Color(0xFFFBF8F5)
val ClinicalRose = Color(0xFFD49AA0)
val ActionRose = Color(0xFF934F5B)
val ClinicalTeal = Color(0xFF176B70)

// Light Mode Clinical Badge Tokens
val TagObstetricBg = Color(0xFFF3E8FF)
val TagObstetricBorder = Color(0xFFDDD6FE)
val TagObstetricText = Color(0xFF6B21A8)

val TagGestationalAgeBg = Color(0xFFE0F2FE)
val TagGestationalAgeBorder = Color(0xFFBAE6FD)
val TagGestationalAgeText = Color(0xFF0369A1)

val TagBloodPositiveBg = Color(0xFFEFF6FF)
val TagBloodPositiveBorder = Color(0xFFBFDBFE)
val TagBloodPositiveText = Color(0xFF1D4ED8)

val TagBloodNegativeBg = Color(0xFFFFE4E6)
val TagBloodNegativeBorder = Color(0xFFFECDD3)
val TagBloodNegativeText = Color(0xFFBE123C)

val TagBloodNeutralBg = Color(0xFFF1F5F9)
val TagBloodNeutralBorder = Color(0xFFCBD5E1)
val TagBloodNeutralText = Color(0xFF334155)

val TagAttentionBg = Color(0xFFFEF3C7)
val TagAttentionBorder = Color(0xFFFDE68A)
val TagAttentionIcon = Color(0xFFB45309)
val TagAttentionText = Color(0xFF92400E)

val TagHighRiskBg = Color(0xFFFEE2E2)
val TagHighRiskBorder = Color(0xFFFCA5A5)
val TagHighRiskText = Color(0xFFDC2626)

val TagNormalRiskBg = Color(0xFFDCFCE7)
val TagNormalRiskBorder = Color(0xFF86EFAC)
val TagNormalRiskText = Color(0xFF166534)

val TagAgeBg = Color(0xFFF1F5F9)
val TagAgeBorder = Color(0xFFCBD5E1)
val TagAgeText = Color(0xFF334155)

// Dark Mode Clinical Badge Tokens (Deep rich containers, luminous text)
val TagObstetricDarkBg = Color(0xFF2E1065)
val TagObstetricDarkBorder = Color(0xFF581C87)
val TagObstetricDarkText = Color(0xFFE9D5FF)

val TagGestationalAgeDarkBg = Color(0xFF082F49)
val TagGestationalAgeDarkBorder = Color(0xFF075985)
val TagGestationalAgeDarkText = Color(0xFFBAE6FD)

val TagBloodPositiveDarkBg = Color(0xFF172554)
val TagBloodPositiveDarkBorder = Color(0xFF1E40AF)
val TagBloodPositiveDarkText = Color(0xFFBFDBFE)

val TagBloodNegativeDarkBg = Color(0xFF4C0519)
val TagBloodNegativeDarkBorder = Color(0xFF9F1239)
val TagBloodNegativeDarkText = Color(0xFFFECDD3)

val TagBloodNeutralDarkBg = Color(0xFF1E293B)
val TagBloodNeutralDarkBorder = Color(0xFF334155)
val TagBloodNeutralDarkText = Color(0xFFCBD5E1)

val TagAttentionDarkBg = Color(0xFF451A03)
val TagAttentionDarkBorder = Color(0xFF78350F)
val TagAttentionDarkIcon = Color(0xFFF59E0B)
val TagAttentionDarkText = Color(0xFFFDE68A)

val TagHighRiskDarkBg = Color(0xFF450A0A)
val TagHighRiskDarkBorder = Color(0xFF991B1B)
val TagHighRiskDarkText = Color(0xFFFCA5A5)

val TagNormalRiskDarkBg = Color(0xFF052E16)
val TagNormalRiskDarkBorder = Color(0xFF166534)
val TagNormalRiskDarkText = Color(0xFF86EFAC)

val TagAgeDarkBg = Color(0xFF1E293B)
val TagAgeDarkBorder = Color(0xFF334155)
val TagAgeDarkText = Color(0xFFE2E8F0)

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
    background = Color(0xFF0E121B),
    onBackground = Color(0xFFE2E2EA),
    surface = Color(0xFF1A202E),
    onSurface = Color(0xFFE2E2EA),
    surfaceVariant = Color(0xFF262E40),
    onSurfaceVariant = Color(0xFFCDC3C6),
    surfaceContainerLowest = Color(0xFF090C13),
    surfaceContainerLow = Color(0xFF141924),
    surfaceContainer = Color(0xFF1E2638),
    surfaceContainerHigh = Color(0xFF283248),
    surfaceContainerHighest = Color(0xFF323D56),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C141D),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8E95A5),
    outlineVariant = Color(0xFF3A4459),
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ClinicalTypography,
        content = content,
    )
}
