package com.sherifradwan.ancnative.ui

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Spec-derived checks (slice_00_foundation.md sections 1, 7, 8) for the record-redesign
 * field-family and status-tag colour tokens in Theme.kt.
 *
 * The test reads token names/values straight from Theme.kt (same approach as ThemeContrastTest),
 * so it stays valid if token hex values are adjusted; it fails loudly if Theme.kt or required
 * tokens cannot be located, so it can never silently pass on stale data.
 */
class RecordTokensContrastTest {

    private fun locateUiDir(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(8) {
            val current = dir ?: return@repeat
            for (candidate in listOf(
                File(current, "app/src/main/java/com/sherifradwan/ancnative/ui"),
                File(current, "src/main/java/com/sherifradwan/ancnative/ui"),
            )) {
                if (candidate.isDirectory) return candidate
            }
            dir = current.parentFile
        }
        fail("Could not locate app/src/main/java/com/sherifradwan/ancnative/ui (cwd=${System.getProperty("user.dir")})")
        throw IllegalStateException("unreachable")
    }

    private val themeFile: File by lazy { File(locateUiDir(), "Theme.kt") }

    /** name -> 0xAARRGGBB int, from `val Name = Color(0x...)` lines in Theme.kt. */
    private fun parseColorTokens(): Map<String, Int> {
        val text = themeFile.readText()
        val regex = Regex("val\\s+(\\w+)\\s*=\\s*Color\\((0x[0-9A-Fa-f]{1,8})\\)")
        val tokens = mutableMapOf<String, Int>()
        for (m in regex.findAll(text)) {
            // 8-digit sRGB hex (e.g. 0xFFEFEBE9) overflows Int when parsed signed; parse as
            // Long then wrap to Int so the value matches the 0xAARRGGBB Int exactly.
            tokens[m.groupValues[1]] = m.groupValues[2].removePrefix("0x").toLong(16).toInt()
        }
        assertTrue(
            "Theme.kt colour tokens not found - test harness problem, not a product problem",
            tokens.containsKey("FieldPatientBg"),
        )
        return tokens
    }

    // WCAG 2.x relative luminance / contrast ratio from sRGB 8-bit components.
    private fun srgbChannelToLinear(channel: Double): Double =
        if (channel <= 0.03928) channel / 12.92 else Math.pow((channel + 0.055) / 1.055, 2.4)

    private fun relativeLuminance(argb: Int): Double {
        val r = srgbChannelToLinear(((argb shr 16) and 0xFF) / 255.0)
        val g = srgbChannelToLinear(((argb shr 8) and 0xFF) / 255.0)
        val b = srgbChannelToLinear((argb and 0xFF) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun argb(hex: String): Int = hex.removePrefix("#").toLong(16).toInt()

    private fun token(tokens: Map<String, Int>, name: String): Int =
        tokens[name] ?: throw AssertionError("missing Theme.kt token $name")

    private fun assertContrastAtLeast(
        text: Int,
        bg: Int,
        minRatio: Double,
        label: String,
        failures: MutableList<String>,
    ) {
        val ratio = contrastRatio(text, bg)
        if (ratio < minRatio) {
            failures += "$label = %.2f:1 (needs >= %.1f:1)".format(ratio, minRatio)
        }
    }

    private data class FieldPair(val label: String, val bg: String)

    private val lightFieldPairs = listOf(
        FieldPair("FieldPatientLabel", "FieldPatientBg"),
        FieldPair("FieldObstetricLabel", "FieldObstetricBg"),
        FieldPair("FieldBackgroundLabel", "FieldBackgroundBg"),
        FieldPair("FieldDeliveryLabel", "FieldDeliveryBg"),
    )

    private val darkFieldPairs = listOf(
        FieldPair("FieldPatientDarkLabel", "FieldPatientDarkBg"),
        FieldPair("FieldObstetricDarkLabel", "FieldObstetricDarkBg"),
        FieldPair("FieldBackgroundDarkLabel", "FieldBackgroundDarkBg"),
        FieldPair("FieldDeliveryDarkLabel", "FieldDeliveryDarkBg"),
    )

    @Test
    fun fieldLabels_onTheirBackgrounds_andSurfaces_meetWcagAa_4_5() {
        val tokens = parseColorTokens()
        val white = argb("#FFFFFFFF")
        val darkSurface = argb("#FF1A202E")
        val lightOnSurface = argb("#FF0D1B3D")
        val darkOnSurface = argb("#FFE2E2EA")
        val lightOnSurfaceVariant = argb("#FF4C3F43")
        val darkOnSurfaceVariant = argb("#FFCDC3C6")
        val failures = mutableListOf<String>()

        for ((labelName, bgName) in lightFieldPairs) {
            val label = token(tokens, labelName)
            val bg = token(tokens, bgName)
            assertContrastAtLeast(label, bg, 4.5, "$labelName on $bgName", failures)
            assertContrastAtLeast(label, white, 4.5, "$labelName on #FFFFFF", failures)
            assertContrastAtLeast(lightOnSurface, bg, 4.5, "#0D1B3D (light onSurface) on $bgName", failures)
            assertContrastAtLeast(lightOnSurfaceVariant, bg, 4.5, "#4C3F43 (light onSurfaceVariant) on $bgName", failures)
        }

        for ((labelName, bgName) in darkFieldPairs) {
            val label = token(tokens, labelName)
            val bg = token(tokens, bgName)
            assertContrastAtLeast(label, bg, 4.5, "$labelName on $bgName", failures)
            assertContrastAtLeast(label, darkSurface, 4.5, "$labelName on #1A202E", failures)
            assertContrastAtLeast(darkOnSurface, bg, 4.5, "#E2E2EA (dark onSurface) on $bgName", failures)
            assertContrastAtLeast(darkOnSurfaceVariant, bg, 4.5, "#CDC3C6 (dark onSurfaceVariant) on $bgName", failures)
        }

        assertTrue("Record field token pairs below WCAG AA 4.5:1: $failures", failures.isEmpty())
    }

    private data class StatusPair(val text: String, val bg: String)

    private val statusPairs = listOf(
        StatusPair("StatusIncompleteText", "StatusIncompleteBg"),
        StatusPair("StatusModerateText", "StatusModerateBg"),
        StatusPair("StatusHighText", "StatusHighBg"),
        StatusPair("StatusIncompleteDarkText", "StatusIncompleteDarkBg"),
        StatusPair("StatusModerateDarkText", "StatusModerateDarkBg"),
        StatusPair("StatusHighDarkText", "StatusHighDarkBg"),
    )

    @Test
    fun statusText_onBackground_meetsWcagAa_4_5() {
        val tokens = parseColorTokens()
        val failures = mutableListOf<String>()

        for ((textName, bgName) in statusPairs) {
            val text = token(tokens, textName)
            val bg = token(tokens, bgName)
            assertContrastAtLeast(text, bg, 4.5, "$textName on $bgName", failures)
        }

        assertTrue("Record status text/background pairs below WCAG AA 4.5:1: $failures", failures.isEmpty())
    }

    private val lightStatusBorders = listOf(
        "StatusIncompleteBorder",
        "StatusModerateBorder",
        "StatusHighBorder",
    )

    @Test
    fun lightStatusBorders_onWhite_meetNonTextContrast_3_0() {
        val tokens = parseColorTokens()
        val white = argb("#FFFFFFFF")
        val failures = mutableListOf<String>()

        for (borderName in lightStatusBorders) {
            val border = token(tokens, borderName)
            assertContrastAtLeast(border, white, 3.0, "$borderName on #FFFFFF", failures)
        }

        assertTrue("Light status border pairs below 3.0:1 on white: $failures", failures.isEmpty())
    }
}
