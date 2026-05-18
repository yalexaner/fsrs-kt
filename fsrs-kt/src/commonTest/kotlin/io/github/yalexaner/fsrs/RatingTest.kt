package io.github.yalexaner.fsrs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RatingTest {

    @Test
    fun fromValueRoundTripsForAllEntries() {
        for (rating in Rating.entries) {
            assertEquals(expected = rating, actual = Rating.fromValue(rating.value))
        }
    }

    @Test
    fun fromValueOnZeroThrowsNoSuchElementException() {
        assertFailsWith<NoSuchElementException> {
            Rating.fromValue(0)
        }
    }

    @Test
    fun fromValueOnFiveThrowsNoSuchElementException() {
        assertFailsWith<NoSuchElementException> {
            Rating.fromValue(5)
        }
    }
}
