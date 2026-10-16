package app.simplifymy.browser.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import app.simplifymy.browser.R

@Immutable
data class BrowserColors(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val hairline: Color,
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    val isLight: Boolean,
)

enum class ThemeMode(@StringRes val labelRes: Int) {
    System(R.string.mode_auto),
    Light(R.string.mode_light),
    Dark(R.string.mode_dark),
    Black(R.string.mode_black),
}

enum class Contrast(@StringRes val labelRes: Int, val shift: Float) {
    Soft(R.string.contrast_soft, -0.12f),
    Normal(R.string.contrast_normal, 0f),
    High(R.string.contrast_high, 0.16f),
}

enum class Accent(val darkFill: Color, val lightFill: Color) {
    Indigo(Color(0xFF6E8BFF), Color(0xFF4A5FE0)),
    Amber(Color(0xFFE0A458), Color(0xFF8F5A12)),
    Jade(Color(0xFF5FBF8F), Color(0xFF12764E)),
    Rose(Color(0xFFD96A8B), Color(0xFFA62F55)),
    Violet(Color(0xFFA98BFF), Color(0xFF6541D6)),
}

private val DarkInk = Color(0xFF0B0B0C)
private val LightInk = Color(0xFFFFFFFF)

private val Dark = BrowserColors(
    background = Color(0xFF0B0B0C),
    surface = Color(0xFF141416),
    surfaceAlt = Color(0xFF1C1C1F),
    hairline = Color(0xFF2A2A2E),
    text = Color(0xFFEDEDEF),
    textMuted = Color(0xFF8A8A92),
    textFaint = Color(0xFF5A5A62),
    accent = Accent.Indigo.darkFill,
    onAccent = DarkInk,
    danger = Color(0xFFFF7A7A),
    isLight = false,
)

private val Black = Dark.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0C0C0D),
    surfaceAlt = Color(0xFF161618),
    hairline = Color(0xFF232326),
)

private val Light = BrowserColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF5F5F6),
    surfaceAlt = Color(0xFFEDEDEF),
    hairline = Color(0xFFE3E3E6),
    text = Color(0xFF131315),
    textMuted = Color(0xFF5E5E66),
    textFaint = Color(0xFF74747C),
    accent = Accent.Indigo.lightFill,
    onAccent = LightInk,
    danger = Color(0xFFC22B2B),
    isLight = true,
)

private fun Color.shifted(colors: BrowserColors, amount: Float): Color = when {
    amount == 0f -> this
    amount < 0f -> lerp(this, colors.background, -amount)
    else -> lerp(this, if (colors.isLight) Color.Black else Color.White, amount)
}

fun browserColors(dark: Boolean, black: Boolean, accent: Accent, contrast: Contrast): BrowserColors {
    val base = if (!dark) Light else if (black) Black else Dark
    val themed = base.copy(accent = if (base.isLight) accent.lightFill else accent.darkFill)
    return themed.copy(
        text = themed.text.shifted(themed, contrast.shift),
        textMuted = themed.textMuted.shifted(themed, contrast.shift),
        textFaint = themed.textFaint.shifted(themed, contrast.shift),
    )
}
