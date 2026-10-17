package app.simplifymy.browser.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import app.simplifymy.browser.R

enum class AppIcon(@StringRes val labelRes: Int, val background: Color) {
    Blue(R.string.icon_blue, Color(0xFF0F62FE)),
    Carbon(R.string.icon_carbon, Color(0xFF161616)),
    Purple(R.string.icon_purple, Color(0xFF8A3FFC)),
    Magenta(R.string.icon_magenta, Color(0xFFD02670)),
    Teal(R.string.icon_teal, Color(0xFF009D9A)),
    Green(R.string.icon_green, Color(0xFF198038)),
    ;

    val enabledByDefault: Boolean get() = this == Blue
}
