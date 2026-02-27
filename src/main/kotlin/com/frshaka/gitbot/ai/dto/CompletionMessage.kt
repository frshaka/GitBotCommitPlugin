package com.frshaka.gitbot.ai.dto

data class CompletionMessage(

    val role: String,
    val content: String,

    // Presente apenas nas respostas de modelos de raciocínio (ex: DeepSeek-R1, QwQ).
    // Contém o Chain-of-Thought interno do modelo quando content ainda está vazio.
    // Ignorado na serialização quando nulo, portanto não é enviado nas requisições.
    val reasoning: String? = null
)
