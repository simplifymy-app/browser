package app.simplifymy.browser.core.database

import kotlinx.coroutines.flow.StateFlow
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.thumbnails.storage.ThumbnailStorage
import mozilla.components.concept.base.images.ImageSaveRequest
import mozilla.components.lib.state.Middleware
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl

private val UNSAFE_KEY_CHARS = Regex("[^a-z0-9_-]")

fun snapshotId(url: String) = ("speeddial_" + url.tryGetHostFromUrl().lowercase().replace(UNSAFE_KEY_CHARS, "_")).take(64)

fun speedDialSnapshots(storage: ThumbnailStorage, hosts: StateFlow<Set<String>>): Middleware<BrowserState, BrowserAction> =
    { store, next, action ->
        if (action is ContentAction.UpdateThumbnailAction) {
            val content = store.state.findTab(action.sessionId)?.content
            if (content != null && !content.private && content.url.tryGetHostFromUrl() in hosts.value) {
                storage.saveThumbnail(ImageSaveRequest(snapshotId(content.url), isPrivate = false), action.thumbnail)
            }
        }
        next(action)
    }
