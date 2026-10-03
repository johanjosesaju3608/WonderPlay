package com.wonderplay.metadata

import com.wonderplay.domain.Track

/** A small, deterministic local classifier. No cloud inference, model download, or user identifiers. */
class SongRelevanceEngine {
    private val negativeTerms = Regex("\\b(reaction|interview|podcast|gameplay|tutorial|vlog|review|news|audiobook|shorts)\\b", RegexOption.IGNORE_CASE)
    private val nonMusicGenres = setOf("podcasts", "spoken word", "comedy", "audiobooks")
    data class ScoredTrack(val track: Track, val musicConfidence: Double, val queryRelevance: Double)

    fun score(track: Track, query: String = ""): ScoredTrack {
        var confidence = if (track.source == "audius" || track.source == "local") 0.76 else 0.5
        if (track.durationMs in 45_000..900_000) confidence += 0.13
        if (track.durationMs in 1..14_999) confidence -= 0.35
        if (track.durationMs > 7_200_000) confidence -= 0.2
        if (track.artist.isNotBlank() && track.artist != "Unknown artist") confidence += 0.04
        if (track.album.isNotBlank()) confidence += 0.04
        if (negativeTerms.containsMatchIn(track.title)) confidence -= 0.48
        if (MetadataResolver.key(track.genre.orEmpty()) in nonMusicGenres) confidence -= 0.7
        val normalized = MetadataResolver.normalize(track)
        val queryTokens = MetadataResolver.key(query).split(' ').filter { it.isNotEmpty() }.toSet()
        val haystack = MetadataResolver.key("${normalized.title} ${normalized.artist} ${normalized.album}")
        val relevance = if (queryTokens.isEmpty()) 0.0 else queryTokens.count { it in haystack }.toDouble() / queryTokens.size
        return ScoredTrack(normalized, confidence.coerceIn(0.0, 1.0), relevance)
    }

    fun rank(tracks: List<Track>, query: String = ""): List<Track> {
        val candidates = tracks.map { score(it, query) }.filter { it.musicConfidence >= 0.45 }
            .sortedByDescending { it.queryRelevance * 2 + it.musicConfidence }
        val buckets = mutableMapOf<String, MutableList<Track>>()
        val output = ArrayList<Track>(candidates.size)
        for (candidate in candidates) {
            val track = candidate.track
            val key = "${MetadataResolver.key(track.artist)}|${MetadataResolver.key(track.title)}"
            val sameKey = buckets.getOrPut(key) { mutableListOf() }
            if (sameKey.none { MetadataResolver.sameRecording(it, track) }) {
                sameKey.add(track); output.add(track)
            }
        }
        return output
    }
}
