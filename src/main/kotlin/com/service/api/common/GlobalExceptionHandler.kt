package com.service.api.common

import com.service.api.common.exception.*
import com.service.api.dto.Response
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

    // ===== 208 중복 가입 거절 =====
    @ExceptionHandler(AlreadySignupCiException::class)
    fun handleAlreadySignupCiException(ex: AlreadySignupCiException): ResponseEntity<Response<AlreadySignupCiInfo>> {
        log.debug("[208-ALREADY_REPORTED] {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.ALREADY_REPORTED).body(Response(
                success = false,
                data = ex.data,
                message = ex.message,
                errorCode = ex.errorCode,
        ))
    }

    // ===== 401 토큰 만료 (토큰 갱신 필요) =====
    @ExceptionHandler(ExpiredTokenException::class)
    fun handleUnauthorized(ex: ExpiredTokenException): ResponseEntity<Void> {
        log.debug("[401-UNAUTHORIZED] {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
    }

    // ===== 403 소셜 로그인이 유효하지 않음 =====
    @ExceptionHandler(InvalidSocialException::class)
    fun handleForbidden(ex: InvalidSocialException): ResponseEntity<Void> {
        log.debug("[403-FORBIDDEN] {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    // ===== 412 필수 약관 동의가 누락되거나, 약관 정보가 최신 약관 정보와 불일치 (약관 갯수, 버전 등 확인 필요) =====
    @ExceptionHandler(TermsAgreementException::class)
    fun handlePreconditionFailed(ex: TermsAgreementException): ResponseEntity<Void> {
        log.debug("[412-PRECONDITION_FAILED] {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build()
    }

    // ===== 423 토큰 정보 불일치 (강제 로그아웃 필요) =====
    @ExceptionHandler(InvalidTokenException::class)
    fun handleLocked(ex: InvalidTokenException): ResponseEntity<Void> {
        log.debug("[423-LOCKED] {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.LOCKED).build()
    }

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
    )
    fun handleBadRequest(ex: Exception): ResponseEntity<Void> {
        log.debug("[400-BAD_REQUEST] {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
    }

    // ===== 마지막: 예상치 못한 예외 =====
    @ExceptionHandler(Exception::class)
    fun handleUnknown(ex: Exception, request: HttpServletRequest): ResponseEntity<Void> {
        log.error("[500-INTERNAL_SERVER_ERROR] {}", ex.message, ex)
        return ResponseEntity.internalServerError().build()
    }
}
