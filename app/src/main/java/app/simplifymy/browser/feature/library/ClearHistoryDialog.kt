package app.simplifymy.browser.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.simplifymy.browser.R
import app.simplifymy.browser.core.history.HistoryPeriod
import app.simplifymy.browser.ui.component.BrowserDialog
import app.simplifymy.browser.ui.component.DropdownSetting
import app.simplifymy.browser.ui.theme.BrowserTheme

@Composable
fun HistoryPeriodSetting(selected: HistoryPeriod, onSelect: (HistoryPeriod) -> Unit) {
    DropdownSetting(
        label = stringResource(R.string.clear_period),
        options = HistoryPeriod.entries,
        selected = selected,
        onSelect = onSelect,
    ) { stringResource(it.labelRes) }
}

@Composable
fun ClearHistoryDialog(onDismiss: () -> Unit, onClear: (HistoryPeriod) -> Unit) {
    var period by remember { mutableStateOf(HistoryPeriod.All) }
    BrowserDialog(
        title = stringResource(R.string.history_clear),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.action_clear),
        onConfirm = { onClear(period) },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        HistoryPeriodSetting(period) { period = it }
    }
}

@Preview
@Composable
private fun ClearHistoryDialogPreview() {
    BrowserTheme { ClearHistoryDialog(onDismiss = {}, onClear = {}) }
}
