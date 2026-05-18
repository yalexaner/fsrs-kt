package io.github.yalexaner.fsrs.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultWeightsTest {

    @Test
    fun defaultWeightArrayHasExactly21Elements() {
        assertEquals(expected = 21, actual = DEFAULT_W.size)
    }

    @Test
    fun spotCheckFirstAndLastWeights() {
        assertEquals(expected = 0.212, actual = DEFAULT_W[0], absoluteTolerance = 1e-9)
        assertEquals(expected = 0.1542, actual = DEFAULT_W[20], absoluteTolerance = 1e-9)
    }
}
