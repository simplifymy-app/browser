package app.simplifymy.browser.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Radius
import app.simplifymy.browser.ui.theme.Space
import kotlinx.coroutines.delay

private const val SNACKBAR_MS = 3_000L

@Composable
fun Snackbar(text: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(text) {
        delay(SNACKBAR_MS)
        onDone()
    }
    Box(
        modifier
            .fillMaxWidth()
            .padding(Space.toolbarInset)
            .clip(Radius.card)
            .background(BrowserTheme.colors.surfaceAlt)
            .padding(horizontal = Space.screen, vertical = Space.toolbarInset),
    ) {
        Text(text, style = BrowserTheme.type.body, color = BrowserTheme.colors.text)
    }
}

@Preview
@Composable
private fun SnackbarPreview() {
    BrowserTheme { Snackbar("uBlock Origin was added", {}) }
}
