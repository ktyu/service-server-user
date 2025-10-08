package com.service.api.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.service.api.common.ApiRequestContextHolder
import com.service.api.common.enum.PushTokenType
import com.service.api.common.enum.SocialType
import com.service.api.common.exception.InvalidSocialException
import com.service.api.common.exception.InvalidTokenException
import com.service.api.model.ServiceApiAccessTokenPayload
import com.service.api.model.ServiceApiRefreshTokenPayload
import com.service.api.persistence.entity.JpaUserDeviceEntity
import com.service.api.persistence.entity.UserDeviceId
import com.service.api.persistence.repository.UserDeviceRepository
import com.service.api.persistence.repository.UserIdentityRepository
import com.service.api.service.social.SocialService
import com.service.api.util.Sha256HashingUtil
import com.fasterxml.jackson.module.kotlin.readValue
import com.service.api.common.enum.OsType
import com.service.api.common.exception.ExpiredTokenException
import com.service.api.model.DeviceVersion
import com.service.api.model.ServiceApiTokenPayload
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets

@Service
class DeviceService(
    private val userDeviceRepository: UserDeviceRepository,
    private val userIdentityRepository: UserIdentityRepository,
    private val socialService: SocialService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun saveDevice(serviceUserId: Long, socialType: SocialType, socialUuid: String?, pushTokenType: PushTokenType, pushToken: String): Pair<String, String> {
        val userIdentitySub = userIdentityRepository.findByServiceUserIdAndDeletedAtIsNull(serviceUserId)?.getSub(socialType)
            ?: throw InvalidSocialException("sub in userIdentityEntity is null: $socialType/$serviceUserId")

        if (socialUuid != null) {
            // 소셜 연결 상태 및 userIdentity 매칭 검증
            val sub = socialService.getSubWithValidation(socialType, socialUuid)
            if (sub != userIdentitySub)
                throw InvalidSocialException("sub not matched: $sub != $userIdentitySub")
        }

        // 헤더 정보 가져와서 기존 저장된거 있나 보고 있으면 비교, 없으면 추가
        val ctx = ApiRequestContextHolder.get()
        val userDeviceEntity = userDeviceRepository.findByIdOrNull(UserDeviceId(serviceUserId, ctx.customDeviceId))
            ?.apply {
                // 기존에 사용했던 디바이스 재로그인
                this.deviceModel = ctx.deviceModel
                this.osType = ctx.osType
                this.osVersion = ctx.osVersion
                this.appVersion = ctx.appVersion
                this.lastLoginAt = LocalDateTime.now()
                this.pushTokenType = pushTokenType
                this.pushToken = pushToken
                this.deletedAt = null
            }
            ?: JpaUserDeviceEntity(
                // 신규 디바이스 로그인
                id = UserDeviceId(serviceUserId, ctx.customDeviceId),
                deviceModel = ctx.deviceModel,
                osType = ctx.osType,
                osVersion = ctx.osVersion,
                appVersion = ctx.appVersion,
                lastLoginAt = LocalDateTime.now(),
                pushTokenType = pushTokenType,
                pushToken = pushToken,
            )

        // 토큰 발급
        val iat = Instant.now().epochSecond
        val (accessToken, refreshToken) = issueTokens(iat, serviceUserId, socialType, userIdentitySub, null)
        userDeviceEntity.accessTokenIssuedAt = iat
        userDeviceEntity.refreshTokenIssuedAt = iat

        userDeviceRepository.save(userDeviceEntity)

        return Pair(accessToken, refreshToken!!)
    }

    @Transactional
    fun refreshTokens(serviceUserId: Long, refreshToken: String): Pair<String, String?> {
        val ctx = ApiRequestContextHolder.get()
        val userDeviceEntity = userDeviceRepository.findByIdAndDeletedAtIsNull(UserDeviceId(serviceUserId, ctx.customDeviceId))
            ?.apply {
                // 요청 헤더와 DB 디바이스 정보 간 (customDeviceId, deviceModel, osType) 일치 여부 검증
                if (this.id.customDeviceId != ctx.customDeviceId)
                    throw InvalidTokenException("customDeviceId unmatched: ${this.id.customDeviceId} != ${ctx.customDeviceId}")
                if (this.deviceModel != ctx.deviceModel)
                    throw InvalidTokenException("deviceModel unmatched: ${this.deviceModel} != ${ctx.deviceModel}")
                if (this.osType != ctx.osType)
                    throw InvalidTokenException("osType unmatched: ${this.osType} != ${ctx.osType}")

                // 요청 헤더와 DB 디바이스 정보 간 (osVersion, appVersion) 변경되었다면 DB 반영
                if (this.osVersion != ctx.osVersion)
                    this.osVersion = ctx.osVersion
                if (this.appVersion != ctx.appVersion)
                    this.appVersion = ctx.appVersion
            }
            ?: throw InvalidTokenException("UserDeviceEntity not found: ${serviceUserId}/${ctx.customDeviceId}")

        // refreshToken 유효성 확인
        val refreshTokenPayload = try {(getServiceApiTokenPayload(refreshToken) as? ServiceApiRefreshTokenPayload)
            ?: throw RuntimeException("class cast fail")
        } catch (e: ExpiredTokenException) {
            throw InvalidTokenException("refresh token expired: ${e.message}")
        }

        with (refreshTokenPayload) {
            // refresh token 과 파라미터/DB 정보 매칭 검증
            if (this.serviceUserId != serviceUserId)
                throw InvalidTokenException("serviceUserId unmatched: ${this.serviceUserId} != $serviceUserId")
            if (this.iat != userDeviceEntity.refreshTokenIssuedAt)
                throw InvalidTokenException("invalid refresh token iat: ${this.iat} != ${userDeviceEntity.refreshTokenIssuedAt}")
            if (this.deviceModel != userDeviceEntity.deviceModel || this.osType != userDeviceEntity.osType)
                throw InvalidTokenException("deviceModel or osType changed: $this != $userDeviceEntity")

            // userIdentity 매칭 검증
            userIdentityRepository.findByServiceUserIdAndDeletedAtIsNull(serviceUserId)?.getSub(socialType)
                ?.also { userIdentitySub ->
                    if (sub != userIdentitySub)
                        throw InvalidTokenException("sub in userIdentityEntity(serviceUserId=$serviceUserId) unmatched: $sub != $userIdentitySub")
                }
                ?: throw InvalidTokenException("sub in userIdentityEntity(serviceUserId=$serviceUserId) is null: $socialType")

            // 소셜 연결 상태 검증
            socialService.getSocialUuidWithValidation(socialType, sub).also { socialUuid ->
                log.info("social status validated: $socialUuid/$socialType/$sub")
            }

            // 토큰 발급 및 iat 갱신
            val newIat = Instant.now().epochSecond
            val (newAccessToken, newRefreshToken) = issueTokens(newIat, serviceUserId, socialType, sub, exp)
            userDeviceEntity.accessTokenIssuedAt = newIat
            if (newRefreshToken != null) userDeviceEntity.refreshTokenIssuedAt = newIat

            userDeviceRepository.save(userDeviceEntity)

            return Pair(newAccessToken, newRefreshToken)
        }
    }

    @Transactional
    fun deleteDevice(serviceUserId: Long, customDeviceId: String) {
        userDeviceRepository.markDeletedByServiceUserIdAndCustomDeviceId(serviceUserId, customDeviceId)
    }

    @Transactional
    fun deleteAllDevice(serviceUserId: Long) {
        userDeviceRepository.markDeletedByServiceUserId(serviceUserId)
    }

    fun getServiceApiTokenPayload(token: String): ServiceApiTokenPayload {
        val parts = token.trim().split('.')
        val payload = when (parts.size) {
            3 -> TokenSupport.decodeJwt(parts)
            5 -> TokenSupport.decryptJwe(parts)
            else -> throw InvalidTokenException("unknown getServiceApiTokenPayload: parts.size=${parts.size}")
        }

        if (payload.exp < Instant.now().epochSecond)
            throw ExpiredTokenException("expired token: ${payload.exp}")

        return payload
    }

    fun getDeviceVersionForAuth(serviceUserId: Long, customDeviceId: String, deviceModel: String, osType: OsType): DeviceVersion? {
        return userDeviceRepository.findByIdAndDeletedAtIsNull(UserDeviceId(serviceUserId, customDeviceId))
            ?.takeIf { it.deviceModel == deviceModel && it.osType == osType }
            ?.run { DeviceVersion(accessTokenIssuedAt, osVersion, appVersion) }
    }

    internal fun getLastLoginDateByServiceUserId(serviceUserId: Long): LocalDate? {
        return userDeviceRepository.findAllById_ServiceUserId(serviceUserId)
            .maxOfOrNull { it.lastLoginAt }
            ?.toLocalDate()
    }

    private fun issueTokens(iat: Long, serviceUserId: Long, socialType: SocialType, sub: String, existingRefreshTokenExp: Long?): Pair<String, String?> {
        // 현 refreshToken 의 만료가 일정기간 이하로 남았거나 없는 경우에 refreshToken 도 발급
        val issueRefreshToken = existingRefreshTokenExp?.let { exp ->
            (exp - Instant.now().epochSecond) < TokenSupport.REFRESH_TOKEN_REISSUE_THRESHOLD.toSeconds()
        } ?: true

        // 토큰 발급 (accessToken-jwt, refreshToken-jwe)
        val accessToken = createAccessToken(iat, serviceUserId)
        val refreshToken = if (issueRefreshToken) createRefreshToken(iat, serviceUserId, socialType, sub) else null

        return Pair(accessToken, refreshToken)
    }

    private fun createAccessToken(iat: Long, serviceUserId: Long): String {
        val ctx = ApiRequestContextHolder.get()
        val exp = Instant.ofEpochSecond(iat).plus(TokenSupport.ACCESS_TOKEN_TTL).epochSecond
        val payload = ServiceApiAccessTokenPayload(
            jti = TokenSupport.generateJwtId(),
            iat = iat,
            exp = exp,
            serviceUserId = serviceUserId,
            customDeviceId = ctx.customDeviceId,
            deviceModel = ctx.deviceModel,
            osType = ctx.osType,
        )

        return TokenSupport.encodeJwt(TokenSupport.JwtHeader(), payload)
    }

    private fun createRefreshToken(iat: Long, serviceUserId: Long, socialType: SocialType, sub: String): String {
        val ctx = ApiRequestContextHolder.get()
        val exp = Instant.ofEpochSecond(iat).plus(TokenSupport.REFRESH_TOKEN_TTL).epochSecond
        val payload = ServiceApiRefreshTokenPayload(
            socialType = socialType,
            sub = sub,
            iat = iat,
            exp = exp,
            serviceUserId = serviceUserId,
            customDeviceId = ctx.customDeviceId,
            deviceModel = ctx.deviceModel,
            osType = ctx.osType,
        )

        return TokenSupport.encryptJwe(TokenSupport.JweHeader(), payload)
    }

    private object TokenSupport {
        val objectMapper = jacksonObjectMapper()
        val secureRandom = SecureRandom()
        private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()
        private val base64UrlDecoder = Base64.getUrlDecoder()

        val ACCESS_TOKEN_TTL: Duration = Duration.ofMinutes(10) // TODO: 개발 다 되면 1시간으로 설정
        val REFRESH_TOKEN_TTL: Duration = Duration.ofMinutes(30) // TODO: 개발 다 되면 90일로 설정
        val REFRESH_TOKEN_REISSUE_THRESHOLD: Duration = Duration.ofMinutes(15) // TODO: 개발 다 되면 ??로 설정

        val accessTokenSecret: ByteArray = loadSecret("SERVICE_ACCESS_TOKEN_SECRET", "service-access-token-default-secret")
        val refreshTokenSecret: ByteArray = loadSecret("SERVICE_REFRESH_TOKEN_SECRET", "service-refresh-token-default-secret")
        const val GCM_TAG_LENGTH_BITS = 128
        const val GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8
        const val GCM_IV_LENGTH_BYTES = 12

        fun base64UrlEncode(data: ByteArray): String = base64UrlEncoder.encodeToString(data)

        fun base64UrlDecode(data: String): ByteArray = base64UrlDecoder.decode(data)

        fun encodeJwt(header: JwtHeader, payload: ServiceApiAccessTokenPayload): String {
            val headerJson = objectMapper.writeValueAsBytes(header)
            val payloadJson = objectMapper.writeValueAsBytes(payload)
            val encodedHeader = base64UrlEncode(headerJson)
            val encodedPayload = base64UrlEncode(payloadJson)
            val data = "$encodedHeader.$encodedPayload"
            val signature = signWithHmac(data.toByteArray(Charsets.UTF_8), accessTokenSecret)
            val encodedSignature = base64UrlEncode(signature)
            return "$data.$encodedSignature"
        }

        fun decodeJwt(parts: List<String>): ServiceApiAccessTokenPayload {
            require(parts.size == 3) { "Invalid JWT token format" }

            val (encodedHeader, encodedPayload, encodedSignature) = parts
            val expectedSignature = signWithHmac("$encodedHeader.$encodedPayload".toByteArray(Charsets.UTF_8), accessTokenSecret)
            val providedSignature = base64UrlDecode(encodedSignature)
            require(MessageDigest.isEqual(providedSignature, expectedSignature)) { "Invalid JWT signature" }

            val header: JwtHeader = objectMapper.readValue(base64UrlDecode(encodedHeader))
            require(header.alg == "HS256") { "Unsupported JWT alg" }
            require(header.typ == "JWT") { "Unsupported JWT typ" }

            val payloadBytes = base64UrlDecode(encodedPayload)
            return objectMapper.readValue(payloadBytes)
        }

        fun generateJwtId(): String {
            val bytes = ByteArray(16)
            secureRandom.nextBytes(bytes)
            return base64UrlEncode(bytes)
        }

        fun encryptJwe(header: JweHeader, payload: ServiceApiRefreshTokenPayload): String {
            val headerJson = objectMapper.writeValueAsBytes(header)
            val payloadJson = objectMapper.writeValueAsBytes(payload)
            val encodedHeader = base64UrlEncode(headerJson)
            val aad = encodedHeader.toByteArray(StandardCharsets.US_ASCII)

            val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(refreshTokenSecret, "AES"),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
            )
            cipher.updateAAD(aad)
            val cipherTextWithTag = cipher.doFinal(payloadJson)
            require(cipherTextWithTag.size >= GCM_TAG_LENGTH_BYTES) { "Invalid GCM output length" }

            val cipherText = cipherTextWithTag.copyOfRange(0, cipherTextWithTag.size - GCM_TAG_LENGTH_BYTES)
            val authTag = cipherTextWithTag.copyOfRange(
                cipherTextWithTag.size - GCM_TAG_LENGTH_BYTES,
                cipherTextWithTag.size,
            )

            val parts = listOf(
                encodedHeader,
                "",
                base64UrlEncode(iv),
                base64UrlEncode(cipherText),
                base64UrlEncode(authTag),
            )

            return parts.joinToString(".")
        }

        fun decryptJwe(parts: List<String>): ServiceApiRefreshTokenPayload {
            require(parts.size == 5) { "Invalid JWE token format" }
            require(parts[1].isEmpty()) { "Unsupported JWE key encryption" }

            val encodedHeader = parts[0]
            val headerBytes = base64UrlDecode(encodedHeader)
            val header: JweHeader = objectMapper.readValue(headerBytes)
            require(header.alg == "dir") { "Unsupported JWE alg" }
            require(header.enc == "A256GCM") { "Unsupported JWE enc" }
            require(header.typ == "JWE") { "Unsupported JWE typ" }

            val iv = base64UrlDecode(parts[2])
            val cipherText = base64UrlDecode(parts[3])
            val authTag = base64UrlDecode(parts[4])

            val aad = encodedHeader.toByteArray(StandardCharsets.US_ASCII)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(refreshTokenSecret, "AES"),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
            )
            cipher.updateAAD(aad)

            val cipherTextWithTag = ByteArray(cipherText.size + authTag.size).also { combined ->
                System.arraycopy(cipherText, 0, combined, 0, cipherText.size)
                System.arraycopy(authTag, 0, combined, cipherText.size, authTag.size)
            }

            val plainBytes = cipher.doFinal(cipherTextWithTag)
            return objectMapper.readValue(plainBytes)
        }

        data class JwtHeader(
            val alg: String = "HS256",
            val typ: String = "JWT",
        )

        data class JweHeader(
            val alg: String = "dir",
            val enc: String = "A256GCM",
            val typ: String = "JWE",
        )

        private fun signWithHmac(data: ByteArray, secret: ByteArray): ByteArray {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret, "HmacSHA256"))
            return mac.doFinal(data)
        }

        private fun loadSecret(envKey: String, fallback: String): ByteArray {
            val candidate = System.getenv(envKey)?.takeIf { it.isNotBlank() }
                ?: System.getProperty(envKey)?.takeIf { it.isNotBlank() }
                ?: fallback
            val hashHex = Sha256HashingUtil.sha256Hex(candidate)
            return Sha256HashingUtil.hexToBytes(hashHex)
        }
    }
}
