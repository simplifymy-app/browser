package app.simplifymy.browser.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.R

object Space {
    val gutter = 4.dp
    val base = 8.dp
    val toolbarInset = 12.dp
    val rowGap = 16.dp
    val screen = 20.dp
    val section = 22.dp
    val hitTarget = 44.dp
    val settingsRow = 56.dp
}

enum class CornerStyle(val radius: Dp) {
    Sharp(2.dp),
    Default(4.dp),
    Soft(6.dp),
    Round(14.dp),
    ;

    val label: String get() = radius.value.toInt().toString()
}

enum class MenuDensity(@StringRes val labelRes: Int, val rowHeight: Dp) {
    Small(R.string.density_small, 36.dp),
    Medium(R.string.density_medium, 44.dp),
    Large(R.string.density_large, 52.dp),
}

object Radius {
    val control = RoundedCornerShape(4.dp)
    val card = RoundedCornerShape(6.dp)
    val sheet = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    val pill = RoundedCornerShape(percent = 50)
}

object Sizes {
    val hairline = 1.dp
    val icon = 20.dp
    val button = 34.dp
    val dropdown = 38.dp
}
