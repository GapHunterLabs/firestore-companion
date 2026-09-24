package dev.gaphunter.firestorecompanion.toolwindow

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.gaphunter.firestorecompanion.auth.JwtBuilder
import dev.gaphunter.firestorecompanion.auth.OAuthTokenClient
import dev.gaphunter.firestorecompanion.auth.ServiceAccountParser
import dev.gaphunter.firestorecompanion.license.CheckLicense
import dev.gaphunter.firestorecompanion.pro.CollectionExporter
import dev.gaphunter.firestorecompanion.pro.ProjectProfile
import dev.gaphunter.firestorecompanion.pro.ProjectProfileStore
import dev.gaphunter.firestorecompanion.pro.QueryFilter
import dev.gaphunter.firestorecompanion.rest.FirestoreDocument
import dev.gaphunter.firestorecompanion.rest.FirestoreRestClient
import dev.gaphunter.firestorecompanion.rest.FirestoreValueFormatter
import dev.gaphunter.firestorecompanion.review.ReviewPrompt
import java.awt.BorderLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.JButton
import javax.swing.JComboBox
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
    private val profileStore = ProjectProfileStore(properties)

    /** Fail-closed: null (facade not ready) and false are both treated as unlicensed everywhere a Pro action is gated. */
    private val isPro: Boolean = CheckLicense.isLicensed() == true

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
    private val editButton = JButton("Edit Selected Document")

    // Pro-only: multi-project profiles, query filter, JSON export. None
    // of these widgets are added to the layout at all when !isPro --
    // fail-closed by construction, not just disabled-but-visible.
    private val profileCombo = JComboBox<String>()
    private val saveProfileButton = JButton("Save Profile")
    private val deleteProfileButton = JButton("Delete Profile")
    private val filterButton = JButton("Filter...")
    private val clearFilterButton = JButton("Clear Filter")
    private val exportButton = JButton("Export Collection to JSON...")

    private var restClient: FirestoreRestClient? = null
    private var currentDocuments: List<FirestoreDocument> = emptyList()
    private var currentPath: String = ""
    private var currentCollectionPath: String? = null
    private var currentFilter: QueryFilter? = null

    init {
        border = JBUI.Borders.empty(8)

        val topPanel = JPanel(GridLayout(if (isPro) 3 else 2, 3, 4, 4)).apply {
            add(JLabel("Service account JSON:"))
            add(serviceAccountPathField)
            add(browseButton)
            add(JLabel("Project ID:"))
            add(projectIdField)
            add(connectButton)
            if (isPro) {
                add(JLabel("Profile:"))
                add(profileCombo)
                val profileButtons = JPanel(GridLayout(1, 2, 2, 0)).apply {
                    add(saveProfileButton)
                    add(deleteProfileButton)
                }
                add(profileButtons)
            }
        }

        val navPanel = JPanel(BorderLayout()).apply {
            add(pathLabel, BorderLayout.CENTER)
            add(rootButton, BorderLayout.EAST)
        }

        val actionCount = if (isPro) 5 else 2
        val documentActionsPanel = JPanel(GridLayout(1, actionCount, 4, 0)).apply {
            add(subcollectionsButton)
            add(editButton)
            if (isPro) {
                add(filterButton)
                add(clearFilterButton)
                add(exportButton)
            }
        }

        val centerPanel = JPanel(BorderLayout()).apply {
            add(navPanel, BorderLayout.NORTH)
            add(JBScrollPane(collectionList), BorderLayout.WEST)
            add(JBScrollPane(documentsTable), BorderLayout.CENTER)
            add(documentActionsPanel, BorderLayout.SOUTH)
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
        editButton.addActionListener { editSelectedDocument() }

        if (isPro) {
            refreshProfileCombo()
            profileCombo.addActionListener { applySelectedProfile() }
            saveProfileButton.addActionListener { saveCurrentAsProfile() }
            deleteProfileButton.addActionListener { deleteSelectedProfile() }
            filterButton.addActionListener { openFilterDialog() }
            clearFilterButton.addActionListener { clearFilter() }
            exportButton.addActionListener { exportCurrentCollection() }
        }
    }

    private fun refreshProfileCombo() {
        val names = profileStore.listProfiles().map { it.name }
        profileCombo.model = javax.swing.DefaultComboBoxModel(names.toTypedArray())
        profileStore.activeProfileName()?.let { profileCombo.selectedItem = it }
    }

    private fun applySelectedProfile() {
        val name = profileCombo.selectedItem as? String ?: return
        val profile = profileStore.listProfiles().firstOrNull { it.name == name } ?: return
        serviceAccountPathField.text = profile.serviceAccountPath
        projectIdField.text = profile.projectId
        profileStore.setActiveProfile(name)
    }

    private fun saveCurrentAsProfile() {
        val serviceAccountPath = serviceAccountPathField.text.trim()
        val projectId = projectIdField.text.trim()
        if (serviceAccountPath.isBlank() || projectId.isBlank()) {
            Messages.showErrorDialog(project, "Set both the service account JSON path and the project ID before saving a profile.", "Firestore Companion")
            return
        }
        val name = Messages.showInputDialog(project, "Profile name (e.g. dev, staging, prod):", "Save Profile", null) ?: return
        if (name.isBlank()) return
        profileStore.saveProfile(ProjectProfile(name.trim(), serviceAccountPath, projectId))
        profileStore.setActiveProfile(name.trim())
        refreshProfileCombo()
    }

    private fun deleteSelectedProfile() {
        val name = profileCombo.selectedItem as? String ?: return
        profileStore.deleteProfile(name)
        refreshProfileCombo()
    }

    private fun openFilterDialog() {
        val collectionPath = currentCollectionPath
        if (collectionPath == null) {
            Messages.showErrorDialog(project, "Select a collection first.", "Firestore Companion")
            return
        }
        val dialog = QueryFilterDialog(collectionPath.substringAfterLast('/', collectionPath))
        if (!dialog.showAndGet()) return
        val filter = try {
            dialog.buildFilter()
        } catch (e: IllegalArgumentException) {
            Messages.showErrorDialog(project, e.message ?: e.toString(), "Firestore Companion")
            return
        }
        currentFilter = filter
        runQuery(collectionPath, filter)
    }

    private fun clearFilter() {
        currentFilter = null
        currentCollectionPath?.let { loadDocuments(it.substringAfterLast('/', it)) }
    }

    private fun runQuery(collectionPath: String, filter: QueryFilter) {
        val client = restClient ?: return
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val documents = client.queryDocuments(collectionPath, filter)
                onEdt { setDocuments(documents) }
            } catch (e: Exception) {
                onEdt { Messages.showErrorDialog(project, e.message ?: e.toString(), "Firestore Companion") }
            }
        }
    }

    private fun exportCurrentCollection() {
        if (currentDocuments.isEmpty()) {
            Messages.showErrorDialog(project, "No documents loaded to export -- select a collection first.", "Firestore Companion")
            return
        }
        // Force the vararg constructor (String, String, String...), not the fixed-arity
        // (String, String, String) overload IntelliJ 2025.2+ added: that overload doesn't
        // exist on sinceBuild=243, and a single bare "json" arg binds to it as an exact
        // match, which is a real verifyPlugin NoSuchMethodError risk on 243, confirmed by
        // diffing the actual FileSaverDescriptor.class across both SDK jars.
        val descriptor = FileSaverDescriptor("Export Collection to JSON", "Choose where to save the exported documents", *arrayOf("json"))
        val wrapper = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
            .save(currentCollectionPath?.substringAfterLast('/', "collection") + ".json") ?: return
        try {
            wrapper.file.writeText(CollectionExporter.toJson(currentDocuments), Charsets.UTF_8)
        } catch (e: Exception) {
            Messages.showErrorDialog(project, e.message ?: e.toString(), "Firestore Companion")
        }
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
                // Real, explicit connection success -- the strongest single
                // "used this for real" signal this plugin has, never fired
                // for a failed auth attempt (caught below).
                ReviewPrompt.recordHit(project)
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
        currentCollectionPath = collectionPath
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

    private fun editSelectedDocument() {
        val row = documentsTable.selectedRow
        if (row < 0 || row >= currentDocuments.size) {
            Messages.showErrorDialog(project, "Select a document row first.", "Firestore Companion")
            return
        }
        val document = currentDocuments[row]
        val dialog = EditDocumentDialog(document.id, document.fields)
        if (!dialog.showAndGet()) return

        val changed = try {
            dialog.changedFields()
        } catch (e: IllegalArgumentException) {
            Messages.showErrorDialog(project, e.message ?: e.toString(), "Firestore Companion")
            return
        }
        if (changed.entries.isEmpty()) return

        val client = restClient ?: return
        val collectionPath = currentCollectionPath
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                client.patchDocument(document.name, changed)
                onEdt { collectionPath?.let { loadDocuments(it.substringAfterLast('/', it)) } }
            } catch (e: Exception) {
                onEdt { Messages.showErrorDialog(project, e.message ?: e.toString(), "Firestore Companion") }
            }
        }
    }

    private fun onEdt(action: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(action)
    }
}
