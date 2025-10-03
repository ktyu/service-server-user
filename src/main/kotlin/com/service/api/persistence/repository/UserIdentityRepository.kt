package com.service.api.persistence.repository

import com.service.api.persistence.entity.JpaUserIdentityEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserIdentityRepository : JpaRepository<JpaUserIdentityEntity, Long> {
    fun findByServiceUserIdAndDeletedAtIsNull(serviceUserId: Long): JpaUserIdentityEntity?

    fun findByHashedCiAndDeletedAtIsNull(hashedCi: String): JpaUserIdentityEntity?

    fun findByKakaoSubAndDeletedAtIsNull(kakaoSub: String): JpaUserIdentityEntity?
}
