package com.frshaka.gitbot.ai.dto

import com.squareup.moshi.Json

data class CompletionRequest(

    val messages: List<CompletionMessage> = listOf(),
    val model: String,
    @Json(name = "max_tokens")
    val maxTokens: Int = 300,
    val temperature: Double = 0.0
)