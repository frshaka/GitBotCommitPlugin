package com.frshaka.gitbot.prompt

import java.nio.charset.StandardCharsets

object PromptLoader {

    fun load(resourcePath: String): String {
        val stream = PromptLoader::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: error("Missing resource: $resourcePath")

        return stream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }
}