package com.service.api.common

import com.service.api.common.enum.OsType

data class ApiRequestContext(
    val customDeviceId: String,
    val deviceModel: String,
    val osType: OsType,
    val osVersion: String,
    val appVersion: String,
    val clientIp: String?,
    var serviceUserId: Long? = null, // 인증 인터셉터를 타는 경우 채워짐
    var socialId: Long? = null, // 인증 인터셉터를 타는 경우 채워짐
)

object ApiRequestContextHolder {
    private val tl = ThreadLocal<ApiRequestContext?>()

    fun set(ctx: ApiRequestContext) = tl.set(ctx)
    fun get(): ApiRequestContext =
        tl.get() ?: error("ApiRequestContext is not set. Did you register the filter?")
    fun clear() = tl.remove()
}
