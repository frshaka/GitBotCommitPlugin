package com.frshaka.gitbot.ai.dto

import com.squareup.moshi.Json

data class CompletionChoice(

    @Json(name = "finish_reason")
    val finishReason: String,
    val index: Int,
    val message: CompletionMessage
)
