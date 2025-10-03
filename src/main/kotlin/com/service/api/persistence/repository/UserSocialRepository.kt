package com.service.api.persistence.repository

import com.service.api.common.enum.SocialType
import com.service.api.persistence.entity.JpaUserSocialEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserSocialRepository : JpaRepository<JpaUserSocialEntity, Long> {

    fun findBySocialTypeAndSub(socialType: SocialType, sub: String): JpaUserSocialEntity?

    fun deleteBySocialUuid(socialUuid: String)

    fun findBySocialUuidAndSocialTypeAndDeletedAtIsNull(socialUuid: String, socialType: SocialType): JpaUserSocialEntity?
}
