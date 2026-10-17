package app.simplifymy.browser.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.R
import app.simplifymy.browser.ui.component.BrowserIcons
import app.simplifymy.browser.ui.component.Favicon
import app.simplifymy.browser.ui.component.IconAction
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Radius
import app.simplifymy.browser.ui.theme.Space
import app.simplifymy.browser.ui.theme.States

@Composable
fun TabStrip(
    tabs: List<TabItem>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val selectedIndex = tabs.indexOfFirst { it.id == selectedId }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
    }
    Row(
        modifier = modifier.background(BrowserTheme.colors.background).padding(horizontal = Space.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Space.gutter),
        ) {
            items(tabs, key = { it.id }) { tab ->
                TabChip(tab, tab.id == selectedId, { onSelect(tab.id) }, { onClose(tab.id) })
            }
        }
        IconAction(BrowserIcons.Plus, stringResource(R.string.action_new_tab), onNewTab)
    }
}

@Composable
fun tabTitle(tab: TabItem): String = when {
    tab.speedDial -> stringResource(R.string.speed_dial)
    tab.title.isNotEmpty() -> tab.title
    else -> tab.url
}

@Composable
private fun TabChip(tab: TabItem, selected: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    val colors = BrowserTheme.colors
    Row(
        modifier = Modifier
            .width(168.dp)
            .height(36.dp)
            .clip(Radius.control)
            .background(if (selected) colors.accent.copy(alpha = States.SELECTED) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = Space.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.base),
    ) {
        Favicon(tab.url.takeUnless { tab.speedDial }.orEmpty(), bitmap = tab.icon, private = tab.private)
        Text(
            tabTitle(tab),
            style = BrowserTheme.type.label,
            color = if (selected) colors.text else colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            BrowserIcons.Close,
            contentDescription = stringResource(R.string.action_close_tab),
            tint = colors.textMuted,
            modifier = Modifier
                .size(36.dp)
                .clip(Radius.control)
                .clickable(onClick = onClose)
                .padding(11.dp),
        )
    }
}

@Preview
@Composable
private fun TabStripPreview() {
    BrowserTheme {
        TabStrip(
            tabs = listOf(
                TabItem("a", "Example Domain", "https://example.com", null, false),
                TabItem("b", "", "about:blank", null, false),
            ),
            selectedId = "b",
            onSelect = {},
            onClose = {},
            onNewTab = {},
        )
    }
}
