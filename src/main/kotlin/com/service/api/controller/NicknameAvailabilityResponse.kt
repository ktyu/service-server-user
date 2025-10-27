package com.service.api.controller

data class NicknameAvailabilityResponse(
    val nickname: String,
    val isForbidden: Boolean,
    val isOccupied: Boolean,
)
