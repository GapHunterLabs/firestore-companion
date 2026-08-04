package dev.gaphunter.firestorecompanion.json

/** Compact (no whitespace) JSON serializer -- used for JWT header/claims
 * and Firestore REST request bodies, neither of which need pretty
 * printing. */
object JsonWriter {
    fun write(node: JsonNode): String {
        val sb = StringBuilder()
        writeValue(node, sb)
        return sb.toString()
    }

    private fun writeValue(node: JsonNode, sb: StringBuilder) {
        when (node) {
            is JsonNode.Null -> sb.append("null")
            is JsonNode.Bool -> sb.append(if (node.value) "true" else "false")
            is JsonNode.Num -> sb.append(formatNumber(node.value))
            is JsonNode.Str -> writeString(node.value, sb)
            is JsonNode.Arr -> {
                sb.append('[')
                node.items.forEachIndexed { index, item ->
                    if (index > 0) sb.append(',')
                    writeValue(item, sb)
                }
                sb.append(']')
            }
            is JsonNode.Obj -> {
                sb.append('{')
                val keys = node.entries.keys.toList()
                keys.forEachIndexed { index, key ->
                    if (index > 0) sb.append(',')
                    writeString(key, sb)
                    sb.append(':')
                    writeValue(node.entries.getValue(key), sb)
                }
                sb.append('}')
            }
        }
    }

    private fun formatNumber(value: Double): String =
        if (value == Math.floor(value) && !value.isInfinite() && Math.abs(value) < 1e15) {
            value.toLong().toString()
        } else {
            value.toString()
        }

    private fun writeString(value: String, sb: StringBuilder) {
        sb.append('"')
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c.code < 0x20) {
                    sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(c)
                }
            }
        }
        sb.append('"')
    }
}
