package app.simplifymy.browser.feature.addons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.simplifymy.browser.R
import app.simplifymy.browser.components
import app.simplifymy.browser.ui.component.BrowserDialog
import app.simplifymy.browser.ui.component.Checkbox
import app.simplifymy.browser.ui.theme.Space
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Radius
import mozilla.components.browser.state.state.WebExtensionState
import mozilla.components.browser.state.state.extension.WebExtensionPromptRequest
import mozilla.components.browser.state.state.extension.WebExtensionPromptRequest.AfterInstallation.Permissions
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.feature.addons.Addon

@Composable
fun ExtensionPrompt(request: WebExtensionPromptRequest, viewModel: AddonsViewModel, onNotice: (String) -> Unit) {
    when (request) {
        is Permissions.Required -> PermissionsDialog(
            title = stringResource(R.string.addon_permissions_title, request.extension.displayName()),
            permissions = request.permissions + request.origins,
            confirmLabel = stringResource(R.string.addon_add),
            dismissLabel = stringResource(R.string.action_cancel),
            askPrivate = true,
            onAnswer = { granted, private -> viewModel.answerPermissions(request, granted, private) },
        )
        is Permissions.Optional -> PermissionsDialog(
            title = stringResource(R.string.addon_optional_title, request.extension.displayName()),
            permissions = request.permissions + request.origins,
            confirmLabel = stringResource(R.string.action_allow),
            dismissLabel = stringResource(R.string.action_deny),
            askPrivate = false,
            onAnswer = { granted, _ -> viewModel.answerPermissions(request, granted, false) },
        )
        is WebExtensionPromptRequest.AfterInstallation.PostInstallation -> {
            val notice = stringResource(R.string.addon_added_title, request.extension.displayName())
            LaunchedEffect(request) {
                onNotice(notice)
                viewModel.consumePrompt()
            }
        }
        is WebExtensionPromptRequest.BeforeInstallation.InstallationFailed -> BrowserDialog(
            title = stringResource(R.string.addon_failed_title),
            onDismiss = viewModel::consumePrompt,
            confirmLabel = stringResource(R.string.action_ok),
            onConfirm = viewModel::consumePrompt,
        )
        else -> LaunchedEffect(request) { viewModel.consumePrompt() }
    }
}

private fun WebExtension.displayName() = getMetadata()?.name ?: id

@Composable
private fun PermissionsDialog(
    title: String,
    permissions: List<String>,
    confirmLabel: String,
    dismissLabel: String,
    askPrivate: Boolean,
    onAnswer: (granted: Boolean, private: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val localized = remember(permissions) { Addon.localizePermissions(permissions, context) }
    var private by remember { mutableStateOf(false) }
    BrowserDialog(
        title = title,
        onDismiss = { onAnswer(false, false) },
        confirmLabel = confirmLabel,
        onConfirm = { onAnswer(true, private) },
        dismissLabel = dismissLabel,
    ) {
        if (localized.isNotEmpty()) {
            Text(stringResource(R.string.addon_permissions_body), style = BrowserTheme.type.body, color = BrowserTheme.colors.textMuted)
            localized.forEach { Text("• $it", style = BrowserTheme.type.body, color = BrowserTheme.colors.text) }
        }
        if (askPrivate) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { private = !private }.padding(vertical = Space.base),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.toolbarInset),
            ) {
                Checkbox(private)
                Text(stringResource(R.string.addons_allow_private), style = BrowserTheme.type.body, color = BrowserTheme.colors.text)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionPopup(extension: WebExtensionState, onDismiss: () -> Unit) {
    val session = extension.popupSession ?: return
    val engine = LocalContext.current.components.engine
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = Radius.sheet,
        containerColor = BrowserTheme.colors.surfaceAlt,
        tonalElevation = 0.dp,
    ) {
        val context = LocalContext.current
        val view = remember(session) { engine.createView(context).also { it.render(session) } }
        DisposableEffect(view) { onDispose { view.release() } }
        AndroidView(
            factory = { view.asView() },
            modifier = Modifier.fillMaxWidth().height(480.dp).navigationBarsPadding(),
        )
    }
}
