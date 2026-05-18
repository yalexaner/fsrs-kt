package io.github.yalexaner.fsrs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ParametersTest {

    @Test
    fun defaultConstructsWithoutErrorAndHasWSize21() {
        val params = Parameters.Default
        assertEquals(expected = 21, actual = params.w.size)
    }

    @Test
    fun defaultArrayIsIndependent() {
        val first = Parameters.Default
        first.w[0] = 999.0
        val second = Parameters.Default
        assertTrue { first.w !== second.w }
        assertEquals(expected = 0.212, actual = second.w[0], absoluteTolerance = 1e-9)
    }

    @Test
    fun rejectsWSize20() {
        assertFailsWith<IllegalArgumentException> {
            Parameters(w = DoubleArray(20), desiredRetention = 0.9)
        }
    }

    @Test
    fun rejectsWSize22() {
        assertFailsWith<IllegalArgumentException> {
            Parameters(w = DoubleArray(22), desiredRetention = 0.9)
        }
    }

    @Test
    fun rejectsDesiredRetention0_0() {
        assertFailsWith<IllegalArgumentException> {
            Parameters(w = DoubleArray(21), desiredRetention = 0.0)
        }
    }

    @Test
    fun rejectsDesiredRetention1_0() {
        assertFailsWith<IllegalArgumentException> {
            Parameters(w = DoubleArray(21), desiredRetention = 1.0)
        }
    }

    @Test
    fun rejectsDesiredRetentionJustBelowLowerBound() {
        assertFailsWith<IllegalArgumentException> {
            Parameters(w = DoubleArray(21), desiredRetention = 0.005)
        }
    }

    @Test
    fun rejectsDesiredRetentionJustAboveUpperBound() {
        assertFailsWith<IllegalArgumentException> {
            Parameters(w = DoubleArray(21), desiredRetention = 0.9991)
        }
    }

    @Test
    fun rejectsNegativeLearningSteps() {
        assertFailsWith<IllegalArgumentException> {
            Parameters(
                w = DoubleArray(21),
                learningStepsSeconds = longArrayOf(-1L)
            )
        }
    }

    @Test
    fun rejectsNegativeRelearningSteps() {
        assertFailsWith<IllegalArgumentException> {
            Parameters(
                w = DoubleArray(21),
                relearningStepsSeconds = longArrayOf(-1L)
            )
        }
    }

    @Test
    fun acceptsEmptyLearningSteps() {
        val params = Parameters(
            w = DoubleArray(21),
            learningStepsSeconds = longArrayOf()
        )
        assertEquals(expected = 0, actual = params.learningStepsSeconds.size)
    }

    @Test
    fun acceptsEmptyRelearningSteps() {
        val params = Parameters(
            w = DoubleArray(21),
            relearningStepsSeconds = longArrayOf()
        )
        assertEquals(expected = 0, actual = params.relearningStepsSeconds.size)
    }

    @Test
    fun rejectsMaximumIntervalDays0() {
        assertFailsWith<IllegalArgumentException> {
            Parameters(w = DoubleArray(21), maximumIntervalDays = 0)
        }
    }

    @Test
    fun rejectsMaximumIntervalDaysNegative() {
        assertFailsWith<IllegalArgumentException> {
            Parameters(w = DoubleArray(21), maximumIntervalDays = -1)
        }
    }

    @Test
    fun equalityWithIdenticalArrayContents() {
        val w = doubleArrayOf(
            0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0,
            1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.1
        )
        val paramsA = Parameters(
            w = w.copyOf(),
            desiredRetention = 0.85,
            learningStepsSeconds = longArrayOf(30L, 300L),
            relearningStepsSeconds = longArrayOf(300L),
            maximumIntervalDays = 1000,
            enableFuzzing = false
        )
        val paramsB = Parameters(
            w = w.copyOf(),
            desiredRetention = 0.85,
            learningStepsSeconds = longArrayOf(30L, 300L),
            relearningStepsSeconds = longArrayOf(300L),
            maximumIntervalDays = 1000,
            enableFuzzing = false
        )
        assertEquals(expected = paramsB, actual = paramsA)
        assertEquals(expected = paramsA.hashCode(), actual = paramsB.hashCode())
    }

    @Test
    fun inequalityWithDifferentW() {
        val wA = doubleArrayOf(
            0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0,
            1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0, 2.1
        )
        val wB = wA.copyOf()
        wB[0] = 0.99
        val paramsA = Parameters(w = wA)
        val paramsB = Parameters(w = wB)
        assertTrue { paramsA != paramsB }
    }

    @Test
    fun boundaryDesiredRetentionAccepts0_01() {
        val params = Parameters(w = DoubleArray(21), desiredRetention = 0.01)
        assertEquals(expected = 0.01, actual = params.desiredRetention, absoluteTolerance = 1e-9)
    }

    @Test
    fun boundaryDesiredRetentionAccepts0_999() {
        val params = Parameters(w = DoubleArray(21), desiredRetention = 0.999)
        assertEquals(expected = 0.999, actual = params.desiredRetention, absoluteTolerance = 1e-9)
    }
}
