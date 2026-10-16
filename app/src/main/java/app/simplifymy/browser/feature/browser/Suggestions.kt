package app.simplifymy.browser.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.ui.component.BrowserIcons
import app.simplifymy.browser.ui.component.Favicon
import app.simplifymy.browser.ui.component.ListRow
import app.simplifymy.browser.ui.component.MenuRow
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Radius
import app.simplifymy.browser.ui.theme.Sizes
import app.simplifymy.browser.ui.theme.Space

private val SUGGESTION_ICON = 24.dp

sealed interface Suggestion {
    data class Page(val title: String, val url: String) : Suggestion
    data class Search(val text: String) : Suggestion
}

data class Completion(val input: String, val text: String)

@Composable
fun SuggestionList(
    suggestions: List<Suggestion>,
    nearBottom: Boolean,
    onPick: (Suggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrowserTheme.colors
    LazyColumn(
        modifier = modifier
            .padding(horizontal = Space.base, vertical = Space.gutter)
            .clip(Radius.card)
            .background(colors.surfaceAlt)
            .border(Sizes.hairline, colors.hairline, Radius.card),
        reverseLayout = nearBottom,
    ) {
        items(suggestions) { suggestion ->
            when (suggestion) {
                is Suggestion.Page -> ListRow(
                    title = suggestion.title.ifEmpty { suggestion.url },
                    subtitle = suggestion.url,
                    onClick = { onPick(suggestion) },
                    leading = { Favicon(suggestion.url, size = SUGGESTION_ICON) },
                )
                is Suggestion.Search -> MenuRow(suggestion.text, { onPick(suggestion) }, icon = BrowserIcons.Search)
            }
        }
    }
}

@Preview
@Composable
private fun SuggestionListPreview() {
    BrowserTheme {
        SuggestionList(
            suggestions = listOf(
                Suggestion.Page("Example Domain", "https://example.com/"),
                Suggestion.Search("example search"),
                Suggestion.Search("example domain names"),
            ),
            nearBottom = false,
            onPick = {},
        )
    }
}
