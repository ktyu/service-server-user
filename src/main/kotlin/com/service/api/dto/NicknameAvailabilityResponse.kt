package com.service.api.dto

data class NicknameAvailabilityResponse(
    val nickname: String,
    val isForbidden: Boolean,
    val isOccupied: Boolean,
)
