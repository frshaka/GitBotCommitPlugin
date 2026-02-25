plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.frshaka"
version = "1.0.0"

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
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }
        changeNotes = """
            Initial version
        """.trimIndent()
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    publishing {
        // Token do Marketplace (criado no perfil do JetBrains Marketplace)
        // Melhor prática: injetar por variável de ambiente/Gradle property, não hardcode.
        token.set(
            providers.environmentVariable("JB_MARKETPLACE_TOKEN")
                .orElse(providers.gradleProperty("jbMarketplaceToken"))
        )

        // Para release normal no canal default:
        channels.set(listOf("default"))

        // Se quiser soltar beta/alpha primeiro, troca para:
        // channels.set(listOf("alpha"))

        // Opcional: publicar como hidden (não aparece publicamente após aprovação).
        // hidden.set(true)
    }

    // Assinatura é opcional aqui.
    // Se você for assinar depois, a própria doc do plugin mostra as opções suportadas.
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