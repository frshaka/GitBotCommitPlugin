package com.frshaka.gitbot.ai.ollama

import com.frshaka.gitbot.ai.dto.CompletionRequest
import com.frshaka.gitbot.ai.dto.CompletionResponse
import com.frshaka.gitbot.ai.ollama.dto.OllamaTagsResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OllamaAPI {

    @GET("api/tags")
    fun tags(): Call<OllamaTagsResponse>

    @POST("v1/chat/completions")
    fun completion(@Body request: CompletionRequest): Call<CompletionResponse>
}
