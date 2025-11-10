package com.service.api.interceptor

import com.service.api.common.ApiRequestContextHolder
import com.service.api.common.exception.ExpiredTokenException
import com.service.api.common.exception.InvalidTokenException
import com.service.api.service.DeviceService
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class AuthorizationInterceptor(
    private val deviceService: DeviceService,
) {

    @Around("@annotation(com.service.api.interceptor.Auth)")
    fun authorize(joinPoint: ProceedingJoinPoint): Any? {
        val request = currentRequest()
        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
            ?: throw ExpiredTokenException("Authorization header is required.")

        val accessToken = extractAccessToken(authorizationHeader)

        // 요청 헤더, DB 디바이스 정보, 토큰 정보 3개 간 일치 여부 확인
        with (deviceService.getServiceApiTokenPayload(accessToken)) {
            val ctx = ApiRequestContextHolder.get()
            ctx.serviceUserId = serviceUserId
            ctx.socialId = socialId

            deviceService.getDeviceVersionForAuth(serviceUserId, customDeviceId, deviceModel, osType)
                ?.also { v ->
                    // DB와 다른 버전 정보가 있으면 토큰 갱신하게 해서 DB 최신화를 유도
                    if (v.accessTokenIat != iat || v.osVersion != ctx.osVersion || v.appVersion != ctx.appVersion)
                        throw ExpiredTokenException("deviceVersion unmatched: $v != $iat/${ctx.osVersion}/${ctx.appVersion}")
                }
                ?: throw InvalidTokenException("deviceVersion not found: $serviceUserId/$customDeviceId")
        }

        return joinPoint.proceed()
    }

    private fun currentRequest(): HttpServletRequest {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            ?: throw RuntimeException("HttpServletRequest is not available.")
        return attributes.request
    }

    private fun extractAccessToken(headerValue: String): String {
        val bearerPrefix = "Bearer "
        if (!headerValue.startsWith(bearerPrefix, ignoreCase = true)) {
            throw ExpiredTokenException("Authorization header must start with Bearer.")
        }

        return headerValue.substring(bearerPrefix.length).trim()
            .takeIf { it.isNotEmpty() }
            ?: throw ExpiredTokenException("Access token is missing in Authorization header.")
    }
}
