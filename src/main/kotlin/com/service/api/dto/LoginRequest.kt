package com.service.api.dto

data class LoginRequest(
    val serviceUserId: Long,
    val social: Social,
    val device: Device,
)
