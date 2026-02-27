package com.frshaka.gitbot.settings

import com.frshaka.gitbot.ai.OpenRouterClient
import com.frshaka.gitbot.prompt.PromptLoader
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class GitBotConfigurable : Configurable {

    private var panel: JPanel? = null

    private val apiKeyField = JPasswordField()
    private val availableModels = loadOpenrouterModels().toMutableList()
    private val modelField = ComboBox<String>()
    private val languageCombo = ComboBox(arrayOf("PT_BR", "EN"))

    private val promptArea = JTextArea(14, 60).apply {
        lineWrap = true
        wrapStyleWord = true
    }

    private val resetButton = JButton("Reset to default")

    override fun getDisplayName(): String = "GitBot Commit"

    override fun createComponent(): JComponent {
        val root = JPanel(BorderLayout())
        val form = JPanel(GridBagLayout())

        val c = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(6, 6, 6, 6)
        }

        fun row(y: Int, label: String, comp: JComponent) {
            c.gridy = y

            c.gridx = 0
            c.weightx = 0.0
            form.add(JBLabel(label), c)

            c.gridx = 1
            c.weightx = 1.0
            form.add(comp, c)
        }

        row(0, "OpenRouter API Key:", apiKeyField)
        row(1, "Model:", modelField)
        row(2, "Commit language:", languageCombo)

        // Prompt editor
        c.gridy = 3
        c.gridx = 0
        c.weightx = 0.0
        c.anchor = GridBagConstraints.NORTHWEST
        form.add(JBLabel("Prompt Template:"), c)

        c.gridx = 1
        c.weightx = 1.0
        c.fill = GridBagConstraints.BOTH
        val promptScroll = JBScrollPane(promptArea).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }
        form.add(promptScroll, c)

        // Reset button under prompt
        c.gridy = 4
        c.gridx = 1
        c.weightx = 1.0
        c.fill = GridBagConstraints.NONE
        c.anchor = GridBagConstraints.WEST
        form.add(resetButton, c)

        // Listeners
        resetButton.addActionListener {
            val lang = languageCombo.selectedItem as String
            promptArea.text = loadDefaultPrompt(lang)
        }

        languageCombo.addActionListener {
            // Quando troca idioma, atualiza o prompt mostrado conforme o que está salvo
            val settings = GitBotSettingsService.getInstance().state
            val lang = languageCombo.selectedItem as String
            ensureDefaultsLoaded(settings)

            promptArea.text = if (lang == "EN") settings.promptEn else settings.promptPtBr
        }

        // Configure model field to show searchable popup
        configureModelField()
        root.add(form, BorderLayout.CENTER)

        panel = root
        reset()
        return root
    }

    override fun isModified(): Boolean {
        val settings = GitBotSettingsService.getInstance().state
        ensureDefaultsLoaded(settings)

        val savedKey = GitBotSecrets.getApiKey() ?: ""

        val uiKey = String(apiKeyField.password).trim()
        val uiModel = modelField.selectedItem as String
        val uiLang = languageCombo.selectedItem as String
        val uiPrompt = promptArea.text

        val currentSavedPrompt = if (uiLang == "EN") settings.promptEn else settings.promptPtBr

        return uiKey != savedKey ||
                uiModel != settings.model ||
                uiLang != settings.language ||
                uiPrompt != currentSavedPrompt
    }

    override fun apply() {
        val settings = GitBotSettingsService.getInstance().state
        ensureDefaultsLoaded(settings)

        val uiKey = String(apiKeyField.password).trim()
        val uiModel = modelField.selectedItem as String
        val uiLang = languageCombo.selectedItem as String
        val uiPrompt = promptArea.text

        if (uiKey.isNotEmpty()) {
            GitBotSecrets.setApiKey(uiKey)
        }

        settings.model = uiModel
        settings.language = uiLang

        if (uiLang == "EN") {
            settings.promptEn = uiPrompt
        } else {
            settings.promptPtBr = uiPrompt
        }
    }

    override fun reset() {
        val settings = GitBotSettingsService.getInstance().state
        ensureDefaultsLoaded(settings)

        val savedKey = GitBotSecrets.getApiKey() ?: ""

        apiKeyField.text = savedKey
        ensureModelInList(settings.model)
        modelField.selectedItem = settings.model
        languageCombo.selectedItem = settings.language

        promptArea.text = if (settings.language == "EN") settings.promptEn else settings.promptPtBr
    }

    override fun disposeUIResources() {
        panel = null
    }

    private fun ensureModelInList(modelName: String) {
        if (modelName.isBlank()) return
        if (modelName !in availableModels) {
            availableModels += modelName
            availableModels.sort()
        }
    }

    private fun configureModelField() {
        // Make the combo box non-editable
        modelField.isEditable = false
        
        // Populate initial model list
        availableModels.forEach { modelField.addItem(it) }
        
        // Intercept mouse clicks to show custom popup instead of default
        modelField.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                // Prevent default popup and show custom one
                e.consume()
                showSearchablePopup()
            }
        })
        
        // Also prevent default popup by removing the default UI behavior
        modelField.isPopupVisible = false
    }
    
    private fun showSearchablePopup() {
        val searchField = JTextField(20)
        val listModel = DefaultListModel<String>()
        availableModels.forEach { listModel.addElement(it) }
        
        val list = JBList(listModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            setSelectedValue(modelField.selectedItem, true)
        }
        
        // Match popup width to combo box width and increase height
        val comboWidth = modelField.width.coerceAtLeast(400)
        val popupHeight = 400
        
        val scrollPane = JBScrollPane(list)
        scrollPane.preferredSize = java.awt.Dimension(comboWidth, popupHeight)
        
        val panel = JPanel(BorderLayout()).apply {
            add(searchField, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
            preferredSize = java.awt.Dimension(comboWidth, popupHeight + 30) // +30 for search field
        }
        
        // Filter list as user types
        searchField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) = filterList()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) = filterList()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) = filterList()
            
            private fun filterList() {
                val filter = searchField.text.trim()
                listModel.clear()
                availableModels
                    .filter { it.contains(filter, ignoreCase = true) }
                    .forEach { listModel.addElement(it) }
            }
        })
        
        val popup = com.intellij.openapi.ui.popup.JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, searchField)
            .setTitle("Select Model")
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .createPopup()
        
        // Handle selection
        list.addListSelectionListener {
            if (!it.valueIsAdjusting && list.selectedValue != null) {
                modelField.selectedItem = list.selectedValue
                popup.closeOk(null)
            }
        }
        
        // Show popup below the combo box
        popup.showUnderneathOf(modelField)
    }

    private fun ensureDefaultsLoaded(settings: GitBotSettingsState) {
        if (settings.promptPtBr.isBlank()) {
            settings.promptPtBr = loadDefaultPrompt("PT_BR")
        }
        if (settings.promptEn.isBlank()) {
            settings.promptEn = loadDefaultPrompt("EN")
        }
    }

    private fun loadDefaultPrompt(lang: String): String {
        return if (lang == "EN") {
            PromptLoader.load("prompts/commit_prompt_en.txt")
        } else {
            PromptLoader.load("prompts/commit_prompt_ptbr.txt")
        }
    }

    private fun loadOpenrouterModels(): List<String> {
        val apiKey = GitBotSecrets.getApiKey() ?: return emptyList()
        val openrouter = OpenRouterClient(apiKey)

        val models = openrouter.models()

        return models
            .map { model -> model.id }
            .sorted()
    }
}