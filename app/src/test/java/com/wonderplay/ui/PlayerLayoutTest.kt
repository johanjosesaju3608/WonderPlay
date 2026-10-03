package com.wonderplay.ui

import org.junit.Assert.*
import org.junit.Test

class PlayerLayoutTest {
    @Test fun springOvershootCannotProduceNegativePlayerPadding() {
        for (value in listOf(-.02f, 0f, .4f, 1f, 1.002f, 1.05f)) {
            val fraction = playerLayoutFraction(value)
            assertTrue("Negative padding for spring value $value", 72f * (1f - fraction) >= 0f)
            assertTrue(fraction in 0f..1f)
        }
    }
}
