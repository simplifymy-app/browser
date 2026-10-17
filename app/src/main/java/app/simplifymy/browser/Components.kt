package app.simplifymy.browser

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import app.simplifymy.browser.core.addons.Amo
import app.simplifymy.browser.core.backup.Backup
import app.simplifymy.browser.core.database.BrowserDatabase
import app.simplifymy.browser.core.database.TabThumbnails
import app.simplifymy.browser.core.database.speedDialSnapshots
import app.simplifymy.browser.core.icons.DeclaredIcons
import app.simplifymy.browser.core.search.ADDITIONAL_SEARCH_ENGINES
import app.simplifymy.browser.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mozilla.components.browser.engine.gecko.GeckoEngine
import mozilla.components.browser.engine.gecko.fetch.GeckoViewFetchClient
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.Icon
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.session.storage.SessionStorage
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.engine.EngineMiddleware
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.PlacesBookmarksStorage
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.browser.thumbnails.ThumbnailsMiddleware
import mozilla.components.browser.thumbnails.storage.ThumbnailStorage
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.feature.addons.AddonManager
import mozilla.components.feature.addons.amo.AMOAddonsProvider
import mozilla.components.feature.addons.update.DefaultAddonUpdater
import mozilla.components.feature.contextmenu.ContextMenuUseCases
import mozilla.components.feature.downloads.DownloadMiddleware
import mozilla.components.feature.downloads.DownloadsUseCases
import mozilla.components.feature.prompts.file.FileUploadsDirCleaner
import mozilla.components.feature.prompts.file.FileUploadsDirCleanerMiddleware
import mozilla.components.feature.search.middleware.SearchMiddleware
import mozilla.components.feature.search.region.RegionMiddleware
import mozilla.components.feature.session.HistoryDelegate
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.sitepermissions.OnDiskSitePermissionsStorage
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.lib.state.Middleware
import mozilla.components.service.location.LocationService
import mozilla.components.support.base.android.NotificationsDelegate
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import mozilla.components.support.utils.DefaultDownloadFileUtils
import org.mozilla.geckoview.GeckoRuntime

private val consumeSaveToPdfResults: Middleware<BrowserState, BrowserAction> = { _, next, action ->
    if (action !is EngineAction.SaveToPdfCompleteAction && action !is EngineAction.SaveToPdfExceptionAction) next(action)
}

class Components(private val context: Context) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val settings by lazy { SettingsRepository(context) }

    val database by lazy { BrowserDatabase.create(context) }

    val backup by lazy { Backup(context.contentResolver, database.speedDial(), bookmarks, settings) }

    val runtime: GeckoRuntime by lazy { GeckoRuntime.create(context) }

    val history by lazy { PlacesHistoryStorage(context) }

    val bookmarks by lazy { PlacesBookmarksStorage(context) }

    val engine by lazy {
        GeckoEngine(context, DefaultSettings(historyTrackingDelegate = HistoryDelegate(lazy { history })), runtime)
    }

    val client by lazy { GeckoViewFetchClient(context, runtime) }

    val icons by lazy { BrowserIcons(context, client) }

    val thumbnails by lazy { ThumbnailStorage(context) }

    val tabThumbnails by lazy { TabThumbnails(thumbnails) }

    private val declaredIcons by lazy { DeclaredIcons(client) }

    suspend fun siteIcon(url: String, discover: Boolean): Bitmap {
        if (discover) declaredIcons.of(url)?.takeIf { it.isNotEmpty() }?.let { return loadIcon(url, it) }
        val cached = icons.loadIcon(IconRequest(url, IconRequest.Size.LAUNCHER)).await()
        if (cached.source != Icon.Source.GENERATOR) return cached.bitmap
        val origin = Uri.parse(url).let { "${it.scheme}://${it.authority}" }
        return loadIcon(
            url,
            listOf(
                IconRequest.Resource("$origin/apple-touch-icon.png", IconRequest.Resource.Type.APPLE_TOUCH_ICON),
                IconRequest.Resource("$origin/favicon.ico", IconRequest.Resource.Type.FAVICON),
            ),
        )
    }

    private suspend fun loadIcon(url: String, resources: List<IconRequest.Resource>) =
        icons.loadIcon(IconRequest(url, IconRequest.Size.LAUNCHER, resources)).await().bitmap

    val sessionStorage by lazy { SessionStorage(context, engine, applicationScope = scope) }

    val fileUploadsDirCleaner by lazy { FileUploadsDirCleaner(scope, cacheDirectory = { context.cacheDir }) }

    val downloadFileUtils by lazy { DefaultDownloadFileUtils(context) }

    val notificationsDelegate by lazy { NotificationsDelegate(NotificationManagerCompat.from(context)) }

    val store by lazy {
        BrowserStore(
            middleware = EngineMiddleware.create(engine) + listOf(
                SearchMiddleware(context, additionalBundledSearchEngineIds = ADDITIONAL_SEARCH_ENGINES),
                RegionMiddleware(context, LocationService.dummy(), applicationScope = scope),
                FileUploadsDirCleanerMiddleware(fileUploadsDirCleaner),
                // speedDialSnapshots must run before tabThumbnails, which consumes UpdateThumbnailAction;
                // ThumbnailsMiddleware after them only deletes thumbnails of removed tabs.
                speedDialSnapshots(thumbnails, speedDialHosts),
                tabThumbnails.middleware,
                ThumbnailsMiddleware(thumbnails),
                consumeSaveToPdfResults,
                DownloadMiddleware(context, DownloadService::class.java, { false }, downloadFileUtils),
            ),
        )
    }

    private val speedDialHosts by lazy {
        database.speedDial().tiles()
            .map { tiles -> tiles.map { it.url.tryGetHostFromUrl() }.toSet() }
            .stateIn(scope, SharingStarted.Eagerly, emptySet())
    }

    val sessionUseCases by lazy { SessionUseCases(store) }

    val tabsUseCases by lazy { TabsUseCases(store) }

    val contextMenuUseCases by lazy { ContextMenuUseCases(store) }

    val downloadsUseCases by lazy { DownloadsUseCases(store, downloadFileUtils) }

    val sitePermissionsStorage by lazy { OnDiskSitePermissionsStorage(context) }

    val addonManager by lazy {
        AddonManager(store, engine, AMOAddonsProvider(context, client, maxCacheAgeInMinutes = 60), addonUpdater)
    }

    val addonUpdater by lazy { DefaultAddonUpdater(context, notificationsDelegate = notificationsDelegate) }

    val amo by lazy { Amo(client) }
}
