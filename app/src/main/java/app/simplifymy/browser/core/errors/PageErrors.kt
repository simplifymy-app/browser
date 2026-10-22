package app.simplifymy.browser.core.errors

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.ErrorType
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.lib.state.Middleware

enum class PageErrorKind { Offline, Unreachable }

data class PageError(val url: String, val kind: PageErrorKind)

private val OFFLINE_ERRORS = setOf(ErrorType.ERROR_OFFLINE, ErrorType.ERROR_NO_INTERNET)

private val UNREACHABLE_ERRORS = setOf(
    ErrorType.ERROR_UNKNOWN_HOST,
    ErrorType.ERROR_CONNECTION_REFUSED,
    ErrorType.ERROR_NET_TIMEOUT,
    ErrorType.ERROR_NET_INTERRUPT,
    ErrorType.ERROR_NET_RESET,
    ErrorType.ERROR_UNKNOWN_PROXY_HOST,
    ErrorType.ERROR_PROXY_CONNECTION_REFUSED,
)

private const val BLANK_ERROR_PAGE = "data:text/html;charset=utf-8,<!doctype html><title></title>"

class PageErrors(private val context: Context, private val store: () -> BrowserStore) : RequestInterceptor {

    private val failedLoads = mutableMapOf<String, PageError>()

    private val shown = MutableStateFlow<Map<String, PageError>>(emptyMap())

    val byTab: StateFlow<Map<String, PageError>> = shown

    val middleware: Middleware<BrowserState, BrowserAction> = { _, next, action ->
        next(action)
        if (action is ContentAction.UpdateLoadingStateAction && !action.loading) settle(action.sessionId)
    }

    override fun onErrorRequest(session: EngineSession, errorType: ErrorType, uri: String?): RequestInterceptor.ErrorResponse? {
        val kind = when {
            errorType in OFFLINE_ERRORS -> PageErrorKind.Offline
            errorType !in UNREACHABLE_ERRORS -> return null
            isOffline() -> PageErrorKind.Offline
            else -> PageErrorKind.Unreachable
        }
        val tab = store().state.tabs.find { it.engineState.engineSession === session } ?: return null
        failedLoads[tab.id] = PageError(uri.orEmpty(), kind)
        return RequestInterceptor.ErrorResponse(BLANK_ERROR_PAGE)
    }

    private fun settle(tabId: String) {
        val error = failedLoads.remove(tabId)
        val openTabs = store().state.tabs.map { it.id }.toSet()
        shown.update { errors ->
            val kept = errors.filterKeys { it in openTabs && it != tabId }
            if (error != null) kept + (tabId to error) else kept
        }
    }

    private fun isOffline(): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true
    }
}
