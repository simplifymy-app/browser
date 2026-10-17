package app.simplifymy.browser.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simplifymy.browser.ui.theme.BrowserTheme
import app.simplifymy.browser.ui.theme.Radius
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl

class ImageLoaders(
    val favicon: suspend (url: String, discover: Boolean) -> Bitmap? = { _, _ -> null },
    val thumbnail: suspend (tabId: String, private: Boolean) -> Bitmap? = { _, _ -> null },
    val image: suspend (url: String) -> Bitmap? = { null },
    val thumbnailVersions: StateFlow<Map<String, Int>> = MutableStateFlow(emptyMap()),
)

val LocalImageLoaders = staticCompositionLocalOf { ImageLoaders() }

@Composable
fun BitmapImage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Image(image, contentDescription = null, modifier = modifier, contentScale = contentScale)
}

@Composable
fun Favicon(
    url: String,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    bitmap: Bitmap? = null,
    private: Boolean = false,
    discover: Boolean = false,
) {
    val loader = LocalImageLoaders.current.favicon
    val loaded by produceState(bitmap, url, bitmap, private, discover) {
        if (bitmap == null && !private && url.isNotEmpty()) value = loader(url, discover)
    }
    val icon = loaded
    if (icon != null) {
        BitmapImage(icon, modifier.size(size).clip(Radius.control))
    } else {
        Box(
            modifier = modifier.size(size).clip(Radius.control).background(BrowserTheme.colors.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                url.tryGetHostFromUrl().removePrefix("www.").take(1).uppercase(),
                style = BrowserTheme.type.meta,
                color = BrowserTheme.colors.textMuted,
            )
        }
    }
}

@Composable
fun TabThumbnail(tabId: String, private: Boolean, modifier: Modifier = Modifier, placeholder: Boolean = true) {
    val loaders = LocalImageLoaders.current
    val versions by loaders.thumbnailVersions.collectAsState()
    val thumbnail by produceState<Bitmap?>(null, tabId, versions[tabId]) {
        loaders.thumbnail(tabId, private)?.let { value = it }
    }
    Box(if (placeholder) modifier.background(BrowserTheme.colors.surfaceAlt) else modifier) {
        thumbnail?.let { BitmapImage(it, Modifier.matchParentSize(), ContentScale.Crop) }
    }
}

@Composable
fun RemoteImage(url: String, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val loader = LocalImageLoaders.current.image
    val bitmap by produceState<Bitmap?>(null, url) { if (url.isNotEmpty()) value = loader(url) }
    Box(modifier.size(size).clip(Radius.control).background(BrowserTheme.colors.surfaceAlt)) {
        bitmap?.let { BitmapImage(it, Modifier.matchParentSize()) }
    }
}

@Preview
@Composable
private fun FaviconPreview() {
    BrowserTheme { Favicon("https://example.com", size = 32.dp) }
}
