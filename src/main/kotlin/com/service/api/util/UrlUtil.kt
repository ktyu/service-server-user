package com.service.api.util

import jakarta.servlet.http.HttpServletRequest

object UrlUtil {
    fun getCurrentBaseUrl(request: HttpServletRequest): String {
        var scheme = request.scheme            // http or https
        val host = request.serverName          // domain.com or IP
        var port = request.serverPort          // 80, 443, 8080 etc
        val context = request.contextPath      // 보통 ""

        if (host.endsWith("service.co.kr")) {
            // 서비스 공식 도메인으로 접근한 경우 https 강제
            scheme = "https"
            port = 443
        }

        val baseUrl =
            if ((scheme == "http" && port == 80) || (scheme == "https" && port == 443))
                "$scheme://$host$context"
            else
                "$scheme://$host:$port$context"

        return baseUrl
    }
}
