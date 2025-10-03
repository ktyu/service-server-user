package com.service.api.dto

data class Response<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errorCode: String? = null,
) {
    companion object {
        fun <T> success(
            data: T,
            message: String? = "Success",
        ): Response<T> =
            Response(
                success = true,
                data = data,
                message = message,
                errorCode = null,
            )

        fun <T> error(
            message: String,
            errorCode: String? = null,
        ): Response<T> =
            Response(
                success = false,
                data = null,
                message = message,
                errorCode = errorCode,
            )
    }
}
