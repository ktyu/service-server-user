package com.service.api.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String?,
)
