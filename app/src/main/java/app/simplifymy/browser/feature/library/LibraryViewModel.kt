package app.simplifymy.browser.feature.library

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.simplifymy.browser.Components
import app.simplifymy.browser.core.backup.MOBILE_BOOKMARKS
import app.simplifymy.browser.componentsFactory
import app.simplifymy.browser.core.history.HistoryPeriod
import app.simplifymy.browser.core.history.clear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.lib.state.ext.flow

data class LibraryEntry(val key: String, val title: String, val url: String, val time: Long)

data class DownloadItem(val state: DownloadState, val bytesPerSecond: Long? = null)

val DownloadState.isActive: Boolean
    get() = status == DownloadState.Status.INITIATED || status == DownloadState.Status.DOWNLOADING || status == DownloadState.Status.PAUSED

private const val SPEED_SMOOTHING = 0.4

private class SpeedSample(val bytes: Long, val time: Long, val rate: Double?)

class LibraryViewModel(private val components: Components) : ViewModel() {

    private val _bookmarks = MutableStateFlow(emptyList<LibraryEntry>())
    val bookmarks: StateFlow<List<LibraryEntry>> = _bookmarks.asStateFlow()

    private val _history = MutableStateFlow(emptyList<LibraryEntry>())
    val history: StateFlow<List<LibraryEntry>> = _history.asStateFlow()

    private val speedSamples = mutableMapOf<String, SpeedSample>()

    val downloads: StateFlow<List<DownloadItem>> = components.store.flow()
        .map { state -> state.downloads.values.sortedByDescending { it.createdTime } }
        .distinctUntilChanged()
        .map { downloads -> downloads.map { DownloadItem(it, speedOf(it)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun speedOf(download: DownloadState): Long? {
        if (download.status != DownloadState.Status.DOWNLOADING) {
            speedSamples.remove(download.id)
            return null
        }
        val now = SystemClock.elapsedRealtime()
        val bytes = download.currentBytesCopied
        val previous = speedSamples[download.id]
        if (previous != null && bytes == previous.bytes) return previous.rate?.toLong()
        val rate = previous?.let {
            val current = (bytes - it.bytes) * 1000.0 / (now - it.time).coerceAtLeast(1)
            it.rate?.let { last -> last + (current - last) * SPEED_SMOOTHING } ?: current
        }
        speedSamples[download.id] = SpeedSample(bytes, now, rate)
        return rate?.toLong()
    }

    fun openDownload(download: DownloadState): Boolean =
        components.downloadFileUtils.openFile(download.fileName, download.directoryPath, download.contentType)

    fun stopDownload(download: DownloadState) = components.cancelDownload(download.id)

    fun removeDownload(download: DownloadState, deleteFile: Boolean) {
        if (download.isActive) components.cancelDownload(download.id)
        components.downloadsUseCases.removeDownload(download.id, deleteFile)
    }

    fun load() {
        viewModelScope.launch {
            _bookmarks.value = components.bookmarks.getTree(MOBILE_BOOKMARKS).getOrNull()?.children.orEmpty()
                .filter { it.url != null }
                .map { LibraryEntry(it.guid, it.title.orEmpty(), it.url!!, it.dateAdded) }
            _history.value = components.history.getVisitsPaginated(0, HISTORY_PAGE)
                .map { LibraryEntry(it.url + it.visitTime, it.title.orEmpty(), it.url, it.visitTime) }
        }
    }

    fun deleteBookmark(entry: LibraryEntry) {
        viewModelScope.launch {
            components.bookmarks.deleteNode(entry.key)
            load()
        }
    }

    fun deleteHistory(entry: LibraryEntry) {
        viewModelScope.launch {
            components.history.deleteVisitsFor(entry.url)
            load()
        }
    }

    fun clearHistory(period: HistoryPeriod) {
        viewModelScope.launch {
            components.history.clear(period)
            load()
        }
    }

    companion object {
        private const val HISTORY_PAGE = 300L
        val Factory = componentsFactory { LibraryViewModel(it) }
    }
}
