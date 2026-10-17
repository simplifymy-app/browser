package app.simplifymy.browser.core.addons

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Request
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

data class AmoAddon(
    val guid: String,
    val name: String,
    val summary: String,
    val iconUrl: String,
    val users: Int,
    val downloadUrl: String,
)

class Amo(private val client: Client) {

    suspend fun search(query: String): List<AmoAddon> {
        val params = mutableMapOf("app" to "android", "type" to "extension", "sort" to "users", "page_size" to "30")
        if (query.isBlank()) params["promoted"] = "recommended" else params["q"] = query.trim()
        val json = get(api("addons/search/", params))
        return json.getJSONArray("results").let { results -> List(results.length()) { results.getJSONObject(it).toAddon() } }
    }

    suspend fun bySlug(slug: String): AmoAddon = get(api("addons/addon/$slug/", mapOf("app" to "android"))).toAddon()

    suspend fun image(url: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching { client.fetch(Request(url)).body.useStream(BitmapFactory::decodeStream) }.getOrNull()
    }

    private suspend fun get(url: String) = withContext(Dispatchers.IO) {
        val body = client.fetch(Request(url)).use { response ->
            if (response.status !in 200..299) throw IOException("HTTP ${response.status} for $url")
            response.body.string()
        }
        JSONObject(body)
    }

    private fun api(path: String, params: Map<String, String>) = Uri.parse("$BASE/api/v5/$path").buildUpon()
        .apply { (params + ("lang" to Locale.getDefault().toLanguageTag())).forEach { (k, v) -> appendQueryParameter(k, v) } }
        .build()
        .toString()

    private fun JSONObject.toAddon(): AmoAddon {
        val locale = optString("default_locale")
        return AmoAddon(
            guid = getString("guid"),
            name = localized("name", locale),
            summary = localized("summary", locale),
            iconUrl = optString("icon_url"),
            users = optInt("average_daily_users"),
            downloadUrl = getJSONObject("current_version").getJSONObject("file").getString("url"),
        )
    }

    private fun JSONObject.text(key: String) = if (isNull(key)) "" else optString(key)

    private fun JSONObject.localized(key: String, locale: String): String {
        val value = optJSONObject(key) ?: return text(key)
        val keys = sequenceOf(Locale.getDefault().toLanguageTag(), Locale.getDefault().language, locale) + value.keys().asSequence().filterNot { it == "_default" }
        return keys.map { value.text(it) }.firstOrNull { it.isNotEmpty() }.orEmpty()
    }

    companion object {
        private const val BASE = "https://addons.mozilla.org"
        private val SLUG = Regex("""addons\.mozilla\.org/(?:[\w-]+/)?(?:android|firefox)/addon/([^/?#]+)""")

        fun slugOf(url: String): String? = SLUG.find(url)?.groupValues?.get(1)
    }
}
