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
@Table(name = "user_social_mapping")
class JpaUserSocialMappingEntity(
    @EmbeddedId
    val id: UserSocialMappingId,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

@Embeddable
data class UserSocialMappingId(
    @Column(name = "service_user_id")
    val serviceUserId: Long,

    @Column(name = "social_id")
    val socialId: Long,
) : Serializable
