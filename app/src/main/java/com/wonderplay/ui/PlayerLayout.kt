package com.wonderplay.ui

/** Springs can overshoot, but sizes, padding and opacity must stay bounded. */
internal fun playerLayoutFraction(animated: Float): Float = animated.coerceIn(0f, 1f)
