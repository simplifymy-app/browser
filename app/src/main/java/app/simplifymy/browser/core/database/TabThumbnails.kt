package app.simplifymy.browser.core.database

import app.simplifymy.browser.SPEED_DIAL_URL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.ContentAction
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

    val middleware: Middleware<BrowserState, BrowserAction> = { store, next, action ->
        if (action is ContentAction.UpdateThumbnailAction) {
            store.state.findTab(action.sessionId)?.let { tab ->
                storage.saveThumbnail(ImageSaveRequest(tab.id, tab.content.private), action.thumbnail).invokeOnCompletion {
                    _versions.update { it + (tab.id to (it[tab.id] ?: 0) + 1) }
                }
            }
        } else {
            next(action)
        }
    }

    fun capture(engineView: EngineView, store: BrowserStore) {
        val tab = store.state.selectedTab?.takeUnless { it.content.url == SPEED_DIAL_URL } ?: return
        engineView.captureThumbnail { bitmap ->
            bitmap?.let { store.dispatch(ContentAction.UpdateThumbnailAction(tab.id, it)) }
        }
    }
}
