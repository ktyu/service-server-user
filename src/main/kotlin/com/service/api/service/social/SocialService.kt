package com.service.api.service.social

import com.fasterxml.uuid.Generators
import com.service.api.client.KapiKakaoComClient
import com.service.api.common.enum.SocialType
import com.service.api.common.exception.InvalidSocialException
import com.service.api.persistence.repository.UserIdentityRepository
import com.service.api.persistence.repository.UserSocialRepository
import com.service.api.util.StringUtil.isValidUuid
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executor

@Service
class SocialService(
    private val userSocialRepository: UserSocialRepository,
    private val userIdentityRepository: UserIdentityRepository,
    private val asyncExecutor: Executor,

    kapiKakaoComClient: KapiKakaoComClient,
    @Value("\${api.kapi-kakao-com.app-id}") kakaoAppId: Int,
    @Value("\${api.kapi-kakao-com.app-admin-key}") kakaoAppAdminKey: String,
) {
    private val kakaoService = SocialKakaoService(userSocialRepository, kapiKakaoComClient, kakaoAppId, kakaoAppAdminKey)

    fun getSub(socialUuid: String, socialType: SocialType): String {
        val userSocialEntity = userSocialRepository.findByIdOrNull(socialUuid)
            ?: throw InvalidSocialException("userSocialEntity not found: $socialUuid")
        if (userSocialEntity.socialType != socialType)
            throw InvalidSocialException("userSocialEntity type unmatched: ${userSocialEntity.socialType} != $socialType")

        return userSocialEntity.sub
    }

    fun getEmail(socialUuid: String): String? {
        val userSocialEntity = userSocialRepository.findByIdOrNull(socialUuid)
            ?: throw InvalidSocialException("userSocialEntity not found: $socialUuid")

        return userSocialEntity.email
    }

    @Transactional
    fun saveSocialStatus(socialType: SocialType, socialAccessToken: String?, socialRefreshToken: String?): Pair<String, Long?> {
        try {
            val sub = when (socialType) {
                SocialType.KAKAO -> kakaoService.getKakaoSubByAccessToken(socialAccessToken!!, socialRefreshToken!!)
                SocialType.APPLE,
                SocialType.NAVER,
                SocialType.GOOGLE -> TODO()
            }

            val newSocialUuid = randomUUID7()
            var userSocialEntityCreatedAt = LocalDateTime.now()
            var serviceUserId: Long? = null

            val userSocialEntity = userSocialRepository.findBySocialTypeAndSub(socialType, sub)
            if (userSocialEntity != null) {
                // 이미 저장되어 있던 소셜 ID 라면, PK를 변경하기 위해 최초 연결된 시점만 보관 후 삭제하고, 연결되어 있는 serviceUserId 에도 갱신해줌
                userSocialEntityCreatedAt = userSocialEntity.createdAt
                userSocialRepository.deleteBySocialUuid(userSocialEntity.socialUuid)
                userSocialRepository.flush()
                serviceUserId = renewUserIdentitySocialUuid(socialType, userSocialEntity.socialUuid, newSocialUuid)
            }

            when (socialType) {
                SocialType.KAKAO -> kakaoService.saveSocialStatus(newSocialUuid, sub, socialAccessToken, socialRefreshToken, userSocialEntityCreatedAt)
                SocialType.APPLE,
                SocialType.NAVER,
                SocialType.GOOGLE -> TODO()
            }

            return Pair(newSocialUuid, serviceUserId)
        } catch (e: NullPointerException) {
            throw IllegalArgumentException(e.message)
        }
    }

    @Transactional
    fun removeSocialStatus(socialType: SocialType, sub: String): Pair<String?, Long?> {
        // 소셜 서버로부터 소셜 연결 해제 또는 소셜 계정 탈퇴 등을 통지 받을 때 처리
        val userSocialEntity = userSocialRepository.findBySocialTypeAndSub(socialType, sub)
            ?: return Pair(null, null)

        userSocialRepository.deleteBySocialUuid(userSocialEntity.socialUuid)

        val serviceUserId = renewUserIdentitySocialUuid(socialType, userSocialEntity.socialUuid, null)

        return Pair(userSocialEntity.socialUuid, serviceUserId)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun validateSocialUuid(socialUuid: String, socialType: SocialType) {
        // socialUuid 는 소셜 정보가 저장되고 5분간만 사용 가능
        if (!isUUID7AndGeneratedWithin(socialUuid, Duration.ofMinutes(5)))
            throw InvalidSocialException("socialUuid is invalid or too old: $socialUuid")

        val userSocialEntity = userSocialRepository.findByIdOrNull(socialUuid)
            ?: throw InvalidSocialException("userSocialEntity not found: $socialUuid in $socialType")
        if (userSocialEntity.socialType != socialType)
            throw InvalidSocialException("userSocialEntity type unmatched: ${userSocialEntity.socialType} != $socialType")

        when (socialType) {
            SocialType.KAKAO -> kakaoService.validateSocialStatus(userSocialEntity)
            SocialType.APPLE,
            SocialType.NAVER,
            SocialType.GOOGLE -> TODO()
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun validateSocialTypeAndSub(socialType: SocialType, sub: String): String {
        val userSocialEntity = userSocialRepository.findBySocialTypeAndSub(socialType, sub)
            ?: throw InvalidSocialException("userSocialEntity not found: $sub in $socialType")

        when (socialType) {
            SocialType.KAKAO -> kakaoService.validateSocialStatus(userSocialEntity)
            SocialType.APPLE,
            SocialType.NAVER,
            SocialType.GOOGLE -> TODO()
        }

        return userSocialEntity.socialUuid
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun deleteSocialUuid(socialUuid: String, socialType: SocialType, revokeSocialStatus: Boolean) {
        val userSocialEntity = userSocialRepository.findByIdOrNull(socialUuid)
            ?: return
        userSocialRepository.deleteBySocialUuid(socialUuid)

        if (revokeSocialStatus) {
            // 소셜 서버로 요청은 별도 스레드에서 비동기로 요청하며, 실패해도 무시함
            asyncExecutor.execute {
                when (socialType) {
                    SocialType.KAKAO -> kakaoService.revokeSocialStatus(userSocialEntity.sub)
                    SocialType.APPLE,
                    SocialType.NAVER,
                    SocialType.GOOGLE -> TODO()
                }
            }
        }
    }

    private fun renewUserIdentitySocialUuid(socialType: SocialType, oldSocialUuid: String, newSocialUuid: String?): Long? {
        val userIdentityEntity = when (socialType) {
            SocialType.KAKAO -> userIdentityRepository.findByKakaoUuid(oldSocialUuid)
            SocialType.APPLE -> userIdentityRepository.findByAppleUuid(oldSocialUuid)
            SocialType.NAVER -> userIdentityRepository.findByNaverUuid(oldSocialUuid)
            SocialType.GOOGLE -> userIdentityRepository.findByGoogleUuid(oldSocialUuid)
        } ?: return null

        userIdentityEntity.setSocialUuid(socialType, newSocialUuid)

        return userIdentityEntity.serviceUserId!!
    }

    private fun randomUUID7(): String {
        return Generators.timeBasedEpochGenerator().generate().toString()
    }

    private fun isUUID7AndGeneratedWithin(socialUuid: String, duration: Duration): Boolean {
        if (!socialUuid.isValidUuid()) return false

        // UUID Version 검사
        val versionChar = socialUuid[14]
        val version = versionChar.digitToIntOrNull(16) ?: return false
        if (version != 7) return false

        // UUID v7 → 상위 48비트에서 timestamp 추출
        val hex = socialUuid.replace("-", "").lowercase(Locale.getDefault())
        val timestampHex = hex.substring(0, 12) // 앞 48비트는 timestamp(ms)
        val generatedAtEpochMs = timestampHex.toLongOrNull(16) ?: return false
        val generatedAt = Instant.ofEpochMilli(generatedAtEpochMs)

        // now - duration 이후에 생성된 것인지 확인 (duration 이내인지)
        return !generatedAt.isBefore(Instant.now().minus(duration))
    }
}
