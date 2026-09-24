package dev.gaphunter.firestorecompanion.json

/**
 * Indented JSON serializer, for human-readable output (the Pro
 * collection-export feature) -- [JsonWriter] stays compact-only since
 * that's all the JWT/REST-request-body use cases ever needed. Shares
 * [JsonWriter]'s number/string escaping so the two writers never
 * diverge on what counts as valid JSON.
 */
object JsonPrettyWriter {
    private const val INDENT_UNIT = "  "

    fun write(node: JsonNode): String {
        val sb = StringBuilder()
        writeValue(node, sb, 0)
        return sb.toString()
    }

    private fun writeValue(node: JsonNode, sb: StringBuilder, depth: Int) {
        when (node) {
            is JsonNode.Null -> sb.append("null")
            is JsonNode.Bool -> sb.append(if (node.value) "true" else "false")
            is JsonNode.Num -> sb.append(JsonWriter.formatNumber(node.value))
            is JsonNode.Str -> JsonWriter.writeString(node.value, sb)
            is JsonNode.Arr -> writeArray(node, sb, depth)
            is JsonNode.Obj -> writeObject(node, sb, depth)
        }
    }

    private fun writeArray(node: JsonNode.Arr, sb: StringBuilder, depth: Int) {
        if (node.items.isEmpty()) {
            sb.append("[]")
            return
        }
        sb.append("[\n")
        node.items.forEachIndexed { index, item ->
            sb.append(INDENT_UNIT.repeat(depth + 1))
            writeValue(item, sb, depth + 1)
            if (index < node.items.size - 1) sb.append(',')
            sb.append('\n')
        }
        sb.append(INDENT_UNIT.repeat(depth)).append(']')
    }

    private fun writeObject(node: JsonNode.Obj, sb: StringBuilder, depth: Int) {
        if (node.entries.isEmpty()) {
            sb.append("{}")
            return
        }
        sb.append("{\n")
        val keys = node.entries.keys.toList()
        keys.forEachIndexed { index, key ->
            sb.append(INDENT_UNIT.repeat(depth + 1))
            JsonWriter.writeString(key, sb)
            sb.append(": ")
            writeValue(node.entries.getValue(key), sb, depth + 1)
            if (index < keys.size - 1) sb.append(',')
            sb.append('\n')
        }
        sb.append(INDENT_UNIT.repeat(depth)).append('}')
    }
}
