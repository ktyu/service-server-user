package com.service.api.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(value = JsonInclude.Include.NON_NULL)
data class SaveIdentityResponse(
    val isMerged: Boolean,
    val serviceUserId: Long? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)
