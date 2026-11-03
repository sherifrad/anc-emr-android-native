package com.sherifradwan.ancnative.storage

import org.json.JSONObject
import org.json.JSONTokener

const val MAX_DRAFT_PAYLOAD_BYTES = 65_536

fun encodeDraftFields(fields: Map<String, String>): String? {
    val sorted = fields.toSortedMap()
    val sb = StringBuilder()
    sb.append('{')
    var first = true
    for ((key, value) in sorted) {
        if (!first) sb.append(',')
        first = false
        sb.append(JSONObject.quote(key))
        sb.append(':')
        sb.append(JSONObject.quote(value))
    }
    sb.append('}')
    val result = sb.toString()
    val utf8Bytes = result.toByteArray(Charsets.UTF_8)
    if (utf8Bytes.size > MAX_DRAFT_PAYLOAD_BYTES) return null
    return result
}

fun decodeDraftFields(json: String): Map<String, String>? {
    val trimmed = json.trim()
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
    return try {
        val tokener = JSONTokener(trimmed)
        val obj = when (val parsed = tokener.nextValue()) {
            is JSONObject -> parsed
            else -> return null
        }
        if (tokener.more()) {
            val next = tokener.nextClean()
            if (next != 0.toChar()) return null
        }
        val result = mutableMapOf<String, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.opt(key)
            if (value is String) {
                result[key] = value
            }
        }
        result
    } catch (_: Throwable) {
        null
    }
}
