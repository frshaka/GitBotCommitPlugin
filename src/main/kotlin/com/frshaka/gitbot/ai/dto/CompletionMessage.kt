package com.frshaka.gitbot.ai.dto

data class CompletionMessage(

    val role: String,
    val content: String,

    // Presente apenas nas respostas de modelos de raciocínio (ex: DeepSeek-R1, QwQ).
    // Contém o Chain-of-Thought interno do modelo quando content ainda está vazio.
    // Este DTO é usado apenas para desserializar respostas. Para requisições, use CompletionMessageRequisicao.
    val reasoning: String? = null
)
