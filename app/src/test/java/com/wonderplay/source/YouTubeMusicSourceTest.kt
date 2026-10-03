package com.wonderplay.source

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

@org.robolectric.annotation.Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class YouTubeMusicSourceTest {
    @Test fun musicMetadataProducesCorrectPlayableIdentity() {
        val item = StreamInfoItem(0,"https://www.youtube.com/watch?v=dQw4w9WgXcQ","Never Gonna Give You Up",StreamType.VIDEO_STREAM)
        item.uploaderName="Rick Astley";item.duration=213
        item.thumbnails=listOf(Image("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",480,360,Image.ResolutionLevel.MEDIUM))
        val track=YouTubeMusicSource.track(item)!!
        assertEquals("youtube:dQw4w9WgXcQ",track.id)
        assertEquals("youtube",track.source)
        assertEquals(213000L,track.durationMs)
        assertEquals("Rick Astley",track.artist)
    }
    @Test fun malformedAndLiveEntriesAreNotMusicTracks() {
        val item=StreamInfoItem(0,"https://www.youtube.com/watch?v=bad","Invalid",StreamType.VIDEO_STREAM)
        item.duration=100
        assertNull(YouTubeMusicSource.track(item))
        assertThrows(com.wonderplay.domain.SourceException::class.java) {YouTubeMusicSource.validId("../not-a-track")}
    }
}
