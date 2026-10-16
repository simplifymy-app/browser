package app.simplifymy.browser.core.search

import mozilla.components.browser.state.action.SearchAction
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.availableSearchEngines
import mozilla.components.browser.state.state.searchEngines
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.state.store.BrowserStore

val ADDITIONAL_SEARCH_ENGINES = listOf("ecosia", "qwant", "youtube", "reddit")

val SearchState.choices: List<SearchEngine>
    get() = (searchEngines + availableSearchEngines).distinctBy { it.id }.filterNot { it.id.startsWith("yandex") }

val SearchState.selectedId: String?
    get() = selectedOrDefaultSearchEngine?.id

fun BrowserStore.selectSearchEngine(engine: SearchEngine) {
    if (engine.type == SearchEngine.Type.BUNDLED_ADDITIONAL && state.search.additionalSearchEngines.none { it.id == engine.id }) {
        dispatch(SearchAction.AddAdditionalSearchEngineAction(engine.id))
    }
    dispatch(SearchAction.SelectSearchEngineAction(engine.id, engine.name))
}
