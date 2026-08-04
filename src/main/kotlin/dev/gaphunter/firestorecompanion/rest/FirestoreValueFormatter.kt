package dev.gaphunter.firestorecompanion.rest

import dev.gaphunter.firestorecompanion.json.JsonNode

/**
 * Renders a Firestore REST API typed-value wrapper (e.g. `{"stringValue":
 * "x"}`, `{"integerValue": "5"}`, `{"mapValue": {"fields": {...}}}`) as a
 * short human-readable string, for the documents table -- the direct
 * answer to the cited complaint about wanting subcollection data "in
 * tabular form as well, not just in JSON."
 *
 * Uses `firstOrNull()`, never `first()`, everywhere a typed-value's
 * single key is read -- the same NoSuchElementException-avoidance
 * discipline as ServiceAccountParser, applied to REST response parsing
 * too.
 */
object FirestoreValueFormatter {
    fun format(value: JsonNode): String {
        val obj = value as? JsonNode.Obj ?: return "null"
        val entry = obj.entries.entries.firstOrNull() ?: return "null"
        val typedValue = entry.value
        return when (entry.key) {
            "stringValue" -> (typedValue as? JsonNode.Str)?.value ?: ""
            "integerValue" -> (typedValue as? JsonNode.Str)?.value
                ?: (typedValue as? JsonNode.Num)?.value?.toLong()?.toString().orEmpty()
            "doubleValue" -> (typedValue as? JsonNode.Num)?.value?.toString() ?: ""
            "booleanValue" -> (typedValue as? JsonNode.Bool)?.value?.toString() ?: ""
            "nullValue" -> "null"
            "timestampValue" -> (typedValue as? JsonNode.Str)?.value ?: ""
            "referenceValue" -> (typedValue as? JsonNode.Str)?.value ?: ""
            "geoPointValue" -> "(geo point)"
            "mapValue" -> formatFields((typedValue as? JsonNode.Obj)?.entries?.get("fields") as? JsonNode.Obj)
            "arrayValue" -> formatArray((typedValue as? JsonNode.Obj)?.entries?.get("values") as? JsonNode.Arr)
            else -> "?"
        }
    }

    fun formatFields(fields: JsonNode.Obj?): String {
        if (fields == null || fields.entries.isEmpty()) return "{}"
        return "{" + fields.entries.entries.joinToString(", ") { (key, value) -> "$key: ${format(value)}" } + "}"
    }

    private fun formatArray(values: JsonNode.Arr?): String {
        if (values == null || values.items.isEmpty()) return "[]"
        return "[" + values.items.joinToString(", ") { format(it) } + "]"
    }
}
