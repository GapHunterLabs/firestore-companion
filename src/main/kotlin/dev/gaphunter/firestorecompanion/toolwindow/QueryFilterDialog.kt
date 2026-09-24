package dev.gaphunter.firestorecompanion.toolwindow

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import dev.gaphunter.firestorecompanion.pro.QueryFilter
import dev.gaphunter.firestorecompanion.pro.QueryFilterValue
import dev.gaphunter.firestorecompanion.pro.QueryOperator
import dev.gaphunter.firestorecompanion.pro.QueryValueType
import java.awt.GridLayout
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Pro feature: a single field/operator/value condition for
 * [dev.gaphunter.firestorecompanion.rest.FirestoreRestClient.queryDocuments],
 * instead of [dev.gaphunter.firestorecompanion.rest.FirestoreRestClient.listDocuments]
 * always bringing back every document in a collection.
 */
class QueryFilterDialog(collectionName: String) : DialogWrapper(true) {
    private val fieldPathField = JBTextField()
    private val operatorCombo = JComboBox(QueryOperator.entries.toTypedArray())
    private val valueTypeCombo = JComboBox(QueryValueType.entries.toTypedArray())
    private val valueField = JBTextField()

    init {
        title = "Filter \"$collectionName\""
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(4, 2, 6, 6))
        panel.add(JBLabel("Field path"))
        panel.add(fieldPathField)
        panel.add(JBLabel("Operator"))
        panel.add(operatorCombo)
        panel.add(JBLabel("Value type"))
        panel.add(valueTypeCombo)
        panel.add(JBLabel("Value"))
        panel.add(valueField)
        return panel
    }

    /** @throws IllegalArgumentException if the field path is blank or the value doesn't parse as the chosen type. */
    fun buildFilter(): QueryFilter {
        val fieldPath = fieldPathField.text.trim()
        if (fieldPath.isEmpty()) throw IllegalArgumentException("Field path can't be empty.")
        val valueType = valueTypeCombo.selectedItem as QueryValueType
        val valueText = valueField.text
        // Validate eagerly so a bad value is caught here, not after the network round-trip.
        QueryFilterValue.build(valueType, valueText)
        return QueryFilter(fieldPath, operatorCombo.selectedItem as QueryOperator, valueType, valueText)
    }
}
