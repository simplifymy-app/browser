package app.simplifymy.browser

import android.app.Application
import android.content.Context
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.DefaultDesktopModeAction
import mozilla.components.browser.state.action.SearchAction
import mozilla.components.feature.addons.update.GlobalAddonDependencyProvider
import mozilla.components.support.ktx.android.content.isMainProcess
import mozilla.components.support.webextensions.WebExtensionSupport

const val SPEED_DIAL_URL = "about:blank"

val Context.components: Components get() = (applicationContext as BrowserApp).components

class BrowserApp : Application() {

    val components by lazy { Components(this) }

    override fun onCreate() {
        super.onCreate()
        if (!isMainProcess()) return
        with(components) {
            store.dispatch(SearchAction.RefreshSearchEnginesAction)
            icons.install(engine, store)
            GlobalAddonDependencyProvider.initialize(addonManager, addonUpdater)
            WebExtensionSupport.initialize(
                runtime = engine,
                store = store,
                onNewTabOverride = { _, engineSession, url, selected, private ->
                    tabsUseCases.addTab(url, selectTab = selected, engineSession = engineSession, private = private)
                },
                onCloseTabOverride = { _, tabId -> tabsUseCases.removeTab(tabId) },
                onSelectTabOverride = { _, tabId -> tabsUseCases.selectTab(tabId) },
                onUpdatePermissionRequest = addonUpdater::onUpdatePermissionRequest,
                onExtensionsLoaded = { addonUpdater.registerForFutureUpdates(it) },
            )
            scope.launch {
                settings.settings.map { !it.mobileView }.distinctUntilChanged().collect {
                    store.dispatch(DefaultDesktopModeAction.DesktopModeUpdated(it))
                }
            }
            scope.launch { appIcons.request(settings.settings.first().appIcon) }
            scope.launch {
                if (settings.settings.first().restoreTabs) tabsUseCases.restore(sessionStorage)
                if (store.state.tabs.isEmpty()) tabsUseCases.addTab(SPEED_DIAL_URL)
                sessionStorage.autoSave(store).periodicallyInForeground().whenGoingToBackground().whenSessionsChange()
            }
        }
    }
}
