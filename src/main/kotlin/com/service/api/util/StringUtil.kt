package com.service.api.util

object StringUtil {
    fun String?.nullIfBlank(): String? {
        if (this == null) {
            return null
        }

        if (this.isBlank()) {
            return null
        }

        return this
    }
}
