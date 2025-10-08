package com.service.api.model

import com.service.api.persistence.entity.JpaUserDeviceEntity
import com.service.api.persistence.entity.JpaUserIdentityEntity
import com.service.api.persistence.entity.JpaUserProfileEntity

interface UserProjection {
    val identity: JpaUserIdentityEntity
    val profile: JpaUserProfileEntity
    val device: JpaUserDeviceEntity
}
