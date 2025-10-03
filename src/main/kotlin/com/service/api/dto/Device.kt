package com.service.api.dto

import com.service.api.common.enum.PushTokenType

data class Device(
    val pushTokenType: PushTokenType = PushTokenType.FCM_REGISTRATION_TOKEN,
    val pushToken: String,
)
