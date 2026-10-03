package com.wonderplay.player

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.wonderplay.AppViewModel
import com.wonderplay.MainActivity
import com.wonderplay.domain.Track
import com.wonderplay.domain.RepeatMode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ShuffleIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun shuffleStartsWithChosenTrackAndVisitsEveryOtherTrackOnce() {
        val file = File(compose.activity.cacheDir, "shuffle-test.wav")
        val samples = 44100 * 30
        val b = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(44100).putInt(88200).putShort(2).putShort(16).put("data".toByteArray()).putInt(samples * 2)
        repeat(samples) { b.putShort(0) }; file.writeBytes(b.array())
        lateinit var vm: AppViewModel
        compose.runOnUiThread { vm = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        val tracks = (0..5).map { Track("local:shuffle-$it", "Shuffle $it", "Fixture", source = "local", streamUrl = file.toURI().toString(), durationMs = 30000) }
        try {
            compose.runOnUiThread { vm.player.setRepeat(RepeatMode.OFF); vm.player.play(tracks, 3); vm.player.setShuffle(true) }
            compose.waitUntil(10000) { vm.player.state.value.isPlaying && vm.player.state.value.shuffle && vm.player.state.value.playbackOrder.firstOrNull() == 3 }
            val order = vm.player.state.value.playbackOrder
            assertEquals((0..5).toList(), order.sorted())
            val visited = mutableListOf(vm.player.state.value.index)
            for (expected in order.drop(1)) {
                compose.runOnUiThread { vm.player.next() }
                compose.waitUntil(5000) { vm.player.state.value.index == expected && vm.player.state.value.isPlaying }
                visited += vm.player.state.value.index
            }
            assertEquals(6, visited.distinct().size)
            compose.runOnUiThread { vm.player.setRepeat(RepeatMode.ALL) }
            compose.waitUntil(3000) { vm.player.state.value.repeat == RepeatMode.ALL }
            compose.runOnUiThread { android.util.Log.d("ShuffleTest", "Before wrap: index=${vm.player.state.value.index}, order=${vm.player.state.value.playbackOrder}, repeat=${vm.player.state.value.repeat}"); vm.player.next() }
            try { compose.waitUntil(5000) { vm.player.state.value.index == 3 && vm.player.state.value.isPlaying } }
            catch (error: Exception) { throw AssertionError("Wrap failed: index=${vm.player.state.value.index}, order=${vm.player.state.value.playbackOrder}, repeat=${vm.player.state.value.repeat}, phase=${vm.player.state.value.phase}", error) }
            compose.runOnUiThread { vm.player.seekTo(8000); vm.player.setShuffle(false) }
            compose.waitUntil(3000) { !vm.player.state.value.shuffle && vm.player.state.value.positionMs >= 7900 }
            assertEquals((0..5).toList(), vm.player.state.value.playbackOrder)
        } finally { compose.runOnUiThread { vm.player.setRepeat(RepeatMode.OFF); vm.player.clearQueue() }; file.delete() }
    }
}
