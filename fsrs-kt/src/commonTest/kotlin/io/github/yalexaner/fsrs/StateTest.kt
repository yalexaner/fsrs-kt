package io.github.yalexaner.fsrs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StateTest {

    @Test
    fun fromValueRoundTripsForAllEntries() {
        for (state in State.entries) {
            assertEquals(expected = state, actual = State.fromValue(state.value))
        }
    }

    @Test
    fun fromValueOnZeroThrowsNoSuchElementException() {
        assertFailsWith<NoSuchElementException> {
            State.fromValue(0)
        }
    }

    @Test
    fun fromValueOnFourThrowsNoSuchElementException() {
        assertFailsWith<NoSuchElementException> {
            State.fromValue(4)
        }
    }
}
