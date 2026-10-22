package app.simplifymy.browser.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.R
import app.simplifymy.browser.core.errors.PageError
import app.simplifymy.browser.core.errors.PageErrorKind
import app.simplifymy.browser.ui.component.BrowserIcons
import app.simplifymy.browser.ui.component.TextButton
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Space
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl

private val ERROR_ICON_SIZE = 32.dp

@Composable
fun PageErrorView(error: PageError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrowserTheme.colors
    val host = error.url.tryGetHostFromUrl()
    val offline = error.kind == PageErrorKind.Offline
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            .padding(horizontal = Space.screen, vertical = Space.section),
        verticalArrangement = if (BrowserTheme.oneHanded) Arrangement.Bottom else Arrangement.Center,
    ) {
        Icon(
            if (offline) BrowserIcons.Offline else BrowserIcons.Unreachable,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(ERROR_ICON_SIZE),
        )
        Spacer(Modifier.height(Space.rowGap))
        Text(
            stringResource(if (offline) R.string.page_offline_title else R.string.page_unreachable_title),
            style = BrowserTheme.type.headline,
            color = colors.text,
        )
        Spacer(Modifier.height(Space.base))
        Text(
            if (offline) stringResource(R.string.page_offline_text) else stringResource(R.string.page_unreachable_text, host),
            style = BrowserTheme.type.body,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(Space.base))
        Text(error.url, style = BrowserTheme.type.meta, color = colors.textFaint, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(Space.section))
        TextButton(stringResource(R.string.page_retry), onRetry, primary = true)
    }
}

@Preview
@Composable
private fun PageErrorViewPreview() {
    BrowserTheme {
        PageErrorView(PageError("https://example.invalid/path", PageErrorKind.Unreachable), onRetry = {})
    }
}

@Preview
@Composable
private fun OfflinePageErrorViewPreview() {
    BrowserTheme {
        PageErrorView(PageError("https://simplifymy.app/", PageErrorKind.Offline), onRetry = {})
    }
}
