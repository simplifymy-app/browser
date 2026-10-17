package app.simplifymy.browser.core.backup

import android.content.ContentResolver
import android.net.Uri
import app.simplifymy.browser.core.database.SpeedDialDao
import app.simplifymy.browser.core.settings.SettingsRepository
import app.simplifymy.browser.core.settings.SpeedDialStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import mozilla.components.concept.storage.BookmarksStorage
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val WEB_URL = Regex("^https?://\\S", RegexOption.IGNORE_CASE)

data class BackupCounts(val tiles: Int, val bookmarks: Int)

class Backup(
    private val resolver: ContentResolver,
    private val speedDial: SpeedDialDao,
    private val bookmarks: BookmarksStorage,
    private val settings: SettingsRepository,
) {

    suspend fun export(uri: Uri): BackupCounts {
        val current = settings.settings.first()
        val tiles = speedDial.tiles().first()
        val marks = mobileBookmarks()
        val json = JSONObject()
            .put(VERSION, FORMAT_VERSION)
            .put(SPEED_DIAL, JSONObject()
                .put(COLUMNS, current.speedDialColumns)
                .put(TILE_STYLE, current.speedDialStyle.name)
                .put(TILE_LIST, links(tiles.map { it.title to it.url })))
            .put(BOOKMARKS, links(marks))
        withContext(Dispatchers.IO) {
            (resolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open $uri")).bufferedWriter().use { it.write(json.toString(2)) }
        }
        return BackupCounts(tiles.size, marks.size)
    }

    suspend fun import(uri: Uri): BackupCounts {
        val json = withContext(Dispatchers.IO) {
            JSONObject((resolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")).bufferedReader().use { it.readText() })
        }
        val dial = json.optJSONObject(SPEED_DIAL)
        dial?.optInt(COLUMNS, 0)?.takeIf { it > 0 }?.let { settings.setSpeedDialColumns(it) }
        dial?.optString(TILE_STYLE)?.let { name -> SpeedDialStyle.entries.find { it.name == name } }?.let { settings.setSpeedDialStyle(it) }

        val knownTiles = speedDial.tiles().first().map { it.url }.toSet()
        val newTiles = importedLinks(dial?.optJSONArray(TILE_LIST)).filter { (_, url) -> url !in knownTiles }
        newTiles.forEach { (title, url) -> speedDial.add(title, url) }

        val knownMarks = mobileBookmarks().map { it.second }.toSet()
        val newMarks = importedLinks(json.optJSONArray(BOOKMARKS)).filter { (_, url) -> url !in knownMarks }
        newMarks.forEach { (title, url) -> bookmarks.addItem(MOBILE_BOOKMARKS, url, title, null) }

        return BackupCounts(newTiles.size, newMarks.size)
    }

    private suspend fun mobileBookmarks() = bookmarks.getTree(MOBILE_BOOKMARKS).getOrNull()?.children.orEmpty()
        .mapNotNull { node -> node.url?.let { node.title.orEmpty() to it } }

    private fun links(items: List<Pair<String, String>>) = JSONArray().apply {
        items.forEach { (title, url) -> put(JSONObject().put(TITLE, title).put(URL, url)) }
    }

    internal companion object {
        fun importedLinks(array: JSONArray?): List<Pair<String, String>> = if (array == null) emptyList() else (0 until array.length())
            .mapNotNull { array.optJSONObject(it) }
            .map { item -> item.optString(TITLE) to item.optString(URL) }
            .filter { (_, url) -> WEB_URL.containsMatchIn(url) }
            .distinctBy { it.second }

        const val FORMAT_VERSION = 1
        const val VERSION = "version"
        const val SPEED_DIAL = "speedDial"
        const val COLUMNS = "columns"
        const val TILE_STYLE = "tiles"
        const val TILE_LIST = "items"
        const val BOOKMARKS = "bookmarks"
        const val TITLE = "title"
        const val URL = "url"
    }
}
