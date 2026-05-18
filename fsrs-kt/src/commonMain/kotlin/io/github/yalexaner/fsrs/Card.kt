package io.github.yalexaner.fsrs

import kotlin.time.Instant

/**
 * An FSRS card with its current scheduling state and metadata.
 *
 * @property cardId caller-assigned identifier.
 * @property state current lifecycle state; defaults to [State.Learning].
 * @property step current learning/relearning step, null in [State.Review].
 * @property stability null until the first successful review.
 * @property difficulty null until the first review.
 * @property due when this card next becomes due.
 * @property lastReview timestamp of the most recent review, or null.
 */
public data class Card(
    public val cardId: Long,
    public val state: State = State.Learning,
    public val step: Int? = 0,
    public val stability: Double? = null,
    public val difficulty: Double? = null,
    public val due: Instant,
    public val lastReview: Instant? = null,
)
