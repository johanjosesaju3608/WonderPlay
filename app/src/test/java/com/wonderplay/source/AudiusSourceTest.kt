package com.wonderplay.source
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONObject
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AudiusSourceTest {
    private fun track(extra: String = "") = JSONObject("""{"id":"abc","title":"Song (Official Audio)","duration":180,"user":{"id":"artist","name":"Artist"},"is_streamable":true,"artwork":{"1000x1000":"https://example.org/art.jpg"}$extra}""")
    @Test fun parseNormalizesRealTrackAndDuration() { val t = AudiusSource.parseTrack(track())!!; assertEquals("Song",t.title); assertEquals(180000L,t.durationMs); assertEquals("audius:abc",t.id) }
    @Test fun rejectsGatedDeletedAndUnavailableTracks() {
        for (field in listOf(",\"is_stream_gated\":true",",\"is_delete\":true",",\"is_streamable\":false",",\"access\":{\"stream\":false}")) assertNull(AudiusSource.parseTrack(track(field)))
    }
    @Test fun supportsBothSingleAndLegacyArrayEnvelopes() {
        val item=track()
        assertEquals("abc",AudiusSource.dataObject(JSONObject().put("data",item))?.getString("id"))
        assertEquals("abc",AudiusSource.dataObject(JSONObject().put("data",org.json.JSONArray().put(item)))?.getString("id"))
    }
    @Test fun missingIdentityIsRejected() { assertNull(AudiusSource.parseTrack(JSONObject("{}"))) }
}
