package app.simplifymy.browser.core.intent

import android.content.Context
import app.simplifymy.browser.core.errors.PageErrors
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.feature.app.links.AppLinksInterceptor

class AppRequestInterceptor(
    private val context: Context,
    pageErrors: PageErrors,
    private val store: () -> BrowserStore,
) : RequestInterceptor by pageErrors {

    private val appLinks by lazy { AppLinksInterceptor(context, store = store()) }

    override fun onLoadRequest(
        engineSession: EngineSession,
        uri: String,
        lastUri: String?,
        hasUserGesture: Boolean,
        isSameDomain: Boolean,
        isRedirect: Boolean,
        isDirectNavigation: Boolean,
        isSubframeRequest: Boolean,
    ): RequestInterceptor.InterceptionResponse? = appLinks.onLoadRequest(
        engineSession,
        uri,
        lastUri,
        hasUserGesture,
        isSameDomain,
        isRedirect,
        isDirectNavigation,
        isSubframeRequest,
    )
}
