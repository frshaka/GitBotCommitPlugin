package com.frshaka.gitbot.ai

sealed interface ProviderHealth {
    data class Healthy(val modelCount: Int) : ProviderHealth
    data class Unreachable(val reason: String) : ProviderHealth
    data class Unauthorized(val reason: String) : ProviderHealth
    data class Unknown(val reason: String) : ProviderHealth
}
