package com.wonderplay.metadata

import com.wonderplay.domain.Track
import java.text.Normalizer
import java.util.Locale

/** Cleans publishing clutter while keeping live, acoustic, remix and remaster editions distinct. */
object MetadataResolver {
    private val whitespace = Regex("\\s+")
    private val decoration = Regex("[\\[(](?:official(?:\\s+(?:music|lyric))?\\s*(?:audio|video)?|lyrics?|audio only|visuali[sz]er|hd|4k)[\\])]", RegexOption.IGNORE_CASE)
    private val suffix = Regex("\\s*[-–—|]\\s*(?:official(?:\\s+music)?\\s+(?:audio|video)|lyrics?|visuali[sz]er|hd|4k)\\s*$", RegexOption.IGNORE_CASE)
    private val version = Regex("\\b(live|acoustic|remix|remaster(?:ed)?|instrumental|radio edit|extended|sped up|slowed|cover|demo)\\b", RegexOption.IGNORE_CASE)
    fun clean(value: String): String = whitespace.replace(value.replace('\u00a0', ' '), " ").trim()
    fun key(value: String): String = Normalizer.normalize(clean(value), Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
    fun normalize(track: Track): Track {
        var artist = clean(track.artist).removeSuffix(" - Topic").trim().ifBlank { "Unknown artist" }
        var title = clean(suffix.replace(decoration.replace(track.title, ""), ""))
        // Split only when a supplied artist is an exact match; arbitrary hyphens are song content.
        listOf(" – ", " — ", " - ").firstOrNull { title.contains(it) }?.let { separator ->
            val parts = title.split(separator, limit = 2)
            if (key(parts[0]) == key(artist)) title = parts[1].trim()
        }
        if (title.contains(" | ")) {
            val parts = title.split(" | ", limit = 2)
            if (key(parts[1]) == key(artist)) title = parts[0].trim()
        }
        title = title.replace(Regex("\\b(?:ft\\.?|featuring)\\s+", RegexOption.IGNORE_CASE), "feat. ")
        return track.copy(title = title.ifBlank { "Untitled" }, artist = artist, album = clean(track.album), durationMs = track.durationMs.coerceAtLeast(0))
    }
    fun versionKey(title: String): String = version.findAll(title).joinToString(" ") { it.value.lowercase(Locale.ROOT) }
    fun sameRecording(left: Track, right: Track): Boolean {
        if (left.id == right.id) return true
        val a = normalize(left); val b = normalize(right)
        return key(a.title) == key(b.title) && key(a.artist) == key(b.artist) && a.explicit == b.explicit &&
            versionKey(a.title) == versionKey(b.title) &&
            (a.durationMs == 0L || b.durationMs == 0L || kotlin.math.abs(a.durationMs - b.durationMs) <= 3_000)
    }
}
