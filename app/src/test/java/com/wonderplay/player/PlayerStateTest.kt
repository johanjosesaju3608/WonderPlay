package com.wonderplay.player
import androidx.media3.common.Player
import com.wonderplay.domain.PlaybackPhase
import org.junit.Test
import org.junit.Assert.*
class PlayerStateTest {
    @Test fun failureTakesPriorityOverResolving() { assertEquals(PlaybackPhase.ERROR,mapPhase(Player.STATE_IDLE,false,true,true,true)) }
    @Test fun restoredQueueStaysPaused() { assertEquals(PlaybackPhase.PAUSED,mapPhase(Player.STATE_IDLE,false,true,false,false)) }
    @Test fun emptyQueueIsIdle() { assertEquals(PlaybackPhase.IDLE,mapPhase(Player.STATE_IDLE,false,false,false,false)) }
    @Test fun endAndBufferingAreDistinct() { assertEquals(PlaybackPhase.ENDED,mapPhase(Player.STATE_ENDED,false,true,false,false)); assertEquals(PlaybackPhase.BUFFERING,mapPhase(Player.STATE_BUFFERING,false,true,false,false)) }
}
