package com.service.api.dto

data class RefreshRequest(
    val serviceUserId: Long,
    val refreshToken: String,
)
