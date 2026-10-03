package com.wonderplay.player
import com.wonderplay.domain.Track
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemArtworkTest {
    @Test fun mediaSessionReceivesHighResolutionArtworkAndPreservesTrack() {
        val track = Track("youtube:abcdefghijk", "Song", "Artist", artworkUrl = "https://lh3.googleusercontent.com/cover=w120-h120-l90-rj")
        val item = TrackMediaCodec.item(track)
        assertTrue(item.mediaMetadata.artworkUri.toString().contains("w1024-h1024"))
        assertEquals(track, TrackMediaCodec.track(item))
    }
}
