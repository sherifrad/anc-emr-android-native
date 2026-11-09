package com.sherifradwan.ancnative.model

import kotlin.math.abs

data class RedatingAdvice(
    val differenceDays: Int,
    val thresholdDays: Int,
)

/** Wider than any real gestation gap; keeps the Int narrowing below from wrapping. */
private const val MAX_ADVICE_SPAN_DAYS = 100_000L

fun redatingThresholdDays(datingGaDays: Long): Int = when {
    datingGaDays <= 62L -> 5
    datingGaDays in 63L..111L -> 7
    datingGaDays in 112L..153L -> 10
    datingGaDays in 154L..195L -> 14
    else -> 21
}

fun redatingAdvice(datingGaDays: Long, scanGaDays: Long): RedatingAdvice? {
    val span = scanGaDays - datingGaDays
    if (abs(span) > MAX_ADVICE_SPAN_DAYS) return null
    val difference = span.toInt()
    val threshold = redatingThresholdDays(datingGaDays)
    return if (abs(difference) > threshold) {
        RedatingAdvice(differenceDays = difference, thresholdDays = threshold)
    } else {
        null
    }
}
