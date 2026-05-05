package com.frshaka.gitbot.ai

enum class ProviderType(val displayName: String) {
    OPENROUTER("OpenRouter"),
    OLLAMA("Ollama");

    companion object {
        fun fromName(name: String?): ProviderType =
            entries.firstOrNull { it.name == name } ?: OPENROUTER
    }
}
