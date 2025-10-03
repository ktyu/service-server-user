package com.service.api.client

import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
interface KapiKakaoComClient {

    @GetExchange("/v1/user/access_token_info")
    fun getAccessTokenInfo(
        @RequestHeader("Authorization") accessTokenHeader: String,
    ) : KakaoAccessTokenInfo

    @GetExchange("/v2/user/me?target_id_type=user_id")
    fun getKakaoAccountInfo(
        @RequestHeader("Authorization") adminKeyHeader: String,
        @RequestParam("target_id") kakaoSub: Long,
    ) : KakaoAccountInfo
}

data class KakaoAccessTokenInfo(
    val id: Long,
    val expires_in: Int,
    val app_id: Int,
)

data class KakaoAccountInfo(
    val id: Long,
    val kakao_account: KakaoAccount?,
)

data class KakaoAccount(
    val email: String?,
    val is_email_valid: Boolean?,
    val is_email_verified: Boolean?,
)
