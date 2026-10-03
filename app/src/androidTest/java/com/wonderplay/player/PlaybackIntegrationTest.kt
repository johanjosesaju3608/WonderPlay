package com.wonderplay.player

import android.content.ComponentName
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wonderplay.WonderPlayApp
import com.wonderplay.domain.Track
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real Media3 decoding/session test; synthetic PCM exists only in the test sandbox. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class PlaybackIntegrationTest {
    @Test fun localAudioPlaysSeeksPausesAndReportsFailure() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = ApplicationProvider.getApplicationContext<WonderPlayApp>()
        val file = File(app.cacheDir, "playback-test.wav")
        val samples = 44_100 * 10
        val data = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        data.put("RIFF".toByteArray()).putInt(36 + samples * 2).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(44_100).putInt(88_200)
            .putShort(2).putShort(16).put("data".toByteArray()).putInt(samples * 2)
        repeat(samples) { data.putShort(0) }
        file.writeBytes(data.array())
        lateinit var controller: MediaController
        val future = MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java))).buildAsync()
        controller = future.get(15, TimeUnit.SECONDS)
        try {
            instrumentation.runOnMainSync {
                controller.setMediaItem(TrackMediaCodec.item(Track("local:test", "Playback test", "Test fixture", source = "local", streamUrl = file.toURI().toString())))
                controller.prepare()
                controller.play()
            }
            await(15_000) { var ready = false; instrumentation.runOnMainSync { ready = controller.isPlaying }; ready }
            instrumentation.runOnMainSync {
                assertTrue(controller.duration >= 9_900)
                controller.seekTo(4_000)
                controller.pause()
            }
            await(3_000) { var paused = false; instrumentation.runOnMainSync { paused = !controller.isPlaying && controller.currentPosition >= 3_900 }; paused }
            instrumentation.runOnMainSync {
                controller.setMediaItem(TrackMediaCodec.item(Track("local:missing", "Unavailable", "Test fixture", source = "local", streamUrl = File(app.cacheDir, "missing-test-audio.wav").toURI().toString())))
                controller.prepare()
                controller.play()
            }
            await(10_000) { var failed = false; instrumentation.runOnMainSync { failed = controller.playerError != null }; failed }
            instrumentation.runOnMainSync {
                controller.setMediaItem(TrackMediaCodec.item(Track("local:test", "Playback test", "Test fixture", source = "local", streamUrl = file.toURI().toString())))
                controller.prepare()
                controller.play()
            }
            await(10_000) { var playing = false; instrumentation.runOnMainSync { playing = controller.isPlaying }; playing }
        } finally {
            instrumentation.runOnMainSync { controller.stop(); controller.clearMediaItems(); controller.release() }
            file.delete()
        }
    }

    private fun await(timeoutMs: Long, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
        fail("Playback condition did not become true within $timeoutMs ms")
    }
}
