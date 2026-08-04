package dev.gaphunter.firestorecompanion.json

/**
 * Minimal, hand-rolled JSON reader -- used to parse both the service
 * account key file and Firestore REST API responses. Small, stable
 * grammar; same "hand-roll over new dependency" call as elsewhere in
 * this workspace (see JsonParser.kt in format-converter-companion for
 * the same rationale spelled out in more detail -- each plugin here is
 * a separate published product/repo, so this is an independent, smaller
 * implementation, not shared code).
 */
object MinimalJsonParser {
    fun parse(text: String): JsonNode {
        val parser = Parser(text)
        val value = parser.parseValue()
        parser.skipWhitespace()
        if (!parser.atEnd()) throw FirestoreJsonException("Unexpected trailing content")
        return value
    }

    private class Parser(private val text: String) {
        var pos = 0

        fun atEnd() = pos >= text.length
        fun peek(): Char = text[pos]
        fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        fun parseValue(): JsonNode {
            skipWhitespace()
            if (atEnd()) throw FirestoreJsonException("Unexpected end of input")
            return when (peek()) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonNode.Str(parseString())
                't' -> parseLiteral("true", JsonNode.Bool(true))
                'f' -> parseLiteral("false", JsonNode.Bool(false))
                'n' -> parseLiteral("null", JsonNode.Null)
                else -> parseNumber()
            }
        }

        fun parseLiteral(literal: String, value: JsonNode): JsonNode {
            if (pos + literal.length > text.length || text.substring(pos, pos + literal.length) != literal) {
                throw FirestoreJsonException("Invalid literal at $pos")
            }
            pos += literal.length
            return value
        }

        fun parseObject(): JsonNode.Obj {
            pos++
            val entries = LinkedHashMap<String, JsonNode>()
            skipWhitespace()
            if (!atEnd() && peek() == '}') {
                pos++
                return JsonNode.Obj(entries)
            }
            while (true) {
                skipWhitespace()
                if (atEnd() || peek() != '"') throw FirestoreJsonException("Expected string key at $pos")
                val key = parseString()
                skipWhitespace()
                if (atEnd() || peek() != ':') throw FirestoreJsonException("Expected ':' at $pos")
                pos++
                entries[key] = parseValue()
                skipWhitespace()
                if (atEnd()) throw FirestoreJsonException("Unterminated object")
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return JsonNode.Obj(entries)
                    }
                    else -> throw FirestoreJsonException("Expected ',' or '}' at $pos")
                }
            }
        }

        fun parseArray(): JsonNode.Arr {
            pos++
            val items = mutableListOf<JsonNode>()
            skipWhitespace()
            if (!atEnd() && peek() == ']') {
                pos++
                return JsonNode.Arr(items)
            }
            while (true) {
                items.add(parseValue())
                skipWhitespace()
                if (atEnd()) throw FirestoreJsonException("Unterminated array")
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return JsonNode.Arr(items)
                    }
                    else -> throw FirestoreJsonException("Expected ',' or ']' at $pos")
                }
            }
        }

        fun parseString(): String {
            pos++
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw FirestoreJsonException("Unterminated string")
                val c = text[pos]
                pos++
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (atEnd()) throw FirestoreJsonException("Unterminated escape")
                        val escaped = text[pos]
                        pos++
                        when (escaped) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'n' -> sb.append('\n')
                            't' -> sb.append('\t')
                            'r' -> sb.append('\r')
                            'b' -> sb.append('\b')
                            'u' -> {
                                if (pos + 4 > text.length) throw FirestoreJsonException("Invalid unicode escape")
                                val code = text.substring(pos, pos + 4).toIntOrNull(16)
                                    ?: throw FirestoreJsonException("Invalid unicode escape")
                                pos += 4
                                sb.append(code.toChar())
                            }
                            else -> throw FirestoreJsonException("Invalid escape '\\$escaped'")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        fun parseNumber(): JsonNode.Num {
            val start = pos
            if (!atEnd() && peek() == '-') pos++
            while (!atEnd() && peek().isDigit()) pos++
            if (!atEnd() && peek() == '.') {
                pos++
                while (!atEnd() && peek().isDigit()) pos++
            }
            if (!atEnd() && (peek() == 'e' || peek() == 'E')) {
                pos++
                if (!atEnd() && (peek() == '+' || peek() == '-')) pos++
                while (!atEnd() && peek().isDigit()) pos++
            }
            if (pos == start) throw FirestoreJsonException("Invalid number at $pos")
            val numberText = text.substring(start, pos)
            return JsonNode.Num(
                numberText.toDoubleOrNull() ?: throw FirestoreJsonException("Invalid number '$numberText'")
            )
        }
    }
}
