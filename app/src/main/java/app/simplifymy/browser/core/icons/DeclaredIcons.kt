package app.simplifymy.browser.core.icons

import android.net.Uri
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.browser.icons.IconRequest.Resource
import mozilla.components.concept.engine.manifest.Size
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Response
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private val LINK_TAG = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
private val ATTRIBUTE = Regex("([a-zA-Z-]+)\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)")
private const val HEAD_END = "</head>"
private const val MAX_HEAD_CHARS = 256 * 1024
private val RETRY_FAILED_AFTER_MS = TimeUnit.HOURS.toMillis(1)

class DeclaredIcons(private val client: Client) {

    private val known = ConcurrentHashMap<String, List<Resource>>()

    private val failedAt = ConcurrentHashMap<String, Long>()

    suspend fun of(pageUrl: String): List<Resource>? {
        val origin = Uri.parse(pageUrl).let { "${it.scheme}://${it.authority}" }
        known[origin]?.let { return it }
        failedAt[origin]?.let { if (SystemClock.elapsedRealtime() - it < RETRY_FAILED_AFTER_MS) return null }
        val icons = withContext(Dispatchers.IO) {
            runCatching {
                client.fetch(Request(pageUrl)).use { response ->
                    if (response.status in 200..299) parse(readHead(response.body), response.url) else null
                }
            }.getOrElse { if (it is CancellationException) throw it else null }
        }
        if (icons == null) {
            failedAt[origin] = SystemClock.elapsedRealtime()
        } else {
            known[origin] = icons
            failedAt.remove(origin)
        }
        return icons
    }

    private fun readHead(body: Response.Body): String = body.useBufferedReader { reader ->
        val head = StringBuilder()
        while (head.length < MAX_HEAD_CHARS) {
            val line = reader.readLine() ?: break
            head.append(line).append('\n')
            if (line.contains(HEAD_END, ignoreCase = true)) break
        }
        head.toString()
    }

    private fun parse(html: String, baseUrl: String): List<Resource> = LINK_TAG.findAll(html).mapNotNull { tag ->
        val attributes = ATTRIBUTE.findAll(tag.value).associate { it.groupValues[1].lowercase() to it.groupValues[2].trim('"', '\'') }
        val type = attributes["rel"]?.lowercase()?.split(' ')?.let { rel ->
            when {
                rel.any { it.startsWith("apple-touch-icon") } -> Resource.Type.APPLE_TOUCH_ICON
                "icon" in rel -> Resource.Type.FAVICON
                else -> null
            }
        } ?: return@mapNotNull null
        val href = attributes["href"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Resource(
            url = resolve(baseUrl, href),
            type = type,
            sizes = attributes["sizes"]?.split(' ')?.mapNotNull { runCatching { Size.parse(it) }.getOrNull() }.orEmpty(),
            mimeType = attributes["type"],
        )
    }.toList()

    private fun resolve(base: String, href: String) = URI(base).resolve(href.replace(" ", "%20")).toString()
}
