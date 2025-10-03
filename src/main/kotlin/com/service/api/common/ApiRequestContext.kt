package com.service.api.common

import com.service.api.common.enum.OsType

data class ApiRequestContext(
    val customDeviceId: String,
    val deviceModel: String,
    val osType: OsType,
    val osVersion: String,
    val appVersion: String,
    val clientIp: String?,
)

object ApiRequestContextHolder {
    private val tl = ThreadLocal<ApiRequestContext?>()

    fun set(ctx: ApiRequestContext) = tl.set(ctx)
    fun get(): ApiRequestContext =
        tl.get() ?: error("ApiRequestContext is not set. Did you register the filter?")
    fun clear() = tl.remove()
}

