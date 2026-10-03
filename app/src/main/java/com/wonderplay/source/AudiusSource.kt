package com.wonderplay.source

import com.wonderplay.domain.*
import com.wonderplay.metadata.MetadataResolver
import com.wonderplay.metadata.SongRelevanceEngine
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject

class AudiusSource : MusicSource {
    override val id = "audius"
    private val http = SourceHttpClient()
    private val ranker = SongRelevanceEngine()
    private suspend fun request(path: String, vararg params: Pair<String,String>): JSONObject {
        val url = "https://api.audius.co/v1/".toHttpUrl().newBuilder()
        path.split('/').forEach { url.addPathSegment(it) }
        url.addQueryParameter("app_name", "wonderPlay")
        params.forEach { url.addQueryParameter(it.first,it.second) }
        return http.json(url.build())
    }
    override suspend fun search(query: String, offset: Int): SearchResult {
        if (query.isBlank()) return SearchResult(emptyList())
        val data = request("tracks/search", "query" to query.take(200), "limit" to "30", "offset" to offset.coerceAtLeast(0).toString()).optJSONArray("data") ?: JSONArray()
        return SearchResult(ranker.rank(parseTracks(data),query),data.length() == 30)
    }
    override suspend fun getTrack(id: String): Track = dataObject(request("tracks/${id.substringAfter(':')}"))?.let(::parseTrack)
        ?: throw SourceException("This track is no longer available for public playback.")
    override suspend fun resolvePlayback(track: Track): PlaybackSource {
        val current = getTrack(track.sourceId)
        return PlaybackSource("https://api.audius.co/v1/tracks/${current.sourceId}/stream?app_name=wonderPlay", "audio/mpeg", "Original stream")
    }
    override suspend fun getPlaylist(id: String): MusicCollection {
        val cleanId = id.substringAfter(':')
        val meta = dataObject(request("playlists/$cleanId")) ?: throw SourceException("This collection is unavailable.")
        val tracks = parseTracks(request("playlists/$cleanId/tracks", "limit" to "100").optJSONArray("data") ?: JSONArray())
        return MusicCollection("audius:$cleanId",meta.optString("playlist_name","Untitled collection"),meta.optJSONObject("user")?.optString("name").orEmpty(),art(meta.optJSONObject("artwork")),tracks,meta.optString("release_date").takeIf { it.length >= 4 }?.take(4))
    }
    override suspend fun getAlbum(id: String) = getPlaylist(id)
    override suspend fun getArtist(id: String): Artist {
        val cleanId = id.substringAfter(':')
        val user=dataObject(request("users/$cleanId")) ?: throw SourceException("This artist is unavailable.")
        val tracks=ranker.rank(parseTracks(request("users/$cleanId/tracks", "limit" to "100").optJSONArray("data") ?: JSONArray()))
        return Artist(cleanId,user.optString("name","Unknown artist"),art(user.optJSONObject("profile_picture")),tracks)
    }
    override suspend fun getRelatedTracks(track: Track): List<Track> = search(track.artist).tracks.filterNot { it.id == track.id }.take(15)
    companion object {
        internal fun dataObject(response: JSONObject): JSONObject? = response.optJSONObject("data") ?: response.optJSONArray("data")?.optJSONObject(0)
        internal fun art(value: JSONObject?): String? = listOf("1000x1000","480x480","150x150").firstNotNullOfOrNull { key -> value?.optString(key)?.takeIf { it.startsWith("https://") } }
        internal fun parseTracks(array: JSONArray): List<Track> = (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(::parseTrack) }
        internal fun parseTrack(json: JSONObject): Track? {
            val id=json.optString("id").takeIf { it.isNotBlank() } ?: return null
            if (json.optBoolean("is_stream_gated") || json.optBoolean("is_delete") || json.optBoolean("is_unlisted") || !json.optBoolean("is_streamable",true) || !json.optBoolean("is_available",true) || json.optJSONObject("access")?.optBoolean("stream",true) == false) return null
            val user=json.optJSONObject("user")
            val album=json.optJSONObject("album_backlink")
            return MetadataResolver.normalize(Track("audius:$id",json.optString("title","Untitled"),user?.optString("name") ?: "Unknown artist",
                album=album?.optString("playlist_name").orEmpty(), artworkUrl=art(json.optJSONObject("artwork")),durationMs=json.optLong("duration").coerceAtLeast(0)*1000,
                sourceId=id,artistId=user?.optString("id")?.takeIf { it.isNotBlank() },albumId=album?.optString("playlist_id")?.takeIf { it.isNotBlank() },
                year=json.optString("release_date").takeIf { it.length>=4 && it.take(4).all(Char::isDigit) }?.take(4),genre=json.optString("genre"),explicit=json.optBoolean("is_explicit") || json.optString("parental_warning_type").equals("Explicit",true),
                permalink=json.optString("permalink").takeIf { it.startsWith('/') }?.let { "https://audius.co$it" }))
        }
    }
}
