plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.frshaka"
version = "1.0.5"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.2.4")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        bundledPlugin("Git4Idea")

        implementation("com.squareup.okhttp3:okhttp:4.12.0")
        implementation("com.squareup.retrofit2:retrofit:2.12.0")
        implementation("com.squareup.moshi:moshi:1.15.2")
        implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
        implementation("com.squareup.retrofit2:converter-moshi:2.12.0")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }
        changeNotes = """
            <h2>Bug Fixes</h2>
            <ul>
                <li>
                    <b>Fixed 400 error when generating commits with any OpenRouter model.</b><br/>
                    The <code>reasoning</code> field from the response DTO was being serialized as
                    <code>"reasoning": null</code> in every outgoing request message. Providers such as
                    Anthropic reject unknown fields with a 400 status. Request and response message DTOs
                    are now separate, so no extra fields are sent to the API.
                </li>
                <li>
                    <b>Fixed infinite 400 loop with Chain-of-Thought reasoning models.</b><br/>
                    When a reasoning model returned only a reasoning step without a final answer, the
                    plugin added it as an assistant message and retried. The resulting conversation ended
                    with an assistant turn, which is invalid for OpenAI-compatible APIs and caused a 400
                    on the next iteration. A user continuation message is now appended after each
                    reasoning step, keeping the conversation structure valid.
                </li>
            </ul>
        """.trimIndent()
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    publishing {
        // Token do Marketplace (criado no perfil do JetBrains Marketplace)
        // Melhor pratica: injetar por variavel de ambiente/Gradle property, nao hardcode.
        token.set(
            providers.environmentVariable("JB_MARKETPLACE_TOKEN")
                .orElse(providers.gradleProperty("jbMarketplaceToken"))
        )

        // Para release normal no canal default:
        channels.set(listOf("default"))

        // Se quiser soltar beta/alpha primeiro, troca para:
        // channels.set(listOf("alpha"))

        // Opcional: publicar como hidden (nao aparece publicamente apos aprovacao).
        // hidden.set(true)
    }

    // Assinatura e opcional aqui.
    // Se voce for assinar depois, a pr�pria doc do plugin mostra as opcoes suportadas.
    // signing { ... }
}
tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.encoding = "UTF-8"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}