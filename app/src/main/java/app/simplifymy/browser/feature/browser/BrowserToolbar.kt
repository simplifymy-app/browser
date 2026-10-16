package app.simplifymy.browser.feature.browser

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.R
import app.simplifymy.browser.core.settings.ToolbarPosition
import app.simplifymy.browser.ui.component.BrowserIcons
import app.simplifymy.browser.ui.component.IconAction
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Motion
import app.simplifymy.browser.ui.theme.Radius
import app.simplifymy.browser.ui.theme.Space
import app.simplifymy.browser.ui.theme.enterTween
import mozilla.components.browser.state.search.SearchEngine

@Composable
fun BrowserToolbar(
    state: BrowserUiState,
    progress: () -> Float,
    onSubmit: (String) -> Unit,
    onStop: () -> Unit,
    onPanel: () -> Unit,
    onTabs: () -> Unit,
    menu: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    onEditingChange: (Boolean) -> Unit = {},
    completion: Completion? = null,
    onQueryChange: (String) -> Unit = {},
    onSelectEngine: (SearchEngine) -> Unit = {},
) {
    val top = state.position == ToolbarPosition.Top
    Column(modifier.background(BrowserTheme.colors.background)) {
        if (!top) ProgressLine(progress)
        Row(
            modifier = Modifier.padding(horizontal = Space.gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AddressBar(
                url = state.url,
                searchIcon = state.searchIcon,
                editing = editing,
                onEditingChange = onEditingChange,
                onSubmit = onSubmit,
                completion = completion,
                onQueryChange = onQueryChange,
                modifier = Modifier.weight(1f).padding(start = Space.gutter),
                private = state.private,
                loading = state.loading,
                onStop = onStop,
                security = state.security,
                issuer = state.issuer,
                searchEngines = state.searchEngines,
                searchEngineId = state.searchEngineId,
                onSelectEngine = onSelectEngine,
            )
            IconAction(BrowserIcons.Panel, stringResource(R.string.action_panel), onPanel)
            TabCounter(state.tabs.size, onTabs)
            menu()
        }
        if (top) ProgressLine(progress)
    }
}

@Composable
private fun TabCounter(count: Int, onClick: () -> Unit) {
    val colors = BrowserTheme.colors
    val label = stringResource(R.string.action_tabs)
    Box(
        modifier = Modifier
            .size(Space.hitTarget)
            .clip(Radius.control)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 22.dp, minHeight = 22.dp)
                .border(1.5.dp, colors.text, Radius.control)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (count > 99) "∞" else count.toString(), style = BrowserTheme.type.meta, color = colors.text)
        }
    }
}

@Composable
private fun ProgressLine(progress: () -> Float) {
    val target = progress()
    val fraction by animateFloatAsState(target, enterTween(Motion.STATE), label = "progress")
    val track = BrowserTheme.colors.hairline
    val accent = BrowserTheme.colors.accent
    Canvas(Modifier.fillMaxWidth().height(2.dp)) {
        if (target > 0f) drawRect(track)
        drawRect(accent, size = size.copy(width = size.width * fraction))
    }
}

@Preview
@Composable
private fun BrowserToolbarPreview() {
    BrowserTheme {
        BrowserToolbar(
            state = BrowserUiState(url = "https://example.com", loading = true, selectedTabId = "a"),
            progress = { 0.4f },
            onSubmit = {},
            onStop = {},
            onPanel = {},
            onTabs = {},
            menu = { IconAction(BrowserIcons.More, "More", {}) },
        )
    }
}
