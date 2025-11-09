package com.service.api.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(value = JsonInclude.Include.NON_NULL)
data class Response<T>(
    val code: String,
    val message: String? = null,
    val content: T? = null,
    val error: T? = null,
) {
    companion object {
        const val SUCCESS = "SUCCESS"

        fun <T> success(
            content: T,
            message: String? = "Success",
        ): Response<T> =
            Response(
                code = SUCCESS,
                content = content,
                message = message,
                error = null,
            )

        fun <T> error(
            errorCode: String,
            message: String,
            error: T,
        ): Response<T> =
            Response(
                code = errorCode,
                content = null,
                message = message,
                error = error,
            )
    }
}
