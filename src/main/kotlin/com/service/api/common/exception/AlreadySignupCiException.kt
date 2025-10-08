package com.service.api.common.exception

import com.service.api.common.enum.SocialType
import java.time.LocalDate

class AlreadySignupCiException(
    val data: AlreadySignupCiInfo?,
    val errorCode: String = "ALREADY_SIGNUP_CI",
    override val message: String = "이미 가입했던 이력이 확인돼요.",
) : RuntimeException()

data class AlreadySignupCiInfo(
    val serviceUserId: Long,
    val socialType: SocialType,
    val email: String,
    val lastLoginDate: LocalDate,
)
