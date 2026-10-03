package com.wonderplay.ui
import org.junit.Assert.*
import org.junit.Test
class MiniPlayerGestureTest {
    @Test fun dominantDirectionChoosesExactlyOneAction() {
        assertEquals(MiniPlayerGesture.EXPAND,miniPlayerGesture(15f,-100f,48f))
        assertEquals(MiniPlayerGesture.DISMISS,miniPlayerGesture(10f,100f,48f))
        assertEquals(MiniPlayerGesture.PREVIOUS,miniPlayerGesture(120f,12f,48f))
        assertEquals(MiniPlayerGesture.NEXT,miniPlayerGesture(-120f,12f,48f))
        assertEquals(MiniPlayerGesture.NONE,miniPlayerGesture(20f,10f,48f))
    }
}
