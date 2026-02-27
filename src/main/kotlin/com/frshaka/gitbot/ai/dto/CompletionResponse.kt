package com.frshaka.gitbot.ai.dto

import com.squareup.moshi.Json

data class CompletionResponse(

    val id: String,
    val choices: List<CompletionChoice> = listOf(),
    val created: Long,
    val model: String,
    @Json(name = "object")
    val obj: String,
    val usage: CompletionUsage
)
