package com.wonderplay.player
import org.junit.Assert.*
import org.junit.Test
class ShuffleOrderTest {
    @Test fun chosenSongStartsOrderAndEveryOccurrenceAppearsOnce() {
        repeat(100) { seed ->
            val order = anchoredShuffleOrder(8, 5, kotlin.random.Random(seed))
            assertEquals(5, order.first()); assertEquals((0..7).toList(), order.sorted())
        }
    }
    @Test fun emptyAndSingleQueuesAreSafe() {
        assertTrue(anchoredShuffleOrder(0, -1).isEmpty()); assertArrayEquals(intArrayOf(0), anchoredShuffleOrder(1, 40))
    }
}
