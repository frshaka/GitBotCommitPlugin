package com.frshaka.gitbot.ai

import com.frshaka.gitbot.ai.dto.CompletionMessage
import com.frshaka.gitbot.ai.dto.CompletionRequest
import com.frshaka.gitbot.ai.dto.CompletionResponse
import com.frshaka.gitbot.ai.dto.ErrorResponse
import com.frshaka.gitbot.ai.dto.Model
import com.frshaka.gitbot.ai.dto.ModelResponse
import com.jetbrains.fus.reporting.serialization.toJsonElement
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class OpenRouterClient(
    private val apiKey: String,
) {
    private val baseUrl: String = "https://openrouter.ai/"
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val api by lazy { retrofitClient() }

    @Volatile
    private var currentCall: Call<*>? = null

    companion object {
        // Limite de iterações do loop COT para evitar ciclo infinito em modelos de raciocínio
        private const val MAX_COT_ITERACOES = 8
    }

    fun completion(model: String, systemPrompt: String, userPrompt: String): String {
        // Histórico de mensagens que cresce a cada passo de raciocínio (COT)
        val historico = mutableListOf(
            CompletionMessage(role = "system", content = systemPrompt),
            CompletionMessage(role = "user", content = userPrompt)
        )

        try {
            repeat(MAX_COT_ITERACOES) { iteracao ->
                val call = api.completion(CompletionRequest(model = model, messages = historico))
                currentCall = call

                val response = call.execute()
                if (!response.isSuccessful) {
                    val errorBody = errorParser<ErrorResponse>(response.errorBody()?.string() ?: "")
                    throw RuntimeException(
                        """Erro durante a geração do completion.
                        Status: ${response.raw().code}
                        Motivo: ${errorBody?.error?.message}
                        """.trimIndent()
                    )
                }

                val mensagem = response.body()!!.choices[0].message

                // Modelo respondeu com conteúdo: retorna imediatamente
                if (mensagem.content.isNotBlank()) {
                    return mensagem.content
                }

                // Content vazio: passo de raciocínio (COT) de modelo de reasoning.
                // Inclui o reasoning no histórico como mensagem do assistente e itera novamente.
                val reasoning = mensagem.reasoning
                if (reasoning.isNullOrBlank()) {
                    throw RuntimeException(
                        "Modelo retornou resposta vazia sem raciocínio na iteração ${iteracao + 1}."
                    )
                }

                historico.add(CompletionMessage(role = "assistant", content = reasoning))
            }

            throw RuntimeException(
                "Modelo não retornou conteúdo após $MAX_COT_ITERACOES iterações de raciocínio (COT)."
            )
        } finally {
            currentCall = null
        }
    }

    fun models(): List<Model> {
        val call = api.models()
        currentCall = call

        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                val errorBody = errorParser<ErrorResponse>(response.errorBody()?.string() ?: "")
                throw RuntimeException("""Erro durante a busca de modelos.
                Status: ${response.raw().code}
                Motivo: ${errorBody?.error?.message}
                """.trimIndent())
            }

            val modelsResponse = response.body()!!
            return modelsResponse.models
        } finally {
            currentCall = null
        }
    }

    fun cancel() = currentCall?.cancel()

    fun isCanceled() = currentCall?.isCanceled ?: false

    private fun okHttpClient() = OkHttpClient().newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val request: Request = chain.request()
                .newBuilder()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .build()
            chain.proceed(request)
        })

    private fun retrofitClient() = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient().build())
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
        .build()
        .create(OpenRouterAPI::class.java)


    inline private fun <reified T : Any> errorParser(json: String): T? {
        val adapter = moshi.adapter(T::class.java)
        return adapter.fromJson(json)
    }
}