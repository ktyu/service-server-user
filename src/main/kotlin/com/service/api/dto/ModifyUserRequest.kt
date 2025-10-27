package com.service.api.dto

import com.service.api.model.Device
import com.service.api.model.Profile

data class ModifyUserRequest(
    val profile: Profile?,
    val device: Device?,
)
