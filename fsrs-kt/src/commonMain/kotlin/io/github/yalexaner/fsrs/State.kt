package io.github.yalexaner.fsrs

/**
 * The three possible lifecycle states of an FSRS card.
 *
 * @property value the integer code used in persistence and the algorithm.
 */
public enum class State(public val value: Int) {
    Learning(1),
    Review(2),
    Relearning(3);

    public companion object {
        public fun fromValue(value: Int): State = entries.first { it.value == value }
    }
}
