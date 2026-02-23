package com.frshaka.gitbot.settings

data class GitBotSettingsState(
    var model: String = "anthropic/claude-3.5-sonnet",
    var language: String = "PT_BR",
    var promptPtBr: String = "",
    var promptEn: String = ""
)