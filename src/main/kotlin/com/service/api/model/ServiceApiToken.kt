package com.service.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.service.api.common.enum.OsType
import com.service.api.common.enum.SocialType
import com.service.api.common.exception.InvalidTokenException

const val TOKEN_ISSUER = "service-api"

enum class TokenType(@get:JsonValue val json: String) {
    ACCESS("access"),
    REFRESH("refresh"),
    ;
}

/**
 * 공통 베이스: iss/token_type은 생성자에서 제외
 * - 직렬화 시 getter로 자동 포함
 * - 역직렬화 시 하위 클래스에서 검증
 */
sealed class ServiceApiTokenPayload {
    @get:JsonProperty("iss")
    val iss: String get() = TOKEN_ISSUER

    @get:JsonProperty("token_type")
    abstract val tokenType: TokenType

    @get:JsonProperty("iat")
    abstract val iat: Long

    @get:JsonProperty("exp")
    abstract val exp: Long

    @get:JsonProperty("service_user_id")
    abstract val serviceUserId: Long

    @get:JsonProperty("social_id")
    abstract val socialId: Long

    @get:JsonProperty("custom_device_id")
    abstract val customDeviceId: String

    @get:JsonProperty("device_model")
    abstract val deviceModel: String

    @get:JsonProperty("os_type")
    abstract val osType: OsType
}

/** 액세스 토큰 */
data class ServiceApiAccessTokenPayload(
    @JsonProperty("jti")
    val jti: String,

    @JsonProperty("iat")
    override val iat: Long,

    @JsonProperty("exp")
    override val exp: Long,

    @JsonProperty("service_user_id")
    override val serviceUserId: Long,

    @JsonProperty("social_id")
    override val socialId: Long,

    @JsonProperty("custom_device_id")
    override val customDeviceId: String,

    @JsonProperty("device_model")
    override val deviceModel: String,

    @JsonProperty("os_type")
    override val osType: OsType,
) : ServiceApiTokenPayload() {

    @JsonProperty("token_type")
    override val tokenType: TokenType = TokenType.ACCESS

    companion object {
        /** 역직렬화(파싱) 시 iss/token_type 값 강제 */
        @JvmStatic
        @JsonCreator
        fun create(
            @JsonProperty("jti") jti: String,
            @JsonProperty("iat") iat: Long,
            @JsonProperty("exp") exp: Long,
            @JsonProperty("service_user_id") serviceUserId: Long,
            @JsonProperty("social_id") socialId: Long,
            @JsonProperty("custom_device_id") customDeviceId: String,
            @JsonProperty("device_model") deviceModel: String,
            @JsonProperty("os_type") osType: OsType,
            @JsonProperty("iss") iss: String? = null,
            @JsonProperty("token_type") tokenTypeRaw: String? = null,
        ): ServiceApiAccessTokenPayload {
            if (iss != null && iss != TOKEN_ISSUER) {
                throw InvalidTokenException("Invalid iss: $iss")
            }
            if (tokenTypeRaw != null && tokenTypeRaw != TokenType.ACCESS.json) {
                throw InvalidTokenException("Invalid token_type for access token: $tokenTypeRaw")
            }
            return ServiceApiAccessTokenPayload(
                jti = jti,
                iat = iat,
                exp = exp,
                serviceUserId = serviceUserId,
                socialId = socialId,
                customDeviceId = customDeviceId,
                deviceModel = deviceModel,
                osType = osType,
            )
        }
    }
}

/** 리프레시 토큰 */
data class ServiceApiRefreshTokenPayload(
    @JsonProperty("iat")
    override val iat: Long,

    @JsonProperty("exp")
    override val exp: Long,

    @JsonProperty("service_user_id")
    override val serviceUserId: Long,

    @JsonProperty("social_id")
    override val socialId: Long,

    @JsonProperty("custom_device_id")
    override val customDeviceId: String,

    @JsonProperty("device_model")
    override val deviceModel: String,

    @JsonProperty("os_type")
    override val osType: OsType,
) : ServiceApiTokenPayload() {

    @JsonProperty("token_type")
    override val tokenType: TokenType = TokenType.REFRESH

    companion object {
        /** 역직렬화(파싱) 시 iss/token_type 값 강제 */
        @JvmStatic
        @JsonCreator
        fun create(
            @JsonProperty("iat") iat: Long,
            @JsonProperty("exp") exp: Long,
            @JsonProperty("service_user_id") serviceUserId: Long,
            @JsonProperty("social_id") socialId: Long,
            @JsonProperty("custom_device_id") customDeviceId: String,
            @JsonProperty("device_model") deviceModel: String,
            @JsonProperty("os_type") osType: OsType,
            @JsonProperty("iss") iss: String? = null,
            @JsonProperty("token_type") tokenTypeRaw: String? = null,
        ): ServiceApiRefreshTokenPayload {
            if (iss != null && iss != TOKEN_ISSUER) {
                throw InvalidTokenException("Invalid iss: $iss")
            }
            if (tokenTypeRaw != null && tokenTypeRaw != TokenType.REFRESH.json) {
                throw InvalidTokenException("Invalid token_type for refresh token: $tokenTypeRaw")
            }
            return ServiceApiRefreshTokenPayload(
                iat = iat,
                exp = exp,
                serviceUserId = serviceUserId,
                socialId = socialId,
                customDeviceId = customDeviceId,
                deviceModel = deviceModel,
                osType = osType,
            )
        }
    }
}
