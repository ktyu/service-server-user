package com.service.api.persistence.entity

import com.service.api.common.enum.GenderType
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_identity",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_hased_ci", columnNames = ["hashed_ci"]),
        UniqueConstraint(name = "uq_kakao_sub", columnNames = ["kakao_sub"]),
        UniqueConstraint(name = "uq_apple_sub", columnNames = ["apple_sub"]),
        UniqueConstraint(name = "uq_naver_sub", columnNames = ["naver_sub"]),
        UniqueConstraint(name = "uq_google_sub", columnNames = ["google_sub"]),
    ]
)
class JpaUserIdentityEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_user_id")
    var serviceUserId: Long? = null,

    @Column(name = "hashed_ci", nullable = false, length = 128)
    val hashedCi: String,

    @Column(name = "mobile_phone_number", nullable = false, length = 16)
    val mobilePhoneNumber: String,

    @Column(name = "is_foreigner", nullable = false)
    val isForeigner: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    val gender: GenderType,

    @Column(name = "birthdate", nullable = false)
    val birthdate: LocalDate,

    @Column(name = "kakao_sub")
    var kakaoSub: String? = null,

    @Column(name = "apple_sub")
    var appleSub: String? = null,

    @Column(name = "naver_sub")
    var naverSub: String? = null,

    @Column(name = "google_sub")
    var googleSub: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
)
