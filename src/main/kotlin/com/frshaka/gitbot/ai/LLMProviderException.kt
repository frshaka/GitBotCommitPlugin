package com.frshaka.gitbot.ai

sealed class LLMProviderException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    class ProviderUnreachable(host: String, cause: Throwable? = null) :
        LLMProviderException("Cannot reach $host", cause)

    class ProviderUnauthorized(message: String) :
        LLMProviderException(message)

    class ModelNotFound(val modelId: String) :
        LLMProviderException("Model '$modelId' not found")

    class RateLimited(message: String) :
        LLMProviderException(message)

    class GenericProviderError(val status: Int, message: String) :
        LLMProviderException(message)
}
