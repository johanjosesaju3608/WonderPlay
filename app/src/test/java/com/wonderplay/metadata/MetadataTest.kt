package com.wonderplay.metadata
import com.wonderplay.domain.Track
import org.junit.Test
import org.junit.Assert.*
class MetadataTest {
    private fun song(id: String, title: String) = Track(id,title,"Artist",durationMs=180000)
    @Test fun cleansPublishingClutterButPreservesVersion() {
        assertEquals("Song (Live)", MetadataResolver.normalize(song("1","Artist – Song (Live) [Lyrics]")).title)
        assertEquals("Song feat. Guest",MetadataResolver.normalize(song("1","Song ft Guest")).title)
    }
    @Test fun duplicateRemovalDoesNotMergeRemixes() {
        val result = SongRelevanceEngine().rank(listOf(song("1","Song"),song("2","Song (Official Audio)"),song("3","Song (Remix)")))
        assertEquals(2,result.size)
    }
    @Test fun filtersSpokenContentAndRanksQuery() {
        val result=SongRelevanceEngine().rank(listOf(song("1","Artist interview").copy(genre="Podcasts"), song("2","Other"),song("3","Sunset")), "Sunset")
        assertEquals(listOf("3","2"),result.map { it.id })
    }
}
