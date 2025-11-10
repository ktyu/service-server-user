package com.service.api.service.social

import com.fasterxml.uuid.Generators
import com.service.api.client.KapiKakaoComClient
import com.service.api.common.enum.SocialType
import com.service.api.common.exception.InvalidSocialException
import com.service.api.persistence.entity.JpaUserSocialMappingEntity
import com.service.api.persistence.entity.UserSocialMappingId
import com.service.api.persistence.repository.UserSocialMappingRepository
import com.service.api.persistence.repository.UserSocialRepository
import com.service.api.util.StringUtil.isValidUuid
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.Executor

@Service
class SocialService(
    private val userSocialRepository: UserSocialRepository,
    private val userSocialMappingRepository: UserSocialMappingRepository,
    private val asyncExecutor: Executor,

    kapiKakaoComClient: KapiKakaoComClient,
    @Value("\${api.kapi-kakao-com.app-id}") kakaoAppId: Int,
    @Value("\${api.kapi-kakao-com.app-admin-key}") kakaoAppAdminKey: String,
) {
    private val kakaoService = SocialKakaoService(kapiKakaoComClient, kakaoAppId, kakaoAppAdminKey)

    fun getServiceUserIdBy(socialId: Long): Long? {
        return userSocialMappingRepository.findByIdSocialId(socialId)?.id?.serviceUserId
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

            var userSocialEntity = userSocialRepository.findBySocialTypeAndSub(socialType, sub)
            userSocialEntity = when (socialType) {
                SocialType.KAKAO -> kakaoService.renewSocialStatus(newSocialUuid, sub, socialAccessToken, socialRefreshToken, userSocialEntity)
                SocialType.APPLE,
                SocialType.NAVER,
                SocialType.GOOGLE -> TODO()
            }
            userSocialEntity = userSocialRepository.save(userSocialEntity)

            val serviceUserId = userSocialMappingRepository.findByIdSocialId(userSocialEntity.socialId!!)?.id?.serviceUserId

            return Pair(newSocialUuid, serviceUserId)
        } catch (e: NullPointerException) {
            throw IllegalArgumentException(e.message)
        }
    }

    @Transactional
    fun deleteSocialStatus(socialType: SocialType, sub: String): Pair<Long?, Long?> {
        // 소셜 서버로부터 소셜 연결 해제 또는 소셜 계정 탈퇴 등을 통지 받을 때 처리
        val userSocialEntity = userSocialRepository.findBySocialTypeAndSub(socialType, sub)
            ?: return Pair(null, null)
        userSocialRepository.delete(userSocialEntity)

        val userSocialMappingEntity = userSocialMappingRepository.findByIdSocialId(userSocialEntity.socialId!!)
            ?: return Pair(userSocialEntity.socialId!!, null)
        userSocialMappingRepository.delete(userSocialMappingEntity)

        return Pair(userSocialEntity.socialId!!, userSocialMappingEntity.id.serviceUserId)
    }

    @Transactional
    fun saveUserSocialMapping(serviceUserId: Long, socialId: Long) {
        userSocialMappingRepository.save(
            JpaUserSocialMappingEntity(UserSocialMappingId(
                serviceUserId = serviceUserId,
                socialId = socialId,
            ))
        )
    }

    @Transactional
    fun mergeAllSocialMappingServiceUserId(sourceServiceUserId: Long, targetServiceUserId: Long) {
        userSocialMappingRepository.updateAllByServiceUserId(sourceServiceUserId, targetServiceUserId)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun validateSocialUuid(socialUuid: String, socialType: SocialType): Long {
        // socialUuid 는 소셜 정보가 저장되고 5분간만 사용 가능
        if (!isUUID7AndGeneratedWithin(socialUuid, Duration.ofMinutes(5)))
            throw InvalidSocialException("socialUuid is invalid or too old: $socialUuid")

        val userSocialEntity = userSocialRepository.findBySocialUuid(socialUuid)
            ?: throw InvalidSocialException("userSocialEntity not found: $socialUuid in $socialType")
        if (userSocialEntity.socialType != socialType)
            throw InvalidSocialException("userSocialEntity type unmatched: ${userSocialEntity.socialType} != $socialType")

        // 소셜 연결 상태 검증
        when (socialType) {
            SocialType.KAKAO -> kakaoService.validateSocialStatus(userSocialEntity)
            SocialType.APPLE,
            SocialType.NAVER,
            SocialType.GOOGLE -> TODO()
        }

        return userSocialEntity.socialId!!
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun validateSocialId(socialId: Long) {
        val userSocialEntity = userSocialRepository.findByIdOrNull(socialId)
            ?: throw InvalidSocialException("userSocialEntity not found: $socialId")

        when (userSocialEntity.socialType) {
            SocialType.KAKAO -> kakaoService.validateSocialStatus(userSocialEntity)
            SocialType.APPLE,
            SocialType.NAVER,
            SocialType.GOOGLE -> TODO()
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun deleteAndRevokeSocialStatus(serviceUserId: Long? = null, socialId: Long) {
        if (serviceUserId != null && serviceUserId != getServiceUserIdBy(socialId)) {
            throw IllegalArgumentException("serviceUserId not matched: $serviceUserId != ${getServiceUserIdBy(socialId)}")
        }
        userSocialMappingRepository.deleteByIdSocialId(socialId)

        val userSocialEntity = userSocialRepository.findByIdOrNull(socialId)
        if (userSocialEntity != null) {
            userSocialRepository.delete(userSocialEntity)

            // 소셜 서버로 요청은 별도 스레드에서 비동기로 요청하며, 실패해도 무시함
            asyncExecutor.execute {
                when (userSocialEntity.socialType) {
                    SocialType.KAKAO -> kakaoService.revokeSocialStatus(userSocialEntity.sub)
                    SocialType.APPLE,
                    SocialType.NAVER,
                    SocialType.GOOGLE -> TODO()
                }
            }
        }
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
