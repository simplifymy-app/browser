package app.simplifymy.browser.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.MenuDensity
import app.simplifymy.browser.ui.theme.Radius
import app.simplifymy.browser.ui.theme.Sizes
import app.simplifymy.browser.ui.theme.Space
import app.simplifymy.browser.ui.theme.States

@Composable
fun MenuRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    leading: @Composable () -> Unit = {},
    value: String? = null,
    checked: Boolean? = null,
) {
    val colors = BrowserTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BrowserTheme.menuDensity.rowHeight)
            .alpha(if (enabled) 1f else States.DISABLED)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.screen),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(Sizes.icon)) else leading()
        Text(label, style = BrowserTheme.type.body, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = BrowserTheme.type.meta, color = colors.textMuted)
        if (checked != null) Checkbox(checked)
    }
}

@Composable
fun Checkbox(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = BrowserTheme.colors
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(Radius.control)
            .background(if (checked) colors.accent else Color.Transparent)
            .border(1.5.dp, if (checked) colors.accent else colors.textMuted, Radius.control),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(BrowserIcons.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun MenuDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(horizontal = Space.screen, vertical = if (BrowserTheme.menuDensity == MenuDensity.Small) 2.dp else Space.gutter)
            .fillMaxWidth()
            .height(Sizes.hairline)
            .background(BrowserTheme.colors.hairline),
    )
}

@Composable
fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .size(Space.hitTarget)
            .alpha(if (enabled) 1f else States.DISABLED)
            .clip(Radius.control)
            .background(BrowserTheme.colors.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = BrowserTheme.colors.text, modifier = Modifier.size(Sizes.icon))
    }
}

@Preview
@Composable
private fun MenuPreview() {
    BrowserTheme {
        Column(Modifier.width(280.dp).background(BrowserTheme.colors.surfaceAlt)) {
            MenuRow("New tab", {}, icon = BrowserIcons.Plus)
            MenuRow("Mobile view", {}, icon = BrowserIcons.Mobile, checked = true)
            MenuDivider()
            Row(Modifier.fillMaxWidth().padding(Space.base), horizontalArrangement = Arrangement.SpaceEvenly) {
                QuickAction(BrowserIcons.Forward, "Forward", {})
                QuickAction(BrowserIcons.Reload, "Reload", {})
            }
        }
    }
}
