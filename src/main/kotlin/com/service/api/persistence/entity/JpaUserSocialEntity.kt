package com.service.api.persistence.entity

import com.service.api.common.enum.SocialType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_social",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_social_uuid", columnNames = ["social_uuid"]),
        UniqueConstraint(name = "uq_social_type_and_sub", columnNames = ["social_type", "sub"]),
    ]
)
class JpaUserSocialEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_id")
    var socialId: Long? = null,

    @Column(name = "social_uuid", nullable = false, columnDefinition = "CHAR(36)")
    var socialUuid: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 8)
    val socialType: SocialType,

    @Column(name = "sub", nullable = false, length = 256)
    val sub: String,

    @Column(name = "social_access_token", length = 1024)
    var socialAccessToken: String?,

    @Column(name = "social_id_token", length = 4096)
    var socialIdToken: String?,

    @Column(name = "social_refresh_token", length = 1024)
    var socialRefreshToken: String?,

    @Column(name = "email", nullable = false, length = 256)
    var email: String,

    @Column(name = "is_email_verified", nullable = false)
    var isEmailVerified: Boolean = false,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
