package io.github.yalexaner.fsrs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ReviewLogTest {

    @Test
    fun twoIdenticalLogsAreEqualAndShareHashCode() {
        val now = Instant.fromEpochSeconds(1_000)
        val logA = ReviewLog(cardId = 42, rating = Rating.Good, reviewDateTime = now, reviewDuration = 5_000L)
        val logB = ReviewLog(cardId = 42, rating = Rating.Good, reviewDateTime = now, reviewDuration = 5_000L)

        assertEquals(expected = logB, actual = logA)
        assertEquals(expected = logA.hashCode(), actual = logB.hashCode())
    }

    @Test
    fun copyUpdatesOnlyTheSpecifiedField() {
        val now = Instant.fromEpochSeconds(1_000)
        val log = ReviewLog(cardId = 1, rating = Rating.Again, reviewDateTime = now, reviewDuration = 3_000L)
        val copied = log.copy(rating = Rating.Good)

        assertEquals(expected = Rating.Good, actual = copied.rating)
        assertEquals(expected = log.cardId, actual = copied.cardId)
        assertEquals(expected = log.reviewDateTime, actual = copied.reviewDateTime)
        assertEquals(expected = log.reviewDuration, actual = copied.reviewDuration)
    }

    @Test
    fun reviewDurationDefaultsToNullWhenOmitted() {
        val now = Instant.fromEpochSeconds(1_000)
        val log = ReviewLog(cardId = 1, rating = Rating.Easy, reviewDateTime = now)

        assertNull(log.reviewDuration)
    }
}
