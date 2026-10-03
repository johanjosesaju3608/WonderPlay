package com.wonderplay.ui

import androidx.core.graphics.ColorUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@org.robolectric.annotation.Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AlbumColorsTest {
    @Test fun coverAccentRemainsReadableOnBothThemes() {
        for (seed in listOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF00FF00.toInt())) {
            val dark = readableAlbumColor(seed, true)
            val light = readableAlbumColor(seed, false)
            assertTrue(ColorUtils.calculateContrast(dark, 0xFF181818.toInt()) >= 4.5)
            assertTrue(ColorUtils.calculateContrast(light, 0xFFFCF8F2.toInt()) >= 4.5)
        }
    }
}
