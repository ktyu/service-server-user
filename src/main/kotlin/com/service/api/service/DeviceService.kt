package com.service.api.service

import com.service.api.common.ApiRequestContextHolder
import com.service.api.common.enum.PushTokenType
import com.service.api.common.enum.SocialType
import com.service.api.common.exception.InvalidSocialException
import com.service.api.common.exception.InvalidTokenException
import com.service.api.persistence.entity.JpaUserDeviceEntity
import com.service.api.persistence.entity.UserDeviceId
import com.service.api.persistence.repository.UserDeviceRepository
import com.service.api.persistence.repository.UserIdentityRepository
import com.service.api.service.social.SocialService
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

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
        val (accessToken, refreshToken) = issueTokens(iat, socialType, userIdentitySub)
        userDeviceEntity.accessTokenIssuedAt = iat
        userDeviceEntity.refreshTokenIssuedAt = iat

        userDeviceRepository.save(userDeviceEntity)

        return Pair(accessToken, refreshToken!!)
    }

    @Transactional
    fun refreshTokens(serviceUserId: Long, refreshToken: String): Pair<String, String?> {
        /** TODO: [토큰 갱신 로직]
         * - refresh token의 exp가 지났거나 service_user_id가 요청과 토큰의 값이 다르면 invalid 응답 (로그아웃 필요)
         * - 소셜측 API 호출해서 연결 상태 유효하지 않거나 현재 소셜의 sub값이 우리DB의 sub값과 다르면 로그아웃 응답 (단, 소셜측 API 에러나 쿼터초과 대비 -> 캐싱 필요)
             - 이 과정에서 소셜측 refresh_token이 새로 나왔다면 우리 db에 갱신저장
         * - (아래에 토큰 검증시 로직에서 첫줄만빼고 동일한 로직 진행)
         * - DB상 user_device의 last_issued_at 현재 시간으로 갱신
         * - refresh_token_issued_at 보고 필요하면 refresh_token도 생성
         * - access_token, refresh_token(이건 필요시에만) 리턴
         * - 최신 약관 있거나 추가정보 없으면 202 리턴
         */
        val ctx = ApiRequestContextHolder.get()
        val userDeviceEntity = userDeviceRepository.findByIdAndDeletedAtIsNull(UserDeviceId(serviceUserId, ctx.customDeviceId))
            ?: throw InvalidTokenException("UserDeviceEntity not found: ${serviceUserId}/${ctx.customDeviceId}")

        // TODO: refresh token 검증 (iat 및 다른 정보들 일치.. 등) - 이건 access token 검증로직과 일치
        if (!refreshToken.endsWith(userDeviceEntity.refreshTokenIssuedAt.toString()))
            throw InvalidTokenException("refresh token iat invalid")
        // TODO: 토큰의 serviceUserId 가 파라미터로 들어온 serviceUserId 와 동일한지도 여기서 검사 필요

        // 소셜 연결 상태 및 userIdentity 매칭 검증
        val socialType = SocialType.KAKAO // TODO: refresh token 에서 꺼내와야함
        val sub = "4422667003" // TODO: refresh token 에서 꺼내와야함
        socialService.getSocialUuidWithValidation(socialType, sub).also { socialUuid ->
            log.info("social status validated: $socialUuid/$socialType/$sub")
        }
        userIdentityRepository.findByServiceUserIdAndDeletedAtIsNull(serviceUserId)?.getSub(socialType)
            ?.also { userIdentitySub ->
                if (userIdentitySub != sub)
                    throw InvalidSocialException("sub in userIdentityEntity(serviceUserId=$serviceUserId) unmatched: $userIdentitySub/$sub")
            }
            ?: throw InvalidSocialException("sub in userIdentityEntity(serviceUserId=$serviceUserId) is null: $socialType")

        // 토큰 발급
        val newIat = Instant.now().epochSecond
        val (newAccessToken, newRefreshToken) = issueTokens(newIat, socialType, sub)
        userDeviceEntity.accessTokenIssuedAt = newIat
        userDeviceEntity.refreshTokenIssuedAt = newIat

        userDeviceRepository.save(userDeviceEntity)

        return Pair(newAccessToken, newRefreshToken)
    }

    private fun issueTokens(iat: Long, socialType: SocialType, sub: String, existingRefreshTokenExp: Long? = null): Pair<String, String?> {
        // TODO: existingRefreshTokenIat 가 null 이거나 만료가 n일 이내 인지 확인해서 refreshToken 발급 필요 여부 판단
        val issueRefreshToken = existingRefreshTokenExp?.let { it < Instant.now().plus(Duration.ofMinutes(1)).epochSecond } ?: true

        // TODO: access_token jwt 생성
        val accessToken = "temp-access-token-${iat}"

        if (!issueRefreshToken) return Pair("", null)

        return Pair(accessToken, "temp-refresh-token-${socialType}-${sub}-${iat}")
    }

    internal fun getLastLoginDateByServiceUserId(serviceUserId: Long): LocalDate? {
        return userDeviceRepository.findAllById_ServiceUserId(serviceUserId)
            .maxOfOrNull { it.lastLoginAt }
            ?.toLocalDate()
    }
}
