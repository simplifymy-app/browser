package app.simplifymy.browser.core.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultBrowserTest {

    @Test
    fun `offers only when not offered, not default, not opened from a link and no other dialog`() {
        val flags = listOf(false, true)
        for (offered in flags) for (isDefault in flags) for (openedFromLink in flags) for (otherDialogVisible in flags) {
            val expected = !offered && !isDefault && !openedFromLink && !otherDialogVisible
            assertEquals(
                "offered=$offered isDefault=$isDefault openedFromLink=$openedFromLink otherDialogVisible=$otherDialogVisible",
                expected,
                shouldOfferDefaultBrowser(offered, isDefault, openedFromLink, otherDialogVisible),
            )
        }
    }

    @Test
    fun `first normal launch of a browser that is not the default gets the offer`() {
        assertEquals(true, shouldOfferDefaultBrowser(offered = false, isDefault = false, openedFromLink = false, otherDialogVisible = false))
    }
}
