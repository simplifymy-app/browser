package app.simplifymy.browser

import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.provider.Browser
import android.view.Gravity
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.simplifymy.browser.core.intent.ExternalLink
import app.simplifymy.browser.core.intent.externalUrl
import app.simplifymy.browser.feature.browser.BrowserScreen
import app.simplifymy.browser.ui.component.ImageLoaders
import app.simplifymy.browser.ui.component.LocalImageLoaders
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.ExternalPackage
import mozilla.components.browser.state.state.PackageCategory
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.thumbnails.BrowserThumbnails
import mozilla.components.concept.base.images.ImageLoadRequest
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags
import mozilla.components.feature.contextmenu.ContextMenuCandidate
import mozilla.components.feature.contextmenu.ContextMenuFeature
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.feature.downloads.temporary.CopyDownloadFeature
import mozilla.components.feature.prompts.PromptFeature
import mozilla.components.feature.search.ext.buildSearchUrl
import mozilla.components.feature.session.FullScreenFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.sitepermissions.SitePermissionsFeature
import mozilla.components.lib.state.ext.flow
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.PermissionsFeature

private class IncomingLink(val link: ExternalLink, val source: SessionState.Source.External, val applicationId: String?)

class MainActivity : AppCompatActivity() {

    private val engineView by lazy { components.engine.createView(this) }

    private var permissionsRequester: PermissionsFeature? = null

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val grants = result.values.map { if (it) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED }
            permissionsRequester?.onPermissionsResult(result.keys.toTypedArray(), grants.toIntArray())
            permissionsRequester = null
        }

    private val sessionFeature by lazy {
        SessionFeature(
            components.store,
            components.sessionUseCases.goBack,
            components.sessionUseCases.goForward,
            engineView,
        )
    }

    private val fullScreenFeature by lazy {
        FullScreenFeature(
            store = components.store,
            sessionUseCases = components.sessionUseCases,
            viewportFitChanged = ::setDisplayCutoutMode,
            fullScreenChanged = ::setSystemBarsHidden,
        )
    }

    private val promptFeature: PromptFeature by lazy {
        PromptFeature(
            activity = this,
            store = components.store,
            fragmentManager = supportFragmentManager,
            tabsUseCases = components.tabsUseCases,
            fileUploadsDirCleaner = components.fileUploadsDirCleaner,
            onNeedToRequestPermissions = { askPermissions(promptFeature, it) },
            isSuggestEmailMaskEnabled = { false },
            isEmailMaskFeatureEnabled = { false },
        )
    }

    private val sitePermissionsFeature: SitePermissionsFeature by lazy {
        SitePermissionsFeature(
            context = this,
            storage = components.sitePermissionsStorage,
            fragmentManager = supportFragmentManager,
            onNeedToRequestPermissions = { askPermissions(sitePermissionsFeature, it) },
            onShouldShowRequestPermissionRationale = { shouldShowRequestPermissionRationale(it) },
            store = components.store,
            promptsStyling = SitePermissionsFeature.PromptsStyling(
                gravity = Gravity.BOTTOM,
                shouldWidthMatchParent = true,
                positiveButtonBackgroundColor = R.color.accent,
                positiveButtonTextColor = R.color.on_accent,
            ),
        )
    }

    private val downloadsFeature: DownloadsFeature by lazy {
        DownloadsFeature(
            applicationContext = applicationContext,
            store = components.store,
            useCases = components.downloadsUseCases,
            onNeedToRequestPermissions = { askPermissions(downloadsFeature, it) },
            downloadFileUtils = components.downloadFileUtils,
            downloadManager = FetchDownloadManager(
                applicationContext,
                components.store,
                DownloadService::class,
                notificationsDelegate = components.notificationsDelegate,
            ),
            fragmentManager = supportFragmentManager,
        )
    }

    private val contextMenuFeature by lazy {
        ContextMenuFeature(
            fragmentManager = supportFragmentManager,
            store = components.store,
            candidates = ContextMenuCandidate.defaultCandidates(
                context = this,
                tabsUseCases = components.tabsUseCases,
                contextMenuUseCases = components.contextMenuUseCases,
                snackBarParentView = engineView.asView(),
                downloadsLocation = { components.downloadFileUtils.currentDownloadLocation },
            ),
            engineView = engineView,
            useCases = components.contextMenuUseCases,
        )
    }

    private val copyImageFeature by lazy {
        CopyDownloadFeature(
            context = applicationContext,
            store = components.store,
            tabId = null,
            onCopyConfirmation = {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(this, R.string.image_copied, Toast.LENGTH_SHORT).show()
                }
            },
            httpClient = components.client,
        )
    }

    private val thumbnailsFeature by lazy { BrowserThumbnails(this, engineView, components.store) }

    private val imageLoaders by lazy {
        val thumbnailSize = (THUMBNAIL_DP * resources.displayMetrics.density).toInt()
        ImageLoaders(
            favicon = components::siteIcon,
            thumbnail = { id, private ->
                if (private) {
                    components.tabThumbnails.privateThumbnail(id)
                } else {
                    components.thumbnails.loadThumbnail(ImageLoadRequest(id, thumbnailSize, false)).await()
                }
            },
            image = components.amo::image,
            thumbnailVersions = components.tabThumbnails.versions,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        components.notificationsDelegate.bindToActivity(this)
        bind(sessionFeature, fullScreenFeature, promptFeature, sitePermissionsFeature, downloadsFeature, contextMenuFeature, copyImageFeature, thumbnailsFeature)
        onBackPressedDispatcher.addCallback(this) {
            if (fullScreenFeature.onBackPressed() || promptFeature.onBackPressed() || sessionFeature.onBackPressed()) return@addCallback
            if (returnToCaller()) return@addCallback
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
        }
        setContent {
            CompositionLocalProvider(LocalImageLoaders provides imageLoaders) {
                BrowserScreen(engineView)
            }
        }
        val fresh = savedInstanceState == null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
        val incoming = if (fresh) takeLink(intent) else null
        lifecycleScope.launch {
            components.tabsRestored.await()
            when {
                incoming != null -> open(incoming)
                components.store.state.tabs.isEmpty() -> components.tabsUseCases.addTab(SPEED_DIAL_URL)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val incoming = takeLink(intent) ?: return
        lifecycleScope.launch {
            components.tabsRestored.await()
            open(incoming)
        }
    }

    private fun takeLink(intent: Intent): IncomingLink? {
        setIntent(Intent())
        val link = externalUrl(
            action = intent.action,
            data = intent.dataString,
            text = intent.getStringExtra(Intent.EXTRA_TEXT),
            query = intent.getStringExtra(SearchManager.QUERY),
        ) ?: return null
        components.openedFromLink.value = true
        val caller = callerPackage(intent)?.let { ExternalPackage(it, PackageCategory.UNKNOWN) }
        val source = when (intent.action) {
            Intent.ACTION_SEND -> SessionState.Source.External.ActionSend(caller)
            Intent.ACTION_WEB_SEARCH -> SessionState.Source.External.ActionSearch(caller)
            else -> SessionState.Source.External.ActionView(caller)
        }
        return IncomingLink(link, source, intent.getStringExtra(Browser.EXTRA_APPLICATION_ID))
    }

    private fun callerPackage(intent: Intent): String? {
        val referrer = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_REFERRER, Uri::class.java)
        val fromReferrer = referrer?.takeIf { it.scheme == ANDROID_APP_SCHEME }?.host
        return (fromReferrer ?: callingPackage)?.takeIf { it.isNotBlank() && it != packageName }
    }

    private suspend fun open(incoming: IncomingLink) {
        val url = when (val link = incoming.link) {
            is ExternalLink.Url -> link.url
            is ExternalLink.Search -> components.store.flow()
                .mapNotNull { it.search.selectedOrDefaultSearchEngine }
                .first()
                .buildSearchUrl(link.text)
        }
        val callerId = incoming.source.caller?.packageId
        val reusable = incoming.applicationId?.takeIf { it == callerId }?.let { id ->
            components.store.state.tabs.lastOrNull { tab ->
                !tab.content.private && (tab.source as? SessionState.Source.External)?.caller?.packageId == id
            }
        }
        if (reusable != null) {
            components.tabsUseCases.selectTab(reusable.id)
            components.sessionUseCases.loadUrl(url, reusable.id, LoadUrlFlags.external())
        } else {
            components.tabsUseCases.addTab(url, selectTab = true, flags = LoadUrlFlags.external(), source = incoming.source)
        }
        components.linkOpened.tryEmit(Unit)
    }

    private fun returnToCaller(): Boolean {
        val tab = components.store.state.selectedTab ?: return false
        if (tab.source !is SessionState.Source.External || tab.content.canGoBack) return false
        components.tabsUseCases.removeTab(tab.id)
        if (components.store.state.tabs.isEmpty()) components.tabsUseCases.addTab(SPEED_DIAL_URL)
        moveTaskToBack(true)
        return true
    }

    override fun onStop() {
        super.onStop()
        components.openedFromLink.value = false
        components.appIcons.flush()
    }

    override fun onDestroy() {
        components.notificationsDelegate.unBindActivity(this)
        super.onDestroy()
    }

    @Deprecated("Prompt file pickers still deliver results through onActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!promptFeature.onActivityResult(requestCode, data, resultCode)) {
            @Suppress("DEPRECATION")
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setSystemBarsHidden(hidden: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (hidden) hide(WindowInsetsCompat.Type.systemBars()) else show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun setDisplayCutoutMode(mode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = mode }
        }
    }

    private fun askPermissions(requester: PermissionsFeature, permissions: Array<String>) {
        permissionsRequester = requester
        permissionsLauncher.launch(permissions)
    }

    private fun bind(vararg features: LifecycleAwareFeature) {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = features.forEach { it.start() }
            override fun onStop(owner: LifecycleOwner) = features.forEach { it.stop() }
        })
    }

    private companion object {
        const val THUMBNAIL_DP = 240
        const val ANDROID_APP_SCHEME = "android-app"
    }
}
