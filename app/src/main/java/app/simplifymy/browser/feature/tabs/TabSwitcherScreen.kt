package app.simplifymy.browser.feature.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.R
import app.simplifymy.browser.feature.browser.TabItem
import app.simplifymy.browser.feature.browser.tabTitle
import app.simplifymy.browser.ui.component.BrowserIcons
import app.simplifymy.browser.ui.component.EmptyState
import app.simplifymy.browser.ui.component.Favicon
import app.simplifymy.browser.ui.component.HeaderScreen
import app.simplifymy.browser.ui.component.IconAction
import app.simplifymy.browser.ui.component.Screen
import app.simplifymy.browser.ui.component.SegmentedPicker
import app.simplifymy.browser.ui.component.TabThumbnail
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Space

@Composable
fun TabSwitcherScreen(
    normalTabs: List<TabItem>,
    privateTabs: List<TabItem>,
    selectedId: String?,
    startPrivate: Boolean,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onNewTab: (private: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var private by remember { mutableStateOf(startPrivate) }
    val tabs = if (private) privateTabs else normalTabs
    val oneHanded = BrowserTheme.oneHanded
    val grid = rememberLazyGridState(initialFirstVisibleItemIndex = tabs.indexOfFirst { it.id == selectedId }.coerceAtLeast(0))
    BackHandler(onBack = onDismiss)
    val picker = @Composable { modifier: Modifier ->
        SegmentedPicker(
            options = listOf(false, true),
            selected = private,
            onSelect = { private = it },
            modifier = modifier,
        ) {
            val count = if (it) privateTabs.size else normalTabs.size
            "${stringResource(if (it) R.string.private_label else R.string.action_tabs)} $count"
        }
    }
    val newTab = @Composable { IconAction(BrowserIcons.Plus, stringResource(R.string.action_new_tab), { onNewTab(private) }) }
    val content = @Composable { modifier: Modifier ->
        if (tabs.isEmpty()) EmptyState(stringResource(R.string.tabs_empty))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = modifier,
            state = grid,
            contentPadding = PaddingValues(Space.toolbarInset),
            horizontalArrangement = Arrangement.spacedBy(Space.toolbarInset),
            verticalArrangement = Arrangement.spacedBy(Space.rowGap, if (oneHanded) Alignment.Bottom else Alignment.Top),
        ) {
            items(tabs, key = { it.id }) { tab ->
                TabCard(tab, tab.id == selectedId, { onSelect(tab.id) }, { onClose(tab.id) })
            }
        }
    }
    if (oneHanded) {
        HeaderScreen(
            title = stringResource(if (private) R.string.private_label else R.string.action_tabs),
            onBack = onDismiss,
            modifier = Modifier.fillMaxSize(),
            titleAtBottom = true,
            actions = { newTab() },
        ) {
            content(Modifier.weight(1f))
            picker(Modifier.fillMaxWidth().padding(horizontal = Space.screen, vertical = Space.base))
        }
        return
    }
    Screen(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Space.gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(BrowserIcons.Close, stringResource(R.string.action_close), onDismiss)
            picker(Modifier.weight(1f).padding(horizontal = Space.base))
            newTab()
        }
        content(Modifier.weight(1f))
    }
}

@Composable
private fun TabCard(tab: TabItem, selected: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    val colors = BrowserTheme.colors
    val shape = RoundedCornerShape(BrowserTheme.corners.radius)
    Column(Modifier.clickable(onClick = onClick)) {
        TabThumbnail(
            tabId = tab.id,
            private = tab.private,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clip(shape)
                .border(2.dp, if (selected) colors.accent else Color.Transparent, shape),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Space.gutter)) {
            Favicon(tab.url.takeUnless { tab.speedDial }.orEmpty(), bitmap = tab.icon, private = tab.private)
            Text(
                tabTitle(tab),
                style = BrowserTheme.type.label,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = Space.base),
            )
            IconAction(BrowserIcons.Close, stringResource(R.string.action_close_tab), onClose, tint = colors.textMuted)
        }
    }
}

@Preview
@Composable
private fun TabSwitcherPreview() {
    BrowserTheme {
        TabSwitcherScreen(
            normalTabs = listOf(
                TabItem("a", "Example Domain", "https://example.com", null, false),
                TabItem("b", "", "about:blank", null, false),
            ),
            privateTabs = emptyList(),
            selectedId = "a",
            startPrivate = false,
            onSelect = {},
            onClose = {},
            onNewTab = {},
            onDismiss = {},
        )
    }
}
