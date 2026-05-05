package com.frshaka.gitbot.ai

interface LLMProvider {
    fun completion(model: String, systemPrompt: String, userPrompt: String): String
    fun models(): List<ProviderModel>
    fun ping(): ProviderHealth
    fun cancel()
    fun isCanceled(): Boolean
}
