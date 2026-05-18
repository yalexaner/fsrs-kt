package io.github.yalexaner.fsrs.internal

/**
 * The 21 FSRS-6 default weight parameters from SPEC.md §2.3.
 * Stored as a shared constant so [io.github.yalexaner.fsrs.Parameters.Default]
 * can copy from it without duplicating the literals.
 */
internal val DEFAULT_W: DoubleArray = doubleArrayOf(
    0.212,
    1.2931,
    2.3065,
    8.2956,
    6.4133,
    0.8334,
    3.0194,
    0.001,
    1.8722,
    0.1666,
    0.796,
    1.4835,
    0.0614,
    0.2629,
    1.6483,
    0.6014,
    1.8729,
    0.5425,
    0.0912,
    0.0658,
    0.1542,
)
