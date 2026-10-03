package com.wonderplay.source

import com.wonderplay.domain.MusicCollection
import com.wonderplay.domain.Track
import com.wonderplay.domain.SourceException
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Anonymous, region-aware YouTube Music home feed; no invented playlist IDs. */
class FeaturedPlaylists(private val http: SourceHttpClient = SourceHttpClient()) {
    suspend fun load(): List<MusicCollection> {
        val region = Locale.getDefault().country.takeIf { it.length == 2 } ?: "US"
        val client = JSONObject().put("clientName", "WEB_REMIX").put("clientVersion", "1.20260930.01.00").put("hl", "en").put("gl", region)
        val body = JSONObject().put("context", JSONObject().put("client", client)).put("browseId", "FEmusic_home")
        val response = http.text("https://music.youtube.com/youtubei/v1/browse".toHttpUrl(), body) ?: throw SourceException("Featured playlists are unavailable. Try again.")
        return parse(JSONObject(response)).ifEmpty { throw SourceException("YouTube Music didn't return featured playlists. Try again later.") }
    }
    suspend fun search(query: String): List<MusicCollection> {
        val client = JSONObject().put("clientName", "WEB_REMIX").put("clientVersion", "1.20260930.01.00").put("hl", "en").put("gl", Locale.getDefault().country.takeIf { it.length == 2 } ?: "US")
        // Dedicated featured-playlist filter; the generic playlists filter returns community lists.
        val body = JSONObject().put("context", JSONObject().put("client", client)).put("query", query.take(200))
            .put("params", "EgeKAQQoADgBagwQDhAKEAMQBBAJEAU%3D")
        val response = http.text("https://music.youtube.com/youtubei/v1/search".toHttpUrl(), body) ?: throw SourceException("Playlist search unavailable.")
        return parseSearch(JSONObject(response))
    }
    suspend fun open(id: String): MusicCollection {
        if (!id.matches(Regex("[A-Za-z0-9_-]{10,100}"))) throw SourceException("Invalid playlist.")
        val client = JSONObject().put("clientName", "WEB_REMIX").put("clientVersion", "1.20260930.01.00").put("hl", "en").put("gl", Locale.getDefault().country.takeIf { it.length == 2 } ?: "US")
        val context = JSONObject().put("client", client)
        suspend fun request(body: JSONObject): JSONObject = JSONObject(http.text("https://music.youtube.com/youtubei/v1/browse".toHttpUrl(), body) ?: throw SourceException("Playlist unavailable."))
        val root = request(JSONObject().put("context", context).put("browseId", "VL$id"))
        val tracks = playlistTracks(root).toMutableList()
        var continuation = continuation(root)
        val seen = mutableSetOf<String>()
        var pages = 1
        while (continuation != null && pages < 5 && seen.add(continuation)) {
            val page = request(JSONObject().put("context", context).put("continuation", continuation))
            tracks += playlistTracks(page)
            continuation = continuation(page)
            pages++
        }
        val header = findObject(root, "musicResponsiveHeaderRenderer") ?: findObject(root, "musicDetailHeaderRenderer")
        val title = runs(header?.optJSONObject("title")).ifBlank { "YouTube Music playlist" }
        val thumbnails = header?.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val art = thumbnails?.let { t -> (0 until t.length()).mapNotNull { t.optJSONObject(it) }.maxByOrNull { it.optInt("width") }?.optString("url") }
        return MusicCollection(id, title, if (continuation != null) "First ${tracks.size} tracks" else "YouTube Music", art, tracks)
            .also { if (tracks.isEmpty()) throw SourceException("No playable tracks were returned for this playlist.") }
    }
    companion object {
        private fun runs(value: JSONObject?): String = value?.optJSONArray("runs")?.let { a -> (0 until a.length()).joinToString("") { a.optJSONObject(it)?.optString("text").orEmpty() } }.orEmpty()
        private fun findObject(value: Any?, key: String, depth: Int = 0): JSONObject? {
            if (depth > 35) return null
            return when(value) {
                is JSONObject -> value.optJSONObject(key) ?: value.keys().asSequence().mapNotNull { findObject(value.opt(it), key, depth + 1) }.firstOrNull()
                is JSONArray -> (0 until value.length()).firstNotNullOfOrNull { findObject(value.opt(it), key, depth + 1) }
                else -> null
            }
        }
        internal fun continuation(root: JSONObject): String? {
            // Section-level continuations contain recommendations, not more playlist songs.
            val shelf = findObject(root, "musicPlaylistShelfRenderer") ?: findObject(root, "musicPlaylistShelfContinuation")
                ?: findObject(root, "musicShelfContinuation") ?: findObject(root, "appendContinuationItemsAction") ?: return null
            return findObject(shelf, "nextContinuationData")?.optString("continuation")?.takeIf { it.isNotBlank() }
                ?: findObject(shelf, "continuationCommand")?.optString("token")?.takeIf { it.isNotBlank() }
        }
        internal fun playlistTracks(root: JSONObject): List<Track> {
            val result = mutableListOf<Track>()
            fun visit(value: Any?, depth: Int) {
                if (depth > 35) return
                when(value) {
                    is JSONObject -> {
                        value.optJSONObject("musicResponsiveListItemRenderer")?.let { row ->
                            val id = row.optJSONObject("playlistItemData")?.optString("videoId").orEmpty()
                            fun column(index: Int) = row.optJSONArray("flexColumns")?.optJSONObject(index)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")
                            val title = runs(column(0)); val artist = runs(column(1)); val album = runs(column(2))
                            val duration = runs(row.optJSONArray("fixedColumns")?.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")?.optJSONObject("text"))
                            val seconds = duration.split(':').map { it.toLongOrNull() }.takeIf { it.size in 2..3 && it.all { n -> n != null } }?.fold(0L) { total, part -> total * 60 + (part ?: 0L) } ?: 0L
                            val images = row.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val art = images?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) }.maxByOrNull { it.optInt("width") }?.optString("url") }?.takeIf { it.startsWith("https://") }
                            if (id.matches(Regex("[A-Za-z0-9_-]{11}")) && title.isNotBlank() && seconds > 0) result += Track("youtube:$id", title, artist, album, art, seconds * 1000, source = "youtube", sourceId = id, permalink = "https://music.youtube.com/watch?v=$id")
                        }
                        value.keys().forEach { visit(value.opt(it), depth + 1) }
                    }
                    is JSONArray -> for (i in 0 until value.length()) visit(value.opt(i), depth + 1)
                }
            }
            visit(root.optJSONObject("contents") ?: root.optJSONObject("continuationContents") ?: root, 0)
            return result
        }
        internal fun parseSearch(root: JSONObject): List<MusicCollection> {
            val result = mutableListOf<MusicCollection>()
            fun visit(value: Any?, depth: Int) {
                if (depth > 35 || result.size >= 30) return
                when(value) {
                    is JSONObject -> {
                        value.optJSONObject("musicResponsiveListItemRenderer")?.let { row ->
                            val endpoint = row.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                            val id = endpoint?.optString("browseId").orEmpty().removePrefix("VL")
                            if (YouTubeMusicSource.isOfficialPlaylist(id)) {
                                fun column(i: Int) = row.optJSONArray("flexColumns")?.optJSONObject(i)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")
                                val title = runs(column(0))
                                val images = row.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                val art = images?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) }.maxByOrNull { it.optInt("width") }?.optString("url") }?.takeIf { it.startsWith("https://") }
                                if (title.isNotBlank()) result += MusicCollection(id, title, "Official playlist · ${runs(column(1))}", art)
                            }
                        }
                        value.keys().forEach { visit(value.opt(it), depth + 1) }
                    }
                    is JSONArray -> for(i in 0 until value.length()) visit(value.opt(i), depth + 1)
                }
            }
            visit(root.optJSONObject("contents"), 0)
            return result.distinctBy { it.id }
        }
        internal fun parse(root: JSONObject): List<MusicCollection> {
            val result = mutableListOf<MusicCollection>()
            fun text(value: JSONObject?): String = value?.optJSONArray("runs")?.let { runs -> (0 until runs.length()).joinToString("") { runs.optJSONObject(it)?.optString("text").orEmpty() } }.orEmpty()
            fun visit(value: Any?, depth: Int) {
                if (depth > 35 || result.size >= 40) return
                when(value) {
                    is JSONObject -> {
                        value.optJSONObject("musicTwoRowItemRenderer")?.let { row ->
                            val endpoint = row.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                            val browseId = endpoint?.optString("browseId").orEmpty()
                            val pageType = endpoint?.optJSONObject("browseEndpointContextSupportedConfigs")?.optJSONObject("browseEndpointContextMusicConfig")?.optString("pageType")
                            if (pageType == "MUSIC_PAGE_TYPE_PLAYLIST" && browseId.startsWith("VL") && browseId.drop(2).matches(Regex("[A-Za-z0-9_-]{10,100}"))) {
                                val thumbnails = row.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                val art = thumbnails?.let { t -> (0 until t.length()).mapNotNull { t.optJSONObject(it) }.maxByOrNull { it.optInt("width") }?.optString("url") }?.takeIf { it.startsWith("https://") }
                                val title = text(row.optJSONObject("title"))
                                if (title.isNotBlank()) result += MusicCollection(browseId.drop(2), title, text(row.optJSONObject("subtitle")), art)
                            }
                        }
                        value.keys().forEach { visit(value.opt(it), depth + 1) }
                    }
                    is JSONArray -> for(i in 0 until value.length()) visit(value.opt(i), depth + 1)
                }
            }
            visit(root.optJSONObject("contents"), 0)
            return result.distinctBy { it.id }
        }
    }
}
