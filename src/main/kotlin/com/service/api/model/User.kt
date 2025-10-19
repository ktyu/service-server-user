package com.service.api.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.service.api.common.AgeGroup
import com.service.api.common.InterestField
import com.service.api.common.enum.*

@JsonInclude(JsonInclude.Include.NON_NULL)
data class User(
    val profile: Profile?,
    val device: Device?,
    val voterType: VoterType?,
    val email: String?,
    val ageGroup: AgeGroup?,
    val socialTypes: List<SocialType>?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Profile(
    val nickname: String?,
    val imageUrl: String?,
    val district: District?,
    val interestFields: Set<InterestField>?,
    val interestLevel: InterestLevel?,
    val termsAgreements: Map<String, Int>?,
)

data class TermsAgreement(
    val termsKey: String,
    val version: Int,
)

data class Device(
    val pushTokenType: PushTokenType = PushTokenType.FCM_REGISTRATION_TOKEN,
    val pushToken: String,
)
