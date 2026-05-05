package com.frshaka.gitbot.ai

data class ProviderModel(
    val id: String,
    val displayName: String = id,
    val description: String? = null
)
