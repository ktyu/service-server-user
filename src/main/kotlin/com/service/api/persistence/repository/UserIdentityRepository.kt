package com.service.api.persistence.repository

import com.service.api.persistence.entity.JpaUserIdentityEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserIdentityRepository : JpaRepository<JpaUserIdentityEntity, Long> {

    fun findByHashedCi(hashedCi: String): JpaUserIdentityEntity?

    @Modifying
    @Query("""
        UPDATE JpaUserIdentityEntity i SET
            i.hashedCi = NULL,
            i.updatedAt = CURRENT_TIMESTAMP,
            i.deletedAt = CURRENT_TIMESTAMP
        WHERE i.identityId = :identityId
    """)
    fun markDeletedByIdentityId(identityId: Long)
}
