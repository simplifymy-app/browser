package app.simplifymy.browser.core.search

import mozilla.components.browser.state.action.SearchAction
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.availableSearchEngines
import mozilla.components.browser.state.state.searchEngines
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.search.ext.buildSearchUrl
import mozilla.components.support.ktx.kotlin.isUrl

val ADDITIONAL_SEARCH_ENGINES = listOf("ecosia", "qwant", "youtube", "reddit")

val SearchState.choices: List<SearchEngine>
    get() = (searchEngines + availableSearchEngines).distinctBy { it.id }.filterNot { it.id.startsWith("yandex") }

val SearchState.selectedId: String?
    get() = selectedOrDefaultSearchEngine?.id

private val SCHEME_SEPARATOR_STOPS = charArrayOf('/', '?', '#')

fun typedUrl(input: String): String? {
    val text = input.trim().replace("\n", "")
    if (text.isEmpty() || !text.isUrl()) return null
    if (text.startsWith("http://") || text.startsWith("https://")) return text
    val separator = text.indexOf(':').takeIf { it > 0 && text.indexOfAny(SCHEME_SEPARATOR_STOPS).let { stop -> stop == -1 || it < stop } }
    return if (separator == null) "http://$text" else text.substring(0, separator).lowercase() + text.substring(separator)
}

fun SearchState.urlFor(input: String): String? {
    val text = input.trim()
    if (text.isEmpty()) return null
    return typedUrl(text) ?: selectedOrDefaultSearchEngine?.buildSearchUrl(text)
}

fun BrowserStore.selectSearchEngine(engine: SearchEngine) {
    if (engine.type == SearchEngine.Type.BUNDLED_ADDITIONAL && state.search.additionalSearchEngines.none { it.id == engine.id }) {
        dispatch(SearchAction.AddAdditionalSearchEngineAction(engine.id))
    }
    dispatch(SearchAction.SelectSearchEngineAction(engine.id, engine.name))
}
