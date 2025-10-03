package com.service.api.dto

data class SignupResponse(
    val serviceUserId: Long,
    val accessToken: String,
    val refreshToken: String,
)
