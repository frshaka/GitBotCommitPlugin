package com.frshaka.gitbot.settings

import com.frshaka.gitbot.ai.LLMProviderFactory
import com.frshaka.gitbot.ai.ProviderHealth
import com.frshaka.gitbot.ai.ProviderType
import com.frshaka.gitbot.prompt.PromptLoader
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import java.awt.Color
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities

class GitBotConfigurable : Configurable {

    private val settings get() = GitBotSettingsService.getInstance().state

    private val providerProperty = AtomicProperty(ProviderType.fromName(settings.provider))

    private fun providerIs(target: ProviderType): ComponentPredicate = object : ComponentPredicate() {
        override fun invoke(): Boolean = providerProperty.get() == target
        override fun addListener(listener: (Boolean) -> Unit) {
            providerProperty.afterChange { listener(it == target) }
        }
    }

    private val apiKeyField = JPasswordField()
    private val ollamaUrlField = JTextField()

    private val availableModels = mutableListOf<String>()
    private val modelField = ComboBox<String>().apply { isEditable = true }

    private val languageCombo = ComboBox(arrayOf("PT_BR", "EN"))

    private val promptArea = JTextArea(14, 60).apply {
        lineWrap = true
        wrapStyleWord = true
    }

    private val resetPromptButton = JButton("Reset to default")
    private val loadModelsButton = JButton("Load Models")
    private val testConnectionButton = JButton("Test Connection")

    private var dialogPanel: DialogPanel? = null

    override fun getDisplayName(): String = "GitBot Commit"

    override fun createComponent(): JComponent {
        configureModelField()

        val pwdWarning = buildMemoryOnlyWarning()

        val panel = panel {
            if (pwdWarning != null) {
                row {
                    cell(pwdWarning).align(AlignX.FILL)
                }.visibleIf(providerIs(ProviderType.OPENROUTER))
            }

            row("Provider:") {
                comboBox(ProviderType.entries.toList())
                    .applyToComponent {
                        renderer = javax.swing.DefaultListCellRenderer().also { it.horizontalAlignment = JLabel.LEFT }
                        setRenderer { list, value, index, selected, focused ->
                            val text = (value as? ProviderType)?.displayName ?: ""
                            javax.swing.DefaultListCellRenderer().getListCellRendererComponent(list, text, index, selected, focused)
                        }
                        selectedItem = providerProperty.get()
                        addActionListener {
                            val newProvider = selectedItem as ProviderType
                            onProviderSwitch(newProvider)
                        }
                    }
            }

            row("OpenRouter API Key:") {
                cell(apiKeyField).align(AlignX.FILL)
            }.visibleIf(providerIs(ProviderType.OPENROUTER))

            row("Server URL:") {
                cell(ollamaUrlField).align(AlignX.FILL)
            }.visibleIf(providerIs(ProviderType.OLLAMA))

            row("") {
                cell(testConnectionButton)
            }.visibleIf(providerIs(ProviderType.OLLAMA))

            row("Model:") {
                cell(modelField).align(AlignX.FILL).resizableColumn().gap(RightGap.SMALL)
                cell(loadModelsButton)
            }

            row("Commit language:") {
                cell(languageCombo)
            }

            row("Prompt Template:") {
                cell(buildPromptScroll()).align(AlignX.FILL).align(AlignY.FILL).resizableColumn()
            }.resizableRow()

            row("") {
                cell(resetPromptButton)
            }
        }

        attachListeners()
        dialogPanel = panel
        reset()
        return panel
    }

    private fun buildPromptScroll(): JBScrollPane = JBScrollPane(promptArea).apply {
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        preferredSize = Dimension(600, 280)
    }

    private fun buildMemoryOnlyWarning(): JComponent? {
        if (!GitBotSecrets.isMemoryOnly()) return null
        val lang = settings.language
        val text = if (lang == "PT_BR") {
            "<html>" +
            "<b>⚠ Atenção: a API Key não será salva entre sessões da IDE.</b><br/><br/>" +
            "O cofre de senhas do IntelliJ está operando apenas em memória porque o " +
            "gerenciador de credenciais do sistema não está disponível.<br/><br/>" +
            "<b>Como corrigir:</b><br/>" +
            "• <b>Windows:</b> verifique se o <i>Windows Credential Manager</i> está ativo.<br/>" +
            "• <b>Linux:</b> instale e inicie o <i>KWallet</i> ou <i>GNOME Keyring (SecretService)</i>.<br/>" +
            "• <b>macOS:</b> verifique se o <i>Keychain Access</i> está desbloqueado." +
            "</html>"
        } else {
            "<html>" +
            "<b>⚠ Warning: your API Key will not be saved between IDE sessions.</b><br/><br/>" +
            "IntelliJ's password safe is running in memory-only mode because the system " +
            "credential manager is not available.<br/><br/>" +
            "<b>How to fix:</b><br/>" +
            "• <b>Windows:</b> make sure <i>Windows Credential Manager</i> is enabled.<br/>" +
            "• <b>Linux:</b> install and start <i>KWallet</i> or <i>GNOME Keyring</i>.<br/>" +
            "• <b>macOS:</b> make sure <i>Keychain Access</i> is unlocked." +
            "</html>"
        }
        return JLabel(text).apply {
            foreground = Color(180, 80, 0)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(200, 120, 0)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            )
        }
    }

    private fun attachListeners() {
        resetPromptButton.addActionListener {
            val lang = languageCombo.selectedItem as String
            promptArea.text = loadDefaultPrompt(lang)
        }

        languageCombo.addActionListener {
            val s = settings
            val lang = languageCombo.selectedItem as String
            ensureDefaultsLoaded(s)
            promptArea.text = if (lang == "EN") s.promptEn else s.promptPtBr
        }

        loadModelsButton.addActionListener { loadModelsAsync() }

        testConnectionButton.addActionListener { testOllamaConnection() }
    }

    private fun onProviderSwitch(newProvider: ProviderType) {
        val current = providerProperty.get()
        if (current == newProvider) return

        // Persiste o modelo atual no campo correspondente ao provider anterior em memória
        val currentModelText = (modelField.editor.item as? String)?.trim()
            ?: (modelField.selectedItem as? String)?.trim().orEmpty()

        when (current) {
            ProviderType.OPENROUTER -> currentOpenRouterModel = currentModelText
            ProviderType.OLLAMA -> currentOllamaModel = currentModelText
        }

        // Limpa lista e popula campo com modelo do novo provider
        availableModels.clear()
        modelField.removeAllItems()
        val nextModel = when (newProvider) {
            ProviderType.OPENROUTER -> currentOpenRouterModel
            ProviderType.OLLAMA -> currentOllamaModel
        }
        if (nextModel.isNotBlank()) {
            availableModels += nextModel
            modelField.addItem(nextModel)
            modelField.selectedItem = nextModel
        }

        providerProperty.set(newProvider)
    }

    private var currentOpenRouterModel: String = ""
    private var currentOllamaModel: String = ""

    override fun isModified(): Boolean {
        val s = settings
        ensureDefaultsLoaded(s)

        val savedKey = GitBotSecrets.getOpenRouterApiKey() ?: ""
        val uiKey = String(apiKeyField.password).trim()
        val uiUrl = ollamaUrlField.text.trim()
        val uiProvider = providerProperty.get().name
        val uiModel = currentModelText()
        val uiLang = languageCombo.selectedItem as String
        val uiPrompt = promptArea.text

        val savedPrompt = if (uiLang == "EN") s.promptEn else s.promptPtBr
        val savedModel = if (providerProperty.get() == ProviderType.OPENROUTER) s.model else s.ollamaModel

        return uiProvider != s.provider ||
                uiKey != savedKey ||
                uiUrl != s.ollamaBaseUrl ||
                uiModel != savedModel ||
                uiLang != s.language ||
                uiPrompt != savedPrompt
    }

    override fun apply() {
        val s = settings
        ensureDefaultsLoaded(s)

        val uiProvider = providerProperty.get()
        val uiKey = String(apiKeyField.password).trim()
        val uiUrl = ollamaUrlField.text.trim().ifEmpty { "http://localhost:11434" }
        val uiModel = currentModelText()
        val uiLang = languageCombo.selectedItem as String
        val uiPrompt = promptArea.text

        if (uiKey.isNotEmpty()) {
            GitBotSecrets.setOpenRouterApiKey(uiKey)
        }

        s.provider = uiProvider.name
        s.ollamaBaseUrl = uiUrl
        when (uiProvider) {
            ProviderType.OPENROUTER -> {
                s.model = uiModel
                currentOpenRouterModel = uiModel
            }
            ProviderType.OLLAMA -> {
                s.ollamaModel = uiModel
                currentOllamaModel = uiModel
            }
        }
        s.language = uiLang
        if (uiLang == "EN") s.promptEn = uiPrompt else s.promptPtBr = uiPrompt
    }

    override fun reset() {
        val s = settings
        ensureDefaultsLoaded(s)

        val provider = ProviderType.fromName(s.provider)
        providerProperty.set(provider)

        apiKeyField.text = GitBotSecrets.getOpenRouterApiKey() ?: ""
        ollamaUrlField.text = s.ollamaBaseUrl
        languageCombo.selectedItem = s.language
        promptArea.text = if (s.language == "EN") s.promptEn else s.promptPtBr

        currentOpenRouterModel = s.model
        currentOllamaModel = s.ollamaModel

        val activeModel = if (provider == ProviderType.OPENROUTER) s.model else s.ollamaModel
        availableModels.clear()
        modelField.removeAllItems()
        if (activeModel.isNotBlank()) {
            availableModels += activeModel
            modelField.addItem(activeModel)
            modelField.selectedItem = activeModel
        }

        // Auto-load apenas se OpenRouter já tem API key salva (preserva UX antiga)
        if (provider == ProviderType.OPENROUTER && apiKeyField.password.isNotEmpty()) {
            loadModelsAsync()
        }
    }

    override fun disposeUIResources() {
        dialogPanel = null
    }

    private fun currentModelText(): String =
        (modelField.editor.item as? String)?.trim()
            ?: (modelField.selectedItem as? String)?.trim()
            ?: ""

    private fun loadModelsAsync() {
        val provider = providerProperty.get()
        val (validation, fetcher) = buildLoader(provider) ?: return

        if (validation != null) {
            JOptionPane.warn(dialogPanel, validation)
            return
        }

        loadModelsButton.isEnabled = false
        loadModelsButton.text = "Loading..."

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching { fetcher() }
            SwingUtilities.invokeLater {
                loadModelsButton.isEnabled = true
                loadModelsButton.text = "Load Models"

                result.onSuccess { models ->
                    if (models.isEmpty()) {
                        JOptionPane.warn(dialogPanel, "Nenhum modelo retornado pelo provider.")
                        return@onSuccess
                    }
                    val current = currentModelText()
                    availableModels.clear()
                    availableModels.addAll(models)
                    modelField.removeAllItems()
                    availableModels.forEach { modelField.addItem(it) }
                    if (current.isNotEmpty()) {
                        modelField.selectedItem = current
                        if (modelField.selectedItem != current) {
                            modelField.editor.item = current
                        }
                    }
                }.onFailure { ex ->
                    JOptionPane.warn(dialogPanel, "Erro ao carregar modelos: ${ex.message ?: ex.javaClass.simpleName}")
                }
            }
        }
    }

    private fun buildLoader(provider: ProviderType): Pair<String?, () -> List<String>>? {
        return when (provider) {
            ProviderType.OPENROUTER -> {
                val key = String(apiKeyField.password).trim().ifEmpty { GitBotSecrets.getOpenRouterApiKey().orEmpty() }
                if (key.isEmpty()) return Pair("Informe a OpenRouter API Key antes de carregar os modelos.", { emptyList() })
                Pair(null, { LLMProviderFactory.createOpenRouter(key).models().map { it.id }.sorted() })
            }
            ProviderType.OLLAMA -> {
                val url = ollamaUrlField.text.trim()
                if (url.isEmpty()) return Pair("Informe a URL do servidor Ollama antes de carregar os modelos.", { emptyList() })
                Pair(null, { LLMProviderFactory.createOllama(url).models().map { it.id }.sorted() })
            }
        }
    }

    private fun testOllamaConnection() {
        val url = ollamaUrlField.text.trim()
        if (url.isEmpty()) {
            JOptionPane.warn(dialogPanel, "Informe a URL do servidor Ollama.")
            return
        }
        testConnectionButton.isEnabled = false
        testConnectionButton.text = "Testing..."

        ApplicationManager.getApplication().executeOnPooledThread {
            val health = runCatching { LLMProviderFactory.createOllama(url).ping() }
                .getOrElse { ex -> ProviderHealth.Unknown(ex.message ?: ex.javaClass.simpleName) }

            SwingUtilities.invokeLater {
                testConnectionButton.isEnabled = true
                testConnectionButton.text = "Test Connection"
                when (health) {
                    is ProviderHealth.Healthy ->
                        JOptionPane.info(dialogPanel, "Ollama is running. ${health.modelCount} model(s) found.")
                    is ProviderHealth.Unreachable ->
                        JOptionPane.error(dialogPanel,
                            "Cannot reach Ollama at $url.\n\n" +
                            "• Install Ollama: https://ollama.com\n" +
                            "• Start the service ('ollama serve' or launch the app)\n" +
                            "• Verify the URL\n\nDetails: ${health.reason}")
                    is ProviderHealth.Unauthorized ->
                        JOptionPane.error(dialogPanel, "Ollama returned 401: ${health.reason}")
                    is ProviderHealth.Unknown ->
                        JOptionPane.error(dialogPanel, "Ollama check failed: ${health.reason}")
                }
            }
        }
    }

    private fun configureModelField() {
        modelField.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                if (availableModels.isNotEmpty()) {
                    e.consume()
                    showSearchablePopup()
                }
            }
        })
    }

    private fun showSearchablePopup() {
        val searchField = JTextField(20)
        val listModel = DefaultListModel<String>()
        availableModels.forEach { listModel.addElement(it) }

        val currentValue = currentModelText()
        val list = JBList(listModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            setSelectedValue(currentValue, true)
        }

        val comboWidth = modelField.width.coerceAtLeast(400)
        val popupHeight = 400

        val scrollPane = JBScrollPane(list).apply {
            preferredSize = Dimension(comboWidth, popupHeight)
        }

        val container = JPanel(java.awt.BorderLayout()).apply {
            add(searchField, java.awt.BorderLayout.NORTH)
            add(scrollPane, java.awt.BorderLayout.CENTER)
            preferredSize = Dimension(comboWidth, popupHeight + 30)
        }

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

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(container, searchField)
            .setTitle("Select Model")
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .createPopup()

        list.addListSelectionListener {
            if (!it.valueIsAdjusting && list.selectedValue != null) {
                modelField.selectedItem = list.selectedValue
                popup.closeOk(null)
            }
        }

        popup.showUnderneathOf(modelField)
    }

    private fun ensureDefaultsLoaded(s: GitBotSettingsState) {
        if (s.promptPtBr.isBlank()) s.promptPtBr = loadDefaultPrompt("PT_BR")
        if (s.promptEn.isBlank()) s.promptEn = loadDefaultPrompt("EN")
    }

    private fun loadDefaultPrompt(lang: String): String =
        if (lang == "EN") PromptLoader.load("prompts/commit_prompt_en.txt")
        else PromptLoader.load("prompts/commit_prompt_ptbr.txt")
}

private object JOptionPane {
    fun warn(parent: JComponent?, msg: String) =
        javax.swing.JOptionPane.showMessageDialog(parent, msg, "GitBot Commit", javax.swing.JOptionPane.WARNING_MESSAGE)
    fun info(parent: JComponent?, msg: String) =
        javax.swing.JOptionPane.showMessageDialog(parent, msg, "GitBot Commit", javax.swing.JOptionPane.INFORMATION_MESSAGE)
    fun error(parent: JComponent?, msg: String) =
        javax.swing.JOptionPane.showMessageDialog(parent, msg, "GitBot Commit", javax.swing.JOptionPane.ERROR_MESSAGE)
}
