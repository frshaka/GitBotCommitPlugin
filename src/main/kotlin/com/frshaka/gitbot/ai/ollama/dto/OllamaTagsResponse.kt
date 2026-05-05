package com.frshaka.gitbot.ai.ollama.dto

data class OllamaTagsResponse(
    val models: List<OllamaTag> = emptyList()
)

data class OllamaTag(
    val name: String,
    val model: String? = null,
    val size: Long? = null,
    val digest: String? = null,
    val details: OllamaTagDetails? = null
)

data class OllamaTagDetails(
    val format: String? = null,
    val family: String? = null,
    val families: List<String>? = null,
    val parameter_size: String? = null,
    val quantization_level: String? = null
)
