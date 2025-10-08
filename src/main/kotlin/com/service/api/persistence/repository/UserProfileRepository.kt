package com.service.api.persistence.repository

import com.service.api.persistence.entity.JpaUserProfileEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserProfileRepository : JpaRepository<JpaUserProfileEntity, Long> {

    @Modifying
    @Query(
        "UPDATE JpaUserProfileEntity p SET " +
            "p.nickname = CONCAT(p.nickname, '_DELETED_', p.serviceUserId), " +
            "p.deletedAt = CURRENT_TIMESTAMP " +
            "WHERE p.serviceUserId = :serviceUserId"
    )
    fun markDeletedByServiceUserId(serviceUserId: Long)
}
