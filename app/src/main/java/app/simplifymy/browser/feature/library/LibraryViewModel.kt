package app.simplifymy.browser.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.simplifymy.browser.Components
import app.simplifymy.browser.core.backup.MOBILE_BOOKMARKS
import app.simplifymy.browser.componentsFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.lib.state.ext.flow

data class LibraryEntry(val key: String, val title: String, val url: String, val time: Long)

class LibraryViewModel(private val components: Components) : ViewModel() {

    private val _bookmarks = MutableStateFlow(emptyList<LibraryEntry>())
    val bookmarks: StateFlow<List<LibraryEntry>> = _bookmarks.asStateFlow()

    private val _history = MutableStateFlow(emptyList<LibraryEntry>())
    val history: StateFlow<List<LibraryEntry>> = _history.asStateFlow()

    val downloads: StateFlow<List<DownloadState>> = components.store.flow()
        .map { state -> state.downloads.values.sortedByDescending { it.createdTime } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun clearHistory() {
        viewModelScope.launch {
            components.history.deleteEverything()
            load()
        }
    }

    companion object {
        private const val HISTORY_PAGE = 300L
        val Factory = componentsFactory { LibraryViewModel(it) }
    }
}
