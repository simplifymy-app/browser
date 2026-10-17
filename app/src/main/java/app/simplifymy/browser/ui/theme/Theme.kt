package app.simplifymy.browser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark, ThemeMode.Black -> true
}

private val LocalColors = staticCompositionLocalOf<BrowserColors> { error("No BrowserTheme") }
private val LocalType = staticCompositionLocalOf { browserType(InterfaceFamily.SpaceGrotesk, TextScale.Default) }
private val LocalCorners = staticCompositionLocalOf { CornerStyle.Default }
private val LocalReduceMotion = staticCompositionLocalOf { false }
private val LocalMenuDensity = staticCompositionLocalOf { MenuDensity.Medium }
private val LocalOneHanded = staticCompositionLocalOf { false }

object BrowserTheme {
    val colors: BrowserColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val type: BrowserType
        @Composable @ReadOnlyComposable get() = LocalType.current
    val corners: CornerStyle
        @Composable @ReadOnlyComposable get() = LocalCorners.current
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
    val menuDensity: MenuDensity
        @Composable @ReadOnlyComposable get() = LocalMenuDensity.current
    val oneHanded: Boolean
        @Composable @ReadOnlyComposable get() = LocalOneHanded.current
}

@Composable
fun BrowserTheme(
    mode: ThemeMode = ThemeMode.System,
    accent: Accent = Accent.Indigo,
    contrast: Contrast = Contrast.Normal,
    corners: CornerStyle = CornerStyle.Default,
    family: InterfaceFamily = InterfaceFamily.SpaceGrotesk,
    textScale: TextScale = TextScale.Default,
    reduceMotion: Boolean = false,
    menuDensity: MenuDensity = MenuDensity.Medium,
    oneHanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = mode.isDark()
    val colors = remember(dark, mode, accent, contrast) {
        browserColors(dark, mode == ThemeMode.Black, accent, contrast)
    }
    val type = remember(family, textScale) { browserType(family, textScale) }

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalType provides type,
        LocalCorners provides corners,
        LocalReduceMotion provides reduceMotion,
        LocalMenuDensity provides menuDensity,
        LocalOneHanded provides oneHanded,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(dark),
            typography = type.toMaterial(),
            content = content,
        )
    }
}

private fun BrowserColors.toMaterial(dark: Boolean) = (if (dark) darkColorScheme() else lightColorScheme()).copy(
    primary = accent,
    onPrimary = onAccent,
    background = background,
    onBackground = text,
    surface = surface,
    onSurface = text,
    surfaceVariant = surfaceAlt,
    onSurfaceVariant = textMuted,
    surfaceContainer = surfaceAlt,
    outline = hairline,
    outlineVariant = hairline,
    error = danger,
)

private fun BrowserType.toMaterial() = Typography(
    headlineSmall = headline,
    titleLarge = title,
    titleMedium = title,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = meta,
    labelLarge = label,
    labelMedium = label,
    labelSmall = section,
)
