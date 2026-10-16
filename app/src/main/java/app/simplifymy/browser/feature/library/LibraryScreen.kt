package app.simplifymy.browser.feature.library

import android.app.DownloadManager
import android.content.Intent
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simplifymy.browser.R
import app.simplifymy.browser.ui.component.BrowserIcons
import app.simplifymy.browser.ui.component.EmptyState
import app.simplifymy.browser.ui.component.Favicon
import app.simplifymy.browser.ui.component.IconAction
import app.simplifymy.browser.ui.component.ListRow
import app.simplifymy.browser.ui.component.Screen
import app.simplifymy.browser.ui.component.ScreenHeader
import app.simplifymy.browser.ui.component.SegmentedPicker
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Space
import mozilla.components.browser.state.state.content.DownloadState

enum class LibrarySection(val labelRes: Int) {
    Bookmarks(R.string.action_bookmarks),
    History(R.string.action_history),
    Downloads(R.string.action_downloads),
}

@Composable
fun LibraryScreen(viewModel: LibraryViewModel, start: LibrarySection, onOpen: (String) -> Unit, onBack: () -> Unit) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.load() }
    BackHandler(onBack = onBack)
    LibraryContent(
        start = start,
        bookmarks = bookmarks,
        history = history,
        downloads = downloads,
        onBack = onBack,
        onOpen = onOpen,
        onDeleteBookmark = viewModel::deleteBookmark,
        onDeleteHistory = viewModel::deleteHistory,
        onClearHistory = viewModel::clearHistory,
        onOpenDownloads = {
            context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
    )
}

@Composable
fun LibraryContent(
    start: LibrarySection,
    bookmarks: List<LibraryEntry>,
    history: List<LibraryEntry>,
    downloads: List<DownloadItem>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onDeleteBookmark: (LibraryEntry) -> Unit,
    onDeleteHistory: (LibraryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
) {
    var section by remember { mutableStateOf(start) }
    Screen(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(section.labelRes), onBack) {
            when (section) {
                LibrarySection.History -> IconAction(BrowserIcons.Delete, stringResource(R.string.history_clear), onClearHistory, enabled = history.isNotEmpty())
                LibrarySection.Downloads -> IconAction(BrowserIcons.Download, stringResource(R.string.downloads_system), onOpenDownloads)
                LibrarySection.Bookmarks -> Unit
            }
        }
        SegmentedPicker(
            options = LibrarySection.entries,
            selected = section,
            onSelect = { section = it },
            modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.base),
        ) { stringResource(it.labelRes) }
        LazyColumn(Modifier.weight(1f).navigationBarsPadding()) {
            when (section) {
                LibrarySection.Bookmarks -> {
                    if (bookmarks.isEmpty()) item { EmptyState(stringResource(R.string.library_empty)) }
                    items(bookmarks, key = { it.key }) { entry -> EntryRow(entry, onOpen, onDeleteBookmark, relative = false) }
                }
                LibrarySection.History -> {
                    if (history.isEmpty()) item { EmptyState(stringResource(R.string.library_empty)) }
                    items(history, key = { it.key }) { entry -> EntryRow(entry, onOpen, onDeleteHistory, relative = true) }
                }
                LibrarySection.Downloads -> {
                    if (downloads.isEmpty()) item { EmptyState(stringResource(R.string.library_empty)) }
                    items(downloads, key = { it.state.id }) { download -> DownloadRow(download, onOpenDownloads) }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: LibraryEntry, onOpen: (String) -> Unit, onDelete: (LibraryEntry) -> Unit, relative: Boolean) {
    val subtitle = if (relative) "${DateUtils.getRelativeTimeSpanString(entry.time)} · ${entry.url}" else entry.url
    ListRow(
        title = entry.title.ifEmpty { entry.url },
        subtitle = subtitle,
        onClick = { onOpen(entry.url) },
        leading = { Favicon(entry.url, size = 24.dp) },
        trailing = { IconAction(BrowserIcons.Close, stringResource(R.string.action_close), { onDelete(entry) }, tint = BrowserTheme.colors.textMuted) },
    )
}

@Composable
private fun DownloadRow(item: DownloadItem, onClick: () -> Unit) {
    val download = item.state
    val colors = BrowserTheme.colors
    val downloading = download.status == DownloadState.Status.DOWNLOADING
    val total = download.contentLength?.takeIf { it > 0 }
    Column {
        ListRow(
            title = download.fileName ?: download.url,
            subtitle = downloadSubtitle(item),
            onClick = onClick,
            leading = { IconAction(BrowserIcons.Download, download.fileName.orEmpty(), onClick, tint = colors.textMuted) },
        )
        if (downloading && total != null) {
            val fraction = (download.currentBytesCopied.toFloat() / total).coerceIn(0f, 1f)
            Canvas(Modifier.fillMaxWidth().padding(horizontal = Space.screen).padding(bottom = Space.base).height(2.dp)) {
                drawRect(colors.hairline)
                drawRect(colors.accent, size = size.copy(width = size.width * fraction))
            }
        }
    }
}

@Composable
private fun downloadSubtitle(item: DownloadItem): String {
    val context = LocalContext.current
    val download = item.state
    fun bytes(value: Long) = Formatter.formatShortFileSize(context, value)
    val total = download.contentLength?.takeIf { it > 0 }
    val parts = when (download.status) {
        DownloadState.Status.DOWNLOADING -> listOfNotNull(
            if (total != null) stringResource(R.string.download_progress, bytes(download.currentBytesCopied), bytes(total)) else bytes(download.currentBytesCopied),
            item.bytesPerSecond?.let { stringResource(R.string.download_speed, bytes(it)) },
        )
        DownloadState.Status.PAUSED -> listOfNotNull(
            stringResource(download.status.labelRes()),
            if (total != null) stringResource(R.string.download_progress, bytes(download.currentBytesCopied), bytes(total)) else null,
        )
        else -> listOfNotNull(stringResource(download.status.labelRes()), total?.let(::bytes))
    }
    return parts.joinToString(" · ")
}

private fun DownloadState.Status.labelRes() = when (this) {
    DownloadState.Status.INITIATED, DownloadState.Status.DOWNLOADING -> R.string.download_in_progress
    DownloadState.Status.PAUSED -> R.string.download_paused
    DownloadState.Status.CANCELLED -> R.string.download_cancelled
    DownloadState.Status.FAILED -> R.string.download_failed
    DownloadState.Status.COMPLETED -> R.string.download_completed
}

@Preview
@Composable
private fun LibraryContentPreview() {
    BrowserTheme {
        LibraryContent(
            start = LibrarySection.History,
            bookmarks = emptyList(),
            history = listOf(LibraryEntry("1", "Example Domain", "https://example.com", 0)),
            downloads = emptyList(),
            onBack = {},
            onOpen = {},
            onDeleteBookmark = {},
            onDeleteHistory = {},
            onClearHistory = {},
            onOpenDownloads = {},
        )
    }
}
