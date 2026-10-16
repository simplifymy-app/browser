package app.simplifymy.browser.feature.addons

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simplifymy.browser.R
import app.simplifymy.browser.core.addons.AmoAddon
import app.simplifymy.browser.ui.component.BitmapImage
import app.simplifymy.browser.ui.component.BrowserIcons
import app.simplifymy.browser.ui.component.EmptyState
import app.simplifymy.browser.ui.component.IconAction
import app.simplifymy.browser.ui.component.InputField
import app.simplifymy.browser.ui.component.ListRow
import app.simplifymy.browser.ui.component.MenuRow
import app.simplifymy.browser.ui.component.RemoteImage
import app.simplifymy.browser.ui.component.Screen
import app.simplifymy.browser.ui.component.ScreenHeader
import app.simplifymy.browser.ui.component.SectionHeader
import app.simplifymy.browser.ui.component.Toggle
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Radius
import app.simplifymy.browser.ui.theme.Space
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.ui.translateName
import mozilla.components.feature.addons.ui.translateSummary
import java.text.NumberFormat

class AddonActions(
    val query: (String) -> Unit,
    val submit: () -> Unit,
    val install: (AmoAddon) -> Unit,
    val setEnabled: (Addon, Boolean) -> Unit,
    val openOptions: (Addon) -> Unit,
    val uninstall: (Addon) -> Unit,
)

@Composable
fun AddonsScreen(viewModel: AddonsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.open() }
    BackHandler(onBack = onBack)
    AddonsContent(
        state = state,
        onBack = onBack,
        actions = AddonActions(
            query = viewModel::setQuery,
            submit = viewModel::submit,
            install = viewModel::install,
            setEnabled = viewModel::setEnabled,
            openOptions = { viewModel.openOptions(it); onBack() },
            uninstall = viewModel::uninstall,
        ),
    )
}

@Composable
fun AddonsContent(state: AddonsState, onBack: () -> Unit, actions: AddonActions) {
    val searching = state.query.isNotBlank()
    Screen(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.action_extensions), onBack)
        InputField(
            value = state.query,
            onValueChange = actions.query,
            placeholder = stringResource(R.string.addons_search_hint),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.screen),
            keyboardType = KeyboardType.Uri,
            onSubmit = actions.submit,
        )
        LazyColumn(Modifier.weight(1f).navigationBarsPadding()) {
            if (!searching) {
                item { SectionHeader(stringResource(R.string.addons_installed)) }
                if (state.installed.isEmpty()) item { EmptyState(stringResource(R.string.addons_empty)) }
                items(state.installed, key = { "i" + it.id }) { addon -> InstalledRow(addon, actions) }
            }
            item { SectionHeader(stringResource(if (searching) R.string.addons_results else R.string.addons_recommended)) }
            when {
                state.failed -> item { EmptyState(stringResource(R.string.addons_error)) }
                state.loading && state.results.isEmpty() -> item { EmptyState("…") }
                state.results.isEmpty() -> item { EmptyState(stringResource(R.string.library_empty)) }
            }
            items(state.results, key = { "r" + it.guid }) { addon ->
                AmoRow(addon, installed = addon.guid in state.installedIds, busy = addon.guid in state.busy) { actions.install(addon) }
            }
        }
    }
}

@Composable
private fun AmoRow(addon: AmoAddon, installed: Boolean, busy: Boolean, onInstall: () -> Unit) {
    val users = stringResource(R.string.addons_users, NumberFormat.getIntegerInstance().format(addon.users))
    ListRow(
        title = addon.name,
        subtitle = listOf(users, addon.summary).filter { it.isNotEmpty() }.joinToString(" · "),
        onClick = if (installed || busy) null else onInstall,
        leading = { RemoteImage(addon.iconUrl) },
        trailing = {
            when {
                installed -> Icon(
                    BrowserIcons.Check,
                    contentDescription = stringResource(R.string.addons_installed_label),
                    tint = BrowserTheme.colors.accent,
                    modifier = Modifier.padding(Space.toolbarInset).size(20.dp),
                )
                busy -> Text("…", style = BrowserTheme.type.label, color = BrowserTheme.colors.textMuted, modifier = Modifier.padding(Space.toolbarInset))
                else -> IconAction(BrowserIcons.Plus, stringResource(R.string.addons_install), onInstall)
            }
        },
    )
}

@Composable
private fun InstalledRow(addon: Addon, actions: AddonActions) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    ListRow(
        title = addon.translateName(context),
        subtitle = addon.translateSummary(context).ifEmpty { addon.version },
        leading = { AddonIcon(addon.installedState?.icon ?: addon.icon) },
        trailing = {
            Toggle(addon.isEnabled(), { actions.setEnabled(addon, it) })
            Box {
                IconAction(BrowserIcons.More, stringResource(R.string.action_more), { menu = true })
                DropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    shape = Radius.card,
                    containerColor = BrowserTheme.colors.surfaceAlt,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    MenuRow(
                        stringResource(R.string.addons_options),
                        { menu = false; actions.openOptions(addon) },
                        icon = BrowserIcons.Settings,
                        enabled = addon.installedState?.optionsPageUrl != null,
                    )
                    MenuRow(stringResource(R.string.addons_uninstall), { menu = false; actions.uninstall(addon) }, icon = BrowserIcons.Delete)
                }
            }
        },
    )
}

@Composable
private fun AddonIcon(icon: Bitmap?) {
    Box(Modifier.size(32.dp).clip(Radius.control), contentAlignment = Alignment.Center) {
        if (icon != null) {
            BitmapImage(icon, Modifier.size(32.dp))
        } else {
            Icon(BrowserIcons.Extensions, contentDescription = null, tint = BrowserTheme.colors.textMuted)
        }
    }
}

@Preview
@Composable
private fun AddonsContentPreview() {
    BrowserTheme {
        AddonsContent(
            state = AddonsState(
                loading = false,
                results = listOf(AmoAddon("dark", "Dark Reader", "Dark mode for every website.", "", 1_345_173, "")),
            ),
            onBack = {},
            actions = AddonActions({}, {}, {}, { _, _ -> }, {}, {}),
        )
    }
}
