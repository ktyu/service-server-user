package com.service.api.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.service.api.common.InterestField
import com.service.api.common.enum.*
import com.service.api.persistence.entity.JpaUserDeviceEntity
import com.service.api.persistence.entity.JpaUserIdentityEntity
import com.service.api.persistence.entity.JpaUserProfileEntity

@JsonInclude(JsonInclude.Include.NON_NULL)
data class User(
    val profile: Profile?,
    val device: Device?,
    val voterType: VoterType?,
    val ageGroup: AgeGroup?,
    val socialAccounts: List<SocialAccount>?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Profile(
    val nickname: String?,
    val imageUrl: String?,
    val district: District?,
    val interestFields: Set<InterestField>?,
    val interestLevel: InterestLevel?,
    val termsAgreements: List<TermsAgreement>?,
)

data class TermsAgreement(
    val termsKey: String,
    val version: Int,
)

data class Device(
    val pushTokenType: PushTokenType,
    val pushToken: String,
)

data class SocialAccount(
    val id: Long,
    val socialType: SocialType,
    val email: String,
)

interface UserProjection {
    val profile: JpaUserProfileEntity
    val device: JpaUserDeviceEntity?
    val identity: JpaUserIdentityEntity?
}
