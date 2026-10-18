package app.simplifymy.browser.feature.browser

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.simplifymy.browser.R
import app.simplifymy.browser.ui.component.BrowserDialog
import app.simplifymy.browser.ui.theme.BrowserTheme

@Composable
fun DefaultBrowserOffer(onMakeDefault: () -> Unit, onDismiss: () -> Unit) {
    BrowserDialog(
        title = stringResource(R.string.default_browser_offer_title),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.default_browser_make),
        onConfirm = onMakeDefault,
        dismissLabel = stringResource(R.string.default_browser_not_now),
    ) {
        Text(stringResource(R.string.default_browser_offer_body), style = BrowserTheme.type.body, color = BrowserTheme.colors.textMuted)
    }
}

@Preview
@Composable
private fun DefaultBrowserOfferPreview() {
    BrowserTheme {
        DefaultBrowserOffer(onMakeDefault = {}, onDismiss = {})
    }
}
