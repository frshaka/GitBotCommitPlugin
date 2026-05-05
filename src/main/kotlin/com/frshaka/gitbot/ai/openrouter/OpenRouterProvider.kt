package com.frshaka.gitbot.ai.openrouter

import com.frshaka.gitbot.ai.LLMProvider
import com.frshaka.gitbot.ai.LLMProviderException
import com.frshaka.gitbot.ai.ProviderHealth
import com.frshaka.gitbot.ai.ProviderModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class OpenRouterProvider(apiKey: String) : LLMProvider {

    private val client = OpenRouterClient(apiKey)
    private val host = "openrouter.ai"

    override fun completion(model: String, systemPrompt: String, userPrompt: String): String {
        return runMapped {
            client.completion(model, systemPrompt, userPrompt)
        }
    }

    override fun models(): List<ProviderModel> {
        return runMapped {
            client.models().map { ProviderModel(id = it.id, displayName = it.name, description = it.description) }
        }
    }

    override fun ping(): ProviderHealth {
        return try {
            val count = client.models().size
            ProviderHealth.Healthy(count)
        } catch (ex: ConnectException) {
            ProviderHealth.Unreachable(ex.message ?: "connection refused")
        } catch (ex: UnknownHostException) {
            ProviderHealth.Unreachable(ex.message ?: "unknown host")
        } catch (ex: SocketTimeoutException) {
            ProviderHealth.Unreachable(ex.message ?: "timeout")
        } catch (ex: IOException) {
            ProviderHealth.Unreachable(ex.message ?: "I/O error")
        } catch (ex: RuntimeException) {
            val msg = ex.message ?: "unknown error"
            if (msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true)) {
                ProviderHealth.Unauthorized(msg)
            } else {
                ProviderHealth.Unknown(msg)
            }
        }
    }

    override fun cancel() = client.cancel().let { }

    override fun isCanceled() = client.isCanceled()

    private inline fun <T> runMapped(block: () -> T): T {
        try {
            return block()
        } catch (ex: ConnectException) {
            throw LLMProviderException.ProviderUnreachable(host, ex)
        } catch (ex: UnknownHostException) {
            throw LLMProviderException.ProviderUnreachable(host, ex)
        } catch (ex: SocketTimeoutException) {
            throw LLMProviderException.ProviderUnreachable(host, ex)
        } catch (ex: IOException) {
            throw LLMProviderException.ProviderUnreachable(host, ex)
        } catch (ex: RuntimeException) {
            throw mapRuntime(ex)
        }
    }

    private fun mapRuntime(ex: RuntimeException): LLMProviderException {
        val msg = ex.message ?: ""
        return when {
            msg.contains("Status: 401") -> LLMProviderException.ProviderUnauthorized(msg)
            msg.contains("Status: 404") -> LLMProviderException.GenericProviderError(404, msg)
            msg.contains("Status: 429") -> LLMProviderException.RateLimited(msg)
            else -> LLMProviderException.GenericProviderError(0, msg.ifBlank { "OpenRouter error" })
        }
    }
}
