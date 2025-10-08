package com.service.api.persistence.mapper

import com.service.api.model.Device
import com.service.api.persistence.entity.JpaUserDeviceEntity

object DeviceMapper {
    fun toModel(entity: JpaUserDeviceEntity) =
        Device(
            pushTokenType = entity.pushTokenType,
            pushToken = entity.pushToken,
        )
}
