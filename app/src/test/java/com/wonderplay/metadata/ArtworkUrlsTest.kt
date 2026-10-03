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
    @Test fun squareSizeMusicUrlsAndVideoCandidatesUseSharperImages() {
        assertEquals("https://lh3.googleusercontent.com/cover=s1024-c", ArtworkUrls.forSize("https://lh3.googleusercontent.com/cover=s120-c", 1024))
        assertEquals("https://i.ytimg.com/vi/id/maxresdefault.jpg", ArtworkUrls.forSize("https://i.ytimg.com/vi/id/hqdefault.jpg", 1024, upgradeVideo=true))
        assertEquals("https://i.ytimg.com/vi/id/hqdefault.jpg", ArtworkUrls.forSize("https://i.ytimg.com/vi/id/hqdefault.jpg", 128, upgradeVideo=true))
    }

}
