package app.simplifymy.browser.feature.browser

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.R
import app.simplifymy.browser.ui.component.BitmapImage
import app.simplifymy.browser.ui.component.BrowserIcons
import app.simplifymy.browser.ui.component.MenuRow
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Radius
import app.simplifymy.browser.ui.theme.Sizes
import app.simplifymy.browser.ui.theme.Space
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl

@Composable
fun AddressBar(
    url: String,
    searchIcon: Bitmap?,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    private: Boolean = false,
    loading: Boolean = false,
    onStop: () -> Unit = {},
    security: Security = Security.Unknown,
    issuer: String = "",
    searchEngines: List<SearchEngine> = emptyList(),
    searchEngineId: String? = null,
    onSelectEngine: (SearchEngine) -> Unit = {},
) {
    var editing by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .height(Sizes.dropdown)
            .clip(Radius.control)
            .background(BrowserTheme.colors.surface)
            .clickable(enabled = !editing) { editing = true }
            .padding(start = Space.gutter, end = Space.toolbarInset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.gutter),
    ) {
        if (editing || url.isEmpty()) {
            SearchEnginePicker(searchIcon, searchEngines, searchEngineId, onSelectEngine)
        } else {
            SecurityBadge(security, issuer, url.tryGetHostFromUrl())
        }
        if (editing) {
            UrlField(url, onSubmit, onClose = { editing = false }, Modifier.weight(1f))
        } else {
            if (private) {
                Text(stringResource(R.string.private_label).uppercase(), style = BrowserTheme.type.section, color = BrowserTheme.colors.textMuted)
            }
            UrlLabel(url, Modifier.weight(1f))
            if (loading) {
                Icon(
                    BrowserIcons.Close,
                    contentDescription = stringResource(R.string.action_stop),
                    tint = BrowserTheme.colors.textMuted,
                    modifier = Modifier.size(16.dp).clickable(onClick = onStop),
                )
            }
        }
    }
}

@Composable
private fun UrlLabel(url: String, modifier: Modifier) {
    val colors = BrowserTheme.colors
    val empty = url.isEmpty()
    val host = colors.text
    val path = colors.textMuted
    val display = remember(url, host, path) { displayUrl(url, host, path) }
    Text(
        text = if (empty) AnnotatedString(stringResource(R.string.address_hint)) else display,
        style = if (empty) BrowserTheme.type.body else BrowserTheme.type.value,
        color = if (empty) colors.textFaint else colors.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private fun displayUrl(url: String, host: Color, path: Color): AnnotatedString {
    val bare = url.substringAfter("://").removePrefix("www.").removeSuffix("/")
    val split = bare.indexOf('/').takeIf { it >= 0 } ?: bare.length
    return buildAnnotatedString {
        withStyle(SpanStyle(color = host)) { append(bare.take(split)) }
        withStyle(SpanStyle(color = path)) { append(bare.drop(split)) }
    }
}

@Composable
private fun LeadingMenu(
    label: String,
    enabled: Boolean,
    icon: @Composable () -> Unit,
    menu: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(Space.hitTarget - Space.base)
                .clip(Radius.control)
                .clickable(enabled = enabled) { open = true }
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = Radius.card,
            containerColor = BrowserTheme.colors.surfaceAlt,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            menu { open = false }
        }
    }
}

@Composable
private fun SecurityBadge(security: Security, issuer: String, host: String) {
    val colors = BrowserTheme.colors
    val title = stringResource(if (security == Security.Secure) R.string.security_secure else R.string.security_insecure)
    LeadingMenu(
        label = title,
        enabled = security != Security.Unknown,
        icon = {
            Icon(
                imageVector = when (security) {
                    Security.Secure -> BrowserIcons.Secure
                    Security.Insecure -> BrowserIcons.Insecure
                    Security.Unknown -> BrowserIcons.Info
                },
                contentDescription = null,
                tint = if (security == Security.Secure) colors.text else colors.textMuted,
                modifier = Modifier.size(Sizes.icon),
            )
        },
    ) {
        Column(Modifier.widthIn(min = 240.dp).padding(horizontal = Space.screen, vertical = Space.toolbarInset)) {
            Text(host, style = BrowserTheme.type.label, color = colors.text)
            Text(title, style = BrowserTheme.type.body, color = colors.textMuted, modifier = Modifier.padding(top = Space.gutter))
            if (security == Security.Secure && issuer.isNotEmpty()) {
                Text(stringResource(R.string.security_issuer, issuer), style = BrowserTheme.type.meta, color = colors.textFaint)
            }
        }
    }
}

@Composable
private fun UrlField(url: String, onSubmit: (String) -> Unit, onClose: () -> Unit, modifier: Modifier) {
    val colors = BrowserTheme.colors
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var input by remember { mutableStateOf(TextFieldValue(url, TextRange(0, url.length))) }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    BackHandler(onBack = onClose)

    BasicTextField(
        value = input,
        onValueChange = { input = it },
        singleLine = true,
        textStyle = BrowserTheme.type.value.copy(color = colors.text),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = {
            onSubmit(input.text)
            onClose()
        }),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (focused && !it.isFocused) onClose()
                focused = it.isFocused
            },
    )
}

@Composable
private fun SearchEnginePicker(
    icon: Bitmap?,
    engines: List<SearchEngine>,
    selectedId: String?,
    onSelect: (SearchEngine) -> Unit,
) {
    LeadingMenu(
        label = stringResource(R.string.setting_search_engine),
        enabled = engines.isNotEmpty(),
        icon = { SearchEngineIcon(icon) },
    ) { dismiss ->
        engines.forEach { engine ->
            MenuRow(
                label = engine.name,
                onClick = { onSelect(engine); dismiss() },
                leading = { SearchEngineIcon(engine.icon) },
                value = if (engine.id == selectedId) "✓" else null,
            )
        }
    }
}

@Composable
private fun SearchEngineIcon(icon: Bitmap?) {
    val modifier = Modifier.size(Sizes.icon)
    if (icon == null) {
        Icon(BrowserIcons.Search, contentDescription = null, tint = BrowserTheme.colors.textMuted, modifier = modifier)
    } else {
        BitmapImage(icon, modifier.clip(Radius.control))
    }
}

@Preview
@Composable
private fun AddressBarPreview() {
    BrowserTheme {
        AddressBar(url = "https://example.com/page", searchIcon = null, onSubmit = {}, modifier = Modifier.fillMaxWidth())
    }
}

@Preview
@Composable
private fun AddressBarPrivatePreview() {
    BrowserTheme {
        AddressBar(url = "", searchIcon = null, onSubmit = {}, modifier = Modifier.fillMaxWidth(), private = true, loading = true)
    }
}
