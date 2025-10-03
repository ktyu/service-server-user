package com.service.api.dto

import com.service.api.common.enum.SocialType

data class SaveValidSocialRequest(
    val socialType: SocialType,
    val socialAccessToken: String?,
    val socialRefreshToken: String?,
    val authorizationCode: String?,
    val nonce: String?,
    val state: String?,
)
