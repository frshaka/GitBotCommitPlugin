package com.frshaka.gitbot.ai.dto

import com.squareup.moshi.Json

data class ErrorResponse(

    val error: ErrorObject
)

data class ErrorObject(

    val code: Int?,
    val message: String?,
    @Json(name = "user_id")
    val userId: String?
)
