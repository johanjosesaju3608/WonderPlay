package com.wonderplay.source

import com.wonderplay.domain.Track
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LyricsTest {
    @Test fun parsesMultipleStampsFractionsOffsetsAndIgnoresMetadata() {
        val result = LrcParser.parse("[ar:Test]\n[offset:-100]\n[00:01.2][00:03.45]First\n[00:02.005]Second\n[00:99.00]Invalid")
        assertEquals(listOf(1100L, 1905L, 3350L), result.map { it.timeMs })
        assertEquals(listOf("First", "Second", "First"), result.map { it.text })
    }
    @Test fun highlightsOnlyAtTimestampAndFollowsBackwardSeek() {
        val lines = LrcParser.parse("[00:05]First\n[00:10]Second\n[00:20]Third")
        assertEquals(-1, LrcParser.activeIndex(lines, 4999))
        assertEquals(0, LrcParser.activeIndex(lines, 5000))
        assertEquals(2, LrcParser.activeIndex(lines, 22000))
        assertEquals(1, LrcParser.activeIndex(lines, 10001))
        assertEquals(-1, LrcParser.activeIndex(emptyList(), 1000))
    }
    @Test fun preventsWrongVersionAndArtistMatches() {
        val track = Track("test", "Song (Official Audio)", "Artist - Topic", durationMs = 180000)
        val record = JSONObject().put("trackName", "Song").put("artistName", "Artist").put("duration", 183)
        assertTrue(LyricsRepository.matches(track, record))
        assertFalse(LyricsRepository.matches(track, record.put("duration", 210)))
        assertFalse(LyricsRepository.matches(track, record.put("duration", 180).put("artistName", "Other")))
        assertFalse(LyricsRepository.matches(track.copy(title = "Song (Live)"), record.put("artistName", "Artist")))
    }
    @Test fun nullLyricsAreNotShownAndPlainLyricsStayUnsynced() {
        assertNull(LyricsRepository.decode(JSONObject("{\"plainLyrics\":null,\"syncedLyrics\":null}")))
        val plain = LyricsRepository.decode(JSONObject().put("plainLyrics", "Test words"))!!
        assertTrue(plain.lines.isEmpty()); assertEquals("Test words", plain.plain)
        assertTrue(LyricsRepository.decode(JSONObject().put("instrumental", true))!!.instrumental)
    }
    @Test fun featuredOnlyIncludesRealPlaylistEndpoints() {
        fun row(id: String, type: String) = JSONObject().put("musicTwoRowItemRenderer", JSONObject().put("title", JSONObject().put("runs", org.json.JSONArray().put(JSONObject().put("text", "Test playlist"))))
            .put("navigationEndpoint", JSONObject().put("browseEndpoint", JSONObject().put("browseId", id).put("browseEndpointContextSupportedConfigs", JSONObject().put("browseEndpointContextMusicConfig", JSONObject().put("pageType", type))))))
        val items = org.json.JSONArray().put(row("VLPL0123456789", "MUSIC_PAGE_TYPE_PLAYLIST")).put(row("VLPL0123456789", "MUSIC_PAGE_TYPE_PLAYLIST")).put(row("MPRE0123456789", "MUSIC_PAGE_TYPE_ALBUM"))
        val result = FeaturedPlaylists.parse(JSONObject().put("contents", JSONObject().put("items", items)))
        assertEquals(1, result.size); assertEquals("PL0123456789", result.single().id)
    }
    @Test fun playlistUsesCanonicalMusicMetadataAndDuration() {
        fun column(text: String) = JSONObject().put("musicResponsiveListItemFlexColumnRenderer", JSONObject().put("text", JSONObject().put("runs", org.json.JSONArray().put(JSONObject().put("text", text)))))
        val row = JSONObject().put("playlistItemData", JSONObject().put("videoId", "abcdefghijk"))
            .put("flexColumns", org.json.JSONArray().put(column("Canonical title")).put(column("Song artist")).put(column("Album")))
            .put("fixedColumns", org.json.JSONArray().put(JSONObject().put("musicResponsiveListItemFixedColumnRenderer", JSONObject().put("text", JSONObject().put("runs", org.json.JSONArray().put(JSONObject().put("text", "3:25")))))))
        val result = FeaturedPlaylists.playlistTracks(JSONObject().put("contents", JSONObject().put("musicResponsiveListItemRenderer", row)))
        assertEquals("Canonical title", result.single().title)
        assertEquals("Song artist", result.single().artist)
        assertEquals(205000L, result.single().durationMs)
    }

    @Test fun playlistPagingIgnoresRecommendationContinuation() {
        val token = JSONObject().put("nextContinuationData", JSONObject().put("continuation", "song-page"))
        val section = JSONObject().put("continuations", org.json.JSONArray().put(token))
        assertNull(FeaturedPlaylists.continuation(section))
        assertEquals("song-page", FeaturedPlaylists.continuation(JSONObject().put("musicPlaylistShelfRenderer", section)))
    }

}
