package app.simplifymy.browser.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.simplifymy.browser.R

private fun variable(resId: Int) = FontFamily(
    listOf(400, 500, 600, 700).map { weight ->
        Font(resId, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
    },
)

enum class InterfaceFamily(val label: String, @StringRes val labelRes: Int? = null) {
    SpaceGrotesk("Space Grotesk"),
    Outfit("Outfit"),
    PlexSans("IBM Plex Sans"),
    System("System", R.string.follow_system),
    ;

    val family: FontFamily by lazy {
        when (this) {
            SpaceGrotesk -> variable(R.font.space_grotesk)
            Outfit -> variable(R.font.outfit)
            PlexSans -> variable(R.font.ibm_plex_sans)
            System -> FontFamily.SansSerif
        }
    }
}

enum class TextScale(val label: String, val factor: Float) {
    Small("S", 0.90f),
    Default("M", 1.00f),
    Large("L", 1.12f),
    Huge("XL", 1.26f),
}

private val Mono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

@Immutable
data class BrowserType(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val value: TextStyle,
    val meta: TextStyle,
    val section: TextStyle,
)

private fun sans(family: FontFamily, size: Float, weight: Int, tracking: Float = 0f) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking.em,
)

private fun mono(size: Float, weight: Int, tracking: Float = 0f) = TextStyle(
    fontFamily = Mono,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking.em,
    fontFeatureSettings = "tnum",
)

fun browserType(family: InterfaceFamily, scale: TextScale): BrowserType {
    val ui = family.family
    val s = scale.factor
    return BrowserType(
        display = sans(ui, 27 * s, 600, -0.03f),
        headline = sans(ui, 22 * s, 600, -0.02f),
        title = sans(ui, 19 * s, 600, -0.02f),
        body = sans(ui, 14.5f * s, 400),
        label = sans(ui, 14 * s, 500),
        value = mono(13 * s, 400),
        meta = mono(11.5f * s, 400),
        section = mono(10.5f * s, 500, 0.09f),
    )
}
