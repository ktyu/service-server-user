package com.service.api.persistence.repository

import com.service.api.common.enum.SocialType
import com.service.api.persistence.entity.JpaUserSocialEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserSocialRepository : JpaRepository<JpaUserSocialEntity, Long> {

    fun findBySocialUuid(socialUuid: String): JpaUserSocialEntity?

    fun findBySocialTypeAndSub(socialType: SocialType, sub: String): JpaUserSocialEntity?
}
