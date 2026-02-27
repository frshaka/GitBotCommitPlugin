package com.frshaka.gitbot.ai

import com.frshaka.gitbot.ai.dto.CompletionRequest
import com.frshaka.gitbot.ai.dto.CompletionResponse
import com.frshaka.gitbot.ai.dto.ModelResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OpenRouterAPI {

    @GET("api/v1/models")
    fun models(): Call<ModelResponse>

    @POST("api/v1/chat/completions")
    fun completion(@Body request: CompletionRequest): Call<CompletionResponse>
}