package com.service.api.dto

data class SaveIdentityResponse(
    val isMerged: Boolean,
    val serviceUserId: Long? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)
