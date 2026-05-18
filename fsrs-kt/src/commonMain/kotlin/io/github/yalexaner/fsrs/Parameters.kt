package io.github.yalexaner.fsrs

import io.github.yalexaner.fsrs.internal.DEFAULT_W

/**
 * FSRS-6 scheduling parameters.
 *
 * @property w the 21 weight parameters; must have exactly 21 elements.
 * @property desiredRetention target recall probability; must be in [0.01, 0.999].
 * @property learningStepsSeconds step durations in seconds for the Learning state.
 * @property relearningStepsSeconds step durations in seconds for the Relearning state.
 * @property maximumIntervalDays upper bound on the scheduled interval.
 * @property enableFuzzing whether to apply interval fuzzing.
 */
public data class Parameters(
    public val w: DoubleArray,
    public val desiredRetention: Double = 0.9,
    public val learningStepsSeconds: LongArray = longArrayOf(60L, 600L),
    public val relearningStepsSeconds: LongArray = longArrayOf(600L),
    public val maximumIntervalDays: Int = 36_500,
    public val enableFuzzing: Boolean = true,
) {
    init {
        require(w.size == 21) { "FSRS-6 requires exactly 21 parameters (got ${w.size})" }
        require(desiredRetention in 0.01..0.999) { "desiredRetention must be in [0.01, 0.999] (got $desiredRetention)" }
        require(learningStepsSeconds.all { it >= 0 }) { "learningStepsSeconds must be non-negative" }
        require(relearningStepsSeconds.all { it >= 0 }) { "relearningStepsSeconds must be non-negative" }
        require(maximumIntervalDays >= 1) { "maximumIntervalDays must be >= 1 (got $maximumIntervalDays)" }
    }

    public companion object {
        public val Default: Parameters
            get() = Parameters(w = DEFAULT_W.copyOf())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Parameters) return false

        return w.contentEquals(other.w) &&
            desiredRetention == other.desiredRetention &&
            learningStepsSeconds.contentEquals(other.learningStepsSeconds) &&
            relearningStepsSeconds.contentEquals(other.relearningStepsSeconds) &&
            maximumIntervalDays == other.maximumIntervalDays &&
            enableFuzzing == other.enableFuzzing
    }

    override fun hashCode(): Int {
        var result = w.contentHashCode()
        result = 31 * result + desiredRetention.hashCode()
        result = 31 * result + learningStepsSeconds.contentHashCode()
        result = 31 * result + relearningStepsSeconds.contentHashCode()
        result = 31 * result + maximumIntervalDays
        result = 31 * result + enableFuzzing.hashCode()
        return result
    }
}
