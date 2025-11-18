package com.service.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.service.api.common.enum.AgeGroup

@JsonInclude(value = JsonInclude.Include.NON_NULL)
data class SaveIdentityResponse(
    val isMerged: Boolean,
    val ageGroup: AgeGroup,
    val serviceUserId: Long? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)
