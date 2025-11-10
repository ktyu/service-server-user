package com.service.api.persistence.repository

import com.service.api.model.SocialAccount
import com.service.api.model.UserProjection
import com.service.api.persistence.entity.JpaUserProfileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<JpaUserProfileEntity, Long> {

    @Query("""
        SELECT p AS profile, d AS device, i AS identity

        FROM JpaUserProfileEntity p

        LEFT JOIN JpaUserDeviceEntity d ON p.serviceUserId = d.id.serviceUserId AND d.id.customDeviceId = :customDeviceId AND d.deletedAt IS NULL

        LEFT JOIN JpaUserIdentityMappingEntity im ON im.id.serviceUserId = :serviceUserId AND im.deletedAt IS NULL
        LEFT JOIN JpaUserIdentityEntity i ON i.identityId = im.id.identityId AND i.deletedAt IS NULL

        WHERE p.serviceUserId = :serviceUserId AND p.deletedAt IS NULL
    """)
    fun findUserProjectionBy(serviceUserId: Long, customDeviceId: String): UserProjection?

    @Query("""
        SELECT new com.service.api.model.SocialAccount(
            s.socialId, s.socialType, s.email
        )
        FROM JpaUserSocialMappingEntity sm
        LEFT JOIN JpaUserSocialEntity s ON s.socialId = sm.id.socialId
        WHERE sm.id.serviceUserId = :serviceUserId
    """)
    fun findSocialAccountsBy(serviceUserId: Long): List<SocialAccount>
}
