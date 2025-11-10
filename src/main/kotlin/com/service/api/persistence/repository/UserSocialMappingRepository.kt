package com.service.api.persistence.repository

import com.service.api.persistence.entity.JpaUserSocialMappingEntity
import com.service.api.persistence.entity.UserSocialMappingId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserSocialMappingRepository : JpaRepository<JpaUserSocialMappingEntity, UserSocialMappingId> {

    fun findByIdSocialId(socialId: Long): JpaUserSocialMappingEntity?

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE user_social_mapping sm SET
            sm.service_user_id = :toServiceUserId,
            sm.updated_at = CURRENT_TIMESTAMP
        WHERE sm.service_user_id = :fromServiceUserId
    """, nativeQuery = true)
    fun updateAllByServiceUserId(fromServiceUserId: Long, toServiceUserId: Long)

    fun deleteByIdSocialId(socialId: Long)
}
