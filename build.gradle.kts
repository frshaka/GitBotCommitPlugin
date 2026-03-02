plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.frshaka"
version = "1.0.3"

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
                    <b>Settings screen no longer freezes on first install.</b><br/>
                    The model list was loaded synchronously on the UI thread during class initialization,
                    causing an infinite loading state whenever the Settings page was opened without a
                    previously configured API Key. The network call is now performed asynchronously in a
                    background thread and never blocks the UI.
                </li>
                <li>
                    <b>Model field is now editable.</b><br/>
                    The model combo box was read-only, making it impossible to enter a model ID manually
                    when the list had not yet been loaded (e.g. on first install). The field now accepts
                    free text input at any time.
                </li>
            </ul>
            <h2>Improvements</h2>
            <ul>
                <li>
                    <b>New "Load Models" button in Settings.</b><br/>
                    A dedicated button next to the model field lets you fetch the full list of available
                    OpenRouter models on demand, after entering your API Key. If a key is already saved,
                    models are loaded automatically in the background when the Settings page is opened.
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