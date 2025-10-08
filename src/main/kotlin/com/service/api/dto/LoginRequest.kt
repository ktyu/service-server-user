package com.service.api.dto

import com.service.api.model.Device

data class LoginRequest(
    val serviceUserId: Long,
    val social: Social,
    val device: Device,
)
