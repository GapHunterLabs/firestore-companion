package dev.gaphunter.firestorecompanion.toolwindow

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import dev.gaphunter.firestorecompanion.json.JsonNode
import dev.gaphunter.firestorecompanion.rest.FirestoreFieldEditor
import dev.gaphunter.firestorecompanion.rest.FirestoreValueFormatter
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * One row per field: editable [JBTextField] for scalar types
 * (string/integer/double/boolean), a disabled, pre-filled field for
 * anything else (map/array/geoPoint/reference/timestamp/null) -- see
 * [FirestoreFieldEditor] for why those stay read-only in this version.
 *
 * [changedFields] only returns fields whose text actually changed, so
 * the caller's `patchDocument` update mask never touches fields the
 * user didn't edit.
 */
class EditDocumentDialog(
    documentId: String,
    private val fields: JsonNode.Obj,
) : DialogWrapper(true) {
    private val editors = LinkedHashMap<String, JBTextField>()

    init {
        title = "Edit Document: $documentId"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val entries = fields.entries.entries.toList()
        val panel = JPanel(GridLayout(entries.size.coerceAtLeast(1), 3, 6, 4))
        for ((key, value) in entries) {
            val typedObj = value as? JsonNode.Obj
            val editable = typedObj != null && FirestoreFieldEditor.isEditable(value)
            val field = JBTextField(FirestoreValueFormatter.format(value))
            field.isEditable = editable
            if (editable) editors[key] = field

            panel.add(JBLabel(key))
            panel.add(field)
            panel.add(JBLabel(if (editable) "" else "(read-only)"))
        }
        return panel
    }

    /** @throws IllegalArgumentException if an edited value doesn't parse as its field's type. */
    fun changedFields(): JsonNode.Obj {
        val changed = LinkedHashMap<String, JsonNode>()
        for ((key, field) in editors) {
            val original = fields.entries[key] as? JsonNode.Obj ?: continue
            val newText = field.text
            if (newText == FirestoreValueFormatter.format(original)) continue
            changed[key] = FirestoreFieldEditor.buildEditedValue(original, newText)
        }
        return JsonNode.Obj(changed)
    }
}
