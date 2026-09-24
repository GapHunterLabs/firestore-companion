package dev.gaphunter.firestorecompanion.pro

import dev.gaphunter.firestorecompanion.json.JsonNode

/** The 6 comparison operators Firestore's structured query supports for a single scalar field filter -- the subset useful for a simple one-condition query builder (array/exists/IN operators are out of scope for this version). */
enum class QueryOperator(val restValue: String, val display: String) {
    EQUAL("EQUAL", "="),
    NOT_EQUAL("NOT_EQUAL", "!="),
    LESS_THAN("LESS_THAN", "<"),
    LESS_THAN_OR_EQUAL("LESS_THAN_OR_EQUAL", "<="),
    GREATER_THAN("GREATER_THAN", ">"),
    GREATER_THAN_OR_EQUAL("GREATER_THAN_OR_EQUAL", ">="),
}

enum class QueryValueType { STRING, INTEGER, DOUBLE, BOOLEAN }

data class QueryFilter(val fieldPath: String, val operator: QueryOperator, val valueType: QueryValueType, val valueText: String)


/** Builds the Firestore typed-value wrapper for a filter's comparison value, from raw dialog text -- same parsing rules as [dev.gaphunter.firestorecompanion.rest.FirestoreFieldEditor] uses for edits, applied here to a query condition instead. */
object QueryFilterValue {
    fun build(type: QueryValueType, text: String): JsonNode.Obj {
        val typed: JsonNode = when (type) {
            QueryValueType.STRING -> JsonNode.Str(text)
            QueryValueType.INTEGER -> JsonNode.Str(
                text.trim().toLongOrNull()?.toString() ?: throw IllegalArgumentException("'$text' isn't a whole number"),
            )
            QueryValueType.DOUBLE -> JsonNode.Num(
                text.trim().toDoubleOrNull() ?: throw IllegalArgumentException("'$text' isn't a number"),
            )
            QueryValueType.BOOLEAN -> JsonNode.Bool(
                when (text.trim().lowercase()) {
                    "true" -> true
                    "false" -> false
                    else -> throw IllegalArgumentException("'$text' isn't true or false")
                },
            )
        }
        val key = when (type) {
            QueryValueType.STRING -> "stringValue"
            QueryValueType.INTEGER -> "integerValue"
            QueryValueType.DOUBLE -> "doubleValue"
            QueryValueType.BOOLEAN -> "booleanValue"
        }
        return JsonNode.Obj(linkedMapOf(key to typed))
    }
}

/**
 * Builds the request body for Firestore's `:runQuery` REST endpoint
 * (structured query, single optional field filter, a result limit) --
 * pure JSON construction, no network/REST-client coupling, so it's
 * unit-testable on its own.
 */
object FirestoreQueryBuilder {
    fun buildRequestBody(collectionId: String, filter: QueryFilter?, limit: Int): JsonNode.Obj {
        val structuredQuery = linkedMapOf<String, JsonNode>(
            "from" to JsonNode.Arr(listOf(JsonNode.Obj(linkedMapOf("collectionId" to JsonNode.Str(collectionId))))),
            "limit" to JsonNode.Num(limit.toDouble()),
        )
        if (filter != null) {
            structuredQuery["where"] = JsonNode.Obj(
                linkedMapOf(
                    "fieldFilter" to JsonNode.Obj(
                        linkedMapOf(
                            "field" to JsonNode.Obj(linkedMapOf("fieldPath" to JsonNode.Str(filter.fieldPath))),
                            "op" to JsonNode.Str(filter.operator.restValue),
                            "value" to QueryFilterValue.build(filter.valueType, filter.valueText),
                        ),
                    ),
                ),
            )
        }
        return JsonNode.Obj(linkedMapOf("structuredQuery" to JsonNode.Obj(structuredQuery)))
    }

    /**
     * Splits a collection path into (parent, collectionId) for
     * `:runQuery`, which -- unlike a plain GET listing -- takes the
     * PARENT document/root path as the URL and names the collection
     * inside the request body's `from`. `"orders"` (root) -> `("",
     * "orders")`; `"users/alice/orders"` -> `("users/alice",
     * "orders")`.
     */
    fun splitParentAndCollection(collectionPath: String): Pair<String, String> {
        val lastSlash = collectionPath.lastIndexOf('/')
        return if (lastSlash < 0) "" to collectionPath else collectionPath.substring(0, lastSlash) to collectionPath.substring(lastSlash + 1)
    }
}
