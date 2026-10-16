package app.simplifymy.browser.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

object Motion {
    const val STATE = 120
    const val TOKEN = 160
    const val CHROME = 240
    const val SCREEN = 320

    val enter: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val exit: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
}

@Composable
@ReadOnlyComposable
fun <T> enterTween(durationMs: Int): TweenSpec<T> =
    tween(if (BrowserTheme.reduceMotion) 0 else durationMs, easing = Motion.enter)

@Composable
@ReadOnlyComposable
fun <T> exitTween(durationMs: Int): TweenSpec<T> =
    tween(if (BrowserTheme.reduceMotion) 0 else durationMs, easing = Motion.exit)
