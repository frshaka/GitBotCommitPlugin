package com.frshaka.gitbot.checkin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory

class GitBotCheckinHandlerFactory : CheckinHandlerFactory() {

    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return object : CheckinHandler() {
            override fun checkinSuccessful() {
                // O clear é agendado via invokeLater para garantir que seja executado
                // somente após o IntelliJ concluir sua própria atualização de UI pós-commit
                // (que pode restaurar a mensagem salva internamente), evitando que o campo
                // seja repreenchido logo após nossa limpeza.
                ApplicationManager.getApplication().invokeLater {
                    try {
                        panel.setCommitMessage("")
                    } catch (_: Exception) {
                        // Painel pode ter sido descartado antes do callback ser executado
                    }
                }
            }
        }
    }
}
