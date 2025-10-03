package com.service.api.model

data class Terms(
    val termsKey: String,
    val version: Int,
    val displayOrder: Int,
    val isMandatory: Boolean,
    val title: String,
    val content: String,
    val contentLink: String,
)
