package com.service.api.service

import com.service.api.client.KakaoAccountInfo
import com.service.api.client.KapiKakaoComClient
import com.service.api.common.enum.SocialType
import com.service.api.common.exception.InvalidSocialException
import com.service.api.persistence.entity.JpaUserSocialEntity
import com.service.api.persistence.repository.UserIdentityRepository
import com.service.api.persistence.repository.UserSocialRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class SocialKakaoService(
    private val userSocialRepository: UserSocialRepository,
    private val userIdentityRepository: UserIdentityRepository,

    private val kapiKakaoComClient: KapiKakaoComClient,
    @Value("\${api.kapi-kakao-com.app-id}") private val kakaoAppId: Int,
    @Value("\${api.kapi-kakao-com.app-admin-key}") private val kakaoAppAdminKey: String,
) {
    private val socialType = SocialType.KAKAO
    /**
     * (참고) 카카오는 어드민 키로 사용자 관리가 가능하므로, 엑세스 토큰과 리프레시 토큰의 유효성을 관리하지 않는다 (저장되어 있어도 유효하지 않을 수 있음)
     */

    @Transactional
    fun saveSocial(socialAccessToken: String, socialRefreshToken: String): Pair<String, Long?> {
        val socialUuid = UUID.randomUUID().toString()
        var serviceUserId: Long? = null

        // 엑세스 토큰 검증하면서 sub 값 획득
        val kakaoSub = getKakaoSubByAccessToken(socialAccessToken)

        val kakaoAccountInfo = getKakaoAccountInfoByKakaoSubWithAdminKey(kakaoSub)

        val userSocialEntity = userSocialRepository.findBySocialTypeAndSub(socialType, kakaoSub)
            ?.run {
                // 기존에 연결된 카카오계정 정보가 존재하면, serviceUserId 만 찾고 삭제
                serviceUserId = userIdentityRepository.findByKakaoSubAndDeletedAtIsNull(kakaoSub)?.serviceUserId
                userSocialRepository.deleteBySocialUuid(this.socialUuid)
                userSocialRepository.flush()
                null // null 로 변환해서 아래의 새 객체를 할당
            }
            ?: JpaUserSocialEntity(
                // 새로 연결된 카카오계정
                socialUuid = socialUuid,
                socialType = socialType,
                sub = kakaoSub,
                socialAccessToken = socialAccessToken,
                socialIdToken = null,
                socialRefreshToken = socialRefreshToken,
                email = kakaoAccountInfo.getEmail(),
                isEmailVerified = kakaoAccountInfo.getIsEmailVerified(),
            )

        userSocialRepository.save(userSocialEntity)

        return Pair(socialUuid, serviceUserId)
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun getSubWithExternalValidation(socialUuid: String): String {
        val userSocialEntity = userSocialRepository.findBySocialUuidAndSocialTypeAndDeletedAtIsNull(socialUuid, socialType)
            ?: throw InvalidSocialException("socialUuid not found: $socialUuid (as $socialType)")

        val kakaoAccountInfo = getKakaoAccountInfoByKakaoSubWithAdminKey(userSocialEntity.sub)

        userSocialRepository.save(userSocialEntity
            .apply {
                // 엑세스 토큰을 갱신하지 않으므로, 소셜 연결 상태를 확인했다는 의미로 updatedAt 필드를 직접 갱신해줌
                this.updatedAt = LocalDateTime.now()

                // 연결 상태 검증을 위해 정보 조회를 하는데, 혹시 이메일이 바뀌었으면 반영해줌
                val currentEmail = kakaoAccountInfo.getEmail()
                val currentIsEmailVerified = kakaoAccountInfo.getIsEmailVerified()
                if (this.email != currentEmail) this.email = currentEmail
                if (this.isEmailVerified != currentIsEmailVerified) this.isEmailVerified = currentIsEmailVerified
            })

        return userSocialEntity.sub
    }

    private fun getKakaoSubByAccessToken(kakaoAccessToken: String): String {
        // 카카오 서버로 액세스 토큰 정보 조회 API 호출
        val kakaoAccessTokenInfo = kotlin.runCatching {
            kapiKakaoComClient.getAccessTokenInfo("Bearer $kakaoAccessToken")
        }.getOrNull() ?: throw InvalidSocialException("Failed to acquire kakaoAccessTokenInfo")

        // 토큰의 app_id 가 우리 앱 정보와 동일해야함
        if (kakaoAccessTokenInfo.app_id != kakaoAppId)
            throw InvalidSocialException("kakao app_id is incorrect: ${kakaoAccessTokenInfo.app_id}")

        // 내부 id 값이 long 으로 넘어오는데, sub 으로 사용하기 위해 string 으로 바꿔서 리턴
        return kakaoAccessTokenInfo.id.toString()
    }

    private fun getKakaoAccountInfoByKakaoSubWithAdminKey(kakaoSub: String) : KakaoAccountInfo {
        // 카카오 서버로 사용자 정보 조회 API 호출 (앱의 어드민 키 사용) TODO: 이거만 호출하면 가입 미완료자 아닌걸로 간주되는지 테스트 한번 해보기.. 1~2일정도 지나도 refresh 토큰 유효하면 괜찮을듯 (https://devtalk.kakao.com/t/notice-unlink-for-users-who-have-not-completed-a-signup/111463)
        try {
            return kapiKakaoComClient.getKakaoAccountInfo("KakaoAK $kakaoAppAdminKey", kakaoSub.toLong())
        } catch (e: Exception) {
            throw InvalidSocialException()// TODO: 400에 연동 끊긴 유저 메세지면 InvalidSocialException 던지고 나머지는 그대로 던져서 500 나가게
        }
    }

    private fun KakaoAccountInfo.getEmail(): String? = this.kakao_account?.email

    private fun KakaoAccountInfo.getIsEmailVerified(): Boolean = this.kakao_account?.run {
        (is_email_valid == true) && (is_email_verified == true)
    } ?: false
}
