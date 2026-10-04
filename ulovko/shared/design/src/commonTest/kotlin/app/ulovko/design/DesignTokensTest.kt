package app.ulovko.design

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesignTokensTest {

    /** Relativní jas podle WCAG 2.2 (sRGB). */
    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private fun contrast(a: Long, b: Long): Double {
        val (light, dark) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    @Test
    fun textColorsMeetWcagContrastInBothThemes() {
        val failures = mutableListOf<String>()
        for ((theme, scheme) in listOf("světlý" to DesignTokens.light, "tmavý" to DesignTokens.dark)) {
            for (req in DesignTokens.contrastRequirements) {
                val ratio = contrast(scheme.byName(req.foreground), scheme.byName(req.background))
                if (ratio < req.minRatio) {
                    failures += "$theme: ${req.foreground} na ${req.background} = ${(ratio * 100).toInt() / 100.0}, potřeba ${req.minRatio}"
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun everyHapticHasAWebFallback() {
        assertEquals(6, HapticToken.entries.size)
        assertTrue(HapticToken.entries.all { it.webPattern.isNotEmpty() })
    }
}
