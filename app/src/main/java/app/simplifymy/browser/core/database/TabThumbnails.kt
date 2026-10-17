package app.simplifymy.browser.core.database

import android.graphics.Bitmap
import android.util.LruCache
import app.simplifymy.browser.SPEED_DIAL_URL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.thumbnails.storage.ThumbnailStorage
import mozilla.components.concept.base.images.ImageSaveRequest
import mozilla.components.concept.engine.EngineView
import mozilla.components.lib.state.Middleware

class TabThumbnails(private val storage: ThumbnailStorage) {

    private val _versions = MutableStateFlow<Map<String, Int>>(emptyMap())
    val versions: StateFlow<Map<String, Int>> = _versions.asStateFlow()

    private val privateThumbnails = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 16).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    // Consumes UpdateThumbnailAction on purpose: bitmaps never reach BrowserState, so they don't sit in memory there.
    val middleware: Middleware<BrowserState, BrowserAction> = { store, next, action ->
        when (action) {
            is ContentAction.UpdateThumbnailAction -> store.state.findTab(action.sessionId)?.let { tab ->
                if (tab.content.private) {
                    privateThumbnails.put(tab.id, action.thumbnail)
                    bumpVersion(tab.id)
                } else {
                    storage.saveThumbnail(ImageSaveRequest(tab.id, false), action.thumbnail).invokeOnCompletion { bumpVersion(tab.id) }
                }
            }
            is TabListAction.RemoveTabAction -> {
                privateThumbnails.remove(action.tabId)
                next(action)
            }
            is TabListAction.RemoveTabsAction -> {
                action.tabIds.forEach { privateThumbnails.remove(it) }
                next(action)
            }
            is TabListAction.RemoveAllPrivateTabsAction, is TabListAction.RemoveAllTabsAction -> {
                privateThumbnails.evictAll()
                next(action)
            }
            else -> next(action)
        }
    }

    fun privateThumbnail(id: String): Bitmap? = privateThumbnails.get(id)

    private fun bumpVersion(id: String) = _versions.update { it + (id to (it[id] ?: 0) + 1) }

    fun capture(engineView: EngineView, store: BrowserStore) {
        val tab = store.state.selectedTab?.takeUnless { it.content.url == SPEED_DIAL_URL } ?: return
        engineView.captureThumbnail { bitmap ->
            bitmap?.let { store.dispatch(ContentAction.UpdateThumbnailAction(tab.id, it)) }
        }
    }
}
