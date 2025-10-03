package com.service.api.dto

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
)
