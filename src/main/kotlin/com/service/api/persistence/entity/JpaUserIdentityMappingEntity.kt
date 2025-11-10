package com.service.api.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.io.Serializable
import java.time.LocalDateTime

@Entity
@Table(name = "user_identity_mapping")
class JpaUserIdentityMappingEntity(
    @EmbeddedId
    val id: UserIdentityMappingId,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
)

@Embeddable
data class UserIdentityMappingId(
    @Column(name = "service_user_id")
    val serviceUserId: Long,

    @Column(name = "identity_id")
    val identityId: Long,
) : Serializable
