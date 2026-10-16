package app.simplifymy.browser.feature.browser

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.R
import app.simplifymy.browser.ui.component.BitmapImage
import app.simplifymy.browser.ui.component.BrowserIcons
import app.simplifymy.browser.ui.component.IconAction
import app.simplifymy.browser.ui.component.MenuDivider
import app.simplifymy.browser.ui.component.MenuRow
import app.simplifymy.browser.ui.component.QuickAction
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Radius
import app.simplifymy.browser.ui.theme.Sizes
import app.simplifymy.browser.ui.theme.Space

class MenuActions(
    val newTab: (private: Boolean) -> Unit,
    val open: (Overlay) -> Unit,
    val forward: () -> Unit,
    val savePage: () -> Unit,
    val mobileView: (Boolean) -> Unit,
    val toggleBookmark: () -> Unit,
    val share: () -> Unit,
    val reload: () -> Unit,
    val exit: () -> Unit,
)

@Composable
fun BrowserMenu(state: BrowserUiState, extensionActions: () -> List<ExtensionAction>, actions: MenuActions) {
    var open by remember { mutableStateOf(false) }
    val run = { action: () -> Unit -> { open = false; action() } }
    val page = state.url.isNotEmpty()
    Box {
        IconAction(BrowserIcons.More, stringResource(R.string.action_more), onClick = { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = Radius.card,
            containerColor = BrowserTheme.colors.surfaceAlt,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.width(288.dp),
        ) {
            MenuRow(stringResource(R.string.action_new_tab), run { actions.newTab(false) }, icon = BrowserIcons.Plus)
            MenuRow(stringResource(R.string.action_new_private_tab), run { actions.newTab(true) }, icon = BrowserIcons.Private)
            MenuRow(stringResource(R.string.action_extensions), run { actions.open(Overlay.Extensions) }, icon = BrowserIcons.Extensions)
            extensionActions().forEach { extension ->
                MenuRow(
                    label = extension.title,
                    onClick = run { extension.action.onClick() },
                    leading = { ExtensionIcon(extension) },
                    value = extension.action.badgeText?.takeIf { it.isNotEmpty() },
                )
            }
            MenuDivider()
            MenuRow(
                stringResource(R.string.mobile_view),
                run { actions.mobileView(state.desktopMode) },
                icon = if (state.desktopMode) BrowserIcons.Desktop else BrowserIcons.Mobile,
                enabled = page,
                checked = !state.desktopMode,
            )
            MenuDivider()
            MenuRow(stringResource(R.string.action_bookmarks), run { actions.open(Overlay.Bookmarks) }, icon = BrowserIcons.Bookmark)
            MenuRow(stringResource(R.string.action_history), run { actions.open(Overlay.History) }, icon = BrowserIcons.History)
            MenuRow(stringResource(R.string.action_downloads), run { actions.open(Overlay.Downloads) }, icon = BrowserIcons.Download)
            MenuDivider()
            MenuRow(stringResource(R.string.action_settings), run { actions.open(Overlay.Settings) }, icon = BrowserIcons.Settings)
            MenuRow(stringResource(R.string.action_exit), run(actions.exit), icon = BrowserIcons.Exit)
            MenuDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.toolbarInset, vertical = Space.base),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QuickAction(BrowserIcons.Forward, stringResource(R.string.action_forward), run(actions.forward), enabled = state.canGoForward)
                QuickAction(BrowserIcons.SavePage, stringResource(R.string.action_save_page), run(actions.savePage), enabled = page)
                QuickAction(
                    if (state.bookmarked) BrowserIcons.BookmarkFilled else BrowserIcons.AddBookmark,
                    stringResource(if (state.bookmarked) R.string.action_remove_bookmark else R.string.action_add_bookmark),
                    run(actions.toggleBookmark),
                    enabled = page,
                )
                QuickAction(BrowserIcons.Share, stringResource(R.string.action_share), run(actions.share), enabled = page)
                QuickAction(BrowserIcons.Reload, stringResource(R.string.action_reload), run(actions.reload), enabled = page)
            }
        }
    }
}

@Composable
private fun ExtensionIcon(extension: ExtensionAction) {
    val px = with(LocalDensity.current) { Sizes.icon.roundToPx() }
    val icon by produceState<Bitmap?>(null, extension.id) { value = extension.action.loadIcon?.invoke(px) }
    Box(Modifier.size(Sizes.icon)) {
        icon?.let { BitmapImage(it, Modifier.size(Sizes.icon)) }
    }
}
