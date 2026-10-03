package com.wonderplay.ui

/** Springs can overshoot, but sizes, padding and opacity must stay bounded. */
internal fun playerLayoutFraction(animated: Float): Float = animated.coerceIn(0f, 1f)

internal enum class MiniPlayerGesture { NONE, EXPAND, DISMISS, PREVIOUS, NEXT }
internal fun miniPlayerGesture(dx:Float,dy:Float,threshold:Float):MiniPlayerGesture = when {
    kotlin.math.max(kotlin.math.abs(dx),kotlin.math.abs(dy)) < threshold -> MiniPlayerGesture.NONE
    kotlin.math.abs(dy)>kotlin.math.abs(dx) -> if(dy<0) MiniPlayerGesture.EXPAND else MiniPlayerGesture.DISMISS
    dx>0 -> MiniPlayerGesture.PREVIOUS
    else -> MiniPlayerGesture.NEXT
}
