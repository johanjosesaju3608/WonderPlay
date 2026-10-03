package com.wonderplay.metadata
import org.junit.Assert.*
import org.junit.Test
class ArtworkUrlsTest {
    @Test fun musicCoverRequestsRealLargePixelsInsteadOfStretchingSearchThumbnail() {
        assertEquals("https://yt3.googleusercontent.com/cover=w1024-h1024-l90-rj",ArtworkUrls.forSize("https://yt3.googleusercontent.com/cover=w120-h120-l90-rj",1024))
    }
    @Test fun unrelatedAndLocalArtworkIsPreserved() {
        listOf("file:///cover.jpg","https://example.com/cover=w120-h120","https://i.ytimg.com/vi/id/hqdefault.jpg").forEach { assertEquals(it,ArtworkUrls.forSize(it,1024)) }
    }
}
