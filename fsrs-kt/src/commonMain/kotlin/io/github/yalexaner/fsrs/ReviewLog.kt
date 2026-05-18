package io.github.yalexaner.fsrs

import kotlin.time.Instant

/**
 * A log entry recording a single review of a card.
 *
 * @property cardId the card that was reviewed.
 * @property rating the answer given by the user.
 * @property reviewDateTime when the review occurred.
 * @property reviewDuration how long the review took, in milliseconds; null if unspecified.
 */
public data class ReviewLog(
    public val cardId: Long,
    public val rating: Rating,
    public val reviewDateTime: Instant,
    public val reviewDuration: Long? = null,
)
