package com.service.api.service.social

import com.service.api.client.KakaoAccountInfo
import com.service.api.client.KapiKakaoComClient
import com.service.api.common.enum.SocialType
import com.service.api.common.exception.InvalidSocialException
import com.service.api.persistence.entity.JpaUserSocialEntity
import com.service.api.persistence.repository.UserSocialRepository
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.LocalDateTime

internal class SocialKakaoService(
    private val userSocialRepository: UserSocialRepository,
    private val kapiKakaoComClient: KapiKakaoComClient,
    private val kakaoAppId: Int,
    private val kakaoAppAdminKey: String,
) {
    private val socialType = SocialType.KAKAO
    /**
     * (참고) 카카오는 어드민 키로 사용자 관리가 가능하므로, 엑세스 토큰과 리프레시 토큰의 유효성을 관리하지 않는다 (저장되어 있어도 유효하지 않을 수 있음)
     */

    internal fun saveSocialStatus(socialUuid: String, socialAccessToken: String, socialRefreshToken: String): String {
        // 엑세스 토큰 검증하면서 sub 값 획득
        val kakaoSub = getKakaoSubByAccessToken(socialAccessToken)

        val kakaoAccountInfo = getKakaoAccountInfoByKakaoSubWithAdminKey(kakaoSub)

        var userSocialEntityCreatedAt: LocalDateTime? = null
        val userSocialEntity = userSocialRepository.findBySocialTypeAndSub(socialType, kakaoSub)
            ?.run {
                // 기존에 연결된 소셜 계정 정보가 존재하면, 최초 연결된 시점과 serviceUserId 를 기록하고 삭제
                userSocialEntityCreatedAt = this.createdAt
                userSocialRepository.deleteBySocialUuid(this.socialUuid)
                userSocialRepository.flush()
                null // null 로 변환해서 아래의 새 객체를 할당 (PK 값인 socialUuid 를 변경하므로 엔티티 삭제 후 재생성 필요)
            }
            ?: JpaUserSocialEntity(
                // 소셜 계정 정보 새로 저장
                socialUuid = socialUuid,
                socialType = socialType,
                sub = kakaoSub,
                socialAccessToken = socialAccessToken,
                socialIdToken = null,
                socialRefreshToken = socialRefreshToken,
                email = kakaoAccountInfo.getEmail(),
                isEmailVerified = kakaoAccountInfo.getIsEmailVerified(),
                createdAt = userSocialEntityCreatedAt ?: LocalDateTime.now(), // 기존에 연결된 카카오계정이었다면 기존 정보의 createdAt 을 보존
            )

        userSocialRepository.save(userSocialEntity)

        return kakaoSub
    }

    internal fun validateSocialStatus(userSocialEntity: JpaUserSocialEntity) {
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
        // 카카오 서버로 사용자 정보 조회 API 호출 (앱의 어드민 키 사용)
        try {
            return kapiKakaoComClient.getKakaoAccountInfo("KakaoAK $kakaoAppAdminKey", kakaoSub.toLong())
        } catch (e: WebClientResponseException.BadRequest) {
            throw InvalidSocialException("got 400 from kakao server. maybe disconnected kakao account!")
        }
    }

    private fun KakaoAccountInfo.getEmail(): String? = this.kakao_account?.email

    private fun KakaoAccountInfo.getIsEmailVerified(): Boolean = this.kakao_account?.run {
        (is_email_valid == true) && (is_email_verified == true)
    } ?: false
}
