package dev.gaphunter.firestorecompanion.pro

import dev.gaphunter.firestorecompanion.json.JsonNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class FirestoreQueryTest {

    @Test
    fun `builds a string equality filter value as stringValue`() {
        val value = QueryFilterValue.build(QueryValueType.STRING, "shipped")
        assertEquals(JsonNode.Obj(linkedMapOf("stringValue" to JsonNode.Str("shipped"))), value)
    }

    @Test
    fun `builds an integer filter value as a string, same int64-precision rule as editing`() {
        val value = QueryFilterValue.build(QueryValueType.INTEGER, "9223372036854775807")
        assertEquals(JsonNode.Obj(linkedMapOf("integerValue" to JsonNode.Str("9223372036854775807"))), value)
    }

    @Test
    fun `rejects a non-numeric integer filter value`() {
        assertThrows(IllegalArgumentException::class.java) { QueryFilterValue.build(QueryValueType.INTEGER, "not-a-number") }
    }

    @Test
    fun `builds a boolean filter value case-insensitively`() {
        assertEquals(JsonNode.Obj(linkedMapOf("booleanValue" to JsonNode.Bool(true))), QueryFilterValue.build(QueryValueType.BOOLEAN, "TRUE"))
    }

    @Test
    fun `request body with no filter has from and limit but no where clause`() {
        val body = FirestoreQueryBuilder.buildRequestBody("orders", null, 50)
        val structuredQuery = body.entries.getValue("structuredQuery") as JsonNode.Obj
        assertEquals(
            JsonNode.Arr(listOf(JsonNode.Obj(linkedMapOf("collectionId" to JsonNode.Str("orders"))))),
            structuredQuery.entries["from"],
        )
        assertEquals(JsonNode.Num(50.0), structuredQuery.entries["limit"])
        assertNull(structuredQuery.entries["where"])
    }

    @Test
    fun `request body with a filter builds the fieldFilter shape Firestore expects`() {
        val filter = QueryFilter("status", QueryOperator.EQUAL, QueryValueType.STRING, "shipped")
        val body = FirestoreQueryBuilder.buildRequestBody("orders", filter, 100)
        val structuredQuery = body.entries.getValue("structuredQuery") as JsonNode.Obj
        val where = structuredQuery.entries.getValue("where") as JsonNode.Obj
        val fieldFilter = where.entries.getValue("fieldFilter") as JsonNode.Obj
        assertEquals(
            JsonNode.Obj(linkedMapOf("fieldPath" to JsonNode.Str("status"))),
            fieldFilter.entries["field"],
        )
        assertEquals(JsonNode.Str("EQUAL"), fieldFilter.entries["op"])
        assertEquals(JsonNode.Obj(linkedMapOf("stringValue" to JsonNode.Str("shipped"))), fieldFilter.entries["value"])
    }

    @Test
    fun `splits a root collection path into an empty parent`() {
        assertEquals("" to "orders", FirestoreQueryBuilder.splitParentAndCollection("orders"))
    }

    @Test
    fun `splits a nested collection path into its document parent and the final collection id`() {
        assertEquals("users/alice" to "orders", FirestoreQueryBuilder.splitParentAndCollection("users/alice/orders"))
    }
}
