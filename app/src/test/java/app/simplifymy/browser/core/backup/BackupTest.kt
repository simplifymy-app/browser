package app.simplifymy.browser.core.backup

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupTest {

    private fun link(title: String, url: String) = JSONObject().put("title", title).put("url", url)

    @Test
    fun `drops links with a scheme other than http or https`() {
        val items = JSONArray()
            .put(link("Script", "javascript:alert(1)"))
            .put(link("File", "file:///data/data/app.simplifymy.browser/databases/browser.db"))
            .put(link("Intent", "intent://scan/#Intent;scheme=zxing;end"))
            .put(link("Data", "data:text/html,<script>alert(1)</script>"))
            .put(link("Padded", " javascript:alert(1)"))
            .put(link("Empty host", "https://"))
            .put(link("Secure", "https://example.com"))
            .put(link("Plain", "HTTP://example.org/page"))

        assertEquals(
            listOf("Secure" to "https://example.com", "Plain" to "HTTP://example.org/page"),
            Backup.importedLinks(items),
        )
    }

    @Test
    fun `skips broken items and keeps the rest`() {
        val items = JSONArray()
            .put("https://not-an-object.com")
            .put(42)
            .put(JSONObject.NULL)
            .put(JSONObject().put("title", "No url"))
            .put(link("Good", "https://example.com"))

        assertEquals(listOf("Good" to "https://example.com"), Backup.importedLinks(items))
    }

    @Test
    fun `keeps the first of duplicate urls`() {
        val items = JSONArray()
            .put(link("First", "https://example.com"))
            .put(link("Second", "https://example.com"))
            .put(link("Other", "https://example.org"))

        assertEquals(
            listOf("First" to "https://example.com", "Other" to "https://example.org"),
            Backup.importedLinks(items),
        )
    }

    @Test
    fun `missing array imports nothing`() {
        assertEquals(emptyList<Pair<String, String>>(), Backup.importedLinks(null))
    }
}
