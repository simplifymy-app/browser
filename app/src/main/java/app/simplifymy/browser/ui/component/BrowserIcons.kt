package app.simplifymy.browser.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private fun icon(pathData: String, filled: Boolean = false): ImageVector =
    ImageVector.Builder(
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = if (filled) SolidColor(Color.Black) else null,
            stroke = if (filled) null else SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

object BrowserIcons {
    val Back = icon("M12 4L6 10L12 16")
    val Forward = icon("M8 4L14 10L8 16")
    val Reload = icon("M16 10a6 6 0 1 1-1.76-4.24M16 3.5v3.25h-3.25")
    val Close = icon("M5 5l10 10M15 5L5 15")
    val Search = icon("M9 3a6 6 0 1 0 0 12a6 6 0 1 0 0-12M13.5 13.5L17 17")
    val Check = icon("M4.375 10.625l3.75 3.75L15.625 5.625")
    val Plus = icon("M10 4v12M4 10h12")
    val Panel = icon("M3.5 4h13v12h-13zM8 4v12")
    val Private = icon(
        "M2.5 11.5a3 3 0 1 0 6 0a3 3 0 1 0-6 0M11.5 11.5a3 3 0 1 0 6 0a3 3 0 1 0-6 0" +
            "M8.5 11c1-.7 2-.7 3 0M3 9.5L5 5M17 9.5L15 5",
    )
    val Extensions = icon(
        "M8 3.5a1.5 1.5 0 0 1 3 0V5h4v4h-1.5a1.5 1.5 0 0 0 0 3H15v4H5v-4h1.5a1.5 1.5 0 0 0 0-3H5V5h3z",
    )
    val Bookmark = icon("M5.5 3h9v14L10 13.5L5.5 17z")
    val BookmarkFilled = icon("M5.5 3h9v14L10 13.5L5.5 17z", filled = true)
    val AddBookmark = icon("M5.5 3h9v14L10 13.5L5.5 17zM10 6.5v5M7.5 9h5")
    val History = icon("M3.5 10a6.5 6.5 0 1 0 1.9-4.6M3.5 3.5v3.3h3.3M10 6.5V10l2.5 1.8")
    val Edit = icon("M13.4 3.6l3 3L7 16H4v-3z")
    val Download = icon("M10 3v10M6 9.5l4 4l4-4M4 16.5h12")
    val Settings = icon(
        "M8.15 4.61L8.93 2.48L11.07 2.48L11.85 4.61L12.51 4.88L14.56 3.92L16.08 5.44L15.12 7.49L15.39 8.15L17.52 8.93L17.52 11.07L15.39 11.85L15.12 12.51L16.08 14.56L14.56 16.08L12.51 15.12L11.85 15.39L11.07 17.52L8.93 17.52L8.15 15.39L7.49 15.12L5.44 16.08L3.92 14.56L4.88 12.51L4.61 11.85L2.48 11.07L2.48 8.93L4.61 8.15L4.88 7.49L3.92 5.44L5.44 3.92L7.49 4.88Z" +
            "M10 7.6a2.4 2.4 0 1 0 0 4.8a2.4 2.4 0 1 0 0-4.8",
    )
    val Secure = icon("M10 2.5l6 2.5v4.5c0 3.8-2.6 6.9-6 8c-3.4-1.1-6-4.2-6-8V5zM7.4 10l1.8 1.8l3.4-3.4")
    val Insecure = icon("M10 2.5l6 2.5v4.5c0 3.8-2.6 6.9-6 8c-3.4-1.1-6-4.2-6-8V5zM10 6.6v4.2M10 13.4v.1")
    val Info = icon("M10 2.8a7.2 7.2 0 1 0 0 14.4a7.2 7.2 0 1 0 0-14.4M10 9v5M10 6.3v.6")
    val Mobile = icon("M6.5 2.5h7a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-7a1 1 0 0 1-1-1v-13a1 1 0 0 1 1-1zM9 14.75h2")
    val Desktop = icon("M2.5 3.5h15v10h-15zM7 16.5h6M10 13.5v3")
    val Exit = icon("M8 3.5H4.5v13H8M12 6.5L15.5 10L12 13.5M15.5 10H7.5")
    val SavePage = icon("M5 2.5h6.5L15 6v11.5H5zM11.5 2.5V6H15M10 8.5v5.5M7.5 11.5l2.5 2.5l2.5-2.5")
    val Share = icon("M10 13V3M6.5 6.5L10 3l3.5 3.5M4 12v4h12v-4")
    val Offline = icon("M2.75 7.75a10.25 10.25 0 0 1 14.5 0M5.25 10.5a6.75 6.75 0 0 1 9.5 0M7.75 13.25a3.25 3.25 0 0 1 4.5 0M10 16v.1M3.5 3.5l13 13")
    val Unreachable = icon("M10 2.75a7.25 7.25 0 1 0 0 14.5a7.25 7.25 0 1 0 0-14.5M2.75 10h14.5M10 2.75c-1.9 2-2.9 4.4-2.9 7.25s1 5.25 2.9 7.25M10 2.75c1.9 2 2.9 4.4 2.9 7.25s-1 5.25-2.9 7.25")
    val Delete = icon("M4 5.5h12M8 5.5v-2h4v2M5.5 5.5l.8 11h7.4l.8-11")
    val More = icon(
        "M10 2.6a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8" +
            "M10 8.6a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8" +
            "M10 14.6a1.4 1.4 0 1 0 0 2.8a1.4 1.4 0 1 0 0-2.8",
        filled = true,
    )
}
