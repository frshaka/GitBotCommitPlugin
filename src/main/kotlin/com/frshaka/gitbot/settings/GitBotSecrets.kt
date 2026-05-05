package com.frshaka.gitbot.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe

object GitBotSecrets {

    private const val SERVICE_PREFIX = "com.frshaka.gitbot"
    private const val OPENROUTER_KEY = "openrouter"

    /**
     * Constrói os atributos de credencial para um provider.
     *
     * IMPORTANTE: para OpenRouter, esta função produz exatamente
     * service="com.frshaka.gitbot.openrouter" + user="openrouter",
     * preservando as credenciais salvas em versões anteriores do plugin.
     */
    private fun attributesFor(providerKey: String) =
        CredentialAttributes("$SERVICE_PREFIX.$providerKey", providerKey)

    fun isMemoryOnly(): Boolean = PasswordSafe.instance.isMemoryOnly

    fun getOpenRouterApiKey(): String? =
        PasswordSafe.instance.get(attributesFor(OPENROUTER_KEY))?.getPasswordAsString()

    fun setOpenRouterApiKey(apiKey: String) {
        PasswordSafe.instance.set(
            attributesFor(OPENROUTER_KEY),
            Credentials(OPENROUTER_KEY, apiKey)
        )
    }

    /**
     * Compatibilidade com chamadas existentes que ainda usavam getApiKey/setApiKey.
     * Encaminha para a credencial do OpenRouter.
     */
    @Deprecated("Use getOpenRouterApiKey()", ReplaceWith("getOpenRouterApiKey()"))
    fun getApiKey(): String? = getOpenRouterApiKey()

    @Deprecated("Use setOpenRouterApiKey(apiKey)", ReplaceWith("setOpenRouterApiKey(apiKey)"))
    fun setApiKey(apiKey: String) = setOpenRouterApiKey(apiKey)
}
