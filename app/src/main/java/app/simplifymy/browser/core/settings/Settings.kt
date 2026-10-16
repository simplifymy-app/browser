package app.simplifymy.browser.core.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.simplifymy.browser.R
import app.simplifymy.browser.ui.theme.Accent
import app.simplifymy.browser.ui.theme.Contrast
import app.simplifymy.browser.ui.theme.CornerStyle
import app.simplifymy.browser.ui.theme.InterfaceFamily
import app.simplifymy.browser.ui.theme.MenuDensity
import app.simplifymy.browser.ui.theme.TextScale
import app.simplifymy.browser.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class SpeedDialStyle(@StringRes val labelRes: Int) {
    IconAndTitle(R.string.tiles_icon_title),
    Icon(R.string.tiles_icon),
    Snapshot(R.string.tiles_snapshot),
}

val SPEED_DIAL_COLUMNS = 2..6

enum class ToolbarPosition(@StringRes val labelRes: Int) {
    Top(R.string.toolbar_top),
    Bottom(R.string.toolbar_bottom),
}

data class BrowserSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val accent: Accent = Accent.Indigo,
    val contrast: Contrast = Contrast.Normal,
    val corners: CornerStyle = CornerStyle.Default,
    val font: InterfaceFamily = InterfaceFamily.SpaceGrotesk,
    val textScale: TextScale = TextScale.Default,
    val toolbarPosition: ToolbarPosition = ToolbarPosition.Bottom,
    val menuDensity: MenuDensity = MenuDensity.Medium,
    val mobileView: Boolean = true,
    val showTabStrip: Boolean = true,
    val hideToolbarOnScroll: Boolean = false,
    val speedDialColumns: Int = 3,
    val speedDialStyle: SpeedDialStyle = SpeedDialStyle.IconAndTitle,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

private object Keys {
    val themeMode = stringPreferencesKey("theme_mode")
    val accent = stringPreferencesKey("accent")
    val contrast = stringPreferencesKey("contrast")
    val corners = stringPreferencesKey("corners")
    val font = stringPreferencesKey("font")
    val textScale = stringPreferencesKey("text_scale")
    val toolbarPosition = stringPreferencesKey("toolbar_position")
    val menuDensity = stringPreferencesKey("menu_density")
    val mobileView = booleanPreferencesKey("mobile_view")
    val showTabStrip = booleanPreferencesKey("show_tab_strip")
    val hideToolbarOnScroll = booleanPreferencesKey("hide_toolbar_on_scroll")
    val speedDialColumns = intPreferencesKey("speed_dial_columns")
    val speedDialStyle = stringPreferencesKey("speed_dial_style")
}

private inline fun <reified T : Enum<T>> Preferences.enum(key: Preferences.Key<String>, fallback: T): T =
    enumValues<T>().find { it.name == this[key] } ?: fallback

class SettingsRepository(private val context: Context) {

    val settings: Flow<BrowserSettings> = context.dataStore.data.map { p ->
        val defaults = BrowserSettings()
        BrowserSettings(
            themeMode = p.enum(Keys.themeMode, defaults.themeMode),
            accent = p.enum(Keys.accent, defaults.accent),
            contrast = p.enum(Keys.contrast, defaults.contrast),
            corners = p.enum(Keys.corners, defaults.corners),
            font = p.enum(Keys.font, defaults.font),
            textScale = p.enum(Keys.textScale, defaults.textScale),
            toolbarPosition = p.enum(Keys.toolbarPosition, defaults.toolbarPosition),
            menuDensity = p.enum(Keys.menuDensity, defaults.menuDensity),
            mobileView = p[Keys.mobileView] ?: defaults.mobileView,
            showTabStrip = p[Keys.showTabStrip] ?: defaults.showTabStrip,
            hideToolbarOnScroll = p[Keys.hideToolbarOnScroll] ?: defaults.hideToolbarOnScroll,
            speedDialColumns = p[Keys.speedDialColumns] ?: defaults.speedDialColumns,
            speedDialStyle = p.enum(Keys.speedDialStyle, defaults.speedDialStyle),
        )
    }

    suspend fun setThemeMode(value: ThemeMode) = put(Keys.themeMode, value)
    suspend fun setAccent(value: Accent) = put(Keys.accent, value)
    suspend fun setContrast(value: Contrast) = put(Keys.contrast, value)
    suspend fun setCorners(value: CornerStyle) = put(Keys.corners, value)
    suspend fun setFont(value: InterfaceFamily) = put(Keys.font, value)
    suspend fun setTextScale(value: TextScale) = put(Keys.textScale, value)
    suspend fun setToolbarPosition(value: ToolbarPosition) = put(Keys.toolbarPosition, value)
    suspend fun setMenuDensity(value: MenuDensity) = put(Keys.menuDensity, value)
    suspend fun setSpeedDialStyle(value: SpeedDialStyle) = put(Keys.speedDialStyle, value)
    suspend fun setSpeedDialColumns(value: Int) = put(Keys.speedDialColumns, value.coerceIn(SPEED_DIAL_COLUMNS))
    suspend fun setMobileView(value: Boolean) = put(Keys.mobileView, value)
    suspend fun setShowTabStrip(value: Boolean) = put(Keys.showTabStrip, value)
    suspend fun setHideToolbarOnScroll(value: Boolean) = put(Keys.hideToolbarOnScroll, value)

    private suspend fun put(key: Preferences.Key<String>, value: Enum<*>) = put(key, value.name)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }
}
