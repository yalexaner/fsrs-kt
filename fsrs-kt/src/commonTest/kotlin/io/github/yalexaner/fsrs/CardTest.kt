package io.github.yalexaner.fsrs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class CardTest {

    @Test
    fun twoIdenticalCardsAreEqualAndShareHashCode() {
        val now = Instant.fromEpochSeconds(1_000)
        val cardA = Card(cardId = 42, due = now, state = State.Review, step = 2, stability = 3.5, difficulty = 5.0)
        val cardB = Card(cardId = 42, due = now, state = State.Review, step = 2, stability = 3.5, difficulty = 5.0)

        assertEquals(expected = cardB, actual = cardA)
        assertEquals(expected = cardA.hashCode(), actual = cardB.hashCode())
    }

    @Test
    fun copyUpdatesOnlyTheSpecifiedField() {
        val now = Instant.fromEpochSeconds(1_000)
        val card = Card(cardId = 1, due = now, state = State.Learning, step = 0, stability = 1.0, difficulty = 2.0, lastReview = now)
        val copied = card.copy(state = State.Review)

        assertEquals(expected = State.Review, actual = copied.state)
        assertEquals(expected = card.cardId, actual = copied.cardId)
        assertEquals(expected = card.step, actual = copied.step)
        assertEquals(expected = card.stability!!, actual = copied.stability!!, absoluteTolerance = 1e-9)
        assertEquals(expected = card.difficulty!!, actual = copied.difficulty!!, absoluteTolerance = 1e-9)
        assertEquals(expected = card.due, actual = copied.due)
        assertEquals(expected = card.lastReview, actual = copied.lastReview)
    }

    @Test
    fun defaultsAreAppliedWhenOnlyRequiredFieldsAreProvided() {
        val card = Card(cardId = 1, due = Instant.fromEpochSeconds(0))

        assertEquals(expected = State.Learning, actual = card.state)
        assertEquals(expected = 0, actual = card.step)
        assertNull(card.stability)
        assertNull(card.difficulty)
        assertNull(card.lastReview)
    }
}
