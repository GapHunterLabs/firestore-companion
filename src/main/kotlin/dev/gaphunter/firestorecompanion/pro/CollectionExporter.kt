package dev.gaphunter.firestorecompanion.pro

import dev.gaphunter.firestorecompanion.json.JsonNode
import dev.gaphunter.firestorecompanion.json.JsonPrettyWriter
import dev.gaphunter.firestorecompanion.rest.FirestoreDocument

/**
 * Renders a document list as a pretty-printed JSON array, one object
 * per document (`id` + the raw Firestore typed-value `fields` wrapper,
 * not a flattened/lossy view) -- Pro feature: a quick local backup/
 * debug snapshot of a collection. Keeps the typed-value wrapper
 * (`{"stringValue": ...}`, `{"integerValue": "5"}`, ...) exactly as
 * Firestore returned it rather than reformatting field values, so an
 * exported file could in principle be fed back through
 * [dev.gaphunter.firestorecompanion.rest.FirestoreFieldEditor] without
 * losing int64 precision or type information.
 */
object CollectionExporter {
    fun toJson(documents: List<FirestoreDocument>): String {
        val array = JsonNode.Arr(
            documents.map { doc ->
                JsonNode.Obj(linkedMapOf("id" to JsonNode.Str(doc.id), "fields" to doc.fields))
            },
        )
        return JsonPrettyWriter.write(array)
    }
}
