package com.service.api.persistence.mapper

import com.service.api.model.Profile
import com.service.api.model.TermsAgreement
import com.service.api.persistence.entity.JpaUserProfileEntity

object ProfileMapper {
    fun toModel(entity: JpaUserProfileEntity) =
        Profile(
            nickname = entity.nickname,
            imageUrl = entity.imageUrl,
            district = entity.district,
            interestFields = entity.interestFields,
            interestLevel = entity.interestLevel,
            termsAgreements = entity.termsAgreements.map { (k, v) -> TermsAgreement(k, v) },
        )
}
