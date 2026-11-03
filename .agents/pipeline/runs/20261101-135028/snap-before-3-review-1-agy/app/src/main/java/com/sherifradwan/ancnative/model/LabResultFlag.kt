package com.sherifradwan.ancnative.model

enum class LabResultFlag {
    NORMAL,
    HIGH,
    LOW,
    UNKNOWN,
}

private val NUMBER_REGEX = Regex("""^\d+(?:\.\d+)?$""")
private val RANGE_BETWEEN_REGEX = Regex("""^(\d+(?:\.\d+)?)\s*[-–]\s*(\d+(?:\.\d+)?)$""")
private val RANGE_LTE_REGEX = Regex("""^<=\s*(\d+(?:\.\d+)?)$""")
private val RANGE_LT_REGEX = Regex("""^<\s*(\d+(?:\.\d+)?)$""")
private val RANGE_GTE_REGEX = Regex("""^>=\s*(\d+(?:\.\d+)?)$""")
private val RANGE_GT_REGEX = Regex("""^>\s*(\d+(?:\.\d+)?)$""")

fun labResultFlag(result: String, referenceRange: String): LabResultFlag {
    val cleanResult = result.trim()
    val cleanRange = referenceRange.trim()

    if (cleanResult.isEmpty() || cleanRange.isEmpty()) {
        return LabResultFlag.UNKNOWN
    }

    if (!NUMBER_REGEX.matches(cleanResult)) {
        return LabResultFlag.UNKNOWN
    }

    val resultNum = cleanResult.toDoubleOrNull() ?: return LabResultFlag.UNKNOWN

    val betweenMatch = RANGE_BETWEEN_REGEX.matchEntire(cleanRange)
    if (betweenMatch != null) {
        val a = betweenMatch.groupValues[1].toDoubleOrNull() ?: return LabResultFlag.UNKNOWN
        val b = betweenMatch.groupValues[2].toDoubleOrNull() ?: return LabResultFlag.UNKNOWN
        if (a > b) return LabResultFlag.UNKNOWN
        return when {
            resultNum < a -> LabResultFlag.LOW
            resultNum > b -> LabResultFlag.HIGH
            else -> LabResultFlag.NORMAL
        }
    }

    val lteMatch = RANGE_LTE_REGEX.matchEntire(cleanRange)
    if (lteMatch != null) {
        val x = lteMatch.groupValues[1].toDoubleOrNull() ?: return LabResultFlag.UNKNOWN
        return if (resultNum <= x) LabResultFlag.NORMAL else LabResultFlag.HIGH
    }

    val ltMatch = RANGE_LT_REGEX.matchEntire(cleanRange)
    if (ltMatch != null) {
        val x = ltMatch.groupValues[1].toDoubleOrNull() ?: return LabResultFlag.UNKNOWN
        return if (resultNum < x) LabResultFlag.NORMAL else LabResultFlag.HIGH
    }

    val gteMatch = RANGE_GTE_REGEX.matchEntire(cleanRange)
    if (gteMatch != null) {
        val x = gteMatch.groupValues[1].toDoubleOrNull() ?: return LabResultFlag.UNKNOWN
        return if (resultNum >= x) LabResultFlag.NORMAL else LabResultFlag.LOW
    }

    val gtMatch = RANGE_GT_REGEX.matchEntire(cleanRange)
    if (gtMatch != null) {
        val x = gtMatch.groupValues[1].toDoubleOrNull() ?: return LabResultFlag.UNKNOWN
        return if (resultNum > x) LabResultFlag.NORMAL else LabResultFlag.LOW
    }

    return LabResultFlag.UNKNOWN
}
