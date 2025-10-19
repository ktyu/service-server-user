package com.service.api.filter

import com.service.api.common.ApiRequestContext
import com.service.api.common.ApiRequestContextHolder
import com.service.api.common.enum.OsType
import com.service.api.util.StringUtil.isValidUuid
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

class ApiHeaderContextFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // (참고) http header 의 deviceModel, osVersion 존재 여부는 alb 에서 체크되서 들어온 상태

            val customDeviceId: String? = request.getHeader("X-Service-Custom-Device-Id")
            if (!customDeviceId.isValidUuid()) {
                log.debug("[400-BAD_REQUEST] invalid customDeviceId: {}", customDeviceId)
                reject(response, HttpStatus.BAD_REQUEST.value())
                return
            }

            val osType: OsType? = OsType.valueOfOrNull(request.getHeader("X-Service-Os-Type"))
            if (osType == null) {
                log.debug("[400-BAD_REQUEST] invalid osType: {}", request.getHeader("X-Service-Os-Type"))
                reject(response, HttpStatus.BAD_REQUEST.value())
                return
            }

            val appVersion: String? = request.getHeader("X-Service-App-Version")
            if (!appVersion.isSupportedAppVersion()) {
                log.debug("[426-UPGRADE_REQUIRED] unsupported appVersion: {}", appVersion)
                reject(response, HttpStatus.UPGRADE_REQUIRED.value())
                return
            }

            // ThreadLocal 에 ApiRequestContext 설정
            ApiRequestContextHolder.set(ApiRequestContext(
                customDeviceId = customDeviceId!!,
                deviceModel = request.getHeader("X-Service-Device-Model")!!,
                osType = osType,
                osVersion = request.getHeader("X-Service-Os-Version")!!,
                appVersion = appVersion!!,
                clientIp = request.getHeader("X-Forwarded-For")?.substringBefore(",") ?: request.remoteAddr
            ))

            filterChain.doFilter(request, response)
        } finally {
            ApiRequestContextHolder.clear()
        }
    }

    private fun reject(res: HttpServletResponse, status: Int) {
        if (!res.isCommitted) {
            res.resetBuffer()          // 혹시 이미 쓰인 내용 제거
            res.status = status        // 원하는 상태코드 지정
            res.flushBuffer()          // 응답 확정 → 체인 중단 필요
        }
    }

    private fun String?.isSupportedAppVersion(): Boolean {
        if (this.isNullOrBlank()) return false

        val parts = this.split('.')
        if (parts.size != 3) return false

        return parts.all { p ->
            val n = p.toIntOrNull() ?: return false
            n in 0..99
        }
    }
}
