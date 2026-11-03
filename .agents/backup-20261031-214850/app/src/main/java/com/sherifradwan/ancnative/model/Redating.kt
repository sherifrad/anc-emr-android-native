package com.sherifradwan.ancnative.model

import kotlin.math.abs

data class RedatingAdvice(
    val differenceDays: Int,
    val thresholdDays: Int,
)

fun redatingThresholdDays(datingGaDays: Long): Int = when {
    datingGaDays <= 62L -> 5
    datingGaDays in 63L..111L -> 7
    datingGaDays in 112L..153L -> 10
    datingGaDays in 154L..195L -> 14
    else -> 21
}

fun redatingAdvice(datingGaDays: Long, scanGaDays: Long): RedatingAdvice? {
    val difference = (scanGaDays - datingGaDays).toInt()
    val threshold = redatingThresholdDays(datingGaDays)
    return if (abs(difference) > threshold) {
        RedatingAdvice(differenceDays = difference, thresholdDays = threshold)
    } else {
        null
    }
}
