package com.wonderplay.domain

import com.wonderplay.metadata.MetadataResolver

/** On-device artist affinity from recent listening and favorites; no account or tracking service. */
object Recommendations {
    data class Seed(val artist: String, val weight: Double)
    private fun artistKey(name: String) = MetadataResolver.key(name.removeSuffix(" - Topic").trim())
    fun seeds(history: List<Track>, favorites: List<Track>): List<Seed> {
        val scores = linkedMapOf<String, Seed>()
        fun add(track: Track, weight: Double) {
            val key = artistKey(track.artist)
            if(track.source != "youtube" || key.isBlank() || key == "unknown artist") return
            val old = scores[key]
            scores[key] = Seed(old?.artist ?: track.artist, (old?.weight ?: 0.0) + weight)
        }
        history.take(30).forEachIndexed { index, track -> add(track, 4.0 / (1.0 + index / 3.0)) }
        favorites.forEach { add(it, 2.0) }
        return scores.values.sortedByDescending { it.weight }.take(3)
    }
    fun rank(candidates: List<Track>, history: List<Track>, favorites: List<Track>): List<Track> {
        val seeds = seeds(history, favorites)
        val known = history + favorites
        val familiar = known.map { it.id }.toSet()
        fun weight(track: Track) = seeds.filter { (" " + artistKey(track.artist) + " ").contains(" " + artistKey(it.artist) + " ") }.maxOfOrNull { it.weight } ?: 0.0
        val counts = mutableMapOf<String, Int>()
        return candidates.distinctBy { it.id }.filter { it.source == "youtube" && it.id !in familiar && known.none { old -> MetadataResolver.sameRecording(old, it) } && (seeds.isEmpty() || weight(it) > 0.0) }
            .sortedByDescending(::weight).filter {
                val key = artistKey(it.artist); val count = counts.getOrDefault(key, 0)
                counts[key] = count + 1; count < 4
            }.take(12)
    }
}
