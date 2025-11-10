package com.service.api.persistence.repository

import com.service.api.persistence.entity.JpaUserIdentityMappingEntity
import com.service.api.persistence.entity.UserIdentityMappingId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserIdentityMappingRepository : JpaRepository<JpaUserIdentityMappingEntity, UserIdentityMappingId> {

    fun findByIdIdentityIdAndDeletedAtIsNull(identityId: Long): JpaUserIdentityMappingEntity?

    fun findByIdServiceUserIdAndDeletedAtIsNull(serviceUserId: Long): JpaUserIdentityMappingEntity?

    @Modifying
    @Query("""
        UPDATE JpaUserIdentityMappingEntity im SET
            im.updatedAt = CURRENT_TIMESTAMP,
            im.deletedAt = CURRENT_TIMESTAMP
        WHERE im.id.identityId = :identityId
    """)
    fun markDeletedByIdentityId(identityId: Long)
}
