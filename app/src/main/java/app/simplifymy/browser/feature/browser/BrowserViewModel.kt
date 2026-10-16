package app.simplifymy.browser.feature.browser

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.simplifymy.browser.Components
import app.simplifymy.browser.core.backup.MOBILE_BOOKMARKS
import app.simplifymy.browser.SPEED_DIAL_URL
import app.simplifymy.browser.componentsFactory
import app.simplifymy.browser.core.search.choices
import app.simplifymy.browser.core.search.selectSearchEngine
import app.simplifymy.browser.core.search.selectedId
import app.simplifymy.browser.core.settings.BrowserSettings
import app.simplifymy.browser.core.settings.ToolbarPosition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SecurityInfo
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.concept.engine.webextension.Action
import mozilla.components.feature.search.ext.buildSearchUrl
import mozilla.components.lib.state.ext.flow
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.support.ktx.kotlin.toNormalizedUrl

data class TabItem(
    val id: String,
    val title: String,
    val url: String,
    val icon: Bitmap?,
    val private: Boolean,
) {
    val speedDial get() = url == SPEED_DIAL_URL
}

enum class Security { Secure, Insecure, Unknown }

data class ExtensionAction(val id: String, val title: String, val action: Action)

data class BrowserUiState(
    val url: String = "",
    val title: String = "",
    val security: Security = Security.Unknown,
    val issuer: String = "",
    val loading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val searchIcon: Bitmap? = null,
    val searchEngines: List<SearchEngine> = emptyList(),
    val searchEngineId: String? = null,
    val position: ToolbarPosition = ToolbarPosition.Bottom,
    val private: Boolean = false,
    val selectedTabId: String? = null,
    val tabs: List<TabItem> = emptyList(),
    val normalTabs: List<TabItem> = emptyList(),
    val privateTabs: List<TabItem> = emptyList(),
    val bookmarked: Boolean = false,
    val desktopMode: Boolean = false,
) {
    val speedDial get() = url.isEmpty() && selectedTabId != null
}

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModel(private val components: Components) : ViewModel() {

    private val store = components.store
    private val bookmarksChanged = MutableStateFlow(0)
    private val pendingUrl = MutableStateFlow<String?>(null)
    private val browser = store.flow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), store.state)

    val progress: StateFlow<Float> = browser
        .map { state -> state.selectedTab?.content?.takeIf { it.loading && it.progress < 100 }?.progress?.div(100f) ?: 0f }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val extensionActions: StateFlow<List<ExtensionAction>> = browser
        .map { it.extensionActions(it.selectedTab) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val bookmarked = combine(
        browser.map { it.selectedTab?.content?.url }.distinctUntilChanged(),
        bookmarksChanged,
    ) { url, _ -> url }.mapLatest { url -> url != null && isBookmarked(url) }

    val settings: StateFlow<BrowserSettings> =
        components.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, BrowserSettings())

    init {
        viewModelScope.launch {
            browser.map { it.selectedTab?.id to it.selectedTab?.content?.url }.distinctUntilChanged().collect { pendingUrl.value = null }
        }
    }

    val state: StateFlow<BrowserUiState> = combine(
        browser,
        components.settings.settings,
        bookmarked,
        pendingUrl,
    ) { browser, settings, bookmarked, pending ->
        val tab = browser.selectedTab
        val content = tab?.content
        val private = content?.private == true
        BrowserUiState(
            url = pending ?: content?.url?.takeUnless { it == SPEED_DIAL_URL }.orEmpty(),
            title = content?.title.orEmpty(),
            security = if (pending != null) Security.Unknown else when (content?.securityInfo) {
                is SecurityInfo.Secure -> Security.Secure
                is SecurityInfo.Insecure -> if (content.url.startsWith("http")) Security.Insecure else Security.Unknown
                else -> Security.Unknown
            },
            issuer = content?.securityInfo?.issuer.orEmpty(),
            loading = content?.loading == true,
            canGoBack = content?.canGoBack == true,
            canGoForward = content?.canGoForward == true,
            searchIcon = browser.search.selectedOrDefaultSearchEngine?.icon,
            searchEngines = browser.search.choices,
            searchEngineId = browser.search.selectedId,
            position = settings.toolbarPosition,
            private = private,
            selectedTabId = tab?.id,
            tabs = browser.tabItems(private),
            normalTabs = browser.tabItems(false),
            privateTabs = browser.tabItems(true),
            bookmarked = bookmarked,
            desktopMode = content?.desktopMode == true,
        )
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserUiState())

    fun submit(input: String) {
        val text = input.trim()
        if (text.isEmpty()) return
        val url = if (text.isUrl()) {
            text.toNormalizedUrl()
        } else {
            store.state.search.selectedOrDefaultSearchEngine?.buildSearchUrl(text) ?: return
        }
        load(url, text)
    }

    fun open(url: String) = load(url, null)

    private fun load(url: String, input: String?) {
        components.sessionUseCases.loadUrl(url, originalInput = input)
        pendingUrl.value = url
    }

    fun goForward() = components.sessionUseCases.goForward()

    fun reload() = components.sessionUseCases.reload()

    fun stopLoading() = components.sessionUseCases.stopLoading()

    fun savePage() = components.sessionUseCases.saveToPdf()

    fun setMobileView(mobile: Boolean) = components.sessionUseCases.requestDesktopSite(enable = !mobile)

    fun newTab(private: Boolean) {
        components.tabsUseCases.addTab(SPEED_DIAL_URL, private = private)
    }

    fun selectTab(id: String) = components.tabsUseCases.selectTab(id)

    fun closeTab(id: String) {
        components.tabsUseCases.removeTab(id)
        if (store.state.tabs.isEmpty()) newTab(private = false)
    }

    fun selectSearchEngine(engine: SearchEngine) = store.selectSearchEngine(engine)

    fun toggleBookmark() {
        val current = state.value
        if (current.url.isEmpty()) return
        viewModelScope.launch {
            val existing = components.bookmarks.getBookmarksWithUrl(current.url).getOrDefault(emptyList())
            if (existing.isEmpty()) {
                components.bookmarks.addItem(MOBILE_BOOKMARKS, current.url, current.title, null)
            } else {
                existing.forEach { components.bookmarks.deleteNode(it.guid) }
            }
            bookmarksChanged.value++
        }
    }

    private suspend fun isBookmarked(url: String) =
        components.bookmarks.getBookmarksWithUrl(url).getOrDefault(emptyList()).isNotEmpty()

    private fun BrowserState.tabItems(private: Boolean) = getNormalOrPrivateTabs(private).map { it.toItem() }

    private fun TabSessionState.toItem() = TabItem(id, content.title, content.url, content.icon, content.private)

    private fun BrowserState.extensionActions(tab: TabSessionState?) = extensions.values
        .filter { it.enabled && it.browserAction != null }
        .map { extension ->
            val action = extension.browserAction!!.copyWithOverride(tab?.extensionState?.get(extension.id)?.browserAction)
            ExtensionAction(extension.id, extension.name ?: action.title.orEmpty(), action)
        }

    companion object {
        val Factory = componentsFactory { BrowserViewModel(it) }
    }
}
