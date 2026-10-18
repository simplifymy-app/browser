package app.simplifymy.browser.core.intent

import android.content.Intent
import app.simplifymy.browser.core.search.typedUrl

sealed interface ExternalLink {
    data class Url(val url: String) : ExternalLink
    data class Search(val text: String) : ExternalLink
}

private val WEB_SCHEME = Regex("^https?://", RegexOption.IGNORE_CASE)
private val EMBEDDED_URL = Regex("https?://[^\\s<>\"]+", RegexOption.IGNORE_CASE)
private const val TRAILING_PUNCTUATION = ".,;:!?)]}'\""

fun externalUrl(action: String?, data: String?, text: String?, query: String?): ExternalLink? = when (action) {
    Intent.ACTION_VIEW -> data?.let(::webUrl)?.let(ExternalLink::Url)
    Intent.ACTION_SEND, Intent.ACTION_WEB_SEARCH -> sharedLink(text?.takeIf { it.isNotBlank() } ?: query)
    else -> null
}

private fun sharedLink(input: String?): ExternalLink? {
    val text = input?.trim().orEmpty()
    if (text.isEmpty()) return null
    typedUrl(text)?.let { return webUrl(it)?.let(ExternalLink::Url) }
    EMBEDDED_URL.find(text)?.value?.trimEnd(*TRAILING_PUNCTUATION.toCharArray())?.let(::webUrl)?.let { return ExternalLink.Url(it) }
    return ExternalLink.Search(text)
}

internal fun webUrl(input: String): String? {
    val url = input.trim()
    val scheme = WEB_SCHEME.find(url) ?: return null
    val host = url.substring(scheme.range.last + 1)
        .substringBefore('/').substringBefore('?').substringBefore('#')
        .substringAfterLast('@')
        .let { if (it.startsWith("[")) it.substringBefore(']').drop(1) else it.substringBefore(':') }
    if (host.isBlank()) return null
    return scheme.value.lowercase() + url.substring(scheme.range.last + 1)
}
