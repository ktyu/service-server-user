package com.service.api.dto

data class PromptResult(
    val why: String,
    val what: String,
    val how: String,
    val tags: List<String>,
)
