package com.service.api.util

object StringUtil {
    val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun String?.isValidUuid(): Boolean {
        if (this.isNullOrBlank() || this.length != 36) return false

        return uuidRegex.matches(this)
    }

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
