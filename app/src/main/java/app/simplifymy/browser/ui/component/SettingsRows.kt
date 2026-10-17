package app.simplifymy.browser.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.R
import app.simplifymy.browser.ui.theme.AppIcon
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Radius
import app.simplifymy.browser.ui.theme.Space

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = BrowserTheme.type.section,
            color = BrowserTheme.colors.textFaint,
            modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.base),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Space.gutter), content = content)
    }
}

@Composable
private fun SettingRow(
    label: String,
    hint: String? = null,
    onClick: (() -> Unit)? = null,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Space.hitTarget + 4.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Space.screen, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.toolbarInset),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = BrowserTheme.type.body, color = BrowserTheme.colors.text)
            if (hint != null) Text(hint, style = BrowserTheme.type.meta, color = BrowserTheme.colors.textFaint)
        }
        control()
    }
}

@Composable
fun PickerSetting(label: String, control: @Composable () -> Unit) = SettingRow(label, control = control)

@Composable
fun ToggleSetting(label: String, checked: Boolean, onChange: (Boolean) -> Unit, hint: String? = null) =
    SettingRow(label, hint, onClick = { onChange(!checked) }) { Toggle(checked, onChange) }

@Composable
fun LinkSetting(label: String, onClick: () -> Unit, hint: String? = null) =
    SettingRow(label, hint, onClick) {
        Icon(BrowserIcons.Forward, contentDescription = null, tint = BrowserTheme.colors.textMuted, modifier = Modifier.size(16.dp))
    }

@Composable
fun ValueSetting(label: String, value: String) = SettingRow(label) {
    Text(value, style = BrowserTheme.type.value, color = BrowserTheme.colors.textMuted)
}

@Composable
fun StepperSetting(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) = SettingRow(label) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.base)) {
        StepButton("−", value > range.first) { onChange(value - 1) }
        Text(value.toString(), style = BrowserTheme.type.value, color = BrowserTheme.colors.text)
        StepButton("+", value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(Radius.control)
            .background(BrowserTheme.colors.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = BrowserTheme.type.label, color = if (enabled) BrowserTheme.colors.text else BrowserTheme.colors.textFaint)
    }
}

@Composable
fun <T> DropdownSetting(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    hint: String? = null,
    optionLabel: @Composable (T) -> String,
) {
    var open by remember { mutableStateOf(false) }
    val colors = BrowserTheme.colors
    SettingRow(label, hint) {
        Box {
            Row(
                modifier = Modifier
                    .clip(Radius.control)
                    .background(colors.surface)
                    .clickable { open = true }
                    .padding(start = Space.toolbarInset, end = Space.base, top = Space.base, bottom = Space.base),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(optionLabel(selected), style = BrowserTheme.type.label, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(BrowserIcons.Forward, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(14.dp).rotate(90f))
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                shape = Radius.card,
                containerColor = colors.surfaceAlt,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                options.forEach { option ->
                    MenuRow(optionLabel(option), { onSelect(option); open = false }, value = if (option == selected) "✓" else null)
                }
            }
        }
    }
}

@Composable
fun SwatchSetting(label: String, swatches: List<Color>, selected: Int, onSelect: (Int) -> Unit) = SettingRow(label) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.base), verticalAlignment = Alignment.CenterVertically) {
        swatches.forEachIndexed { index, color ->
            val chosen = index == selected
            Box(
                Modifier
                    .size(if (chosen) 26.dp else 22.dp)
                    .clip(Radius.pill)
                    .background(color)
                    .border(2.dp, if (chosen) BrowserTheme.colors.text else Color.Transparent, Radius.pill)
                    .clickable { onSelect(index) },
            )
        }
    }
}

@Composable
fun AppIconSetting(selected: AppIcon, hint: String, onSelect: (AppIcon) -> Unit) {
    val colors = BrowserTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.screen, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppIconPreview(selected)
            Column(Modifier.weight(1f)) {
                Text(stringResource(selected.labelRes), style = BrowserTheme.type.body, color = colors.text)
                Text(hint, style = BrowserTheme.type.meta, color = colors.textFaint)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.screen),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon.entries.forEach { icon ->
                val chosen = icon == selected
                Box(
                    Modifier
                        .size(if (chosen) 26.dp else 22.dp)
                        .clip(Radius.pill)
                        .background(icon.background)
                        .border(2.dp, if (chosen) colors.text else Color.Transparent, Radius.pill)
                        .clickable { onSelect(icon) },
                )
            }
        }
    }
}

@Composable
private fun AppIconPreview(icon: AppIcon) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(APP_ICON_SHAPE)
            .background(icon.background)
            .border(1.dp, BrowserTheme.colors.hairline, APP_ICON_SHAPE),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.requiredSize(78.dp),
        )
    }
}

private val APP_ICON_SHAPE = RoundedCornerShape(15.dp)

@Preview
@Composable
private fun SettingsRowsPreview() {
    BrowserTheme {
        Screen {
            SettingsSection("Theme") {
                DropdownSetting("Mode", listOf("Auto", "Light"), "Auto", {}) { it }
                SwatchSetting("Accent", listOf(Color.Blue, Color.Yellow, Color.Green), 0, {})
                AppIconSetting(AppIcon.Blue, "Applied when you leave the app", {})
                ToggleSetting("Cookies", true, {}, hint = "Clears on exit")
                LinkSetting("Licenses", {})
                ValueSetting("Version", "0.1.0")
            }
        }
    }
}
