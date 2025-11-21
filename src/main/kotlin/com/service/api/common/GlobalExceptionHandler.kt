package com.service.api.common

import com.service.api.common.exception.*
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice(basePackages = ["com.service.api"])
@Order(0)
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    // ===== 400 잘못된 요청 =====
    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        BindException::class,
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
        MissingRequestHeaderException::class,
        HttpMessageNotReadableException::class,
        HttpRequestMethodNotSupportedException::class,

        IllegalArgumentException::class,
        TermsAgreementException::class,
    )
    fun handleBadRequest(ex: Exception): ResponseEntity<Void> {
        log.debug("[400-BAD_REQUEST] {}", ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
    }

    // ===== 401 인증 실패 (엑세스 토큰 리프레시 필요) =====
    @ExceptionHandler(
        // 토큰 만료
        ExpiredTokenException::class,

        // 유효하지 않은 인증
        InvalidTokenException::class,
        InvalidSocialException::class,
        ServiceUserNotFoundException::class,
    )
    fun handleUnauthorized(ex: Exception): ResponseEntity<Void> {
        log.debug("[401-UNAUTHORIZED] {}", ex.message)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
    }

    // ===== 403 토큰 갱신 실패 (로그아웃 필요) =====
    @ExceptionHandler(RefreshTokenFailedException::class)
    fun handleForbidden(ex: RefreshTokenFailedException): ResponseEntity<Void> {
        log.debug("[403-FORBIDDEN] {}", ex.message)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    // ===== 마지막: 예상치 못한 예외 =====
    @ExceptionHandler(Exception::class)
    fun handleUnknown(ex: Exception): ResponseEntity<Void> {
        log.error("[500-INTERNAL_SERVER_ERROR] {}", ex.message, ex)
        return ResponseEntity.internalServerError().build()
    }
}
