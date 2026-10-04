package com.wonderplay.domain

import org.junit.Assert.*
import org.junit.Test

class RecommendationsTest {
    private fun song(id: String, artist: String = "Artist A", source: String = "youtube") = Track(id, id, artist, source = source)
    @Test fun recentListeningAndFavoritesChooseSeedsWithoutLocalFiles() {
        val seeds = Recommendations.seeds(listOf(song("latest", "Artist B"), song("old"), song("local", "Private artist", "local")), listOf(song("favorite", "Artist B")))
        assertEquals("Artist B", seeds.first().artist)
        assertFalse(seeds.any { it.artist == "Private artist" })
    }
    @Test fun excludesKnownSongsLocalFilesAndUnrelatedArtists() {
        val played = song("played")
        val favorite = song("favorite")
        val newSong = song("new")
        assertEquals(listOf(newSong), Recommendations.rank(listOf(played, favorite, newSong, song("unrelated", "Other"), song("local", source = "local"), newSong), listOf(played), listOf(favorite)))
    }
    @Test fun limitsOneArtistAndPrioritizesRecentAffinity() {
        val a = (0..6).map { song("a$it") }
        val b = (0..6).map { song("b$it", "Artist B") }
        val ranked = Recommendations.rank(a + b, listOf(song("last", "Artist B"), song("older")), emptyList())
        assertEquals("Artist B", ranked.first().artist)
        assertEquals(4, ranked.count { it.artist == "Artist A" })
        assertEquals(4, ranked.count { it.artist == "Artist B" })
    }
    @Test fun emptyHistoryAllowsDiscoveryAndTopicSuffixMatches() {
        assertEquals(1, Recommendations.rank(listOf(song("new")), emptyList(), emptyList()).size)
        assertEquals(1, Recommendations.rank(listOf(song("new", "Artist A - Topic")), listOf(song("played")), emptyList()).size)
    }
    @Test fun artistAffinityMatchesWholeNamesInsteadOfSubstrings() {
        assertTrue(Recommendations.rank(listOf(song("new", "Asian Artist")), listOf(song("played", "Sia")), emptyList()).isEmpty())
    }
    @Test fun skipsAlternateUploadsOfKnownRecording() {
        val known = Track("old-id", "A song", "Artist A", durationMs = 180000)
        val duplicate = known.copy(id = "alternate-id", durationMs = 181000)
        assertTrue(Recommendations.rank(listOf(duplicate), listOf(known), emptyList()).isEmpty())
    }

}
