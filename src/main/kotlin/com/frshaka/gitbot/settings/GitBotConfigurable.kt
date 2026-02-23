package com.frshaka.gitbot.settings

import com.frshaka.gitbot.prompt.PromptLoader
import com.intellij.openapi.options.Configurable
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class GitBotConfigurable : Configurable {

    private var panel: JPanel? = null

    private val apiKeyField = JPasswordField()
    private val modelField = JTextField()
    private val languageCombo = JComboBox(arrayOf("PT_BR", "EN"))

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
            form.add(JLabel(label), c)

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
        form.add(JLabel("Prompt Template:"), c)

        c.gridx = 1
        c.weightx = 1.0
        c.fill = GridBagConstraints.BOTH
        val promptScroll = JScrollPane(promptArea).apply {
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
        val uiModel = modelField.text.trim()
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
        val uiModel = modelField.text.trim()
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
        modelField.text = settings.model
        languageCombo.selectedItem = settings.language

        promptArea.text = if (settings.language == "EN") settings.promptEn else settings.promptPtBr
    }

    override fun disposeUIResources() {
        panel = null
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
}