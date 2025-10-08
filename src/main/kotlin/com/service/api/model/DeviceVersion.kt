package com.service.api.model

data class DeviceVersion(
    val accessTokenIat: Long,
    val osVersion: String,
    val appVersion: String,
)
