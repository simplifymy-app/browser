package app.simplifymy.browser.feature.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simplifymy.browser.R
import app.simplifymy.browser.core.history.HistoryPeriod
import app.simplifymy.browser.feature.library.HistoryPeriodSetting
import app.simplifymy.browser.core.settings.BrowserSettings
import app.simplifymy.browser.core.settings.Language
import app.simplifymy.browser.core.settings.SPEED_DIAL_COLUMNS
import app.simplifymy.browser.core.settings.SettingsRepository
import app.simplifymy.browser.core.settings.SpeedDialStyle
import app.simplifymy.browser.core.settings.ToolbarPosition
import app.simplifymy.browser.ui.component.AppIconSetting
import app.simplifymy.browser.ui.component.BrowserDialog
import app.simplifymy.browser.ui.component.DropdownSetting
import app.simplifymy.browser.ui.component.LinkSetting
import app.simplifymy.browser.ui.component.PickerSetting
import app.simplifymy.browser.ui.component.Screen
import app.simplifymy.browser.ui.component.ScreenHeader
import app.simplifymy.browser.ui.component.Snackbar
import app.simplifymy.browser.ui.component.SegmentedPicker
import app.simplifymy.browser.ui.component.SettingsSection
import app.simplifymy.browser.ui.component.StepperSetting
import app.simplifymy.browser.ui.component.SwatchSetting
import app.simplifymy.browser.ui.component.ToggleSetting
import app.simplifymy.browser.ui.component.ValueSetting
import app.simplifymy.browser.ui.theme.Accent
import app.simplifymy.browser.ui.theme.AppIcon
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Contrast
import app.simplifymy.browser.ui.theme.CornerStyle
import app.simplifymy.browser.ui.theme.InterfaceFamily
import app.simplifymy.browser.ui.theme.MenuDensity
import app.simplifymy.browser.ui.theme.Space
import app.simplifymy.browser.ui.theme.TextScale
import app.simplifymy.browser.ui.theme.ThemeMode
import org.mozilla.geckoview.BuildConfig as GeckoBuildConfig
import java.time.LocalDate

private const val BACKUP_MIME = "application/json"
private val BACKUP_OPEN_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")
private const val CONTACT_URL = "https://www.simplifymy.app/contact/"
private const val LICENSES_URL = "about:license"

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onOpenUrl: (String) -> Unit, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val engines by viewModel.searchEngines.collectAsStateWithLifecycle()
    val backup by viewModel.backup.collectAsStateWithLifecycle()
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        uri?.let(viewModel::export)
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::import)
    }
    BackHandler(onBack = onBack)
    SettingsContent(
        backupMessage = backup?.message(),
        onBackupShown = viewModel::backupShown,
        onExport = { exporter.launch("browser-backup-${LocalDate.now()}.json") },
        onImport = { importer.launch(BACKUP_OPEN_TYPES) },
        settings = settings,
        engines = engines,
        update = viewModel::update,
        onSelectEngine = { id -> engines.choices.find { it.id == id }?.let(viewModel::selectSearchEngine) },
        onSelectAppIcon = viewModel::setAppIcon,
        onClearData = viewModel::clearData,
        onOpenUrl = onOpenUrl,
        onBack = onBack,
    )
}

@Composable
fun SettingsContent(
    backupMessage: String?,
    onBackupShown: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    settings: BrowserSettings,
    engines: SearchEngines,
    update: (suspend SettingsRepository.() -> Unit) -> Unit,
    onSelectEngine: (String) -> Unit,
    onSelectAppIcon: (AppIcon) -> Unit,
    onClearData: (history: HistoryPeriod?, siteData: Boolean, cache: Boolean) -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var clearing by remember { mutableStateOf(false) }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }

    Box(Modifier.fillMaxSize()) {
        Screen(Modifier.fillMaxSize()) {
            ScreenHeader(stringResource(R.string.action_settings), onBack)
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = Space.screen),
                verticalArrangement = Arrangement.spacedBy(Space.section),
            ) {
                SettingsSection(stringResource(R.string.section_browser)) {
                    DropdownSetting(
                        label = stringResource(R.string.setting_search_engine),
                        options = engines.choices.map { it.id },
                        selected = engines.selectedId.orEmpty(),
                        onSelect = onSelectEngine,
                    ) { id -> engines.choices.find { it.id == id }?.name.orEmpty() }
                    ToggleSetting(
                        stringResource(R.string.mobile_view),
                        settings.mobileView,
                        { update { setMobileView(it) } },
                        hint = stringResource(R.string.setting_mobile_view_hint),
                    )
                    ToggleSetting(
                    stringResource(R.string.setting_tab_strip),
                    settings.showTabStrip,
                    { update { setShowTabStrip(it) } },
                    hint = stringResource(R.string.setting_tab_strip_hint),
                )
                ToggleSetting(
                    stringResource(R.string.setting_hide_toolbar),
                    settings.hideToolbarOnScroll,
                    { update { setHideToolbarOnScroll(it) } },
                    hint = stringResource(R.string.setting_hide_toolbar_hint),
                )
                ToggleSetting(
                    stringResource(R.string.setting_restore_tabs),
                    settings.restoreTabs,
                    { update { setRestoreTabs(it) } },
                    hint = stringResource(R.string.setting_restore_tabs_hint),
                )
                PickerSetting(stringResource(R.string.toolbar_position)) {
                        SegmentedPicker(ToolbarPosition.entries, settings.toolbarPosition, { update { setToolbarPosition(it) } }) {
                            stringResource(it.labelRes)
                        }
                    }
                    PickerSetting(stringResource(R.string.setting_menu_density)) {
                        SegmentedPicker(MenuDensity.entries, settings.menuDensity, { update { setMenuDensity(it) } }) {
                            stringResource(it.labelRes)
                        }
                    }
                }

                SettingsSection(stringResource(R.string.speed_dial)) {
                    StepperSetting(stringResource(R.string.setting_columns), settings.speedDialColumns, SPEED_DIAL_COLUMNS) {
                        update { setSpeedDialColumns(it) }
                    }
                    DropdownSetting(
                        label = stringResource(R.string.setting_tiles),
                        options = SpeedDialStyle.entries,
                        selected = settings.speedDialStyle,
                        onSelect = { update { setSpeedDialStyle(it) } },
                        hint = stringResource(R.string.setting_tiles_hint).takeIf { settings.speedDialStyle == SpeedDialStyle.Snapshot },
                    ) { stringResource(it.labelRes) }
                }

                SettingsSection(stringResource(R.string.section_theme)) {
                    DropdownSetting(
                        label = stringResource(R.string.setting_mode),
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        onSelect = { update { setThemeMode(it) } },
                    ) { stringResource(it.labelRes) }
                    SwatchSetting(
                        label = stringResource(R.string.setting_accent),
                        swatches = Accent.entries.map { if (BrowserTheme.colors.isLight) it.lightFill else it.darkFill },
                        selected = settings.accent.ordinal,
                        onSelect = { update { setAccent(Accent.entries[it]) } },
                    )
                    PickerSetting(stringResource(R.string.setting_contrast)) {
                        SegmentedPicker(Contrast.entries, settings.contrast, { update { setContrast(it) } }) { stringResource(it.labelRes) }
                    }
                    PickerSetting(stringResource(R.string.setting_corner_radius)) {
                        SegmentedPicker(CornerStyle.entries, settings.corners, { update { setCorners(it) } }) { it.label }
                    }
                }

                SettingsSection(stringResource(R.string.setting_app_icon)) {
                    AppIconSetting(settings.appIcon, stringResource(R.string.setting_app_icon_hint), onSelectAppIcon)
                }

                SettingsSection(stringResource(R.string.section_typography)) {
                    DropdownSetting(
                        label = stringResource(R.string.setting_font),
                        options = InterfaceFamily.entries,
                        selected = settings.font,
                        onSelect = { update { setFont(it) } },
                    ) { font -> font.labelRes?.let { stringResource(it) } ?: font.label }
                    DropdownSetting(
                        label = stringResource(R.string.setting_language),
                        options = Language.entries,
                        selected = Language.current(),
                        onSelect = Language::apply,
                    ) { if (it == Language.System) stringResource(R.string.follow_system) else it.label }
                    PickerSetting(stringResource(R.string.setting_text_size)) {
                        SegmentedPicker(TextScale.entries, settings.textScale, { update { setTextScale(it) } }) { it.label }
                    }
                }

                SettingsSection(stringResource(R.string.section_privacy)) {
                    ToggleSetting(
                        stringResource(R.string.setting_search_suggestions),
                        settings.searchSuggestions,
                        { update { setSearchSuggestions(it) } },
                        hint = stringResource(R.string.setting_search_suggestions_hint),
                    )
                    LinkSetting(stringResource(R.string.setting_clear_data), { clearing = true }, stringResource(R.string.setting_clear_data_hint))
                }

                SettingsSection(stringResource(R.string.section_backup)) {
                    LinkSetting(stringResource(R.string.backup_export), onExport, stringResource(R.string.backup_export_hint))
                    LinkSetting(stringResource(R.string.backup_import), onImport, stringResource(R.string.backup_import_hint))
                }

                SettingsSection(stringResource(R.string.section_about)) {
                    ValueSetting(stringResource(R.string.setting_version), version)
                    ValueSetting(stringResource(R.string.setting_engine), stringResource(R.string.powered_by_gecko, GeckoBuildConfig.MOZILLA_VERSION))
                    LinkSetting(stringResource(R.string.setting_licenses), { onOpenUrl(LICENSES_URL) })
                    LinkSetting(stringResource(R.string.setting_contact), { uriHandler.openUri(CONTACT_URL) }, stringResource(R.string.setting_contact_hint))
                }
            }
        }

        backupMessage?.let {
            Snackbar(it, onBackupShown, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
    }

    if (clearing) ClearDataDialog(onDismiss = { clearing = false }, onClear = { h, s, c -> onClearData(h, s, c); clearing = false })
}

@Composable
private fun ClearDataDialog(onDismiss: () -> Unit, onClear: (HistoryPeriod?, Boolean, Boolean) -> Unit) {
    var history by remember { mutableStateOf(true) }
    var period by remember { mutableStateOf(HistoryPeriod.All) }
    var siteData by remember { mutableStateOf(true) }
    var cache by remember { mutableStateOf(true) }
    BrowserDialog(
        title = stringResource(R.string.setting_clear_data),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.action_clear),
        onConfirm = { onClear(period.takeIf { history }, siteData, cache) },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        ToggleSetting(stringResource(R.string.clear_history), history, { history = it })
        if (history) HistoryPeriodSetting(period) { period = it }
        ToggleSetting(stringResource(R.string.clear_site_data), siteData, { siteData = it })
        ToggleSetting(stringResource(R.string.clear_cache), cache, { cache = it })
    }
}

@Preview
@Composable
private fun SettingsContentPreview() {
    BrowserTheme {
        SettingsContent(
            backupMessage = null,
            onBackupShown = {},
            onExport = {},
            onImport = {},
            settings = BrowserSettings(),
            engines = SearchEngines(),
            update = {},
            onSelectEngine = {},
            onSelectAppIcon = {},
            onClearData = { _, _, _ -> },
            onOpenUrl = {},
            onBack = {},
        )
    }
}

@Composable
private fun BackupResult.message() = when (this) {
    is BackupResult.Exported -> stringResource(R.string.backup_exported, counts.tiles, counts.bookmarks)
    is BackupResult.Imported -> stringResource(R.string.backup_imported, counts.tiles, counts.bookmarks)
    BackupResult.Failed -> stringResource(R.string.backup_failed)
}
