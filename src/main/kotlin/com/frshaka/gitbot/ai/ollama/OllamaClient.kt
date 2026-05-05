package com.frshaka.gitbot.ai.ollama

import com.frshaka.gitbot.ai.dto.CompletionMessageRequisicao
import com.frshaka.gitbot.ai.dto.CompletionRequest
import com.frshaka.gitbot.ai.ollama.dto.OllamaTag
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class OllamaClient(
    rawBaseUrl: String,
) {
    private val baseUrl: String = normalizeBaseUrl(rawBaseUrl)

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val api by lazy { retrofitClient() }

    @Volatile
    private var currentCall: Call<*>? = null

    companion object {
        private const val MAX_COT_ITERACOES = 8

        private fun normalizeBaseUrl(url: String): String {
            val trimmed = url.trim()
            require(trimmed.isNotEmpty()) { "Ollama base URL cannot be empty" }
            require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                "Ollama base URL must start with http:// or https://"
            }
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }
    }

    fun completion(model: String, systemPrompt: String, userPrompt: String): String {
        val historico = mutableListOf(
            CompletionMessageRequisicao(role = "system", content = systemPrompt),
            CompletionMessageRequisicao(role = "user", content = userPrompt)
        )

        try {
            repeat(MAX_COT_ITERACOES) { iteracao ->
                val call = api.completion(CompletionRequest(model = model, messages = historico))
                currentCall = call

                val response = call.execute()
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    throw RuntimeException(
                        """Erro durante a geração do completion (Ollama).
                        Status: ${response.raw().code}
                        Motivo: $errorBody
                        """.trimIndent()
                    )
                }

                val body = response.body()
                    ?: throw RuntimeException("Ollama retornou resposta vazia.")

                val mensagem = body.choices.firstOrNull()?.message
                    ?: throw RuntimeException("Ollama retornou sem choices.")

                if (mensagem.content.isNotBlank()) {
                    return mensagem.content
                }

                val reasoning = mensagem.reasoning
                if (reasoning.isNullOrBlank()) {
                    throw RuntimeException(
                        "Modelo retornou resposta vazia sem raciocínio na iteração ${iteracao + 1}."
                    )
                }

                historico.add(CompletionMessageRequisicao(role = "assistant", content = reasoning))
                historico.add(CompletionMessageRequisicao(role = "user", content = "Continue com sua resposta final."))
            }

            throw RuntimeException(
                "Modelo não retornou conteúdo após $MAX_COT_ITERACOES iterações de raciocínio (COT)."
            )
        } finally {
            currentCall = null
        }
    }

    fun tags(): List<OllamaTag> {
        val call = api.tags()
        currentCall = call

        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                throw RuntimeException(
                    "Erro ao listar modelos do Ollama. Status: ${response.raw().code}"
                )
            }
            return response.body()?.models.orEmpty()
        } finally {
            currentCall = null
        }
    }

    fun cancel() {
        currentCall?.cancel()
    }

    fun isCanceled(): Boolean = currentCall?.isCanceled ?: false

    private fun okHttpClient() = OkHttpClient().newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun retrofitClient() = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient())
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
        .build()
        .create(OllamaAPI::class.java)
}
