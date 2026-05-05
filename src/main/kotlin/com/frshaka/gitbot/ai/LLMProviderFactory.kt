package com.frshaka.gitbot.ai

import com.frshaka.gitbot.ai.ollama.OllamaProvider
import com.frshaka.gitbot.ai.openrouter.OpenRouterProvider
import com.frshaka.gitbot.settings.GitBotSecrets
import com.frshaka.gitbot.settings.GitBotSettingsState

object LLMProviderFactory {

    fun create(state: GitBotSettingsState): LLMProvider {
        return when (ProviderType.fromName(state.provider)) {
            ProviderType.OPENROUTER -> {
                val apiKey = GitBotSecrets.getOpenRouterApiKey()?.trim().orEmpty()
                require(apiKey.isNotEmpty()) { "OpenRouter API key not configured" }
                OpenRouterProvider(apiKey)
            }
            ProviderType.OLLAMA -> {
                val url = state.ollamaBaseUrl.trim()
                require(url.isNotEmpty()) { "Ollama base URL not configured" }
                OllamaProvider(url)
            }
        }
    }

    fun createOpenRouter(apiKey: String): LLMProvider = OpenRouterProvider(apiKey)

    fun createOllama(baseUrl: String): LLMProvider = OllamaProvider(baseUrl)
}
