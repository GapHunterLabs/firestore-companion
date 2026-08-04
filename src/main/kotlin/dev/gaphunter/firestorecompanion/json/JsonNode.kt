package dev.gaphunter.firestorecompanion.json

sealed class JsonNode {
    object Null : JsonNode()
    data class Bool(val value: Boolean) : JsonNode()
    data class Num(val value: Double) : JsonNode()
    data class Str(val value: String) : JsonNode()
    data class Arr(val items: List<JsonNode>) : JsonNode()
    data class Obj(val entries: LinkedHashMap<String, JsonNode>) : JsonNode()
}

class FirestoreJsonException(message: String) : Exception(message)
