package io.github.yalexaner.fsrs

/**
 * The four possible review outcomes for an FSRS card.
 *
 * @property value the integer code used in persistence and the algorithm.
 */
public enum class Rating(public val value: Int) {
    Again(1),
    Hard(2),
    Good(3),
    Easy(4);

    public companion object {
        public fun fromValue(value: Int): Rating = entries.first { it.value == value }
    }
}
