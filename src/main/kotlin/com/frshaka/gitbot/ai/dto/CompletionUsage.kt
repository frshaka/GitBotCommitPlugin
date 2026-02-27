package com.frshaka.gitbot.ai.dto

import com.squareup.moshi.Json

data class CompletionUsage(

    @Json(name = "completion_tokens")
    val completionTokens: Int?,
    @Json(name = "prompt_tokens")
    val promptTokens: Int?,
    @Json(name = "total_tokens")
    val totalTokens: Int?
)
