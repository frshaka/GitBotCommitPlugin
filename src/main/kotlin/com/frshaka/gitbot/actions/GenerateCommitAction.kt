package com.frshaka.gitbot.actions

import com.frshaka.gitbot.ai.OpenRouterClient
import com.frshaka.gitbot.settings.GitBotSecrets
import com.frshaka.gitbot.settings.GitBotSettingsService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.ui.Refreshable
import com.intellij.openapi.wm.WindowManager
import git4idea.repo.GitRepositoryManager
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

class GenerateCommitAction : AnAction() {

    private val isRunning = AtomicBoolean(false)

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !isRunning.get()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (!isRunning.compareAndSet(false, true)) {
            WindowManager.getInstance().getStatusBar(project)?.info = "GitBot Commit: generation already running."
            return
        }

        val repoManager = GitRepositoryManager.getInstance(project)
        val repository = repoManager.repositories.firstOrNull()
        val commitMessageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)

        if (repository == null) {
            isRunning.set(false)
            Messages.showErrorDialog(project, "No Git repository found.", "GitBot Commit")
            return
        }

        val checkinPanel = e.getData(Refreshable.PANEL_KEY) as? CheckinProjectPanel

        // Arquivos rastreados pelo git (já staged) que estão marcados no painel de commit
        val selectedChanges = checkinPanel?.selectedChanges?.takeIf { it.isNotEmpty() }
            ?: e.getData(VcsDataKeys.SELECTED_CHANGES)?.takeIf { it.isNotEmpty() }?.toList()

        val selectedPaths = selectedChanges
            ?.mapNotNull { it.afterRevision?.file?.path ?: it.beforeRevision?.file?.path }
            ?.takeIf { it.isNotEmpty() }

        // Arquivos não versionados: marcados no painel mas ainda ausentes do índice do git.
        // CheckinProjectPanel.getFiles() retorna todos os java.io.File marcados para o commit
        // (tanto mudanças rastreadas quanto arquivos não versionados), então subtraímos o que já
        // está coberto por selectedPaths para isolar apenas os não versionados.
        // Caminhos canônicos são usados na comparação para tratar diferenças de separador entre
        // o VFS do IntelliJ (barras normais) e java.io.File no Windows (barras invertidas).
        val trackedCanonical = selectedPaths
            ?.map { java.io.File(it).canonicalPath }
            ?.toSet() ?: emptySet()
        val unversionedFiles: List<java.io.File> = checkinPanel?.files
            ?.filter { it.canonicalPath !in trackedCanonical }
            ?: emptyList()

        val fullDiff = buildCombinedDiff(repository.root.path, selectedPaths, unversionedFiles)

        if (fullDiff.isBlank()) {
            isRunning.set(false)
            Messages.showWarningDialog(
                project,
                "No staged changes found. Please stage your changes first.",
                "GitBot Commit"
            )
            return
        }

        val apiKey = GitBotSecrets.getApiKey()?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            isRunning.set(false)
            Messages.showErrorDialog(
                project,
                "Configure your OpenRouter API key in Settings ? GitBot Commit.",
                "GitBot Commit"
            )
            return
        }

        val settings = GitBotSettingsService.getInstance().state
        val model = settings.model.trim()
        if (model.isEmpty()) {
            isRunning.set(false)
            Messages.showErrorDialog(
                project,
                "Configure the model in Settings ? GitBot Commit.",
                "GitBot Commit"
            )
            return
        }

        val systemPrompt = when (settings.language) {
            "EN" -> settings.promptEn
            else -> settings.promptPtBr
        }

        val userPrompt = "Here's the git diff:\n\n$fullDiff"

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "GitBot Commit, generating message",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                val client = OpenRouterClient(apiKey)

                try {
                    indicator.text = "GitBot Commit"
                    indicator.text2 = "Calling OpenRouter..."
                    indicator.isIndeterminate = true

                    if (indicator.isCanceled) {
                        client.cancel()
                        return
                    }

                    val commitText = try {
                        client.completion(model, systemPrompt, userPrompt)
                    } catch (ex: Exception) {
                        if (client.isCanceled()) return

                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                project,
                                "OpenRouter error: ${ex.message}",
                                "GitBot Commit"
                            )
                        }
                        return
                    }

                    if (indicator.isCanceled) {
                        client.cancel()
                        return
                    }

                    indicator.text2 = "Rendering preview..."

                    ApplicationManager.getApplication().invokeLater {
                        showCommitPreviewDialog(project, commitText, commitMessageControl)
                    }
                } finally {
                    if (indicator.isCanceled) {
                        client.cancel()
                    }
                    isRunning.set(false)
                }
            }
        })
    }

    /**
     * Constrói um diff combinado cobrindo:
     *  - arquivos rastreados (já staged), filtrados por [trackedPaths] quando fornecido;
     *  - arquivos não versionados (ainda não no índice do git), lidos diretamente do disco.
     *
     * Retorna o diff completo de todos os arquivos staged como fallback quando não há seleção explícita.
     */
    private fun buildCombinedDiff(
        repoPath: String,
        trackedPaths: List<String>?,
        unversionedFiles: List<java.io.File>
    ): String {
        val parts = mutableListOf<String>()

        // Diff staged — executado apenas quando há caminhos rastreados selecionados,
        // OU quando não há arquivos não versionados (fallback para o diff completo).
        if (trackedPaths != null || unversionedFiles.isEmpty()) {
            val staged = getStagedDiff(repoPath, trackedPaths)
            if (staged.isNotBlank()) parts.add(staged)
        }

        // Arquivos não versionados: tenta git diff --cached primeiro (o arquivo pode ter sido
        // staged por outro meio); caso contrário, constrói um diff sintético a partir do conteúdo.
        for (file in unversionedFiles) {
            val cached = runGitDiff(repoPath, listOf("--cached"), listOf(file.absolutePath))
            if (cached.isNotBlank()) {
                parts.add(cached)
            } else {
                val synthetic = getNewFileDiff(repoPath, file.absolutePath)
                if (synthetic.isNotBlank()) parts.add(synthetic)
            }
        }

        return parts.joinToString("\n")
    }

    /**
     * Gera um diff unificado sintético para um arquivo que ainda não está no índice do git,
     * formatado de forma idêntica ao que `git diff --cached` produziria após o staging.
     */
    private fun getNewFileDiff(repoPath: String, filePath: String): String {
        return try {
            val relPath = java.io.File(repoPath).toURI()
                .relativize(java.io.File(filePath).toURI()).path
            val lines = java.io.File(filePath).readLines(Charsets.UTF_8)
            buildString {
                appendLine("diff --git a/$relPath b/$relPath")
                appendLine("new file mode 100644")
                appendLine("--- /dev/null")
                appendLine("+++ b/$relPath")
                appendLine("@@ -0,0 +1,${lines.size} @@")
                lines.forEach { line -> appendLine("+$line") }
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun getStagedDiff(repoPath: String, filePaths: List<String>? = null): String {
        val cached = runGitDiff(repoPath, listOf("--cached"), filePaths)
        if (cached.isNotBlank()) return cached
        return runGitDiff(repoPath, listOf("HEAD"), filePaths)
    }

    private fun runGitDiff(repoPath: String, extraArgs: List<String>, filePaths: List<String>?): String {
        return try {
            val args = mutableListOf("git", "diff", "--unified=3")
            args.addAll(extraArgs)
            if (!filePaths.isNullOrEmpty()) {
                val repoUri = java.io.File(repoPath).toURI()
                val relativePaths = filePaths.mapNotNull { absPath ->
                    try {
                        repoUri.relativize(java.io.File(absPath).toURI()).path.ifBlank { null }
                    } catch (_: Exception) {
                        absPath
                    }
                }
                if (relativePaths.isNotEmpty()) {
                    args.add("--")
                    args.addAll(relativePaths)
                }
            }

            val process = ProcessBuilder(args)
                .directory(java.io.File(repoPath))
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
            process.waitFor()
            output
        } catch (ex: Exception) {
            ""
        }
    }

    private fun showCommitPreviewDialog(project: Project, initialText: String, commitMessageControl: Any?) {
        val dialog = object : DialogWrapper(project) {

            private val textArea = JTextArea(initialText).apply {
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
                caretPosition = 0
            }

            init {
                title = "GitBot Commit Preview"
                setOKButtonText("Apply")
                init()
            }

            override fun createCenterPanel(): JPanel {
                val scrollPane = JScrollPane(textArea).apply {
                    preferredSize = Dimension(900, 450)
                }

                val editButton = JButton("Edit").apply {
                    addActionListener {
                        textArea.isEditable = true
                        textArea.requestFocus()
                    }
                }

                val copyButton = JButton("Copy").apply {
                    addActionListener {
                        val sel = StringSelection(textArea.text)
                        java.awt.Toolkit.getDefaultToolkit()
                            .systemClipboard
                            .setContents(sel, null)
                    }
                }

                val topBar = JPanel().apply {
                    add(editButton)
                    add(copyButton)
                }

                return JPanel(BorderLayout()).apply {
                    add(topBar, BorderLayout.NORTH)
                    add(scrollPane, BorderLayout.CENTER)
                }
            }

            override fun doOKAction() {
                val text = textArea.text

                if (commitMessageControl != null) {
                    try {
                        val method = commitMessageControl.javaClass.getMethod("setCommitMessage", String::class.java)
                        method.invoke(commitMessageControl, text)
                    } catch (ignored: Exception) {
                        val sel = StringSelection(text)
                        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, null)
                    }
                } else {
                    val sel = StringSelection(text)
                    java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, null)
                }

                super.doOKAction()
            }
        }

        dialog.show()
    }
}
