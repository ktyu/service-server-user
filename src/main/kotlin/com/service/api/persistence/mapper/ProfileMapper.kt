package com.service.api.persistence.mapper

import com.service.api.model.Profile
import com.service.api.persistence.entity.JpaUserProfileEntity

object ProfileMapper {
    fun toModel(entity: JpaUserProfileEntity, email: String? = null) =
        Profile(
            nickname = entity.nickname,
            imageUrl = entity.imageUrl,
            email = email,
            district = entity.district,
            interestFields = entity.interestFields,
            interestLevel = entity.interestLevel,
            termsAgreements = entity.termsAgreements,
        )
}
