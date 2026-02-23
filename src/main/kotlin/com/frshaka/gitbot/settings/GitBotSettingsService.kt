package com.frshaka.gitbot.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "GitBotCommitSettings",
    storages = [Storage("gitbot-commit.xml")]
)
class GitBotSettingsService : PersistentStateComponent<GitBotSettingsState> {

    private var state = GitBotSettingsState()

    override fun getState(): GitBotSettingsState = state

    override fun loadState(state: GitBotSettingsState) {
        this.state = state
    }

    companion object {
        fun getInstance(): GitBotSettingsService =
            ApplicationManager.getApplication().getService(GitBotSettingsService::class.java)
    }
}