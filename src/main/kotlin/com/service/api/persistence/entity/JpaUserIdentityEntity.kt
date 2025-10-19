package com.service.api.persistence.entity

import com.service.api.common.enum.GenderType
import com.service.api.common.enum.SocialType
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_identity",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_hased_ci", columnNames = ["hashed_ci"]),
        UniqueConstraint(name = "uq_kakao_uuid", columnNames = ["kakao_uuid"]),
        UniqueConstraint(name = "uq_apple_uuid", columnNames = ["apple_uuid"]),
        UniqueConstraint(name = "uq_naver_uuid", columnNames = ["naver_uuid"]),
        UniqueConstraint(name = "uq_google_uuid", columnNames = ["google_uuid"]),
    ]
)
class JpaUserIdentityEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_user_id")
    var serviceUserId: Long? = null,

    @Column(name = "hashed_ci", length = 64)
    var hashedCi: String?,

    @Column(name = "is_foreigner", nullable = false)
    val isForeigner: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    val gender: GenderType,

    @Column(name = "birthdate", nullable = false)
    val birthdate: LocalDate,

    @Column(name = "kakao_uuid", columnDefinition = "CHAR(36)")
    var kakaoUuid: String? = null,

    @Column(name = "apple_uuid", columnDefinition = "CHAR(36)")
    var appleUuid: String? = null,

    @Column(name = "naver_uuid", columnDefinition = "CHAR(36)")
    var naverUuid: String? = null,

    @Column(name = "google_uuid", columnDefinition = "CHAR(36)")
    var googleUuid: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Version
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
) {

    fun getSocialUuid(socialType: SocialType): String? {
        return when (socialType) {
            SocialType.KAKAO -> kakaoUuid
            SocialType.APPLE -> appleUuid
            SocialType.NAVER -> naverUuid
            SocialType.GOOGLE -> googleUuid
        }
    }

    fun setSocialUuid(socialType: SocialType, socialUuid: String?) {
        when (socialType) {
            SocialType.KAKAO -> kakaoUuid = socialUuid
            SocialType.APPLE -> appleUuid = socialUuid
            SocialType.NAVER -> naverUuid = socialUuid
            SocialType.GOOGLE -> googleUuid = socialUuid
        }
    }

    fun getExistSocials(): List<Pair<SocialType, String>> {
        return buildList {
            SocialType.entries.forEach { socialType ->
                when (socialType) {
                    SocialType.KAKAO -> kakaoUuid?.let { add(Pair(socialType, it)) }
                    SocialType.APPLE -> appleUuid?.let { add(Pair(socialType, it)) }
                    SocialType.NAVER -> naverUuid?.let { add(Pair(socialType, it)) }
                    SocialType.GOOGLE -> googleUuid?.let { add(Pair(socialType, it)) }
                }
            }
        }
    }

    fun getLatestSocialUuid(): String? {
        return getExistSocials().maxOfOrNull { it.second }
    }
}
