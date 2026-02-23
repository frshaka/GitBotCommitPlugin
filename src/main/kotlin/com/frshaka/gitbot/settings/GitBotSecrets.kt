package com.frshaka.gitbot.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe

object GitBotSecrets {

    private const val SERVICE_NAME = "com.frshaka.gitbot.openrouter"

    private fun attributes() = CredentialAttributes(SERVICE_NAME)

    fun getApiKey(): String? {
        return PasswordSafe.instance.get(attributes())?.getPasswordAsString()
    }

    fun setApiKey(apiKey: String) {
        PasswordSafe.instance.set(attributes(), Credentials("openrouter", apiKey))
    }
}