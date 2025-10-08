package com.service.api.persistence.repository

import com.service.api.model.UserProjection
import com.service.api.persistence.entity.JpaUserIdentityEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserIdentityRepository : JpaRepository<JpaUserIdentityEntity, Long> {
    fun findByServiceUserIdAndDeletedAtIsNull(serviceUserId: Long): JpaUserIdentityEntity?

    fun findByHashedCiAndDeletedAtIsNull(hashedCi: String): JpaUserIdentityEntity?

    fun findByKakaoSubAndDeletedAtIsNull(kakaoSub: String): JpaUserIdentityEntity?

    @Query(
        "SELECT i AS identity, p AS profile, d AS device " +
            "FROM JpaUserIdentityEntity i " +
            "JOIN JpaUserProfileEntity p ON i.serviceUserId = p.serviceUserId AND p.deletedAt IS NULL " +
            "JOIN JpaUserDeviceEntity d ON i.serviceUserId = d.id.serviceUserId AND d.id.customDeviceId = :customDeviceId AND d.deletedAt IS NULL " +
            "WHERE i.serviceUserId = :serviceUserId AND i.deletedAt IS NULL"
    )
    fun findUserProjectionByServiceUserIdAndCustomDeviceIdAndDeletedAtIsNull(serviceUserId: Long, customDeviceId: String): UserProjection

    @Modifying
    @Query(
        "UPDATE JpaUserIdentityEntity i SET " +
            "i.hashedCi = CONCAT(i.hashedCi, '_DELETED_', i.serviceUserId), " +
            "i.kakaoSub = null, i.appleSub = null, i.naverSub = null, i.googleSub = null, " +
            "i.deletedAt = CURRENT_TIMESTAMP " +
            "WHERE i.serviceUserId = :serviceUserId"
    )
    fun markDeletedByServiceUserId(serviceUserId: Long)
}
