package com.wonderplay.metadata

import com.wonderplay.domain.Track
import com.wonderplay.source.SourceHttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Bounded canonical fallback. Creator-supplied square artwork already has priority. */
class ArtworkResolver {
    private val mutex=Mutex()
    private val cache=java.util.Collections.synchronizedMap(object:LinkedHashMap<String,String?>(64,.75f,true) {
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,String?>?) = size>64
    })
    private var lastRequest=0L
    private val http=SourceHttpClient()
    fun clear() = cache.clear()
    suspend fun resolve(track:Track):String? = mutex.withLock {
        if(track.artworkUrl!=null) return@withLock track.artworkUrl
        val key=MetadataResolver.key(track.artist+" "+track.title)
        if(cache.containsKey(key)) return@withLock cache[key]
        val wait=1100-(android.os.SystemClock.elapsedRealtime()-lastRequest)
        if(wait>0) delay(wait)
        lastRequest=android.os.SystemClock.elapsedRealtime()
        val result=try {
            val artist=track.artist.replace("\"",""); val title=track.title.replace("\"","")
            val url="https://musicbrainz.org/ws/2/recording".toHttpUrl().newBuilder().addQueryParameter("query","recording:\"$title\" AND artist:\"$artist\"").addQueryParameter("fmt","json").addQueryParameter("limit","3").build()
            val recordings=http.json(url).optJSONArray("recordings")
            val match=(0 until (recordings?.length() ?: 0)).mapNotNull { recordings?.optJSONObject(it) }.firstOrNull {
                it.optInt("score")>=95 && MetadataResolver.key(it.optString("title"))==MetadataResolver.key(track.title) &&
                    it.optJSONArray("artist-credit")?.optJSONObject(0)?.optString("name")?.let(MetadataResolver::key)==MetadataResolver.key(track.artist)
            }
            val release=match?.optJSONArray("releases")?.optJSONObject(0)?.optString("id")
            release?.takeIf { it.matches(Regex("[a-f0-9-]{36}")) }?.let { "https://coverartarchive.org/release/$it/front-500" }
        } catch(cancelled:kotlinx.coroutines.CancellationException) { throw cancelled } catch(_:Exception) { null }
        cache[key]=result; result
    }
}
