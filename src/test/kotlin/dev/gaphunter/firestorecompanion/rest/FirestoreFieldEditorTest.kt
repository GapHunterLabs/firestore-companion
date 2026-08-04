package dev.gaphunter.firestorecompanion.rest

import dev.gaphunter.firestorecompanion.json.JsonNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreFieldEditorTest {
    private fun typed(key: String, value: JsonNode): JsonNode.Obj = JsonNode.Obj(linkedMapOf(key to value))

    @Test
    fun `scalar types are editable`() {
        assertTrue(FirestoreFieldEditor.isEditable(typed("stringValue", JsonNode.Str("x"))))
        assertTrue(FirestoreFieldEditor.isEditable(typed("integerValue", JsonNode.Str("5"))))
        assertTrue(FirestoreFieldEditor.isEditable(typed("doubleValue", JsonNode.Num(1.5))))
        assertTrue(FirestoreFieldEditor.isEditable(typed("booleanValue", JsonNode.Bool(true))))
    }

    @Test
    fun `map array geoPoint reference timestamp and null are not editable`() {
        assertFalse(FirestoreFieldEditor.isEditable(typed("mapValue", JsonNode.Obj(linkedMapOf()))))
        assertFalse(FirestoreFieldEditor.isEditable(typed("arrayValue", JsonNode.Obj(linkedMapOf()))))
        assertFalse(FirestoreFieldEditor.isEditable(typed("geoPointValue", JsonNode.Obj(linkedMapOf()))))
        assertFalse(FirestoreFieldEditor.isEditable(typed("referenceValue", JsonNode.Str("x"))))
        assertFalse(FirestoreFieldEditor.isEditable(typed("timestampValue", JsonNode.Str("x"))))
        assertFalse(FirestoreFieldEditor.isEditable(typed("nullValue", JsonNode.Null)))
    }

    @Test
    fun `editing a stringValue preserves the type`() {
        val edited = FirestoreFieldEditor.buildEditedValue(typed("stringValue", JsonNode.Str("Alice")), "Alicia")
        assertEquals("stringValue", edited.entries.keys.first())
        assertEquals("Alicia", (edited.entries["stringValue"] as JsonNode.Str).value)
    }

    @Test
    fun `editing an integerValue re-encodes as a numeric string`() {
        val edited = FirestoreFieldEditor.buildEditedValue(typed("integerValue", JsonNode.Str("30")), "31")
        assertEquals("31", (edited.entries["integerValue"] as JsonNode.Str).value)
    }

    @Test
    fun `an invalid integer input throws instead of silently corrupting the field`() {
        assertThrows(IllegalArgumentException::class.java) {
            FirestoreFieldEditor.buildEditedValue(typed("integerValue", JsonNode.Str("30")), "not a number")
        }
    }

    @Test
    fun `editing a doubleValue parses as a double`() {
        val edited = FirestoreFieldEditor.buildEditedValue(typed("doubleValue", JsonNode.Num(1.5)), "2.75")
        assertEquals(2.75, (edited.entries["doubleValue"] as JsonNode.Num).value, 0.0001)
    }

    @Test
    fun `editing a booleanValue accepts true or false case-insensitively`() {
        val edited = FirestoreFieldEditor.buildEditedValue(typed("booleanValue", JsonNode.Bool(false)), "TRUE")
        assertEquals(true, (edited.entries["booleanValue"] as JsonNode.Bool).value)
    }

    @Test
    fun `an invalid boolean input throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            FirestoreFieldEditor.buildEditedValue(typed("booleanValue", JsonNode.Bool(false)), "maybe")
        }
    }

    @Test
    fun `editing a non-editable type throws instead of producing a corrupted field`() {
        assertThrows(IllegalArgumentException::class.java) {
            FirestoreFieldEditor.buildEditedValue(typed("mapValue", JsonNode.Obj(linkedMapOf())), "{}")
        }
    }
}
