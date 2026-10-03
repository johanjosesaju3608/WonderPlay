package com.wonderplay.player

/** Starting shuffle must leave every other queue occurrence available afterwards. */
internal fun anchoredShuffleOrder(count: Int, current: Int, random: kotlin.random.Random = kotlin.random.Random.Default): IntArray {
    if (count <= 0) return intArrayOf()
    val first = current.coerceIn(0, count - 1)
    return (listOf(first) + (0 until count).filter { it != first }.shuffled(random)).toIntArray()
}
