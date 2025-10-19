package com.service.api.persistence.repository

import com.service.api.model.UserProjection
import com.service.api.persistence.entity.JpaUserIdentityEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserIdentityRepository : JpaRepository<JpaUserIdentityEntity, Long> {
    fun findByServiceUserIdAndDeletedAtIsNull(serviceUserId: Long): JpaUserIdentityEntity?

    fun findByHashedCi(hashedCi: String): JpaUserIdentityEntity?

    fun findByKakaoUuid(kakaoUuid: String): JpaUserIdentityEntity?
    fun findByAppleUuid(appleUuid: String): JpaUserIdentityEntity?
    fun findByNaverUuid(naverUuid: String): JpaUserIdentityEntity?
    fun findByGoogleUuid(googleUuid: String): JpaUserIdentityEntity?

    @Query("SELECT i AS identity, p AS profile, d AS device " +
            "FROM JpaUserIdentityEntity i " +
            "JOIN JpaUserProfileEntity p ON i.serviceUserId = p.serviceUserId AND p.deletedAt IS NULL " +
            "LEFT JOIN JpaUserDeviceEntity d ON i.serviceUserId = d.id.serviceUserId AND d.id.customDeviceId = :customDeviceId AND d.deletedAt IS NULL " +
            "WHERE i.serviceUserId = :serviceUserId AND i.deletedAt IS NULL")
    fun findUserProjectionByServiceUserIdAndCustomDeviceIdAndDeletedAtIsNull(serviceUserId: Long, customDeviceId: String): UserProjection?
}
