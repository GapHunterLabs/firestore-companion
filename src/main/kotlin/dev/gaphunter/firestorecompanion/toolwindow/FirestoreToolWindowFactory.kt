package dev.gaphunter.firestorecompanion.toolwindow

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import dev.gaphunter.firestorecompanion.auth.JwtBuilder
import dev.gaphunter.firestorecompanion.auth.OAuthTokenClient
import dev.gaphunter.firestorecompanion.auth.ServiceAccountParser
import dev.gaphunter.firestorecompanion.rest.FirestoreDocument
import dev.gaphunter.firestorecompanion.rest.FirestoreRestClient
import dev.gaphunter.firestorecompanion.rest.FirestoreValueFormatter
import java.awt.BorderLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

private const val PROP_SERVICE_ACCOUNT_PATH = "dev.gaphunter.firestorecompanion.serviceAccountPath"
private const val PROP_PROJECT_ID = "dev.gaphunter.firestorecompanion.projectId"

class FirestoreToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = FirestorePanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

/**
 * Collection list (left) + documents table (right), with breadcrumb-style
 * drill-down into subcollections -- the direct answer to the cited
 * complaint: "it works very poorly with sub-collections. I would like to
 * see these sub-collections in tabular form as well, not just in JSON."
 * All network I/O runs on a pooled thread; only Swing updates happen via
 * `invokeLater`. The service account JSON's contents (and the resulting
 * access token) are held in memory only for the duration of a session --
 * only the file PATH and project ID are persisted (via
 * [PropertiesComponent], not a secret store, because neither value is a
 * secret).
 */
private class FirestorePanel(private val project: Project) : JPanel(BorderLayout()) {
    private val properties = PropertiesComponent.getInstance(project)

    private val serviceAccountPathField = JBTextField(properties.getValue(PROP_SERVICE_ACCOUNT_PATH, ""))
    private val projectIdField = JBTextField(properties.getValue(PROP_PROJECT_ID, ""))
    private val connectButton = JButton("Connect")
    private val browseButton = JButton("Browse...")
    private val pathLabel = JLabel("Path: (root)")
    private val rootButton = JButton("Root")
    private val collectionListModel = javax.swing.DefaultListModel<String>()
    private val collectionList = JBList(collectionListModel)
    private val documentsTableModel = DefaultTableModel(arrayOf("Document ID", "Fields"), 0)
    private val documentsTable = JBTable(documentsTableModel)
    private val subcollectionsButton = JButton("Open Subcollections of Selected Document")

    private var restClient: FirestoreRestClient? = null
    private var currentDocuments: List<FirestoreDocument> = emptyList()
    private var currentPath: String = ""

    init {
        val topPanel = JPanel(GridLayout(2, 3, 4, 4)).apply {
            add(JLabel("Service account JSON:"))
            add(serviceAccountPathField)
            add(browseButton)
            add(JLabel("Project ID:"))
            add(projectIdField)
            add(connectButton)
        }

        val navPanel = JPanel(BorderLayout()).apply {
            add(pathLabel, BorderLayout.CENTER)
            add(rootButton, BorderLayout.EAST)
        }

        val centerPanel = JPanel(BorderLayout()).apply {
            add(navPanel, BorderLayout.NORTH)
            add(JBScrollPane(collectionList), BorderLayout.WEST)
            add(JBScrollPane(documentsTable), BorderLayout.CENTER)
            add(subcollectionsButton, BorderLayout.SOUTH)
        }

        add(topPanel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)

        browseButton.addActionListener { browseForServiceAccount() }
        connectButton.addActionListener { connect() }
        rootButton.addActionListener { navigateTo("") }
        collectionList.addListSelectionListener {
            if (!it.valueIsAdjusting) collectionList.selectedValue?.let { collectionId -> loadDocuments(collectionId) }
        }
        subcollectionsButton.addActionListener { openSubcollectionsOfSelectedDocument() }
    }

    private fun browseForServiceAccount() {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json")
        val file = FileChooser.chooseFile(descriptor, project, null) ?: return
        serviceAccountPathField.text = file.path
    }

    private fun connect() {
        val serviceAccountPath = serviceAccountPathField.text.trim()
        val projectId = projectIdField.text.trim()
        if (serviceAccountPath.isBlank() || projectId.isBlank()) {
            Messages.showErrorDialog(project, "Set both the service account JSON path and the project ID.", "Firestore Companion")
            return
        }
        properties.setValue(PROP_SERVICE_ACCOUNT_PATH, serviceAccountPath)
        properties.setValue(PROP_PROJECT_ID, projectId)

        connectButton.isEnabled = false
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val rawJson = File(serviceAccountPath).readText(Charsets.UTF_8)
                val credentials = ServiceAccountParser.parse(rawJson)
                val assertion = JwtBuilder.buildSignedAssertion(credentials, System.currentTimeMillis() / 1000)
                val accessToken = OAuthTokenClient().fetchAccessToken(credentials, assertion)
                restClient = FirestoreRestClient(projectId, accessToken)
                navigateTo("")
                onEdt { connectButton.isEnabled = true }
            } catch (e: Exception) {
                onEdt {
                    connectButton.isEnabled = true
                    Messages.showErrorDialog(project, e.message ?: e.toString(), "Firestore Connection Failed")
                }
            }
        }
    }

    private fun navigateTo(documentPath: String) {
        val client = restClient ?: return
        currentPath = documentPath
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val collectionIds = if (documentPath.isBlank()) {
                    client.listRootCollectionIds()
                } else {
                    client.listSubcollectionIds(documentPath)
                }
                onEdt {
                    pathLabel.text = "Path: " + documentPath.ifBlank { "(root)" }
                    collectionListModel.clear()
                    collectionIds.forEach { collectionListModel.addElement(it) }
                    setDocuments(emptyList())
                }
            } catch (e: Exception) {
                onEdt { Messages.showErrorDialog(project, e.message ?: e.toString(), "Firestore Companion") }
            }
        }
    }

    private fun loadDocuments(collectionId: String) {
        val client = restClient ?: return
        val collectionPath = if (currentPath.isBlank()) collectionId else "$currentPath/$collectionId"
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val documents = client.listDocuments(collectionPath)
                onEdt { setDocuments(documents) }
            } catch (e: Exception) {
                onEdt { Messages.showErrorDialog(project, e.message ?: e.toString(), "Firestore Companion") }
            }
        }
    }

    private fun setDocuments(documents: List<FirestoreDocument>) {
        currentDocuments = documents
        documentsTableModel.rowCount = 0
        for (document in documents) {
            documentsTableModel.addRow(arrayOf(document.id, FirestoreValueFormatter.formatFields(document.fields)))
        }
    }

    private fun openSubcollectionsOfSelectedDocument() {
        val row = documentsTable.selectedRow
        if (row < 0 || row >= currentDocuments.size) {
            Messages.showErrorDialog(project, "Select a document row first.", "Firestore Companion")
            return
        }
        val document = currentDocuments[row]
        val documentsPrefix = "/documents/"
        val relativePath = document.name.substringAfter(documentsPrefix, document.name)
        navigateTo(relativePath)
    }

    private fun onEdt(action: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(action)
    }
}
