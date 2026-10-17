package app.simplifymy.browser.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.R
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Motion
import app.simplifymy.browser.ui.theme.Radius
import app.simplifymy.browser.ui.theme.Sizes
import app.simplifymy.browser.ui.theme.Space
import app.simplifymy.browser.ui.theme.States
import app.simplifymy.browser.ui.theme.enterTween

@Composable
fun IconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = BrowserTheme.colors.text,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(Space.hitTarget)
            .alpha(if (enabled) 1f else States.DISABLED)
            .clip(Radius.control)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(Sizes.icon))
    }
}

@Composable
fun Toggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = BrowserTheme.colors
    val track by animateColorAsState(if (checked) colors.accent else colors.surfaceAlt, enterTween(Motion.STATE), label = "track")
    val knob by animateDpAsState(if (checked) 20.dp else 2.dp, enterTween(Motion.STATE), label = "knob")
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(Radius.pill)
            .background(track)
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = knob, y = 3.dp)
                .size(20.dp)
                .clip(Radius.pill)
                .background(if (checked) colors.onAccent else colors.textMuted),
        )
    }
}

@Composable
fun TextButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false, enabled: Boolean = true) {
    val colors = BrowserTheme.colors
    Box(
        modifier = modifier
            .sizeIn(minHeight = Sizes.button)
            .alpha(if (enabled) 1f else States.DISABLED)
            .clip(Radius.control)
            .background(if (primary) colors.accent else colors.surfaceAlt)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.toolbarInset),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = BrowserTheme.type.label, color = if (primary) colors.onAccent else colors.text)
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.gutter, vertical = Space.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAction(BrowserIcons.Back, stringResource(R.string.action_back), onBack)
        Text(
            title,
            style = BrowserTheme.type.title,
            color = BrowserTheme.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = Space.base),
        )
        actions()
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = BrowserTheme.type.section,
        color = BrowserTheme.colors.textMuted,
        modifier = modifier.padding(start = Space.screen, end = Space.screen, top = Space.section, bottom = Space.base),
    )
}

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    leading: @Composable () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Space.settingsRow)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = Space.screen, end = Space.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.toolbarInset),
    ) {
        leading()
        Column(Modifier.weight(1f).padding(vertical = Space.base)) {
            Text(title, style = BrowserTheme.type.body, color = BrowserTheme.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = BrowserTheme.type.meta, color = BrowserTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = BrowserTheme.type.body,
        color = BrowserTheme.colors.textFaint,
        modifier = modifier.padding(Space.screen),
    )
}

@Composable
fun <T> SegmentedPicker(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (T) -> String,
) {
    val colors = BrowserTheme.colors
    Row(
        modifier = modifier.clip(Radius.card).background(colors.surface).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 28.dp, minHeight = 30.dp)
                    .clip(Radius.control)
                    .background(if (on) colors.accent else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = BrowserTheme.type.label, color = if (on) colors.onAccent else colors.textMuted)
            }
        }
    }
}

@Composable
fun Screen(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(BrowserTheme.colors.background), content = content)
}

@Preview
@Composable
private fun ControlsPreview() {
    BrowserTheme {
        Screen {
            ScreenHeader("Extensions", onBack = {})
            SectionHeader("Installed")
            ListRow("uBlock Origin", subtitle = "1.62.0", trailing = { Toggle(true, {}) })
            Row(Modifier.padding(Space.screen), horizontalArrangement = Arrangement.spacedBy(Space.base)) {
                TextButton("Cancel", {})
                TextButton("Add", {}, primary = true)
            }
            SegmentedPicker(listOf("Tabs", "Private"), "Tabs", {}, Modifier.padding(Space.screen)) { it }
        }
    }
}

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onSubmit: (() -> Unit)? = null,
) {
    val colors = BrowserTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = BrowserTheme.type.body.copy(color = colors.text),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (onSubmit != null) ImeAction.Search else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(onSearch = { onSubmit?.invoke() }),
        modifier = modifier
            .heightIn(min = Sizes.dropdown)
            .clip(Radius.control)
            .background(colors.surface)
            .padding(horizontal = Space.toolbarInset, vertical = Space.base),
        decorationBox = { field ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = BrowserTheme.type.body, color = colors.textFaint, maxLines = 1)
                field()
            }
        },
    )
}
