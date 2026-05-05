package com.frshaka.gitbot.ai.ollama

import com.frshaka.gitbot.ai.LLMProvider
import com.frshaka.gitbot.ai.LLMProviderException
import com.frshaka.gitbot.ai.ProviderHealth
import com.frshaka.gitbot.ai.ProviderModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class OllamaProvider(private val baseUrl: String) : LLMProvider {

    private val client = OllamaClient(baseUrl)

    override fun completion(model: String, systemPrompt: String, userPrompt: String): String {
        return runMapped(model) {
            client.completion(model, systemPrompt, userPrompt)
        }
    }

    override fun models(): List<ProviderModel> {
        return runMapped(modelId = null) {
            client.tags().map { ProviderModel(id = it.name, displayName = it.name) }
        }
    }

    override fun ping(): ProviderHealth {
        return try {
            val count = client.tags().size
            ProviderHealth.Healthy(count)
        } catch (ex: ConnectException) {
            ProviderHealth.Unreachable(unreachableMessage(ex))
        } catch (ex: UnknownHostException) {
            ProviderHealth.Unreachable(unreachableMessage(ex))
        } catch (ex: SocketTimeoutException) {
            ProviderHealth.Unreachable(unreachableMessage(ex))
        } catch (ex: IOException) {
            ProviderHealth.Unreachable(unreachableMessage(ex))
        } catch (ex: RuntimeException) {
            val msg = ex.message ?: "unknown error"
            if (msg.contains("404")) {
                // /api/tags 404 indica baseUrl/path incorretos, não versão.
                // O 404 de versão antiga (0.1.27) só aparece em /v1/chat/completions.
                ProviderHealth.Unknown("Endpoint /api/tags not found at $baseUrl. Verify the server URL is correct.")
            } else {
                ProviderHealth.Unknown(msg)
            }
        }
    }

    override fun cancel() = client.cancel()

    override fun isCanceled() = client.isCanceled()

    private inline fun <T> runMapped(modelId: String?, block: () -> T): T {
        try {
            return block()
        } catch (ex: ConnectException) {
            throw LLMProviderException.ProviderUnreachable(baseUrl, ex)
        } catch (ex: UnknownHostException) {
            throw LLMProviderException.ProviderUnreachable(baseUrl, ex)
        } catch (ex: SocketTimeoutException) {
            throw LLMProviderException.ProviderUnreachable(baseUrl, ex)
        } catch (ex: IOException) {
            throw LLMProviderException.ProviderUnreachable(baseUrl, ex)
        } catch (ex: RuntimeException) {
            throw mapRuntime(ex, modelId)
        }
    }

    private fun mapRuntime(ex: RuntimeException, modelId: String?): LLMProviderException {
        val msg = ex.message ?: ""
        return when {
            // 404 numa requisição de completion com model conhecido geralmente é "model not found"
            msg.contains("Status: 404") && modelId != null && msg.contains("not found", ignoreCase = true) ->
                LLMProviderException.ModelNotFound(modelId)
            // 404 em /v1/chat/completions (modelId presente mas sem "not found"): provável versão antiga
            msg.contains("Status: 404") && modelId != null ->
                LLMProviderException.GenericProviderError(404, "$msg\n\nTip: update Ollama to 0.1.27+ for /v1/chat/completions support.")
            // 404 sem modelId = chamada de listagem (/api/tags) → URL incorreta
            msg.contains("Status: 404") ->
                LLMProviderException.GenericProviderError(404, "$msg\n\nTip: verify the Ollama server URL is correct.")
            msg.contains("Status: 401") ->
                LLMProviderException.ProviderUnauthorized(msg)
            msg.contains("Status: 429") ->
                LLMProviderException.RateLimited(msg)
            else ->
                LLMProviderException.GenericProviderError(0, msg.ifBlank { "Ollama error" })
        }
    }

    private fun unreachableMessage(ex: Throwable): String =
        "Cannot reach $baseUrl: ${ex.message ?: ex.javaClass.simpleName}"
}
