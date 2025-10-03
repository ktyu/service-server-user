package com.service.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.service.api.common.enum.SocialType

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SaveValidSocialResponse(
    val social: Social,
    val serviceUserId: Long?,
)

data class Social(
    val socialType: SocialType,
    val socialUuid: String,
)
