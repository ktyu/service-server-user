package com.service.api.service.social

import com.service.api.client.KapiKakaoComClient
import com.service.api.common.enum.SocialType
import com.service.api.common.exception.InvalidSocialException
import com.service.api.persistence.repository.UserIdentityRepository
import com.service.api.persistence.repository.UserSocialRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class SocialService(
    private val userSocialRepository: UserSocialRepository,
    private val userIdentityRepository: UserIdentityRepository,

    kapiKakaoComClient: KapiKakaoComClient,
    @Value("\${api.kapi-kakao-com.app-id}") kakaoAppId: Int,
    @Value("\${api.kapi-kakao-com.app-admin-key}") kakaoAppAdminKey: String,
) {
    private val kakaoService = SocialKakaoService(userSocialRepository, kapiKakaoComClient, kakaoAppId, kakaoAppAdminKey)

    @Transactional
    fun saveSocialStatus(socialType: SocialType, socialAccessToken: String?, socialRefreshToken: String?): Pair<String, Long?> {
        val socialUuid = UUID.randomUUID().toString()

        // 각 소셜 별로 필요한 정보가 부족한 경우, NullPointerException 던져지고 컨트롤러에서 400으로 변환해서 리턴
        val sub = when (socialType) {
            SocialType.KAKAO -> kakaoService.saveSocialStatus(socialUuid, socialAccessToken!!, socialRefreshToken!!)
            SocialType.APPLE,
            SocialType.NAVER,
            SocialType.GOOGLE -> TODO()
        }

        val serviceUserId = userIdentityRepository.findByKakaoSubAndDeletedAtIsNull(sub)?.serviceUserId
        return Pair(socialUuid, serviceUserId)
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun getSubWithValidation(socialType: SocialType, socialUuid: String): String {
        val userSocialEntity = userSocialRepository.findBySocialUuidAndDeletedAtIsNull(socialUuid)
            ?: throw InvalidSocialException("userSocialEntity not found: $socialUuid in $socialType")
        if (userSocialEntity.socialType != socialType)
            throw InvalidSocialException("userSocialEntity type unmatched: ${userSocialEntity.socialType} != $socialType")

        when (socialType) {
            SocialType.KAKAO -> kakaoService.validateSocialStatus(userSocialEntity)
            SocialType.APPLE,
            SocialType.NAVER,
            SocialType.GOOGLE -> TODO()
        }

        return userSocialEntity.sub
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun getSocialUuidWithValidation(socialType: SocialType, sub: String): String {
        val userSocialEntity = userSocialRepository.findBySocialTypeAndSubAndDeletedAtIsNull(socialType, sub)
            ?: throw InvalidSocialException("userSocialEntity not found: $sub in $socialType")

        when (socialType) {
            SocialType.KAKAO -> kakaoService.validateSocialStatus(userSocialEntity)
            SocialType.APPLE,
            SocialType.NAVER,
            SocialType.GOOGLE -> TODO()
        }

        return userSocialEntity.socialUuid
    }
}
