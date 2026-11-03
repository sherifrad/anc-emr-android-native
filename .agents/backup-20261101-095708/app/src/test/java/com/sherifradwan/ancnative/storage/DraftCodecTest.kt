package com.sherifradwan.ancnative.storage

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftCodecTest {
    private fun roundTrip(fields: Map<String, String>): Map<String, String>? {
        val json = encodeDraftFields(fields)
        assertNotNull(json)
        return decodeDraftFields(json!!)
    }

    @Test
    fun emptyMapRoundTrips() {
        assertEquals(emptyMap<String, String>(), roundTrip(emptyMap()))
    }

    @Test
    fun oneFieldRoundTrips() {
        assertEquals(mapOf("notes" to "hello"), roundTrip(mapOf("notes" to "hello")))
    }

    @Test
    fun manyFieldsRoundTrip() {
        val fields = (1..50).associate { "field$it" to "value $it" }
        assertEquals(fields, roundTrip(fields))
    }

    @Test
    fun arabicTextRoundTripsExactly() {
        val fields = mapOf("notes" to "ملاحظات الزيارة: ضغط الدم مرتفع", "name" to "فاطمة")
        assertEquals(fields, roundTrip(fields))
    }

    @Test
    fun emojiRoundTripsExactly() {
        val fields = mapOf("notes" to "baby 👶🏽 ok ✅ 🇪🇬")
        assertEquals(fields, roundTrip(fields))
    }

    @Test
    fun quotesBackslashesAndNewlinesRoundTripExactly() {
        val fields = mapOf(
            "quote" to "she said \"no\" and 'yes'",
            "backslash" to "C:\\path\\to\\file \\n not a newline",
            "newlines" to "line1\nline2\r\nline3\ttabbed",
            "key \"with\" quote\n" to "v",
        )
        assertEquals(fields, roundTrip(fields))
    }

    @Test
    fun valueJustUnderTheCapIsStoredAndOverTheCapReturnsNull() {
        val overhead = encodeDraftFields(mapOf("a" to ""))!!.toByteArray(Charsets.UTF_8).size
        val exactlyAtCap = mapOf("a" to "x".repeat(MAX_DRAFT_PAYLOAD_BYTES - overhead))
        val atCapJson = encodeDraftFields(exactlyAtCap)
        assertNotNull(atCapJson)
        assertEquals(MAX_DRAFT_PAYLOAD_BYTES, atCapJson!!.toByteArray(Charsets.UTF_8).size)
        assertEquals(exactlyAtCap, decodeDraftFields(atCapJson))

        val justUnder = mapOf("a" to "x".repeat(MAX_DRAFT_PAYLOAD_BYTES - overhead - 1))
        assertNotNull(encodeDraftFields(justUnder))

        val justOver = mapOf("a" to "x".repeat(MAX_DRAFT_PAYLOAD_BYTES - overhead + 1))
        assertNull(encodeDraftFields(justOver))
    }

    @Test
    fun capCountsUtf8BytesNotCharacters() {
        val manyArabic = mapOf("a" to "ب".repeat(MAX_DRAFT_PAYLOAD_BYTES / 2))
        assertNull(encodeDraftFields(manyArabic))
    }

    @Test
    fun capValueIsSixtyFiveKiB() {
        assertEquals(65_536, MAX_DRAFT_PAYLOAD_BYTES)
    }

    @Test
    fun malformedJsonDecodesToNullWithoutThrowing() {
        for (bad in listOf("", "   ", "not json", "{", "{\"a\":", "[\"a\"]", "null", "\"text\"", "{\"a\":\"b\"", "{'a':'b'}x")) {
            assertNull("input: $bad", decodeDraftFields(bad))
        }
    }

    @Test
    fun nonStringValuesAreIgnoredAndStringValuesKept() {
        val decoded = decodeDraftFields("""{"count":3,"flag":true,"nothing":null,"nested":{"a":"b"},"list":["x"],"ok":"yes"}""")
        assertEquals(mapOf("ok" to "yes"), decoded)
    }

    @Test
    fun unknownKeysAreKept() {
        val decoded = decodeDraftFields("""{"futureField":"kept","notes":"n"}""")
        assertEquals(mapOf("futureField" to "kept", "notes" to "n"), decoded)
    }

    @Test
    fun keyOrderIsStableAndSorted() {
        val first = encodeDraftFields(linkedMapOf("zeta" to "1", "alpha" to "2", "mid" to "3"))
        val second = encodeDraftFields(linkedMapOf("mid" to "3", "alpha" to "2", "zeta" to "1"))
        assertEquals(first, second)
        val json = first!!
        assertTrue(json.indexOf("\"alpha\"") < json.indexOf("\"mid\""))
        assertTrue(json.indexOf("\"mid\"") < json.indexOf("\"zeta\""))
        assertEquals(setOf("alpha", "mid", "zeta"), JSONObject(json).keys().asSequence().toSet())
    }

    @Test
    fun draftRecordEqualityComparesEveryField() {
        fun record() = DraftRecord(
            patientId = "p1",
            screenKind = DraftScreenKind.VISIT,
            editorKind = DraftEditorKind.EDIT,
            recordId = "v1",
            baseRevision = 3L,
            fields = mapOf("notes" to "n"),
            updatedAtMillis = 1_000L,
        )
        assertEquals(record(), record())
        assertEquals(record().hashCode(), record().hashCode())
        assertNotEquals(record(), record().copy(fields = mapOf("notes" to "other")))
        assertNotEquals(record(), record().copy(screenKind = DraftScreenKind.LAB))
        assertNotEquals(record(), record().copy(baseRevision = null))
        assertNotEquals(record(), record().copy(updatedAtMillis = 2_000L))
        assertEquals(DraftRecord("__NEW_PATIENT__", DraftScreenKind.PATIENT_PROFILE, DraftEditorKind.CREATE, null, null, emptyMap(), 0L).patientId, NEW_PATIENT_DRAFT_KEY)
    }
}
