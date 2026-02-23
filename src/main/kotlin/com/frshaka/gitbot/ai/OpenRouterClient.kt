package com.frshaka.gitbot.ai

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class OpenRouterClient(
    private val apiKey: String,
    private val model: String
) {
    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper()

    fun newCall(systemPrompt: String, userPrompt: String): Call {
        val url = "https://openrouter.ai/api/v1/chat/completions"

        val bodyMap = mapOf(
            "model" to model,
            "temperature" to 0.0,
            "max_tokens" to 300,
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userPrompt)
            )
        )

        val json = mapper.writeValueAsString(bodyMap)
        val requestBody = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        return client.newCall(request)
    }

    fun execute(call: Call): String {
        call.execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected response ${response.code}")
            }

            val bytes = response.body?.bytes() ?: throw IOException("Empty response body")
            val root = mapper.readTree(bytes)
            return root["choices"][0]["message"]["content"].asText()
        }
    }

    fun generateCommit(systemPrompt: String, userPrompt: String): String {
        val call = newCall(systemPrompt, userPrompt)
        return execute(call)
    }
}