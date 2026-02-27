package com.frshaka.gitbot.ai.dto

import com.squareup.moshi.Json

data class ModelResponse (

    @Json(name = "data")
    val models: List<Model>
)

data class Model(

    val id: String,
    @Json(name = "canonical_slug")
    val slug: String,
    val name: String,
    val description: String,
    val pricing: ModelPricing
)

data class ModelPricing(

    val prompt: String,
    val completion: String,
    val request: String?,
    val image: String?
)
