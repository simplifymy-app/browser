package app.simplifymy.browser.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.simplifymy.browser.Components
import app.simplifymy.browser.componentsFactory
import app.simplifymy.browser.core.backup.BackupCounts
import app.simplifymy.browser.core.search.choices
import app.simplifymy.browser.core.search.selectSearchEngine
import app.simplifymy.browser.core.search.selectedId
import app.simplifymy.browser.core.settings.BrowserSettings
import app.simplifymy.browser.core.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.concept.engine.Engine
import mozilla.components.lib.state.ext.flow

sealed interface BackupResult {
    data class Exported(val counts: BackupCounts) : BackupResult
    data class Imported(val counts: BackupCounts) : BackupResult
    data object Failed : BackupResult
}

data class SearchEngines(val choices: List<SearchEngine> = emptyList(), val selectedId: String? = null)

class SettingsViewModel(private val components: Components) : ViewModel() {

    val settings: StateFlow<BrowserSettings> =
        components.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, BrowserSettings())

    val searchEngines: StateFlow<SearchEngines> = components.store.flow()
        .map { SearchEngines(it.search.choices, it.search.selectedId) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchEngines())

    private val _backup = MutableStateFlow<BackupResult?>(null)
    val backup: StateFlow<BackupResult?> = _backup.asStateFlow()

    fun export(uri: Uri) = runBackup { BackupResult.Exported(components.backup.export(uri)) }

    fun import(uri: Uri) = runBackup { BackupResult.Imported(components.backup.import(uri)) }

    fun backupShown() {
        _backup.value = null
    }

    private fun runBackup(block: suspend () -> BackupResult) {
        viewModelScope.launch { _backup.value = runCatching { block() }.getOrDefault(BackupResult.Failed) }
    }

    fun update(change: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { components.settings.change() }
    }

    fun selectSearchEngine(engine: SearchEngine) = components.store.selectSearchEngine(engine)

    fun clearData(history: Boolean, siteData: Boolean, cache: Boolean) {
        viewModelScope.launch {
            if (history) components.history.deleteEverything()
            val types = listOfNotNull(
                Engine.BrowsingData.ALL_SITE_DATA.takeIf { siteData },
                Engine.BrowsingData.ALL_CACHES.takeIf { cache },
            )
            if (types.isNotEmpty()) components.engine.clearData(Engine.BrowsingData.select(*types.toIntArray()))
        }
    }

    companion object {
        val Factory = componentsFactory { SettingsViewModel(it) }
    }
}
