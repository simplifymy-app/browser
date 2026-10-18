package app.simplifymy.browser.core.intent

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalLinksTest {

    private fun view(data: String?) = externalUrl(Intent.ACTION_VIEW, data, null, null)

    private fun send(text: String?) = externalUrl(Intent.ACTION_SEND, null, text, null)

    private fun search(query: String?) = externalUrl(Intent.ACTION_WEB_SEARCH, null, null, query)

    @Test
    fun `view opens http and https links`() {
        assertEquals(ExternalLink.Url("https://example.com"), view("https://example.com"))
        assertEquals(ExternalLink.Url("http://example.com/page?q=1#top"), view("http://example.com/page?q=1#top"))
    }

    @Test
    fun `view accepts an uppercase scheme`() {
        assertEquals(ExternalLink.Url("https://Example.com/A"), view("HTTPS://Example.com/A"))
        assertEquals(ExternalLink.Url("http://example.com"), view("Http://example.com"))
    }

    @Test
    fun `view rejects every scheme other than http and https`() {
        listOf(
            "javascript:alert(1)",
            "JavaScript:alert(1)",
            "file:///sdcard/x.html",
            "content://com.example.provider/page.html",
            "data:text/html,<script>alert(1)</script>",
            "intent://scan/#Intent;scheme=zxing;end",
            "about:config",
            "ftp://example.com",
            "market://details?id=app",
            "example.com",
            " javascript:alert(1)",
        ).forEach { assertNull(it, view(it)) }
    }

    @Test
    fun `view rejects a link without a host`() {
        listOf("https://", "http:///path", "https://?q=1", "https://#top", "https://user@", "https://:443/").forEach {
            assertNull(it, view(it))
        }
    }

    @Test
    fun `view without data opens nothing`() {
        assertNull(view(null))
        assertNull(view(""))
    }

    @Test
    fun `shared url becomes a link`() {
        assertEquals(ExternalLink.Url("https://example.org"), send("https://example.org"))
        assertEquals(ExternalLink.Url("http://example.org"), send("  example.org  "))
        assertEquals(ExternalLink.Url("https://example.org/a"), search("HTTPS://example.org/a"))
    }

    @Test
    fun `shared text with a url inside opens the first url`() {
        assertEquals(ExternalLink.Url("https://example.com"), send("Look at this https://example.com"))
        assertEquals(ExternalLink.Url("https://example.org/page"), send("see https://example.org/page, and http://other.com"))
        assertEquals(ExternalLink.Url("https://example.org/a"), send("Article (https://example.org/a)."))
    }

    @Test
    fun `shared text with a rejected scheme opens nothing`() {
        assertNull(send("javascript:alert(1)"))
        assertNull(send("file:///sdcard/x.html"))
    }

    @Test
    fun `plain text becomes a search`() {
        assertEquals(ExternalLink.Search("gecko"), search("gecko"))
        assertEquals(ExternalLink.Search("how does gecko render"), send("  how does gecko render "))
        assertEquals(ExternalLink.Search("see https:// later"), send("see https:// later"))
    }

    @Test
    fun `send prefers the text and falls back to the query`() {
        assertEquals(ExternalLink.Search("text"), externalUrl(Intent.ACTION_SEND, null, "text", "query"))
        assertEquals(ExternalLink.Search("query"), externalUrl(Intent.ACTION_WEB_SEARCH, null, " ", "query"))
    }

    @Test
    fun `blank input opens nothing`() {
        assertNull(send(null))
        assertNull(send(""))
        assertNull(send("   \n "))
        assertNull(search(null))
        assertNull(search("  "))
    }

    @Test
    fun `other actions open nothing`() {
        assertNull(externalUrl(Intent.ACTION_MAIN, "https://example.com", "https://example.com", null))
        assertNull(externalUrl(null, "https://example.com", null, null))
    }
}
