package com.service.api.service.social

import com.service.api.client.KakaoAccountInfo
import com.service.api.client.KapiKakaoComClient
import com.service.api.common.enum.SocialType
import com.service.api.common.exception.InvalidSocialException
import com.service.api.persistence.entity.JpaUserSocialEntity
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.LocalDateTime

internal class SocialKakaoService(
    private val kapiKakaoComClient: KapiKakaoComClient,
    private val kakaoAppId: Int,
    private val kakaoAppAdminKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val socialType = SocialType.KAKAO
    /**
     * (참고) 카카오는 어드민 키로 사용자 관리가 가능하므로, 엑세스 토큰과 리프레시 토큰의 유효성을 관리하지 않는다 (저장되어 있어도 유효하지 않을 수 있음)
     */

    internal fun getKakaoSubByAccessToken(kakaoAccessToken: String, kakaoRefreshToken: String): String {
        if (kakaoRefreshToken.isBlank())
            throw InvalidSocialException("kakaoRefreshToken is blank")

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

    internal fun renewSocialStatus(socialUuid: String, kakaoSub: String, socialAccessToken: String, socialRefreshToken: String, existingEntity: JpaUserSocialEntity? = null): JpaUserSocialEntity {
        val kakaoAccountInfo = getKakaoAccountInfoByKakaoSubWithAdminKey(kakaoSub)

        return existingEntity?.apply {
            this.socialUuid = socialUuid
            this.socialAccessToken = socialAccessToken
            this.socialRefreshToken = socialRefreshToken
            this.email = kakaoAccountInfo.getEmail()
            this.isEmailVerified = kakaoAccountInfo.getIsEmailVerified()
        } ?: JpaUserSocialEntity(
            socialUuid = socialUuid,
            socialType = socialType,
            sub = kakaoSub,
            socialAccessToken = socialAccessToken,
            socialIdToken = null,
            socialRefreshToken = socialRefreshToken,
            email = kakaoAccountInfo.getEmail(),
            isEmailVerified = kakaoAccountInfo.getIsEmailVerified(),
        )
    }

    internal fun validateSocialStatus(userSocialEntity: JpaUserSocialEntity) {
        with (userSocialEntity) {
            val kakaoAccountInfo = getKakaoAccountInfoByKakaoSubWithAdminKey(sub)

            // 엑세스 토큰을 갱신하지 않으므로, 소셜 연결 상태를 확인했다는 의미로 updatedAt 필드를 직접 갱신해줌
            updatedAt = LocalDateTime.now()

            // 연결 상태 검증을 위해 정보 조회를 하는데, 이메일이 바뀌었을 수 있으므로 반영해줌
            email = kakaoAccountInfo.getEmail()
            isEmailVerified = kakaoAccountInfo.getIsEmailVerified()
        }
    }

    internal fun revokeSocialStatus(kakaoSub: String) {
        try {
            kapiKakaoComClient.unlinkKakaoAccount("KakaoAK $kakaoAppAdminKey", kakaoSub.toLong())
                .let {
                    if (it.id != kakaoSub.toLong())
                        log.error("id in response unmatched: ${it.id} != ${kakaoSub.toLong()}")
                }
            log.info("socialStatus revoked: kakaoSub=$kakaoSub")
        } catch (e: Exception) {
            log.error("got exception while calling kakao server: ${e.message}", e)
        }
    }

    private fun getKakaoAccountInfoByKakaoSubWithAdminKey(kakaoSub: String) : KakaoAccountInfo {
        // 카카오 서버로 사용자 정보 조회 API 호출 (앱의 어드민 키 사용)
        try {
            return kapiKakaoComClient.getKakaoAccountInfo("KakaoAK $kakaoAppAdminKey", kakaoSub.toLong())
        } catch (e: WebClientResponseException.BadRequest) {
            throw InvalidSocialException("got 400 from kakao server. maybe disconnected kakao account!")
        }
    }

    private fun KakaoAccountInfo.getEmail(): String = this.kakao_account?.email ?: ""

    private fun KakaoAccountInfo.getIsEmailVerified(): Boolean = this.kakao_account?.run {
        (is_email_valid == true) && (is_email_verified == true)
    } ?: false
}
