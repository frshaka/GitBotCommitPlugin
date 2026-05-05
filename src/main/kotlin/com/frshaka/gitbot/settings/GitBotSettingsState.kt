package com.frshaka.gitbot.settings

import com.frshaka.gitbot.ai.ProviderType

data class GitBotSettingsState(
    var provider: String = ProviderType.OPENROUTER.name,
    var model: String = "anthropic/claude-3.5-sonnet",
    var ollamaModel: String = "",
    var ollamaBaseUrl: String = "http://localhost:11434",
    var language: String = "PT_BR",
    var promptPtBr: String = "",
    var promptEn: String = ""
)
