plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.frshaka"
version = "1.0.6"

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
            <h2>1.0.6 — Multi-provider support</h2>
            <ul>
                <li>
                    <b>New provider: Ollama (local).</b> Run commit generation 100% offline against models
                    pulled with <code>ollama pull</code>. Requires Ollama 0.1.27+ for OpenAI-compatible
                    <code>/v1/chat/completions</code> support.
                </li>
                <li>
                    <b>Provider selector in Settings.</b> Switch between <b>OpenRouter</b> (cloud) and
                    <b>Ollama</b> (local). The settings panel was migrated to the modern Kotlin UI DSL,
                    showing only the relevant fields per provider.
                </li>
                <li>
                    <b>Test Connection for Ollama.</b> Verifies the server is reachable and reports the
                    number of installed models in one click.
                </li>
                <li>
                    <b>User-friendly error handling.</b> Distinct messages for unreachable providers,
                    invalid API key, missing model (with <code>ollama pull</code> hint), and rate limits.
                </li>
                <li>
                    <b>Per-provider model memory.</b> Switching providers preserves the previously
                    selected model on each side, so you don't lose your selection.
                </li>
            </ul>
            <h2>1.0.5 — Bug fixes</h2>
            <ul>
                <li>
                    <b>Fixed 400 error when generating commits with any OpenRouter model.</b>
                    Request and response message DTOs are now separate, so no extra fields are sent.
                </li>
                <li>
                    <b>Fixed infinite 400 loop with Chain-of-Thought reasoning models.</b>
                    A user continuation message is now appended after each reasoning step.
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