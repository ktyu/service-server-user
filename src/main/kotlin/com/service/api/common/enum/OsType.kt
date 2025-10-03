package com.service.api.common.enum

enum class OsType {
    iOS,
    Android,
    ;

    companion object {
        fun valueOfOrNull(name: String?): OsType? = entries.find { it.name.uppercase() == name?.uppercase() }
    }
}
