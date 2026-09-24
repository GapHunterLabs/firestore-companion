package dev.gaphunter.firestorecompanion.json

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonPrettyWriterTest {

    @Test
    fun `empty object and array stay on one line`() {
        assertEquals("{}", JsonPrettyWriter.write(JsonNode.Obj(LinkedHashMap())))
        assertEquals("[]", JsonPrettyWriter.write(JsonNode.Arr(emptyList())))
    }

    @Test
    fun `a flat object is indented one level`() {
        val node = JsonNode.Obj(linkedMapOf("a" to JsonNode.Num(1.0), "b" to JsonNode.Bool(true)))
        assertEquals("{\n  \"a\": 1,\n  \"b\": true\n}", JsonPrettyWriter.write(node))
    }

    @Test
    fun `nesting increases the indent`() {
        val node = JsonNode.Obj(linkedMapOf("outer" to JsonNode.Obj(linkedMapOf("inner" to JsonNode.Str("x")))))
        assertEquals("{\n  \"outer\": {\n    \"inner\": \"x\"\n  }\n}", JsonPrettyWriter.write(node))
    }

    @Test
    fun `it round-trips back through MinimalJsonParser to an equal tree`() {
        val original = JsonNode.Obj(
            linkedMapOf(
                "s" to JsonNode.Str("hello \"world\""),
                "n" to JsonNode.Num(3.5),
                "arr" to JsonNode.Arr(listOf(JsonNode.Bool(false), JsonNode.Null)),
            ),
        )
        val printed = JsonPrettyWriter.write(original)
        assertEquals(original, MinimalJsonParser.parse(printed))
    }
}
