package com.wonderplay.ui
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.wonderplay.MainActivity
import com.wonderplay.AppViewModel
import com.wonderplay.domain.Track
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MiniPlayerIntegrationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun swipesNavigateExpandDismissAndPreservePlaybackAtBothQueueEnds() {
        val file=File(compose.activity.cacheDir,"gesture-test.wav")
        val samples=44100*30
        val b=ByteBuffer.allocate(44+samples*2).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36+samples*2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(44100).putInt(88200).putShort(2).putShort(16).put("data".toByteArray()).putInt(samples*2)
        repeat(samples){b.putShort(0)};file.writeBytes(b.array())
        lateinit var vm:AppViewModel
        compose.runOnUiThread { vm=ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        val track=Track("local:gesture-one","Gesture one","Fixture",source="local",streamUrl=file.toURI().toString(),durationMs=30000)
        try {
            compose.runOnUiThread {vm.player.play(listOf(track,track.copy(id="local:gesture-two",title="Gesture two")))}
            compose.waitUntil(10000){vm.player.state.value.isPlaying}
            compose.waitUntil(3000) { vm.lyrics.value.trackId == track.id }
            compose.runOnIdle { assertFalse(vm.lyrics.value.loading); assertNull(vm.lyrics.value.lyrics) }
            compose.onNodeWithTag("Mini player").performTouchInput {swipe(center,Offset(center.x,-height.toFloat()),350)}
            compose.onNodeWithTag("Lyrics panel").assertDoesNotExist()
            compose.onNodeWithContentDescription("Close player").performClick()
            compose.runOnUiThread {vm.player.seekTo(8000)}
            compose.waitUntil(3000){vm.player.state.value.positionMs>=7900}
            compose.onNodeWithTag("Mini player").performTouchInput {swipeRight()}
            compose.runOnIdle {assertEquals(0,vm.player.state.value.index);assertTrue(vm.player.state.value.positionMs>=7900);assertTrue(vm.player.state.value.isPlaying)}
            compose.onNodeWithTag("Mini player").performTouchInput {swipeLeft()}
            compose.waitUntil(5000){vm.player.state.value.index==1 && vm.player.state.value.isPlaying}
            compose.runOnUiThread {vm.player.seekTo(8000)}
            compose.waitUntil(3000){vm.player.state.value.positionMs>=7900}
            compose.onNodeWithTag("Mini player").performTouchInput {swipeLeft()}
            compose.runOnIdle {assertEquals(1,vm.player.state.value.index);assertTrue(vm.player.state.value.positionMs>=7900);assertTrue(vm.player.state.value.isPlaying)}
            compose.onNodeWithTag("Mini player").performTouchInput {swipeRight()}
            compose.waitUntil(5000){vm.player.state.value.index==0}
            compose.onNodeWithTag("Mini player").performTouchInput {swipe(center,Offset(center.x,-height.toFloat()),350)}
            compose.onNodeWithContentDescription("Close player").assertIsDisplayed().performClick()
            compose.onNodeWithTag("Mini player").performTouchInput {swipe(center,Offset(center.x,height*2f),350)}
            compose.waitUntil(3000){vm.player.state.value.current==null}
            compose.onNodeWithTag("Mini player").assertDoesNotExist()
            compose.runOnIdle {assertFalse(vm.player.state.value.isPlaying);assertTrue(vm.player.state.value.queue.isEmpty())}
        } finally {compose.runOnUiThread {vm.player.clearQueue()};file.delete()}
    }
}
