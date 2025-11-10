package com.service.api.persistence.entity

import com.service.api.common.enum.GenderType
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_identity",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_hashed_ci", columnNames = ["hashed_ci"]),
    ]
)
class JpaUserIdentityEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identity_id")
    var identityId: Long? = null,

    @Column(name = "hashed_ci", length = 64)
    var hashedCi: String?,

    @Column(name = "is_foreigner", nullable = false)
    val isForeigner: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    val gender: GenderType,

    @Column(name = "birthdate", nullable = false)
    val birthdate: LocalDate,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
)
