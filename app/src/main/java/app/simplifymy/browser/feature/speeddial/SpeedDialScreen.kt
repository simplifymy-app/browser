package app.simplifymy.browser.feature.speeddial

import androidx.compose.foundation.background
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.simplifymy.browser.R
import app.simplifymy.browser.core.database.SpeedDialTile
import app.simplifymy.browser.core.database.snapshotId
import app.simplifymy.browser.core.settings.SpeedDialStyle
import app.simplifymy.browser.ui.component.BrowserDialog
import app.simplifymy.browser.ui.component.BrowserIcons
import app.simplifymy.browser.ui.component.Favicon
import app.simplifymy.browser.ui.component.InputField
import app.simplifymy.browser.ui.component.MenuRow
import app.simplifymy.browser.ui.component.TabThumbnail
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Radius
import app.simplifymy.browser.ui.theme.Sizes
import app.simplifymy.browser.ui.theme.Space

@Composable
fun SpeedDialScreen(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: SpeedDialViewModel = viewModel(factory = SpeedDialViewModel.Factory)
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<SpeedDialTile?>(null) }
    var adding by remember { mutableStateOf(false) }

    SpeedDialGrid(
        tiles = tiles,
        columns = settings.speedDialColumns,
        style = settings.speedDialStyle,
        onOpen = { onOpen(it.url) },
        onAdd = { adding = true },
        onEdit = { editing = it },
        onReorder = viewModel::reorder,
        onRemove = viewModel::remove,
        modifier = modifier,
    )
    if (adding || editing != null) {
        TileDialog(
            tile = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { title, url ->
                viewModel.save(editing, title, url)
                adding = false
                editing = null
            },
        )
    }
}

@Composable
fun SpeedDialGrid(
    tiles: List<SpeedDialTile>,
    columns: Int,
    style: SpeedDialStyle,
    onOpen: (SpeedDialTile) -> Unit,
    onAdd: () -> Unit,
    onEdit: (SpeedDialTile) -> Unit,
    onReorder: (List<SpeedDialTile>) -> Unit,
    onRemove: (SpeedDialTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val drag = remember { TileDrag() }
    drag.sync(tiles)
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize().background(BrowserTheme.colors.background),
        contentPadding = PaddingValues(Space.screen),
        horizontalArrangement = Arrangement.spacedBy(Space.base),
        verticalArrangement = Arrangement.spacedBy(Space.base),
    ) {
        items(drag.order, key = { it.id }) { tile ->
            val dragging = drag.draggingId == tile.id
            TileCard(
                tile = tile,
                style = style,
                onOpen = { onOpen(tile) },
                onEdit = { onEdit(tile) },
                onRemove = { onRemove(tile) },
                modifier = if (dragging) {
                    Modifier.zIndex(1f).graphicsLayer {
                        translationX = drag.offset.x
                        translationY = drag.offset.y
                        scaleX = DRAG_SCALE
                        scaleY = DRAG_SCALE
                    }
                } else {
                    Modifier.animateItem()
                },
                dragHandle = Modifier.pointerInput(tile.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { drag.start(tile.id) },
                        onDrag = { change, amount ->
                            change.consume()
                            drag.move(amount, gridState)
                        },
                        onDragEnd = { drag.finish(onReorder) },
                        onDragCancel = { drag.finish(onReorder) },
                    )
                },
                showMenu = drag.menuFor == tile.id,
                onMenuDismiss = { drag.menuFor = null },
            )
        }
        item { AddTile(style, onAdd) }
    }
}

private const val SNAPSHOT_RATIO = 0.8f

@Composable
private fun tileShape() = RoundedCornerShape(BrowserTheme.corners.radius)

private const val DRAG_SCALE = 1.06f

private class TileDrag {
    var order by mutableStateOf(emptyList<SpeedDialTile>())
    var draggingId by mutableStateOf<Long?>(null)
    var offset by mutableStateOf(Offset.Zero)
    var menuFor by mutableStateOf<Long?>(null)
    private var source = emptyList<SpeedDialTile>()
    private var travelled = 0f

    fun sync(tiles: List<SpeedDialTile>) {
        if (draggingId == null && tiles != source) {
            source = tiles
            order = tiles
        }
    }

    fun start(id: Long) {
        draggingId = id
        offset = Offset.Zero
        travelled = 0f
    }

    fun move(amount: Offset, grid: LazyGridState) {
        val id = draggingId ?: return
        offset += amount
        travelled += amount.getDistance()
        val items = grid.layoutInfo.visibleItemsInfo
        val dragged = items.firstOrNull { it.key == id } ?: return
        val center = dragged.offset.toOffset() + offset + Offset(dragged.size.width / 2f, dragged.size.height / 2f)
        val target = items.firstOrNull { info ->
            info.key != id && info.key is Long &&
                center.x in info.offset.x.toFloat()..(info.offset.x + info.size.width).toFloat() &&
                center.y in info.offset.y.toFloat()..(info.offset.y + info.size.height).toFloat()
        } ?: return
        val from = order.indexOfFirst { it.id == id }
        val to = order.indexOfFirst { it.id == target.key }
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        offset += (dragged.offset - target.offset).toOffset()
    }

    fun finish(onReorder: (List<SpeedDialTile>) -> Unit) {
        val id = draggingId ?: return
        draggingId = null
        offset = Offset.Zero
        when {
            order != source -> onReorder(order)
            travelled < MENU_SLOP -> menuFor = id
        }
    }

    private companion object {
        const val MENU_SLOP = 24f
    }
}

@Composable
private fun TileCard(
    tile: SpeedDialTile,
    style: SpeedDialStyle,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier,
    dragHandle: Modifier,
    showMenu: Boolean,
    onMenuDismiss: () -> Unit,
) {
    val dismiss = { action: () -> Unit -> { onMenuDismiss(); action() } }
    Box(modifier) {
        val click = Modifier.combinedClickable(onClick = onOpen, onLongClick = {}).then(dragHandle)
        when (style) {
            SpeedDialStyle.Snapshot -> SnapshotTile(tile, click)
            SpeedDialStyle.Icon -> TileBox(click) { Favicon(tile.url, size = 40.dp, discover = true) }
            SpeedDialStyle.IconAndTitle -> TileBox(click) {
                Favicon(tile.url, size = 32.dp, discover = true)
                TileTitle(tile.title, Modifier.padding(top = Space.base))
            }
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onMenuDismiss,
            shape = Radius.card,
            containerColor = BrowserTheme.colors.surfaceAlt,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            MenuRow(stringResource(R.string.speed_dial_edit), dismiss(onEdit), icon = BrowserIcons.Edit)
            MenuRow(stringResource(R.string.speed_dial_remove), dismiss(onRemove), icon = BrowserIcons.Delete)
        }
    }
}

@Composable
private fun TileBox(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(tileShape())
            .background(BrowserTheme.colors.surface)
            .then(modifier)
            .padding(Space.base),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Composable
private fun SnapshotTile(tile: SpeedDialTile, modifier: Modifier) {
    Column(modifier = Modifier.fillMaxWidth().clip(tileShape()).then(modifier)) {
        Box(Modifier.fillMaxWidth().aspectRatio(SNAPSHOT_RATIO).clip(tileShape()), contentAlignment = Alignment.Center) {
            Box(Modifier.matchParentSize().background(BrowserTheme.colors.surface))
            Favicon(tile.url, size = 32.dp, discover = true)
            TabThumbnail(snapshotId(tile.url), private = false, modifier = Modifier.matchParentSize(), placeholder = false)
        }
        TileTitle(tile.title, Modifier.fillMaxWidth().padding(top = Space.gutter))
    }
}

@Composable
private fun TileTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = BrowserTheme.type.meta,
        color = BrowserTheme.colors.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun AddTile(style: SpeedDialStyle, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (style == SpeedDialStyle.Snapshot) SNAPSHOT_RATIO else 1f)
            .clip(tileShape())
            .background(BrowserTheme.colors.surface)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            BrowserIcons.Plus,
            contentDescription = stringResource(R.string.speed_dial_add),
            tint = BrowserTheme.colors.textMuted,
            modifier = Modifier.size(Sizes.icon),
        )
    }
}

@Composable
private fun TileDialog(tile: SpeedDialTile?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf(tile?.title.orEmpty()) }
    var url by remember { mutableStateOf(tile?.url.orEmpty()) }
    BrowserDialog(
        title = stringResource(if (tile == null) R.string.speed_dial_add else R.string.speed_dial_edit),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = { if (url.isNotBlank()) onSave(title, url) },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        InputField(title, { title = it }, stringResource(R.string.field_title), Modifier.fillMaxWidth())
        InputField(url, { url = it }, stringResource(R.string.field_url), Modifier.fillMaxWidth(), KeyboardType.Uri)
    }
}

@Preview
@Composable
private fun SpeedDialGridPreview() {
    BrowserTheme {
        SpeedDialGrid(
            tiles = listOf(
                SpeedDialTile(1, "Wikipedia", "https://wikipedia.org", 0),
                SpeedDialTile(2, "Add-ons", "https://addons.mozilla.org", 1),
            ),
            columns = 3,
            style = SpeedDialStyle.IconAndTitle,
            onOpen = {},
            onAdd = {},
            onEdit = {},
            onReorder = {},
            onRemove = {},
        )
    }
}
