package dev.gaphunter.firestorecompanion.rest

import dev.gaphunter.firestorecompanion.json.JsonNode

/**
 * Rebuilds a Firestore typed-value wrapper from edited text, preserving
 * the field's original type rather than guessing a new one -- editing a
 * `stringValue` always produces another `stringValue`, never a type
 * change by accident. Only scalar types are editable in this version
 * (string/integer/double/boolean); map/array/geoPoint/reference/
 * timestamp/null fields stay read-only in the edit dialog, same
 * deliberate scope cut already applied elsewhere in this plugin (v1
 * shipped read-only browsing; this adds editing for the types that are
 * safe to round-trip through a single text field, not every type).
 *
 * Pure and PSI/UI-free, like the rest of this plugin's `rest`/`json`
 * packages -- testable without a live Firestore project.
 */
object FirestoreFieldEditor {
    private val EDITABLE_TYPES = setOf("stringValue", "integerValue", "doubleValue", "booleanValue")

    fun isEditable(value: JsonNode): Boolean {
        val obj = value as? JsonNode.Obj ?: return false
        val typeKey = obj.entries.keys.firstOrNull() ?: return false
        return typeKey in EDITABLE_TYPES
    }

    /**
     * @throws IllegalArgumentException if [original] isn't an editable
     * scalar, or [newText] doesn't parse as that type's expected format.
     */
    fun buildEditedValue(original: JsonNode.Obj, newText: String): JsonNode.Obj {
        val typeKey = original.entries.keys.firstOrNull()
            ?: throw IllegalArgumentException("Field has no typed value to preserve")
        val newTyped: JsonNode = when (typeKey) {
            "stringValue" -> JsonNode.Str(newText)
            // Firestore's REST API encodes integerValue as a numeric string (int64 doesn't
            // round-trip safely through JSON numbers), so the rebuilt value must match that.
            "integerValue" -> JsonNode.Str(parseLong(newText).toString())
            "doubleValue" -> JsonNode.Num(parseDouble(newText))
            "booleanValue" -> JsonNode.Bool(parseBoolean(newText))
            else -> throw IllegalArgumentException("Field type '$typeKey' isn't editable in this version")
        }
        return JsonNode.Obj(linkedMapOf(typeKey to newTyped))
    }

    private fun parseLong(text: String): Long =
        text.trim().toLongOrNull() ?: throw IllegalArgumentException("'$text' isn't a whole number")

    private fun parseDouble(text: String): Double =
        text.trim().toDoubleOrNull() ?: throw IllegalArgumentException("'$text' isn't a number")

    private fun parseBoolean(text: String): Boolean = when (text.trim().lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw IllegalArgumentException("'$text' isn't true or false")
    }
}
