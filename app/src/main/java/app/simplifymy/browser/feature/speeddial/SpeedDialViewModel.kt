package app.simplifymy.browser.feature.speeddial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.simplifymy.browser.Components
import app.simplifymy.browser.componentsFactory
import app.simplifymy.browser.core.database.SpeedDialTile
import app.simplifymy.browser.core.settings.BrowserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mozilla.components.support.ktx.kotlin.toNormalizedUrl

class SpeedDialViewModel(components: Components) : ViewModel() {

    val settings: StateFlow<BrowserSettings> =
        components.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, BrowserSettings())

    private val dao = components.database.speedDial()

    val tiles: StateFlow<List<SpeedDialTile>> =
        dao.tiles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(tile: SpeedDialTile?, title: String, url: String) {
        val normalized = url.trim().toNormalizedUrl()
        val name = title.trim().ifEmpty { normalized }
        viewModelScope.launch {
            if (tile == null) dao.add(name, normalized) else dao.update(tile.copy(title = name, url = normalized))
        }
    }

    fun remove(tile: SpeedDialTile) {
        viewModelScope.launch { dao.delete(tile) }
    }

    fun reorder(ordered: List<SpeedDialTile>) {
        viewModelScope.launch { dao.update(*ordered.mapIndexed { index, tile -> tile.copy(position = index) }.toTypedArray()) }
    }

    companion object {
        val Factory = componentsFactory { SpeedDialViewModel(it) }
    }
}
