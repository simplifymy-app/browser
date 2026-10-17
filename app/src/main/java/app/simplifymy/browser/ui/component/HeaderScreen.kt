package app.simplifymy.browser.ui.component

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.R
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Motion
import app.simplifymy.browser.ui.theme.Space
import app.simplifymy.browser.ui.theme.enterTween
import kotlin.math.roundToInt

private const val EXPANDED_SHARE = 0.195f
private const val LARGE_TITLE_SCALE = 1.5f
private const val TITLE_POSITION = 0.69f

@Composable
fun HeaderScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleAtBottom: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!BrowserTheme.oneHanded) {
        Screen(modifier) {
            ScreenHeader(title, onBack, Modifier.statusBarsPadding(), actions = actions)
            content()
        }
        return
    }
    val range = LocalWindowInfo.current.containerSize.height * EXPANDED_SHARE
    val snap = enterTween<Float>(Motion.CHROME)
    val header = remember(range, snap) { CollapsingHeader(range, snap) }
    if (titleAtBottom) {
        Screen(modifier.statusBarsPadding().nestedScroll(header)) {
            CollapsingGap { range * header.fraction }
            content()
            TitleRow(title, { 0f }, onBack, Modifier.navigationBarsPadding(), actions)
        }
        return
    }
    Screen(modifier.nestedScroll(header)) {
        CollapsingGap(Modifier.statusBarsPadding()) { range * TITLE_POSITION * header.fraction }
        TitleRow(title, { header.fraction }, onBack, Modifier, actions)
        CollapsingGap { range * (1f - TITLE_POSITION) * header.fraction }
        content()
    }
}

@Composable
private fun CollapsingGap(modifier: Modifier = Modifier, height: () -> Float) {
    Box(
        modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val size = height().roundToInt()
                val gap = measurable.measure(Constraints.fixed(constraints.maxWidth, size))
                layout(gap.width, size) { gap.place(0, 0) }
            },
    )
}

@Composable
private fun TitleRow(
    title: String,
    fraction: () -> Float,
    onBack: () -> Unit,
    modifier: Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(Space.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAction(BrowserIcons.Back, stringResource(R.string.action_back), onBack)
        CollapsingTitle(title, fraction, Modifier.weight(1f).padding(start = Space.base))
        actions()
    }
}

@Composable
private fun CollapsingTitle(title: String, fraction: () -> Float, modifier: Modifier) {
    val small = BrowserTheme.type.title
    val display = BrowserTheme.type.display
    val large = remember(display) { display.copy(fontSize = display.fontSize * LARGE_TITLE_SCALE) }
    Text(
        title,
        style = lerp(small, large, fraction()),
        color = BrowserTheme.colors.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Stable
private class CollapsingHeader(private val range: Float, private val snap: AnimationSpec<Float>) : NestedScrollConnection {

    var offset by mutableFloatStateOf(0f)
        private set

    val fraction: Float get() = if (range > 0f) 1f + offset / range else 0f

    private fun consume(delta: Float): Float {
        val next = (offset + delta).coerceIn(-range, 0f)
        val used = next - offset
        offset = next
        return used
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource) =
        if (available.y < 0f) Offset(0f, consume(available.y)) else Offset.Zero

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) =
        if (available.y > 0f) Offset(0f, consume(available.y)) else Offset.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (offset != 0f && offset != -range) {
            animate(offset, if (fraction > 0.5f) 0f else -range, animationSpec = snap) { value, _ -> offset = value }
        }
        return Velocity.Zero
    }
}

@Preview
@Composable
private fun HeaderScreenPreview() {
    BrowserTheme(oneHanded = true) {
        HeaderScreen("Settings", onBack = {}) {
            SectionHeader("Browser")
            ListRow("Search engine", subtitle = "Google")
        }
    }
}
