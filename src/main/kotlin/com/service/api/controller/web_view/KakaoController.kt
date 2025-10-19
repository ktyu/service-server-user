package com.service.api.controller.web_view

import com.service.api.common.enum.SocialType
import com.service.api.service.social.SocialService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/web-view")
class KakaoController(
    private val socialService: SocialService,
    @Value("\${api.kapi-kakao-com.app-id}") private val kakaoAppId: Int,
    @Value("\${api.kapi-kakao-com.app-admin-key}") private val kakaoAppAdminKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/callback/kakao/unlinked")
    fun callbackKakaoRevoked(
        @RequestHeader("Authorization") authorization: String,
        @RequestParam("app_id") appId: String,
        @RequestParam("user_id") userId: String,
        @RequestParam("referrer_type") referrerType: String,
        @RequestParam("group_user_token", required = false) groupUserToken: String?,
    ): ResponseEntity<Unit> {
        try {
            if (appId.toInt() != kakaoAppId)
                throw RuntimeException("kakaoAppId wrong: $appId")
            if (authorization != "KakaoAK $kakaoAppAdminKey")
                throw RuntimeException("authorization header wrong: $authorization")

            val (kakaoUuid, serviceUserId) = socialService.removeSocialStatus(SocialType.KAKAO, userId)
            if (kakaoUuid == null)
                throw RuntimeException("not existing socialStatus tried to remove by kakao server: sub=$userId")

            log.info("socialStatus removed by kakao server: socialUuid=$kakaoUuid, serviceUserId=$serviceUserId")
        } catch (e: Exception) {
            log.error("failed to removeSocialStatus: ${e.message}", e)
        }

        return ResponseEntity.ok().build()
    }
}
