package app.simplifymy.browser.feature.browser

import androidx.compose.animation.core.animateFloatAsState
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.simplifymy.browser.core.settings.ToolbarPosition
import app.simplifymy.browser.feature.addons.AddonsScreen
import app.simplifymy.browser.feature.addons.AddonsViewModel
import app.simplifymy.browser.feature.addons.ExtensionPopup
import app.simplifymy.browser.feature.addons.ExtensionPrompt
import app.simplifymy.browser.feature.library.LibraryScreen
import app.simplifymy.browser.feature.library.LibrarySection
import app.simplifymy.browser.feature.library.LibraryViewModel
import app.simplifymy.browser.feature.settings.SettingsScreen
import app.simplifymy.browser.feature.settings.SettingsViewModel
import app.simplifymy.browser.feature.speeddial.SpeedDialScreen
import app.simplifymy.browser.feature.tabs.TabSwitcherScreen
import app.simplifymy.browser.ui.component.Snackbar
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Motion
import app.simplifymy.browser.ui.theme.enterTween
import kotlinx.coroutines.flow.combine
import mozilla.components.concept.engine.EngineView
import app.simplifymy.browser.ui.theme.ThemeMode
import mozilla.components.support.ktx.android.content.share

private val EXTENSIONS_SHORTCUTS = setOf("addons", "about:addons")
private val NOTICE_OFFSET = 112.dp

@OptIn(ExperimentalLayoutApi::class)
private val ChromeInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.exclude(WindowInsets.ime).union(WindowInsets.imeAnimationTarget)

enum class Overlay(val section: LibrarySection? = null) {
    None,
    Tabs,
    Extensions,
    Bookmarks(LibrarySection.Bookmarks),
    History(LibrarySection.History),
    Downloads(LibrarySection.Downloads),
    Settings,
}

@Composable
fun BrowserScreen(engineView: EngineView) {
    val viewModel: BrowserViewModel = viewModel(factory = BrowserViewModel.Factory)
    val addons: AddonsViewModel = viewModel(factory = AddonsViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress = viewModel.progress.collectAsStateWithLifecycle()
    val extensionActions = viewModel.extensionActions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val prompt by addons.prompt.collectAsStateWithLifecycle()
    val popup by addons.popup.collectAsStateWithLifecycle()
    var overlay by remember { mutableStateOf(Overlay.None) }
    var notice by remember { mutableStateOf<String?>(null) }
    val close = { overlay = Overlay.None }
    val context = LocalContext.current
    val activity = LocalActivity.current

    val menuActions = MenuActions(
        newTab = {
            viewModel.captureThumbnail(engineView)
            viewModel.newTab(it)
        },
        open = { overlay = it },
        forward = viewModel::goForward,
        savePage = viewModel::savePage,
        mobileView = viewModel::setMobileView,
        toggleBookmark = viewModel::toggleBookmark,
        share = { context.share(state.url) },
        reload = viewModel::reload,
        exit = { activity?.finishAndRemoveTask() },
    )

    LaunchedEffect(settings.themeMode) { AppCompatDelegate.setDefaultNightMode(settings.themeMode.nightMode()) }
    BrowserTheme(
        mode = if (state.private) ThemeMode.Black else settings.themeMode,
        accent = settings.accent,
        contrast = settings.contrast,
        corners = settings.corners,
        family = settings.font,
        textScale = settings.textScale,
        menuDensity = settings.menuDensity,
    ) {
        SystemBars()
        Box(Modifier.fillMaxSize()) {
            BrowserChrome(
                state = state,
                showTabStrip = settings.showTabStrip,
                hideOnScroll = settings.hideToolbarOnScroll,
                progress = { progress.value },
                extensionActions = { extensionActions.value },
                viewModel = viewModel,
                engineView = engineView,
                menuActions = menuActions,
                onTabs = {
                    viewModel.captureThumbnail(engineView)
                    overlay = Overlay.Tabs
                },
                onPanel = { overlay = Overlay.Bookmarks },
                onSubmit = { if (it.trim().lowercase() in EXTENSIONS_SHORTCUTS) overlay = Overlay.Extensions else viewModel.submit(it) },
            )
            when (overlay) {
                Overlay.None -> Unit
                Overlay.Tabs -> TabSwitcherScreen(
                    normalTabs = state.normalTabs,
                    privateTabs = state.privateTabs,
                    selectedId = state.selectedTabId,
                    startPrivate = state.private,
                    onSelect = { viewModel.selectTab(it); close() },
                    onClose = viewModel::closeTab,
                    onNewTab = { viewModel.newTab(it); close() },
                    onDismiss = close,
                )
                Overlay.Extensions -> AddonsScreen(addons, close)
                Overlay.Settings -> SettingsScreen(
                    viewModel = viewModel(factory = SettingsViewModel.Factory),
                    onOpenUrl = { viewModel.newTab(false); viewModel.open(it); close() },
                    onBack = close,
                )
                else -> LibraryScreen(
                    viewModel = viewModel(factory = LibraryViewModel.Factory),
                    start = overlay.section!!,
                    onOpen = { viewModel.open(it); close() },
                    onBack = close,
                )
            }
            prompt?.let { ExtensionPrompt(it, addons) { text -> notice = text } }
            notice?.let { text ->
                Snackbar(
                    text = text,
                    onDone = { notice = null },
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = NOTICE_OFFSET),
                )
            }
            popup?.let { extension -> ExtensionPopup(extension) { addons.closePopup(extension) } }
        }
    }
}

@Composable
private fun BrowserChrome(
    state: BrowserUiState,
    showTabStrip: Boolean,
    hideOnScroll: Boolean,
    progress: () -> Float,
    extensionActions: () -> List<ExtensionAction>,
    viewModel: BrowserViewModel,
    engineView: EngineView,
    menuActions: MenuActions,
    onTabs: () -> Unit,
    onPanel: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val top = state.position == ToolbarPosition.Top
    val chrome = rememberChromeVisibility(engineView, hideOnScroll, state.url)
    var editing by remember { mutableStateOf(false) }
    val completion = viewModel.completion.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    LaunchedEffect(editing) { if (!editing) viewModel.setQuery("") }
    val toolbar = @Composable {
        BrowserToolbar(
            state = state,
            progress = progress,
            onSubmit = onSubmit,
            onStop = viewModel::stopLoading,
            onPanel = onPanel,
            onTabs = onTabs,
            menu = { BrowserMenu(state, extensionActions, menuActions) },
            editing = editing,
            onEditingChange = { editing = it },
            completion = completion.value,
            onQueryChange = viewModel::setQuery,
            onSelectEngine = viewModel::selectSearchEngine,
        )
    }
    val strip = @Composable {
        TabStrip(
            tabs = state.tabs,
            selectedId = state.selectedTabId,
            onSelect = {
                if (it != state.selectedTabId) viewModel.captureThumbnail(engineView)
                viewModel.selectTab(it)
            },
            onClose = viewModel::closeTab,
            onNewTab = {
                viewModel.captureThumbnail(engineView)
                viewModel.newTab(state.private)
            },
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(BrowserTheme.colors.background)
            .windowInsetsPadding(ChromeInsets),
    ) {
        if (top && chrome.present) {
            Column(Modifier.graphicsLayer { alpha = chrome.alpha() }) {
                if (showTabStrip) strip()
                toolbar()
            }
        }
        Box(Modifier.weight(1f)) {
            AndroidView(factory = { engineView.asView() }, modifier = Modifier.fillMaxSize())
            if (state.speedDial) SpeedDialScreen(onOpen = viewModel::open)
            if (editing && suggestions.isNotEmpty()) {
                SuggestionList(
                    suggestions = suggestions,
                    nearBottom = !top,
                    onPick = { suggestion ->
                        editing = false
                        when (suggestion) {
                            is Suggestion.Page -> viewModel.open(suggestion.url)
                            is Suggestion.Search -> onSubmit(suggestion.text)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().align(if (top) Alignment.TopCenter else Alignment.BottomCenter),
                )
            }
        }
        if (!top && chrome.present) {
            Column(Modifier.graphicsLayer { alpha = chrome.alpha() }) {
                toolbar()
                if (showTabStrip) strip()
            }
        }
    }
}

@Composable
private fun SystemBars() {
    val view = LocalView.current
    val light = BrowserTheme.colors.isLight
    val window = LocalActivity.current?.window ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }
}

private class ChromeVisibility(val present: Boolean, val alpha: () -> Float)

@Composable
private fun rememberChromeVisibility(engineView: EngineView, enabled: Boolean, url: String): ChromeVisibility {
    var hidden by remember { mutableStateOf(false) }
    LaunchedEffect(enabled, url) {
        hidden = false
        if (enabled) {
            combine(engineView.verticalScrollPosition, engineView.verticalScrollDelta) { position, delta -> position to delta }
                .collect { (position, delta) ->
                    when {
                        position <= 0f || delta < -SCROLL_THRESHOLD -> hidden = false
                        delta > SCROLL_THRESHOLD -> hidden = true
                    }
                }
        }
    }
    val alpha = animateFloatAsState(if (hidden) 0f else 1f, enterTween(Motion.TOKEN), label = "chrome")
    return ChromeVisibility(present = !hidden || alpha.value > 0f, alpha = { alpha.value })
}

private const val SCROLL_THRESHOLD = 8f

private fun ThemeMode.nightMode() = when (this) {
    ThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.Dark, ThemeMode.Black -> AppCompatDelegate.MODE_NIGHT_YES
}
