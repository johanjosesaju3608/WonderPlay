package com.wonderplay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val Coffee = Color(0xFFF1DBC5)
internal val LocalPlayerGradient = staticCompositionLocalOf { listOf(Color(0xFF383028), Color(0xFF181818)) }

/** Small software decode, reused by Coil and computed off the UI thread. No full-size bitmap retained. */
@Composable
internal fun albumAccent(artwork: String?, enabled: Boolean, dark: Boolean): Color {
    val context = LocalContext.current
    val seed by produceState<Int?>(null, artwork, enabled) {
        value = null
        if (enabled && !artwork.isNullOrBlank()) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val result = context.imageLoader.execute(ImageRequest.Builder(context).data(artwork).size(96,96).allowHardware(false).build())
                    val bitmap = (result.image as? BitmapImage)?.bitmap ?: return@runCatching null
                    val palette = Palette.from(bitmap).maximumColorCount(12).generate()
                    palette.vibrantSwatch?.rgb ?: palette.mutedSwatch?.rgb ?: palette.dominantSwatch?.rgb
                }.getOrNull()
            }
        }
    }
    val target = seed?.let { readableAlbumColor(it, dark) } ?: if (dark) 0xFFF1DBC5.toInt() else 0xFF6B4930.toInt()
    val color by animateColorAsState(Color(target), tween(if(LocalReducedMotion.current) 0 else 450), label="album accent")
    return color
}

internal fun readableAlbumColor(seed: Int, dark: Boolean): Int {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed, hsl)
    hsl[1] = hsl[1].coerceIn(.18f, .65f)
    hsl[2] = if (dark) .78f else .32f
    val background = if(dark) 0xFF181818.toInt() else 0xFFFCF8F2.toInt()
    var color = ColorUtils.HSLToColor(hsl)
    repeat(20) {
        if(ColorUtils.calculateContrast(color, background) >= 4.5) return color
        hsl[2] = (hsl[2] + if(dark) .02f else -.02f).coerceIn(.1f,.92f)
        color = ColorUtils.HSLToColor(hsl)
    }
    return color
}
