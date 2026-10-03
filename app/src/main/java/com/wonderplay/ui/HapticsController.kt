package com.wonderplay.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf

internal enum class HapticEvent { TAP, SELECT, FAVORITE, SKIP, DRAG_START, REORDER, SEEK, DISMISS }

/** Respects both app preferences and Android's global touch-feedback setting. */
internal class HapticsController(private val view: View?, private val enabled: Boolean) {
    fun perform(event: HapticEvent) {
        if (!enabled) return
        val feedback = when (event) {
            HapticEvent.DRAG_START -> HapticFeedbackConstants.LONG_PRESS
            HapticEvent.FAVORITE, HapticEvent.SELECT -> HapticFeedbackConstants.CONTEXT_CLICK
            HapticEvent.REORDER, HapticEvent.SEEK, HapticEvent.SKIP -> HapticFeedbackConstants.CLOCK_TICK
            HapticEvent.TAP, HapticEvent.DISMISS -> HapticFeedbackConstants.KEYBOARD_TAP
        }
        view?.performHapticFeedback(feedback)
    }
}
internal val LocalWonderHaptics = staticCompositionLocalOf { HapticsController(null, false) }
