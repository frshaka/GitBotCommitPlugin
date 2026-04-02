package com.frshaka.gitbot.ai.dto

// DTO usado exclusivamente nas requisições à API.
// Não contém o campo "reasoning" para evitar que seja serializado
// e enviado como campo desconhecido a providers que não o aceitam.
data class CompletionMessageRequisicao(
    val role: String,
    val content: String
)
