package app.simplifymy.browser.ui.component

import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Radius
import app.simplifymy.browser.ui.theme.Space

@Composable
fun BrowserDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Dialog(onDismissRequest = onDismiss) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val gravity = if (BrowserTheme.oneHanded) Gravity.BOTTOM else Gravity.CENTER
        SideEffect { window?.setGravity(gravity) }
        DialogCard(title, onDismiss, confirmLabel, onConfirm, dismissLabel, content)
    }
}

@Composable
private fun DialogCard(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.card)
            .background(BrowserTheme.colors.surfaceAlt)
            .padding(Space.screen),
        verticalArrangement = Arrangement.spacedBy(Space.toolbarInset),
    ) {
        Text(title, style = BrowserTheme.type.title, color = BrowserTheme.colors.text)
        content()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.base, Alignment.End)) {
            if (dismissLabel != null) TextButton(dismissLabel, onDismiss)
            TextButton(confirmLabel, onConfirm, primary = true)
        }
    }
}

@Preview
@Composable
private fun DialogPreview() {
    BrowserTheme {
        DialogCard("Add uBlock Origin?", {}, "Add", {}, "Cancel") {
            Text("Access your data for all websites", style = BrowserTheme.type.body, color = BrowserTheme.colors.textMuted)
        }
    }
}
